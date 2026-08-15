package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.QuranDao
import com.example.data.model.Surah
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AudioRepository(
    private val context: Context,
    private val quranDao: QuranDao,
    private val remoteConfigService: RemoteConfigService
) {
    private val tag = "AudioRepository"

    companion object {
        const val REFRESH_INTERVAL_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Initializes the specified reciter configuration by loading Room cache,
     * seeding from assets/templates if empty, and checking 24-hours Retrofit refresh window.
     */
    suspend fun initializeReciter(reciterId: String, forceRefresh: Boolean = false) {
        Log.d(tag, "VERBOSE: initializeReciter requested for reciterId: $reciterId, forceRefresh: $forceRefresh")
        try {
            val cachedReciter = quranDao.getCachedReciter(reciterId)
            val surahs = quranDao.getSurahsForReciterDirect(reciterId)
            Log.d(tag, "VERBOSE: cachedReciter: $cachedReciter, surahs count: ${surahs.size}")

            // Seed local/fallback asset configuration if completely missing
            if (cachedReciter == null || surahs.isEmpty()) {
                Log.d(tag, "VERBOSE: Cache miss for reciter: $reciterId. Seeding local fallback asset config...")
                remoteConfigService.seedLocalAssetFallback(reciterId)
            }

            // Check if 24 hours have elapsed
            val lastUpdated = quranDao.getCachedReciter(reciterId)?.lastUpdated ?: 0L
            val timeSinceUpdate = System.currentTimeMillis() - lastUpdated
            Log.d(tag, "VERBOSE: lastUpdated time: $lastUpdated, timeSinceUpdate: $timeSinceUpdate ms")

            if (forceRefresh || cachedReciter == null || timeSinceUpdate > REFRESH_INTERVAL_MS) {
                Log.d(tag, "VERBOSE: Config expired or force refresh. Checking remote Retrofit update for: $reciterId")
                val reciterInfo = RemoteConfigService.KNOWN_RECITERS.find { it.id == reciterId }
                if (reciterInfo != null) {
                    val success = remoteConfigService.downloadAndCacheConfig(reciterId, reciterInfo.configUrl)
                    if (success) {
                        Log.d(tag, "VERBOSE: Dynamic remote config parsed and saved successfully to Room SQLite Cache for $reciterId")
                    } else {
                        Log.w(tag, "VERBOSE: Network refresh failed. Reverting to existing Room database cached copy for $reciterId")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "VERBOSE ERROR: Error during initializeReciter for $reciterId", e)
        }
    }

    /**
     * Retrieves Flow of Surahs for a given reciter from SQL Room Cache.
     */
    fun getSurahsFlow(reciterId: String): Flow<List<Surah>> {
        return quranDao.getSurahsForReciterFlow(reciterId).map { entities ->
            entities.map { entity ->
                Surah(
                    number = entity.number,
                    nameArabic = entity.nameArabic,
                    nameEnglish = entity.nameEnglish,
                    revelationType = entity.revelationType,
                    versesCount = entity.versesCount,
                    typicalDurationMinutes = entity.typicalDurationMinutes,
                    audioUrl = entity.audioUrl
                )
            }
        }
    }

    suspend fun getSurahsDirect(reciterId: String): List<Surah> {
        return quranDao.getSurahsForReciterDirect(reciterId).map { entity ->
            Surah(
                number = entity.number,
                nameArabic = entity.nameArabic,
                nameEnglish = entity.nameEnglish,
                revelationType = entity.revelationType,
                versesCount = entity.versesCount,
                typicalDurationMinutes = entity.typicalDurationMinutes,
                audioUrl = entity.audioUrl
            )
        }
    }

    suspend fun getSurahByNumber(reciterId: String, number: Int): Surah? {
        val entity = quranDao.getSurahByNumber(reciterId, number) ?: return null
        return Surah(
            number = entity.number,
            nameArabic = entity.nameArabic,
            nameEnglish = entity.nameEnglish,
            revelationType = entity.revelationType,
            versesCount = entity.versesCount,
            typicalDurationMinutes = entity.typicalDurationMinutes,
            audioUrl = entity.audioUrl
        )
    }

    suspend fun getAudioUrl(reciterId: String, surahId: Int): String {
        return quranDao.getSurahByNumber(reciterId, surahId)?.audioUrl ?: ""
    }
}
