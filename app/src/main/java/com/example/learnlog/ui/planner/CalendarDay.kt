package com.example.learnlog.ui.planner

import org.threeten.bp.LocalDate

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val taskCount: Int,
    val completedCount: Int,
    val inProgressCount: Int = 0,
    val overdueCount: Int,
    val hasHighPriority: Boolean
)

