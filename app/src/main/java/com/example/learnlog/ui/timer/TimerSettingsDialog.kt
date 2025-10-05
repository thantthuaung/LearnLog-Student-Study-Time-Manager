package com.example.learnlog.ui.timer

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.example.learnlog.R
import com.example.learnlog.data.model.TimerSettings
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial

class TimerSettingsDialog(private val currentSettings: TimerSettings) : DialogFragment() {
    private lateinit var focusDurationInput: EditText
    private lateinit var shortBreakDurationInput: EditText
    private lateinit var longBreakDurationInput: EditText
    private lateinit var longBreakIntervalInput: EditText
    private lateinit var autoStartBreaksSwitch: SwitchMaterial
    private lateinit var autoStartPomodorosSwitch: SwitchMaterial
    private lateinit var soundEffectsSwitch: SwitchMaterial
    private lateinit var vibrationSwitch: SwitchMaterial
    private lateinit var presetsChipGroup: ChipGroup

    private var onSettingsChanged: ((TimerSettings) -> Unit)? = null

    fun setOnSettingsChangedListener(listener: (TimerSettings) -> Unit) {
        onSettingsChanged = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = layoutInflater.inflate(R.layout.dialog_timer_settings, null)
        initializeViews(view)
        loadCurrentSettings()
        setupPresetChips()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setTitle("Timer Settings")
            .setPositiveButton("Save") { _, _ -> saveSettings() }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun initializeViews(view: View) {
        focusDurationInput = view.findViewById(R.id.focusDurationInput)
        shortBreakDurationInput = view.findViewById(R.id.shortBreakDurationInput)
        longBreakDurationInput = view.findViewById(R.id.longBreakDurationInput)
        longBreakIntervalInput = view.findViewById(R.id.longBreakIntervalInput)
        autoStartBreaksSwitch = view.findViewById(R.id.autoStartBreaksSwitch)
        autoStartPomodorosSwitch = view.findViewById(R.id.autoStartPomodorosSwitch)
        soundEffectsSwitch = view.findViewById(R.id.soundEffectsSwitch)
        vibrationSwitch = view.findViewById(R.id.vibrationSwitch)
        presetsChipGroup = view.findViewById(R.id.presetsChipGroup)
    }

    private fun loadCurrentSettings() {
        focusDurationInput.setText(currentSettings.focusDuration.toString())
        shortBreakDurationInput.setText(currentSettings.shortBreakDuration.toString())
        longBreakDurationInput.setText(currentSettings.longBreakDuration.toString())
        longBreakIntervalInput.setText(currentSettings.longBreakInterval.toString())
        autoStartBreaksSwitch.isChecked = currentSettings.autoStartBreaks
        autoStartPomodorosSwitch.isChecked = currentSettings.autoStartPomodoros
        soundEffectsSwitch.isChecked = currentSettings.enableSoundEffects
        vibrationSwitch.isChecked = currentSettings.enableVibration
    }

    private fun setupPresetChips() {
        presetsChipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.preset25_5 -> applyPreset(25, 5, 15)
                R.id.preset45_10 -> applyPreset(45, 10, 20)
                R.id.preset60_15 -> applyPreset(60, 15, 30)
            }
        }
    }

    private fun applyPreset(focus: Int, shortBreak: Int, longBreak: Int) {
        focusDurationInput.setText(focus.toString())
        shortBreakDurationInput.setText(shortBreak.toString())
        longBreakDurationInput.setText(longBreak.toString())
    }

    private fun saveSettings() {
        val settings = TimerSettings(
            focusDuration = focusDurationInput.text.toString().toIntOrNull() ?: 25,
            shortBreakDuration = shortBreakDurationInput.text.toString().toIntOrNull() ?: 5,
            longBreakDuration = longBreakDurationInput.text.toString().toIntOrNull() ?: 15,
            longBreakInterval = longBreakIntervalInput.text.toString().toIntOrNull() ?: 4,
            autoStartBreaks = autoStartBreaksSwitch.isChecked,
            autoStartPomodoros = autoStartPomodorosSwitch.isChecked,
            enableSoundEffects = soundEffectsSwitch.isChecked,
            enableVibration = vibrationSwitch.isChecked
        )
        onSettingsChanged?.invoke(settings)
    }
}
