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
    private val _tasks = taskRepository.getTasks()

    val uiState: StateFlow<List<TaskEntity>> = combine(_tasks, _filter, _sort) { tasks, filter, sort ->
        val filteredTasks = when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.DUE -> tasks.filter { !it.completed && it.dueAt != null }
            TaskFilter.COMPLETED -> tasks.filter { it.completed }
        }
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
