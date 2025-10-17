package com.example.learnlog.ui.planner.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.DialogFragment
import com.example.learnlog.R
import com.example.learnlog.data.model.StudySession
import com.example.learnlog.data.model.StudyType
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

class AddStudySessionDialog : DialogFragment() {
    private lateinit var titleInput: TextInputEditText
    private lateinit var subjectInput: TextInputEditText
    private lateinit var dateInput: TextInputEditText
    private lateinit var timeInput: TextInputEditText
    private lateinit var durationInput: TextInputEditText
    private lateinit var sessionTypeInput: AutoCompleteTextView
    private lateinit var reminderSwitch: SwitchMaterial
    private lateinit var suggestButton: MaterialButton

    private var selectedDateTime = Calendar.getInstance()
    private var onSessionCreated: ((StudySession) -> Unit)? = null
    private var onSuggestionsRequested: (() -> Unit)? = null

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun setOnSessionCreatedListener(listener: (StudySession) -> Unit) {
        onSessionCreated = listener
    }

    fun setOnSuggestionsRequestedListener(listener: () -> Unit) {
        onSuggestionsRequested = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_study_session, null)
        initializeViews(dialogView)
        setupInputs()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_add_session_title))
            .setPositiveButton(getString(R.string.action_add)) { dialog: DialogInterface, _: Int ->
                val session = createSession()
                onSessionCreated?.invoke(session)
            }
            .setNegativeButton(getString(R.string.action_cancel)) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .create()
    }

    private fun initializeViews(view: View) {
        titleInput = view.findViewById(R.id.titleInput)
        subjectInput = view.findViewById(R.id.subjectInput)
        dateInput = view.findViewById(R.id.dateInput)
        timeInput = view.findViewById(R.id.timeInput)
        durationInput = view.findViewById(R.id.durationInput)
        sessionTypeInput = view.findViewById(R.id.sessionTypeInput)
        reminderSwitch = view.findViewById(R.id.reminderSwitch)
        suggestButton = view.findViewById(R.id.suggestButton)
        updateDateDisplay()
        updateTimeDisplay()
    }

    private fun setupInputs() {
        // Setup session type dropdown
        val sessionTypes = StudyType.values().map { it.name }
        sessionTypeInput.setAdapter(ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            sessionTypes
        ))

        // Set default type
        sessionTypeInput.setText(sessionTypes.first(), false)

        // Setup date picker
        dateInput.setOnClickListener { showDatePicker() }
        timeInput.setOnClickListener { showTimePicker() }

        // Setup suggestions button
        suggestButton.setOnClickListener {
            dismiss()
            onSuggestionsRequested?.invoke()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDateTime.set(year, month, day)
                updateDateDisplay()
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedDateTime.set(Calendar.MINUTE, minute)
                updateTimeDisplay()
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateDateDisplay() {
        dateInput.setText(dateFormat.format(selectedDateTime.time))
    }

    private fun updateTimeDisplay() {
        timeInput.setText(timeFormat.format(selectedDateTime.time))
    }

    private fun createSession(): StudySession {
        val durationMinutes = durationInput.text.toString().toIntOrNull() ?: 60
        // Convert Calendar to ThreeTen BP LocalDateTime in an API-compatible way
        val instant = org.threeten.bp.Instant.ofEpochMilli(selectedDateTime.timeInMillis)
        val startDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val endDateTime = startDateTime.plusMinutes(durationMinutes.toLong())

        return StudySession(
            id = 0, // ID will be assigned by the repository/viewmodel
            title = titleInput.text.toString(),
            subject = subjectInput.text.toString(),
            startTime = startDateTime,
            endTime = endDateTime,
            durationMinutes = durationMinutes,
            type = StudyType.valueOf(sessionTypeInput.text.toString())
        )
    }
}
