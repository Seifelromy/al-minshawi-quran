package com.example

import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppTheme
import com.example.ui.QuranViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Supports full-bleed immersive status bar / notch safe areas
        enableEdgeToEdge()

        // Asynchronously request POST_NOTIFICATIONS on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to request POST_NOTIFICATIONS permission", e)
            }
        }

        setContent {
            val app = LocalContext.current.applicationContext as QuranApplication
            
            val viewModel: QuranViewModel = viewModel(
                factory = QuranViewModel.Factory(app.repository, app.audioRepository, app.playbackManager)
            )

            val themeState by viewModel.theme.collectAsState()
            val darkTheme = when (themeState) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        android.util.Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Playback Restored")
    }

    override fun onStop() {
        super.onStop()
        android.util.Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Playback Backgrounded")
        try {
            val app = applicationContext as? QuranApplication
            val isPlaying = app?.playbackManager?.state?.value?.isPlaying ?: false
            if (isPlaying) {
                // Post delay of 5 seconds to verify it continues to survive backgrounding cleanly
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val isStillPlaying = app?.playbackManager?.state?.value?.isPlaying ?: false
                    if (isStillPlaying) {
                        android.util.Log.i("DIAGNOSTICS", "[DIAGNOSTICS] Playback Survived Background")
                    } else {
                        android.util.Log.w("DIAGNOSTICS", "[DIAGNOSTICS] Playback stopped in background after onStop")
                    }
                }, 5000)
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Exception checking playback survival in background", e)
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
