package com.example.learnlog.data.repository

import com.example.learnlog.data.dao.NoteDao
import com.example.learnlog.data.Note
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(private val noteDao: NoteDao) {

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesBySubject(subjectId: Long): Flow<List<Note>> {
        return noteDao.getNotesBySubject(subjectId)
    }

    fun getNotesByTask(taskId: Long): Flow<List<Note>> {
        return noteDao.getNotesByTask(taskId)
    }

    fun getPinnedNotes(): Flow<List<Note>> {
        return noteDao.getPinnedNotes()
    }

    suspend fun insert(note: Note): Long {
        return noteDao.insertNote(note)
    }

    suspend fun update(note: Note) {
        noteDao.updateNote(note)
    }

    suspend fun delete(note: Note) {
        noteDao.deleteNote(note)
    }

    suspend fun getNoteById(noteId: Long): Note? {
        return noteDao.getNoteById(noteId)
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        return noteDao.searchNotes("%$query%")
    }

    fun searchNotesWithFilters(query: String, subjectId: Long?, taskId: Long?): Flow<List<Note>> {
        return noteDao.searchNotesWithFilters("%$query%", subjectId, taskId)
    }

    suspend fun togglePin(noteId: Long, isPinned: Boolean) {
        noteDao.togglePin(noteId, isPinned)
    }
}
