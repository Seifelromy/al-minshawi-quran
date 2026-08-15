package com.example.data.database

import androidx.room.*

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val surahId: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val surahId: Int,
    val localFilePath: String,
    val fileSize: Long,
    val downloadedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey val surahId: Int,
    val lastPlayedTimestamp: Long,
    val playCount: Int
)

@Entity(tableName = "continue_listening")
data class ContinueListeningEntity(
    @PrimaryKey val id: Int = 1, // Anchor single-row tracker
    val surahId: Int,
    val positionMs: Long,
    val durationMs: Long,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_reciters")
data class CachedReciterEntity(
    @PrimaryKey val id: String,
    val name: String,
    val configUrl: String,
    val version: Int,
    val lastUpdated: Long
)

@Entity(tableName = "cached_surahs", primaryKeys = ["reciterId", "number"])
data class CachedSurahEntity(
    val reciterId: String,
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val revelationType: String,
    val versesCount: Int,
    val typicalDurationMinutes: Int,
    val audioUrl: String
)

