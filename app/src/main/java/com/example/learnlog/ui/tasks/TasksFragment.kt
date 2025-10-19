package com.example.learnlog.ui.tasks

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.FragmentTasksBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TasksViewModel by viewModels()

    private lateinit var tasksAdapter: TasksAdapter
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
        setupSwipeToDelete()
    }

    private fun setupRecyclerView() {
        tasksAdapter = TasksAdapter(
            onToggleComplete = { task, isChecked ->
                viewModel.toggleComplete(task, isChecked)
            },
            onStartTimer = { task ->
                val action = TasksFragmentDirections.actionTasksFragmentToTimerFragment(
                    taskId = task.id,
                    taskTitle = task.title
                )
                findNavController().navigate(action)
            },
            onEdit = { task ->
                showAddEditSheet(task)
            }
        )

        headerAdapter = HeaderAdapter()
        filterAdapter = FilterAdapter(
            onFilterChanged = { filter -> viewModel.setFilter(filter) },
            onSortClicked = { sortButton -> showSortMenu(sortButton) }
        )

        val concatAdapter = ConcatAdapter(headerAdapter, filterAdapter, tasksAdapter)

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
                    tasksAdapter.submitList(tasks)
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

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (viewHolder is TasksAdapter.TaskViewHolder) {
                    val position = viewHolder.bindingAdapterPosition
                    val task = tasksAdapter.currentList[position]

                    if (direction == ItemTouchHelper.LEFT) { // Delete
                        viewModel.delete(task.id)
                        Snackbar.make(binding.root, "Task deleted", Snackbar.LENGTH_LONG)
                            .setAction("UNDO") {
                                viewModel.insert(task)
                            }
                            .show()
                    } else { // Complete
                        viewModel.toggleComplete(task, !task.completed)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float,
                actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable()
                val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
                val checkIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_check)

                if (dX > 0) { // Swiping to the right (Complete)
                    background.color = Color.GREEN
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + (dX.toInt()),
                        itemView.bottom
                    )
                    checkIcon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = itemView.bottom - iconMargin
                        it.setBounds(
                            itemView.left + iconMargin,
                            iconTop,
                            itemView.left + iconMargin + it.intrinsicWidth,
                            iconBottom
                        )
                        it.draw(c)
                    }
                } else if (dX < 0) { // Swiping to the left (Delete)
                    background.color = Color.RED
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    deleteIcon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = itemView.bottom - iconMargin
                        it.setBounds(
                            itemView.right - iconMargin - it.intrinsicWidth,
                            iconTop,
                            itemView.right - iconMargin,
                            iconBottom
                        )
                        it.draw(c)
                    }
                } else {
                    background.setBounds(0, 0, 0, 0)
                }

                background.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerViewTasks)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
