package com.example.learnlog.ui.planner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.repository.TaskRepository
import com.example.learnlog.ui.tasks.TaskRow
import com.example.learnlog.ui.tasks.TasksViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.YearMonth
import org.threeten.bp.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

enum class PlannerFilter { ALL, OPEN, OVERDUE, DONE }

data class DayPlan(
    val date: LocalDate,
    val rows: List<TaskRow>,
    val taskCount: Int,
    val plannedMinutes: Int
)

@HiltViewModel
class PlannerViewModel @Inject constructor(
    taskRepository: TaskRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    val firstDayOfWeek: DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private val _month = savedState.getStateFlow(KEY_MONTH, YearMonth.now().toString())
    private val _selected = savedState.getStateFlow(KEY_DAY, LocalDate.now().toEpochDay())
    private val _filter = savedState.getStateFlow(KEY_FILTER, PlannerFilter.ALL)

    val currentMonth: StateFlow<YearMonth> = _month.map { YearMonth.parse(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, YearMonth.now())
    val selectedDate: StateFlow<LocalDate> = _selected.map { LocalDate.ofEpochDay(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, LocalDate.now())

    private val tasks = taskRepository.getTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val calendarDays: StateFlow<List<CalendarDay>> = combine(currentMonth, selectedDate, tasks) { month, selected, tasks ->
        buildMonth(month, selected, tasks)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dayPlan: StateFlow<DayPlan> = combine(selectedDate, _filter, tasks) { date, filter, tasks ->
        val now = LocalDateTime.now()
        val ofDay = tasks.filter { it.dueAt?.toLocalDate() == date }
        val visible = ofDay.filter {
            when (filter) {
                PlannerFilter.ALL -> true
                PlannerFilter.OPEN -> !it.completed && !it.isOverdue(now)
                PlannerFilter.OVERDUE -> it.isOverdue(now)
                PlannerFilter.DONE -> it.completed
            }
        }.sortedWith(compareBy<TaskEntity> { it.completed }.then(TasksViewModel.BY_DUE))
        DayPlan(date, visible.map { TaskRow.Item(it) }, ofDay.size, ofDay.filter { !it.completed }.sumOf { it.durationMinutes })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DayPlan(LocalDate.now(), emptyList(), 0, 0))

    fun goToPreviousMonth() { savedState[KEY_MONTH] = currentMonth.value.minusMonths(1).toString() }
    fun goToNextMonth() { savedState[KEY_MONTH] = currentMonth.value.plusMonths(1).toString() }

    fun goToToday() {
        val today = LocalDate.now()
        savedState[KEY_MONTH] = YearMonth.from(today).toString()
        savedState[KEY_DAY] = today.toEpochDay()
    }

    fun selectDate(date: LocalDate) {
        savedState[KEY_DAY] = date.toEpochDay()
        val month = YearMonth.from(date)
        if (month != currentMonth.value) savedState[KEY_MONTH] = month.toString()
    }

    fun setFilter(filter: PlannerFilter) { savedState[KEY_FILTER] = filter }

    /** A new-task template for the selected day (id 0, so the editor inserts it). */
    fun newTaskTemplate(): TaskEntity = TaskEntity(
        title = "",
        subject = null,
        dueAt = TasksViewModel.defaultDue(selectedDate.value),
        priority = TaskEntity.PRIORITY_MEDIUM,
        status = TaskEntity.STATUS_PENDING,
        type = TaskEntity.TYPE_ASSIGNMENT
    )

    private fun buildMonth(month: YearMonth, selected: LocalDate, tasks: List<TaskEntity>): List<CalendarDay> {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val byDate = tasks.filter { it.dueAt != null }.groupBy { it.dueAt!!.toLocalDate() }

        val first = month.atDay(1)
        val leading = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        val gridStart = first.minusDays(leading.toLong())
        val cells = ((leading + month.lengthOfMonth() + 6) / 7) * 7

        return (0 until cells).map { i ->
            val date = gridStart.plusDays(i.toLong())
            val dayTasks = byDate[date].orEmpty()
            CalendarDay(
                date = date,
                isCurrentMonth = YearMonth.from(date) == month,
                isToday = date == today,
                isSelected = date == selected,
                taskCount = dayTasks.size,
                completedCount = dayTasks.count { it.completed },
                inProgressCount = dayTasks.count { !it.completed && it.status == TaskEntity.STATUS_IN_PROGRESS },
                overdueCount = dayTasks.count { it.isOverdue(now) },
                hasHighPriority = dayTasks.any { !it.completed && it.priority == TaskEntity.PRIORITY_HIGH }
            )
        }
    }

    companion object {
        private const val KEY_MONTH = "month"
        private const val KEY_DAY = "day"
        private const val KEY_FILTER = "filter"
    }
}
