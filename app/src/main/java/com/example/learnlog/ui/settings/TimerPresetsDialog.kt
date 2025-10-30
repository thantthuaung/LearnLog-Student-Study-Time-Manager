package com.example.learnlog.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnlog.R
import com.example.learnlog.data.model.TimerPreset
import com.example.learnlog.databinding.DialogTimerPresetsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimerPresetsDialog : DialogFragment() {
    private var _binding: DialogTimerPresetsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    private lateinit var adapter: TimerPresetEditAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setView(onCreateView(layoutInflater, null, savedInstanceState))
            .setTitle("Timer Presets")
            .setPositiveButton("Done") { _, _ -> savePresets() }
            .setNegativeButton("Cancel", null)
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTimerPresetsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupObservers()
        setupAddButton()
    }

    private fun setupRecyclerView() {
        adapter = TimerPresetEditAdapter(
            onEdit = { preset, position -> showEditDialog(preset, position) },
            onDelete = { preset, position -> deletePreset(position) },
            onSetDefault = { position -> adapter.setDefaultIndex(position) }
        )

        binding.recyclerViewPresets.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewPresets.adapter = adapter

        val touchHelper = ItemTouchHelper(TimerPresetTouchHelper(adapter))
        touchHelper.attachToRecyclerView(binding.recyclerViewPresets)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                adapter.submitList(settings.timerPresets)
                adapter.setDefaultIndex(settings.defaultPresetIndex)
            }
        }
    }

    private fun setupAddButton() {
        binding.btnAddPreset.setOnClickListener {
            val currentList = adapter.currentList
            if (currentList.size >= 14) { // 8 default + 6 custom
                Snackbar.make(binding.root, "Maximum 14 presets allowed", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Duration (minutes, 1-180)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add Preset")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val minutes = input.text.toString().toIntOrNull()
                if (minutes == null || minutes < 1 || minutes > 180) {
                    Snackbar.make(binding.root, "Enter valid minutes (1-180)", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val currentList = adapter.currentList
                if (currentList.any { it.durationMinutes == minutes }) {
                    Snackbar.make(binding.root, "Preset already exists", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newPreset = TimerPreset(minutes, "$minutes min")
                val newList = currentList.toMutableList()
                newList.add(newPreset)
                adapter.submitList(newList)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(preset: TimerPreset, position: Int) {
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Duration (minutes, 1-180)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(preset.durationMinutes.toString())
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Preset")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val minutes = input.text.toString().toIntOrNull()
                if (minutes == null || minutes < 1 || minutes > 180) {
                    Snackbar.make(binding.root, "Enter valid minutes (1-180)", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val currentList = adapter.currentList
                if (currentList.any { it.durationMinutes == minutes && it != preset }) {
                    Snackbar.make(binding.root, "Preset already exists", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val newList = currentList.toMutableList()
                newList[position] = TimerPreset(minutes, "$minutes min")
                adapter.submitList(newList)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePreset(position: Int) {
        val currentList = adapter.currentList.toMutableList()
        currentList.removeAt(position)
        adapter.submitList(currentList)

        // Adjust default if needed
        viewLifecycleOwner.lifecycleScope.launch {
            val settings = viewModel.settings.value
            if (settings.defaultPresetIndex == position) {
                viewModel.updateDefaultPreset(0)
            } else if (settings.defaultPresetIndex > position) {
                viewModel.updateDefaultPreset(settings.defaultPresetIndex - 1)
            }
        }
    }

    private fun savePresets() {
        val presets = adapter.currentList
        viewModel.updateTimerPresets(presets)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

