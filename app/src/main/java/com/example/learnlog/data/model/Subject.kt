package com.example.learnlog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.learnlog.data.converter.DateTimeConverter
import org.threeten.bp.LocalDateTime

@Entity(tableName = "subjects")
@TypeConverters(DateTimeConverter::class)
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
