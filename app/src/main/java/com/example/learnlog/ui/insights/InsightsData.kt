package com.example.learnlog.ui.insights

import org.threeten.bp.LocalDate

data class InsightsData(
    val totalFocusMinutes: Int = 0,
    val timeBySubject: Map<String, Int> = emptyMap(),
    val currentStreak: Int = 0,
    val plannedMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val completionRate: Float = 0f,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val plannedVsActualByDay: Map<LocalDate, PlannedVsActual> = emptyMap(),
    val topTasks: List<TopTask> = emptyList(),
    val totalInterruptions: Int = 0,
    val avgSessionMinutes: Int = 0,
    val dateRange: DateRange = DateRange.TODAY
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

