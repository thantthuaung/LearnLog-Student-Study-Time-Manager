package com.example.learnlog.data.dao

import androidx.room.*
import com.example.learnlog.data.model.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): Subject?

    @Insert
    suspend fun insertSubject(subject: Subject): Long

    @Update
    suspend fun updateSubject(subject: Subject)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getSubjectsOrderedByName(): Flow<List<Subject>>
}
