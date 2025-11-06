package com.example.learnlog.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.TaskDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataBackupViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val sessionDao: SessionLogDao
) : ViewModel() {

    private val _taskCount = MutableStateFlow(0)
    val taskCount: StateFlow<Int> = _taskCount

    private val _sessionCount = MutableStateFlow(0)
    val sessionCount: StateFlow<Int> = _sessionCount

    private val _plannerCount = MutableStateFlow(0)
    val plannerCount: StateFlow<Int> = _plannerCount

    init {
        refreshCounts()
    }

    fun refreshCounts() {
        viewModelScope.launch {
            _taskCount.value = taskDao.getAll().first().size
            _sessionCount.value = sessionDao.getAllSessions().first().size
            // Planner uses tasks, so we'll use task count for now
            _plannerCount.value = taskDao.getAll().first().size
        }
    }
}

