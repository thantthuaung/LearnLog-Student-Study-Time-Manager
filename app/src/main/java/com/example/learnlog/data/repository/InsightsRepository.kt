package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.SessionLogEntity
import com.example.learnlog.data.model.Task
import com.example.learnlog.data.model.TaskStatus
import com.example.learnlog.ui.insights.DateRange
import com.example.learnlog.ui.insights.InsightsData
import com.example.learnlog.ui.insights.PlannedVsActual
import com.example.learnlog.ui.insights.TopTask
import com.example.learnlog.util.DateTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsRepository @Inject constructor(
    private val sessionLogDao: SessionLogDao,
    private val tasksRepository: TasksRepository,
    private val dateTimeProvider: DateTimeProvider
) {

    fun getInsightsData(
        dateRange: DateRange,
        customStart: LocalDate? = null,
        customEnd: LocalDate? = null
    ): Flow<InsightsData> {
        val (startDate, endDate) = getDateRangeFor(dateRange, customStart, customEnd)

        return combine(
            tasksRepository.getAllTasks(),
            sessionLogDao.getCompletedSessions()
        ) { tasks, allSessions ->
            val tasksById = tasks.associateBy { it.id }
            val sessions = allSessions.filter { it.startTime.toLocalDate().inRange(startDate, endDate) }

            val totalFocusMinutes = sessions.sumOf { it.durationMinutes }

            val timeBySubject = sessions
                .groupBy { subjectOf(it, tasksById) }
                .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
                .filterValues { it > 0 }
                .toList()
                .sortedByDescending { it.second }
                .let { sorted ->
                    if (sorted.size <= MAX_SUBJECT_SLICES) sorted
                    else sorted.take(MAX_SUBJECT_SLICES - 1) +
                        (OTHER to sorted.drop(MAX_SUBJECT_SLICES - 1).sumOf { it.second })
                }

            val tasksInRange = tasks.filter { it.dueDate?.toLocalDate()?.inRange(startDate, endDate) == true }
            val completedCount = tasksInRange.count { it.status == TaskStatus.COMPLETED }
            val plannedByDay = tasksInRange
                .groupBy { it.dueDate!!.toLocalDate() }
                .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }
            val actualByDay = sessions
                .groupBy { it.startTime.toLocalDate() }
                .mapValues { (_, list) -> list.sumOf { it.durationMinutes } }

            val days = ChronoUnit.DAYS.between(startDate, endDate).toInt() + 1
            val chartDates = if (days <= MAX_DENSE_DAYS) {
                (0 until days).map { startDate.plusDays(it.toLong()) }
            } else {
                (plannedByDay.keys + actualByDay.keys).sorted()
            }
            val plannedVsActualByDay = chartDates.associateWith { date ->
                PlannedVsActual(plannedByDay[date] ?: 0, actualByDay[date] ?: 0)
            }.toSortedMap()

            val topTasks = sessions
                .filter { it.taskId != null }
                .groupBy { it.taskId!! }
                .mapNotNull { (taskId, list) ->
                    tasksById[taskId]?.let { task ->
                        TopTask(task.id, task.title, task.subject, list.sumOf { it.durationMinutes })
                    }
                }
                .sortedByDescending { it.totalMinutes }
                .take(5)

            val (current, best) = calculateStreaks(allSessions)

            InsightsData(
                totalFocusMinutes = totalFocusMinutes,
                sessionCount = sessions.size,
                avgSessionMinutes = if (sessions.isNotEmpty()) totalFocusMinutes / sessions.size else 0,
                timeBySubject = timeBySubject,
                currentStreak = current,
                bestStreak = best,
                plannedMinutes = plannedByDay.values.sum(),
                actualMinutes = totalFocusMinutes,
                completionRate = if (tasksInRange.isNotEmpty()) completedCount.toFloat() / tasksInRange.size else 0f,
                totalTasks = tasksInRange.size,
                completedTasks = completedCount,
                plannedVsActualByDay = plannedVsActualByDay,
                topTasks = topTasks,
                dateRange = dateRange,
                rangeStart = startDate,
                rangeEnd = endDate,
                isLoaded = true
            )
        }
    }

    private fun subjectOf(session: SessionLogEntity, tasksById: Map<Long, Task>): String =
        session.taskId?.let { tasksById[it]?.subject }?.takeIf { it.isNotBlank() }
            ?: session.subject?.takeIf { it.isNotBlank() && it != LEGACY_STANDALONE_LABEL }
            ?: GENERAL

    private fun LocalDate.inRange(start: LocalDate, end: LocalDate) = !isBefore(start) && !isAfter(end)

    private fun getDateRangeFor(
        dateRange: DateRange,
        customStart: LocalDate?,
        customEnd: LocalDate?
    ): Pair<LocalDate, LocalDate> {
        val today = dateTimeProvider.now().toLocalDate()
        return when (dateRange) {
            DateRange.TODAY -> today to today
            DateRange.WEEK -> {
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                startOfWeek to startOfWeek.plusDays(6)
            }
            DateRange.MONTH -> today.withDayOfMonth(1) to today.withDayOfMonth(today.lengthOfMonth())
            DateRange.CUSTOM -> {
                val start = customStart ?: today
                val end = customEnd ?: today
                if (end.isBefore(start)) end to start else start to end
            }
        }
    }

    /**
     * Streaks are computed over all history, independent of the selected range. The current
     * streak stays alive through today until midnight if yesterday had a session.
     */
    private fun calculateStreaks(sessions: List<SessionLogEntity>): Pair<Int, Int> {
        val studyDays = sessions
            .filter { it.durationMinutes > 0 }
            .map { it.startTime.toLocalDate() }
            .toSortedSet()
        if (studyDays.isEmpty()) return 0 to 0

        val today = dateTimeProvider.now().toLocalDate()
        var cursor = if (today in studyDays) today else today.minusDays(1)
        var current = 0
        while (cursor in studyDays) {
            current++
            cursor = cursor.minusDays(1)
        }

        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in studyDays) {
            run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return current to best
    }

    companion object {
        const val GENERAL = "General"
        const val OTHER = "Other"
        private const val LEGACY_STANDALONE_LABEL = "Focus Session"
        private const val MAX_SUBJECT_SLICES = 6
        private const val MAX_DENSE_DAYS = 31
    }
}
