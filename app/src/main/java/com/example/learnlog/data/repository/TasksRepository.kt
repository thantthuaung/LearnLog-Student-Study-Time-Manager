package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.model.Task
import com.example.learnlog.data.model.TaskStatus
import com.example.learnlog.data.model.TaskPriority
import com.example.learnlog.data.model.TaskType
import com.example.learnlog.util.DateTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TasksRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val dateTimeProvider: DateTimeProvider
) {
    // Now reading from actual Room database instead of in-memory list

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAll().map { entities ->
        entities.map { it.toTask() }
    }

    fun getUpcomingTasks(): Flow<List<Task>> = taskDao.getAll().map { entities ->
        val now = dateTimeProvider.now()
        entities.map { it.toTask() }.filter { task ->
            task.dueDate?.let { it.isAfter(now) } == true && task.status != TaskStatus.COMPLETED
        }
    }

    fun getOverdueTasks(): Flow<List<Task>> = taskDao.getAll().map { entities ->
        val now = dateTimeProvider.now()
        entities.map { it.toTask() }.filter { task ->
            task.dueDate?.let { it.isBefore(now) } == true && task.status != TaskStatus.COMPLETED
        }
    }

    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getAll().map { entities ->
        entities.map { it.toTask() }.filter { task ->
            task.status == TaskStatus.COMPLETED
        }
    }

    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasks(query).map { entities ->
        entities.map { it.toTask() }
    }

    suspend fun addTask(task: Task): Long {
        return taskDao.insert(task.toEntity())
    }

    suspend fun updateTask(task: Task) {
        taskDao.update(task.toEntity())
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteById(taskId)
    }

    // Extension functions to convert between Task and TaskEntity
    private fun TaskEntity.toTask(): Task {
        return Task(
            id = id,
            title = title,
            subject = subject,
            dueDate = dueAt,
            priority = when (priority) {
                3 -> TaskPriority.HIGH
                2 -> TaskPriority.MEDIUM
                else -> TaskPriority.LOW
            },
            type = when (type) {
                "EXAM" -> TaskType.EXAM
                "ASSIGNMENT" -> TaskType.ASSIGNMENT
                "PROJECT" -> TaskType.PROJECT
                "STUDY_SESSION" -> TaskType.STUDY_SESSION
                else -> TaskType.ASSIGNMENT // Default to ASSIGNMENT
            },
            status = when (status) {
                "COMPLETED" -> TaskStatus.COMPLETED
                "IN_PROGRESS" -> TaskStatus.IN_PROGRESS
                else -> TaskStatus.PENDING
            },
            description = notes,
            isNotificationEnabled = false
        )
    }

    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = id,
            title = title,
            subject = subject,
            dueAt = dueDate,
            priority = when (priority) {
                TaskPriority.HIGH -> 3
                TaskPriority.MEDIUM -> 2
                TaskPriority.LOW -> 1
            },
            type = when (type) {
                TaskType.EXAM -> "EXAM"
                TaskType.ASSIGNMENT -> "ASSIGNMENT"
                TaskType.PROJECT -> "PROJECT"
                TaskType.STUDY_SESSION -> "STUDY_SESSION"
            },
            status = when (status) {
                TaskStatus.COMPLETED -> "COMPLETED"
                TaskStatus.IN_PROGRESS -> "IN_PROGRESS"
                TaskStatus.PENDING -> "PENDING"
            },
            completed = status == TaskStatus.COMPLETED,
            notes = description
        )
    }
}
