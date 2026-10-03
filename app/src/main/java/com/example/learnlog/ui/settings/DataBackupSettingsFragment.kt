package com.example.learnlog.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.learnlog.R
import com.example.learnlog.data.export.DataExporter
import com.example.learnlog.data.export.DataImporter
import com.example.learnlog.data.export.ExportResult
import com.example.learnlog.data.export.ImportMode
import com.example.learnlog.data.export.ImportResult
import com.example.learnlog.databinding.FragmentSettingsPageBinding
import com.example.learnlog.databinding.SettingsDataOverviewBinding
import com.example.learnlog.ui.common.Motion
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class DataBackupSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels({ requireParentFragment() })

    @Inject lateinit var dataExporter: DataExporter
    @Inject lateinit var dataImporter: DataImporter

    private var pendingImportMode = ImportMode.MERGE
    private var busy = false

    private val createJson = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { export { dataExporter.exportToJson(it) } }
    }
    private val createZip = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { export { dataExporter.exportToCsvZip(it) } }
    }
    private val pickBackup = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { import(it, pendingImportMode) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val builder = SettingsBuilder(binding.pageContainer)

        val overview = SettingsDataOverviewBinding.inflate(layoutInflater, binding.pageContainer, false)
        builder.custom(overview.root)

        builder.group(getString(R.string.data_backup)) {
            row(R.drawable.ic_download, getString(R.string.data_export_json), getString(R.string.data_export_json_sub)) {
                if (!busy) createJson.launch("learnlog_backup_${stamp()}.json")
            }
            row(R.drawable.ic_layers, getString(R.string.data_export_csv), getString(R.string.data_export_csv_sub)) {
                if (!busy) createZip.launch("learnlog_spreadsheets_${stamp()}.zip")
            }
        }
        builder.group(getString(R.string.data_restore)) {
            row(R.drawable.ic_upload, getString(R.string.data_import_merge), getString(R.string.data_import_merge_sub)) {
                pendingImportMode = ImportMode.MERGE
                if (!busy) pickBackup.launch(MIME_TYPES)
            }
            row(R.drawable.ic_restart, getString(R.string.data_import_replace), getString(R.string.data_import_replace_sub)) {
                MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_LearnLog_AlertDialog_Destructive)
                    .setTitle(R.string.data_replace_title)
                    .setMessage(R.string.data_replace_message)
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.data_replace_confirm) { _, _ ->
                        pendingImportMode = ImportMode.REPLACE
                        pickBackup.launch(MIME_TYPES)
                    }
                    .show()
            }
        }
        builder.group(getString(R.string.data_danger)) {
            row(R.drawable.ic_delete_forever, getString(R.string.data_reset), getString(R.string.data_reset_sub), destructive = true) {
                confirmReset()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.counts.collect { c ->
                    Motion.countTo(overview.tvTaskCount, c.tasks) { it.toString() }
                    Motion.countTo(overview.tvSessionCount, c.sessions) { it.toString() }
                    Motion.countTo(overview.tvNoteCount, c.notes) { it.toString() }
                }
            }
        }
    }

    private fun export(block: suspend () -> ExportResult) {
        busy = true
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = block()) {
                is ExportResult.Success -> snack(getString(R.string.data_exported, result.taskCount, result.sessionCount, result.noteCount))
                is ExportResult.Error -> snack(getString(R.string.data_export_failed, result.message))
            }
            busy = false
        }
    }

    private fun import(uri: Uri, mode: ImportMode) {
        busy = true
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = dataImporter.importData(uri, mode)) {
                is ImportResult.Success -> snack(getString(R.string.data_imported, result.taskCount, result.sessionCount, result.noteCount))
                is ImportResult.Error -> snack(getString(R.string.data_import_failed, result.message))
            }
            busy = false
        }
    }

    private fun confirmReset() {
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_LearnLog_AlertDialog_Destructive)
            .setIcon(R.drawable.ic_delete_forever)
            .setTitle(R.string.data_reset_title)
            .setMessage(R.string.data_reset_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.data_reset_confirm) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.resetAllData()
                    snack(getString(R.string.data_reset_done))
                }
            }
            .show()
    }

    private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())

    private fun snack(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val MIME_TYPES = arrayOf("application/json", "application/zip", "application/octet-stream", "text/plain")
    }
}
