package com.example.learnlog.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.DialogAddTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class AddEditTaskBottomSheet : BottomSheetDialogFragment() {
    private val viewModel: TasksViewModel by activityViewModels()
    private var task: TaskEntity? = null
    private var _binding: DialogAddTaskBinding? = null
    private val binding get() = _binding!!

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedPriority: Int = 0 // 0=Low, 1=Medium, 2=High
    private var selectedType: String = "Assignment"

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

        setupPriorityDropdown()
        setupTypeDropdown()
        setupDatePicker()
        setupTimePicker()
        setupValidation()

        val currentTask = task
        if (currentTask != null) {
            binding.dialogTitle.text = getString(R.string.edit_task)
            binding.titleInput.setText(currentTask.title)
            binding.subjectInput.setText(currentTask.subject ?: "")

            // Set date and time from task.dueAt
            currentTask.dueAt?.let { dueAt ->
                selectedDate = dueAt.toLocalDate()
                selectedTime = dueAt.toLocalTime()
                updateDateDisplay()
                updateTimeDisplay()
            }

            // Set priority
            selectedPriority = currentTask.priority
            val priorityOptions = resources.getStringArray(R.array.priority_options)
            binding.priorityInput.setText(priorityOptions[selectedPriority], false)

            // Set type
            selectedType = currentTask.type
            binding.typeInput.setText(selectedType, false)

            binding.descriptionInput.setText(currentTask.notes ?: "")
            binding.btnSave.text = getString(R.string.update_task)
        } else {
            binding.dialogTitle.text = getString(R.string.add_task)
            binding.btnSave.text = getString(R.string.save_task)
        }

        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveTask()
            }
        }
    }

    private fun setupPriorityDropdown() {
        val priorities = resources.getStringArray(R.array.priority_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, priorities)
        binding.priorityInput.setAdapter(adapter)
        binding.priorityInput.setOnItemClickListener { _, _, position, _ ->
            selectedPriority = position
            validateInputs()
        }
    }

    private fun setupTypeDropdown() {
        val types = resources.getStringArray(R.array.type_options)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, types)
        binding.typeInput.setAdapter(adapter)
        binding.typeInput.setOnItemClickListener { _, _, position, _ ->
            selectedType = types[position]
            validateInputs()
        }
    }

    private fun setupDatePicker() {
        binding.dateInput.setOnClickListener {
            val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Due Date")

            selectedDate?.let {
                val millis = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                datePickerBuilder.setSelection(millis)
            }

            val datePicker = datePickerBuilder.build()
            datePicker.addOnPositiveButtonClickListener { selection ->
                selectedDate = LocalDate.ofEpochDay(selection / (24 * 60 * 60 * 1000))
                updateDateDisplay()
                validateInputs()
            }
            datePicker.show(parentFragmentManager, "DATE_PICKER")
        }
    }

    private fun setupTimePicker() {
        binding.timeInput.setOnClickListener {
            val timePickerBuilder = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setTitleText("Select Due Time")

            selectedTime?.let {
                timePickerBuilder.setHour(it.hour)
                timePickerBuilder.setMinute(it.minute)
            }

            val timePicker = timePickerBuilder.build()
            timePicker.addOnPositiveButtonClickListener {
                selectedTime = LocalTime.of(timePicker.hour, timePicker.minute)
                updateTimeDisplay()
                validateInputs()
            }
            timePicker.show(parentFragmentManager, "TIME_PICKER")
        }
    }

    private fun updateDateDisplay() {
        selectedDate?.let {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
            binding.dateInput.setText(it.format(formatter))
        }
    }

    private fun updateTimeDisplay() {
        selectedTime?.let {
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            binding.timeInput.setText(it.format(formatter))
        }
    }

    private fun setupValidation() {
        binding.titleInput.doAfterTextChanged { validateInputs() }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validate title
        val title = binding.titleInput.text.toString()
        if (title.isBlank()) {
            binding.titleInput.error = getString(R.string.error_required_field)
            isValid = false
        } else {
            binding.titleInput.error = null
        }

        // Validate date and time
        if (selectedDate == null) {
            binding.dateInput.error = getString(R.string.error_required_field)
            isValid = false
        } else {
            binding.dateInput.error = null
        }

        if (selectedTime == null) {
            binding.timeInput.error = getString(R.string.error_required_field)
            isValid = false
        } else {
            binding.timeInput.error = null
        }

        // Validate date/time not in the past
        if (selectedDate != null && selectedTime != null) {
            val dueDateTime = LocalDateTime.of(selectedDate, selectedTime)
            if (dueDateTime.isBefore(LocalDateTime.now())) {
                binding.dateInput.error = getString(R.string.error_past_date)
                isValid = false
            }
        }

        // Enable/disable save button
        binding.btnSave.isEnabled = isValid

        return isValid
    }

    private fun saveTask() {
        val title = binding.titleInput.text.toString()
        val subject = binding.subjectInput.text.toString()
        val notes = binding.descriptionInput.text.toString()
        val dueAt = if (selectedDate != null && selectedTime != null) {
            LocalDateTime.of(selectedDate, selectedTime)
        } else {
            null
        }

        val now = LocalDateTime.now()
        val existingTask = task

        val newTask = if (existingTask != null) {
            existingTask.copy(
                title = title,
                subject = subject.takeIf { it.isNotBlank() },
                dueAt = dueAt,
                priority = selectedPriority,
                type = selectedType,
                notes = notes.takeIf { it.isNotBlank() },
                updatedAt = now
            )
        } else {
            TaskEntity(
                title = title,
                subject = subject.takeIf { it.isNotBlank() },
                dueAt = dueAt,
                priority = selectedPriority,
                status = "PENDING",
                progress = 0,
                completed = false,
                notes = notes.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now,
                type = selectedType
            )
        }

        if (existingTask == null) {
            viewModel.saveNew(newTask)
        } else {
            viewModel.updateExisting(newTask)
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
