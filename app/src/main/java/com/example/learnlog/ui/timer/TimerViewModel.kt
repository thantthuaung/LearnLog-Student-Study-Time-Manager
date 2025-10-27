package com.example.learnlog.ui.timer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.SessionLogEntity
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val sessionLogDao: SessionLogDao
) : ViewModel() {

    private val _timerFinishedEvent = MutableLiveData<TimerFinishData>()
    val timerFinishedEvent: LiveData<TimerFinishData> = _timerFinishedEvent

    private val _currentTask = MutableLiveData<TaskEntity?>()
    val currentTask: LiveData<TaskEntity?> = _currentTask

    private var currentSessionId: Long = 0

    fun loadTask(taskId: Long) {
        if (taskId == -1L) return
        viewModelScope.launch {
            val task = withContext(Dispatchers.IO) {
                taskRepository.getTaskById(taskId)
            }
            _currentTask.value = task
        }
    }

    fun startTimerSession(taskId: Long?, taskTitle: String?, durationMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = LocalDateTime.now()
            val session = SessionLogEntity(
                taskId = taskId,
                subject = taskTitle,
                startTime = startTime,
                endTime = startTime.plusMinutes(durationMinutes.toLong()),
                durationMinutes = durationMinutes,
                type = "FOCUS",
                isCompleted = false
            )

            currentSessionId = sessionLogDao.insertSession(session)

            // Mark task as IN_PROGRESS
            if (taskId != null && taskId != -1L) {
                val task = taskRepository.getTaskById(taskId)
                task?.let {
                    taskRepository.updateTask(it.copy(status = "IN_PROGRESS"))
                }
            }
        }
    }

    fun completeTimerSession(actualDurationMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            if (currentSessionId != 0L) {
                // Retrieve the existing session
                val existingSession = sessionLogDao.getSessionById(currentSessionId)
                existingSession?.let { session ->
                    // Update session with actual end time
                    val endTime = LocalDateTime.now()
                    val updatedSession = session.copy(
                        endTime = endTime,
                        durationMinutes = actualDurationMinutes,
                        isCompleted = true
                    )
                    sessionLogDao.updateSession(updatedSession)

                    // Emit event to show completion dialog
                    withContext(Dispatchers.Main) {
                        _timerFinishedEvent.value = TimerFinishData(
                            taskId = _currentTask.value?.id,
                            sessionId = currentSessionId,
                            durationMinutes = actualDurationMinutes
                        )
                    }
                }
            }
        }
    }

    fun markTaskCompleted(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskRepository.getTaskById(taskId)
            task?.let {
                taskRepository.updateTask(it.copy(status = "COMPLETED", completed = true))
            }
        }
    }

    fun keepTaskInProgress(taskId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = taskRepository.getTaskById(taskId)
            task?.let {
                taskRepository.updateTask(it.copy(status = "IN_PROGRESS"))
            }
        }
    }

    // Standalone session methods (no task)
    fun startStandaloneSession(durationMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val startTime = LocalDateTime.now()
            val session = SessionLogEntity(
                taskId = null, // No task linked
                subject = "Focus Session",
                startTime = startTime,
                endTime = startTime.plusMinutes(durationMinutes.toLong()),
                durationMinutes = durationMinutes,
                type = "FOCUS",
                isCompleted = false
            )
            currentSessionId = sessionLogDao.insertSession(session)
        }
    }

    suspend fun completeStandaloneSession(actualDurationMinutes: Int) {
        withContext(Dispatchers.IO) {
            if (currentSessionId != 0L) {
                val existingSession = sessionLogDao.getSessionById(currentSessionId)
                existingSession?.let { session ->
                    val endTime = LocalDateTime.now()
                    val updatedSession = session.copy(
                        endTime = endTime,
                        durationMinutes = actualDurationMinutes,
                        isCompleted = true
                    )
                    sessionLogDao.updateSession(updatedSession)
                }
            }
            // Reset for next session
            currentSessionId = 0
        }
    }
}

data class TimerFinishData(
    val taskId: Long?,
    val sessionId: Long,
    val durationMinutes: Int
)
