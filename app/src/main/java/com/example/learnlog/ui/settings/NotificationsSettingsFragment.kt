package com.example.learnlog.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.databinding.FragmentSettingsPageBinding
import com.example.learnlog.timer.TimerAlerts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationsSettingsFragment : Fragment() {

    private var _binding: FragmentSettingsPageBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels({ requireParentFragment() })

    private lateinit var enabledRow: SettingsBuilder.Row
    private lateinit var soundRow: SettingsBuilder.Row
    private lateinit var vibrateRow: SettingsBuilder.Row
    private lateinit var ongoingRow: SettingsBuilder.Row
    private lateinit var blockedRow: SettingsBuilder.Row
    private lateinit var blockedGroup: SettingsBuilder.Group

    private val sounds = listOf(UserPreferences.SOUND_DEFAULT, UserPreferences.SOUND_ALARM, UserPreferences.SOUND_NONE)

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.setNotificationsEnabled(granted)
        refreshBlocked()
        if (!granted) Snackbar.make(binding.root, R.string.notif_permission_denied, Snackbar.LENGTH_LONG)
            .setAction(R.string.notif_open_settings) { openSystemSettings() }.show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val builder = SettingsBuilder(binding.pageContainer)
        val labels = resources.getStringArray(R.array.sound_options)

        builder.group(getString(R.string.settings_notifications)) {
            enabledRow = switch(R.drawable.ic_notifications, getString(R.string.notif_enabled), getString(R.string.notif_enabled_sub)) { on ->
                if (on && !TimerAlerts.canPostNotifications(requireContext()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setNotificationsEnabled(on)
                }
            }
            soundRow = row(R.drawable.ic_volume, getString(R.string.notif_sound)) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.notif_sound)
                    .setSingleChoiceItems(labels, sounds.indexOf(currentSound).coerceAtLeast(0)) { d, which ->
                        viewModel.setNotificationSound(sounds[which]); d.dismiss()
                    }
                    .show()
            }
            vibrateRow = switch(R.drawable.ic_vibration, getString(R.string.notif_vibrate)) { viewModel.setVibrate(it) }
            ongoingRow = switch(R.drawable.ic_timelapse, getString(R.string.notif_ongoing), getString(R.string.notif_ongoing_sub)) { viewModel.setOngoing(it) }
        }
        blockedGroup = builder.group(null) {
            blockedRow = row(R.drawable.ic_info, getString(R.string.notif_permission_denied), destructive = true) { openSystemSettings() }
        }
        refreshBlocked()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notificationsEnabled.collect { on ->
                        val effective = on && TimerAlerts.canPostNotifications(requireContext())
                        enabledRow.checked = on
                        soundRow.enabled = on
                        vibrateRow.enabled = on
                        refreshBlocked(on && !effective)
                    }
                }
                launch {
                    viewModel.notificationSound.collect {
                        currentSound = it
                        soundRow.value = labels[sounds.indexOf(it).coerceAtLeast(0)]
                    }
                }
                launch { viewModel.vibrate.collect { vibrateRow.checked = it } }
                launch { viewModel.ongoing.collect { ongoingRow.checked = it } }
            }
        }
    }

    private var currentSound: String = UserPreferences.SOUND_DEFAULT

    override fun onResume() {
        super.onResume()
        if (::blockedGroup.isInitialized) refreshBlocked()
    }

    private fun refreshBlocked(show: Boolean = !TimerAlerts.canPostNotifications(requireContext())) {
        blockedGroup.binding.root.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        runCatching { startActivity(intent) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
