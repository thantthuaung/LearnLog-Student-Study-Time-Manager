package com.example.learnlog.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.learnlog.R
import com.example.learnlog.data.Note
import com.example.learnlog.databinding.FragmentAddEditNoteBinding
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddEditNoteFragment : Fragment() {

    private var _binding: FragmentAddEditNoteBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddEditNoteViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        observeNote()
        observeTags()
        setupTagInput()
        setupSaveButton()
    }

    private fun observeNote() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.note.collectLatest { note: Note? ->
                note?.let { currentNote ->
                    binding.editTextTitle.setText(currentNote.title)
                    binding.editTextContent.setText(currentNote.content)
                }
            }
        }
    }

    private fun observeTags() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tags.collect { tags ->
                val chipGroup = view?.findViewById<ChipGroup>(R.id.chipGroupTags)
                chipGroup?.removeAllViews()
                tags.forEach { tag ->
                    addTagChip(tag, chipGroup)
                }
            }
        }
    }

    private fun setupTagInput() {
        view?.findViewById<Button>(R.id.buttonAddTag)?.setOnClickListener {
            showAddTagDialog()
        }
    }

    private fun showAddTagDialog() {
        val input = TextInputEditText(requireContext()).apply {
            hint = "Tag name"
            setPadding(64, 32, 64, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Tag")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val tag = input.text.toString().trim()
                if (tag.isNotBlank()) {
                    viewModel.addTag(tag)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun addTagChip(tag: String, chipGroup: ChipGroup?) {
        val chip = Chip(requireContext()).apply {
            text = tag
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                viewModel.removeTag(tag)
            }
        }
        chipGroup?.addView(chip)
    }

    private fun setupSaveButton() {
        binding.fabSaveNote.setOnClickListener {
            val title = binding.editTextTitle.text.toString()
            val content = binding.editTextContent.text.toString()

            if (title.isBlank()) {
                binding.textInputLayoutTitle.error = "Title is required"
                return@setOnClickListener
            }

            binding.textInputLayoutTitle.error = null
            viewModel.saveNote(title, content)
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
