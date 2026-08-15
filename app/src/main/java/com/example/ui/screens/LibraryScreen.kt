package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Surah
import com.example.ui.AppLanguage
import com.example.ui.LocalizedStrings
import com.example.ui.QuranViewModel
import com.example.ui.theme.IslamicGoldSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(viewModel: QuranViewModel) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val language by viewModel.language.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val surahList by viewModel.surahList.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()

    val isAr = language == AppLanguage.ARABIC

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dynamic search header panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        text = LocalizedStrings.get("search_hint", language),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.White
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color.White
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IslamicGoldSecondary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
        }

        // Surah list presentation
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp), // Space for persistent bottom player
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(surahList, key = { it.number }) { surah ->
                val isFav = favorites.any { it.number == surah.number }
                val isDownloaded = downloads.any { it.number == surah.number }
                val isDownloading = downloadProgress.containsKey(surah.number)
                val progress = downloadProgress[surah.number] ?: 0f

                SurahItemCard(
                    surah = surah,
                    isAr = isAr,
                    isFavorite = isFav,
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    downloadProgress = progress,
                    onPlay = { viewModel.playSurah(surah) },
                    onFavoriteToggle = { viewModel.toggleFavorite(surah.number) },
                    onDownloadToggle = {
                        if (isDownloaded) {
                            viewModel.deleteDownload(surah.number)
                        } else if (!isDownloading) {
                            viewModel.downloadSurah(context, surah)
                        }
                    },
                    onShare = {
                        shareSurah(context, surah, language)
                    }
                )
            }
        }
    }
}

@Composable
fun SurahItemCard(
    surah: Surah,
    isAr: Boolean,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onPlay: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownloadToggle: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onPlay() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Surah Badge Number
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.number.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Surah Name metadata
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isAr) surah.nameArabic else surah.nameEnglish,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Revelation badge
                        Box(
                            modifier = Modifier
                                .background(
                                    IslamicGoldSecondary.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            val revLabel = if (surah.revelationType == "Meccan") "revelation_meccan" else "revelation_medinan"
                            Text(
                                text = LocalizedStrings.get(revLabel, if (isAr) AppLanguage.ARABIC else AppLanguage.ENGLISH),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${surah.versesCount} ${if (isAr) "آية" else "Verses"} • ~${surah.typicalDurationMinutes} ${if (isAr) "دقيقة" else "min"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // If Language is Arabic, we can also display Arabic Calligraphy name on the right side if english is current tab!
                if (!isAr) {
                    Text(
                        text = surah.nameArabic,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )

            // Direct actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play Action Card Button
                TextButton(
                    onClick = onPlay,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = LocalizedStrings.get("play", if (isAr) AppLanguage.ARABIC else AppLanguage.ENGLISH),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Share Action Button
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Add to Favorites Button
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Download Action Button with progress indicator
                    IconButton(
                        onClick = onDownloadToggle,
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(
                                progress = downloadProgress,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = if (isDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                                contentDescription = "Download",
                                tint = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun shareSurah(context: Context, surah: Surah, language: AppLanguage) {
    val rawMsg = LocalizedStrings.get("share_message", language)
    val surahName = if (language == AppLanguage.ARABIC) surah.nameArabic else surah.nameEnglish
    val shareText = String.format(rawMsg, surahName) + 
        "\n\nhttps://server11.mp3quran.net/minsh/${String.format("%03d", surah.number)}.mp3"
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Surah"))
}
