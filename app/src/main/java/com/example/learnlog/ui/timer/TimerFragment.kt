package com.example.learnlog.ui.timer

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.learnlog.R
import com.example.learnlog.data.model.TimerPreset
import com.example.learnlog.data.model.TimerPresets
import com.example.learnlog.data.model.TimerState
import com.example.learnlog.databinding.FragmentTimerBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class TimerFragment : Fragment() {
    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels()

    private var timerState = TimerState.IDLE
    private var currentDurationMs = 25 * 60 * 1000L
    private var remainingMillis = currentDurationMs
    private var countDownTimer: CountDownTimer? = null
    private var sessionStarted = false
    private var selectedPresetIndex = 4 // Default to 25 minutes

    private lateinit var notificationManager: TimerNotificationManager
    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null

    private lateinit var presetAdapter: TimerPresetAdapter

    companion object {
        private const val STATE_DURATION_MS = "duration_ms"
        private const val STATE_REMAINING_MS = "remaining_ms"
        private const val STATE_IS_RUNNING = "is_running"
        private const val STATE_SESSION_STARTED = "session_started"
        private const val STATE_SELECTED_PRESET = "selected_preset"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Restore state
        savedInstanceState?.let {
            currentDurationMs = it.getLong(STATE_DURATION_MS, 25 * 60 * 1000L)
            remainingMillis = it.getLong(STATE_REMAINING_MS, currentDurationMs)
            sessionStarted = it.getBoolean(STATE_SESSION_STARTED, false)
            selectedPresetIndex = it.getInt(STATE_SELECTED_PRESET, 4)

            if (it.getBoolean(STATE_IS_RUNNING, false)) {
                // Will resume in onViewCreated
                timerState = TimerState.PAUSED
            }
        }

        // Handle system back button
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            handleBackPress()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DURATION_MS, currentDurationMs)
        outState.putLong(STATE_REMAINING_MS, remainingMillis)
        outState.putBoolean(STATE_IS_RUNNING, timerState == TimerState.RUNNING)
        outState.putBoolean(STATE_SESSION_STARTED, sessionStarted)
        outState.putInt(STATE_SELECTED_PRESET, selectedPresetIndex)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notificationManager = TimerNotificationManager(requireContext())
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(requireContext(), notificationUri)

        setupUI()
        setupControls()
        setupPresets()
        updateTimerDisplay()
    }

    private fun setupUI() {
        // Regular timer page title
        binding.topBar.pageTitle.text = getString(R.string.nav_timer)

        // Hide task-specific UI (standalone timer mode)
        binding.taskInfoContainer.isVisible = false
        binding.btnMarkCompleted.isVisible = false
        binding.btnAddNote.isVisible = false
        binding.taskActionsContainer.visibility = View.GONE
        binding.textPrompt.visibility = View.GONE

        // Set up presets in 3-column grid (9 items: 8 presets + custom)
        binding.presetsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
    }

    private fun setupPresets() {
        presetAdapter = TimerPresetAdapter(
            onPresetClick = { preset ->
                if (timerState == TimerState.RUNNING) {
                    Snackbar.make(binding.root, "Stop timer to change duration", Snackbar.LENGTH_SHORT).show()
                    return@TimerPresetAdapter
                }

                if (preset.durationMinutes == 0) {
                    showCustomDurationPicker()
                } else {
                    setDuration(preset.durationMinutes)
                }
            },
            onPresetLongClick = { preset ->
                // Long press for future custom preset saving
                Snackbar.make(binding.root, "Long press to save custom preset (coming soon)", Snackbar.LENGTH_SHORT).show()
                true
            }
        )

        binding.presetsRecyclerView.adapter = presetAdapter

        // Add default presets plus custom option
        val presets = TimerPresets.DEFAULT_PRESETS.toMutableList()
        presets.add(TimerPreset(0, "Custom"))
        presetAdapter.submitList(presets)

        // Highlight selected preset
        presetAdapter.setSelectedIndex(selectedPresetIndex)
    }

    private fun setupControls() {
        // Start/Pause button
        binding.btnStartPause.setOnClickListener {
            when (timerState) {
                TimerState.IDLE, TimerState.PAUSED -> startTimer()
                TimerState.RUNNING -> pauseTimer()
                else -> {}
            }
        }

        // Reset button
        binding.btnReset.setOnClickListener {
            resetTimer()
        }

        // Settings button (hidden for now)
        binding.btnSettings.setOnClickListener {
            showTimerSettings()
        }

        // Time adjustment buttons
        binding.btnIncrease.setOnClickListener {
            adjustTime(5 * 60 * 1000L) // +5 minutes
        }

        binding.btnDecrease.setOnClickListener {
            adjustTime(-1 * 60 * 1000L) // -1 minute
        }

        // Keep screen on toggle
        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun startTimer() {
        // Start session on first start (standalone, no task)
        if (!sessionStarted && timerState == TimerState.IDLE) {
            val durationMinutes = (currentDurationMs / 1000 / 60).toInt()
            viewModel.startStandaloneSession(durationMinutes)
            sessionStarted = true
        }

        timerState = TimerState.RUNNING
        binding.btnStartPause.text = getString(R.string.pause_timer)
        binding.btnStartPause.setIconResource(R.drawable.ic_pause)
        binding.btnReset.visibility = View.VISIBLE
        binding.presetsRecyclerView.isEnabled = false

        // Show notification
        notificationManager.showTimerNotification(remainingMillis)

        countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                updateTimerDisplay()

                // Update notification every 10 seconds
                if (millisUntilFinished % 10000 < 1000) {
                    notificationManager.updateTimerNotification(millisUntilFinished)
                }
            }

            override fun onFinish() {
                timerState = TimerState.IDLE
                remainingMillis = 0
                updateTimerDisplay()
                onTimerComplete()
            }
        }.start()
    }

    private fun setDuration(minutes: Int) {
        if (timerState == TimerState.RUNNING) {
            Snackbar.make(binding.root, "Stop timer to change duration", Snackbar.LENGTH_SHORT).show()
            return
        }

        currentDurationMs = minutes * 60 * 1000L
        remainingMillis = currentDurationMs

        // Update selected preset index
        val presets = TimerPresets.DEFAULT_PRESETS
        selectedPresetIndex = presets.indexOfFirst { it.durationMinutes == minutes }
        if (selectedPresetIndex == -1) selectedPresetIndex = presets.size // Custom

        presetAdapter.setSelectedIndex(selectedPresetIndex)
        updateTimerDisplay()
    }

    private fun adjustTime(deltaMs: Long) {
        if (timerState == TimerState.IDLE) {
            // Adjust preset duration
            val newDuration = (currentDurationMs + deltaMs).coerceIn(60 * 1000L, 180 * 60 * 1000L)
            currentDurationMs = newDuration
            remainingMillis = newDuration
            selectedPresetIndex = -1 // Mark as custom
        } else if (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED) {
            // Adjust remaining time live
            val newRemaining = (remainingMillis + deltaMs).coerceIn(60 * 1000L, 180 * 60 * 1000L)
            remainingMillis = newRemaining
            currentDurationMs = remainingMillis // Update total duration too
        }
        updateTimerDisplay()
    }

    private fun showCustomDurationPicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(0)
            .setMinute(25)
            .setTitleText("Select Duration")
            .build()

        picker.addOnPositiveButtonClickListener {
            val hours = picker.hour
            val minutes = picker.minute
            val totalMinutes = hours * 60 + minutes

            if (totalMinutes < TimerPresets.MIN_DURATION_MINUTES) {
                Snackbar.make(binding.root, "Minimum duration is 1 minute", Snackbar.LENGTH_SHORT).show()
            } else if (totalMinutes > TimerPresets.MAX_DURATION_MINUTES) {
                Snackbar.make(binding.root, "Maximum duration is 3 hours", Snackbar.LENGTH_SHORT).show()
            } else {
                setDuration(totalMinutes)
            }
        }

        picker.show(childFragmentManager, "time_picker")
    }

    private fun updateTimerDisplay() {
        val minutes = remainingMillis / 1000 / 60
        val seconds = remainingMillis / 1000 % 60
        binding.textCountdown.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        // Update progress
        val progress = if (currentDurationMs > 0) {
            ((currentDurationMs - remainingMillis).toFloat() / currentDurationMs * 100).toInt()
        } else 0
        binding.progressCircle.progress = progress
    }

    private fun pauseTimer() {
        timerState = TimerState.PAUSED
        binding.btnStartPause.text = getString(R.string.resume_timer)
        binding.btnStartPause.setIconResource(R.drawable.ic_play)
        countDownTimer?.cancel()

        // Update notification to paused state
        notificationManager.updateTimerNotification(remainingMillis, isPaused = true)
    }

    private fun resetTimer() {
        timerState = TimerState.IDLE
        countDownTimer?.cancel()
        sessionStarted = false
        remainingMillis = currentDurationMs
        updateTimerDisplay()
        binding.btnStartPause.text = getString(R.string.start_timer)
        binding.btnStartPause.setIconResource(R.drawable.ic_play)
        binding.btnReset.visibility = View.INVISIBLE
        binding.presetsRecyclerView.isEnabled = true

        // Cancel notification
        notificationManager.cancelTimerNotification()
    }

    private fun onTimerComplete() {
        // Play sound and vibrate
        if (binding.switchSound.isChecked) {
            ringtone?.play()
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }

        // Complete the standalone session
        val actualDurationMinutes = (currentDurationMs / 1000 / 60).toInt()
        lifecycleScope.launch {
            viewModel.completeStandaloneSession(actualDurationMinutes)
        }

        // Cancel notification
        notificationManager.cancelTimerNotification()

        // Reset UI and show completion message
        sessionStarted = false
        resetTimer()
        Snackbar.make(binding.root, "Session saved! Great focus session!", Snackbar.LENGTH_LONG).show()
    }

    private fun showTimerSettings() {
        // TODO: Show settings bottom sheet with sound, vibrate, keep screen on options
        Snackbar.make(binding.root, "Settings coming soon", Snackbar.LENGTH_SHORT).show()
    }

    private fun handleBackPress() {
        if (timerState == TimerState.RUNNING) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Timer is running")
                .setMessage("Are you sure you want to go back? Your timer will be paused.")
                .setPositiveButton("Go Back") { _, _ ->
                    pauseTimer()
                    findNavController().navigateUp()
                }
                .setNegativeButton("Stay", null)
                .show()
        } else {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()

        // Clear keep screen on flag
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Keep notification if timer is still running
        if (timerState != TimerState.RUNNING) {
            notificationManager.cancelTimerNotification()
        }

        _binding = null
    }
}

