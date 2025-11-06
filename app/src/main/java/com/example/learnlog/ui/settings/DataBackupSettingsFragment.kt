package com.example.learnlog.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.learnlog.data.export.DataExporter
import com.example.learnlog.data.export.DataImporter
import com.example.learnlog.data.export.ImportMode
import com.example.learnlog.data.export.ImportResult
import com.example.learnlog.databinding.SettingsDataBackupBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DataBackupSettingsFragment : Fragment() {
    private var _binding: SettingsDataBackupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DataBackupViewModel by viewModels()

    @Inject
    lateinit var dataExporter: DataExporter

    @Inject
    lateinit var dataImporter: DataImporter

    private var pendingImportMode: ImportMode = ImportMode.MERGE

    private val createJsonDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { exportToJson(it) }
    }

    private val createCsvDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let { exportToCsv(it) }
    }

    private val pickDocument = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { importData(it, pendingImportMode) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsDataBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    private fun setupViews() {
        binding.btnExportJson.setOnClickListener {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            createJsonDocument.launch("learnlog_backup_$timestamp.json")
        }

        binding.btnExportCsv.setOnClickListener {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            createCsvDocument.launch("learnlog_backup_$timestamp.csv")
        }

        binding.btnImportMerge.setOnClickListener {
            pendingImportMode = ImportMode.MERGE
            pickDocument.launch(arrayOf("application/json", "text/csv"))
        }

        binding.btnImportReplace.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Replace All Data?")
                .setMessage("This will delete all existing data and replace it with the imported data. This cannot be undone.")
                .setPositiveButton("Replace All") { _, _ ->
                    pendingImportMode = ImportMode.REPLACE
                    pickDocument.launch(arrayOf("application/json", "text/csv"))
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun exportToJson(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.btnExportJson.isEnabled = false
                binding.btnExportCsv.isEnabled = false

                withContext(Dispatchers.IO) {
                    dataExporter.exportToJson(uri)
                }

                Snackbar.make(binding.root, "Data exported successfully", Snackbar.LENGTH_LONG)
                    .setAction("Open") {
                        openFile(uri)
                    }
                    .show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.btnExportJson.isEnabled = true
                binding.btnExportCsv.isEnabled = true
            }
        }
    }

    private fun exportToCsv(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.btnExportJson.isEnabled = false
                binding.btnExportCsv.isEnabled = false

                withContext(Dispatchers.IO) {
                    dataExporter.exportToCsv(uri)
                }

                Snackbar.make(binding.root, "Data exported successfully", Snackbar.LENGTH_LONG)
                    .setAction("Open") {
                        openFile(uri)
                    }
                    .show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Export failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.btnExportJson.isEnabled = true
                binding.btnExportCsv.isEnabled = true
            }
        }
    }

    private fun importData(uri: Uri, mode: ImportMode) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.btnImportMerge.isEnabled = false
                binding.btnImportReplace.isEnabled = false

                val result = withContext(Dispatchers.IO) {
                    dataImporter.importData(uri, mode)
                }

                when (result) {
                    is ImportResult.Success -> {
                        Snackbar.make(
                            binding.root,
                            "Imported ${result.taskCount} tasks, ${result.sessionCount} sessions",
                            Snackbar.LENGTH_LONG
                        ).show()

                        // Refresh counts
                        viewModel.refreshCounts()
                    }
                    is ImportResult.Error -> {
                        Snackbar.make(binding.root, "Import failed: ${result.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Import failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                binding.btnImportMerge.isEnabled = true
                binding.btnImportReplace.isEnabled = true
            }
        }
    }

    private fun openFile(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, requireContext().contentResolver.getType(uri))
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            // No app to open file
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.taskCount.collect { count ->
                binding.tvTaskCount.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.sessionCount.collect { count ->
                binding.tvSessionCount.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.plannerCount.collect { count ->
                binding.tvPlannerCount.text = count.toString()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

