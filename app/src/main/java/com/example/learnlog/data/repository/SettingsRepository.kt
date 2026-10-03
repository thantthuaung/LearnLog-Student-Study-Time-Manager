package com.example.learnlog.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.learnlog.data.model.AppSettings
import com.example.learnlog.data.model.TimerPreset
import com.example.learnlog.data.model.TimerPresets
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** Timer presets and which one the Timer opens with. */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.settingsDataStore
    private val gson = Gson()

    companion object {
        private val TIMER_PRESETS_KEY = stringPreferencesKey("timer_presets")
        private val DEFAULT_PRESET_INDEX_KEY = intPreferencesKey("default_preset_index")
    }

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        val presets = prefs[TIMER_PRESETS_KEY]
            ?.let { json -> runCatching { gson.fromJson<List<TimerPreset>>(json, object : TypeToken<List<TimerPreset>>() {}.type) }.getOrNull() }
            ?.filter { it.durationMinutes in TimerPresets.MIN_DURATION_MINUTES..TimerPresets.MAX_DURATION_MINUTES }
            ?.takeIf { it.isNotEmpty() }
            ?: TimerPresets.DEFAULT_PRESETS
        val defaultIndex = (prefs[DEFAULT_PRESET_INDEX_KEY] ?: TimerPresets.DEFAULT_INDEX)
            .coerceIn(0, presets.lastIndex)
        AppSettings(timerPresets = presets, defaultPresetIndex = defaultIndex)
    }

    suspend fun current(): AppSettings = settingsFlow.first()

    suspend fun updateTimerPresets(presets: List<TimerPreset>) {
        val sorted = presets.distinctBy { it.durationMinutes }.sortedBy { it.durationMinutes }
        dataStore.edit { it[TIMER_PRESETS_KEY] = gson.toJson(sorted) }
    }

    suspend fun updateDefaultPresetIndex(index: Int) {
        dataStore.edit { it[DEFAULT_PRESET_INDEX_KEY] = index }
    }
}
