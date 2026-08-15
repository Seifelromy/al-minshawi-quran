package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.AppLanguage
import com.example.ui.LocalizedStrings
import com.example.ui.QuranViewModel

enum class NavigationTab {
    Home, Library, Downloads, Favorites, Settings
}

@Composable
fun MainScreen(viewModel: QuranViewModel) {
    var activeTab by remember { mutableStateOf(NavigationTab.Home) }
    val language by viewModel.language.collectAsState()

    Scaffold(
        bottomBar = {
            Column {
                // Persistent Player bar floats right above Bottom Nav Bar if any surah is active
                PlayerCompanion(viewModel = viewModel)

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    // 1. HOME TAB
                    NavigationBarItem(
                        selected = activeTab == NavigationTab.Home,
                        onClick = { activeTab = NavigationTab.Home },
                        label = { Text(text = LocalizedStrings.get("home", language)) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.Home) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        }
                    )

                    // 2. LIBRARY TAB
                    NavigationBarItem(
                        selected = activeTab == NavigationTab.Library,
                        onClick = { activeTab = NavigationTab.Library },
                        label = { Text(text = LocalizedStrings.get("surahs", language)) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.Library) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                contentDescription = "Library"
                            )
                        }
                    )

                    // 3. DOWNLOADS TAB
                    NavigationBarItem(
                        selected = activeTab == NavigationTab.Downloads,
                        onClick = { activeTab = NavigationTab.Downloads },
                        label = { Text(text = LocalizedStrings.get("downloads", language)) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.Downloads) Icons.Filled.CloudDownload else Icons.Outlined.CloudDownload,
                                contentDescription = "Downloads"
                            )
                        }
                    )

                    // 4. FAVORITES TAB
                    NavigationBarItem(
                        selected = activeTab == NavigationTab.Favorites,
                        onClick = { activeTab = NavigationTab.Favorites },
                        label = { Text(text = LocalizedStrings.get("favorites", language)) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.Favorites) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Favorites"
                            )
                        }
                    )

                    // 5. SETTINGS TAB
                    NavigationBarItem(
                        selected = activeTab == NavigationTab.Settings,
                        onClick = { activeTab = NavigationTab.Settings },
                        label = { Text(text = LocalizedStrings.get("settings", language)) },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == NavigationTab.Settings) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .statusBarsPadding()
        ) {
            when (activeTab) {
                NavigationTab.Home -> HomeScreen(viewModel = viewModel) { targetIndex ->
                    // Navigate callback
                    activeTab = when (targetIndex) {
                        1 -> NavigationTab.Library
                        2 -> NavigationTab.Downloads
                        else -> NavigationTab.Home
                    }
                }
                NavigationTab.Library -> LibraryScreen(viewModel = viewModel)
                NavigationTab.Downloads -> DownloadsScreen(viewModel = viewModel)
                NavigationTab.Favorites -> FavoritesScreen(viewModel = viewModel)
                NavigationTab.Settings -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
