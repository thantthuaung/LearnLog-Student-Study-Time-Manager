package com.example.learnlog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/** User-facing settings. Every flag here is read somewhere outside the Settings screen. */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Profile (avatar only; name and email come from the Firebase account)
        val AVATAR_URI = stringPreferencesKey("avatar_uri")

        // Timer
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEEP_RUNNING_IN_BACKGROUND = booleanPreferencesKey("keep_running_in_background")
        val CONFIRM_ON_STOP = booleanPreferencesKey("confirm_on_stop")
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")

        // Notifications
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val NOTIFICATION_VIBRATE = booleanPreferencesKey("notification_vibrate")
        val SHOW_ONGOING_NOTIFICATION = booleanPreferencesKey("show_ongoing_notification")
        val ASKED_NOTIFICATION_PERMISSION = booleanPreferencesKey("asked_notification_permission")

        val LAST_SETTINGS_SECTION = intPreferencesKey("last_settings_section")

        const val SOUND_DEFAULT = "default"
        const val SOUND_ALARM = "alarm"
        const val SOUND_NONE = "none"
        const val DEFAULT_DAILY_GOAL = 120
    }

    val avatarUri: Flow<String?> = dataStore.data.map { it[AVATAR_URI] }

    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_ON] ?: false }
    val keepRunningInBackground: Flow<Boolean> = dataStore.data.map { it[KEEP_RUNNING_IN_BACKGROUND] ?: true }
    val confirmOnStop: Flow<Boolean> = dataStore.data.map { it[CONFIRM_ON_STOP] ?: true }
    val dailyGoalMinutes: Flow<Int> = dataStore.data.map { it[DAILY_GOAL_MINUTES] ?: DEFAULT_DAILY_GOAL }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val notificationSound: Flow<String> = dataStore.data.map { it[NOTIFICATION_SOUND] ?: SOUND_DEFAULT }
    val notificationVibrate: Flow<Boolean> = dataStore.data.map { it[NOTIFICATION_VIBRATE] ?: true }
    val showOngoingNotification: Flow<Boolean> = dataStore.data.map { it[SHOW_ONGOING_NOTIFICATION] ?: true }

    val lastSettingsSection: Flow<Int> = dataStore.data.map { it[LAST_SETTINGS_SECTION] ?: 0 }

    suspend fun snapshot(): Preferences = dataStore.data.first()

    suspend fun updateAvatarUri(uri: String?) = dataStore.edit { p ->
        if (uri != null) p[AVATAR_URI] = uri else p.remove(AVATAR_URI)
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) = dataStore.edit { it[KEEP_SCREEN_ON] = enabled }
    suspend fun updateKeepRunningInBackground(enabled: Boolean) = dataStore.edit { it[KEEP_RUNNING_IN_BACKGROUND] = enabled }
    suspend fun updateConfirmOnStop(enabled: Boolean) = dataStore.edit { it[CONFIRM_ON_STOP] = enabled }
    suspend fun updateDailyGoalMinutes(minutes: Int) = dataStore.edit { it[DAILY_GOAL_MINUTES] = minutes }

    suspend fun updateNotificationsEnabled(enabled: Boolean) = dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    suspend fun updateNotificationSound(sound: String) = dataStore.edit { it[NOTIFICATION_SOUND] = sound }
    suspend fun updateNotificationVibrate(enabled: Boolean) = dataStore.edit { it[NOTIFICATION_VIBRATE] = enabled }
    suspend fun updateShowOngoingNotification(enabled: Boolean) = dataStore.edit { it[SHOW_ONGOING_NOTIFICATION] = enabled }

    suspend fun hasAskedNotificationPermission(): Boolean = snapshot()[ASKED_NOTIFICATION_PERMISSION] ?: false
    suspend fun setAskedNotificationPermission() = dataStore.edit { it[ASKED_NOTIFICATION_PERMISSION] = true }

    suspend fun updateLastSettingsSection(section: Int) = dataStore.edit { it[LAST_SETTINGS_SECTION] = section }
}
