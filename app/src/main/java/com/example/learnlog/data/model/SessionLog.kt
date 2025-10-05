package com.example.learnlog.data.model

import androidx.room.*
import com.example.learnlog.data.converter.DateTimeConverter
import com.example.learnlog.data.converter.SessionTypeConverter
import org.threeten.bp.LocalDateTime

import com.example.learnlog.data.model.SessionType

@Entity(tableName = "session_logs")
data class SessionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "subject_id")
    val subjectId: Long,

    @ColumnInfo(name = "task_id")
    val taskId: Long? = null,

    @TypeConverters(DateTimeConverter::class)
    @ColumnInfo(name = "start_time")
    val startTime: LocalDateTime,

    @TypeConverters(DateTimeConverter::class)
    @ColumnInfo(name = "end_time")
    val endTime: LocalDateTime,

    @ColumnInfo(name = "focus_minutes")
    val focusMinutes: Int,

    @ColumnInfo(name = "was_planned", defaultValue = "0")
    val wasPlanned: Boolean = false,

    @TypeConverters(SessionTypeConverter::class)
    @ColumnInfo(name = "type")
    val type: SessionType = SessionType.FOCUS,

    @ColumnInfo(name = "break_minutes")
    val breakMinutes: Int = 0
)
