package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.model.SessionLog
import com.example.learnlog.data.model.TaskStatus
import com.example.learnlog.ui.insights.DateRange
import com.example.learnlog.ui.insights.InsightsData
import com.example.learnlog.ui.insights.PlannedVsActual
import com.example.learnlog.ui.insights.TopTask
import com.example.learnlog.util.DateTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import org.threeten.bp.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightsRepository @Inject constructor(
    private val sessionLogDao: SessionLogDao,
    private val tasksRepository: TasksRepository,
    private val plannerRepository: PlannerRepository,
    private val dateTimeProvider: DateTimeProvider
) {

    fun getInsightsData(dateRange: DateRange, customStart: LocalDate? = null, customEnd: LocalDate? = null): Flow<InsightsData> {
        val (startDate, endDate) = getDateRangeFor(dateRange, customStart, customEnd)

        return combine(
            tasksRepository.getAllTasks(),
            plannerRepository.getSessionsForWeek(startDate.atStartOfDay())
        ) { tasks, plannedSessions ->

            // For now, return mock data since session logs might be empty
            // In production, fetch from sessionLogDao.getSessionsInTimeRange()
            val sessions = emptyList<SessionLog>() // TODO: Fetch real sessions

            // Total focus time
            val totalFocusMinutes = sessions.sumOf { it.focusMinutes }

            // Time by subject
            val timeBySubject = sessions
                .groupBy { session ->
                    tasks.find { it.id == session.taskId }?.subject ?: "Other"
                }
                .mapValues { (_, sessions) -> sessions.sumOf { it.focusMinutes } }
                .filter { it.value > 0 }

            // Current streak
            val streak = calculateStreak(sessions)

            // Planned vs actual
            val filteredPlanned = plannedSessions.filter { session ->
                val sessionDate = session.startTime.toLocalDate()
                !sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate)
            }
            val totalPlanned = filteredPlanned.sumOf { it.durationMinutes }
            val totalActual = totalFocusMinutes

            // Completion rate
            val tasksInRange = tasks.filter { task ->
                task.dueDate?.let { dueDate ->
                    val dueLocalDate = dueDate.toLocalDate()
                    !dueLocalDate.isBefore(startDate) && !dueLocalDate.isAfter(endDate)
                } ?: false
            }
            val completedCount = tasksInRange.count { it.status == TaskStatus.COMPLETED }
            val totalCount = tasksInRange.size
            val completionRate = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

            // Planned vs actual by day
            val plannedByDay = filteredPlanned
                .groupBy { it.startTime.toLocalDate() }
                .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes } }

            val actualByDay = sessions
                .groupBy { it.startTime.toLocalDate() }
                .mapValues { (_, sessions) -> sessions.sumOf { it.focusMinutes } }

            val allDates = (plannedByDay.keys + actualByDay.keys).toSet()
            val plannedVsActualByDay = allDates.associate { date ->
                date to PlannedVsActual(
                    plannedMinutes = plannedByDay[date] ?: 0,
                    actualMinutes = actualByDay[date] ?: 0
                )
            }.toSortedMap()

            // Top tasks by time
            val topTasks = sessions
                .filter { it.taskId != null }
                .groupBy { it.taskId }
                .mapNotNull { (taskId, taskSessions) ->
                    val task = tasks.find { it.id == taskId }
                    task?.let {
                        TopTask(
                            taskId = it.id,
                            title = it.title,
                            subject = it.subject,
                            totalMinutes = taskSessions.sumOf { s -> s.focusMinutes }
                        )
                    }
                }
                .sortedByDescending { it.totalMinutes }
                .take(5)

            // Interruptions (count sessions with breaks)
            val totalInterruptions = sessions.count { it.breakMinutes > 0 }
            val avgSessionMinutes = if (sessions.isNotEmpty())
                sessions.sumOf { it.focusMinutes } / sessions.size
            else 0

            InsightsData(
                totalFocusMinutes = totalFocusMinutes,
                timeBySubject = timeBySubject,
                currentStreak = streak,
                plannedMinutes = totalPlanned,
                actualMinutes = totalActual,
                completionRate = completionRate,
                totalTasks = totalCount,
                completedTasks = completedCount,
                plannedVsActualByDay = plannedVsActualByDay,
                topTasks = topTasks,
                totalInterruptions = totalInterruptions,
                avgSessionMinutes = avgSessionMinutes,
                dateRange = dateRange
            )
        }
    }

    private fun getDateRangeFor(dateRange: DateRange, customStart: LocalDate?, customEnd: LocalDate?): Pair<LocalDate, LocalDate> {
        val now = dateTimeProvider.now()
        return when (dateRange) {
            DateRange.TODAY -> {
                val today = now.toLocalDate()
                today to today
            }
            DateRange.WEEK -> {
                val today = now.toLocalDate()
                val startOfWeek = today.minusDays(today.dayOfWeek.value.toLong() - 1)
                startOfWeek to startOfWeek.plusDays(6)
            }
            DateRange.MONTH -> {
                val today = now.toLocalDate()
                val startOfMonth = today.withDayOfMonth(1)
                val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
                startOfMonth to endOfMonth
            }
            DateRange.CUSTOM -> {
                (customStart ?: now.toLocalDate()) to (customEnd ?: now.toLocalDate())
            }
        }
    }

    private fun calculateStreak(sessions: List<SessionLog>): Int {
        if (sessions.isEmpty()) return 0

        val today = dateTimeProvider.now().toLocalDate()
        val sessionsByDate = sessions
            .groupBy { it.startTime.toLocalDate() }
            .mapValues { (_, sessions) -> sessions.sumOf { it.focusMinutes } }
            .filterValues { it >= 25 } // At least 25 minutes to count

        var streak = 0
        var currentDate = today

        while (sessionsByDate.containsKey(currentDate)) {
            streak++
            currentDate = currentDate.minusDays(1)
        }

        return streak
    }
}
