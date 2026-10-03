package com.example.learnlog.ui.tasks

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.repository.TaskRepository
import com.example.learnlog.databinding.DialogAddTaskBinding
import com.example.learnlog.ui.common.Format
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {
    val subjects = repository.getSubjects().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    suspend fun save(task: TaskEntity): Long = repository.saveTask(task)
    suspend fun delete(task: TaskEntity) = repository.deleteTask(task.id)
}

/**
 * Create or edit a task. A task with id 0 (new, or a pre-filled template from the Planner)
 * is inserted; anything else is updated. The host is told via the [RESULT_KEY] fragment result.
 */
@AndroidEntryPoint
class AddEditTaskBottomSheet : BottomSheetDialogFragment() {

    private val viewModel: TaskEditorViewModel by viewModels()
    private var _binding: DialogAddTaskBinding? = null
    private val binding get() = _binding!!

    private lateinit var original: TaskEntity
    private var dueDate: LocalDate? = null
    private var dueTime: LocalTime = DEFAULT_TIME
    private var durationMinutes = 30

    private val dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val passed: TaskEntity? = arguments?.let {
            @Suppress("DEPRECATION")
            it.getParcelable(ARG_TASK)
        }
        original = passed ?: TaskEntity(
            title = "",
            subject = null,
            dueAt = TasksViewModel.defaultDue(),
            priority = TaskEntity.PRIORITY_MEDIUM,
            status = TaskEntity.STATUS_PENDING,
            type = TaskEntity.TYPE_ASSIGNMENT
        )
        val state = savedInstanceState
        dueDate = if (state?.containsKey(STATE_HAS_DATE) == true) {
            if (state.getBoolean(STATE_HAS_DATE)) LocalDate.ofEpochDay(state.getLong(STATE_DATE)) else null
        } else original.dueAt?.toLocalDate()
        dueTime = state?.getInt(STATE_TIME, -1)?.takeIf { it >= 0 }?.let { LocalTime.ofSecondOfDay(it.toLong()) }
            ?: original.dueAt?.toLocalTime() ?: DEFAULT_TIME
        durationMinutes = state?.getInt(STATE_DURATION) ?: original.durationMinutes
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isNew = original.isNew

        binding.dialogTitle.setText(if (isNew) R.string.add_task else R.string.edit_task)
        binding.btnSave.setText(if (isNew) R.string.save_task else R.string.update_task)
        binding.btnDelete.isVisible = !isNew

        if (savedInstanceState == null) {
            binding.titleInput.setText(original.title)
            binding.subjectInput.setText(original.subject.orEmpty(), false)
            binding.notesInput.setText(original.notes.orEmpty())
        }

        setupPriority()
        setupTypes()
        setupDurations()
        setupDue()
        setupSubjects()

        binding.titleInput.doAfterTextChanged { binding.titleLayout.error = null }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        if (isNew && savedInstanceState == null) binding.titleInput.requestFocus()
    }

    private fun setupPriority() {
        val labels = resources.getStringArray(R.array.priority_options)
        val chips = listOf(binding.chipPriorityLow, binding.chipPriorityMedium, binding.chipPriorityHigh)
        chips.forEachIndexed { i, chip -> chip.text = labels[i] }
        chips.getOrNull(original.priority.coerceIn(0, 2))?.isChecked = true
    }

    private fun setupTypes() {
        val labels = resources.getStringArray(R.array.type_options)
        val current = TaskEntity.normalizeType(original.type)
        TaskEntity.TYPES.forEachIndexed { i, type ->
            binding.typeGroup.addView(newChip(labels[i]).apply {
                tag = type
                id = View.generateViewId()
                isChecked = type == current
            })
        }
    }

    private fun setupDurations() {
        val options = listOf(15, 30, 45, 60, 90, 120)
        options.forEach { minutes ->
            binding.durationGroup.addView(newChip(Format.compact(minutes)).apply {
                tag = minutes
                id = View.generateViewId()
                isChecked = minutes == durationMinutes
            })
        }
        val custom = newChip(if (durationMinutes in options) getString(R.string.custom) else Format.compact(durationMinutes)).apply {
            id = View.generateViewId()
            tag = TAG_CUSTOM
            isChecked = durationMinutes !in options
            setOnClickListener { askCustomDuration(this) }
        }
        binding.durationGroup.addView(custom)
        binding.durationGroup.setOnCheckedStateChangeListener { group, ids ->
            val chip = ids.firstOrNull()?.let { group.findViewById<Chip>(it) } ?: return@setOnCheckedStateChangeListener
            (chip.tag as? Int)?.let { durationMinutes = it }
        }
    }

    private fun askCustomDuration(chip: Chip) {
        val layout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.custom_minutes_hint)
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val input = com.google.android.material.textfield.TextInputEditText(layout.context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(durationMinutes.toString())
            selectAll()
        }
        layout.addView(input)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.custom_minutes_title)
            .setView(layout)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_ok) { _, _ ->
                val value = input.text.toString().toIntOrNull()
                if (value != null && value in 1..480) {
                    durationMinutes = value
                    chip.text = Format.compact(value)
                    chip.isChecked = true
                } else {
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root, getString(R.string.invalid_minutes, 1, 480),
                        com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
        input.requestFocus()
    }

    private fun setupDue() {
        renderDue()
        binding.btnDate.setOnClickListener {
            val selection = (dueDate ?: LocalDate.now()).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.pick_date)
                .setSelection(selection)
                .build()
                .apply {
                    addOnPositiveButtonClickListener { millis ->
                        dueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        renderDue()
                    }
                }
                .show(childFragmentManager, "date")
        }
        binding.btnTime.setOnClickListener {
            MaterialTimePicker.Builder()
                .setTimeFormat(if (DateFormat.is24HourFormat(requireContext())) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
                .setHour(dueTime.hour)
                .setMinute(dueTime.minute)
                .setTitleText(R.string.pick_time)
                .build()
                .apply {
                    addOnPositiveButtonClickListener {
                        dueTime = LocalTime.of(hour, minute)
                        if (dueDate == null) dueDate = LocalDate.now()
                        renderDue()
                    }
                }
                .show(childFragmentManager, "time")
        }
        binding.btnClearDue.setOnClickListener {
            dueDate = null
            renderDue()
        }
    }

    private fun renderDue() {
        val date = dueDate
        binding.btnDate.text = when (date) {
            null -> getString(R.string.no_due_date)
            LocalDate.now() -> getString(R.string.today)
            LocalDate.now().plusDays(1) -> getString(R.string.section_tomorrow)
            else -> date.format(dateFormat)
        }
        binding.btnTime.isVisible = date != null
        binding.btnClearDue.isVisible = date != null
        binding.btnTime.text = Format.time(LocalDateTime.of(LocalDate.now(), dueTime))
    }

    private fun setupSubjects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.subjects.collect { subjects ->
                binding.subjectInput.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, subjects)
                )
            }
        }
    }

    private fun save() {
        val title = binding.titleInput.text.toString().trim()
        if (title.isEmpty()) {
            binding.titleLayout.error = getString(R.string.task_title_required)
            binding.titleInput.requestFocus()
            return
        }
        val priority = when (binding.priorityGroup.checkedChipId) {
            R.id.chip_priority_low -> TaskEntity.PRIORITY_LOW
            R.id.chip_priority_high -> TaskEntity.PRIORITY_HIGH
            else -> TaskEntity.PRIORITY_MEDIUM
        }
        val type = binding.typeGroup.findViewById<Chip>(binding.typeGroup.checkedChipId)?.tag as? String
            ?: TaskEntity.TYPE_ASSIGNMENT

        val updated = original.copy(
            title = title,
            subject = binding.subjectInput.text.toString().trim().takeIf { it.isNotEmpty() },
            dueAt = dueDate?.atTime(dueTime),
            priority = priority,
            type = type,
            notes = binding.notesInput.text.toString().trim().takeIf { it.isNotEmpty() },
            durationMinutes = durationMinutes
        )
        val isNew = original.isNew
        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            viewModel.save(updated)
            setFragmentResult(RESULT_KEY, bundleOf(RESULT_IS_NEW to isNew))
            dismissAllowingStateLoss()
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_LearnLog_AlertDialog_Destructive)
            .setTitle(R.string.delete_task_title)
            .setMessage(getString(R.string.delete_task_message, original.title))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    viewModel.delete(original)
                    setFragmentResult(RESULT_KEY, bundleOf(RESULT_DELETED to original))
                    dismissAllowingStateLoss()
                }
            }
            .show()
    }

    private fun newChip(text: CharSequence) = (layoutInflater.inflate(R.layout.view_choice_chip, binding.root as ViewGroup, false) as Chip).apply {
        this.text = text
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_HAS_DATE, dueDate != null)
        dueDate?.let { outState.putLong(STATE_DATE, it.toEpochDay()) }
        outState.putInt(STATE_TIME, dueTime.toSecondOfDay())
        outState.putInt(STATE_DURATION, durationMinutes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddEditTaskBottomSheet"
        const val RESULT_KEY = "task_editor_result"
        const val RESULT_IS_NEW = "is_new"
        const val RESULT_DELETED = "deleted"

        private const val ARG_TASK = "arg_task"
        private const val STATE_HAS_DATE = "has_date"
        private const val STATE_DATE = "date"
        private const val STATE_TIME = "time"
        private const val STATE_DURATION = "duration"
        private const val TAG_CUSTOM = "custom"
        private val DEFAULT_TIME: LocalTime = LocalTime.of(23, 59)

        /** [task] null = new task due today; a task with id 0 = new task pre-filled (e.g. a Planner date). */
        fun newInstance(task: TaskEntity? = null) = AddEditTaskBottomSheet().apply {
            arguments = Bundle().apply { task?.let { putParcelable(ARG_TASK, it) } }
        }
    }
}
