package com.example.learnlog.data.dao

import androidx.room.*
import com.example.learnlog.data.entity.DailyRollupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRollupDao {
    @Query("SELECT * FROM daily_rollups WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    fun getRollupsInRange(startDate: String, endDate: String): Flow<List<DailyRollupEntity>>

    @Query("SELECT * FROM daily_rollups WHERE date = :date")
    suspend fun getRollupForDate(date: String): DailyRollupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rollup: DailyRollupEntity)

    @Query("DELETE FROM daily_rollups WHERE date < :beforeDate")
    suspend fun deleteOldRollups(beforeDate: String)

    @Query("DELETE FROM daily_rollups")
    suspend fun clearAll()
}

