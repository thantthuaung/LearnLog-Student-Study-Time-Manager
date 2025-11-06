package com.example.learnlog.ui.settings

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

/**
 * Dialog for managing timer presets
 * TODO: Implement full preset management with TimerPreset model
 */
@AndroidEntryPoint
class TimerPresetsDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Timer Presets")
            .setMessage("Timer preset management will be available soon.\n\nFor now, you can use the default presets in the Timer screen:\n• 25 min (Pomodoro)\n• 5 min (Short Break)\n• 15 min (Long Break)\n• Custom duration")
            .setPositiveButton("OK", null)
            .create()
    }
}

