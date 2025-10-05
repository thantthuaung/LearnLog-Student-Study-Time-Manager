package com.example.learnlog.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int? = null,
    val weeklyTargetMinutes: Int = 120
)
