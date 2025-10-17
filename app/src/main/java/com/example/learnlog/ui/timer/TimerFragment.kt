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
import com.example.learnlog.databinding.FragmentTimerBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.fragment.navArgs

@AndroidEntryPoint
class TimerFragment : Fragment(R.layout.fragment_timer) {
    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val args: TimerFragmentArgs by navArgs()

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

        binding.chipPage.text = "Timer"

        // Set task from navigation arguments
        if (args.taskId != -1L) {
            binding.textPrompt.text = args.taskTitle
        }

        notificationManager = TimerNotificationManager(requireContext())
        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(requireContext(), notificationUri)

        setupUI()
    }

    private fun setupUI() {
        updateTimerDisplay()
        updateStreakAndSessionCount()

        binding.btnStartPause.setOnClickListener {
            when (timerState) {
                TimerState.IDLE, TimerState.PAUSED -> startTimer()
                TimerState.RUNNING -> pauseTimer()
                TimerState.BREAK -> startTimer() // Handle break state same as IDLE/PAUSED
            }
        }

        binding.btnReset.setOnClickListener {
            resetTimer()
        }

        binding.chipGroupPresets.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.chipPomodoro -> setTimerType(TimerType.FOCUS)
                R.id.chipShortBreak -> setTimerType(TimerType.SHORT_BREAK)
                R.id.chipLongBreak -> setTimerType(TimerType.LONG_BREAK)
            }
        }

        binding.btnSettings.setOnClickListener {
            // TODO: Show settings dialog
        }
    }

    private fun updateTimerDisplay() {
        val minutes = remainingMillis / 1000 / 60
        val seconds = remainingMillis / 1000 % 60
        binding.textCountdown.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateStreakAndSessionCount() {
        binding.textStreak.text = streak.toString()
        binding.textSessionsCompleted.text = sessionsCompleted.toString()
    }

    private fun startTimer() {
        timerState = TimerState.RUNNING
        binding.btnStartPause.setIconResource(R.drawable.ic_pause) // Assuming you have a pause icon
        binding.btnReset.visibility = View.VISIBLE

        countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                updateTimerDisplay()
            }

            override fun onFinish() {
                timerState = TimerState.IDLE
                remainingMillis = 0
                updateTimerDisplay()
                // TODO: Handle timer completion (sound, vibration, next state)
            }
        }.start()
    }

    private fun pauseTimer() {
        timerState = TimerState.PAUSED
        binding.btnStartPause.setIconResource(R.drawable.ic_play)
        countDownTimer?.cancel()
    }

    private fun resetTimer() {
        timerState = TimerState.IDLE
        countDownTimer?.cancel()
        setTimerType(timerType) // Resets to the current type's duration
        binding.btnStartPause.setIconResource(R.drawable.ic_play)
        binding.btnReset.visibility = View.INVISIBLE
    }

    private fun setTimerType(newType: TimerType) {
        timerType = newType
        currentDuration = when (newType) {
            TimerType.FOCUS -> timerSettings.focusDuration * 60 * 1000L
            TimerType.SHORT_BREAK -> timerSettings.shortBreakDuration * 60 * 1000L
            TimerType.LONG_BREAK -> timerSettings.longBreakDuration * 60 * 1000L
        }
        remainingMillis = currentDuration
        updateTimerDisplay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}
