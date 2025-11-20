package com.example.learnlog.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferences @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        // Profile
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val EMAIL = stringPreferencesKey("email")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")

        // Timer Preferences
        val DEFAULT_PRESET_ID = longPreferencesKey("default_preset_id")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val KEEP_RUNNING_IN_BACKGROUND = booleanPreferencesKey("keep_running_in_background")
        val CONFIRM_ON_STOP = booleanPreferencesKey("confirm_on_stop")

        // Notification Preferences
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val NOTIFICATION_VIBRATE = booleanPreferencesKey("notification_vibrate")
        val SHOW_ONGOING_NOTIFICATION = booleanPreferencesKey("show_ongoing_notification")

        // Last opened section
        val LAST_SETTINGS_SECTION = intPreferencesKey("last_settings_section")
    }

    // Profile flows
    val displayName: Flow<String> = dataStore.data.map { it[DISPLAY_NAME] ?: "" }
    val email: Flow<String> = dataStore.data.map { it[EMAIL] ?: "" }
    val avatarUri: Flow<String?> = dataStore.data.map { it[AVATAR_URI] }

    // Timer preference flows
    val defaultPresetId: Flow<Long?> = dataStore.data.map { it[DEFAULT_PRESET_ID] }
    val keepScreenOn: Flow<Boolean> = dataStore.data.map { it[KEEP_SCREEN_ON] ?: false }
    val keepRunningInBackground: Flow<Boolean> = dataStore.data.map { it[KEEP_RUNNING_IN_BACKGROUND] ?: false }
    val confirmOnStop: Flow<Boolean> = dataStore.data.map { it[CONFIRM_ON_STOP] ?: true }

    // Notification preference flows
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val notificationSound: Flow<String> = dataStore.data.map { it[NOTIFICATION_SOUND] ?: "default" }
    val notificationVibrate: Flow<Boolean> = dataStore.data.map { it[NOTIFICATION_VIBRATE] ?: true }
    val showOngoingNotification: Flow<Boolean> = dataStore.data.map { it[SHOW_ONGOING_NOTIFICATION] ?: true }

    val lastSettingsSection: Flow<Int> = dataStore.data.map { it[LAST_SETTINGS_SECTION] ?: 0 }

    // Update functions
    suspend fun updateProfile(displayName: String, email: String) {
        dataStore.edit { preferences ->
            preferences[DISPLAY_NAME] = displayName
            preferences[EMAIL] = email
        }
    }

    suspend fun updateAvatarUri(uri: String?) {
        dataStore.edit { preferences ->
            if (uri != null) {
                preferences[AVATAR_URI] = uri
            } else {
                preferences.remove(AVATAR_URI)
            }
        }
    }

    suspend fun updateDefaultPresetId(presetId: Long?) {
        dataStore.edit { preferences ->
            if (presetId != null) {
                preferences[DEFAULT_PRESET_ID] = presetId
            } else {
                preferences.remove(DEFAULT_PRESET_ID)
            }
        }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON] = enabled
        }
    }

    suspend fun updateKeepRunningInBackground(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEEP_RUNNING_IN_BACKGROUND] = enabled
        }
    }

    suspend fun updateConfirmOnStop(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[CONFIRM_ON_STOP] = enabled
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateNotificationSound(sound: String) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_SOUND] = sound
        }
    }

    suspend fun updateNotificationVibrate(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_VIBRATE] = enabled
        }
    }

    suspend fun updateShowOngoingNotification(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SHOW_ONGOING_NOTIFICATION] = enabled
        }
    }

    suspend fun updateLastSettingsSection(section: Int) {
        dataStore.edit { preferences ->
            preferences[LAST_SETTINGS_SECTION] = section
        }
    }

    suspend fun clearProfile() {
        dataStore.edit { preferences ->
            preferences.remove(DISPLAY_NAME)
            preferences.remove(EMAIL)
            preferences.remove(AVATAR_URI)
        }
    }
}

