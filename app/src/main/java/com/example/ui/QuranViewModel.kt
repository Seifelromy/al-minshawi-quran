package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioPlaybackManager
import com.example.data.audio.PlaybackState
import com.example.data.database.ContinueListeningEntity
import com.example.data.model.Surah
import com.example.data.repository.QuranRepository
import com.example.data.repository.AudioRepository
import com.example.data.repository.RemoteConfigService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

enum class AppLanguage {
    ARABIC, ENGLISH
}

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

class QuranViewModel(
    private val repository: QuranRepository,
    private val audioRepository: AudioRepository,
    val playbackManager: AudioPlaybackManager
) : ViewModel() {

    // --- Active Selected Reciter Stream ---
    private val _activeReciterId = MutableStateFlow("minshawi")
    val activeReciterId = _activeReciterId.asStateFlow()

    // --- Search & Library Streams ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 100% dynamic surahs flow straight from the Room dynamic cache!
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val rawSurahList: StateFlow<List<Surah>> = _activeReciterId
        .flatMapLatest { reciterId ->
            audioRepository.getSurahsFlow(reciterId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val surahList: StateFlow<List<Surah>> = _searchQuery
        .combine(rawSurahList) { query, list ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.nameEnglish.contains(query, ignoreCase = true) ||
                    it.nameArabic.contains(query, ignoreCase = true) ||
                    it.number.toString() == query
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Database Streams ---
    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val favorites: StateFlow<List<Surah>> = repository.favoriteEntities
        .combine(rawSurahList) { entities, surahs ->
            val map = surahs.associateBy { it.number }
            entities.mapNotNull { map[it.surahId] }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val downloads: StateFlow<List<Surah>> = repository.downloadEntities
        .combine(rawSurahList) { entities, surahs ->
            val map = surahs.associateBy { it.number }
            entities.mapNotNull { map[it.surahId] }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val recentlyPlayed: StateFlow<List<Surah>> = repository.playbackHistoryEntities
        .combine(rawSurahList) { entities, surahs ->
            val map = surahs.associateBy { it.number }
            entities.sortedByDescending { it.lastPlayedTimestamp }
                .mapNotNull { map[it.surahId] }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    val mostPlayed: StateFlow<List<Surah>> = repository.playbackHistoryEntities
        .combine(rawSurahList) { entities, surahs ->
            val map = surahs.associateBy { it.number }
            entities.sortedByDescending { it.playCount }
                .mapNotNull { map[it.surahId] }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val continueListening: StateFlow<ContinueListeningEntity?> = repository.continueListening
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Local Custom Setting States ---
    private val _language = MutableStateFlow(AppLanguage.ARABIC)
    val language = _language.asStateFlow()

    private val _theme = MutableStateFlow(AppTheme.SYSTEM)
    val theme = _theme.asStateFlow()

    // Download state Map: [SurahNumber -> progress float 0..1]
    private val _downloadProgress = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    // --- Active Audio Playback State ---
    val playbackState: StateFlow<PlaybackState> = playbackManager.state

    init {
        // Automatically listen to player position changes and periodically cache to continue-listening DB safely
        viewModelScope.launch {
            var lastRecordedSurahId: Int? = null
            var lastSavedTimeMs = 0L

            playbackManager.state.collect { state ->
                try {
                    val surah = state.currentSurah
                    if (surah != null) {
                        if (state.isPlaying) {
                            // Record history once upon playing a new Surah
                            if (lastRecordedSurahId != surah.number) {
                                lastRecordedSurahId = surah.number
                                repository.recordPlayback(surah.number)
                            }

                            // Periodically save progress to DB for resume caching (at most every 5 seconds)
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastSavedTimeMs >= 5000L) {
                                lastSavedTimeMs = currentTime
                                repository.saveContinueListening(
                                    surahId = surah.number,
                                    positionMs = state.progressMs,
                                    durationMs = state.durationMs
                                )
                            }
                        }
                    } else {
                        lastRecordedSurahId = null
                    }
                } catch (ce: Exception) {
                    android.util.Log.e("QuranViewModel", "VERBOSE ERROR: Exception in state collector inside QuranViewModel", ce)
                }
            }
        }
    }

    fun selectProvider(providerId: String) {
        viewModelScope.launch {
            _activeReciterId.value = providerId
            audioRepository.initializeReciter(providerId)
            
            // Sync with exoplayer's active state
            playbackManager.selectProvider(providerId)
            
            // If currently playing, reload queue
            val current = playbackState.value.currentSurah
            if (current != null && playbackState.value.isPlaying) {
                val updatedSurah = audioRepository.getSurahByNumber(providerId, current.number)
                if (updatedSurah != null) {
                    playbackManager.playSurah(updatedSurah, rawSurahList.value)
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playSurah(surah: Surah) {
        playbackManager.playSurah(surah, rawSurahList.value)
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun stop() {
        playbackManager.stop()
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun skipForward() {
        playbackManager.skipForward()
    }

    fun skipBackward() {
        playbackManager.skipBackward()
    }

    fun playNext() {
        playbackManager.playNext()
    }

    fun playPrevious() {
        playbackManager.playPrevious()
    }

    fun setPlaybackSpeed(speed: Float) {
        playbackManager.setPlaybackSpeed(speed)
    }

    fun cycleRepeatMode() {
        playbackManager.cycleRepeatMode()
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun setSleepTimer(minutes: Int) {
        playbackManager.setSleepTimer(minutes)
    }

    fun toggleFavorite(surahId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(surahId)
        }
    }

    fun changeLanguage(lang: AppLanguage) {
        _language.value = lang
    }

    fun changeTheme(newTheme: AppTheme) {
        _theme.value = newTheme
    }

    // --- File Download Engine (Local Offline caching) ---
    fun downloadSurah(context: Context, surah: Surah) {
        val surahId = surah.number
        if (_downloadProgress.value.contains(surahId)) return // Already in progress

        _downloadProgress.value = _downloadProgress.value + (surahId to 0.01f)
        val appContext = context.applicationContext

        viewModelScope.launch(Dispatchers.IO) {
            var success = false
            try {
                // Same fallback sources the player uses, so a dead primary host doesn't break downloads
                val candidates = playbackManager.getCandidateUrls(_activeReciterId.value, surahId, surah.audioUrl)
                    .filter { it.isNotBlank() }

                val dir = File(appContext.filesDir, "downloads")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val destFile = File(dir, String.format("%03d.mp3", surahId))
                val tempFile = File(dir, String.format("%03d.mp3.part", surahId))

                for (urlString in candidates) {
                    try {
                        downloadToFile(urlString, tempFile) { progress ->
                            withContext(Dispatchers.Main) {
                                _downloadProgress.value = _downloadProgress.value + (surahId to progress)
                            }
                        }
                        if (destFile.exists()) destFile.delete()
                        if (!tempFile.renameTo(destFile)) {
                            tempFile.copyTo(destFile, overwrite = true)
                            tempFile.delete()
                        }
                        repository.saveDownload(surahId, destFile.absolutePath, destFile.length())
                        success = true
                        break
                    } catch (e: Exception) {
                        android.util.Log.w("QuranViewModel", "Download failed from $urlString", e)
                        tempFile.delete()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                withContext(Dispatchers.Main) {
                    _downloadProgress.value = _downloadProgress.value - surahId
                    if (!success) {
                        val msg = if (_language.value == AppLanguage.ARABIC)
                            "تعذر تحميل السورة، تحقق من الاتصال بالإنترنت"
                        else
                            "Download failed, please check your connection"
                        android.widget.Toast.makeText(appContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private suspend fun downloadToFile(urlString: String, target: File, onProgress: suspend (Float) -> Unit) {
        var currentUrl = urlString
        var connection: HttpURLConnection
        var redirects = 0
        // Follow redirects manually (HttpURLConnection won't follow cross-protocol/host redirects reliably)
        while (true) {
            connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) QuranApp")
            }
            val code = connection.responseCode
            if (code in 300..399 && redirects < 5) {
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (location.isNullOrBlank()) throw java.io.IOException("Redirect without location")
                currentUrl = URL(URL(currentUrl), location).toString()
                redirects++
                continue
            }
            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw java.io.IOException("HTTP $code for $currentUrl")
            }
            break
        }

        try {
            val fileLength = connection.contentLengthLong
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var bytesWritten = 0L
                    var lastProgress = 0.01f
                    var lastUpdateTime = System.currentTimeMillis()
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        bytesWritten += bytesRead
                        if (fileLength > 0) {
                            val progress = bytesWritten.toFloat() / fileLength.toFloat()
                            val now = System.currentTimeMillis()
                            if (progress - lastProgress >= 0.01f || now - lastUpdateTime >= 100L) {
                                lastProgress = progress
                                lastUpdateTime = now
                                onProgress(progress)
                            }
                        }
                    }
                    if (fileLength > 0 && bytesWritten < fileLength) {
                        throw java.io.IOException("Incomplete download: $bytesWritten/$fileLength")
                    }
                    if (bytesWritten == 0L) throw java.io.IOException("Empty response")
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    fun deleteDownload(surahId: Int) {
        viewModelScope.launch {
            repository.deleteDownload(surahId)
        }
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun resumeSession() {
        viewModelScope.launch {
            val session = repository.getContinueListeningDirect()
            if (session != null) {
                val surah = audioRepository.getSurahByNumber(_activeReciterId.value, session.surahId)
                if (surah != null) {
                    playbackManager.playSurah(surah, rawSurahList.value, session.positionMs)
                }
            }
        }
    }

    fun clearAllDownloads(context: Context) {
        viewModelScope.launch {
            val list = downloads.value
            for (surah in list) {
                repository.deleteDownload(surah.number)
            }
            // Clear files dir
            try {
                val dir = File(context.filesDir, "downloads")
                if (dir.exists()) {
                    dir.deleteRecursively()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Factory(
        private val repository: QuranRepository,
        private val audioRepository: AudioRepository,
        private val playbackManager: AudioPlaybackManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuranViewModel(repository, audioRepository, playbackManager) as T
        }
    }
}
