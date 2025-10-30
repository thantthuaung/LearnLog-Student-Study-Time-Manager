package com.example.learnlog.data.export

import com.example.learnlog.data.entity.SessionLogEntity
import com.example.learnlog.data.entity.TaskEntity
import com.google.gson.annotations.SerializedName
import org.threeten.bp.format.DateTimeFormatter

data class ExportData(
    @SerializedName("export_version") val exportVersion: Int = 1,
    @SerializedName("export_date") val exportDate: String,
    @SerializedName("tasks") val tasks: List<TaskExport>,
    @SerializedName("sessions") val sessions: List<SessionExport>,
    @SerializedName("planner") val plannerEntries: List<PlannerExport>
)

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
    val type: String = "ASSIGNMENT"
) {
    companion object {
        private val formatter = DateTimeFormatter.ISO_INSTANT

        fun fromEntity(task: TaskEntity): TaskExport {
            return TaskExport(
                id = task.id,
                title = task.title,
                subject = task.subject,
                priority = task.priority,
                status = task.status,
                dueAt = task.dueAt?.atZone(org.threeten.bp.ZoneId.systemDefault())?.toInstant()?.toString(),
                createdAt = task.createdAt.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant().toString(),
                updatedAt = task.updatedAt.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant().toString(),
                notes = task.notes,
                type = task.type
            )
        }

        fun toEntity(export: TaskExport): TaskEntity {
            return TaskEntity(
                id = export.id,
                title = export.title,
                subject = export.subject,
                priority = export.priority,
                status = export.status,
                dueAt = export.dueAt?.let {
                    org.threeten.bp.Instant.parse(it).atZone(org.threeten.bp.ZoneId.systemDefault()).toLocalDateTime()
                },
                createdAt = org.threeten.bp.Instant.parse(export.createdAt).atZone(org.threeten.bp.ZoneId.systemDefault()).toLocalDateTime(),
                updatedAt = org.threeten.bp.Instant.parse(export.updatedAt).atZone(org.threeten.bp.ZoneId.systemDefault()).toLocalDateTime(),
                notes = export.notes,
                completed = export.status == "COMPLETED",
                progress = if (export.status == "COMPLETED") 100 else 0,
                type = export.type
            )
        }
    }
}

data class SessionExport(
    val id: Long,
    @SerializedName("task_id") val taskId: Long?,
    val subject: String?,
    @SerializedName("start_at") val startAt: String,
    @SerializedName("end_at") val endAt: String,
    @SerializedName("duration_s") val durationSeconds: Int,
    val notes: String?
) {
    companion object {
        fun fromEntity(session: SessionLogEntity): SessionExport {
            return SessionExport(
                id = session.id,
                taskId = session.taskId,
                subject = session.subject,
                startAt = session.startTime.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant().toString(),
                endAt = session.endTime.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant().toString(),
                durationSeconds = session.durationMinutes * 60,
                notes = session.notes
            )
        }

        fun toEntity(export: SessionExport): SessionLogEntity {
            return SessionLogEntity(
                id = export.id,
                taskId = export.taskId,
                subject = export.subject,
                startTime = org.threeten.bp.Instant.parse(export.startAt).atZone(org.threeten.bp.ZoneId.systemDefault()).toLocalDateTime(),
                endTime = org.threeten.bp.Instant.parse(export.endAt).atZone(org.threeten.bp.ZoneId.systemDefault()).toLocalDateTime(),
                durationMinutes = export.durationSeconds / 60,
                type = "FOCUS",
                isCompleted = true,
                notes = export.notes
            )
        }
    }
}

data class PlannerExport(
    val id: Long,
    @SerializedName("task_id") val taskId: Long,
    @SerializedName("planned_start") val plannedStart: String,
    @SerializedName("planned_end") val plannedEnd: String,
    @SerializedName("planned_minutes") val plannedMinutes: Int
)

