package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.data.model.Surah
import com.example.ui.AppLanguage
import com.example.ui.LocalizedStrings
import com.example.ui.QuranViewModel
import com.example.ui.theme.IslamicGoldSecondary
import com.example.ui.theme.IslamicGreenPrimary
import com.example.data.repository.RemoteConfigService
import androidx.compose.ui.platform.testTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerCompanion(
    viewModel: QuranViewModel,
    modifier: Modifier = Modifier
) {
    val language by viewModel.language.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val playbackState by viewModel.playbackState.collectAsState()
    val surah = playbackState.currentSurah

    var isExpanded by remember { mutableStateOf(false) }

    val activeReciterId by viewModel.activeReciterId.collectAsState()
    val reciterName = remember(activeReciterId) {
        RemoteConfigService.KNOWN_RECITERS.find { it.id == activeReciterId }?.displayName
            ?: "Sheikh Muhammad Siddiq Al-Minshawi"
    }

    if (surah == null) return

    Box(modifier = modifier.fillMaxWidth()) {
        // 1. Persistent Mini Player (visible as long as playing/paused context is set)
        AnimatedVisibility(
            visible = !isExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { isExpanded = true }
                    .testTag("mini_player_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge logo
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(IslamicGoldSecondary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAr) surah.nameArabic else surah.nameEnglish,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = reciterName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                maxLines = 1
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Forward 15s
                            IconButton(onClick = { viewModel.skipBackward() }) {
                                Icon(
                                    imageVector = Icons.Default.Replay10,
                                    contentDescription = "Backward 15s",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            // Play button
                            IconButton(
                                onClick = {
                                    android.util.Log.d("MINI_PLAYER_VERBOSE", "Play/Pause Button Clicked on Mini Player")
                                    viewModel.togglePlayPause()
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("mini_player_play_button")
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            // Close button
                            IconButton(
                                onClick = {
                                    android.util.Log.d("MINI_PLAYER_VERBOSE", "Close (X) Button Clicked on Mini Player")
                                    viewModel.stop()
                                },
                                modifier = Modifier.testTag("mini_player_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close player",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Progress bar below mini-player
                    val progressRatio = if (playbackState.durationMs > 0) {
                        playbackState.progressMs.toFloat() / playbackState.durationMs.toFloat()
                    } else 0f
                    LinearProgressIndicator(
                        progress = progressRatio.coerceIn(0f, 1f),
                        modifier = Modifier.fillMaxWidth().height(3.5.dp),
                        color = IslamicGoldSecondary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    )
                }
            }
        }

        // 2. Fullscreen grand playback overlay sheet
        AnimatedVisibility(
            visible = isExpanded,
            enter = slideInVertically(animationSpec = tween(350), initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(animationSpec = tween(350), targetOffsetY = { it }) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent),
                color = MaterialTheme.colorScheme.background
            ) {
                // Main container with deep gradient backdrop mimicking an elegant Islamic mihrab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .drawBehind {
                            val brush = Brush.verticalGradient(
                                colors = listOf(
                                    IslamicGreenPrimary.copy(alpha = 0.12f),
                                    Color.Transparent
                                )
                            )
                            drawRect(brush)
                        }
                        .padding(horizontal = 24.dp)
                        .padding(top = 16.dp, bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header close button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isExpanded = false }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimize Player",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = LocalizedStrings.get("app_title", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )

                        IconButton(onClick = { viewModel.toggleFavorite(surah.number) }) {
                            val favorites by viewModel.favorites.collectAsState()
                            val isFavorite = favorites.any { it.number == surah.number }
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Add to favorites",
                                tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Artwork display: Gold calligraphic shield framed nicely
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        IslamicGreenPrimary,
                                        Color(0xFF032217)
                                    )
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Islamic decorative sign (surah number inside octahedron crown)
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .background(IslamicGoldSecondary, RoundedCornerShape(18.dp))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = surah.number.toString(),
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF101820)
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = surah.nameArabic,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IslamicGoldSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Title & Description metadatas
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isAr) surah.nameArabic else surah.nameEnglish,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = reciterName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Seek controls
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Slider progress bar
                        var sliderPosition by remember { mutableStateOf<Float?>(null) }
                        val currentPosition = playbackState.progressMs.toFloat()
                        val duration = playbackState.durationMs.toFloat()

                        Slider(
                            value = sliderPosition ?: if (duration > 0) currentPosition else 0f,
                            onValueChange = { sliderPosition = it },
                            onValueChangeFinished = {
                                sliderPosition?.let { viewModel.seekTo(it.toLong()) }
                                sliderPosition = null
                            },
                            valueRange = 0f..duration.coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                activeTrackColor = IslamicGoldSecondary,
                                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                thumbColor = IslamicGoldSecondary
                            )
                        )

                        // Duration text metrics
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val activePosSec = (sliderPosition?.toLong() ?: playbackState.progressMs) / 1000
                            val activeMin = activePosSec / 60
                            val activeSec = activePosSec % 60

                            val totalSec = playbackState.durationMs / 1000
                            val totalMin = totalSec / 60
                            val totalSecRem = totalSec % 60

                            Text(
                                text = String.format("%02d:%02d", activeMin, activeSec),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = String.format("%02d:%02d", totalMin, totalSecRem),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Row actions player: Shuffle, Prev, Rewind 15s, Play/Pause, FastFwd 15s, Next, Repeat
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Shuffle control
                            IconButton(onClick = { viewModel.toggleShuffle() }) {
                                Icon(
                                    imageVector = if (playbackState.isShuffleEnabled) Icons.Default.Shuffle else Icons.Outlined.Shuffle,
                                    contentDescription = "Shuffle",
                                    tint = if (playbackState.isShuffleEnabled) IslamicGoldSecondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Back Surah
                            IconButton(onClick = { viewModel.playPrevious() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous Surah",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // 15s Rewind
                            IconButton(onClick = { viewModel.skipBackward() }) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Backward 15s",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // PLAY / PAUSE
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier
                                    .size(72.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // 15s Forward
                            IconButton(onClick = { viewModel.skipForward() }) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Forward 15s",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Next Surah
                            IconButton(onClick = { viewModel.playNext() }) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Surah",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Repeat cycle
                            IconButton(onClick = { viewModel.cycleRepeatMode() }) {
                                val repeatIcon = when (playbackState.repeatMode) {
                                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                                    Player.REPEAT_MODE_ALL -> Icons.Default.Repeat
                                    else -> Icons.Outlined.Repeat
                                }
                                val repeatColor = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) {
                                    IslamicGoldSecondary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                                Icon(
                                    imageVector = repeatIcon,
                                    contentDescription = "Repeat",
                                    tint = repeatColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Informational row showing current speed and active sleep timer status
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${playbackState.playbackSpeed}x Speed",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (playbackState.sleepTimerMinutesLeft > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${playbackState.sleepTimerMinutesLeft}m Close Timer",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
