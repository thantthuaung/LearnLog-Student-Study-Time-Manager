package com.example.learnlog.data.model

data class AppSettings(
    val timerPresets: List<TimerPreset> = TimerPresets.DEFAULT_PRESETS,
    val defaultPresetIndex: Int = TimerPresets.DEFAULT_INDEX
) {
    val defaultPreset: TimerPreset
        get() = timerPresets.getOrElse(defaultPresetIndex) { timerPresets.first() }
}
