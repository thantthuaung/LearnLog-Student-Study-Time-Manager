package com.example.learnlog.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Precomputed daily rollup to improve performance for Analytics/Insights.
 * Updated nightly by WorkManager.
 */
@Entity(tableName = "daily_rollups")
data class DailyRollupEntity(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val totalMinutes: Int,
    val sessionCount: Int,
    val subjectBreakdown: String, // JSON: {"Math": 120, "Science": 60}
    val lastUpdated: Long = System.currentTimeMillis()
)

