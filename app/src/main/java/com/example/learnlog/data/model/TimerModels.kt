package com.example.learnlog.data.model

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    BREAK
}

enum class TimerType {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK
}

data class TimerSettings(
    val focusDuration: Int = 25, // minutes
    val shortBreakDuration: Int = 5,
    val longBreakDuration: Int = 15,
    val longBreakInterval: Int = 4,
    val autoStartBreaks: Boolean = true,
    val autoStartPomodoros: Boolean = false,
    val enableSoundEffects: Boolean = true,
    val enableVibration: Boolean = true
)

data class TimerSession(
    val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val duration: Int, // minutes
    val type: TimerType,
    val subject: String? = null,
    val taskId: Long? = null,
    val label: String? = null,
    var completedDuration: Int = 0,
    var state: TimerState = TimerState.IDLE,
    var streak: Int = 0,
    var cyclesCompleted: Int = 0
)
