package com.example.learnlog.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentSettingsBinding
import com.example.learnlog.auth.AuthManager
import com.example.learnlog.data.export.DataExporter
import com.example.learnlog.data.export.DataImporter
import com.example.learnlog.data.export.ImportMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    @Inject
    lateinit var dataExporter: DataExporter

    @Inject
    lateinit var dataImporter: DataImporter

    @Inject
    lateinit var authManager: AuthManager

    private var selectedExportFormat: ExportFormat = ExportFormat.JSON

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/*")
    ) { uri ->
        uri?.let { exportData(it) }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { previewImport(it) }
    }

    private enum class ExportFormat { JSON, CSV }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupObservers()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                binding.switchNotifications.isChecked = settings.notificationsEnabled
                binding.switchSound.isChecked = settings.soundEnabled
                binding.switchVibration.isChecked = settings.vibrationEnabled
                binding.tvSoundTone.text = settings.selectedSoundTone
            }
        }
    }

    private fun setupClickListeners() {
        // Timer Presets
        binding.cardTimerPresets.setOnClickListener {
            // TODO: Open timer presets dialog
            showTimerPresetsDialog()
        }

        // Notifications
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNotificationsEnabled(isChecked)
        }

        // Sound
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateSoundEnabled(isChecked)
        }

        // Vibration
        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateVibrationEnabled(isChecked)
        }

        // Sound Tone
        binding.cardSoundTone.setOnClickListener {
            showSoundToneDialog()
        }

        // Export Data
        binding.cardExport.setOnClickListener {
            showExportFormatDialog()
        }

        // Import Data
        binding.cardImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "application/zip", "application/octet-stream"))
        }

        // Clear All Data
        binding.cardClearData.setOnClickListener {
            showClearDataDialog()
        }

        // Sign Out
        binding.cardSignOut.setOnClickListener {
            showSignOutDialog()
        }

        // Version
        binding.tvVersion.text = "Version 1.0"
    }

    private fun showTimerPresetsDialog() {
        TimerPresetsDialog().show(childFragmentManager, "timer_presets")
    }

    private fun showSoundToneDialog() {
        val tones = arrayOf("Default", "Bell", "Chime", "Ding", "Silent")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Sound Tone")
            .setItems(tones) { _, which ->
                viewModel.updateSoundTone(tones[which].lowercase())
            }
            .show()
    }

    private fun showClearDataDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Clear All Data?")
            .setMessage("This will permanently delete all tasks, sessions, notes, and settings. This action cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                // TODO: Implement clear all data
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showExportFormatDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Export Format")
            .setItems(arrayOf("JSON", "CSV (ZIP)")) { _, which ->
                selectedExportFormat = if (which == 0) ExportFormat.JSON else ExportFormat.CSV
                val extension = if (selectedExportFormat == ExportFormat.JSON) "json" else "zip"
                val timestamp = System.currentTimeMillis()
                exportLauncher.launch("learnlog_backup_$timestamp.$extension")
            }
            .show()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun exportData(uri: Uri) {
        binding.progressIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val result = when (selectedExportFormat) {
                    ExportFormat.JSON -> dataExporter.exportToJson(uri)
                    ExportFormat.CSV -> dataExporter.exportToCsv(uri)
                }

                binding.progressIndicator.visibility = View.GONE

                when (result) {
                    is com.example.learnlog.data.export.ExportResult.Success -> {
                        Snackbar.make(
                            binding.root,
                            "Exported ${result.taskCount} tasks, ${result.sessionCount} sessions",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is com.example.learnlog.data.export.ExportResult.Error -> {
                        Snackbar.make(binding.root, "Export failed: ${result.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                binding.progressIndicator.visibility = View.GONE
                Snackbar.make(binding.root, "Export error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun previewImport(uri: Uri) {
        binding.progressIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                when (val preview = dataImporter.previewImport(uri)) {
                    is com.example.learnlog.data.export.ImportPreview.Success -> {
                        binding.progressIndicator.visibility = View.GONE
                        showImportOptionsDialog(uri, preview.taskCount, preview.sessionCount)
                    }
                    is com.example.learnlog.data.export.ImportPreview.Error -> {
                        binding.progressIndicator.visibility = View.GONE
                        Snackbar.make(binding.root, "Invalid file: ${preview.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                binding.progressIndicator.visibility = View.GONE
                Snackbar.make(binding.root, "Preview error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showImportOptionsDialog(uri: Uri, taskCount: Int, sessionCount: Int) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Import Data")
            .setMessage("Found $taskCount tasks and $sessionCount sessions.\n\nChoose import mode:")
            .setPositiveButton("Merge") { _, _ ->
                importData(uri, ImportMode.MERGE)
            }
            .setNegativeButton("Replace All") { _, _ ->
                confirmReplaceAll(uri)
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun confirmReplaceAll(uri: Uri) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Replace All Data?")
            .setMessage("This will DELETE all existing data and replace with imported data. This cannot be undone!")
            .setPositiveButton("Replace") { _, _ ->
                importData(uri, ImportMode.REPLACE)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun importData(uri: Uri, mode: ImportMode) {
        binding.progressIndicator.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                when (val result = dataImporter.importData(uri, mode)) {
                    is com.example.learnlog.data.export.ImportResult.Success -> {
                        binding.progressIndicator.visibility = View.GONE
                        Snackbar.make(
                            binding.root,
                            "Imported ${result.taskCount} tasks, ${result.sessionCount} sessions",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                    is com.example.learnlog.data.export.ImportResult.Error -> {
                        binding.progressIndicator.visibility = View.GONE
                        Snackbar.make(binding.root, "Import failed: ${result.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                binding.progressIndicator.visibility = View.GONE
                Snackbar.make(binding.root, "Import error: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun showSignOutDialog() {
        val currentUser = authManager.currentUser
        val userName = currentUser?.displayName ?: currentUser?.email ?: "User"
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out, $userName?")
            .setPositiveButton("Sign Out") { _, _ ->
                authManager.signOut()
                // Navigate to login screen
                findNavController().navigate(R.id.loginFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

