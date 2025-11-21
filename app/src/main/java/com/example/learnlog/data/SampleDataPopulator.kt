package com.example.learnlog.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.entity.SessionLogEntity
import kotlinx.coroutines.flow.first
import org.threeten.bp.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sampleDataStore by preferencesDataStore(name = "sample_data")

@Singleton
class SampleDataPopulator @Inject constructor(
    private val taskDao: TaskDao,
    private val sessionLogDao: SessionLogDao,
    private val context: Context
) {
    private val SAMPLE_DATA_POPULATED_KEY = booleanPreferencesKey("sample_data_populated")

    suspend fun populateIfNeeded() {
        val preferences = context.sampleDataStore.data.first()
        val isPopulated = preferences[SAMPLE_DATA_POPULATED_KEY] ?: false

        if (!isPopulated) {
            populateSampleData()
            context.sampleDataStore.edit { prefs ->
                prefs[SAMPLE_DATA_POPULATED_KEY] = true
            }
        }
    }

    private suspend fun populateSampleData() {
        val now = LocalDateTime.now()

        // Add sample tasks (4 tasks: 1 completed, 3 pending/in-progress)
        val tasks = listOf(
            TaskEntity(
                title = "Complete Math Assignment",
                subject = "Mathematics",
                dueAt = now.plusDays(2).withHour(23).withMinute(59),
                priority = 1, // Medium
                status = "IN_PROGRESS",
                progress = 60,
                completed = false,
                type = "ASSIGNMENT",
                createdAt = now.minusDays(3),
                updatedAt = now
            ),
            TaskEntity(
                title = "Study for Physics Exam",
                subject = "Physics",
                dueAt = now.plusDays(5).withHour(9).withMinute(0),
                priority = 2, // High
                status = "PENDING",
                progress = 30,
                completed = false,
                type = "EXAM",
                createdAt = now.minusDays(2),
                updatedAt = now
            ),
            TaskEntity(
                title = "Read History Chapter 5",
                subject = "History",
                dueAt = now.minusDays(1).withHour(23).withMinute(59),
                priority = 0, // Low
                status = "COMPLETED",
                progress = 100,
                completed = true,
                type = "READING",
                createdAt = now.minusDays(4),
                updatedAt = now.minusDays(1)
            ),
            TaskEntity(
                title = "Chemistry Lab Report",
                subject = "Chemistry",
                dueAt = now.plusDays(3).withHour(17).withMinute(0),
                priority = 1, // Medium
                status = "PENDING",
                progress = 0,
                completed = false,
                type = "LAB",
                createdAt = now.minusDays(1),
                updatedAt = now
            )
        )

        tasks.forEach { task ->
            taskDao.insert(task)
        }

        // Add sample study sessions (total 4h 30min of study time)
        val sessions = listOf(
            // Today: 45 minutes
            SessionLogEntity(
                taskId = 1,
                subject = "Mathematics",
                startTime = now.withHour(9).withMinute(0),
                endTime = now.withHour(9).withMinute(45),
                durationMinutes = 45,
                type = "FOCUS",
                isCompleted = true,
                notes = "Practice problems"
            ),
            // Yesterday: 2 hours 15 minutes total
            SessionLogEntity(
                taskId = 2,
                subject = "Physics",
                startTime = now.minusDays(1).withHour(10).withMinute(0),
                endTime = now.minusDays(1).withHour(11).withMinute(15),
                durationMinutes = 75,
                type = "FOCUS",
                isCompleted = true,
                notes = "Reviewed mechanics"
            ),
            SessionLogEntity(
                taskId = 3,
                subject = "History",
                startTime = now.minusDays(1).withHour(16).withMinute(0),
                endTime = now.minusDays(1).withHour(17).withMinute(0),
                durationMinutes = 60,
                type = "FOCUS",
                isCompleted = true,
                notes = "Chapter reading completed"
            ),
            // 2 days ago: 1 hour 30 minutes
            SessionLogEntity(
                taskId = 1,
                subject = "Mathematics",
                startTime = now.minusDays(2).withHour(14).withMinute(0),
                endTime = now.minusDays(2).withHour(15).withMinute(30),
                durationMinutes = 90,
                type = "FOCUS",
                isCompleted = true,
                notes = "Worked on algebra problems"
            )
        )

        sessions.forEach { session ->
            sessionLogDao.insertSession(session)
        }
    }
}

