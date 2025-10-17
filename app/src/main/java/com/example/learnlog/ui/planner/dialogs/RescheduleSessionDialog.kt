package com.example.learnlog.ui.planner.dialogs

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import com.example.learnlog.R
import com.example.learnlog.data.model.StudySession
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class RescheduleSessionDialog : DialogFragment() {
    private var session: StudySession? = null
    private var onSessionRescheduled: ((StudySession) -> Unit)? = null
    private lateinit var dateInput: TextInputEditText
    private lateinit var timeInput: TextInputEditText
    private val selectedDateTime = Calendar.getInstance()

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reschedule_session, null)
        initializeViews(dialogView)
        setupDateTimeListeners()
        setupInitialDateTime()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setTitle(getString(R.string.dialog_reschedule_title))
            .setPositiveButton(getString(R.string.action_update)) { _: DialogInterface, _: Int ->
                updateSession()
            }
            .setNegativeButton(getString(R.string.action_cancel)) { dialog: DialogInterface, _: Int ->
                dialog.dismiss()
            }
            .create()
    }

    private fun initializeViews(view: View) {
        dateInput = view.findViewById(R.id.dateInput)
        timeInput = view.findViewById(R.id.timeInput)
    }

    private fun setupDateTimeListeners() {
        dateInput.setOnClickListener {
            showDatePicker()
        }

        timeInput.setOnClickListener {
            showTimePicker()
        }
    }

    private fun setupInitialDateTime() {
        session?.let {
            selectedDateTime.time = org.threeten.bp.DateTimeUtils.toDate(it.startTime.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant())
            updateDateTimeDisplay()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _: DatePicker, year: Int, month: Int, day: Int ->
                selectedDateTime.set(year, month, day)
                updateDateTimeDisplay()
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _: TimePicker, hour: Int, minute: Int ->
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hour)
                selectedDateTime.set(Calendar.MINUTE, minute)
                updateDateTimeDisplay()
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateDateTimeDisplay() {
        dateInput.setText(dateFormat.format(selectedDateTime.time))
        timeInput.setText(timeFormat.format(selectedDateTime.time))
    }

    private fun updateSession() {
        val startDateTime = org.threeten.bp.LocalDateTime.ofInstant(org.threeten.bp.DateTimeUtils.toInstant(selectedDateTime.time), org.threeten.bp.ZoneId.systemDefault())
        val updatedSession = session?.copy(
            startTime = startDateTime,
            endTime = startDateTime.plusMinutes(session!!.durationMinutes.toLong())
        )
        if (updatedSession != null) {
            onSessionRescheduled?.invoke(updatedSession)
        }
        dismiss()
    }

    fun setSession(session: StudySession) {
        this.session = session
    }

    fun setOnSessionRescheduledListener(listener: (StudySession) -> Unit) {
        onSessionRescheduled = listener
    }
}
