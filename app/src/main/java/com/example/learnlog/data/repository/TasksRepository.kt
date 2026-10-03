package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.model.Task
import com.example.learnlog.data.model.TaskPriority
import com.example.learnlog.data.model.TaskStatus
import com.example.learnlog.data.model.TaskType
import com.example.learnlog.util.DateTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Read-only domain view of tasks used by Insights. Writes go through [TaskRepository]. */
@Singleton
class TasksRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val dateTimeProvider: DateTimeProvider
) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAll().map { entities ->
        entities.map { it.toTask() }
    }

    fun getUpcomingTasks(): Flow<List<Task>> = getAllTasks().map { tasks ->
        val now = dateTimeProvider.now()
        tasks.filter { it.dueDate?.isAfter(now) == true && it.status != TaskStatus.COMPLETED }
    }

    fun getOverdueTasks(): Flow<List<Task>> = getAllTasks().map { tasks ->
        val now = dateTimeProvider.now()
        tasks.filter { it.dueDate?.isBefore(now) == true && it.status != TaskStatus.COMPLETED }
    }

    private fun TaskEntity.toTask(): Task = Task(
        id = id,
        title = title,
        subject = subject,
        dueDate = dueAt,
        // The UI stores priority as an index into R.array.priority_options (0 low, 1 medium, 2 high)
        priority = when (priority) {
            TaskEntity.PRIORITY_HIGH -> TaskPriority.HIGH
            TaskEntity.PRIORITY_MEDIUM -> TaskPriority.MEDIUM
            else -> TaskPriority.LOW
        },
        type = when (TaskEntity.normalizeType(type)) {
            TaskEntity.TYPE_EXAM -> TaskType.EXAM
            TaskEntity.TYPE_PROJECT -> TaskType.PROJECT
            TaskEntity.TYPE_STUDY -> TaskType.STUDY_SESSION
            else -> TaskType.ASSIGNMENT
        },
        // `completed` is authoritative; older rows may have it set without the status string
        status = when {
            completed || status == TaskEntity.STATUS_COMPLETED -> TaskStatus.COMPLETED
            status == TaskEntity.STATUS_IN_PROGRESS -> TaskStatus.IN_PROGRESS
            else -> TaskStatus.PENDING
        },
        description = notes,
        isNotificationEnabled = false,
        durationMinutes = durationMinutes
    )
}
