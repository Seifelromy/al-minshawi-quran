package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow

class QuranRepository(private val quranDao: QuranDao) {

    // --- Favorites ---
    val favoriteEntities: Flow<List<FavoriteEntity>> = quranDao.getFavorites()

    fun isFavorite(surahId: Int): Flow<Boolean> = quranDao.isFavoriteFlow(surahId)

    suspend fun toggleFavorite(surahId: Int) {
        if (quranDao.isFavoriteDirect(surahId)) {
            quranDao.removeFavorite(surahId)
        } else {
            quranDao.addFavorite(FavoriteEntity(surahId = surahId))
        }
    }

    // --- Downloads ---
    val downloadEntities: Flow<List<DownloadEntity>> = quranDao.getDownloads()

    fun isDownloaded(surahId: Int): Flow<Boolean> = quranDao.isDownloadedFlow(surahId)

    suspend fun getDownloadPath(surahId: Int): String? {
        return quranDao.getDownload(surahId)?.localFilePath
    }

    suspend fun saveDownload(surahId: Int, localFilePath: String, fileSize: Long) {
        quranDao.addDownload(
            DownloadEntity(
                surahId = surahId,
                localFilePath = localFilePath,
                fileSize = fileSize
            )
        )
    }

    suspend fun deleteDownload(surahId: Int) {
        val download = quranDao.getDownload(surahId)
        if (download != null) {
            val file = java.io.File(download.localFilePath)
            if (file.exists()) {
                file.delete()
            }
            quranDao.removeDownload(surahId)
        }
    }

    // --- History & Metrics ---
    val playbackHistoryEntities: Flow<List<PlaybackHistoryEntity>> = quranDao.getPlaybackHistory()

    suspend fun recordPlayback(surahId: Int) {
        quranDao.incrementPlayCount(surahId)
    }

    // --- Continue Listening ---
    val continueListening: Flow<ContinueListeningEntity?> = quranDao.getContinueListeningFlow()

    suspend fun saveContinueListening(surahId: Int, positionMs: Long, durationMs: Long) {
        if (surahId in 1..114) {
            quranDao.saveContinueListening(
                ContinueListeningEntity(
                    surahId = surahId,
                    positionMs = positionMs,
                    durationMs = durationMs
                )
            )
        }
    }

    suspend fun getContinueListeningDirect(): ContinueListeningEntity? {
        return quranDao.getContinueListeningDirect()
    }

    suspend fun clearContinueListening() {
        quranDao.clearContinueListening()
    }
}
