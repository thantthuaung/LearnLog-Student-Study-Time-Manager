package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.SessionLogEntity
import com.example.learnlog.data.model.TaskStatus
import com.example.learnlog.ui.insights.DateRange
import com.example.learnlog.ui.insights.InsightsData
import com.example.learnlog.ui.insights.PlannedVsActual
import com.example.learnlog.ui.insights.TopTask
import com.example.learnlog.util.DateTimeProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId
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

        val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        return combine(
            tasksRepository.getAllTasks(),
            plannerRepository.getSessionsForWeek(startDate.atStartOfDay()),
            sessionLogDao.getSessionsInTimeRange(startTimestamp, endTimestamp)
        ) { tasks, plannedSessions, sessionEntities ->

            val totalFocusMinutes = sessionEntities
                .filter { it.isCompleted }
                .sumOf { it.durationMinutes }

            val timeBySubject = sessionEntities
                .filter { it.isCompleted }
                .groupBy { session ->
                    session.taskId?.let { taskId ->
                        tasks.find { it.id == taskId }?.subject
                    } ?: session.subject ?: "General Study"
                }
                .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes } }
                .filter { it.value > 0 }

            val streak = calculateStreak(sessionEntities)

            val filteredPlanned = plannedSessions.filter { session ->
                val sessionDate = session.startTime.toLocalDate()
                !sessionDate.isBefore(startDate) && !sessionDate.isAfter(endDate)
            }
            val totalPlanned = filteredPlanned.sumOf { it.durationMinutes }
            val totalActual = totalFocusMinutes

            val tasksInRange = tasks.filter { task ->
                task.dueDate?.let { dueDateTime ->
                    val dueLocalDate = dueDateTime.toLocalDate()
                    !dueLocalDate.isBefore(startDate) && !dueLocalDate.isAfter(endDate)
                } ?: false
            }
            val completedCount = tasksInRange.count { it.status == TaskStatus.COMPLETED }
            val totalCount = tasksInRange.size
            val completionRate = if (totalCount > 0) {
                completedCount.toFloat() / totalCount.toFloat()
            } else {
                0f
            }

            val plannedByDay = filteredPlanned
                .groupBy { it.startTime.toLocalDate() }
                .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes } }

            val actualByDay = sessionEntities
                .filter { it.isCompleted }
                .groupBy { it.startTime.toLocalDate() }
                .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes } }

            val allDates = (plannedByDay.keys + actualByDay.keys).toSet()
            val plannedVsActualByDay = allDates.associate { date ->
                date to PlannedVsActual(
                    plannedMinutes = plannedByDay[date] ?: 0,
                    actualMinutes = actualByDay[date] ?: 0
                )
            }.toSortedMap()

            val topTasks = sessionEntities
                .filter { it.taskId != null && it.isCompleted }
                .groupBy { it.taskId }
                .mapNotNull { (taskId, taskSessions) ->
                    val task = tasks.find { it.id == taskId }
                    task?.let {
                        TopTask(
                            taskId = it.id,
                            title = it.title,
                            subject = it.subject ?: "",
                            totalMinutes = taskSessions.sumOf { s -> s.durationMinutes }
                        )
                    }
                }
                .sortedByDescending { it.totalMinutes }
                .take(5)

            val completedSessions = sessionEntities.filter { it.isCompleted }
            val totalInterruptions = 0
            val avgSessionMinutes = if (completedSessions.isNotEmpty())
                completedSessions.sumOf { it.durationMinutes } / completedSessions.size
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

    private fun calculateStreak(sessions: List<SessionLogEntity>): Int {
        if (sessions.isEmpty()) return 0

        val today = dateTimeProvider.now().toLocalDate()

        val sessionsByDate = sessions
            .filter { it.isCompleted }
            .groupBy { it.startTime.toLocalDate() }
            .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes } }
            .filterValues { it > 0 }

        var streak = 0
        var currentDate = today

        while (sessionsByDate.containsKey(currentDate)) {
            streak++
            currentDate = currentDate.minusDays(1)
        }

        return streak
    }
}

