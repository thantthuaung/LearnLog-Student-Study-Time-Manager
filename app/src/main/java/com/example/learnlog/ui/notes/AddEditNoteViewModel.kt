package com.example.learnlog.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.Note
import com.example.learnlog.data.repository.NoteRepository
import com.example.learnlog.util.DateTimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val dateTimeProvider: DateTimeProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val noteId = savedStateHandle.get<Long>("noteId") ?: -1L
    private val _note = MutableStateFlow<Note?>(null)
    val note: StateFlow<Note?> = _note

    init {
        if (noteId != -1L) {
            viewModelScope.launch {
                _note.value = noteRepository.getNoteById(noteId)
            }
        }
    }

    fun saveNote(title: String, content: String) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val currentDateTime = dateTimeProvider.now()
            val noteToSave = if (noteId != -1L) {
                _note.value?.copy(
                    title = title,
                    content = content,
                    updatedAt = currentDateTime
                )
            } else {
                Note(
                    title = title,
                    content = content,
                    subjectId = null,
                    tags = emptyList(),
                    createdAt = currentDateTime,
                    updatedAt = currentDateTime
                )
            }

            noteToSave?.let { note ->
                if (noteId != -1L) {
                    noteRepository.update(note)
                } else {
                    noteRepository.insert(note)
                }
            }
        }
    }
}
