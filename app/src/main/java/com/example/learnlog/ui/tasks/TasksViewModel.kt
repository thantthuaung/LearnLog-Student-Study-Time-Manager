package com.example.learnlog.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import javax.inject.Inject

enum class TaskSection { OVERDUE, TODAY, TOMORROW, UPCOMING, NO_DATE, DONE }

sealed class TaskRow {
    abstract val key: String
    data class Header(val section: TaskSection, val count: Int) : TaskRow() {
        override val key = "h_${section.name}"
    }
    data class Item(val task: TaskEntity) : TaskRow() {
        override val key = "t_${task.id}"
    }
}

data class TasksUiState(
    val rows: List<TaskRow> = emptyList(),
    val counts: Map<TaskFilter, Int> = emptyMap(),
    val todayTotal: Int = 0,
    val todayDone: Int = 0,
    val todayFocusMinutes: Int = 0,
    val hasAnyTasks: Boolean = false,
    val isSearching: Boolean = false,
    val filter: TaskFilter = TaskFilter.ALL,
    val loaded: Boolean = false
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    sessionLogDao: SessionLogDao,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val filter = savedState.getStateFlow(KEY_FILTER, TaskFilter.ALL)
    private val sort = savedState.getStateFlow(KEY_SORT, TaskSort.DUE_DATE)
    private val query = savedState.getStateFlow(KEY_QUERY, "")

    private val todayFocus = sessionLogDao.getCompletedSessions().map { sessions ->
        val today = LocalDate.now()
        sessions.filter { it.startTime.toLocalDate() == today }.sumOf { it.durationMinutes }
    }

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepository.getTasks(), filter, sort, query, todayFocus
    ) { tasks, filter, sort, query, focus ->
        build(tasks, filter, sort, query.trim(), focus)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    val currentSort: TaskSort get() = sort.value

    fun setFilter(value: TaskFilter) { savedState[KEY_FILTER] = value }
    fun setSort(value: TaskSort) { savedState[KEY_SORT] = value }
    fun setSearchQuery(value: String) { savedState[KEY_QUERY] = value }

    fun setCompleted(task: TaskEntity, done: Boolean) = viewModelScope.launch {
        taskRepository.updateTask(task.withCompleted(done))
    }

    fun delete(task: TaskEntity) = viewModelScope.launch { taskRepository.deleteTask(task.id) }

    /** Re-inserts a deleted task with its original id so undo is lossless. */
    fun restore(task: TaskEntity) = viewModelScope.launch { taskRepository.insertTask(task) }

    fun duplicate(task: TaskEntity) = viewModelScope.launch {
        taskRepository.saveTask(task.copy(id = 0, title = "${task.title} (copy)").withCompleted(false))
    }

    // ─────────────── list building ───────────────

    private fun build(tasks: List<TaskEntity>, filter: TaskFilter, sort: TaskSort, query: String, focus: Int): TasksUiState {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()

        val searched = if (query.isEmpty()) tasks else tasks.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.subject?.contains(query, ignoreCase = true) == true ||
                it.notes?.contains(query, ignoreCase = true) == true
        }

        fun sectionOf(t: TaskEntity): TaskSection = when {
            t.completed -> TaskSection.DONE
            t.dueAt == null -> TaskSection.NO_DATE
            t.dueAt.isBefore(now) -> TaskSection.OVERDUE
            t.dueAt.toLocalDate() == today -> TaskSection.TODAY
            t.dueAt.toLocalDate() == today.plusDays(1) -> TaskSection.TOMORROW
            else -> TaskSection.UPCOMING
        }

        val counts = mapOf(
            TaskFilter.ALL to searched.count { !it.completed },
            TaskFilter.TODAY to searched.count { !it.completed && it.dueAt?.toLocalDate() == today },
            TaskFilter.UPCOMING to searched.count { !it.completed && it.dueAt?.isAfter(now) == true && it.dueAt.toLocalDate() != today },
            TaskFilter.OVERDUE to searched.count { it.isOverdue(now) },
            TaskFilter.DONE to searched.count { it.completed }
        )

        val visible = when (filter) {
            TaskFilter.ALL -> searched
            TaskFilter.TODAY -> searched.filter { it.dueAt?.toLocalDate() == today }
            TaskFilter.UPCOMING -> searched.filter { !it.completed && it.dueAt?.isAfter(now) == true && it.dueAt.toLocalDate() != today }
            TaskFilter.OVERDUE -> searched.filter { it.isOverdue(now) }
            TaskFilter.DONE -> searched.filter { it.completed }
        }

        val comparator: Comparator<TaskEntity> = when (sort) {
            TaskSort.DUE_DATE -> BY_DUE.thenByDescending { it.priority }
            TaskSort.PRIORITY -> compareByDescending<TaskEntity> { it.priority }.then(BY_DUE)
            TaskSort.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
        }

        val rows = mutableListOf<TaskRow>()
        if (filter == TaskFilter.ALL || filter == TaskFilter.TODAY) {
            visible.groupBy(::sectionOf).toSortedMap().forEach { (section, items) ->
                rows += TaskRow.Header(section, items.size)
                val sorted = if (section == TaskSection.DONE) items.sortedByDescending { it.updatedAt } else items.sortedWith(comparator)
                sorted.mapTo(rows) { TaskRow.Item(it) }
            }
        } else {
            val sorted = if (filter == TaskFilter.DONE) visible.sortedByDescending { it.updatedAt } else visible.sortedWith(comparator)
            sorted.mapTo(rows) { TaskRow.Item(it) }
        }

        val dueToday = tasks.filter { it.dueAt?.toLocalDate() == today }
        return TasksUiState(
            rows = rows,
            counts = counts,
            todayTotal = dueToday.size,
            todayDone = dueToday.count { it.completed },
            todayFocusMinutes = focus,
            hasAnyTasks = tasks.isNotEmpty(),
            isSearching = query.isNotEmpty(),
            filter = filter,
            loaded = true
        )
    }

    companion object {
        private const val KEY_FILTER = "filter"
        private const val KEY_SORT = "sort"
        private const val KEY_QUERY = "query"

        /** Earliest due first; tasks without a due date last. */
        val BY_DUE = Comparator<TaskEntity> { a, b ->
            val x = a.dueAt
            val y = b.dueAt
            when {
                x == null && y == null -> 0
                x == null -> 1
                y == null -> -1
                else -> x.compareTo(y)
            }
        }

        /** Default deadline for new tasks: end of the given day, never "right now". */
        fun defaultDue(date: LocalDate = LocalDate.now()): LocalDateTime = date.atTime(LocalTime.of(23, 59))
    }
}
