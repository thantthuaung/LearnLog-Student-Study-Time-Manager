package com.example.learnlog.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.dao.DailyRollupDao
import com.example.learnlog.data.dao.SessionLogDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AnalyticsData(
    val totalMinutes: Int = 0,
    val sessionCount: Int = 0,
    val weeklyData: Map<String, Int> = emptyMap(),
    val subjectBreakdown: Map<String, Int> = emptyMap(),
    val topTasks: List<Pair<String, Int>> = emptyList(),
    val isEmpty: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val sessionLogDao: SessionLogDao,
    private val dailyRollupDao: DailyRollupDao,
    private val taskDao: TaskDao
) : ViewModel() {

    enum class DateRange {
        LAST_7_DAYS, LAST_30_DAYS, LAST_90_DAYS
    }

    private val _dateRange = MutableStateFlow(DateRange.LAST_7_DAYS)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val gson = Gson()

    val analyticsData: StateFlow<AnalyticsData> = _dateRange.flatMapLatest { range ->
        val (startDate, endDate) = getDateRangeTimestamps(range)

        combine(
            dailyRollupDao.getRollupsInRange(startDate, endDate),
            sessionLogDao.getSessionsInTimeRange(
                dateFormat.parse(startDate)?.time ?: 0L,
                System.currentTimeMillis()
            ),
            taskDao.getAll()
        ) { rollups, sessions, tasks ->
            // Use rollups as base, supplement with recent sessions
            val totalMinutes = rollups.sumOf { it.totalMinutes } +
                sessions.filter { session ->
                    val sessionDate = session.startTime.toLocalDate().toString()
                    rollups.none { it.date == sessionDate }
                }.sumOf { it.durationMinutes }

            val sessionCount = rollups.sumOf { it.sessionCount } +
                sessions.count { session ->
                    val sessionDate = session.startTime.toLocalDate().toString()
                    rollups.none { it.date == sessionDate }
                }

            // Weekly data (grouped by day)
            val weeklyData = buildWeeklyData(rollups, sessions, range)

            // Subject breakdown
            val subjectBreakdown = buildSubjectBreakdown(rollups, sessions)

            // Top tasks
            val topTasks = buildTopTasks(sessions, tasks)

            AnalyticsData(
                totalMinutes = totalMinutes,
                sessionCount = sessionCount,
                weeklyData = weeklyData,
                subjectBreakdown = subjectBreakdown,
                topTasks = topTasks,
                isEmpty = totalMinutes == 0
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsData()
    )

    fun setDateRange(range: DateRange) {
        _dateRange.value = range
    }

    private fun getDateRangeTimestamps(range: DateRange): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)

        calendar.add(Calendar.DAY_OF_YEAR, when (range) {
            DateRange.LAST_7_DAYS -> -7
            DateRange.LAST_30_DAYS -> -30
            DateRange.LAST_90_DAYS -> -90
        })
        val startDate = dateFormat.format(calendar.time)

        return startDate to endDate
    }

    private fun buildWeeklyData(
        rollups: List<com.example.learnlog.data.entity.DailyRollupEntity>,
        sessions: List<com.example.learnlog.data.entity.SessionLogEntity>,
        range: DateRange
    ): Map<String, Int> {
        val days = when (range) {
            DateRange.LAST_7_DAYS -> 7
            DateRange.LAST_30_DAYS -> 30
            DateRange.LAST_90_DAYS -> 90
        }

        val calendar = Calendar.getInstance()
        val result = mutableMapOf<String, Int>()

        for (i in days - 1 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val date = dateFormat.format(calendar.time)
            val dayName = SimpleDateFormat("EEE", Locale.US).format(calendar.time)

            val rollupMinutes = rollups.find { it.date == date }?.totalMinutes ?: 0
            val sessionMinutes = sessions.filter {
                it.startTime.toLocalDate().toString() == date
            }.sumOf { it.durationMinutes }

            result[dayName] = rollupMinutes + sessionMinutes
        }

        return result
    }

    private fun buildSubjectBreakdown(
        rollups: List<com.example.learnlog.data.entity.DailyRollupEntity>,
        sessions: List<com.example.learnlog.data.entity.SessionLogEntity>
    ): Map<String, Int> {
        val result = mutableMapOf<String, Int>()

        // From rollups
        rollups.forEach { rollup ->
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val breakdown: Map<String, Int> = gson.fromJson(rollup.subjectBreakdown, type)
            breakdown.forEach { (subject, minutes) ->
                result[subject] = (result[subject] ?: 0) + minutes
            }
        }

        // From recent sessions
        sessions.filter { !it.subject.isNullOrEmpty() }
            .groupBy { it.subject!! }
            .forEach { (subject, sessionList) ->
                val minutes = sessionList.sumOf { it.durationMinutes }
                result[subject] = (result[subject] ?: 0) + minutes
            }

        return result.toList()
            .sortedByDescending { it.second }
            .take(10)
            .toMap()
    }

    private fun buildTopTasks(
        sessions: List<com.example.learnlog.data.entity.SessionLogEntity>,
        tasks: List<com.example.learnlog.data.entity.TaskEntity>
    ): List<Pair<String, Int>> {
        val taskMinutes = sessions
            .filter { it.taskId != null }
            .groupBy { it.taskId }
            .mapValues { (_, sessions) -> sessions.sumOf { it.durationMinutes } }

        return taskMinutes.entries
            .mapNotNull { (taskId, minutes) ->
                val task = tasks.find { it.id == taskId }
                task?.let { it.title to minutes }
            }
            .sortedByDescending { it.second }
            .take(5)
    }
}

