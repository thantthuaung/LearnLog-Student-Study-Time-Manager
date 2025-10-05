package com.example.learnlog.data.repository

import com.example.learnlog.data.model.Task
import com.example.learnlog.data.model.TaskStatus
import com.example.learnlog.data.model.TaskPriority
import com.example.learnlog.data.model.TaskType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.Date
import java.util.Calendar

class TasksRepository {
    private val tasksFlow = MutableStateFlow<List<Task>>(emptyList())

    init {
        // Add sample tasks
        addSampleTasks()
    }

    private fun addSampleTasks() {
        val calendar = Calendar.getInstance()

        // Add an upcoming task
        calendar.add(Calendar.DAY_OF_MONTH, 2)
        addTask(
            Task(
                id = 1,
                title = "Math Assignment",
                subject = "Mathematics",
                dueDate = calendar.time,
                priority = TaskPriority.HIGH,
                type = TaskType.ASSIGNMENT,
                status = TaskStatus.IN_PROGRESS,
                progress = 60,
                description = "Complete exercises 1-10"
            )
        )

        // Add an overdue task
        calendar.add(Calendar.DAY_OF_MONTH, -4)
        addTask(
            Task(
                id = 2,
                title = "Physics Lab Report",
                subject = "Physics",
                dueDate = calendar.time,
                priority = TaskPriority.MEDIUM,
                type = TaskType.ASSIGNMENT,
                status = TaskStatus.PENDING,
                progress = 30,
                description = "Write lab report for experiment"
            )
        )

        // Add a completed task
        addTask(
            Task(
                id = 3,
                title = "English Essay",
                subject = "English",
                dueDate = Date(),
                priority = TaskPriority.LOW,
                type = TaskType.ASSIGNMENT,
                status = TaskStatus.COMPLETED,
                progress = 100,
                description = "Essay on Shakespeare"
            )
        )
    }

    fun getAllTasks(): Flow<List<Task>> = tasksFlow

    fun getUpcomingTasks(): Flow<List<Task>> = tasksFlow.map { tasks ->
        tasks.filter { task ->
            task.dueDate.after(Date()) && task.status != TaskStatus.COMPLETED
        }
    }

    fun getOverdueTasks(): Flow<List<Task>> = tasksFlow.map { tasks ->
        tasks.filter { task ->
            task.dueDate.before(Date()) && task.status != TaskStatus.COMPLETED
        }
    }

    fun getCompletedTasks(): Flow<List<Task>> = tasksFlow.map { tasks ->
        tasks.filter { task -> task.status == TaskStatus.COMPLETED }
    }

    fun searchTasks(query: String): Flow<List<Task>> = tasksFlow.map { tasks ->
        tasks.filter { task ->
            task.title.contains(query, ignoreCase = true) ||
                    task.subject.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true)
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
