package com.example.learnlog.ui.analytics

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.repository.InsightsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.threeten.bp.LocalDate
import javax.inject.Inject

data class AnalyticsData(
    val days: Int = 7,
    val totalMinutes: Int = 0,
    val sessionCount: Int = 0,
    /** One entry per calendar day in the range, oldest first (days with no focus are 0). */
    val perDay: List<Pair<LocalDate, Int>> = emptyList(),
    val subjects: List<Pair<String, Int>> = emptyList(),
    val topTasks: List<Pair<String, Int>> = emptyList(),
    val bestDay: Pair<LocalDate, Int>? = null,
    val loaded: Boolean = false
) {
    val isEmpty: Boolean get() = totalMinutes == 0
    val averageMinutes: Int get() = if (sessionCount > 0) totalMinutes / sessionCount else 0
}

/**
 * Trends over the last 7 / 30 / 90 days, computed straight from completed focus sessions so
 * totals always match Insights (no double counting with the nightly rollups).
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    sessionLogDao: SessionLogDao,
    taskDao: TaskDao,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val range = savedState.getStateFlow(KEY_DAYS, 7)

    val analyticsData: StateFlow<AnalyticsData> = combine(
        range, sessionLogDao.getCompletedSessions(), taskDao.getAll()
    ) { days, sessions, tasks ->
        val today = LocalDate.now()
        val start = today.minusDays(days - 1L)
        val inRange = sessions.filter { !it.startTime.toLocalDate().isBefore(start) }
        val tasksById = tasks.associateBy { it.id }

        val byDay = inRange.groupBy { it.startTime.toLocalDate() }.mapValues { (_, l) -> l.sumOf { it.durationMinutes } }
        val perDay = (0 until days).map { start.plusDays(it.toLong()) }.map { it to (byDay[it] ?: 0) }

        val subjects = inRange
            .groupBy { s ->
                s.taskId?.let { tasksById[it]?.subject }?.takeIf { it.isNotBlank() }
                    ?: s.subject?.takeIf { it.isNotBlank() && it != "Focus Session" }
                    ?: InsightsRepository.GENERAL
            }
            .mapValues { (_, l) -> l.sumOf { it.durationMinutes } }
            .toList().sortedByDescending { it.second }.take(8)

        val topTasks = inRange.filter { it.taskId != null }
            .groupBy { it.taskId!! }
            .mapNotNull { (id, l) -> tasksById[id]?.let { it.title to l.sumOf { s -> s.durationMinutes } } }
            .sortedByDescending { it.second }.take(5)

        AnalyticsData(
            days = days,
            totalMinutes = inRange.sumOf { it.durationMinutes },
            sessionCount = inRange.size,
            perDay = perDay,
            subjects = subjects,
            topTasks = topTasks,
            bestDay = perDay.filter { it.second > 0 }.maxByOrNull { it.second },
            loaded = true
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsData())

    val days: Int get() = range.value

    fun setDays(days: Int) { savedState[KEY_DAYS] = days }

    companion object {
        private const val KEY_DAYS = "days"
    }
}
