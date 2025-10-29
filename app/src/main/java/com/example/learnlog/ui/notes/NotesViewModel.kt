package com.example.learnlog.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.data.Note
import com.example.learnlog.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedSubjectId: Long? = null,
    val selectedTaskId: Long? = null,
    val showPinnedOnly: Boolean = false
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    private val _selectedTaskId = MutableStateFlow<Long?>(null)
    private val _showPinnedOnly = MutableStateFlow(false)

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<Note>> = combine(
        _searchQuery,
        _selectedSubjectId,
        _selectedTaskId,
        _showPinnedOnly
    ) { query, subjectId, taskId, pinnedOnly ->
        NoteFilters(query, subjectId, taskId, pinnedOnly)
    }.flatMapLatest { filters ->
        when {
            filters.showPinnedOnly -> repository.getPinnedNotes()
            filters.taskId != null -> repository.getNotesByTask(filters.taskId)
            filters.subjectId != null -> repository.getNotesBySubject(filters.subjectId)
            filters.searchQuery.isNotBlank() -> repository.searchNotesWithFilters(
                filters.searchQuery,
                filters.subjectId,
                filters.taskId
            )
            else -> repository.allNotes
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            notes.collect { notesList ->
                _uiState.update { it.copy(notes = notesList, isLoading = false) }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setSubjectFilter(subjectId: Long?) {
        _selectedSubjectId.value = subjectId
        _uiState.update { it.copy(selectedSubjectId = subjectId) }
    }

    fun setTaskFilter(taskId: Long?) {
        _selectedTaskId.value = taskId
        _uiState.update { it.copy(selectedTaskId = taskId) }
    }

    fun togglePinnedFilter() {
        _showPinnedOnly.value = !_showPinnedOnly.value
        _uiState.update { it.copy(showPinnedOnly = _showPinnedOnly.value) }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedSubjectId.value = null
        _selectedTaskId.value = null
        _showPinnedOnly.value = false
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedSubjectId = null,
                selectedTaskId = null,
                showPinnedOnly = false
            )
        }
    }

    fun insertNote(note: Note) = viewModelScope.launch {
        try {
            repository.insert(note)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun updateNote(note: Note) = viewModelScope.launch {
        try {
            repository.update(note)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun deleteNote(note: Note) = viewModelScope.launch {
        try {
            repository.delete(note)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun togglePin(note: Note) = viewModelScope.launch {
        try {
            repository.togglePin(note.id, !note.isPinned)
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

private data class NoteFilters(
    val searchQuery: String,
    val subjectId: Long?,
    val taskId: Long?,
    val showPinnedOnly: Boolean
)
