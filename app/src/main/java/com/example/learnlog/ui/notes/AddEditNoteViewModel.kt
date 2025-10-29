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

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags

    private val _subjectId = MutableStateFlow<Long?>(null)
    private val _taskId = MutableStateFlow<Long?>(null)

    init {
        if (noteId != -1L) {
            viewModelScope.launch {
                val existingNote = noteRepository.getNoteById(noteId)
                _note.value = existingNote
                _tags.value = existingNote?.tags ?: emptyList()
                _subjectId.value = existingNote?.subjectId
                _taskId.value = existingNote?.taskId
            }
        } else {
            // Check if creating from task/subject context
            _subjectId.value = savedStateHandle.get<Long>("subjectId")?.takeIf { it > 0 }
            _taskId.value = savedStateHandle.get<Long>("taskId")?.takeIf { it > 0 }
        }
    }

    fun saveNote(
        title: String,
        content: String,
        tags: List<String> = _tags.value,
        subjectId: Long? = _subjectId.value,
        taskId: Long? = _taskId.value
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            val currentDateTime = dateTimeProvider.now()
            val noteToSave = if (noteId != -1L) {
                _note.value?.copy(
                    title = title,
                    content = content,
                    tags = tags,
                    subjectId = subjectId,
                    taskId = taskId,
                    updatedAt = currentDateTime
                )
            } else {
                Note(
                    title = title,
                    content = content,
                    subjectId = subjectId,
                    taskId = taskId,
                    tags = tags,
                    isPinned = false,
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

    fun addTag(tag: String) {
        if (tag.isNotBlank() && !_tags.value.contains(tag)) {
            _tags.value = _tags.value + tag
        }
    }

    fun removeTag(tag: String) {
        _tags.value = _tags.value - tag
    }

    fun setSubject(subjectId: Long?) {
        _subjectId.value = subjectId
    }

    fun setTask(taskId: Long?) {
        _taskId.value = taskId
    }
}
