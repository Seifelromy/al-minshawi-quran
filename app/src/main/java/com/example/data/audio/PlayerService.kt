package com.example.data.audio

import android.content.Intent
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaNotification
import androidx.media3.session.DefaultMediaNotificationProvider
import com.example.QuranApplication

class PlayerService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        android.util.Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Service Created")
        
        try {
            android.util.Log.d("PlayerService", "Initializing ExoPlayer and MediaSession within service")
            
            // Build the ExoPlayer instance cleanly on the service lifecycle thread
            val player = ExoPlayer.Builder(this)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true // Automatic AudioFocus logic
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build()
                
            exoPlayer = player

            // Build the MediaSession
            val session = MediaSession.Builder(this, player)
                .setId("AlMinshawiQuranPlaybackSession")
                .build()
            mediaSession = session

            // Add the session to the service's session pool
            addSession(session)

            // Register custom notification provider using our dedicated channel ID
            val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
                .setChannelId("quran_playback_channel")
                .build()
            setMediaNotificationProvider(notificationProvider)
            
            android.util.Log.d("PlayerService", "[DIAGNOSTICS] PlayerService initialized with ExoPlayer ($player) and MediaSession ($session)")
        } catch (e: Exception) {
            android.util.Log.e("PlayerService", "FATAL: Failed to build ExoPlayer inside PlayerService onCreate", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Service Started")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        android.util.Log.d("PlayerService", "onGetSession called with controller: ${controllerInfo.packageName}. Returning: $mediaSession")
        return mediaSession
    }

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        android.util.Log.i("DIAGNOSTICS", "[DIAGNOSTICS] onUpdateNotification incoming startingInForeground: $startInForegroundRequired")
        if (startInForegroundRequired) {
            android.util.Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Service Promoted To Foreground")
        }
        android.util.Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Notification Attached")
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        android.util.Log.d("PlayerService", "onTaskRemoved called")
        val app = application as? QuranApplication
        val isPlaying = exoPlayer?.isPlaying ?: false
        android.util.Log.d("PlayerService", "onTaskRemoved. playing status = $isPlaying")
        
        if (!isPlaying) {
            android.util.Log.i("PlayerService", "Not playing; stopping managers and self.")
            try {
                app?.playbackManager?.stop()
            } catch (e: Exception) {
                android.util.Log.e("PlayerService", "Error during app managers stop", e)
            }
            try {
                super.onTaskRemoved(rootIntent)
            } catch (e: Exception) {
                android.util.Log.e("PlayerService", "Error in super.onTaskRemoved", e)
            }
            stopSelf()
        } else {
            android.util.Log.i("PlayerService", "Currently playing; surviving task swipe!")
            try {
                super.onTaskRemoved(rootIntent)
            } catch (e: Exception) {
                android.util.Log.e("PlayerService", "Error in super.onTaskRemoved during active playback", e)
            }
        }
    }

    override fun onDestroy() {
        android.util.Log.d("PlayerService", "onDestroy called")
        try {
            mediaSession?.let { session ->
                session.player.release()
                session.release()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerService", "Error releasing media products", e)
        }
        exoPlayer = null
        mediaSession = null
        super.onDestroy()
    }
}
