package com.example.learnlog.ui.planner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.FragmentPlannerBinding
import com.example.learnlog.ui.tasks.AddEditTaskBottomSheet
import com.example.learnlog.ui.tasks.TaskEntityAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class PlannerFragment : Fragment(R.layout.fragment_planner) {
    private var _binding: FragmentPlannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlannerViewModel by viewModels()

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var dayTasksAdapter: TaskEntityAdapter

    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dayDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set page title in header
        binding.topBar.pageTitle.text = getString(R.string.page_planner_title)

        setupCalendar()
        setupDayTasksList()
        setupFilters()
        setupButtons()
        observeViewModel()
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { day ->
            viewModel.selectDate(day.date)
        }

        binding.calendarRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupDayTasksList() {
        dayTasksAdapter = TaskEntityAdapter(
            onTaskClick = { task ->
                showAddEditSheet(task)
            },
            onStartTimer = { task ->
                // Mark task as IN_PROGRESS when timer starts
                viewModel.toggleTaskCompletion(task, false)

                // Open task timer bottom sheet (popup)
                val timerSheet = com.example.learnlog.ui.timer.TaskTimerBottomSheet.newInstance(
                    taskId = task.id,
                    taskTitle = task.title,
                    taskSubject = task.subject,
                    durationMinutes = 25 // Default 25 minutes
                )
                timerSheet.show(childFragmentManager, "TaskTimerSheet")
            },
            onCheckChanged = { task, isChecked ->
                viewModel.toggleTaskCompletion(task, isChecked)
            },
            onLongPress = { task ->
                showTaskActionsMenu(task)
            }
        )

        binding.dayTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dayTasksAdapter
        }
    }

    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                checkedIds.contains(R.id.chipPending) -> "PENDING"
                checkedIds.contains(R.id.chipInProgress) -> "IN_PROGRESS"
                checkedIds.contains(R.id.chipCompleted) -> "COMPLETED"
                checkedIds.contains(R.id.chipOverdue) -> "OVERDUE"
                else -> null // All
            }
            viewModel.setFilter(filter)
        }
    }

    private fun setupButtons() {
        // Month navigation
        binding.btnPrevMonth.setOnClickListener {
            viewModel.goToPreviousMonth()
        }

        binding.btnNextMonth.setOnClickListener {
            viewModel.goToNextMonth()
        }

        binding.btnToday.setOnClickListener {
            viewModel.goToToday()
        }

        // FAB for adding task
        binding.fabAddTask.setOnClickListener {
            val selectedDate = viewModel.selectedDate.value
            showAddEditSheet(null, selectedDate)
        }

        // Empty state CTA button
        binding.btnEmptyAddTask.setOnClickListener {
            val selectedDate = viewModel.selectedDate.value
            showAddEditSheet(null, selectedDate)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe current month
                launch {
                    viewModel.currentMonth.collect { month ->
                        binding.textMonthYear.text = month.format(monthYearFormatter)
                    }
                }

                // Observe calendar days
                launch {
                    viewModel.calendarDays.collect { days ->
                        calendarAdapter.submitList(days)
                    }
                }

                // Observe selected date
                launch {
                    viewModel.selectedDate.collect { date ->
                        binding.textSelectedDate.text = getString(
                            R.string.tasks_for_date,
                            date.format(dayDateFormatter)
                        )
                    }
                }

                // Observe day tasks
                launch {
                    viewModel.dayTasks.collect { tasks ->
                        dayTasksAdapter.submitList(tasks)
                        binding.emptyState.isVisible = tasks.isEmpty()
                        binding.dayTasksRecyclerView.isVisible = tasks.isNotEmpty()
                    }
                }
            }
        }
    }

    private fun showAddEditSheet(task: TaskEntity?, prefilledDate: LocalDate? = null) {
        val bottomSheet = if (task != null) {
            AddEditTaskBottomSheet.newInstance(task)
        } else if (prefilledDate != null) {
            // Create a new task with the selected date
            val taskWithDate = TaskEntity(
                title = "",
                subject = null,
                dueAt = prefilledDate.atTime(23, 59),
                priority = 0,
                status = "PENDING",
                type = "ASSIGNMENT"
            )
            AddEditTaskBottomSheet.newInstance(taskWithDate)
        } else {
            AddEditTaskBottomSheet.newInstance(null)
        }
        bottomSheet.show(childFragmentManager, "AddEditTaskBottomSheet")
    }

    private fun showTaskActionsMenu(task: TaskEntity) {
        val options = arrayOf(
            "Reschedule",
            "Mark Completed",
            "Edit",
            "Duplicate",
            "Delete"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(task.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showRescheduleOptions(task)
                    1 -> viewModel.toggleTaskCompletion(task, true)
                    2 -> showAddEditSheet(task)
                    3 -> duplicateTask(task)
                    4 -> showDeleteConfirmation(task)
                }
            }
            .show()
    }

    private fun showRescheduleOptions(task: TaskEntity) {
        val rescheduleSheet = RescheduleTaskBottomSheet.newInstance(task) { date, time ->
            val newDateTime = date.atTime(time)
            viewModel.updateTaskDueDate(task.id, newDateTime)
            Snackbar.make(binding.root, "Task rescheduled", Snackbar.LENGTH_SHORT).show()
        }
        rescheduleSheet.show(childFragmentManager, "RescheduleSheet")
    }

    private fun duplicateTask(task: TaskEntity) {
        // Open AddEditTaskBottomSheet with duplicated task (no ID)
        val duplicatedTask = task.copy(
            id = 0,
            title = "${task.title} (Copy)"
        )
        showAddEditSheet(duplicatedTask)
    }

    private fun showDeleteConfirmation(task: TaskEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete \"${task.title}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteTask(task.id)
                Snackbar.make(binding.root, "Task deleted", Snackbar.LENGTH_SHORT)
                    .setAction("Undo") {
                        // TODO: Implement undo
                    }
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
