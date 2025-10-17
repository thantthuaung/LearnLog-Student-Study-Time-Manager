package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(private val taskDao: TaskDao) {

    fun getTasks(): Flow<List<TaskEntity>> = taskDao.getAll()

    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insert(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.update(task)
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
