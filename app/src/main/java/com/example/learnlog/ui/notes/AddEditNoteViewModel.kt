package com.example.learnlog.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.Note
import com.example.learnlog.data.repository.NoteRepository
import com.example.learnlog.util.DateTimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddEditNoteViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val dateTimeProvider: DateTimeProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var noteId: Long = savedStateHandle.get<Long>("noteId") ?: -1L
    val isNew: Boolean get() = noteId == -1L

    private val _note = MutableStateFlow<Note?>(null)
    /** The stored note (null while loading or for a new note). */
    val note: StateFlow<Note?> = _note

    private val _tags = MutableStateFlow(savedStateHandle.get<ArrayList<String>>(KEY_TAGS)?.toList() ?: emptyList())
    val tags: StateFlow<List<String>> = _tags

    private val _pinned = MutableStateFlow(savedStateHandle.get<Boolean>(KEY_PINNED) ?: false)
    val pinned: StateFlow<Boolean> = _pinned

    /** Set once the editor fields have been filled from the database, so rotation never overwrites typing. */
    var populated: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_POPULATED) ?: false
        set(value) { savedStateHandle[KEY_POPULATED] = value }

    init {
        if (!isNew) {
            viewModelScope.launch {
                val existing = noteRepository.getNoteById(noteId)
                _note.value = existing
                if (!populated && existing != null) {
                    setTags(existing.tags)
                    setPinned(existing.isPinned)
                }
            }
        }
    }

    fun addTag(tag: String) {
        val clean = tag.trim().removePrefix("#")
        if (clean.isNotEmpty() && clean !in _tags.value) setTags(_tags.value + clean)
    }

    fun removeTag(tag: String) = setTags(_tags.value - tag)

    fun togglePinned() = setPinned(!_pinned.value)

    private fun setTags(tags: List<String>) {
        _tags.value = tags
        savedStateHandle[KEY_TAGS] = ArrayList(tags)
    }

    private fun setPinned(value: Boolean) {
        _pinned.value = value
        savedStateHandle[KEY_PINNED] = value
    }

    /**
     * Saves the note. A missing title falls back to the first line of the body. Returns false
     * when there is nothing to save. Runs to completion even if the screen closes immediately.
     */
    suspend fun save(rawTitle: String, content: String): Boolean {
        val title = rawTitle.trim().ifEmpty { content.trim().lineSequence().firstOrNull()?.take(60)?.trim().orEmpty() }
        if (title.isEmpty()) return false
        val now = dateTimeProvider.now()
        val existing = _note.value
        val unchanged = existing != null && existing.title == title && existing.content == content &&
            existing.tags == _tags.value && existing.isPinned == _pinned.value
        if (unchanged) return true
        withContext(NonCancellable) {
            if (existing != null) {
                noteRepository.update(existing.copy(title = title, content = content, tags = _tags.value, isPinned = _pinned.value, updatedAt = now))
            } else {
                noteId = noteRepository.insert(
                    Note(title = title, content = content, tags = _tags.value, isPinned = _pinned.value, createdAt = now, updatedAt = now)
                )
                _note.value = noteRepository.getNoteById(noteId)
            }
        }
        return true
    }

    suspend fun delete() {
        val existing = _note.value ?: return
        withContext(NonCancellable) { noteRepository.delete(existing) }
    }

    fun restore(note: Note) = viewModelScope.launch { noteRepository.insert(note) }

    companion object {
        private const val KEY_TAGS = "tags"
        private const val KEY_PINNED = "pinned"
        private const val KEY_POPULATED = "populated"
    }
}
