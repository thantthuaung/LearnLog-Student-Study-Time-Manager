package com.example.learnlog.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.learnlog.ui.tasks.TaskFilter
import com.example.learnlog.ui.tasks.TaskSort
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object PreferencesKeys {
        val TASK_FILTER = stringPreferencesKey("task_filter")
        val TASK_SORT = stringPreferencesKey("task_sort")
    }

    val taskFilterFlow: Flow<TaskFilter> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val filterName = preferences[PreferencesKeys.TASK_FILTER] ?: TaskFilter.ALL.name
            TaskFilter.valueOf(filterName)
        }

    suspend fun updateTaskFilter(taskFilter: TaskFilter) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASK_FILTER] = taskFilter.name
        }
    }

    val taskSortFlow: Flow<TaskSort> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val sortName = preferences[PreferencesKeys.TASK_SORT] ?: TaskSort.DUE_DATE.name
            TaskSort.valueOf(sortName)
        }

    suspend fun updateTaskSort(taskSort: TaskSort) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TASK_SORT] = taskSort.name
        }
    }
}

