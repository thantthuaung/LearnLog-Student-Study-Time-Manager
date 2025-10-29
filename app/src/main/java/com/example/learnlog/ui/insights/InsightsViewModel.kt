package com.example.learnlog.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.repository.InsightsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.threeten.bp.LocalDate
import javax.inject.Inject

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsRepository: InsightsRepository
) : ViewModel() {

    private val _dateRange = MutableStateFlow(DateRange.TODAY)
    private val _customStartDate = MutableStateFlow<LocalDate?>(null)
    private val _customEndDate = MutableStateFlow<LocalDate?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val insightsData: StateFlow<InsightsData> = combine(
        _dateRange,
        _customStartDate,
        _customEndDate
    ) { range, customStart, customEnd ->
        Triple(range, customStart, customEnd)
    }.flatMapLatest { (range, customStart, customEnd) ->
        insightsRepository.getInsightsData(range, customStart, customEnd)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsData()
    )

    fun setTimeRange(range: DateRange) {
        _dateRange.value = range
    }

    fun setCustomDateRange(start: LocalDate, end: LocalDate) {
        _customStartDate.value = start
        _customEndDate.value = end
        _dateRange.value = DateRange.CUSTOM
    }
}
