package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.example.data.audio.AudioPlaybackManager
import com.example.data.database.QuranDatabase
import com.example.data.repository.QuranRepository
import com.example.data.repository.AudioRepository
import com.example.data.repository.RemoteConfigService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuranApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: QuranDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            QuranDatabase::class.java,
            "al_minshawi_quran_database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    val repository: QuranRepository by lazy {
        QuranRepository(database.quranDao())
    }

    val remoteConfigService: RemoteConfigService by lazy {
        RemoteConfigService(this, database.quranDao())
    }

    val audioRepository: AudioRepository by lazy {
        AudioRepository(this, database.quranDao(), remoteConfigService)
    }

    val playbackManager: AudioPlaybackManager by lazy {
        AudioPlaybackManager(this) { surahId ->
            repository.getDownloadPath(surahId)
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Create dedicated notification channel for media playback
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "quran_playback_channel"
            val name = "Quran Playback"
            val descriptionText = "Notifications for Quran audio playback controls"
            val importance = android.app.NotificationManager.IMPORTANCE_LOW
            val channel = android.app.NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                setSound(null, null)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        // Setup global uncaught exception handler to prevent app crashes and log complete details
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            android.util.Log.e(
                "MINI_PLAYER_CRASH_INVESTIGATION_UNCAUGHT",
                "FATAL UNCAUGHT EXCEPTION ON THREAD: ${thread.name}",
                throwable
            )
            val stackTraceString = android.util.Log.getStackTraceString(throwable)
            android.util.Log.e("MINI_PLAYER_CRASH_INVESTIGATION_UNCAUGHT", "Exception Type: ${throwable.javaClass.canonicalName ?: throwable.javaClass.name}")
            android.util.Log.e("MINI_PLAYER_CRASH_INVESTIGATION_UNCAUGHT", "Message: ${throwable.message}")
            android.util.Log.e("MINI_PLAYER_CRASH_INVESTIGATION_UNCAUGHT", "Stack Trace:\n$stackTraceString")
            
            // Delegate cleanly to Android's default handler to crash normally and prevent main thread hanging / ANRs
            defaultHandler?.uncaughtException(thread, throwable)
        }
        
        // 1. Instantly initialize/update primary reciter config in database cache asynchronously
        applicationScope.launch {
            audioRepository.initializeReciter("minshawi")
        }

        // Proactive initialization of playback components asynchronously on the main loop
        applicationScope.launch {
            withContext(Dispatchers.Main) {
                // Fetch lazy property safely on the main thread (but asynchronously to prevent onCreate blocking)
                val pm = playbackManager
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        playbackManager.release()
    }
}
