package com.example.learnlog.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentTasksBinding
import com.google.android.material.snackbar.Snackbar
import java.time.LocalDateTime
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TasksFragment : Fragment(R.layout.fragment_tasks) {
    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val sampleTasks = mutableListOf(
        TaskItem(
            id = 1L,
            title = "Math Homework",
            subject = "Mathematics",
            dueDateTime = LocalDateTime.now().plusDays(1),
            priority = TaskPriority.HIGH,
            type = TaskType.ASSIGNMENT,
            status = TaskStatus.PENDING,
            progress = 0
        ),
        TaskItem(
            id = 2L,
            title = "Science Project",
            subject = "Science",
            dueDateTime = LocalDateTime.now().plusDays(2),
            priority = TaskPriority.MEDIUM,
            type = TaskType.REVISION, // Changed from PROJECT to REVISION
            status = TaskStatus.IN_PROGRESS,
            progress = 50
        ),
        TaskItem(
            id = 3L,
            title = "History Exam",
            subject = "History",
            dueDateTime = LocalDateTime.now().plusHours(5),
            priority = TaskPriority.HIGH,
            type = TaskType.EXAM,
            status = TaskStatus.PENDING,
            progress = 0
        )
    )

    private lateinit var adapter: TasksAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TasksAdapter(
            items = sampleTasks,
            onEdit = { task -> Toast.makeText(requireContext(), "Edit: ${task.title}", Toast.LENGTH_SHORT).show() },
            onDelete = { task ->
                sampleTasks.remove(task)
                adapter.notifyDataSetChanged()
                Snackbar.make(binding.root, "Task deleted", Snackbar.LENGTH_SHORT).show()
            },
            onMarkComplete = { task ->
                val idx = sampleTasks.indexOf(task)
                if (idx != -1) {
                    sampleTasks[idx] = task.copy(status = TaskStatus.COMPLETED, progress = 100)
                    adapter.notifyItemChanged(idx)
                    Snackbar.make(binding.root, "Task marked as complete", Snackbar.LENGTH_SHORT).show()
                }
            },
            onStartTimer = { task -> Toast.makeText(requireContext(), "Start timer for: ${task.title}", Toast.LENGTH_SHORT).show() }
        )
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter

        binding.fabAddTask.setOnClickListener {
            Toast.makeText(requireContext(), "Add Task dialog (to be implemented)", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
