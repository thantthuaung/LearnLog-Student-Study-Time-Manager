package com.example.learnlog.ui.insights

data class InsightsData(
    val totalFocusMinutes: Int = 0,
    val timeBySubject: Map<String, Int> = emptyMap(),
    val currentStreak: Int = 0,
    val plannedMinutes: Int = 0,
    val actualMinutes: Int = 0
)
