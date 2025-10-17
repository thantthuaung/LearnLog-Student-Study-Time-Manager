package com.example.learnlog.data.dao

import androidx.room.*
import com.example.learnlog.data.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY createdAt DESC")
    fun getNotesBySubject(subjectId: Long): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes 
        WHERE title LIKE :query 
        OR content LIKE :query 
        ORDER BY createdAt DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?
}
