package com.example.learnlog.data.model

import java.util.Date

enum class TaskPriority {
    HIGH, MEDIUM, LOW
}

enum class TaskType {
    ASSIGNMENT, EXAM, CLASS, REVISION, PERSONAL
}

enum class TaskStatus {
    PENDING, IN_PROGRESS, COMPLETED
}

data class Task(
    val id: Long = 0,
    var title: String,
    var subject: String,
    var dueDate: Date,
    var priority: TaskPriority,
    var type: TaskType,
    var status: TaskStatus,
    var progress: Int = 0, // 0-100 for progress indicator
    var description: String = "",
    var isNotificationEnabled: Boolean = true,
    var createdAt: Date = Date()
)
