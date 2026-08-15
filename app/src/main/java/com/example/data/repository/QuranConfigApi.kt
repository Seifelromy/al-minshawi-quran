package com.example.data.repository

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class RemoteSurahJson(
    @Json(name = "number") val number: Int,
    @Json(name = "nameArabic") val nameArabic: String,
    @Json(name = "nameEnglish") val nameEnglish: String,
    @Json(name = "revelationType") val revelationType: String,
    @Json(name = "versesCount") val versesCount: Int,
    @Json(name = "typicalDurationMinutes") val typicalDurationMinutes: Int,
    @Json(name = "audioUrl") val audioUrl: String
)

@JsonClass(generateAdapter = true)
data class RemoteQuranConfig(
    @Json(name = "version") val version: Int,
    @Json(name = "reciter") val reciter: String,
    @Json(name = "surahs") val surahs: List<RemoteSurahJson>
)

interface QuranConfigApi {
    @GET
    suspend fun getConfig(@Url url: String): RemoteQuranConfig
}
