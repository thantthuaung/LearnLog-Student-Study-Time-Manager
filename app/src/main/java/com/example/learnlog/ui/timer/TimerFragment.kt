package com.example.learnlog.ui.timer

import android.content.Context
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Vibrator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.learnlog.R
import com.example.learnlog.data.model.TimerSettings
import com.example.learnlog.data.model.TimerState
import com.example.learnlog.data.model.TimerType
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TimerFragment : Fragment(R.layout.fragment_timer) {
    private lateinit var progressCircle: CircularProgressIndicator
    private lateinit var textCountdown: TextView
    private lateinit var textSessionLabel: TextView
    private lateinit var btnStartPause: MaterialButton
    private lateinit var btnReset: MaterialButton
    private lateinit var chipGroupPresets: ChipGroup
    private lateinit var btnSettings: MaterialButton
    private lateinit var textStreak: TextView
    private lateinit var textSessionsCompleted: TextView
    private lateinit var textPrompt: TextView
    private lateinit var btnPickSession: MaterialButton

    private var timerSettings = TimerSettings()
    private var timerState = TimerState.IDLE
    private var timerType = TimerType.FOCUS
    private var cyclesCompleted = 0
    private var streak = 0
    private var sessionsCompleted = 0
    private var currentDuration = timerSettings.focusDuration * 60 * 1000L
    private var remainingMillis = currentDuration
    private var countDownTimer: CountDownTimer? = null

    private var sessionLabel: String? = null
    private var linkedTaskId: Long? = null
    private var linkedSubject: String? = null

    private lateinit var notificationManager: TimerNotificationManager
    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressCircle = view.findViewById(R.id.progressCircle)
        textCountdown = view.findViewById(R.id.textCountdown)
        textSessionLabel = view.findViewById(R.id.textSessionLabel)
        btnStartPause = view.findViewById(R.id.btnStartPause)
        btnReset = view.findViewById(R.id.btnReset)
        chipGroupPresets = view.findViewById(R.id.chipGroupPresets)
        btnSettings = view.findViewById(R.id.btnSettings)
        textStreak = view.findViewById(R.id.textStreak)
        textSessionsCompleted = view.findViewById(R.id.textSessionsCompleted)
        textPrompt = view.findViewById(R.id.textPrompt)
        btnPickSession = view.findViewById(R.id.btnPickSession)

        notificationManager = TimerNotificationManager(requireContext())
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(requireContext(), notificationUri)

        setupUI()
    }

    private fun setupUI() {
        updateCountdownDisplay(remainingMillis)
        updateProgress(remainingMillis)
        updateSessionLabel()
        updateStats()
        btnStartPause.setOnClickListener { toggleTimer() }
        btnReset.setOnClickListener { resetTimer() }
        btnSettings.setOnClickListener { showSettingsDialog() }
        btnPickSession.setOnClickListener { showSessionPicker() }
        setupPresetChips()
    }

    private fun setupPresetChips() {
        chipGroupPresets.removeAllViews()
        val presets = listOf(
            Triple("25/5", 25, 5),
            Triple("45/10", 45, 10),
            Triple("60/15", 60, 15)
        )
        presets.forEachIndexed { idx, (label, focus, shortBreak) ->
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                id = View.generateViewId()
                setOnClickListener {
                    timerSettings = timerSettings.copy(
                        focusDuration = focus,
                        shortBreakDuration = shortBreak,
                        longBreakDuration = when (focus) {
                            25 -> 15
                            45 -> 20
                            60 -> 30
                            else -> 15
                        }
                    )
                    resetTimer()
                }
            }
            chipGroupPresets.addView(chip)
            if (focus == timerSettings.focusDuration && shortBreak == timerSettings.shortBreakDuration) {
                chip.isChecked = true
            }
        }
    }

    private fun toggleTimer() {
        if (timerState == TimerState.RUNNING) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        timerState = TimerState.RUNNING
        btnStartPause.text = "Pause"
        countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                updateCountdownDisplay(remainingMillis)
                updateProgress(remainingMillis)
                // Show persistent notification
                notificationManager.showTimerNotification(
                    textCountdown.text.toString(),
                    when (timerType) {
                        TimerType.FOCUS -> com.example.learnlog.data.model.SessionType.FOCUS
                        else -> com.example.learnlog.data.model.SessionType.REVISION // Use REVISION for break types
                    }
                )
            }

            override fun onFinish() {
                onSessionComplete()
            }
        }.start()
    }

    private fun pauseTimer() {
        timerState = TimerState.PAUSED
        btnStartPause.text = "Start"
        countDownTimer?.cancel()
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timerState = TimerState.IDLE
        remainingMillis = when (timerType) {
            TimerType.FOCUS -> timerSettings.focusDuration * 60 * 1000L
            TimerType.SHORT_BREAK -> timerSettings.shortBreakDuration * 60 * 1000L
            TimerType.LONG_BREAK -> timerSettings.longBreakDuration * 60 * 1000L
        }
        updateCountdownDisplay(remainingMillis)
        updateProgress(remainingMillis)
        btnStartPause.text = "Start"
    }

    private fun updateCountdownDisplay(millis: Long) {
        val minutes = (millis / 1000) / 60
        val seconds = (millis / 1000) % 60
        textCountdown.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateProgress(millis: Long) {
        val total = when (timerType) {
            TimerType.FOCUS -> timerSettings.focusDuration * 60 * 1000L
            TimerType.SHORT_BREAK -> timerSettings.shortBreakDuration * 60 * 1000L
            TimerType.LONG_BREAK -> timerSettings.longBreakDuration * 60 * 1000L
        }
        val percent = ((total - millis).toFloat() / total * 100).toInt()
        progressCircle.progress = percent
    }

    private fun updateSessionLabel() {
        // Change color for focus/break
        val (circleColor, bgColor) = when (timerType) {
            TimerType.FOCUS -> Pair(requireContext().getColor(R.color.pomodoroFocus), Color.WHITE)
            TimerType.SHORT_BREAK -> Pair(requireContext().getColor(R.color.pomodoroShortBreak), Color.parseColor("#E3FCEC"))
            TimerType.LONG_BREAK -> Pair(requireContext().getColor(R.color.pomodoroLongBreak), Color.parseColor("#E3E7FC"))
        }
        progressCircle.setIndicatorColor(circleColor)
        requireView().setBackgroundColor(bgColor)
        textSessionLabel.text = when (timerType) {
            TimerType.FOCUS -> sessionLabel?.let { "Focus: $it" } ?: "Focus Session"
            TimerType.SHORT_BREAK -> "Short Break"
            TimerType.LONG_BREAK -> "Long Break"
        }
    }

    private fun updateStats() {
        textStreak.text = "Streak: $streak days"
        textSessionsCompleted.text = "Sessions Completed: $sessionsCompleted"
    }

    private fun onSessionComplete() {
        // TODO: Handle motivational prompt, streak/cycle logic, break/long break switching, notifications, logging, etc.
        textPrompt.text = "Nice! You finished 1 focus block 🎉"
        sessionsCompleted++
        updateStats()
        // Example: switch to break
        if (timerType == TimerType.FOCUS) {
            cyclesCompleted++
            timerType = if (cyclesCompleted % timerSettings.longBreakInterval == 0) TimerType.LONG_BREAK else TimerType.SHORT_BREAK
        } else {
            timerType = TimerType.FOCUS
        }
        // Show session complete notification
        notificationManager.showSessionCompleteNotification(
            when (timerType) {
                TimerType.FOCUS -> com.example.learnlog.data.model.SessionType.FOCUS
                else -> com.example.learnlog.data.model.SessionType.REVISION // Use REVISION for break types
            }
        )
        // Vibrate and play sound if enabled
        if (timerSettings.enableVibration) {
            vibrator?.vibrate(500)
        }
        if (timerSettings.enableSoundEffects) {
            ringtone?.play()
        }
        resetTimer()
        updateSessionLabel()
    }

    private fun showSettingsDialog() {
        val dialog = TimerSettingsDialog(timerSettings)
        dialog.setOnSettingsChangedListener { newSettings ->
            timerSettings = newSettings
            resetTimer()
            setupPresetChips()
        }
        dialog.show(parentFragmentManager, "TimerSettingsDialog")
    }

    private fun showSessionPicker() {
        // TODO: Show dialog to pick a planner session or enter a custom label/subject
        // On selection, set sessionLabel, linkedTaskId, linkedSubject and call updateSessionLabel()
    }
}
