package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {

    // --- Favorites ---
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE surahId = :surahId)")
    fun isFavoriteFlow(surahId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE surahId = :surahId)")
    suspend fun isFavoriteDirect(surahId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE surahId = :surahId")
    suspend fun removeFavorite(surahId: Int)

    // --- Downloads ---
    @Query("SELECT * FROM downloads ORDER BY downloadedAt DESC")
    fun getDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE surahId = :surahId)")
    fun isDownloadedFlow(surahId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE surahId = :surahId)")
    suspend fun isDownloadedDirect(surahId: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addDownload(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE surahId = :surahId")
    suspend fun removeDownload(surahId: Int)

    @Query("SELECT * FROM downloads WHERE surahId = :surahId")
    suspend fun getDownload(surahId: Int): DownloadEntity?

    // --- Playback History ---
    @Query("SELECT * FROM playback_history ORDER BY lastPlayedTimestamp DESC")
    fun getPlaybackHistory(): Flow<List<PlaybackHistoryEntity>>

    @Query("SELECT * FROM playback_history WHERE surahId = :surahId")
    suspend fun getHistoryItem(surahId: Int): PlaybackHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveHistoryItem(item: PlaybackHistoryEntity)

    // Helper transaction to track custom play counts
    @Transaction
    suspend fun incrementPlayCount(surahId: Int) {
        val existing = getHistoryItem(surahId)
        if (existing != null) {
            saveHistoryItem(existing.copy(
                lastPlayedTimestamp = System.currentTimeMillis(),
                playCount = existing.playCount + 1
            ))
        } else {
            saveHistoryItem(PlaybackHistoryEntity(
                surahId = surahId,
                lastPlayedTimestamp = System.currentTimeMillis(),
                playCount = 1
            ))
        }
    }

    // --- Continue Listening ---
    @Query("SELECT * FROM continue_listening WHERE id = 1")
    fun getContinueListeningFlow(): Flow<ContinueListeningEntity?>

    @Query("SELECT * FROM continue_listening WHERE id = 1")
    suspend fun getContinueListeningDirect(): ContinueListeningEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveContinueListening(session: ContinueListeningEntity)

    @Query("DELETE FROM continue_listening WHERE id = 1")
    suspend fun clearContinueListening()

    // --- Caching Configuration Tables ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReciter(reciter: CachedReciterEntity)

    @Query("SELECT * FROM cached_reciters WHERE id = :id")
    suspend fun getCachedReciter(id: String): CachedReciterEntity?

    @Query("SELECT * FROM cached_reciters")
    suspend fun getAllCachedRecitersDirect(): List<CachedReciterEntity>

    @Query("SELECT * FROM cached_reciters")
    fun getAllCachedRecitersFlow(): Flow<List<CachedReciterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<CachedSurahEntity>)

    @Query("DELETE FROM cached_surahs WHERE reciterId = :reciterId")
    suspend fun clearSurahsForReciter(reciterId: String)

    @Query("SELECT * FROM cached_surahs WHERE reciterId = :reciterId ORDER BY number ASC")
    fun getSurahsForReciterFlow(reciterId: String): Flow<List<CachedSurahEntity>>

    @Query("SELECT * FROM cached_surahs WHERE reciterId = :reciterId ORDER BY number ASC")
    suspend fun getSurahsForReciterDirect(reciterId: String): List<CachedSurahEntity>

    @Query("SELECT * FROM cached_surahs WHERE reciterId = :reciterId AND number = :number")
    suspend fun getSurahByNumber(reciterId: String, number: Int): CachedSurahEntity?
}

@Database(
    entities = [
        FavoriteEntity::class,
        DownloadEntity::class,
        PlaybackHistoryEntity::class,
        ContinueListeningEntity::class,
        CachedReciterEntity::class,
        CachedSurahEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun quranDao(): QuranDao
}
