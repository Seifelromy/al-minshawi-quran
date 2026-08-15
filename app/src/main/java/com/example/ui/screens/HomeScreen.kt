package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Surah
import com.example.ui.AppLanguage
import com.example.ui.LocalizedStrings
import com.example.ui.QuranViewModel
import com.example.ui.theme.IslamicGoldSecondary
import com.example.ui.theme.IslamicGreenPrimary

@Composable
fun HomeScreen(
    viewModel: QuranViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val language by viewModel.language.collectAsState()
    val isAr = language == AppLanguage.ARABIC

    val recentList by viewModel.recentlyPlayed.collectAsState()
    val mostList by viewModel.mostPlayed.collectAsState()
    val favList by viewModel.favorites.collectAsState()
    val downloadList by viewModel.downloads.collectAsState()
    val contListening by viewModel.continueListening.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 120.dp) // Space for player height
    ) {
        // Grand Islamic Banner Hero
        item {
            MinshawiHeroBanner(language)
        }

        // Section: Continue Listening
        if (contListening != null) {
            val lastSurah = Surah.ALL_SURAHS.find { it.number == contListening!!.surahId }
            if (lastSurah != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = LocalizedStrings.get("continue_listening", language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.resumeSession() },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${LocalizedStrings.get("resume_surah", language)}${if (isAr) lastSurah.nameArabic else lastSurah.nameEnglish}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val progressPct = if (contListening!!.durationMs > 0) {
                                        (contListening!!.positionMs * 100) / contListening!!.durationMs
                                    } else 0
                                    
                                    val currentSec = contListening!!.positionMs / 1000
                                    val min = currentSec / 60
                                    val sec = currentSec % 60
                                    
                                    Text(
                                        text = String.format("%02d:%02d (%d%%)", min, sec, progressPct),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Recently Played
        item {
            SurahHorizontalSection(
                title = LocalizedStrings.get("recently_played", language),
                emptyMessage = LocalizedStrings.get("empty_recent", language),
                items = recentList.take(6),
                isAr = isAr,
                onItemClick = { viewModel.playSurah(it) }
            )
        }

        // Section: Most Played
        item {
            SurahHorizontalSection(
                title = LocalizedStrings.get("most_played", language),
                emptyMessage = LocalizedStrings.get("empty_recent", language),
                items = mostList.take(6),
                isAr = isAr,
                onItemClick = { viewModel.playSurah(it) }
            )
        }

        // Section: Favorite Surahs
        item {
            SurahHorizontalSection(
                title = LocalizedStrings.get("favorite_surahs", language),
                emptyMessage = LocalizedStrings.get("empty_favorites", language),
                items = favList.take(6),
                isAr = isAr,
                onItemClick = { viewModel.playSurah(it) }
            )
        }

        // Quick Navigation Helpers
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = LocalizedStrings.get("cache_management", language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedButton(
                        onClick = { onNavigateToTab(2) }, // Downloads Tab
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${LocalizedStrings.get("downloads", language)} (${downloadList.size})",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavigateToTab(1) }, // Library Tab
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Outlined.LibraryMusic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LocalizedStrings.get("surahs", language),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MinshawiHeroBanner(language: AppLanguage) {
    val isAr = language == AppLanguage.ARABIC

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        IslamicGreenPrimary,
                        Color(0xFF074732)
                    )
                )
            )
            .border(
                1.5.dp, 
                Brush.horizontalGradient(listOf(IslamicGoldSecondary, Color.Transparent)), 
                RoundedCornerShape(24.dp)
            )
    ) {
        // Decorative Islamic Arch Drawing behind text using Compose Canvas draw
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // Golden lines for arches
                    val width = size.width
                    val height = size.height
                    val archPath = androidx.compose.ui.graphics.Path().apply {
                        moveTo(width * 0.7f, height)
                        cubicTo(width * 0.82f, height * 0.5f, width * 0.9f, height * 0.2f, width, height * 0.1f)
                        moveTo(width * 0.6f, height)
                        cubicTo(width * 0.75f, height * 0.6f, width * 0.85f, height * 0.3f, width, height * 0.2f)
                    }
                    drawPath(
                        path = archPath,
                        color = IslamicGoldSecondary.copy(alpha = 0.25f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
        )

        // Contents
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = if (isAr) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .background(IslamicGoldSecondary, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "QURAN RECITER",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF101820),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = LocalizedStrings.get("reciter_title", language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = LocalizedStrings.get("reciter_desc", language),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = if (isAr) TextAlign.End else TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        }
    }
}

@Composable
fun SurahHorizontalSection(
    title: String,
    emptyMessage: String,
    items: List<Surah>,
    isAr: Boolean,
    onItemClick: (Surah) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { surah ->
                    Card(
                        modifier = Modifier
                            .width(145.dp)
                            .clickable { onItemClick(surah) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = surah.nameArabic,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = surah.nameEnglish,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
