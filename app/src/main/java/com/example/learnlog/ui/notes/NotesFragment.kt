package com.example.learnlog.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentNotesBinding
import com.example.learnlog.ui.base.BaseFragment
import com.example.learnlog.ui.common.Motion.animateFirstLayout
import com.example.learnlog.ui.common.Reselectable
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotesFragment : BaseFragment(), Reselectable {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotesViewModel by viewModels()
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopBar(binding.topBar.root, getString(R.string.page_notes_title))

        notesAdapter = NotesAdapter(
            onClick = { openEditor(it.id) },
            onPinClick = viewModel::togglePin
        )
        binding.recyclerViewNotes.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.recyclerViewNotes.adapter = notesAdapter
        setupSwipeToDelete()

        binding.searchInput.doAfterTextChanged { viewModel.setSearchQuery(it?.toString().orEmpty()) }
        binding.chipPinnedOnly.setOnCheckedChangeListener { _, checked -> viewModel.setPinnedOnly(checked) }
        binding.fabAddNote.setOnClickListener { openEditor(-1L) }
        binding.emptyState.emptyAction.setOnClickListener { openEditor(-1L) }
        binding.recyclerViewNotes.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 8 && binding.fabAddNote.isExtended) binding.fabAddNote.shrink()
                else if ((dy < -8 || !rv.canScrollVertically(-1)) && !binding.fabAddNote.isExtended) binding.fabAddNote.extend()
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { if (it.loaded) render(it) }
            }
        }
    }

    private fun render(state: NotesUiState) {
        notesAdapter.submitList(state.notes) {
            if (state.notes.isNotEmpty()) binding.recyclerViewNotes.animateFirstLayout()
        }
        setTopBarSubtitle(binding.topBar.root,
            if (state.totalCount > 0) resources.getQuantityString(R.plurals.notes_count, state.totalCount, state.totalCount) else null)
        if (binding.chipPinnedOnly.isChecked != state.pinnedOnly) binding.chipPinnedOnly.isChecked = state.pinnedOnly

        val empty = state.notes.isEmpty()
        binding.emptyContainer.isVisible = empty
        binding.recyclerViewNotes.isVisible = !empty
        if (empty && !state.isFiltering) binding.fabAddNote.hide() else binding.fabAddNote.show()
        if (empty) with(binding.emptyState) {
            if (state.isFiltering) {
                emptyIcon.setImageResource(R.drawable.ic_search)
                emptyTitle.setText(R.string.empty_search_title)
                emptyMessage.setText(R.string.no_notes_found)
                emptyAction.isVisible = false
            } else {
                emptyIcon.setImageResource(R.drawable.ic_empty_notes)
                emptyTitle.setText(R.string.no_notes_title)
                emptyMessage.setText(R.string.no_notes_message)
                emptyAction.setText(R.string.notes_new)
                emptyAction.isVisible = true
            }
        }
    }

    private fun setupSwipeToDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val note = notesAdapter.currentList.getOrNull(viewHolder.bindingAdapterPosition) ?: return
                viewModel.deleteNote(note)
                Snackbar.make(binding.root, R.string.note_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_undo) { viewModel.restoreNote(note) }
                    .setAnchorView(binding.fabAddNote)
                    .show()
            }
        }).attachToRecyclerView(binding.recyclerViewNotes)
    }

    private fun openEditor(noteId: Long) {
        findNavController().navigate(NotesFragmentDirections.actionNotesFragmentToAddEditNoteFragment(noteId))
    }

    override fun onReselected() {
        _binding ?: return
        binding.recyclerViewNotes.smoothScrollToPosition(0)
        binding.appBar.setExpanded(true, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
