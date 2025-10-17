package com.example.learnlog.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.learnlog.data.converter.DateTimeConverter
import org.threeten.bp.LocalDateTime

@Entity(tableName = "session_logs")
@TypeConverters(DateTimeConverter::class)
data class SessionLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "task_id")
    val taskId: Long?,

    @ColumnInfo(name = "subject")
    val subject: String?,

    @ColumnInfo(name = "start_time")
    val startTime: LocalDateTime,

    @ColumnInfo(name = "end_time")
    val endTime: LocalDateTime,

    @ColumnInfo(name = "duration_minutes")
    val durationMinutes: Int,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean,

    @ColumnInfo(name = "notes")
    val notes: String? = null
)
