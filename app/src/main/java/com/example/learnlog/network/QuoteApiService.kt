package com.example.learnlog.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.http.GET

/**
 * Quote of the day from ZenQuotes (https://zenquotes.io). The previous provider,
 * api.quotable.io, no longer accepts connections.
 */
interface QuoteApiService {

    @GET("today")
    suspend fun getQuoteOfTheDay(): List<QuoteResponse>
}

@JsonClass(generateAdapter = true)
data class QuoteResponse(
    @Json(name = "q") val content: String,
    @Json(name = "a") val author: String
)
