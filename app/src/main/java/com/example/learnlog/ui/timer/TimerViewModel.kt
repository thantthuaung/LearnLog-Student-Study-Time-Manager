package com.example.learnlog.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.model.AppSettings
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.data.repository.SettingsRepository
import com.example.learnlog.data.repository.TaskRepository
import com.example.learnlog.timer.FocusTimer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import javax.inject.Inject

data class DailyGoal(val focusedMinutes: Int, val goalMinutes: Int) {
    val fraction: Float get() = if (goalMinutes > 0) (focusedMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f) else 0f
    val reached: Boolean get() = goalMinutes in 1..focusedMinutes
}

/** Thin UI layer over the app-wide [FocusTimer]. */
@HiltViewModel
class TimerViewModel @Inject constructor(
    val timer: FocusTimer,
    private val taskRepository: TaskRepository,
    settingsRepository: SettingsRepository,
    sessionLogDao: SessionLogDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val state: StateFlow<FocusTimer.State> = timer.state
    val events = timer.events

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val openTasks: StateFlow<List<TaskEntity>> = taskRepository.getTasks()
        .map { tasks ->
            tasks.filter { !it.completed }
                .sortedWith(compareBy<TaskEntity> { it.dueAt == null }.thenBy { it.dueAt?.toString() ?: "" })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailyGoal: StateFlow<DailyGoal> = combine(
        sessionLogDao.getCompletedSessions().map { sessions ->
            val today = LocalDate.now()
            sessions.filter { it.startTime.toLocalDate() == today }.sumOf { it.durationMinutes }
        },
        userPreferences.dailyGoalMinutes
    ) { focused, goal -> DailyGoal(focused, goal) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyGoal(0, UserPreferences.DEFAULT_DAILY_GOAL))

    val keepScreenOn: Flow<Boolean> = userPreferences.keepScreenOn

    fun setKeepScreenOn(enabled: Boolean) = viewModelScope.launch { userPreferences.updateKeepScreenOn(enabled) }

    suspend fun confirmOnStop(): Boolean = userPreferences.confirmOnStop.first()

    fun link(task: TaskEntity?) = timer.linkTask(task?.toLinked())

    fun markTaskDone(taskId: Long) = viewModelScope.launch { taskRepository.setCompleted(taskId, true) }

    suspend fun shouldAskNotificationPermission(): Boolean = !userPreferences.hasAskedNotificationPermission()
    fun notificationPermissionAsked() = viewModelScope.launch { userPreferences.setAskedNotificationPermission() }

    companion object {
        fun TaskEntity.toLinked() = FocusTimer.LinkedTask(id, title, subject)
    }
}
