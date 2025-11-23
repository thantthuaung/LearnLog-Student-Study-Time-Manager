package com.example.learnlog.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.activityViewModels
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.DialogAddTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.LocalDateTime
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class AddEditTaskBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: TasksViewModel by activityViewModels()
    private var _binding: DialogAddTaskBinding? = null
    private val binding get() = _binding!!

    private var task: TaskEntity? = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            task = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_TASK, TaskEntity::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_TASK)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews()
        setupInputs()

        // Pre-fill if editing existing task
        task?.let { existingTask ->
            binding.dialogTitle.text = getString(R.string.edit_task)
            titleInput.setText(existingTask.title)
            subjectInput.setText(existingTask.subject ?: "")
            descriptionInput.setText(existingTask.notes ?: "")
            durationInput.setText(existingTask.durationMinutes.toString())

            // Set due date/time if exists
            existingTask.dueAt?.let { dueAt ->
                selectedDate = Calendar.getInstance().apply {
                    set(dueAt.year, dueAt.monthValue - 1, dueAt.dayOfMonth,
                        dueAt.hour, dueAt.minute)
                }
                updateDateTimeDisplay()
            }

            // Set priority
            val priorityArray = resources.getStringArray(R.array.priority_options)
            if (existingTask.priority in priorityArray.indices) {
                priorityInput.setText(priorityArray[existingTask.priority], false)
            }

            binding.btnSave.text = getString(R.string.update_task)
        } ?: run {
            binding.dialogTitle.text = getString(R.string.add_task)
            binding.btnSave.text = getString(R.string.save_task)
        }

        binding.btnSave.setOnClickListener {
            saveTask()
        }
    }

    private fun initializeViews() {
        titleInput = binding.titleInput
        subjectInput = binding.subjectInput
        dateInput = binding.dateInput
        timeInput = binding.timeInput
        priorityInput = binding.priorityInput
        typeInput = binding.typeInput
        durationInput = binding.durationInput
        descriptionInput = binding.descriptionInput
        notificationSwitch = binding.notificationSwitch
    }

    private fun setupInputs() {
        // Set default duration
        durationInput.setText("30")
        durationInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                durationInput.selectAll()
            }
        }

        // Setup priority dropdown
        val priorities = resources.getStringArray(R.array.priority_options)
        priorityInput.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, priorities))

        // Setup type dropdown
        val types = resources.getStringArray(R.array.type_options)
        typeInput.setAdapter(ArrayAdapter(requireContext(),
            android.R.layout.simple_dropdown_item_1line, types))

        // Setup date/time pickers
        dateInput.setOnClickListener { showDatePicker() }
        timeInput.setOnClickListener { showTimePicker() }

        // Set default date/time to now
        updateDateTimeDisplay()
    }

    private fun showDatePicker() {
        val picker = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateTimeDisplay()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
        picker.show()
    }

    private fun showTimePicker() {
        val picker = TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                selectedDate.set(Calendar.HOUR_OF_DAY, hour)
                selectedDate.set(Calendar.MINUTE, minute)
                updateDateTimeDisplay()
            },
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE),
            false
        )
        picker.show()
    }

    private fun updateDateTimeDisplay() {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        dateInput.setText(dateFormat.format(selectedDate.time))
        timeInput.setText(timeFormat.format(selectedDate.time))
    }

    private fun saveTask() {
        val title = titleInput.text.toString()
        if (title.isBlank()) {
            titleInput.error = "Title is required"
            return
        }

        // Get duration with validation
        val durationText = durationInput.text.toString().trim()
        val duration = if (durationText.isEmpty()) {
            30
        } else {
            val parsed = durationText.toIntOrNull()
            when {
                parsed == null -> {
                    durationInput.error = "Please enter a valid number"
                    return
                }
                parsed < 1 -> {
                    durationInput.error = "Duration must be at least 1 minute"
                    return
                }
                parsed > 480 -> {
                    durationInput.error = "Duration cannot exceed 480 minutes (8 hours)"
                    return
                }
                else -> parsed
            }
        }

        val dueDateTime = LocalDateTime.of(
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH) + 1,
            selectedDate.get(Calendar.DAY_OF_MONTH),
            selectedDate.get(Calendar.HOUR_OF_DAY),
            selectedDate.get(Calendar.MINUTE)
        )

        // Get priority from dropdown (default to MEDIUM = 1)
        val priorityText = priorityInput.text.toString()
        val priorities = resources.getStringArray(R.array.priority_options)
        val priority = priorities.indexOf(priorityText).takeIf { it >= 0 } ?: 1

        val existingTask = task
        if (existingTask != null) {
            // Update existing task
            val updatedTask = existingTask.copy(
                title = title,
                subject = subjectInput.text.toString().takeIf { it.isNotBlank() },
                dueAt = dueDateTime,
                priority = priority,
                notes = descriptionInput.text.toString().takeIf { it.isNotBlank() },
                durationMinutes = duration
            )
            viewModel.updateExisting(updatedTask)
        } else {
            // Create new task
            val newTask = TaskEntity(
                title = title,
                subject = subjectInput.text.toString().takeIf { it.isNotBlank() },
                dueAt = dueDateTime,
                priority = priority,
                type = typeInput.text.toString().takeIf { it.isNotBlank() } ?: "Assignment",
                status = "PENDING",
                notes = descriptionInput.text.toString().takeIf { it.isNotBlank() },
                durationMinutes = duration,
                completed = false,
                createdAt = LocalDateTime.now()
            )
            viewModel.saveNew(newTask)
        }

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TASK = "arg_task"

        fun newInstance(task: TaskEntity? = null): AddEditTaskBottomSheet {
            return AddEditTaskBottomSheet().apply {
                arguments = Bundle().apply {
                    task?.let { putParcelable(ARG_TASK, it) }
                }
            }
        }
    }
}

