package com.example.learnlog.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentNotesBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.topBar.pageTitle.text = getString(R.string.page_notes_title)

        // Apply filters from arguments if any
        arguments?.let {
            val subjectId = it.getLong("subjectId", -1L)
            val taskId = it.getLong("taskId", -1L)
            if (subjectId > 0) viewModel.setSubjectFilter(subjectId)
            if (taskId > 0) viewModel.setTaskFilter(taskId)
        }

        setupRecyclerView()
        setupSearchView()
        setupFab()
        observeNotes()
        setupSwipeToDelete()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onClick = { note ->
                val action = NotesFragmentDirections.actionNotesFragmentToAddEditNoteFragment(note.id)
                findNavController().navigate(action)
            },
            onPinClick = { note ->
                viewModel.togglePin(note)
            }
        )

        binding.recyclerViewNotes.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupSearchView() {
        val searchView = view?.findViewById<SearchView>(R.id.searchView)
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        // Filter chips
        view?.findViewById<com.google.android.material.chip.Chip>(R.id.chipPinnedOnly)?.setOnCheckedChangeListener { _, _ ->
            viewModel.togglePinnedFilter()
        }

        view?.findViewById<com.google.android.material.chip.Chip>(R.id.chipClearFilters)?.setOnClickListener {
            viewModel.clearFilters()
            searchView?.setQuery("", false)
            view?.findViewById<com.google.android.material.chip.Chip>(R.id.chipPinnedOnly)?.isChecked = false
        }
    }

    private fun setupFab() {
        binding.fabAddNote.setOnClickListener {
            val action = NotesFragmentDirections.actionNotesFragmentToAddEditNoteFragment(-1L)
            findNavController().navigate(action)
        }
    }

    private fun observeNotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                notesAdapter.submitList(notes)
                updateEmptyState(notes.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.error?.let { error ->
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }

                // Update filter chip visibility
                val hasFilters = state.searchQuery.isNotBlank() ||
                                state.selectedSubjectId != null ||
                                state.selectedTaskId != null ||
                                state.showPinnedOnly
                view?.findViewById<com.google.android.material.chip.Chip>(R.id.chipClearFilters)?.isVisible = hasFilters
            }
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val note = notesAdapter.currentList[position]

                viewModel.deleteNote(note)

                Snackbar.make(binding.root, "Note deleted", Snackbar.LENGTH_LONG)
                    .setAction("Undo") {
                        viewModel.insertNote(note)
                    }
                    .show()
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        view?.findViewById<View>(R.id.emptyStateLayout)?.isVisible = isEmpty
        binding.recyclerViewNotes.isVisible = !isEmpty
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
