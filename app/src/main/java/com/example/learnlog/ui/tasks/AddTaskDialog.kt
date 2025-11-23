package com.example.learnlog.ui.tasks

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.DialogFragment
import com.example.learnlog.R
import com.example.learnlog.data.model.Task
import com.example.learnlog.data.model.TaskPriority
import com.example.learnlog.data.model.TaskStatus
import com.example.learnlog.data.model.TaskType
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import org.threeten.bp.LocalDateTime

class AddTaskDialog : DialogFragment() {
    private lateinit var titleInput: TextInputEditText
    private lateinit var subjectInput: TextInputEditText
    private lateinit var dateInput: TextInputEditText
    private lateinit var timeInput: TextInputEditText
    private lateinit var priorityInput: AutoCompleteTextView
    private lateinit var typeInput: AutoCompleteTextView
    private lateinit var durationInput: TextInputEditText
    private lateinit var descriptionInput: TextInputEditText
    private lateinit var notificationSwitch: SwitchMaterial

    private var selectedDate: Calendar = Calendar.getInstance()
    private var onTaskCreated: ((Task) -> Unit)? = null

    fun setOnTaskCreatedListener(listener: (Task) -> Unit) {
        onTaskCreated = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_add_task, null)

        // Initialize views
        initializeViews(view)
        setupInputs()

        return builder.setView(view)
            .setTitle("Add New Task")
            .setPositiveButton("Create") { _, _ -> createTask() }
            .setNegativeButton("Cancel", null)
            .create()
    }

    private fun initializeViews(view: android.view.View) {
        titleInput = view.findViewById(R.id.titleInput)
        subjectInput = view.findViewById(R.id.subjectInput)
        dateInput = view.findViewById(R.id.dateInput)
        timeInput = view.findViewById(R.id.timeInput)
        priorityInput = view.findViewById(R.id.priorityInput)
        typeInput = view.findViewById(R.id.typeInput)
        durationInput = view.findViewById(R.id.durationInput)
        descriptionInput = view.findViewById(R.id.descriptionInput)
        notificationSwitch = view.findViewById(R.id.notificationSwitch)
    }

    private fun setupInputs() {
        // Setup priority dropdown
        val priorities = TaskPriority.entries.map { it.name }
        priorityInput.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, priorities))

        // Setup type dropdown
        val types = TaskType.entries.map { it.name }
        typeInput.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, types))

        // Setup date picker
        dateInput.setOnClickListener { showDatePicker() }
        timeInput.setOnClickListener { showTimePicker() }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hour)
                selectedDate.set(Calendar.MINUTE, minute)
                updateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        dateInput.setText(dateFormat.format(selectedDate.time))
    }

    private fun updateTimeDisplay() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        timeInput.setText(timeFormat.format(selectedDate.time))
    }

    private fun createTask() {
        val title = titleInput.text.toString()
        if (title.isBlank()) {
            titleInput.error = "Title is required"
            return
        }

        // Get duration from input, default to 30 if empty or invalid
        val duration = durationInput.text.toString().toIntOrNull()?.coerceIn(1, 480) ?: 30

        val dueDateTime = LocalDateTime.of(
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH) + 1,  // Calendar months are 0-based
            selectedDate.get(Calendar.DAY_OF_MONTH),
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE)
        )

        val task = Task(
            title = title,
            subject = subjectInput.text.toString(),
            dueDate = dueDateTime,
            priority = TaskPriority.valueOf(priorityInput.text.toString().ifEmpty { TaskPriority.MEDIUM.name }),
            type = TaskType.valueOf(typeInput.text.toString().ifEmpty { TaskType.STUDY_SESSION.name }),
            status = TaskStatus.PENDING,
            description = descriptionInput.text.toString(),
            isNotificationEnabled = notificationSwitch.isChecked,
            durationMinutes = duration
        )
        onTaskCreated?.invoke(task)
    }
}
