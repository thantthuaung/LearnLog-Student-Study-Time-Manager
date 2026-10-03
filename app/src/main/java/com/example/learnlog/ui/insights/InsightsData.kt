package com.example.learnlog.ui.insights

import org.threeten.bp.LocalDate

data class InsightsData(
    val totalFocusMinutes: Int = 0,
    val sessionCount: Int = 0,
    val avgSessionMinutes: Int = 0,
    /** Subject -> minutes, largest first; anything past the 5th subject is folded into "Other". */
    val timeBySubject: List<Pair<String, Int>> = emptyList(),
    /** Consecutive days (ending today, or yesterday if nothing logged yet today) with a session. */
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val plannedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val completionRate: Float = 0f,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val plannedVsActualByDay: Map<LocalDate, PlannedVsActual> = emptyMap(),
    val topTasks: List<TopTask> = emptyList(),
    val dateRange: DateRange = DateRange.TODAY,
    val rangeStart: LocalDate = LocalDate.now(),
    val rangeEnd: LocalDate = LocalDate.now(),
    val isLoaded: Boolean = false
)

data class PlannedVsActual(
    val plannedMinutes: Int,
    val actualMinutes: Int
)

data class TopTask(
    val taskId: Long,
    val title: String,
    val subject: String?,
    val totalMinutes: Int
)

enum class DateRange {
    TODAY, WEEK, MONTH, CUSTOM
}
