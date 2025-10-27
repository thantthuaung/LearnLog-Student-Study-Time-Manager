package com.example.learnlog.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.FragmentTasksBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TasksViewModel by viewModels()

    private lateinit var taskEntityAdapter: TaskEntityAdapter
    private lateinit var headerAdapter: HeaderAdapter
    private lateinit var filterAdapter: FilterAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set page title in header
        binding.topBar.pageTitle.text = getString(R.string.page_tasks_title)

        setupRecyclerView()
        setupFab()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        taskEntityAdapter = TaskEntityAdapter(
            onTaskClick = { task ->
                showAddEditSheet(task)
            },
            onStartTimer = { task ->
                // Mark task as IN_PROGRESS when timer starts
                viewModel.markAsInProgress(task)

                // Open task timer bottom sheet (popup)
                val timerSheet = com.example.learnlog.ui.timer.TaskTimerBottomSheet.newInstance(
                    taskId = task.id,
                    taskTitle = task.title,
                    taskSubject = task.subject,
                    durationMinutes = 25 // Default 25 minutes, can be adjusted
                )
                timerSheet.show(childFragmentManager, "TaskTimerSheet")
            },
            onCheckChanged = { task, isChecked ->
                viewModel.toggleComplete(task, isChecked)
            },
            onLongPress = { task ->
                showTaskActionsMenu(task)
            }
        )

        headerAdapter = HeaderAdapter()
        filterAdapter = FilterAdapter(
            onFilterChanged = { filter -> viewModel.setFilter(filter) },
            onSortClicked = { sortButton -> showSortMenu(sortButton) }
        )

        val concatAdapter = ConcatAdapter(headerAdapter, filterAdapter, taskEntityAdapter)

        binding.recyclerViewTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupFab() {
        binding.fabAddTask.setOnClickListener {
            showAddEditSheet(null)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { tasks ->
                    taskEntityAdapter.submitList(tasks)
                    binding.emptyState.isVisible = tasks.isEmpty()
                    binding.recyclerViewTasks.isVisible = tasks.isNotEmpty()
                }
            }
        }
    }

    private fun showSortMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.sort_tasks_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.sort_by_due_date -> viewModel.setSort(TaskSort.DUE_DATE)
                R.id.sort_by_priority -> viewModel.setSort(TaskSort.PRIORITY)
                R.id.sort_by_title -> viewModel.setSort(TaskSort.TITLE)
            }
            true
        }
        popup.show()
    }

    private fun showAddEditSheet(task: TaskEntity?) {
        val bottomSheet = AddEditTaskBottomSheet.newInstance(task)
        bottomSheet.show(childFragmentManager, "AddEditTaskBottomSheet")
    }

    private fun showTaskActionsMenu(task: TaskEntity) {
        val options = arrayOf(
            "Mark Completed",
            "Edit",
            "Duplicate",
            "Delete"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(task.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.toggleComplete(task, true)
                    1 -> showAddEditSheet(task)
                    2 -> duplicateTask(task)
                    3 -> showDeleteConfirmation(task)
                }
            }
            .show()
    }

    private fun duplicateTask(task: TaskEntity) {
        val duplicatedTask = task.copy(
            id = 0, // Will get new ID on insert
            title = "${task.title} (Copy)",
            completed = false,
            status = "PENDING"
        )
        viewModel.insert(duplicatedTask)
        Snackbar.make(binding.root, "Task duplicated", Snackbar.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(task: TaskEntity) {
        // Check if timer is running for this task (simplified for now)
        // In a full implementation, check actual timer state

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete task \"${task.title}\"?")
            .setMessage("This will remove the task from Tasks. Study sessions remain.")
            .setPositiveButton("Delete") { _, _ ->
                performDelete(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete(task: TaskEntity) {
        // Optimistically remove from list - handled by ViewModel and Flow
        viewModel.deleteTaskWithUndo(task)

        // Show undo snackbar
        Snackbar.make(binding.root, "Task deleted", Snackbar.LENGTH_LONG)
            .setAction("UNDO") {
                viewModel.restoreTask(task)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
