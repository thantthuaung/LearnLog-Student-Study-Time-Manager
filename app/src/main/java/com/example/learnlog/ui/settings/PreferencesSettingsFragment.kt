package com.example.learnlog.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.learnlog.databinding.SettingsPreferencesBinding
import com.example.learnlog.ui.settings.TimerPresetsDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PreferencesSettingsFragment : Fragment() {
    private var _binding: SettingsPreferencesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsPreferencesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    private fun setupViews() {
        binding.btnSelectDefaultPreset.setOnClickListener {
            // Open preset selection dialog
            // This would show a dialog with available presets
            Snackbar.make(binding.root, "Preset selection - to be implemented with timer preset data", Snackbar.LENGTH_SHORT).show()
        }

        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateKeepScreenOn(isChecked)
        }

        binding.switchConfirmOnStop.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateConfirmOnStop(isChecked)
        }

        binding.btnManagePresets.setOnClickListener {
            // Open timer presets management dialog
            TimerPresetsDialog().show(childFragmentManager, "timer_presets")
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.defaultPresetId.collect { presetId ->
                val text = if (presetId != null) {
                    "Preset ID: $presetId" // TODO: Fetch actual preset name
                } else {
                    "None selected"
                }
                binding.defaultPresetValue.text = text
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.keepScreenOn.collect { enabled ->
                if (binding.switchKeepScreenOn.isChecked != enabled) {
                    binding.switchKeepScreenOn.isChecked = enabled
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.confirmOnStop.collect { enabled ->
                if (binding.switchConfirmOnStop.isChecked != enabled) {
                    binding.switchConfirmOnStop.isChecked = enabled
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

