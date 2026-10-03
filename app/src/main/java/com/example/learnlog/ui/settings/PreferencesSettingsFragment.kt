package com.example.learnlog.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.learnlog.R
import com.example.learnlog.data.model.TimerPresets
import com.example.learnlog.databinding.FragmentSettingsPageBinding
import com.example.learnlog.ui.common.Dialogs
import com.example.learnlog.ui.common.Format
import com.example.learnlog.util.ThemePrefs
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PreferencesSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val builder = SettingsBuilder(binding.pageContainer)

        lateinit var themeRow: SettingsBuilder.Row
        builder.group(getString(R.string.pref_appearance)) {
            themeRow = row(R.drawable.ic_dark_mode, getString(R.string.pref_theme), value = themeLabel(viewModel.themeMode)) {
                chooseTheme { themeRow.value = themeLabel(it) }
            }
        }

        lateinit var defaultRow: SettingsBuilder.Row
        lateinit var presetsRow: SettingsBuilder.Row
        lateinit var goalRow: SettingsBuilder.Row
        lateinit var screenRow: SettingsBuilder.Row
        lateinit var runningRow: SettingsBuilder.Row
        lateinit var confirmRow: SettingsBuilder.Row
        builder.group(getString(R.string.pref_focus)) {
            defaultRow = row(R.drawable.ic_timer, getString(R.string.pref_default_preset)) { chooseDefaultPreset() }
            presetsRow = row(R.drawable.ic_tune, getString(R.string.pref_manage_presets), getString(R.string.pref_manage_presets_sub)) { editPresets() }
            goalRow = row(R.drawable.ic_target, getString(R.string.pref_daily_goal)) { editGoal() }
            screenRow = switch(R.drawable.ic_screen_on, getString(R.string.pref_keep_screen_on)) { viewModel.setKeepScreenOn(it) }
            runningRow = switch(R.drawable.ic_timelapse, getString(R.string.pref_keep_running), getString(R.string.pref_keep_running_sub)) { viewModel.setKeepRunning(it) }
            confirmRow = switch(R.drawable.ic_check_circle, getString(R.string.pref_confirm_stop)) { viewModel.setConfirmOnStop(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.appSettings.collect { s ->
                        defaultRow.value = Format.compact(s.defaultPreset.durationMinutes)
                        presetsRow.value = s.timerPresets.joinToString(" · ") { Format.compact(it.durationMinutes) }
                            .let { if (it.length > 22) "${s.timerPresets.size}" else it }
                    }
                }
                launch { viewModel.dailyGoal.collect { goalRow.value = Format.compact(it) } }
                launch { viewModel.keepScreenOn.collect { screenRow.checked = it } }
                launch { viewModel.keepRunning.collect { runningRow.checked = it } }
                launch { viewModel.confirmOnStop.collect { confirmRow.checked = it } }
            }
        }
    }

    private fun themeLabel(mode: Int) = getString(
        when (mode) {
            ThemePrefs.LIGHT -> R.string.theme_light
            ThemePrefs.DARK -> R.string.theme_dark
            else -> R.string.theme_system
        }
    )

    private fun chooseTheme(onChosen: (Int) -> Unit) {
        val modes = listOf(ThemePrefs.SYSTEM, ThemePrefs.LIGHT, ThemePrefs.DARK)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_theme)
            .setSingleChoiceItems(modes.map(::themeLabel).toTypedArray(), modes.indexOf(viewModel.themeMode).coerceAtLeast(0)) { dialog, which ->
                dialog.dismiss()
                onChosen(modes[which])
                // Applying recreates the activity, so let the dialog close first
                view?.postDelayed({ viewModel.themeMode = modes[which] }, 150)
            }
            .show()
    }

    private fun chooseDefaultPreset() {
        val settings = viewModel.appSettings.value
        val labels = settings.timerPresets.map { Format.minutes(requireContext(), it.durationMinutes) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pref_default_preset)
            .setSingleChoiceItems(labels, settings.defaultPresetIndex) { dialog, which ->
                viewModel.setDefaultPreset(which)
                dialog.dismiss()
            }
            .show()
    }

    /** Preset editor: tap a chip to remove it, "Add" to add a custom duration. */
    private fun editPresets() {
        val current = viewModel.appSettings.value.timerPresets.map { it.durationMinutes }.toMutableList()
        val group = ChipGroup(requireContext()).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
        }
        fun render() {
            group.removeAllViews()
            current.sorted().forEach { minutes ->
                group.addView(Chip(requireContext()).apply {
                    text = Format.compact(minutes)
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        if (current.size <= 1) snack(getString(R.string.presets_min_one))
                        else { current.remove(minutes); render() }
                    }
                })
            }
            group.addView(Chip(requireContext()).apply {
                text = getString(R.string.presets_add)
                setChipIconResource(R.drawable.ic_add)
                isChipIconVisible = true
                setOnClickListener {
                    if (current.size >= 8) { snack(getString(R.string.presets_max)); return@setOnClickListener }
                    Dialogs.askMinutes(requireContext(), getString(R.string.presets_add), 20,
                        TimerPresets.MIN_DURATION_MINUTES, TimerPresets.MAX_DURATION_MINUTES) { m ->
                        if (m !in current) { current.add(m); render() }
                    }
                }
            })
        }
        render()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.presets_title)
            .setMessage(R.string.presets_message)
            .setView(group)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ -> viewModel.setPresets(current) }
            .show()
    }

    private fun editGoal() {
        viewLifecycleOwner.lifecycleScope.launch {
            Dialogs.askMinutes(requireContext(), getString(R.string.daily_goal_title), viewModel.dailyGoal.first(), 10, 720) {
                viewModel.setDailyGoal(it)
            }
        }
    }

    private fun snack(message: String) = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
