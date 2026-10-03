package com.example.learnlog.ui.insights

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.repository.CachedQuote
import com.example.learnlog.data.repository.InsightsRepository
import com.example.learnlog.data.repository.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsRepository: InsightsRepository,
    private val quoteRepository: QuoteRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val range = savedState.getStateFlow(KEY_RANGE, DateRange.TODAY)
    private val customStart = savedState.getStateFlow<Long?>(KEY_START, null)
    private val customEnd = savedState.getStateFlow<Long?>(KEY_END, null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val insightsData: StateFlow<InsightsData> = combine(range, customStart, customEnd) { r, s, e -> Triple(r, s, e) }
        .flatMapLatest { (r, s, e) ->
            insightsRepository.getInsightsData(r, s?.let(LocalDate::ofEpochDay), e?.let(LocalDate::ofEpochDay))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsData())

    val quote: StateFlow<CachedQuote?> = quoteRepository.observeQuote()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedRange: DateRange get() = range.value

    init {
        viewModelScope.launch { quoteRepository.refreshIfStale() }
    }

    fun setTimeRange(value: DateRange) { savedState[KEY_RANGE] = value }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        savedState[KEY_START] = start.toEpochDay()
        savedState[KEY_END] = end.toEpochDay()
        savedState[KEY_RANGE] = DateRange.CUSTOM
    }

    companion object {
        private const val KEY_RANGE = "range"
        private const val KEY_START = "start"
        private const val KEY_END = "end"
    }
}
