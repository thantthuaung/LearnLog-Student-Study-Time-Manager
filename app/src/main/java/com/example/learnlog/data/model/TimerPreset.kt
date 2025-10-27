package com.example.learnlog.data.model

data class TimerPreset(
    val durationMinutes: Int,
    val label: String
)

object TimerPresets {
    val DEFAULT_PRESETS = listOf(
        TimerPreset(5, "5 min"),
        TimerPreset(10, "10 min"),
        TimerPreset(15, "15 min"),
        TimerPreset(20, "20 min"),
        TimerPreset(25, "25 min"),
        TimerPreset(30, "30 min"),
        TimerPreset(45, "45 min"),
        TimerPreset(60, "60 min")
    )

    const val MIN_DURATION_MINUTES = 1
    const val MAX_DURATION_MINUTES = 180 // 3 hours
}

data class TimerConfig(
    val durationMinutes: Int = 25,
    val autoBreakDuration: Int = 5,
    val soundEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val keepScreenOn: Boolean = false,
    val customPresets: List<TimerPreset> = emptyList()
)

enum class TimerSource {
    TASK,
    TAB
}

