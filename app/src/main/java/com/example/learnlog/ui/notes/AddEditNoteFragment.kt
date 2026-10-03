package com.example.learnlog.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentAddEditNoteBinding
import com.example.learnlog.ui.common.Format
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class AddEditNoteFragment : Fragment() {

    private var _binding: FragmentAddEditNoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEditNoteViewModel by viewModels()
    private var closing = false

    private val editedFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddEditNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { saveAndClose() }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { saveAndClose() }
        binding.fabSaveNote.setOnClickListener { saveAndClose() }
        binding.chipAddTag.setOnClickListener { showAddTagDialog() }
        binding.toolbar.menu.findItem(R.id.action_delete).isVisible = !viewModel.isNew
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_pin -> { viewModel.togglePinned(); true }
                R.id.action_delete -> { confirmDelete(); true }
                else -> false
            }
        }
        binding.editTextContent.doAfterTextChanged { updateMeta() }

        viewLifecycleOwner.lifecycleScope.launch {
            launch {
                viewModel.note.collect { note ->
                    if (note != null && !viewModel.populated) {
                        binding.editTextTitle.setText(note.title)
                        binding.editTextContent.setText(note.content)
                        viewModel.populated = true
                    }
                    binding.toolbar.menu.findItem(R.id.action_delete).isVisible = note != null
                    updateMeta()
                }
            }
            launch { viewModel.tags.collect(::renderTags) }
            launch {
                viewModel.pinned.collect { pinned ->
                    binding.toolbar.menu.findItem(R.id.action_pin).apply {
                        setIcon(if (pinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outlined)
                        title = getString(if (pinned) R.string.note_unpin else R.string.note_pin)
                        icon?.setTint(ContextCompat.getColor(requireContext(), if (pinned) R.color.ll_primary else R.color.ll_on_surface_variant))
                    }
                }
            }
        }

        if (viewModel.isNew && savedInstanceState == null) {
            viewModel.populated = true
            binding.editTextTitle.requestFocus()
            binding.editTextTitle.post {
                (requireContext().getSystemService(InputMethodManager::class.java))
                    ?.showSoftInput(binding.editTextTitle, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    private fun updateMeta() {
        val words = binding.editTextContent.text.toString().split(Regex("\\s+")).count { it.isNotBlank() }
        val wordsText = getString(R.string.note_words, words)
        val updated = viewModel.note.value?.updatedAt
        binding.textMeta.text = if (updated != null) getString(R.string.note_edited, updated.format(editedFormat)) + " · " + wordsText else wordsText
    }

    private fun renderTags(tags: List<String>) {
        val group = binding.chipGroupTags
        // Keep the trailing "Add tag" chip, rebuild the rest
        for (i in group.childCount - 1 downTo 0) {
            if (group.getChildAt(i).id != R.id.chip_add_tag) group.removeViewAt(i)
        }
        tags.forEachIndexed { i, tag ->
            val chip = Chip(requireContext()).apply {
                text = "#$tag"
                isCloseIconVisible = true
                setOnCloseIconClickListener { viewModel.removeTag(tag) }
                setTextAppearanceResource(R.style.TextAppearance_LearnLog_LabelLarge)
            }
            group.addView(chip, i)
        }
    }

    private fun showAddTagDialog() {
        val layout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.note_tag_hint)
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            prefixText = "#"
        }
        val input = TextInputEditText(layout.context).apply { setSingleLine() }
        layout.addView(input)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.note_add_tag)
            .setView(layout)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_add) { _, _ -> viewModel.addTag(input.text.toString()) }
            .show()
        input.requestFocus()
    }

    private fun saveAndClose() {
        if (closing) return
        val title = binding.editTextTitle.text.toString()
        val content = binding.editTextContent.text.toString()
        if (title.isBlank() && content.isBlank() && viewModel.isNew) {
            close(); return
        }
        closing = true
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.save(title, content)) close()
            else {
                closing = false
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.note_discard_title)
                    .setMessage(R.string.note_discard_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.timer_discard) { _, _ -> close() }
                    .show()
            }
        }
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_LearnLog_AlertDialog_Destructive)
            .setTitle(R.string.note_delete_title)
            .setMessage(R.string.note_delete_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                closing = true
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.delete()
                    close()
                }
            }
            .show()
    }

    private fun close() {
        view?.let { v ->
            requireContext().getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(v.windowToken, 0)
        }
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
