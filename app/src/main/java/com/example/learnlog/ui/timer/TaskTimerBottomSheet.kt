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
import androidx.fragment.app.viewModels
import com.example.learnlog.R
import com.example.learnlog.databinding.BottomSheetTaskTimerBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class TaskTimerBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetTaskTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels()

    private var taskId: Long = -1L
    private var taskTitle: String = ""
    private var taskSubject: String? = null
    private var initialDurationMinutes: Int = 25

    private var currentDurationMs = 25 * 60 * 1000L
    private var remainingMillis = currentDurationMs
    private var countDownTimer: CountDownTimer? = null
    private var actualStartTimeMillis: Long = 0 // Track actual wall-clock start time
    private var sessionStarted = false
    private var isRunning = false

    private var vibrator: Vibrator? = null
    private var ringtone: Ringtone? = null

    companion object {
        private const val ARG_TASK_ID = "taskId"
        private const val ARG_TASK_TITLE = "taskTitle"
        private const val ARG_TASK_SUBJECT = "taskSubject"
        private const val ARG_DURATION_MINUTES = "durationMinutes"

        fun newInstance(
            taskId: Long,
            taskTitle: String,
            taskSubject: String?,
            durationMinutes: Int = 25
        ): TaskTimerBottomSheet {
            return TaskTimerBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_TASK_ID, taskId)
                    putString(ARG_TASK_TITLE, taskTitle)
                    putString(ARG_TASK_SUBJECT, taskSubject)
                    putInt(ARG_DURATION_MINUTES, durationMinutes)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            taskId = it.getLong(ARG_TASK_ID, -1L)
            taskTitle = it.getString(ARG_TASK_TITLE, "Focus Session")
            taskSubject = it.getString(ARG_TASK_SUBJECT)
            initialDurationMinutes = it.getInt(ARG_DURATION_MINUTES, 25)
        }

        currentDurationMs = initialDurationMinutes * 60 * 1000L
        remainingMillis = currentDurationMs
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetTaskTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(requireContext(), notificationUri)

        setupUI()
        setupControls()
        updateDisplay()
    }

    private fun setupUI() {
        binding.textTaskTitle.text = taskTitle

        if (taskSubject.isNullOrBlank()) {
            binding.chipSubject.visibility = View.GONE
        } else {
            binding.chipSubject.visibility = View.VISIBLE
            binding.chipSubject.text = taskSubject
        }
    }

    private fun setupControls() {
        // Start/Pause button
        binding.btnStartPause.setOnClickListener {
            if (isRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        // Stop button
        binding.btnStop.setOnClickListener {
            stopTimer()
        }

        // Time adjustment
        binding.btnIncrease.setOnClickListener {
            if (!isRunning) {
                adjustTime(5 * 60 * 1000L) // +5 minutes
            }
        }

        binding.btnDecrease.setOnClickListener {
            if (!isRunning) {
                adjustTime(-1 * 60 * 1000L) // -1 minute
            }
        }

        // Keep screen on toggle
        binding.switchKeepScreenOn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    private fun startTimer() {
        // Start session on first start
        if (!sessionStarted) {
            val durationMinutes = (currentDurationMs / 1000 / 60).toInt()
            viewModel.startTimerSession(taskId, taskTitle, durationMinutes)
            sessionStarted = true
        }
            actualStartTimeMillis = System.currentTimeMillis() // Record actual start time
        isRunning = true
        binding.btnStartPause.text = "Pause"
        binding.btnStartPause.setIconResource(R.drawable.ic_pause)
        binding.btnStop.visibility = View.VISIBLE
        binding.btnIncrease.isEnabled = false
        binding.btnDecrease.isEnabled = false

        countDownTimer = object : CountDownTimer(remainingMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                updateDisplay()
            }

            override fun onFinish() {
                onTimerComplete()
            }
        }.start()
    }

    private fun pauseTimer() {
        isRunning = false
        binding.btnStartPause.text = "Resume"
        binding.btnStartPause.setIconResource(R.drawable.ic_play)
        countDownTimer?.cancel()
    }

    private fun stopTimer() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Stop Timer?")
            .setMessage("Are you sure you want to stop this timer session?")
            .setPositiveButton("Stop") { _, _ ->
                completeSession(false)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun adjustTime(deltaMs: Long) {
        val newDuration = (currentDurationMs + deltaMs).coerceIn(60 * 1000L, 180 * 60 * 1000L)
        currentDurationMs = newDuration
        remainingMillis = newDuration
        updateDisplay()
    }

    private fun updateDisplay() {
        val minutes = remainingMillis / 1000 / 60
        val seconds = remainingMillis / 1000 % 60
        binding.textCountdown.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)

        // Update progress
        val progress = if (currentDurationMs > 0) {
            ((currentDurationMs - remainingMillis).toFloat() / currentDurationMs * 100).toInt()
        } else 0
        binding.progressCircle.progress = progress
    }

    private fun onTimerComplete() {
        isRunning = false
        remainingMillis = 0
        updateDisplay()

        // Play sound and vibrate
        if (binding.switchSound.isChecked) {
            ringtone?.play()
            @Suppress("DEPRECATION")
            vibrator?.vibrate(500)
        }

        // Show completion dialog
        showCompletionDialog()
    }

    private fun showCompletionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Great Work!")
            .setMessage("You've completed your focus session. Would you like to mark this task as completed?")
            .setPositiveButton("Mark Completed") { _, _ ->
                completeSession(true)
            }
            .setNegativeButton("Keep In Progress") { _, _ ->
                completeSession(false)
            }
            .setCancelable(false)
            .show()
    }

    private fun completeSession(markTaskCompleted: Boolean) {
        // Calculate actual elapsed time from wall-clock, not countdown timer
        val actualElapsedMs = if (actualStartTimeMillis > 0) {
            System.currentTimeMillis() - actualStartTimeMillis
        } else {
            // Fallback to countdown calculation if start time wasn't set
            currentDurationMs - remainingMillis
        }
        val actualDurationMinutes = (actualElapsedMs / 1000 / 60).toInt().coerceAtLeast(1)

        viewModel.completeTimerSession(actualDurationMinutes)

        if (markTaskCompleted) {
            viewModel.markTaskCompleted(taskId)
        }

        val message = if (markTaskCompleted) {
            "Task completed! Session saved."
        } else {
            "Session saved"
        }

        activity?.findViewById<View>(android.R.id.content)?.let { rootView ->
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
        }

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()

        // Clean up screen on flag
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        _binding = null
    }
}

