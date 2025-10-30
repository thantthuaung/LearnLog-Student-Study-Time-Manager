package com.example.learnlog.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.learnlog.data.model.AppSettings
import com.example.learnlog.data.model.TimerPreset
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore
    private val gson = Gson()

    companion object {
        private val TIMER_PRESETS_KEY = stringPreferencesKey("timer_presets")
        private val DEFAULT_PRESET_INDEX_KEY = intPreferencesKey("default_preset_index")
        private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        private val SOUND_ENABLED_KEY = booleanPreferencesKey("sound_enabled")
        private val SELECTED_SOUND_TONE_KEY = stringPreferencesKey("selected_sound_tone")
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { preferences ->
        val presetsJson = preferences[TIMER_PRESETS_KEY]
        val presets = if (presetsJson != null) {
            val type = object : TypeToken<List<TimerPreset>>() {}.type
            gson.fromJson<List<TimerPreset>>(presetsJson, type)
        } else {
            AppSettings().timerPresets
        }

        AppSettings(
            timerPresets = presets,
            defaultPresetIndex = preferences[DEFAULT_PRESET_INDEX_KEY] ?: 0,
            notificationsEnabled = preferences[NOTIFICATIONS_ENABLED_KEY] ?: true,
            soundEnabled = preferences[SOUND_ENABLED_KEY] ?: true,
            selectedSoundTone = preferences[SELECTED_SOUND_TONE_KEY] ?: "default",
            vibrationEnabled = preferences[VIBRATION_ENABLED_KEY] ?: true
        )
    }

    suspend fun updateTimerPresets(presets: List<TimerPreset>) {
        dataStore.edit { preferences ->
            preferences[TIMER_PRESETS_KEY] = gson.toJson(presets)
        }
    }

    suspend fun updateDefaultPresetIndex(index: Int) {
        dataStore.edit { preferences ->
            preferences[DEFAULT_PRESET_INDEX_KEY] = index
        }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[SOUND_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateSelectedSoundTone(tone: String) {
        dataStore.edit { preferences ->
            preferences[SELECTED_SOUND_TONE_KEY] = tone
        }
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[VIBRATION_ENABLED_KEY] = enabled
        }
    }
}

