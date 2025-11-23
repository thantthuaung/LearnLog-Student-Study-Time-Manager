package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.DailyRollupDao
import com.example.learnlog.data.dao.NoteDao
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.SubjectDao
import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataResetRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val taskDao: TaskDao,
    private val sessionLogDao: SessionLogDao,
    private val noteDao: NoteDao,
    private val subjectDao: SubjectDao,
    private val dailyRollupDao: DailyRollupDao
) {

    /**
     * Clears all data from the database
     * WARNING: This action cannot be undone!
     */
    suspend fun resetAllData() {
        withContext(Dispatchers.IO) {
            appDatabase.clearAllTables()
        }
    }
}

