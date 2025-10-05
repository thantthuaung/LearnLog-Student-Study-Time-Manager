package com.example.learnlog.data.dao

import androidx.room.*
import com.example.learnlog.data.model.SessionLog
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.LocalDateTime

@Dao
interface SessionLogDao {
    @Query("SELECT * FROM session_logs")
    fun getAllSessions(): Flow<List<SessionLog>>

    @Query("SELECT * FROM session_logs WHERE start_time >= :startTime AND end_time <= :endTime")
    fun getSessionsInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<List<SessionLog>>

    @Query("SELECT COALESCE(SUM(focus_minutes), 0) FROM session_logs WHERE start_time >= :startTime AND end_time <= :endTime")
    fun getTotalFocusTimeInRange(startTime: LocalDateTime, endTime: LocalDateTime): Flow<Int>

    @Query("SELECT COALESCE(SUM(focus_minutes), 0) FROM session_logs WHERE subject_id = :subjectId AND start_time >= :startTime AND end_time <= :endTime")
    fun getFocusTimeBySubject(subjectId: Long, startTime: LocalDateTime, endTime: LocalDateTime): Flow<Int>

    @Query("SELECT COUNT(*) FROM session_logs WHERE start_time >= :startTime AND end_time <= :endTime")
    fun getSessionCount(startTime: LocalDateTime, endTime: LocalDateTime): Flow<Int>

    @Insert
    suspend fun insertSession(session: SessionLog): Long

    @Update
    suspend fun updateSession(session: SessionLog)

    @Delete
    suspend fun deleteSession(session: SessionLog)
}
