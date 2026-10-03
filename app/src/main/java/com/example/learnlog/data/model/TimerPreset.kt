package com.example.learnlog.data.model

data class TimerPreset(
    val durationMinutes: Int,
    val label: String = "$durationMinutes min"
)

object TimerPresets {
    val DEFAULT_PRESETS = listOf(
        TimerPreset(5),
        TimerPreset(15),
        TimerPreset(25),
        TimerPreset(45),
        TimerPreset(60),
        TimerPreset(90)
    )

    /** 25 minutes – a classic Pomodoro. */
    const val DEFAULT_INDEX = 2

    const val MIN_DURATION_MINUTES = 1
    const val MAX_DURATION_MINUTES = 180 // 3 hours
}
