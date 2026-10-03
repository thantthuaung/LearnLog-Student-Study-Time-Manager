package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.TaskDao
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

    fun getSubjects(): Flow<List<String>> = taskDao.getSubjects()

    suspend fun insertTask(task: TaskEntity): Long = taskDao.insert(task)

    /** Inserts new tasks (id 0) and updates existing ones. */
    suspend fun saveTask(task: TaskEntity): Long =
        if (task.isNew) {
            taskDao.insert(task.copy(createdAt = LocalDateTime.now(), updatedAt = LocalDateTime.now()))
        } else {
            taskDao.update(task.copy(updatedAt = LocalDateTime.now()))
            task.id
        }

    suspend fun updateTask(task: TaskEntity) {
        // Sessions are only created by the timer, never by editing a task
        taskDao.update(task.copy(updatedAt = LocalDateTime.now()))
    }

    suspend fun setCompleted(taskId: Long, done: Boolean) {
        taskDao.getById(taskId)?.let { taskDao.update(it.withCompleted(done)) }
    }

    suspend fun markInProgress(taskId: Long) {
        taskDao.getById(taskId)?.let { if (!it.completed) taskDao.update(it.withInProgress()) }
    }

    suspend fun deleteTask(id: Long) = taskDao.deleteById(id)

    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getById(id)

    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)
}
