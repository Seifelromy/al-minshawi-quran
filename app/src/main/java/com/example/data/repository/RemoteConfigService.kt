package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.database.CachedReciterEntity
import com.example.data.database.CachedSurahEntity
import com.example.data.database.QuranDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class RemoteConfigService(
    private val context: Context,
    private val quranDao: QuranDao
) {
    private val tag = "RemoteConfigService"

    // Configure standard Moshi
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Configure OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    // Configure Retrofit
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://thiqatuk.com/") // Default baseline base URL
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api: QuranConfigApi = retrofit.create(QuranConfigApi::class.java)

    companion object {
        const val PRIMARY_DEFAULT_URL = "https://thiqatuk.com/quran/minshawi.json"
        
        // Dynamic reciter configs definition to support future reciters dynamically
        val KNOWN_RECITERS = listOf(
            ReciterInfo("minshawi", "Muhammad Siddiq Al-Minshawi", PRIMARY_DEFAULT_URL, "quran_minshawi.json"),
            ReciterInfo("abdul_basit", "Abdul Basit", "https://thiqatuk.com/quran/abdul_basit.json", "https://server7.mp3quran.net/basit/{padded}.mp3"),
            ReciterInfo("al_hussary", "Al-Hussary", "https://thiqatuk.com/quran/al_hussary.json", "https://server13.mp3quran.net/husr/{padded}.mp3"),
            ReciterInfo("alafasy", "Mishary Alafasy", "https://thiqatuk.com/quran/alafasy.json", "https://server8.mp3quran.net/afs/{padded}.mp3"),
            ReciterInfo("muaiqly", "Maher Al-Muaiqly", "https://thiqatuk.com/quran/maher_al_muaiqly.json", "https://server12.mp3quran.net/maher/{padded}.mp3")
        )
    }

    data class ReciterInfo(
        val id: String,
        val displayName: String,
        val configUrl: String,
        val fallbackSource: String // either local asset name or audio URL template
    )

    /**
     * Downloads configuration for any reciter using Retrofit.
     * Caches downloaded parameters straight into Room database cache!
     */
    suspend fun downloadAndCacheConfig(reciterId: String, url: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(tag, "Downloading remote configuration from: $url")
            val response = api.getConfig(url)
            
            // Check list validation
            if (response.surahs.isNotEmpty()) {
                val reciterEntity = CachedReciterEntity(
                    id = reciterId,
                    name = response.reciter.takeIf { it.isNotBlank() } ?: KNOWN_RECITERS.find { it.id == reciterId }?.displayName ?: reciterId,
                    configUrl = url,
                    version = response.version,
                    lastUpdated = System.currentTimeMillis()
                )
                
                val surahEntities = response.surahs.map { s ->
                    CachedSurahEntity(
                        reciterId = reciterId,
                        number = s.number,
                        nameArabic = s.nameArabic,
                        nameEnglish = s.nameEnglish,
                        revelationType = s.revelationType,
                        versesCount = s.versesCount,
                        typicalDurationMinutes = s.typicalDurationMinutes,
                        audioUrl = s.audioUrl
                    )
                }

                // Inject into Room
                quranDao.insertReciter(reciterEntity)
                quranDao.clearSurahsForReciter(reciterId)
                quranDao.insertSurahs(surahEntities)
                
                Log.d(tag, "Successfully refreshed and cached in Room database for reciterId: $reciterId")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to download remote configuration for: $reciterId via Retrofit", e)
        }
        return@withContext false
    }

    /**
     * Seed initial configuration from localized assets for offline-first guarantee.
     */
    suspend fun seedLocalAssetFallback(reciterId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Find reciter definition
            val reciterInfo = KNOWN_RECITERS.find { it.id == reciterId } ?: return@withContext false
            val assetName = reciterInfo.fallbackSource
            
            if (assetName.endsWith(".json")) {
                context.assets.open(assetName).use { stream ->
                    val reader = InputStreamReader(stream)
                    val text = reader.readText()
                    reader.close()
                    
                    val configResponse = moshi.adapter(RemoteQuranConfig::class.java).fromJson(text)
                    if (configResponse != null && configResponse.surahs.isNotEmpty()) {
                        val reciterEntity = CachedReciterEntity(
                            id = reciterId,
                            name = configResponse.reciter,
                            configUrl = reciterInfo.configUrl,
                            version = configResponse.version,
                            lastUpdated = System.currentTimeMillis()
                        )
                        val surahEntities = configResponse.surahs.map { s ->
                            CachedSurahEntity(
                                reciterId = reciterId,
                                number = s.number,
                                nameArabic = s.nameArabic,
                                nameEnglish = s.nameEnglish,
                                revelationType = s.revelationType,
                                versesCount = s.versesCount,
                                typicalDurationMinutes = s.typicalDurationMinutes,
                                audioUrl = s.audioUrl
                            )
                        }

                        quranDao.insertReciter(reciterEntity)
                        quranDao.clearSurahsForReciter(reciterId)
                        quranDao.insertSurahs(surahEntities)

                        Log.d(tag, "Seeded $reciterId from local pristine fallback asset config.")
                        return@withContext true
                    }
                }
            } else {
                // Generates dynamic template metadata (like for Mishary Alafasy etc.)
                // to support infinite custom future reciters even with simple templates offline
                val fallbackUrlTemplate = assetName
                val dummyConfigResponse = generateFallbackSurahList(reciterId, reciterInfo.displayName, fallbackUrlTemplate)
                
                quranDao.insertReciter(dummyConfigResponse.first)
                quranDao.clearSurahsForReciter(reciterId)
                quranDao.insertSurahs(dummyConfigResponse.second)

                Log.d(tag, "Seeded template fallback metadata for $reciterId")
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to seed local pristine template configuration for: $reciterId", e)
        }
        return@withContext false
    }

    /**
     * Generates Surah configuration metadata on-the-fly when offline, completely dynamic.
     */
    private fun generateFallbackSurahList(
        reciterId: String,
        reciterName: String,
        audioUrlTemplate: String
    ): Pair<CachedReciterEntity, List<CachedSurahEntity>> {
        val reciter = CachedReciterEntity(
            id = reciterId,
            name = reciterName,
            configUrl = KNOWN_RECITERS.find { it.id == reciterId }?.configUrl ?: "",
            version = 1,
            lastUpdated = System.currentTimeMillis()
        )

        // Maps raw static structures into dynamic entities
        val list = com.example.data.model.Surah.ALL_SURAHS.map { s ->
            val padded = String.format("%03d", s.number)
            val audioUrl = audioUrlTemplate.replace("{padded}", padded)
            CachedSurahEntity(
                reciterId = reciterId,
                number = s.number,
                nameArabic = s.nameArabic,
                nameEnglish = s.nameEnglish,
                revelationType = s.revelationType,
                versesCount = s.versesCount,
                typicalDurationMinutes = s.typicalDurationMinutes,
                audioUrl = audioUrl
            )
        }
        return Pair(reciter, list)
    }
}
