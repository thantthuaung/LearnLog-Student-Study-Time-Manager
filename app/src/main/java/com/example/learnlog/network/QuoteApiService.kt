package com.example.learnlog.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

/**
 * API Service for fetching motivational quotes
 * Uses quotable.io - a free quote API
 */
interface QuoteApiService {

    @GET("random?tags=inspirational")
    suspend fun getRandomQuote(): QuoteResponse
}

@JsonClass(generateAdapter = true)
data class QuoteResponse(
    @Json(name = "_id")
    val id: String,
    @Json(name = "content")
    val content: String,
    @Json(name = "author")
    val author: String,
    @Json(name = "tags")
    val tags: List<String>
)

