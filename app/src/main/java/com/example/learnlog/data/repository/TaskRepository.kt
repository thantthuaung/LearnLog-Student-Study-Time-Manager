package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.entity.SessionLogEntity
import com.example.learnlog.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val sessionLogDao: SessionLogDao
) {

    fun getTasks(): Flow<List<TaskEntity>> = taskDao.getAll()

    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insert(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        // Check if task was just completed
        val oldTask = taskDao.getById(task.id)
        val wasJustCompleted = oldTask != null && !oldTask.completed && task.completed

        // Update the task
        taskDao.update(task)

        // If task was just completed, log a session
        if (wasJustCompleted) {
            val session = SessionLogEntity(
                taskId = task.id,
                subject = task.subject,
                startTime = LocalDateTime.now().minusMinutes(1), // Assume 1 min ago
                endTime = LocalDateTime.now(),
                durationMinutes = 1,
                type = "TASK_COMPLETION",
                isCompleted = true,
                notes = "Task marked complete"
            )
            sessionLogDao.insertSession(session)
        }
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteById(id)
    }

    suspend fun getTaskById(id: Long): TaskEntity? {
        return taskDao.getById(id)
    }

    fun searchTasks(query: String): Flow<List<TaskEntity>> {
        return taskDao.searchTasks(query)
    }
}
