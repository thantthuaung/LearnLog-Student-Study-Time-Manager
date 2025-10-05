package com.example.learnlog.ui.planner

import java.time.LocalDateTime

enum class SessionType { ASSIGNMENT, EXAM, CLASS, PERSONAL, REVISION }
enum class SessionStatus { PLANNED, COMPLETED, SKIPPED }

data class StudySession(
    val id: Long = 0L,
    val subject: String,
    val type: SessionType,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val durationMinutes: Int,
    val status: SessionStatus = SessionStatus.PLANNED,
    val linkedTaskId: Long? = null,
    val color: Int? = null, // For color-coding by subject
    val isManual: Boolean = false // True if user-added, false if auto-scheduled
)
