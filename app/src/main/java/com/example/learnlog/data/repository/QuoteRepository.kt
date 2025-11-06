package com.example.learnlog.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.learnlog.network.QuoteApiService
import com.example.learnlog.network.QuoteResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private val Context.quoteDataStore: DataStore<Preferences> by preferencesDataStore(name = "quotes")

/**
 * Repository for managing motivational quotes with daily caching
 */
@Singleton
class QuoteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quoteApiService: QuoteApiService
) {

    private object PreferencesKeys {
        val QUOTE_CONTENT = stringPreferencesKey("quote_content")
        val QUOTE_AUTHOR = stringPreferencesKey("quote_author")
        val FETCHED_AT = longPreferencesKey("fetched_at")
    }

    /**
     * Get quote of the day with caching logic
     * Fetches new quote once per day, uses cached value otherwise
     */
    suspend fun getQuoteOfTheDay(): Result<CachedQuote> {
        val preferences = context.quoteDataStore.data.first()
        val cachedContent = preferences[PreferencesKeys.QUOTE_CONTENT]
        val cachedAuthor = preferences[PreferencesKeys.QUOTE_AUTHOR]
        val fetchedAt = preferences[PreferencesKeys.FETCHED_AT] ?: 0L

        val now = System.currentTimeMillis()
        val oneDayInMillis = TimeUnit.DAYS.toMillis(1)
        val shouldRefresh = (now - fetchedAt) > oneDayInMillis

        return if (shouldRefresh) {
            // Try to fetch new quote
            try {
                val response = quoteApiService.getRandomQuote()
                saveQuote(response, now)
                Result.success(CachedQuote(response.content, response.author, now, isFromCache = false))
            } catch (e: Exception) {
                // If fetch fails but we have cached data, use it
                if (cachedContent != null && cachedAuthor != null) {
                    Result.success(CachedQuote(cachedContent, cachedAuthor, fetchedAt, isFromCache = true))
                } else {
                    Result.failure(e)
                }
            }
        } else {
            // Use cached quote
            if (cachedContent != null && cachedAuthor != null) {
                Result.success(CachedQuote(cachedContent, cachedAuthor, fetchedAt, isFromCache = true))
            } else {
                // No cache, fetch new
                try {
                    val response = quoteApiService.getRandomQuote()
                    saveQuote(response, now)
                    Result.success(CachedQuote(response.content, response.author, now, isFromCache = false))
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    /**
     * Observe cached quote as Flow
     */
    fun observeCachedQuote(): Flow<CachedQuote?> {
        return context.quoteDataStore.data.map { preferences ->
            val content = preferences[PreferencesKeys.QUOTE_CONTENT]
            val author = preferences[PreferencesKeys.QUOTE_AUTHOR]
            val fetchedAt = preferences[PreferencesKeys.FETCHED_AT] ?: 0L

            if (content != null && author != null) {
                CachedQuote(content, author, fetchedAt, isFromCache = true)
            } else {
                null
            }
        }
    }

    private suspend fun saveQuote(quote: QuoteResponse, timestamp: Long) {
        context.quoteDataStore.edit { preferences ->
            preferences[PreferencesKeys.QUOTE_CONTENT] = quote.content
            preferences[PreferencesKeys.QUOTE_AUTHOR] = quote.author
            preferences[PreferencesKeys.FETCHED_AT] = timestamp
        }
    }

    /**
     * Force refresh quote (for testing or manual refresh)
     */
    suspend fun forceRefresh(): Result<CachedQuote> {
        return try {
            val now = System.currentTimeMillis()
            val response = quoteApiService.getRandomQuote()
            saveQuote(response, now)
            Result.success(CachedQuote(response.content, response.author, now, isFromCache = false))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CachedQuote(
    val content: String,
    val author: String,
    val fetchedAt: Long,
    val isFromCache: Boolean
)

