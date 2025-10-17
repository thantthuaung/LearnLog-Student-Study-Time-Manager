package com.example.learnlog.data.model

import org.threeten.bp.LocalDateTime

data class Task(
    val id: Long = 0,
    val title: String,
    val subject: String?,
    val dueDate: LocalDateTime?,
    val priority: TaskPriority,
    val type: TaskType,
    val status: TaskStatus,
    val description: String?,
    val isNotificationEnabled: Boolean
)
