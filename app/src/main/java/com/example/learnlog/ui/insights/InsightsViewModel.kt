package com.example.learnlog.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.repository.InsightsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class TimeRange {
    TODAY, WEEK, MONTH
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val insightsRepository: InsightsRepository
) : ViewModel() {
    private val _timeRange = MutableStateFlow(TimeRange.TODAY)

    val insightsData = _timeRange.map { range ->
        // Temporary mock data
        InsightsData(
            totalFocusMinutes = 120,
            timeBySubject = mapOf(
                "Math" to 45,
                "Physics" to 35,
                "Chemistry" to 40
            ),
            currentStreak = 3,
            plannedMinutes = 180,
            actualMinutes = 120
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InsightsData()
    )

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }
}
