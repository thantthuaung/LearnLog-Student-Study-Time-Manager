package com.example.learnlog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    // Profile flows
    val displayName: Flow<String> = userPreferences.displayName
    val email: Flow<String> = userPreferences.email
    val avatarUri: Flow<String?> = userPreferences.avatarUri

    // Timer preference flows
    val defaultPresetId: Flow<Long?> = userPreferences.defaultPresetId
    val keepScreenOn: Flow<Boolean> = userPreferences.keepScreenOn
    val confirmOnStop: Flow<Boolean> = userPreferences.confirmOnStop

    // Notification preference flows
    val notificationsEnabled: Flow<Boolean> = userPreferences.notificationsEnabled
    val notificationSound: Flow<String> = userPreferences.notificationSound
    val notificationVibrate: Flow<Boolean> = userPreferences.notificationVibrate
    val showOngoingNotification: Flow<Boolean> = userPreferences.showOngoingNotification

    fun updateProfile(displayName: String, email: String) {
        viewModelScope.launch {
            userPreferences.updateProfile(displayName, email)
        }
    }

    fun updateAvatarUri(uri: String?) {
        viewModelScope.launch {
            userPreferences.updateAvatarUri(uri)
        }
    }

    fun updateDefaultPresetId(presetId: Long?) {
        viewModelScope.launch {
            userPreferences.updateDefaultPresetId(presetId)
        }
    }

    fun updateKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateKeepScreenOn(enabled)
        }
    }

    fun updateConfirmOnStop(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateConfirmOnStop(enabled)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateNotificationsEnabled(enabled)
        }
    }

    fun updateNotificationSound(sound: String) {
        viewModelScope.launch {
            userPreferences.updateNotificationSound(sound)
        }
    }

    fun updateNotificationVibrate(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateNotificationVibrate(enabled)
        }
    }

    fun updateShowOngoingNotification(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.updateShowOngoingNotification(enabled)
        }
    }

    fun saveLastSection(section: Int) {
        viewModelScope.launch {
            userPreferences.updateLastSettingsSection(section)
        }
    }

    fun clearProfile() {
        viewModelScope.launch {
            userPreferences.clearProfile()
        }
    }
}

