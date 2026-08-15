package com.example.data.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import androidx.media3.session.MediaController
import android.content.ComponentName
import java.util.concurrent.Executor
import com.example.data.model.Surah
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

enum class FailoverToastAction {
    SHOW_TOAST
}

// Global Thread-Safe Cache for Stream Health & Server Latency Metrics
object SourceHealthCache {
    private val hostLatencies = ConcurrentHashMap<String, Long>()
    private val unhealthyHosts = ConcurrentHashMap<String, Long>() // Hostname -> Expiration timestamp

    fun recordSuccess(url: String, latency: Long) {
        val host = getHost(url) ?: return
        hostLatencies[host] = latency
        unhealthyHosts.remove(host)
    }

    fun recordFailure(url: String) {
        val host = getHost(url) ?: return
        hostLatencies.remove(host)
        // Set penalty: keep un-selected for 8 minutes
        unhealthyHosts[host] = System.currentTimeMillis() + 480000L
    }

    fun getHostLatency(url: String): Long {
        val host = getHost(url) ?: return Long.MAX_VALUE
        val expiration = unhealthyHosts[host]
        if (expiration != null && expiration > System.currentTimeMillis()) {
            return Long.MAX_VALUE - 1000L // Heavy penalty
        }
        return hostLatencies[host] ?: 2000L // Default moderate latency
    }

    private fun getHost(url: String): String? {
        return try {
            URL(url).host
        } catch (e: Exception) {
            null
        }
    }
}

data class PlaybackState(
    val currentSurah: Surah? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val progressMs: Long = 0,
    val durationMs: Long = 0,
    val playbackSpeed: Float = 1.0f,
    val repeatMode: Int = Player.REPEAT_MODE_OFF, // OFF, ONE, ALL
    val isShuffleEnabled: Boolean = false,
    val activeProviderId: String = "minshawi_murattal",
    val sleepTimerMinutesLeft: Int = 0, // 0 means inactive
    val playlist: List<Surah> = Surah.ALL_SURAHS
)

class AudioPlaybackManager(
    private val context: Context,
    private val getLocalDownloadPath: suspend (Int) -> String?
) {
    private val applicationContext = context.applicationContext
    
    private var mediaController: MediaController? = null
    
    val exoPlayer: Player?
        get() = mediaController

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var playbackInitJob: Job? = null
    private var progressHandler: Handler? = null

    // For multi-link failover and robust fallback
    private var candidateUrls: List<String> = emptyList()
    private var currentUrlAttemptIndex = 0
    private var lastPlaybackPositionMs = 0L

    // Metrics for analytics
    private var failoverCount = 0

    init {
        initPlayer()
    }

    private fun initPlayer() {
        if (mediaController != null) return

        Log.d("DIAGNOSTICS", "[DIAGNOSTICS] Initializing MediaController connection")
        try {
            val sessionToken = SessionToken(applicationContext, ComponentName(applicationContext, PlayerService::class.java))
            val controllerFuture = MediaController.Builder(applicationContext, sessionToken).buildAsync()
            controllerFuture.addListener({
                try {
                    val controller = controllerFuture.get()
                    mediaController = controller
                    setupPlayerListeners(controller)
                    
                    // Sync state from background session
                    _state.value = _state.value.copy(
                        isPlaying = controller.isPlaying,
                        isLoading = controller.playbackState == Player.STATE_BUFFERING,
                        progressMs = controller.currentPosition,
                        durationMs = controller.duration.coerceAtLeast(0),
                        playbackSpeed = controller.playbackParameters.speed,
                        repeatMode = controller.repeatMode
                    )
                    if (controller.isPlaying) {
                        startProgressUpdates()
                    }
                    Log.d("DIAGNOSTICS", "[DIAGNOSTICS] MediaController connected and state synced")
                } catch (e: Exception) {
                    Log.e("DIAGNOSTICS", "Failed to connect MediaController", e)
                }
            }, Executor { command -> Handler(Looper.getMainLooper()).post(command) })
        } catch (e: Exception) {
            Log.e("DIAGNOSTICS", "Failed to init MediaController", e)
        }
    }

    fun setupPlayerListeners(player: Player) {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Playback Started")
                }
                Log.d("AudioPlaybackManager", "VERBOSE: onIsPlayingChanged = $isPlaying")
                try {
                    val currentPlaybackState = player.playbackState
                    val pos = if (currentPlaybackState != Player.STATE_IDLE) {
                        try { player.currentPosition } catch (e: Exception) { 0L }
                    } else 0L

                    val dur = if (currentPlaybackState != Player.STATE_IDLE) {
                        try { player.duration.coerceAtLeast(0) } catch (e: Exception) { 0L }
                    } else 0L

                    _state.value = _state.value.copy(
                        isPlaying = isPlaying,
                        isLoading = currentPlaybackState == Player.STATE_BUFFERING,
                        progressMs = pos,
                        durationMs = dur
                    )
                    if (isPlaying) {
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                } catch (e: Exception) {
                    Log.e("AudioPlaybackManager", "Error in onIsPlayingChanged listener", e)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d("AudioPlaybackManager", "VERBOSE: onPlaybackStateChanged = $playbackState")
                try {
                    val pos = if (playbackState != Player.STATE_IDLE) {
                        try { player.currentPosition } catch (e: Exception) { 0L }
                    } else 0L

                    val dur = if (playbackState != Player.STATE_IDLE) {
                        try { player.duration.coerceAtLeast(0) } catch (e: Exception) { 0L }
                    } else 0L

                    _state.value = _state.value.copy(
                        isLoading = playbackState == Player.STATE_BUFFERING,
                        progressMs = pos,
                        durationMs = dur
                    )

                    if (playbackState == Player.STATE_ENDED) {
                        Log.d("AudioPlaybackManager", "VERBOSE: playback state ended")
                        handleSurahEnded()
                    }
                } catch (e: Exception) {
                    Log.e("AudioPlaybackManager", "Error in onPlaybackStateChanged listener", e)
                }
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                Log.d("AudioPlaybackManager", "VERBOSE: onPlaybackParametersChanged, speed = ${playbackParameters.speed}")
                try {
                    _state.value = _state.value.copy(playbackSpeed = playbackParameters.speed)
                } catch (e: Exception) {
                    Log.e("AudioPlaybackManager", "Error in onPlaybackParametersChanged listener", e)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("AudioPlaybackManager", "ExoPlayer error on URL index $currentUrlAttemptIndex: ${error.message}", error)
                
                // Record host failure in cache instantly
                if (currentUrlAttemptIndex < candidateUrls.size) {
                    SourceHealthCache.recordFailure(candidateUrls[currentUrlAttemptIndex])
                }

                // Record position so we resume cleanly
                lastPlaybackPositionMs = try { player.currentPosition } catch (e: Exception) { 0L }

                val nextIndex = currentUrlAttemptIndex + 1
                if (nextIndex < candidateUrls.size) {
                    currentUrlAttemptIndex = nextIndex
                    failoverCount++
                    Log.w("AudioPlaybackManager", "Fallback engaged! Retrying with candidate url at index $currentUrlAttemptIndex")
                    
                    Handler(Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            applicationContext,
                            "جاري التبديل للمشغل البديل لتفادي عطل السيرفر الحالي...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

                    coroutineScope.launch {
                        delay(500) // Small break to prevent tight recursion loops
                        playCurrentCandidateUrl()
                    }
                } else {
                    Log.e("AudioPlaybackManager", "All available candidate server URLs failed. Halting playback.")
                    Handler(Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(
                            applicationContext,
                            "عذراً، تعذر الاتصال بكافة سيرفرات البث حالياً لتشغيل هذه السورة.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    _state.value = _state.value.copy(isPlaying = false)
                }
            }
        })

        progressHandler = Handler(Looper.getMainLooper())
    }

    private fun xoPlayerInstantiated(): Boolean = mediaController != null

    fun playSurah(surah: Surah, customplaylist: List<Surah> = Surah.ALL_SURAHS, startPositionMs: Long = 0L) {
        val playRequestStartTime = System.currentTimeMillis()
        Log.i("DIAGNOSTICS", "[DIAGNOSTICS] playSurah command received for Surah: ${surah.number}")

        initPlayer()

        _state.value = _state.value.copy(
            currentSurah = surah,
            playlist = customplaylist,
            isLoading = true
        )

        // Cancel any previous pending playback initialization to prevent race conditions
        playbackInitJob?.cancel()

        // Move metadata, database query, and network discovery logic entirely to Dispatchers.IO
        playbackInitJob = coroutineScope.launch(Dispatchers.IO) {
            val localPath = getLocalDownloadPath(surah.number)
            val finalCandidates = if (localPath != null && File(localPath).exists()) {
                Log.d("DIAGNOSTICS", "[DIAGNOSTICS] Playing from verified local storage path: $localPath")
                listOf(Uri.fromFile(File(localPath)).toString())
            } else {
                val rawCandidates = getCandidateUrls(_state.value.activeProviderId, surah.number, surah.audioUrl)
                validateAndRankSources(rawCandidates)
            }

            withContext(Dispatchers.Main) {
                candidateUrls = finalCandidates
                currentUrlAttemptIndex = 0
                lastPlaybackPositionMs = startPositionMs

                // Wait for players to be fully constructed if asynchronous initialization is in progress
                var waitAttempts = 0
                while (exoPlayer == null && waitAttempts < 100) {
                    delay(50)
                    waitAttempts++
                }

                playCurrentCandidateUrl()

                val elapsedMs = System.currentTimeMillis() - playRequestStartTime
                Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Startup delay completed in: ${elapsedMs}ms. Playback active.")
            }
        }
    }

    // Parallel asynchronous connect validation engine
    private suspend fun validateAndRankSources(sources: List<String>): List<String> = withContext(Dispatchers.IO) {
        val validationStart = System.currentTimeMillis()
        Log.d("DIAGNOSTICS", "[DIAGNOSTICS] Starting parallel HTTP validation for candidate URLs: $sources")

        if (sources.size <= 1) {
            return@withContext sources
        }

        val deferredResults = sources.map { urlString ->
            async {
                val startTime = System.currentTimeMillis()
                try {
                    val urlObj = URL(urlString)
                    val connection = urlObj.openConnection() as HttpURLConnection
                    connection.requestMethod = "HEAD"
                    connection.connectTimeout = 2000 // 2 seconds connect timeout
                    connection.readTimeout = 2000    // 2 seconds read timeout
                    connection.instanceFollowRedirects = true
                    connection.connect()
                    
                    val responseCode = connection.responseCode
                    connection.disconnect()

                    if (responseCode in 200..399) {
                        val latency = System.currentTimeMillis() - startTime
                        Log.d("DIAGNOSTICS", "[DIAGNOSTICS] URL Verified: $urlString, Response: $responseCode, Latency: ${latency}ms")
                        SourceHealthCache.recordSuccess(urlString, latency)
                        Pair(urlString, latency)
                    } else {
                        Log.w("DIAGNOSTICS", "[DIAGNOSTICS] URL Rejected with code: $responseCode - $urlString")
                        SourceHealthCache.recordFailure(urlString)
                        Pair(urlString, Long.MAX_VALUE)
                    }
                } catch (e: Exception) {
                    Log.w("DIAGNOSTICS", "[DIAGNOSTICS] Ping timed out or failed for: $urlString. Message: ${e.message}")
                    SourceHealthCache.recordFailure(urlString)
                    Pair(urlString, Long.MAX_VALUE)
                }
            }
        }

        // Await all concurrently running head pings
        val pingMetrics = deferredResults.awaitAll()

        // Sort based on real-time latency
        val orderedCandidates = pingMetrics.sortedBy { it.second }.map { it.first }

        val elapsedValidationTime = System.currentTimeMillis() - validationStart
        Log.d("DIAGNOSTICS", "[DIAGNOSTICS] Source verification finished in ${elapsedValidationTime}ms. Priority mapping: $orderedCandidates")

        orderedCandidates
    }

    private fun playCurrentCandidateUrl() {
        val player = exoPlayer ?: return
        if (candidateUrls.isEmpty() || currentUrlAttemptIndex >= candidateUrls.size) {
            Log.e("AudioPlaybackManager", "No verified URLs available")
            return
        }

        val urlString = candidateUrls[currentUrlAttemptIndex]
        val uri = Uri.parse(urlString)
        val surah = _state.value.currentSurah ?: return

        coroutineScope.launch(Dispatchers.Main) {
            Log.d("AudioPlaybackManager", "Resolving stream attempt [$currentUrlAttemptIndex/${candidateUrls.size - 1}]: $urlString")
            val reciterName = com.example.data.repository.RemoteConfigService.KNOWN_RECITERS.find { it.id == _state.value.activeProviderId }?.displayName
                ?: "Sheikh Muhammad Siddiq Al-Minshawi"

            val mediaMetadata = MediaMetadata.Builder()
                .setTitle(surah.nameEnglish)
                .setSubtitle(surah.nameArabic)
                .setArtist(reciterName)
                .build()

            val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMediaId(surah.number.toString())
                .setMediaMetadata(mediaMetadata)
                .build()

            player.stop()
            player.clearMediaItems()
            player.setMediaItem(mediaItem)
            player.prepare()
            if (lastPlaybackPositionMs > 0L) {
                player.seekTo(lastPlaybackPositionMs)
            }
            player.play()

            _state.value = _state.value.copy(
                durationMs = player.duration.coerceAtLeast(0)
            )
        }
    }

    private fun getCandidateUrls(providerId: String, surahNumber: Int, defaultUrl: String): List<String> {
        val padded = String.format("%03d", surahNumber)
        val candidates = mutableListOf<String>()

        // 1. Prioritize reliable HTTPS CDNs first, followed by original ones, and finally Internet Archive backstops
        when (providerId) {
            "minshawi", "minshawi_murattal" -> {
                candidates.add("https://download.quranicaudio.com/quran/muhammad_siddiq_al-minshawi/murattal/$padded.mp3")
                candidates.add("https://server11.mp3quran.net/minsh/$padded.mp3")
                candidates.add("https://archive.org/download/muhammad-siddiq-al-minshawi-quran/$padded.mp3")
            }
            "minshawi_mujawwad" -> {
                candidates.add("https://download.quranicaudio.com/quran/muhammad_siddiq_al-minshawi/mujawwad/$padded.mp3")
                candidates.add("https://server11.mp3quran.net/minsh_mjwd/$padded.mp3")
                candidates.add("https://archive.org/download/Muhammad_Siddiq_Al-Minshawi_mujawwad/$padded.mp3")
            }
            "abdul_basit" -> {
                candidates.add("https://download.quranicaudio.com/quran/abdul_basit_murattal/$padded.mp3")
                candidates.add("https://server7.mp3quran.net/basit/$padded.mp3")
                candidates.add("https://archive.org/download/abdul-basit-murattal/$padded.mp3")
            }
            "al_hussary" -> {
                candidates.add("https://download.quranicaudio.com/quran/mahmoud_khalil_al-husary/$padded.mp3")
                candidates.add("https://server13.mp3quran.net/husr/$padded.mp3")
                candidates.add("https://archive.org/download/mahmoud-khalil-al-husasry-murattal/$padded.mp3")
            }
            "alafasy" -> {
                candidates.add("https://download.quranicaudio.com/quran/mishary_rashid_alafasy/$padded.mp3")
                candidates.add("https://server8.mp3quran.net/afs/$padded.mp3")
                candidates.add("https://archive.org/download/mishary-rashid-al-afasy_202011/$padded.mp3")
            }
            "muaiqly" -> {
                candidates.add("https://download.quranicaudio.com/quran/maher_al_muaiqly/$padded.mp3")
                candidates.add("https://server12.mp3quran.net/maher/$padded.mp3")
                candidates.add("https://archive.org/download/maher-al-muaiqly-murattal/$padded.mp3")
            }
        }

        // Add the database seeded default url if it's unique
        if (defaultUrl.isNotBlank() && !candidates.contains(defaultUrl)) {
            candidates.add(defaultUrl)
        }

        // Add a general-purpose MP3Quran fallback with alternative server block
        val backupMp3Quran = defaultUrl.replace("server11.mp3quran.net", "server10.mp3quran.net")
        if (backupMp3Quran.isNotBlank() && !candidates.contains(backupMp3Quran)) {
            candidates.add(backupMp3Quran)
        }

        return candidates.distinct()
    }

    fun togglePlayPause() {
        initPlayer()
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || player.mediaItemCount == 0) {
                _state.value.currentSurah?.let {
                    playSurah(it, _state.value.playlist)
                } ?: Surah.ALL_SURAHS.firstOrNull()?.let {
                    playSurah(it, Surah.ALL_SURAHS)
                }
            } else {
                player.play()
            }
        }
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun stop() {
        Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Player close (X) button clicked. Stopping playback and hiding player.")
        
        // Cancel any pending playback initialization to prevent playing after close
        playbackInitJob?.cancel()

        try {
            exoPlayer?.let { player ->
                player.stop()
                player.clearMediaItems()
            }
        } catch (e: Exception) {
            Log.e("DIAGNOSTICS", "Error stopping player on close button: ${e.message}", e)
        }

        stopProgressUpdates()

        _state.value = _state.value.copy(
            currentSurah = null,
            isPlaying = false,
            progressMs = 0,
            durationMs = 0,
            isLoading = false
        )

        try {
            val intent = Intent(applicationContext, PlayerService::class.java)
            applicationContext.stopService(intent)
        } catch (e: Exception) {
            Log.e("DIAGNOSTICS", "Failed to stop PlayerService on close", e)
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        player.seekTo(positionMs)
        _state.value = _state.value.copy(progressMs = positionMs)
    }

    fun skipForward() {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + 15000).coerceAtMost(player.duration)
        seekTo(newPos)
    }

    fun skipBackward() {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition - 15000).coerceAtLeast(0)
        seekTo(newPos)
    }

    fun playNext() {
        val list = _state.value.playlist
        val current = _state.value.currentSurah
        if (current == null) {
            list.firstOrNull()?.let { playSurah(it, list) }
            return
        }

        if (_state.value.isShuffleEnabled) {
            val nextRnd = list.randomOrNull() ?: current
            playSurah(nextRnd, list)
            return
        }

        val currentIndex = list.indexOfFirst { it.number == current.number }
        if (currentIndex != -1 && currentIndex < list.size - 1) {
            playSurah(list[currentIndex + 1], list)
        } else if (_state.value.repeatMode == Player.REPEAT_MODE_ALL) {
            list.firstOrNull()?.let { playSurah(it, list) }
        }
    }

    fun playPrevious() {
        val list = _state.value.playlist
        val current = _state.value.currentSurah
        if (current == null) {
            list.firstOrNull()?.let { playSurah(it, list) }
            return
        }

        val currentIndex = list.indexOfFirst { it.number == current.number }
        if (currentIndex != -1 && currentIndex > 0) {
            playSurah(list[currentIndex - 1], list)
        } else if (_state.value.repeatMode == Player.REPEAT_MODE_ALL) {
            list.lastOrNull()?.let { playSurah(it, list) }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = exoPlayer ?: return
        player.setPlaybackSpeed(speed)
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun cycleRepeatMode() {
        val player = exoPlayer ?: return
        val nextMode = when (_state.value.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }

        player.repeatMode = nextMode
        _state.value = _state.value.copy(repeatMode = nextMode)
    }

    fun toggleShuffle() {
        val newVal = !_state.value.isShuffleEnabled
        _state.value = _state.value.copy(isShuffleEnabled = newVal)
    }

    fun selectProvider(providerId: String) {
        _state.value = _state.value.copy(activeProviderId = providerId)
        // If playing stream, restart with new URL to take effect instantly
        val current = _state.value.currentSurah
        if (current != null && _state.value.isPlaying) {
            coroutineScope.launch {
                val isDownloaded = getLocalDownloadPath(current.number) != null
                if (!isDownloaded) {
                    playSurah(current, _state.value.playlist)
                }
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes == 0) {
            _state.value = _state.value.copy(sleepTimerMinutesLeft = 0)
            return
        }

        _state.value = _state.value.copy(sleepTimerMinutesLeft = minutes)
        sleepTimerJob = coroutineScope.launch {
            var left = minutes
            while (left > 0) {
                delay(60000L) // Count down every 1 minute
                left--
                _state.value = _state.value.copy(sleepTimerMinutesLeft = left)
            }
            // Timer expired, pause sound
            pause()
            _state.value = _state.value.copy(sleepTimerMinutesLeft = 0)
        }
    }

    private fun handleSurahEnded() {
        if (_state.value.repeatMode == Player.REPEAT_MODE_ONE) {
            _state.value.currentSurah?.let { playSurah(it, _state.value.playlist) }
        } else {
            playNext()
        }
    }

    private val progressRunnable = object : Runnable {
        override fun run() {
            try {
                val player = exoPlayer ?: return
                var isCurrentlyPlaying = false
                try {
                    isCurrentlyPlaying = player.isPlaying
                } catch (pe: Exception) {
                    // Player might be released
                }

                if (isCurrentlyPlaying) {
                    var currentPos = 0L
                    var duration = 0L
                    try {
                        currentPos = player.currentPosition
                        duration = player.duration.coerceAtLeast(0)
                    } catch (pe: Exception) {
                        // Player might be in idle or released state
                    }
                    _state.value = _state.value.copy(
                        progressMs = currentPos,
                        durationMs = duration
                    )
                    progressHandler?.postDelayed(this, 1000L)
                }
            } catch (e: Exception) {
                android.util.Log.e("AudioPlaybackManager", "VERBOSE ERROR: Exception in progressRunnable run", e)
            }
        }
    }

    private fun startProgressUpdates() {
        stopProgressUpdates()
        progressHandler?.post(progressRunnable)
    }

    private fun stopProgressUpdates() {
        progressRunnable.let { progressHandler?.removeCallbacks(it) }
    }

    fun release() {
        stopProgressUpdates()
        sleepTimerJob?.cancel()
        coroutineScope.cancel()
        
        // Clean release of MediaController connection
        mediaController?.let { controller ->
            controller.release()
            mediaController = null
        }
        
        try {
            val intent = Intent(applicationContext, PlayerService::class.java)
            applicationContext.stopService(intent)
        } catch (e: Exception) {
            Log.e("AudioPlaybackManager", "Error stopping PlayerService in release", e)
        }
    }
}
