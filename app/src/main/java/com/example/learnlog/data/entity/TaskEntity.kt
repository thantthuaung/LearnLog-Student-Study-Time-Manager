package com.example.learnlog.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.learnlog.data.converter.DateTimeConverter
import com.example.learnlog.data.converter.LocalDateTimeParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.threeten.bp.LocalDateTime

@Parcelize
@TypeParceler<LocalDateTime?, LocalDateTimeParceler>
@Entity(tableName = "tasks")
@TypeConverters(DateTimeConverter::class)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String?,
    val dueAt: LocalDateTime?,
    val priority: Int,
    val status: String,
    val progress: Int = 0,
    val completed: Boolean = false,
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val type: String,
    val durationMinutes: Int = 30  // Default 30 minutes for planned time
) : Parcelable
