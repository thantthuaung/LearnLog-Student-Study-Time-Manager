package com.example.learnlog.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.learnlog.data.converter.DateTimeConverter
import com.example.learnlog.data.converter.StringListConverter
import org.threeten.bp.LocalDateTime

@Entity(tableName = "notes")
@TypeConverters(
    DateTimeConverter::class,
    StringListConverter::class
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val subjectId: Long?,
    val tags: List<String>,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
