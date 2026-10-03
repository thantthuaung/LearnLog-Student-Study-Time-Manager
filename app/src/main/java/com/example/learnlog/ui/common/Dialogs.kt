package com.example.learnlog.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.InputType
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.learnlog.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

object Dialogs {
    /** Asks for a whole number of minutes in [min]..[max]. */
    fun askMinutes(
        context: Context,
        title: CharSequence,
        initial: Int,
        min: Int,
        max: Int,
        onResult: (Int) -> Unit
    ) {
        val density = context.resources.displayMetrics.density
        val layout = TextInputLayout(context).apply {
            hint = context.getString(R.string.minutes_range_hint, min, max)
            setPadding((20 * density).toInt(), (8 * density).toInt(), (20 * density).toInt(), 0)
            suffixText = "min"
        }
        val input = TextInputEditText(layout.context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(initial.toString())
            selectAll()
        }
        layout.addView(input)
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(layout)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_ok, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = input.text.toString().toIntOrNull()
                if (value == null || value !in min..max) {
                    layout.error = context.getString(R.string.invalid_minutes, min, max)
                } else {
                    onResult(value)
                    dialog.dismiss()
                }
            }
            input.requestFocus()
        }
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        dialog.show()
    }
}

/**
 * Asks for the notification permission (Android 13+) the first time it matters – when a
 * timer starts – then runs the action whatever the answer. Create it as a property so the
 * launcher is registered before the fragment starts.
 */
class NotificationPermissionGate(
    private val fragment: Fragment,
    private val shouldAsk: suspend () -> Boolean,
    private val markAsked: () -> Unit
) {
    private var pending: (() -> Unit)? = null
    private val launcher = fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending?.invoke()
        pending = null
    }

    fun run(action: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(fragment.requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            action(); return
        }
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            if (shouldAsk()) {
                markAsked()
                pending = action
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                action()
            }
        }
    }
}
