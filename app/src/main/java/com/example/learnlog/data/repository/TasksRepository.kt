package com.example.learnlog.data.repository

import com.example.learnlog.data.model.Task
import com.example.learnlog.data.model.TaskStatus
import com.example.learnlog.data.model.TaskPriority
import com.example.learnlog.data.model.TaskType
import com.example.learnlog.util.DateTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRepository @Inject constructor(
    private val dateTimeProvider: DateTimeProvider
) {
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    init {
        // Add sample tasks
        addSampleTasks()
    }

    private fun addSampleTasks() {
        val now = dateTimeProvider.now()

        // Add an upcoming task
        addTask(
            Task(
                id = 1,
                title = "Math Assignment",
                subject = "Mathematics",
                dueDate = now.plusDays(2),
                priority = TaskPriority.HIGH,
                type = TaskType.ASSIGNMENT,
                status = TaskStatus.IN_PROGRESS,
                description = "Complete exercises 1-10",
                isNotificationEnabled = false
            )
        )

        // Add an overdue task
        addTask(
            Task(
                id = 2,
                title = "Physics Lab Report",
                subject = "Physics",
                dueDate = now.minusDays(2),
                priority = TaskPriority.MEDIUM,
                type = TaskType.ASSIGNMENT,
                status = TaskStatus.PENDING,
                description = "Write lab report for experiment",
                isNotificationEnabled = false
            )
        )

        // Add a completed task
        addTask(
            Task(
                id = 3,
                title = "English Essay",
                subject = "English",
                dueDate = now,
                priority = TaskPriority.LOW,
                type = TaskType.ASSIGNMENT,
                status = TaskStatus.COMPLETED,
                description = "Essay on Shakespeare",
                isNotificationEnabled = false
            )
        )
    }

    fun getAllTasks(): Flow<List<Task>> = tasksFlow

    fun getUpcomingTasks(): Flow<List<Task>> = tasksFlow.map { tasks ->
        val now = dateTimeProvider.now()
        tasks.filter { task ->
            task.dueDate?.let { it.isAfter(now) } == true && task.status != TaskStatus.COMPLETED
        }
    }

    fun getOverdueTasks(): Flow<List<Task>> = tasksFlow.map { tasks ->
        val now = dateTimeProvider.now()
        tasks.filter { task ->
            task.dueDate?.let { it.isBefore(now) } == true && task.status != TaskStatus.COMPLETED
        }
    }

    fun getCompletedTasks(): Flow<List<Task>> = tasksFlow.map { tasks ->
        tasks.filter { task -> task.status == TaskStatus.COMPLETED }
    }

    fun searchTasks(query: String): Flow<List<Task>> = tasksFlow.map { tasks ->
        tasks.filter { task ->
            task.title.contains(query, ignoreCase = true) ||
                    task.subject?.contains(query, ignoreCase = true) == true ||
                    task.description?.contains(query, ignoreCase = true) == true
        }
    }

    fun addTask(task: Task) {
        val currentTasks = tasksFlow.value.toMutableList()
        currentTasks.add(task)
        tasksFlow.value = currentTasks
    }

    fun updateTask(task: Task) {
        val currentTasks = tasksFlow.value.toMutableList()
        val index = currentTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            currentTasks[index] = task
            tasksFlow.value = currentTasks
        }
    }

    fun deleteTask(taskId: Long) {
        val currentTasks = tasksFlow.value.toMutableList()
        currentTasks.removeAll { it.id == taskId }
        tasksFlow.value = currentTasks
    }
}
