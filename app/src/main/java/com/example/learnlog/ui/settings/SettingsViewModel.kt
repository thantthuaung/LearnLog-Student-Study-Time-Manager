package com.example.learnlog.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.auth.AuthManager
import com.example.learnlog.data.dao.NoteDao
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.TaskDao
import com.example.learnlog.data.model.AppSettings
import com.example.learnlog.data.model.TimerPreset
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.data.repository.DataResetRepository
import com.example.learnlog.data.repository.SettingsRepository
import com.example.learnlog.util.ThemePrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DataCounts(val tasks: Int = 0, val sessions: Int = 0, val notes: Int = 0)

/** Shared by all settings sections (scoped to the Settings screen). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences,
    private val settingsRepository: SettingsRepository,
    private val dataResetRepository: DataResetRepository,
    val authManager: AuthManager,
    taskDao: TaskDao,
    sessionLogDao: SessionLogDao,
    noteDao: NoteDao
) : ViewModel() {

    // Account
    val avatarUri: Flow<String?> = userPreferences.avatarUri
    fun updateAvatarUri(uri: String?) = viewModelScope.launch { userPreferences.updateAvatarUri(uri) }
    suspend fun updateDisplayName(name: String): Result<Unit> = authManager.updateDisplayName(name)
    suspend fun sendPasswordReset(): Result<Unit> =
        authManager.currentUser?.email?.let { authManager.sendPasswordResetEmail(it) }
            ?: Result.failure(IllegalStateException("No email on this account"))

    // Appearance
    var themeMode: Int
        get() = ThemePrefs.get(context)
        set(value) = ThemePrefs.set(context, value)

    // Timer
    val appSettings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    fun setDefaultPreset(index: Int) = viewModelScope.launch { settingsRepository.updateDefaultPresetIndex(index) }
    fun setPresets(minutes: List<Int>) = viewModelScope.launch {
        val current = appSettings.value
        val defaultMinutes = current.defaultPreset.durationMinutes
        val presets = minutes.distinct().sorted().map { TimerPreset(it) }
        settingsRepository.updateTimerPresets(presets)
        // Keep the same default duration if it still exists
        settingsRepository.updateDefaultPresetIndex(presets.indexOfFirst { it.durationMinutes == defaultMinutes }.coerceAtLeast(0))
    }

    val dailyGoal: Flow<Int> = userPreferences.dailyGoalMinutes
    fun setDailyGoal(minutes: Int) = viewModelScope.launch { userPreferences.updateDailyGoalMinutes(minutes) }
    val keepScreenOn: Flow<Boolean> = userPreferences.keepScreenOn
    fun setKeepScreenOn(v: Boolean) = viewModelScope.launch { userPreferences.updateKeepScreenOn(v) }
    val keepRunning: Flow<Boolean> = userPreferences.keepRunningInBackground
    fun setKeepRunning(v: Boolean) = viewModelScope.launch { userPreferences.updateKeepRunningInBackground(v) }
    val confirmOnStop: Flow<Boolean> = userPreferences.confirmOnStop
    fun setConfirmOnStop(v: Boolean) = viewModelScope.launch { userPreferences.updateConfirmOnStop(v) }

    // Alerts
    val notificationsEnabled: Flow<Boolean> = userPreferences.notificationsEnabled
    fun setNotificationsEnabled(v: Boolean) = viewModelScope.launch { userPreferences.updateNotificationsEnabled(v) }
    val notificationSound: Flow<String> = userPreferences.notificationSound
    fun setNotificationSound(v: String) = viewModelScope.launch { userPreferences.updateNotificationSound(v) }
    val vibrate: Flow<Boolean> = userPreferences.notificationVibrate
    fun setVibrate(v: Boolean) = viewModelScope.launch { userPreferences.updateNotificationVibrate(v) }
    val ongoing: Flow<Boolean> = userPreferences.showOngoingNotification
    fun setOngoing(v: Boolean) = viewModelScope.launch { userPreferences.updateShowOngoingNotification(v) }

    // Data
    val counts: StateFlow<DataCounts> = combine(
        taskDao.getAll().map { it.size },
        sessionLogDao.getCompletedSessions().map { it.size },
        noteDao.getAllNotes().map { it.size }
    ) { t, s, n -> DataCounts(t, s, n) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataCounts())

    suspend fun resetAllData() = dataResetRepository.resetAllData()

    fun saveLastSection(section: Int) = viewModelScope.launch { userPreferences.updateLastSettingsSection(section) }
}
