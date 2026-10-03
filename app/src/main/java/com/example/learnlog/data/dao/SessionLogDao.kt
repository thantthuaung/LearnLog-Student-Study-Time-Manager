package com.example.learnlog.data.dao

import androidx.room.*
import com.example.learnlog.data.entity.SessionLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionLogDao {
    @Query("SELECT * FROM session_logs ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs ORDER BY start_time DESC")
    suspend fun getAllOnce(): List<SessionLogEntity>

    @Query("SELECT * FROM session_logs WHERE is_completed = 1 ORDER BY start_time DESC")
    fun getCompletedSessions(): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs WHERE task_id = :taskId")
    fun getSessionsForTask(taskId: Long): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Long): SessionLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<SessionLogEntity>)

    @Update
    suspend fun updateSession(session: SessionLogEntity)

    @Delete
    suspend fun deleteSession(session: SessionLogEntity)

    @Query("DELETE FROM session_logs")
    suspend fun deleteAll()

    /** Removes rows left behind by the old timer, which inserted a row on start and never finished it. */
    @Query("DELETE FROM session_logs WHERE is_completed = 0")
    suspend fun deleteUnfinished()

    @Query("SELECT * FROM session_logs WHERE start_time >= :startTime AND start_time < :endTime")
    fun getSessionsInTimeRange(startTime: Long, endTime: Long): Flow<List<SessionLogEntity>>

    @Query("SELECT * FROM session_logs WHERE is_completed = 1 AND start_time >= :startTime AND start_time < :endTime")
    fun getCompletedInRange(startTime: Long, endTime: Long): Flow<List<SessionLogEntity>>
}
