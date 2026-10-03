package com.example.learnlog.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.learnlog.data.dao.DailyRollupDao
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.DailyRollupEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import dagger.Lazy
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId

/**
 * Nightly job that stores per-day totals of completed focus sessions (last 90 days) and
 * prunes rollups older than 180 days.
 */
@HiltWorker
class DailyRollupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    // Lazy: the per-account database must not be opened before someone has signed in
    private val sessionLogDao: Lazy<SessionLogDao>,
    private val dailyRollupDao: Lazy<DailyRollupDao>
) : CoroutineWorker(context, params) {

    private val gson = Gson()

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null) return Result.success()
        return try {
            val zone = ZoneId.systemDefault()
            val start = LocalDateTime.of(LocalDate.now().minusDays(90), LocalTime.MIN)
            val sessions = sessionLogDao.get().getCompletedInRange(
                start.atZone(zone).toInstant().toEpochMilli(),
                LocalDateTime.now().atZone(zone).toInstant().toEpochMilli()
            ).first()

            sessions.groupBy { it.startTime.toLocalDate().toString() }.forEach { (date, daySessions) ->
                val breakdown = daySessions
                    .groupBy { it.subject?.takeIf { s -> s.isNotBlank() } ?: "General" }
                    .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
                dailyRollupDao.get().insertOrUpdate(
                    DailyRollupEntity(
                        date = date,
                        totalMinutes = daySessions.sumOf { it.durationMinutes },
                        sessionCount = daySessions.size,
                        subjectBreakdown = gson.toJson(breakdown),
                        lastUpdated = System.currentTimeMillis()
                    )
                )
            }
            dailyRollupDao.get().deleteOldRollups(LocalDate.now().minusDays(180).toString())
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
