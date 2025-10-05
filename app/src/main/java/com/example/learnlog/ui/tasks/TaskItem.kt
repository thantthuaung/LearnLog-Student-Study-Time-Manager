package com.example.learnlog.ui.tasks

import java.time.LocalDateTime

enum class TaskPriority { HIGH, MEDIUM, LOW }
enum class TaskType { ASSIGNMENT, EXAM, CLASS, REVISION, PERSONAL }
enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }

data class TaskItem(
    val id: Long = 0L,
    val title: String,
    val subject: String,
    val dueDateTime: LocalDateTime,
    val priority: TaskPriority,
    val type: TaskType,
    val status: TaskStatus = TaskStatus.PENDING,
    val progress: Int = 0 // percent complete
)
