package com.example.learnlog.ui.planner

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.learnlog.ui.tasks.TaskItem
import com.example.learnlog.ui.tasks.TaskStatus
import com.example.learnlog.ui.tasks.TaskType
import java.time.LocalDateTime

class PlannerViewModel : ViewModel() {
    private val _sessions = MutableLiveData<List<StudySession>>(emptyList())
    val sessions: LiveData<List<StudySession>> = _sessions

    private val _calendarMode = MutableLiveData(CalendarMode.WEEK)
    val calendarMode: LiveData<CalendarMode> = _calendarMode

    private var nextSessionId = 1L

    fun setTasks(tasks: List<TaskItem>) {
        // Smart scheduling: suggest sessions based on deadlines, workload, and free time
        val now = LocalDateTime.now()
        val sessions = tasks.filter { it.status != TaskStatus.COMPLETED }
            .flatMap { task ->
                // Example: split each task into 2 sessions, balance urgent/long-term
                val firstSession = StudySession(
                    id = nextSessionId++,
                    subject = task.subject,
                    type = when (task.type) {
                        TaskType.ASSIGNMENT -> SessionType.ASSIGNMENT
                        TaskType.EXAM -> SessionType.EXAM
                        TaskType.CLASS -> SessionType.CLASS
                        TaskType.REVISION -> SessionType.REVISION
                        TaskType.PERSONAL -> SessionType.PERSONAL
                    },
                    startTime = now.plusDays(1),
                    endTime = now.plusDays(1).plusHours(1),
                    durationMinutes = 60,
                    linkedTaskId = task.id,
                    color = null,
                    isManual = false
                )
                val secondSession = firstSession.copy(
                    id = nextSessionId++,
                    startTime = now.plusDays(2),
                    endTime = now.plusDays(2).plusHours(1)
                )
                listOf(firstSession, secondSession)
            }
        _sessions.value = sessions
    }

    fun addManualSession(session: StudySession) {
        _sessions.value = _sessions.value.orEmpty() + session.copy(id = nextSessionId++, isManual = true)
    }

    fun rescheduleSession(sessionId: Long, newStart: LocalDateTime, newEnd: LocalDateTime) {
        _sessions.value = _sessions.value.orEmpty().map {
            if (it.id == sessionId) it.copy(startTime = newStart, endTime = newEnd) else it
        }
    }

    fun markSessionCompleted(sessionId: Long) {
        _sessions.value = _sessions.value.orEmpty().map {
            if (it.id == sessionId) it.copy(status = SessionStatus.COMPLETED) else it
        }
        // TODO: Update linked task progress
    }

    fun markSessionSkipped(sessionId: Long) {
        _sessions.value = _sessions.value.orEmpty().map {
            if (it.id == sessionId) it.copy(status = SessionStatus.SKIPPED) else it
        }
    }

    fun setCalendarMode(mode: CalendarMode) {
        _calendarMode.value = mode
    }

    // TODO: Integration with Timer, Insights, and Notifications
}

enum class CalendarMode { WEEK, DAY }
