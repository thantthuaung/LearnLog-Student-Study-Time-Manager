package com.example.learnlog.data.model

data class AppSettings(
    val timerPresets: List<TimerPreset> = TimerPresets.DEFAULT_PRESETS,
    val defaultPresetIndex: Int = 0,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val selectedSoundTone: String = "default",
    val vibrationEnabled: Boolean = true
)

