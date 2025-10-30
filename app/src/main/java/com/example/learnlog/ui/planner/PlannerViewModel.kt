package com.example.learnlog.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.repository.TaskRepository
import com.example.learnlog.util.DateTimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth

@HiltViewModel
class PlannerViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val dateTimeProvider: DateTimeProvider
) : ViewModel() {

    // Current month and selected date
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Filter state
    private val _filterStatus = MutableStateFlow<String?>(null)

    // All tasks from DB
    private val allTasks: StateFlow<List<TaskEntity>> = taskRepository.getTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Calendar days for the current month
    val calendarDays: StateFlow<List<CalendarDay>> = combine(
        _currentMonth,
        _selectedDate,
        allTasks
    ) { month, selected, tasks ->
        generateCalendarDays(month, selected, tasks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Tasks for the selected date with filter
    val dayTasks: StateFlow<List<TaskEntity>> = combine(
        _selectedDate,
        _filterStatus,
        allTasks
    ) { date, filter, tasks ->
        filterTasksForDate(date, filter, tasks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Navigation
    fun goToPreviousMonth() {
        _currentMonth.value = _currentMonth.value.minusMonths(1)
    }

    fun goToNextMonth() {
        _currentMonth.value = _currentMonth.value.plusMonths(1)
    }

    fun goToToday() {
        val today = LocalDate.now()
        _currentMonth.value = YearMonth.from(today)
        _selectedDate.value = today
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // Filter
    fun setFilter(status: String?) {
        _filterStatus.value = status
    }

    // Task operations
    fun updateTaskDueDate(taskId: Long, newDueDate: org.threeten.bp.LocalDateTime) {
        viewModelScope.launch {
            val task = allTasks.value.find { it.id == taskId }
            task?.let {
                taskRepository.updateTask(it.copy(dueAt = newDueDate))
            }
        }
    }

    fun toggleTaskCompletion(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.updateTask(
                task.copy(
                    completed = completed,
                    status = if (completed) "COMPLETED" else "PENDING"
                )
            )
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    // Helper: Generate calendar days
    private fun generateCalendarDays(
        month: YearMonth,
        selectedDate: LocalDate,
        tasks: List<TaskEntity>
    ): List<CalendarDay> {
        val firstDayOfMonth = month.atDay(1)
        val lastDayOfMonth = month.atEndOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 // 0 = Sunday
        val today = LocalDate.now()

        val days = mutableListOf<CalendarDay>()

        // Add days from previous month to fill the first week
        val previousMonth = month.minusMonths(1)
        val daysInPreviousMonth = previousMonth.lengthOfMonth()
        for (i in (daysInPreviousMonth - firstDayOfWeek + 1)..daysInPreviousMonth) {
            val date = previousMonth.atDay(i)
            days.add(createCalendarDay(date, false, today, selectedDate, tasks))
        }

        // Add days of current month
        for (day in 1..lastDayOfMonth.dayOfMonth) {
            val date = month.atDay(day)
            days.add(createCalendarDay(date, true, today, selectedDate, tasks))
        }

        // Add days from next month to complete the grid (6 weeks = 42 cells)
        val nextMonth = month.plusMonths(1)
        val remainingDays = 42 - days.size
        for (day in 1..remainingDays) {
            val date = nextMonth.atDay(day)
            days.add(createCalendarDay(date, false, today, selectedDate, tasks))
        }

        return days
    }

    private fun createCalendarDay(
        date: LocalDate,
        isCurrentMonth: Boolean,
        today: LocalDate,
        selectedDate: LocalDate,
        tasks: List<TaskEntity>
    ): CalendarDay {
        val dayTasks = tasks.filter { task ->
            task.dueAt?.toLocalDate() == date
        }

        val completedCount = dayTasks.count { it.completed }
        val inProgressCount = dayTasks.count { it.status == "IN_PROGRESS" && !it.completed }
        val overdueCount = dayTasks.count { task ->
            !task.completed && task.dueAt?.isBefore(dateTimeProvider.now()) == true
        }
        val hasHighPriority = dayTasks.any { it.priority == 2 } // 2 = HIGH

        return CalendarDay(
            date = date,
            isCurrentMonth = isCurrentMonth,
            isToday = date == today,
            isSelected = date == selectedDate,
            taskCount = dayTasks.size,
            completedCount = completedCount,
            inProgressCount = inProgressCount,
            overdueCount = overdueCount,
            hasHighPriority = hasHighPriority
        )
    }

    // Helper: Filter tasks for a specific date
    private fun filterTasksForDate(
        date: LocalDate,
        filter: String?,
        tasks: List<TaskEntity>
    ): List<TaskEntity> {
        val now = dateTimeProvider.now()

        return tasks.filter { task ->
            task.dueAt?.toLocalDate() == date
        }.filter { task ->
            when (filter) {
                "PENDING" -> task.status == "PENDING"
                "IN_PROGRESS" -> task.status == "IN_PROGRESS"
                "COMPLETED" -> task.completed
                "OVERDUE" -> !task.completed && task.dueAt?.isBefore(now) == true
                else -> true // "ALL" or null
            }
        }.sortedWith(
            compareByDescending<TaskEntity> { it.priority }
                .thenBy { it.dueAt }
        )
    }
}
