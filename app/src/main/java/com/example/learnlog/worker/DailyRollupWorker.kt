package com.example.learnlog.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.learnlog.data.dao.DailyRollupDao
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.DailyRollupEntity
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Background worker to compute daily rollups for performance optimization.
 * Runs nightly to precompute totals per day.
 */
@HiltWorker
class DailyRollupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sessionLogDao: SessionLogDao,
    private val dailyRollupDao: DailyRollupDao
) : CoroutineWorker(context, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        return try {
            // Compute rollups for the last 90 days
            val today = org.threeten.bp.LocalDate.now()
            val startDateLocal = today.minusDays(90)

            val startDateTime = org.threeten.bp.LocalDateTime.of(startDateLocal, org.threeten.bp.LocalTime.MIN)
            val endDateTime = org.threeten.bp.LocalDateTime.now()

            // Get all sessions in range
            val sessions = sessionLogDao.getSessionsInTimeRange(
                startDateTime.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                endDateTime.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant().toEpochMilli()
            ).first()

            // Group by date
            val sessionsByDate = sessions.groupBy { session ->
                session.startTime.toLocalDate().toString()
            }

            // Compute rollup for each day
            sessionsByDate.forEach { (date, daySessions) ->
                val totalMinutes = daySessions.sumOf { it.durationMinutes }
                val sessionCount = daySessions.size

                // Group by subject
                val subjectBreakdown = daySessions
                    .filter { !it.subject.isNullOrEmpty() }
                    .groupBy { it.subject!! }
                    .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes } }

                val rollup = DailyRollupEntity(
                    date = date,
                    totalMinutes = totalMinutes,
                    sessionCount = sessionCount,
                    subjectBreakdown = gson.toJson(subjectBreakdown),
                    lastUpdated = System.currentTimeMillis()
                )

                dailyRollupDao.insertOrUpdate(rollup)
            }

            // Clean up old rollups (older than 180 days)
            val cutoffDate = org.threeten.bp.LocalDate.now().minusDays(180).toString()
            dailyRollupDao.deleteOldRollups(cutoffDate)

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}

