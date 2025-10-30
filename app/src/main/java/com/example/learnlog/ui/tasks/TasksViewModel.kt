package com.example.learnlog.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(TaskFilter.ALL)
    private val _sort = MutableStateFlow(TaskSort.DUE_DATE)
    private val _searchQuery = MutableStateFlow("")
    private val _tasks = taskRepository.getTasks()

    val uiState: StateFlow<List<TaskEntity>> = combine(_tasks, _filter, _sort, _searchQuery) { tasks, filter, sort, query ->
        // First apply search filter
        val searchFiltered = if (query.isBlank()) {
            tasks
        } else {
            tasks.filter { task ->
                task.title.contains(query, ignoreCase = true) ||
                task.subject?.contains(query, ignoreCase = true) == true
            }
        }

        // Then apply status filter
        val filteredTasks = when (filter) {
            TaskFilter.ALL -> searchFiltered
            TaskFilter.DUE -> searchFiltered.filter { !it.completed && it.dueAt != null }
            TaskFilter.COMPLETED -> searchFiltered.filter { it.completed }
        }

        // Finally apply sorting
        when (sort) {
            TaskSort.DUE_DATE -> filteredTasks.sortedBy { it.dueAt }
            TaskSort.PRIORITY -> filteredTasks.sortedByDescending { it.priority }
            TaskSort.TITLE -> filteredTasks.sortedBy { it.title }
        }
    }.let {
        val flow = MutableStateFlow<List<TaskEntity>>(emptyList())
        viewModelScope.launch {
            it.collect { value ->
                flow.value = value
            }
        }
        flow
    }

    fun setFilter(filter: TaskFilter) {
        _filter.value = filter
    }

    fun setSort(sort: TaskSort) {
        _sort.value = sort
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleComplete(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(completed = completed))
        }
    }

    fun delete(taskId: Long) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun saveNew(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insertTask(task)
        }
    }

    fun updateExisting(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun insert(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insertTask(task)
        }
    }

    fun markAsInProgress(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(status = "IN_PROGRESS"))
        }
    }

    fun deleteTaskWithUndo(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task.id)
        }
    }

    fun restoreTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.insertTask(task)
        }
    }
}
