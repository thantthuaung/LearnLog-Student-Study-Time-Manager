package com.example.learnlog.ui.planner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentPlannerBinding
import com.example.learnlog.ui.tasks.TaskItem
import com.example.learnlog.ui.tasks.TaskPriority
import com.example.learnlog.ui.tasks.TaskStatus
import com.example.learnlog.ui.tasks.TaskType
import java.time.LocalDateTime
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.button.MaterialButtonToggleGroup
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlannerFragment : Fragment(R.layout.fragment_planner) {
    private var _binding: FragmentPlannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlannerViewModel by viewModels()
    private lateinit var adapter: StudySessionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = StudySessionAdapter(
            onMarkComplete = { session ->
                viewModel.markSessionCompleted(session.id)
                Snackbar.make(binding.root, "Session marked as complete", Snackbar.LENGTH_SHORT).show()
            },
            onSkip = { session ->
                viewModel.markSessionSkipped(session.id)
                Snackbar.make(binding.root, "Session skipped", Snackbar.LENGTH_SHORT).show()
            },
            onStartTimer = { session ->
                Toast.makeText(requireContext(), "Start timer for: ${session.subject}", Toast.LENGTH_SHORT).show()
                // TODO: Integrate with Timer
            }
        )
        binding.recyclerViewPlanner.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPlanner.adapter = adapter

        // Week/Day toggle
        binding.toggleCalendarMode.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnWeekView -> viewModel.setCalendarMode(CalendarMode.WEEK)
                    R.id.btnDayView -> viewModel.setCalendarMode(CalendarMode.DAY)
                }
            }
        }

        // Inject sample tasks for demo (replace with real task integration)
        val sampleTasks = listOf(
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
                type = TaskType.REVISION,
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
        viewModel.setTasks(sampleTasks)

        viewModel.sessions.observe(viewLifecycleOwner, Observer { sessions ->
            adapter.submitList(sessions)
        })

        // FAB for manual session addition
        binding.fabAddSession.setOnClickListener {
            // TODO: Show dialog to add manual session
            Toast.makeText(requireContext(), "Add Study Session dialog (to be implemented)", Toast.LENGTH_SHORT).show()
        }

        // Drag & drop (stub)
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // TODO: Implement drag & drop reordering/rescheduling
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewPlanner)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
