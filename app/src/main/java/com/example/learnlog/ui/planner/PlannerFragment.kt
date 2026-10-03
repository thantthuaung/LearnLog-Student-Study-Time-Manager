package com.example.learnlog.ui.planner

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentPlannerBinding
import com.example.learnlog.ui.base.BaseFragment
import com.example.learnlog.ui.common.Format
import com.example.learnlog.ui.common.Motion
import com.example.learnlog.ui.common.Reselectable
import com.example.learnlog.ui.tasks.TaskInteractions
import com.example.learnlog.ui.tasks.TaskListAdapter
import com.example.learnlog.ui.tasks.TaskOpsViewModel
import com.example.learnlog.ui.tasks.TaskSwipeCallback
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.util.Locale
import kotlin.math.abs

@AndroidEntryPoint
class PlannerFragment : BaseFragment(), Reselectable {

    private var _binding: FragmentPlannerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlannerViewModel by viewModels()
    private val ops: TaskOpsViewModel by viewModels()
    private lateinit var interactions: TaskInteractions

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var tasksAdapter: TaskListAdapter
    private var shownMonth: YearMonth? = null

    private val monthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())
    private val dayFormat = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        interactions = TaskInteractions(this, ops) { binding.fabAddTask }
        interactions.register()

        setupTopBar(
            binding.topBar.root,
            title = getString(R.string.page_planner_title),
            actionIcon = R.drawable.ic_today,
            actionDescription = getString(R.string.today)
        ) { viewModel.goToToday() }

        setupWeekdays()
        setupCalendar()
        setupDayList()
        setupFilters()
        binding.btnPrevMonth.setOnClickListener { viewModel.goToPreviousMonth() }
        binding.btnNextMonth.setOnClickListener { viewModel.goToNextMonth() }
        binding.fabAddTask.setOnClickListener { interactions.create(viewModel.newTaskTemplate()) }
        binding.emptyState.emptyAction.setOnClickListener { interactions.create(viewModel.newTaskTemplate()) }
        observe()
    }

    private fun setupWeekdays() {
        val row = binding.weekdayRow
        row.removeAllViews()
        (0 until 7).forEach { i ->
            val day = viewModel.firstDayOfWeek.plus(i.toLong())
            row.addView(TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                text = day.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelMedium)
                typeface = androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.jakarta_semibold)
                setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant))
            })
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter { day -> viewModel.selectDate(day.date) }
        binding.calendarRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = calendarAdapter
            itemAnimator = null
        }
        // Swipe the month grid sideways to change month
        val detector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                val dx = e2.x - (e1?.x ?: e2.x)
                if (abs(dx) > 120 && abs(velocityX) > abs(velocityY) * 1.5f) {
                    if (dx < 0) viewModel.goToNextMonth() else viewModel.goToPreviousMonth()
                    return true
                }
                return false
            }
        })
        binding.calendarRecyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                detector.onTouchEvent(e)
                return false
            }
        })
    }

    private fun setupDayList() {
        tasksAdapter = TaskListAdapter(
            onTaskClick = interactions::edit,
            onTaskLongClick = interactions::showActions,
            onCheckedChange = interactions::setCompleted,
            onStartFocus = interactions::focus,
            showTimeOnly = true
        )
        binding.dayTasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.dayTasksRecyclerView.adapter = tasksAdapter
        ItemTouchHelper(
            TaskSwipeCallback(requireContext(), tasksAdapter,
                onSwipeComplete = { interactions.setCompleted(it, !it.completed) },
                onSwipeDelete = interactions::delete)
        ).attachToRecyclerView(binding.dayTasksRecyclerView)
    }

    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setFilter(
                when (ids.firstOrNull()) {
                    R.id.chip_pending -> PlannerFilter.OPEN
                    R.id.chip_overdue -> PlannerFilter.OVERDUE
                    R.id.chip_completed -> PlannerFilter.DONE
                    else -> PlannerFilter.ALL
                }
            )
        }
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentMonth.collect { month ->
                        binding.textMonthYear.text = month.format(monthFormat)
                        setTopBarSubtitle(binding.topBar.root, month.format(monthFormat))
                    }
                }
                launch {
                    viewModel.calendarDays.collect { days ->
                        if (days.isEmpty()) return@collect
                        val month = YearMonth.from(days[days.size / 2].date)
                        val previous = shownMonth
                        shownMonth = month
                        if (previous != null && previous != month) {
                            slideMonth(forward = month.isAfter(previous)) { calendarAdapter.submitList(days) }
                        } else {
                            calendarAdapter.submitList(days)
                        }
                    }
                }
                launch {
                    viewModel.dayPlan.collect { plan ->
                        binding.textSelectedDate.text = when (plan.date) {
                            LocalDate.now() -> getString(R.string.today) + " · " + plan.date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
                            else -> plan.date.format(dayFormat)
                        }
                        binding.textDaySummary.text = if (plan.taskCount == 0) getString(R.string.planner_day_empty)
                        else getString(
                            R.string.planner_day_summary, plan.taskCount, Format.minutes(requireContext(), plan.plannedMinutes)
                        )
                        tasksAdapter.submitList(plan.rows)
                        val empty = plan.rows.isEmpty()
                        binding.emptyContainer.isVisible = empty
                        binding.dayTasksRecyclerView.isVisible = !empty
                        if (empty) with(binding.emptyState) {
                            emptyIcon.setImageResource(R.drawable.ic_empty_calendar)
                            emptyTitle.setText(R.string.planner_empty_title)
                            emptyMessage.setText(R.string.planner_empty_message)
                            emptyAction.setText(R.string.planner_add_for_day)
                            emptyAction.isVisible = true
                        }
                    }
                }
            }
        }
    }

    /** Slides the month grid out and the new month in from the direction of travel. */
    private fun slideMonth(forward: Boolean, swap: () -> Unit) {
        val grid = binding.calendarRecyclerView
        val distance = grid.width * 0.25f * if (forward) -1 else 1
        grid.animate().cancel()
        grid.animate().translationX(distance).alpha(0f).setDuration(120).withEndAction {
            swap()
            grid.translationX = -distance
            grid.animate().translationX(0f).alpha(1f).setDuration(240).setInterpolator(Motion.EMPHASIZED).start()
        }.start()
    }

    override fun onReselected() {
        _binding ?: return
        viewModel.goToToday()
        binding.appBar.setExpanded(true, true)
        binding.dayTasksRecyclerView.smoothScrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        shownMonth = null
    }
}
