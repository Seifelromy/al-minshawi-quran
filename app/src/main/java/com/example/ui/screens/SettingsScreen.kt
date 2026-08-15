package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.RemoteConfigService
import com.example.ui.AppLanguage
import com.example.ui.AppTheme
import com.example.ui.LocalizedStrings
import com.example.ui.QuranViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: QuranViewModel) {
    val context = LocalContext.current

    val language by viewModel.language.collectAsState()
    val activeTheme by viewModel.theme.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val playbackState by viewModel.playbackState.collectAsState()
    val activeProviderId = playbackState.activeProviderId
    val currentSpeed = playbackState.playbackSpeed
    val secondsLeft = playbackState.sleepTimerMinutesLeft

    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var isSpeedDropdownExpanded by remember { mutableStateOf(false) }
    var isTimerDropdownExpanded by remember { mutableStateOf(false) }

    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    val sleepMinutesOpts = listOf(0, 5, 10, 15, 30, 45, 60, 90)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp), // Height space for float player
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Application Banner Settings
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = if (isAr) Alignment.End else Alignment.Start) {
                    Text(
                        text = LocalizedStrings.get("app_title", language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Version 1.0.0 (Premium Plus Client)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Language block selector
        item {
            SettingsSectionHeader(title = LocalizedStrings.get("language_option", language), icon = Icons.Default.Translate)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.changeLanguage(AppLanguage.ARABIC) },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (language == AppLanguage.ARABIC) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "العربية",
                            fontWeight = FontWeight.Bold,
                            color = if (language == AppLanguage.ARABIC) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { viewModel.changeLanguage(AppLanguage.ENGLISH) },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (language == AppLanguage.ENGLISH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "English",
                            fontWeight = FontWeight.Bold,
                            color = if (language == AppLanguage.ENGLISH) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Theme selection card row
        item {
            SettingsSectionHeader(title = LocalizedStrings.get("theme_option", language), icon = Icons.Default.Palette)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    AppTheme.LIGHT to LocalizedStrings.get("theme_light", language),
                    AppTheme.DARK to LocalizedStrings.get("theme_dark", language),
                    AppTheme.SYSTEM to LocalizedStrings.get("theme_system", language)
                ).forEach { (themeKey, name) ->
                    val isSelected = activeTheme == themeKey
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.changeTheme(themeKey) },
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Audio Provider Switching
        item {
            SettingsSectionHeader(title = LocalizedStrings.get("audio_provider", language), icon = Icons.Default.CloudQueue)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val currentProvider = RemoteConfigService.KNOWN_RECITERS.find { it.id == activeProviderId } ?: RemoteConfigService.KNOWN_RECITERS.first()
                ExposedDropdownMenuBox(
                    expanded = isProviderDropdownExpanded,
                    onExpandedChange = { isProviderDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentProvider.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProviderDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = isProviderDropdownExpanded,
                        onDismissRequest = { isProviderDropdownExpanded = false }
                    ) {
                        RemoteConfigService.KNOWN_RECITERS.forEach { provider ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = provider.displayName,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    viewModel.selectProvider(provider.id)
                                    isProviderDropdownExpanded = false
                                    Toast.makeText(context, "${provider.displayName} Selected", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Playback speed Selector
        item {
            SettingsSectionHeader(title = LocalizedStrings.get("playback_speed", language), icon = Icons.Default.Speed)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = isSpeedDropdownExpanded,
                    onExpandedChange = { isSpeedDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "${currentSpeed}x",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isSpeedDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = isSpeedDropdownExpanded,
                        onDismissRequest = { isSpeedDropdownExpanded = false }
                    ) {
                        speeds.forEach { speed ->
                            DropdownMenuItem(
                                text = { Text(text = "${speed}x", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.setPlaybackSpeed(speed)
                                    isSpeedDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Sleep Timer Option
        item {
            SettingsSectionHeader(title = LocalizedStrings.get("sleep_timer", language), icon = Icons.Default.Timer)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                val activeTimerLabel = if (secondsLeft > 0) {
                    "$secondsLeft ${LocalizedStrings.get("sleep_timer_val", language)}"
                } else {
                    LocalizedStrings.get("sleep_timer_inactive", language)
                }

                ExposedDropdownMenuBox(
                    expanded = isTimerDropdownExpanded,
                    onExpandedChange = { isTimerDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = activeTimerLabel,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTimerDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = isTimerDropdownExpanded,
                        onDismissRequest = { isTimerDropdownExpanded = false }
                    ) {
                        sleepMinutesOpts.forEach { min ->
                            val textName = if (min == 0) {
                                LocalizedStrings.get("sleep_timer_inactive", language)
                            } else {
                                "$min ${LocalizedStrings.get("sleep_timer_val", language)}"
                            }
                            DropdownMenuItem(
                                text = { Text(text = textName, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    viewModel.setSleepTimer(min)
                                    isTimerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Cache Management action button
        item {
            SettingsSectionHeader(title = LocalizedStrings.get("cache_management", language), icon = Icons.Default.SdCardAlert)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.clearAllDownloads(context)
                        Toast.makeText(
                            context,
                            LocalizedStrings.get("cache_cleared", language),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = LocalizedStrings.get("clear_cache", language),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
