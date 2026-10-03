package com.example.learnlog.data.export

import com.example.learnlog.data.Note
import com.example.learnlog.data.entity.SessionLogEntity
import com.example.learnlog.data.entity.TaskEntity
import com.google.gson.annotations.SerializedName
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId

private val zone: ZoneId get() = ZoneId.systemDefault()
internal fun LocalDateTime.toIso(): String = atZone(zone).toInstant().toString()
internal fun String.fromIso(): LocalDateTime = Instant.parse(this).atZone(zone).toLocalDateTime()

/**
 * Backup file format. Version 2 adds notes, task duration/completion and session completion.
 * Version 1 files (no such fields) still import; missing values fall back to safe defaults.
 */
data class ExportData(
    @SerializedName("export_version") val exportVersion: Int = CURRENT_VERSION,
    @SerializedName("export_date") val exportDate: String,
    @SerializedName("tasks") val tasks: List<TaskExport>? = emptyList(),
    @SerializedName("sessions") val sessions: List<SessionExport>? = emptyList(),
    @SerializedName("notes") val notes: List<NoteExport>? = emptyList()
) {
    companion object {
        const val CURRENT_VERSION = 2
    }
}

data class TaskExport(
    val id: Long,
    val title: String,
    val subject: String?,
    val priority: Int,
    val status: String,
    @SerializedName("due_at") val dueAt: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val notes: String?,
    val type: String? = TaskEntity.TYPE_ASSIGNMENT,
    val completed: Boolean? = null,
    @SerializedName("duration_minutes") val durationMinutes: Int? = null
) {
    fun toEntity(id: Long = this.id): TaskEntity {
        val done = completed ?: (status == TaskEntity.STATUS_COMPLETED)
        return TaskEntity(
            id = id,
            title = title,
            subject = subject,
            priority = priority.coerceIn(0, 2),
            status = if (done) TaskEntity.STATUS_COMPLETED else status.takeIf { it != TaskEntity.STATUS_COMPLETED } ?: TaskEntity.STATUS_PENDING,
            dueAt = dueAt?.fromIso(),
            createdAt = createdAt.fromIso(),
            updatedAt = updatedAt.fromIso(),
            notes = notes,
            completed = done,
            progress = if (done) 100 else 0,
            type = TaskEntity.normalizeType(type),
            durationMinutes = durationMinutes ?: 30
        )
    }

    companion object {
        fun fromEntity(task: TaskEntity) = TaskExport(
            id = task.id,
            title = task.title,
            subject = task.subject,
            priority = task.priority,
            status = task.status,
            dueAt = task.dueAt?.toIso(),
            createdAt = task.createdAt.toIso(),
            updatedAt = task.updatedAt.toIso(),
            notes = task.notes,
            type = task.type,
            completed = task.completed,
            durationMinutes = task.durationMinutes
        )
    }
}

data class SessionExport(
    val id: Long,
    @SerializedName("task_id") val taskId: Long?,
    val subject: String?,
    @SerializedName("start_at") val startAt: String,
    @SerializedName("end_at") val endAt: String,
    @SerializedName("duration_s") val durationSeconds: Int,
    val notes: String?,
    @SerializedName("is_completed") val isCompleted: Boolean? = null
) {
    fun toEntity(id: Long = this.id, taskId: Long? = this.taskId) = SessionLogEntity(
        id = id,
        taskId = taskId,
        subject = subject,
        startTime = startAt.fromIso(),
        endTime = endAt.fromIso(),
        durationMinutes = durationSeconds / 60,
        type = "FOCUS",
        // v1 files did not record this; treat sessions as completed only if they have duration
        isCompleted = isCompleted ?: (durationSeconds >= 60),
        notes = notes
    )

    companion object {
        fun fromEntity(session: SessionLogEntity) = SessionExport(
            id = session.id,
            taskId = session.taskId,
            subject = session.subject,
            startAt = session.startTime.toIso(),
            endAt = session.endTime.toIso(),
            durationSeconds = session.durationMinutes * 60,
            notes = session.notes,
            isCompleted = session.isCompleted
        )
    }
}

data class NoteExport(
    val id: Long,
    val title: String,
    val content: String,
    @SerializedName("task_id") val taskId: Long?,
    val tags: List<String>?,
    @SerializedName("is_pinned") val isPinned: Boolean,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
) {
    fun toEntity(id: Long = this.id, taskId: Long? = this.taskId) = Note(
        id = id,
        title = title,
        content = content,
        taskId = taskId,
        tags = tags ?: emptyList(),
        isPinned = isPinned,
        createdAt = createdAt.fromIso(),
        updatedAt = updatedAt.fromIso()
    )

    companion object {
        fun fromEntity(note: Note) = NoteExport(
            id = note.id,
            title = note.title,
            content = note.content,
            taskId = note.taskId,
            tags = note.tags,
            isPinned = note.isPinned,
            createdAt = note.createdAt.toIso(),
            updatedAt = note.updatedAt.toIso()
        )
    }
}
