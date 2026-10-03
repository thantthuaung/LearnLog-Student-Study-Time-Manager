package com.example.learnlog.ui.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.Note
import com.example.learnlog.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val totalCount: Int = 0,
    val query: String = "",
    val pinnedOnly: Boolean = false,
    val loaded: Boolean = false
) {
    val isFiltering: Boolean get() = query.isNotBlank() || pinnedOnly
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val query = savedState.getStateFlow(KEY_QUERY, "")
    private val pinnedOnly = savedState.getStateFlow(KEY_PINNED, false)

    /** Search covers title, body and tags; pinned notes always come first. */
    val uiState: StateFlow<NotesUiState> = combine(repository.allNotes, query, pinnedOnly) { notes, q, pinned ->
        val needle = q.trim()
        val visible = notes.filter { note ->
            (!pinned || note.isPinned) && (needle.isEmpty() ||
                note.title.contains(needle, true) || note.content.contains(needle, true) ||
                note.tags.any { it.contains(needle, true) })
        }.sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedAt })
        NotesUiState(visible, notes.size, q, pinned, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun setSearchQuery(value: String) { savedState[KEY_QUERY] = value }
    fun setPinnedOnly(value: Boolean) { savedState[KEY_PINNED] = value }

    fun deleteNote(note: Note) = viewModelScope.launch { repository.delete(note) }
    fun restoreNote(note: Note) = viewModelScope.launch { repository.insert(note) }
    fun togglePin(note: Note) = viewModelScope.launch { repository.togglePin(note.id, !note.isPinned) }

    companion object {
        private const val KEY_QUERY = "query"
        private const val KEY_PINNED = "pinned"
    }
}
