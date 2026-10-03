package com.example.learnlog.ui.timer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.learnlog.MainActivity
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.model.TimerPresets
import com.example.learnlog.databinding.BottomSheetTaskTimerBinding
import com.example.learnlog.timer.FocusTimer
import com.example.learnlog.timer.FocusTimerService
import com.example.learnlog.timer.TimerAlerts
import com.example.learnlog.ui.common.Format
import com.example.learnlog.ui.common.Motion
import com.example.learnlog.ui.common.NotificationPermissionGate
import com.example.learnlog.ui.timer.TimerViewModel.Companion.toLinked
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Quick focus session for one task. It is only a view onto the app-wide FocusTimer, so
 * closing the sheet never stops the session – it keeps running on the Focus tab.
 */
@AndroidEntryPoint
class TaskTimerBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTaskTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels()
    private lateinit var task: TaskEntity
    private var chosenMinutes = 25

    private val permissionGate = NotificationPermissionGate(
        this,
        shouldAsk = { viewModel.shouldAskNotificationPermission() },
        markAsked = { viewModel.notificationPermissionAsked() }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        task = requireArguments().getParcelable(ARG_TASK)!!
        chosenMinutes = savedInstanceState?.getInt(STATE_MINUTES)
            ?: task.durationMinutes.coerceIn(TimerPresets.MIN_DURATION_MINUTES, TimerPresets.MAX_DURATION_MINUTES)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetTaskTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textTaskTitle.text = task.title
        binding.textSubject.isVisible = !task.subject.isNullOrBlank()
        binding.textSubject.text = task.subject

        setupDurations()
        binding.btnStartPause.setOnClickListener { onPrimary() }
        binding.btnFinish.setOnClickListener { viewModel.timer.stop(save = true) }
        binding.btnOpenTimer.setOnClickListener { openTimerTab() }
        binding.btnSwitch.setOnClickListener {
            viewModel.timer.stop(save = true)
            viewModel.timer.acknowledgeFinished()
            startForThisTask()
        }
        binding.btnMarkDone.setOnClickListener {
            viewModel.markTaskDone(task.id)
            TimerAlerts.tick(requireContext())
            viewModel.timer.acknowledgeFinished()
            dismiss()
        }
        binding.btnKeepGoing.setOnClickListener {
            viewModel.timer.acknowledgeFinished()
            dismiss()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::render)
            }
        }
    }

    private fun isMine(s: FocusTimer.State) = s.task?.id == task.id

    private fun onPrimary() {
        val s = viewModel.state.value
        when {
            isMine(s) && s.phase == FocusTimer.Phase.RUNNING -> viewModel.timer.pause()
            isMine(s) && s.phase == FocusTimer.Phase.PAUSED -> viewModel.timer.start()
            else -> startForThisTask()
        }
    }

    private fun startForThisTask() {
        viewModel.timer.prepare(task.toLinked(), chosenMinutes)
        permissionGate.run {
            viewModel.timer.start()
            TimerAlerts.tick(requireContext())
        }
    }

    private fun setupDurations() {
        val options = (listOf(15, 25, 45, 60) + chosenMinutes).distinct().sorted()
        options.forEach { minutes ->
            val chip = (layoutInflater.inflate(R.layout.view_choice_chip, binding.durationChips, false) as Chip).apply {
                id = View.generateViewId()
                text = Format.compact(minutes)
                tag = minutes
                isChecked = minutes == chosenMinutes
            }
            binding.durationChips.addView(chip)
        }
        binding.durationChips.setOnCheckedStateChangeListener { group, ids ->
            (ids.firstOrNull()?.let { group.findViewById<Chip>(it) }?.tag as? Int)?.let {
                chosenMinutes = it
                render(viewModel.state.value)
            }
        }
    }

    private fun render(s: FocusTimer.State) {
        val b = _binding ?: return
        val mine = isMine(s)
        val busyElsewhere = s.isActive && !mine
        b.busyCard.isVisible = busyElsewhere
        if (busyElsewhere) {
            b.busyText.text = s.task?.let { getString(R.string.timer_running_elsewhere, it.title) }
                ?: getString(R.string.timer_running_free)
        }

        val phase = if (mine) s.phase else FocusTimer.Phase.IDLE
        val totalMs = if (mine) s.totalMs else chosenMinutes * 60_000L
        val remaining = if (mine && phase != FocusTimer.Phase.IDLE) s.remainingMs else totalMs
        b.textCountdown.text = FocusTimerService.formatClock(remaining)
        b.progressRing.setProgressCompat(if (mine) (s.progress * 1000).toInt() else 0, phase != FocusTimer.Phase.RUNNING)
        b.progressRing.setIndicatorColor(MaterialColors.getColor(b.progressRing,
            when (phase) {
                FocusTimer.Phase.PAUSED -> com.google.android.material.R.attr.colorSecondary
                FocusTimer.Phase.FINISHED -> com.google.android.material.R.attr.colorTertiary
                else -> com.google.android.material.R.attr.colorPrimary
            }))
        b.textState.setText(
            when (phase) {
                FocusTimer.Phase.RUNNING -> R.string.timer_focusing
                FocusTimer.Phase.PAUSED -> R.string.timer_paused
                FocusTimer.Phase.FINISHED -> R.string.timer_complete
                FocusTimer.Phase.IDLE -> R.string.timer_ready
            }
        )

        b.durationsScroll.isVisible = phase == FocusTimer.Phase.IDLE && !busyElsewhere
        b.controls.isVisible = phase != FocusTimer.Phase.FINISHED && !busyElsewhere
        b.btnFinish.isVisible = mine && s.isActive
        b.btnStartPause.setText(
            when (phase) {
                FocusTimer.Phase.RUNNING -> R.string.pause
                FocusTimer.Phase.PAUSED -> R.string.resume
                else -> R.string.action_start_focus
            }
        )
        b.btnStartPause.setIconResource(if (phase == FocusTimer.Phase.RUNNING) R.drawable.ic_pause else R.drawable.ic_play)

        val finished = mine && s.phase == FocusTimer.Phase.FINISHED
        if (finished && !b.completePanel.isVisible) Motion.staggerIn(listOf(b.completePanel))
        b.completePanel.isVisible = finished
        if (finished) {
            b.completeMessage.text = getString(R.string.timer_complete_message, Format.minutes(requireContext(), s.lastSavedMinutes)) +
                "\n" + getString(R.string.timer_complete_task, task.title)
        }
    }

    private fun openTimerTab() {
        dismiss()
        (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
            ?.selectedItemId = R.id.timerFragment
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_MINUTES, chosenMinutes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TaskTimerSheet"
        private const val ARG_TASK = "task"
        private const val STATE_MINUTES = "minutes"

        fun newInstance(task: TaskEntity) = TaskTimerBottomSheet().apply {
            arguments = bundleOf(ARG_TASK to task)
        }
    }
}
