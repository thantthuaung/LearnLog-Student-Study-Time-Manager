package com.example.learnlog.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.learnlog.network.QuoteApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.threeten.bp.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.quoteDataStore: DataStore<Preferences> by preferencesDataStore(name = "quotes")

/**
 * Motivational quote, fetched at most once per calendar day. When offline, the last fetched
 * quote is shown; with no cache at all, a bundled quote for the day is used instead.
 */
@Singleton
class QuoteRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quoteApiService: QuoteApiService
) {

    private object Keys {
        val CONTENT = stringPreferencesKey("quote_content")
        val AUTHOR = stringPreferencesKey("quote_author")
        val FETCHED_DAY = longPreferencesKey("fetched_epoch_day")
    }

    /** Emits the cached quote, or a bundled one when nothing has been cached yet. */
    fun observeQuote(): Flow<CachedQuote> = context.quoteDataStore.data.map { prefs ->
        val content = prefs[Keys.CONTENT]
        val author = prefs[Keys.AUTHOR]
        if (content != null && author != null) {
            CachedQuote(content, author, isFromCache = true)
        } else {
            fallbackFor(LocalDate.now())
        }
    }

    /** Refreshes the cache if it is from a previous day. Network failures are silent. */
    suspend fun refreshIfStale() {
        val today = LocalDate.now().toEpochDay()
        val fetchedDay = context.quoteDataStore.data.first()[Keys.FETCHED_DAY]
        if (fetchedDay == today) return
        runCatching { quoteApiService.getQuoteOfTheDay().firstOrNull() }
            .getOrNull()
            ?.takeIf { it.content.isNotBlank() && !it.content.startsWith("Too many requests") }
            ?.let { quote ->
                context.quoteDataStore.edit { prefs ->
                    prefs[Keys.CONTENT] = quote.content.trim()
                    prefs[Keys.AUTHOR] = quote.author.trim()
                    prefs[Keys.FETCHED_DAY] = today
                }
            }
    }

    private fun fallbackFor(date: LocalDate): CachedQuote {
        val (content, author) = FALLBACK[(date.toEpochDay() % FALLBACK.size).toInt()]
        return CachedQuote(content, author, isFromCache = false)
    }

    companion object {
        private val FALLBACK = listOf(
            "The secret of getting ahead is getting started." to "Mark Twain",
            "It always seems impossible until it's done." to "Nelson Mandela",
            "Success is the sum of small efforts, repeated day in and day out." to "Robert Collier",
            "Don't watch the clock; do what it does. Keep going." to "Sam Levenson",
            "The expert in anything was once a beginner." to "Helen Hayes",
            "Learning never exhausts the mind." to "Leonardo da Vinci",
            "Focus on being productive instead of busy." to "Tim Ferriss",
            "You don't have to be great to start, but you have to start to be great." to "Zig Ziglar",
            "Small daily improvements are the key to staggering long-term results." to "Robin Sharma",
            "Education is the passport to the future." to "Malcolm X"
        )
    }
}

data class CachedQuote(
    val content: String,
    val author: String,
    val isFromCache: Boolean
)
