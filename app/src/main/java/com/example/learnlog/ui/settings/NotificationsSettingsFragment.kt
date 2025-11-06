package com.example.learnlog.ui.settings

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.learnlog.databinding.SettingsNotificationsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationsSettingsFragment : Fragment() {
    private var _binding: SettingsNotificationsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels({ requireParentFragment() })

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.updateNotificationsEnabled(true)
            createNotificationChannel()
            Snackbar.make(binding.root, "Notifications enabled", Snackbar.LENGTH_SHORT).show()
        } else {
            binding.switchNotificationsEnabled.isChecked = false
            Snackbar.make(binding.root, "Notification permission denied", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SettingsNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeData()
    }

    private fun setupViews() {
        binding.switchNotificationsEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkAndRequestNotificationPermission()
            } else {
                viewModel.updateNotificationsEnabled(false)
            }
        }

        binding.soundContainer.setOnClickListener {
            showSoundSelectionDialog()
        }

        binding.switchVibrate.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateNotificationVibrate(isChecked)
        }

        binding.switchOngoingNotification.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateShowOngoingNotification(isChecked)
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    viewModel.updateNotificationsEnabled(true)
                    createNotificationChannel()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Notification Permission")
                        .setMessage("LearnLog needs notification permission to alert you when timers complete.")
                        .setPositiveButton("Grant") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            binding.switchNotificationsEnabled.isChecked = false
                        }
                        .show()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // No runtime permission needed before Android 13
            viewModel.updateNotificationsEnabled(true)
            createNotificationChannel()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Notifications"
            val descriptionText = "Notifications for timer completion and updates"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("learnlog_timer", name, importance).apply {
                description = descriptionText
            }

            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showSoundSelectionDialog() {
        val sounds = arrayOf("Default", "Bell", "Chime", "Ding", "None")
        var selectedIndex = 0

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationSound.collect { currentSound ->
                selectedIndex = sounds.indexOfFirst { it.lowercase() == currentSound.lowercase() }.coerceAtLeast(0)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Notification Sound")
            .setSingleChoiceItems(sounds, selectedIndex) { dialog, which ->
                val selected = sounds[which].lowercase()
                viewModel.updateNotificationSound(selected)
                binding.notificationSoundValue.text = sounds[which]
                dialog.dismiss()
                Snackbar.make(binding.root, "Sound changed to ${sounds[which]}", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationsEnabled.collect { enabled ->
                if (binding.switchNotificationsEnabled.isChecked != enabled) {
                    binding.switchNotificationsEnabled.isChecked = enabled
                }

                // Enable/disable other controls
                binding.soundContainer.isEnabled = enabled
                binding.switchVibrate.isEnabled = enabled
                binding.switchOngoingNotification.isEnabled = enabled
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationSound.collect { sound ->
                binding.notificationSoundValue.text = sound.replaceFirstChar { it.uppercase() }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationVibrate.collect { enabled ->
                if (binding.switchVibrate.isChecked != enabled) {
                    binding.switchVibrate.isChecked = enabled
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showOngoingNotification.collect { enabled ->
                if (binding.switchOngoingNotification.isChecked != enabled) {
                    binding.switchOngoingNotification.isChecked = enabled
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

