package com.example.learnlog.ui.planner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnlog.R
import com.example.learnlog.data.model.CalendarMode
import com.example.learnlog.data.model.StudySession
import com.example.learnlog.databinding.FragmentPlannerBinding
import com.example.learnlog.ui.tasks.TaskItem
import com.example.learnlog.ui.tasks.TaskPriority
import com.example.learnlog.ui.tasks.TaskStatus
import com.example.learnlog.ui.tasks.TaskType
import com.example.learnlog.util.DateTimeProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlannerFragment : Fragment(R.layout.fragment_planner) {
    private var _binding: FragmentPlannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlannerViewModel by viewModels()
    private lateinit var adapter: StudySessionAdapter

    @Inject
    lateinit var dateTimeProvider: DateTimeProvider

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupObservers()
        loadSampleTasks()
    }

    private fun setupUI() {
        binding.topBar.pageChip.text = getString(R.string.nav_planner)

        adapter = StudySessionAdapter(
            onMarkComplete = { session: StudySession ->
                viewModel.markSessionCompleted(session.id)
                Snackbar.make(binding.root, "Session marked as complete", Snackbar.LENGTH_SHORT).show()
            },
            onSkip = { session: StudySession ->
                viewModel.markSessionSkipped(session.id)
                Snackbar.make(binding.root, "Session skipped", Snackbar.LENGTH_SHORT).show()
            },
            onStartTimer = { session: StudySession ->
                Toast.makeText(requireContext(), "Start timer for: ${session.subject}", Toast.LENGTH_SHORT).show()
                // TODO: Integrate with Timer
            }
        )
        binding.recyclerViewPlanner.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPlanner.adapter = adapter

        // Week/Day toggle
        binding.toggleCalendarMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnWeekView -> viewModel.setCalendarMode(CalendarMode.WEEK)
                    R.id.btnDayView -> viewModel.setCalendarMode(CalendarMode.DAY)
                }
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.plannerItems.collect { items ->
                adapter.submitList(items)
            }
        }
    }

    private fun loadSampleTasks() {
        val tasks = listOf(
            TaskItem(
                id = 1L,
                title = "Math Homework",
                subject = "Mathematics",
                dueDateTime = dateTimeProvider.now().plusDays(1),
                priority = TaskPriority.HIGH,
                type = TaskType.ASSIGNMENT,
                status = TaskStatus.PENDING
            ),
            TaskItem(
                id = 2L,
                title = "Science Project",
                subject = "Science",
                dueDateTime = dateTimeProvider.now().plusDays(2),
                priority = TaskPriority.MEDIUM,
                type = TaskType.REVISION,
                status = TaskStatus.IN_PROGRESS,
                progress = 50
            ),
            TaskItem(
                id = 3L,
                title = "History Exam",
                subject = "History",
                dueDateTime = dateTimeProvider.now().plusHours(5),
                priority = TaskPriority.HIGH,
                type = TaskType.EXAM,
                status = TaskStatus.PENDING
            )
        )
        viewModel.loadTasks(tasks)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
