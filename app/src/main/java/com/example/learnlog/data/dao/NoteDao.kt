package com.example.learnlog.data.dao

import androidx.room.*
import com.example.learnlog.data.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE subjectId = :subjectId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesBySubject(subjectId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE taskId = :taskId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByTask(taskId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY updatedAt DESC")
    fun getPinnedNotes(): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes 
        WHERE title LIKE :query 
        OR content LIKE :query 
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("""
        SELECT * FROM notes 
        WHERE (title LIKE :query OR content LIKE :query)
        AND (:subjectId IS NULL OR subjectId = :subjectId)
        AND (:taskId IS NULL OR taskId = :taskId)
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotesWithFilters(query: String, subjectId: Long?, taskId: Long?): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): Note?

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :noteId")
    suspend fun togglePin(noteId: Long, isPinned: Boolean)
}
