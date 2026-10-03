package com.example.learnlog.ui.timer

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.learnlog.R
import com.example.learnlog.data.model.TimerPresets
import com.example.learnlog.databinding.FragmentTimerBinding
import com.example.learnlog.timer.FocusTimer
import com.example.learnlog.timer.FocusTimerService
import com.example.learnlog.timer.TimerAlerts
import com.example.learnlog.ui.base.BaseFragment
import com.example.learnlog.ui.common.Dialogs
import com.example.learnlog.ui.common.Format
import com.example.learnlog.ui.common.Motion
import com.example.learnlog.ui.common.NotificationPermissionGate
import com.example.learnlog.ui.common.Reselectable
import com.example.learnlog.ui.timer.TimerViewModel.Companion.toLinked
import com.google.android.material.chip.Chip
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TimerFragment : BaseFragment(), Reselectable {

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TimerViewModel by viewModels()

    private val permissionGate = NotificationPermissionGate(
        this,
        shouldAsk = { viewModel.shouldAskNotificationPermission() },
        markAsked = { viewModel.notificationPermissionAsked() }
    )

    private var glowAnimator: ObjectAnimator? = null
    private var lastPhase: FocusTimer.Phase? = null
    private var keepScreenOnPref = false
    private var renderedPresets: List<Int> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopBar(binding.topBar.root, getString(R.string.page_timer_title), getString(R.string.timer_ready))
        setupControls()
        observe()
    }

    private fun setupControls() = with(binding) {
        btnStartPause.setOnClickListener {
            val s = viewModel.state.value
            if (s.phase == FocusTimer.Phase.RUNNING) viewModel.timer.pause()
            else permissionGate.run {
                viewModel.timer.start()
                TimerAlerts.tick(requireContext())
            }
        }
        btnReset.setOnClickListener { confirmReset() }
        btnFinish.setOnClickListener { confirmFinish() }
        btnIncrease.setOnClickListener { viewModel.timer.addTime(5 * 60_000L) }
        btnDecrease.setOnClickListener { viewModel.timer.addTime(-60_000L) }
        taskCard.setOnClickListener { if (!viewModel.state.value.isActive) chooseTask() }
        taskAction.setOnClickListener {
            if (viewModel.state.value.task != null) viewModel.link(null) else chooseTask()
        }
        switchKeepScreenOn.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) viewModel.setKeepScreenOn(checked)
        }
        presetChips.setOnCheckedStateChangeListener { group, ids ->
            val chip = ids.firstOrNull()?.let { group.findViewById<Chip>(it) } ?: return@setOnCheckedStateChangeListener
            (chip.tag as? Int)?.let { viewModel.timer.setDuration(it) }
        }
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.settings.collect { renderPresets(it.timerPresets.map { p -> p.durationMinutes }) } }
                launch {
                    viewModel.dailyGoal.collect { goal ->
                        binding.goalText.text = getString(R.string.timer_goal_progress, Format.compact(goal.focusedMinutes), Format.compact(goal.goalMinutes))
                        binding.goalProgress.setProgressCompat((goal.fraction * 1000).toInt(), true)
                        binding.goalReached.isVisible = goal.reached
                    }
                }
                launch {
                    viewModel.keepScreenOn.collect {
                        keepScreenOnPref = it
                        binding.switchKeepScreenOn.isChecked = it
                        applyKeepScreenOn(viewModel.state.value)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is FocusTimer.Event.Completed -> Motion.pop(binding.completeIcon)
                            is FocusTimer.Event.Stopped -> snack(
                                if (event.minutes > 0) getString(R.string.timer_saved, Format.minutes(requireContext(), event.minutes))
                                else getString(R.string.timer_discarded)
                            )
                        }
                    }
                }
            }
        }
    }

    private fun render(s: FocusTimer.State) = with(binding) {
        val phase = s.phase
        val phaseChanged = phase != lastPhase
        val ctx = requireContext()

        textCountdown.text = FocusTimerService.formatClock(if (phase == FocusTimer.Phase.IDLE) s.totalMs else s.remainingMs)
        progressRing.setProgressCompat((s.progress * 1000).toInt(), phase != FocusTimer.Phase.RUNNING)

        textFocused.text = when (phase) {
            FocusTimer.Phase.IDLE -> Format.minutes(ctx, (s.totalMs / 60_000).toInt())
            FocusTimer.Phase.FINISHED -> "+" + Format.minutes(ctx, s.lastSavedMinutes)
            else -> Format.minutes(ctx, s.focusedMinutes) + " · " + Format.minutes(ctx, (s.totalMs / 60_000).toInt())
        }

        if (phaseChanged) {
            val (labelRes, bgAttr, fgAttr) = when (phase) {
                FocusTimer.Phase.IDLE -> Triple(R.string.timer_ready, com.google.android.material.R.attr.colorSurfaceContainerHigh, com.google.android.material.R.attr.colorOnSurfaceVariant)
                FocusTimer.Phase.RUNNING -> Triple(R.string.timer_focusing, com.google.android.material.R.attr.colorPrimaryContainer, com.google.android.material.R.attr.colorOnPrimaryContainer)
                FocusTimer.Phase.PAUSED -> Triple(R.string.timer_paused, com.google.android.material.R.attr.colorSecondaryContainer, com.google.android.material.R.attr.colorOnSecondaryContainer)
                FocusTimer.Phase.FINISHED -> Triple(R.string.timer_complete, com.google.android.material.R.attr.colorTertiaryContainer, com.google.android.material.R.attr.colorOnTertiaryContainer)
            }
            stateLabel.setText(labelRes)
            stateLabel.backgroundTintList = ColorStateList.valueOf(MaterialColors.getColor(stateLabel, bgAttr))
            stateLabel.setTextColor(MaterialColors.getColor(stateLabel, fgAttr))
            setTopBarSubtitle(topBar.root, getString(labelRes))
            progressRing.setIndicatorColor(
                MaterialColors.getColor(progressRing, if (phase == FocusTimer.Phase.PAUSED) com.google.android.material.R.attr.colorSecondary
                else if (phase == FocusTimer.Phase.FINISHED) com.google.android.material.R.attr.colorTertiary
                else com.google.android.material.R.attr.colorPrimary)
            )

            btnStartPause.setText(
                when (phase) {
                    FocusTimer.Phase.RUNNING -> R.string.pause
                    FocusTimer.Phase.PAUSED -> R.string.resume
                    else -> R.string.timer_start
                }
            )
            btnStartPause.setIconResource(if (phase == FocusTimer.Phase.RUNNING) R.drawable.ic_pause else R.drawable.ic_play)
            if (lastPhase != null) Motion.pop(btnStartPause)

            fadeVisible(btnReset, s.isActive, animate = lastPhase != null)
            fadeVisible(btnFinish, s.isActive, animate = lastPhase != null)
            controls.isVisible = phase != FocusTimer.Phase.FINISHED
            adjustRow.isVisible = phase != FocusTimer.Phase.FINISHED
            presetsLabel.isVisible = phase == FocusTimer.Phase.IDLE
            presetsScroll.isVisible = phase == FocusTimer.Phase.IDLE
            renderCompletion(s)
            setGlow(phase == FocusTimer.Phase.RUNNING)
            applyKeepScreenOn(s)
            lastPhase = phase
        }
        renderTask(s)
        if (phase == FocusTimer.Phase.IDLE) syncPresetSelection(s)
    }

    private fun renderTask(s: FocusTimer.State) = with(binding) {
        val task = s.task
        taskCard.isVisible = task != null || !s.isActive
        if (task != null) {
            taskLabel.setText(R.string.timer_linked_task)
            taskTitle.text = listOfNotNull(task.title, task.subject).joinToString(" · ")
            taskTitle.setTextColor(MaterialColors.getColor(taskTitle, com.google.android.material.R.attr.colorOnSurface))
            taskAction.setText(R.string.timer_unlink)
        } else {
            taskLabel.setText(R.string.timer_choose_task)
            taskTitle.setText(R.string.timer_link_task)
            taskTitle.setTextColor(MaterialColors.getColor(taskTitle, com.google.android.material.R.attr.colorPrimary))
            taskAction.setText(R.string.action_change)
        }
        taskAction.isVisible = !s.isActive && s.phase != FocusTimer.Phase.FINISHED && task != null
    }

    private fun renderCompletion(s: FocusTimer.State) = with(binding) {
        val finished = s.phase == FocusTimer.Phase.FINISHED
        if (finished && !completeCard.isVisible) Motion.staggerIn(listOf(completeCard))
        completeCard.isVisible = finished
        if (!finished) return@with
        val minutes = Format.minutes(requireContext(), s.lastSavedMinutes)
        val task = s.task
        if (task != null) {
            completeMessage.text = getString(R.string.timer_complete_message, minutes) + "\n" + getString(R.string.timer_complete_task, task.title)
            completePrimary.setText(R.string.action_mark_done)
            completeSecondary.setText(R.string.timer_keep_going)
            completePrimary.setOnClickListener {
                viewModel.markTaskDone(task.id)
                TimerAlerts.tick(requireContext())
                viewModel.timer.acknowledgeFinished()
                snack(getString(R.string.task_completed_toast))
            }
            completeSecondary.setOnClickListener { viewModel.timer.acknowledgeFinished() }
        } else {
            completeMessage.text = getString(R.string.timer_complete_message, minutes)
            completePrimary.setText(R.string.timer_new_session)
            completeSecondary.setText(R.string.action_done)
            completePrimary.setOnClickListener { viewModel.timer.acknowledgeFinished(); viewModel.timer.start() }
            completeSecondary.setOnClickListener { viewModel.timer.acknowledgeFinished() }
        }
    }

    private fun renderPresets(minutes: List<Int>) {
        if (minutes == renderedPresets) return
        renderedPresets = minutes
        val group = binding.presetChips
        group.removeAllViews()
        minutes.forEach { m ->
            group.addView(newChip(Format.compact(m)).apply { tag = m; id = View.generateViewId() })
        }
        group.addView(newChip(getString(R.string.custom)).apply {
            id = View.generateViewId()
            isCheckable = false
            setChipIconResource(R.drawable.ic_tune)
            isChipIconVisible = true
            chipIconSize = resources.displayMetrics.density * 18
            setOnClickListener { askCustomDuration() }
        })
        syncPresetSelection(viewModel.state.value)
    }

    private fun syncPresetSelection(s: FocusTimer.State) {
        val minutes = (s.totalMs / 60_000).toInt()
        val group = binding.presetChips
        for (i in 0 until group.childCount) {
            val chip = group.getChildAt(i) as Chip
            val shouldCheck = chip.tag == minutes
            if (chip.isCheckable && chip.isChecked != shouldCheck) chip.isChecked = shouldCheck
        }
    }

    private fun askCustomDuration() {
        Dialogs.askMinutes(
            requireContext(), getString(R.string.timer_custom_duration),
            initial = (viewModel.state.value.totalMs / 60_000).toInt(),
            min = TimerPresets.MIN_DURATION_MINUTES, max = TimerPresets.MAX_DURATION_MINUTES
        ) { viewModel.timer.setDuration(it) }
    }

    private fun chooseTask() {
        val tasks = viewModel.openTasks.value
        if (tasks.isEmpty()) {
            snack(getString(R.string.timer_no_open_tasks)); return
        }
        val labels = tasks.map { t -> listOfNotNull(t.title, t.subject).joinToString("  ·  ") }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.timer_choose_task)
            .setItems(labels) { _, which ->
                val task = tasks[which]
                viewModel.link(task)
                if (viewModel.state.value.phase == FocusTimer.Phase.IDLE) viewModel.timer.setDuration(task.durationMinutes.coerceIn(1, TimerPresets.MAX_DURATION_MINUTES))
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun confirmFinish() {
        val s = viewModel.state.value
        viewLifecycleOwner.lifecycleScope.launch {
            if (!viewModel.confirmOnStop() && s.focusedMs >= 60_000) {
                viewModel.timer.stop(save = true); return@launch
            }
            val focused = Format.minutes(requireContext(), s.focusedMinutes.coerceAtLeast(1))
            val builder = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.timer_finish_title)
            if (s.focusedMs < 60_000) {
                builder.setMessage(R.string.timer_finish_too_short)
                    .setNegativeButton(R.string.timer_keep_going, null)
                    .setPositiveButton(R.string.timer_discard) { _, _ -> viewModel.timer.stop(save = false) }
            } else {
                builder.setMessage(getString(R.string.timer_finish_message, focused))
                    .setNeutralButton(R.string.action_cancel, null)
                    .setNegativeButton(R.string.timer_discard) { _, _ -> viewModel.timer.stop(save = false) }
                    .setPositiveButton(R.string.timer_save) { _, _ -> viewModel.timer.stop(save = true) }
            }
            builder.show()
        }
    }

    private fun confirmReset() {
        val s = viewModel.state.value
        if (s.focusedMs < 60_000) {
            viewModel.timer.stop(save = false); return
        }
        MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_LearnLog_AlertDialog_Destructive)
            .setTitle(R.string.timer_reset)
            .setMessage(getString(R.string.timer_finish_message, Format.minutes(requireContext(), s.focusedMinutes)))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.timer_discard) { _, _ -> viewModel.timer.stop(save = false) }
            .show()
    }

    private fun setGlow(on: Boolean) {
        val glow = binding.ringGlow
        if (on) {
            if (glowAnimator?.isRunning == true) return
            glow.alpha = 0f
            glowAnimator = ObjectAnimator.ofPropertyValuesHolder(
                glow,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.15f, 0.55f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.94f, 1.04f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.94f, 1.04f)
            ).apply {
                duration = 1800
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                start()
            }
        } else {
            glowAnimator?.cancel()
            glowAnimator = null
            glow.animate().alpha(0f).scaleX(1f).scaleY(1f).setDuration(300).start()
        }
    }

    private fun applyKeepScreenOn(s: FocusTimer.State) {
        val window = activity?.window ?: return
        if (keepScreenOnPref && s.isActive) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun fadeVisible(view: View, visible: Boolean, animate: Boolean) {
        view.animate().cancel()
        if (!animate) {
            view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
            view.alpha = 1f; view.scaleX = 1f; view.scaleY = 1f
            return
        }
        if (visible) {
            view.isVisible = true
            view.alpha = 0f; view.scaleX = 0.6f; view.scaleY = 0.6f
            view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(Motion.EMPHASIZED).start()
        } else {
            view.animate().alpha(0f).scaleX(0.6f).scaleY(0.6f).setDuration(160)
                .withEndAction { view.visibility = View.INVISIBLE }.start()
        }
    }

    private fun newChip(text: CharSequence) =
        (layoutInflater.inflate(R.layout.view_choice_chip, binding.presetChips, false) as Chip).apply { this.text = text }

    private fun snack(message: String) {
        val root = view ?: return
        Snackbar.make(root, message, Snackbar.LENGTH_SHORT).apply {
            activity?.findViewById<View>(R.id.bottom_nav_container)?.takeIf { it.isVisible }?.let { anchorView = it }
        }.show()
    }

    override fun onReselected() {
        _binding?.scroll?.smoothScrollTo(0, 0)
    }

    override fun onDestroyView() {
        glowAnimator?.cancel()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroyView()
        _binding = null
        lastPhase = null
        renderedPresets = emptyList()
    }
}
