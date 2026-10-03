package com.example.learnlog.timer

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.entity.SessionLogEntity
import com.example.learnlog.data.model.TimerPresets
import com.example.learnlog.data.preferences.UserPreferences
import com.example.learnlog.data.repository.SettingsRepository
import com.example.learnlog.data.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDateTime
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * The one focus timer for the whole app. It lives outside any screen, so switching tabs,
 * rotating the phone or closing the task sheet never stops a running session.
 *
 * Elapsed focus time is measured with [SystemClock.elapsedRealtime] and excludes pauses.
 * A session is written to the database only when it ends (naturally or via "Finish"), so
 * abandoned sessions never pollute Insights or Analytics.
 */
@Singleton
class FocusTimer @Inject constructor(
    @ApplicationContext private val context: Context,
    // Providers keep the per-account database closed until a session is actually saved
    private val taskRepository: Provider<TaskRepository>,
    private val sessionLogDao: Provider<SessionLogDao>,
    private val userPreferences: UserPreferences,
    private val settingsRepository: SettingsRepository
) {
    enum class Phase { IDLE, RUNNING, PAUSED, FINISHED }

    data class LinkedTask(val id: Long, val title: String, val subject: String?)

    data class State(
        val phase: Phase = Phase.IDLE,
        val totalMs: Long = DEFAULT_MS,
        val remainingMs: Long = DEFAULT_MS,
        val focusedMs: Long = 0L,
        val task: LinkedTask? = null,
        /** Minutes saved by the session that just ended (shown on the completion card). */
        val lastSavedMinutes: Int = 0
    ) {
        val isActive: Boolean get() = phase == Phase.RUNNING || phase == Phase.PAUSED
        val progress: Float get() = if (totalMs > 0) (1f - remainingMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
        val focusedMinutes: Int get() = minutesOf(focusedMs)
    }

    sealed interface Event {
        /** The countdown reached zero and the session was saved. */
        data class Completed(val minutes: Int, val task: LinkedTask?) : Event
        /** The user finished early; [minutes] is 0 when nothing was long enough to save. */
        data class Stopped(val minutes: Int, val task: LinkedTask?) : Event
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private var accumulatedMs = 0L
    private var runningSince = 0L
    private var sessionStartedAt: LocalDateTime? = null
    private var ticker: Job? = null
    private var userChoseDuration = false

    init {
        scope.launch {
            val preset = settingsRepository.current().defaultPreset
            if (!userChoseDuration && _state.value.phase == Phase.IDLE) {
                setDurationInternal(preset.durationMinutes)
            }
        }
        scope.launch {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    scope.launch {
                        if (_state.value.phase == Phase.RUNNING && !userPreferences.keepRunningInBackground.first()) {
                            pause()
                        }
                    }
                }
            })
        }
    }

    // ───────────────────────── configuration (only while idle) ─────────────────────────

    fun setDuration(minutes: Int) {
        if (_state.value.isActive) return
        userChoseDuration = true
        setDurationInternal(minutes)
    }

    private fun setDurationInternal(minutes: Int) {
        val ms = minutes.coerceIn(TimerPresets.MIN_DURATION_MINUTES, TimerPresets.MAX_DURATION_MINUTES) * 60_000L
        _state.update { it.copy(phase = Phase.IDLE, totalMs = ms, remainingMs = ms, focusedMs = 0L, lastSavedMinutes = 0) }
    }

    fun linkTask(task: LinkedTask?) {
        if (_state.value.isActive) return
        _state.update { it.copy(task = task) }
    }

    /** Prepares a session for [task] (used by the task timer sheet). */
    fun prepare(task: LinkedTask?, minutes: Int) {
        if (_state.value.isActive) return
        linkTask(task)
        setDuration(minutes)
    }

    // ───────────────────────── controls ─────────────────────────

    fun toggle() = when (_state.value.phase) {
        Phase.RUNNING -> pause()
        else -> start()
    }

    fun start() {
        val current = _state.value
        when (current.phase) {
            Phase.RUNNING -> return
            Phase.IDLE, Phase.FINISHED -> {
                if (current.phase == Phase.FINISHED) {
                    _state.update { it.copy(remainingMs = it.totalMs, focusedMs = 0L, lastSavedMinutes = 0) }
                }
                accumulatedMs = 0L
                sessionStartedAt = LocalDateTime.now()
                current.task?.let { task ->
                    scope.launch(Dispatchers.IO) { taskRepository.get().markInProgress(task.id) }
                }
            }
            Phase.PAUSED -> Unit
        }
        runningSince = SystemClock.elapsedRealtime()
        _state.update { it.copy(phase = Phase.RUNNING) }
        startTicker()
        scope.launch {
            if (userPreferences.showOngoingNotification.first()) FocusTimerService.start(context)
        }
    }

    fun pause() {
        if (_state.value.phase != Phase.RUNNING) return
        accumulatedMs += SystemClock.elapsedRealtime() - runningSince
        ticker?.cancel()
        _state.update { it.copy(phase = Phase.PAUSED, focusedMs = accumulatedMs, remainingMs = (it.totalMs - accumulatedMs).coerceAtLeast(0)) }
    }

    /** Adds (or removes) time from the current session. Never drops below one minute left. */
    fun addTime(deltaMs: Long) {
        val s = _state.value
        if (s.phase == Phase.FINISHED) return
        val focused = focusedNow()
        val minTotal = if (s.isActive) focused + 60_000L else TimerPresets.MIN_DURATION_MINUTES * 60_000L
        val newTotal = (s.totalMs + deltaMs).coerceIn(minTotal, TimerPresets.MAX_DURATION_MINUTES * 60_000L)
        if (!s.isActive) userChoseDuration = true
        _state.update { it.copy(totalMs = newTotal, remainingMs = newTotal - focused) }
        if (s.phase == Phase.RUNNING) FocusTimerService.refresh(context)
    }

    /**
     * Ends the session early. With [save] the focused time so far is logged (when at least a
     * minute); otherwise it is discarded. Returns the minutes saved.
     */
    fun stop(save: Boolean): Int {
        val s = _state.value
        if (!s.isActive) return 0
        val focused = focusedNow()
        ticker?.cancel()
        val minutes = if (save && focused >= 60_000L) minutesOf(focused) else 0
        if (minutes > 0) persist(minutes, s.task)
        resetInternal(keepTask = false)
        _events.tryEmit(Event.Stopped(minutes, s.task))
        return minutes
    }

    /** Leaves the "session complete" state and gets ready for the next one. */
    fun acknowledgeFinished() {
        if (_state.value.phase == Phase.FINISHED) resetInternal(keepTask = false)
    }

    private fun resetInternal(keepTask: Boolean) {
        accumulatedMs = 0L
        sessionStartedAt = null
        _state.update {
            it.copy(
                phase = Phase.IDLE,
                remainingMs = it.totalMs,
                focusedMs = 0L,
                task = if (keepTask) it.task else null
            )
        }
    }

    // ───────────────────────── internals ─────────────────────────

    private fun focusedNow(): Long {
        val s = _state.value
        return if (s.phase == Phase.RUNNING) accumulatedMs + (SystemClock.elapsedRealtime() - runningSince)
        else accumulatedMs
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val focused = focusedNow()
                val remaining = _state.value.totalMs - focused
                if (remaining <= 0L) {
                    complete()
                    break
                }
                _state.update { it.copy(focusedMs = focused, remainingMs = remaining) }
                delay(TICK_MS)
            }
        }
    }

    private fun complete() {
        val s = _state.value
        val minutes = minutesOf(s.totalMs)
        accumulatedMs = s.totalMs
        persist(minutes, s.task)
        _state.update { it.copy(phase = Phase.FINISHED, remainingMs = 0L, focusedMs = it.totalMs, lastSavedMinutes = minutes) }
        _events.tryEmit(Event.Completed(minutes, s.task))
        scope.launch { TimerAlerts.sessionComplete(context, userPreferences, minutes, s.task?.title) }
    }

    private fun persist(minutes: Int, task: LinkedTask?) {
        val start = sessionStartedAt ?: LocalDateTime.now().minusMinutes(minutes.toLong())
        scope.launch {
            withContext(Dispatchers.IO) {
                sessionLogDao.get().insertSession(
                    SessionLogEntity(
                        taskId = task?.id,
                        subject = task?.subject,
                        startTime = start,
                        endTime = LocalDateTime.now(),
                        durationMinutes = minutes,
                        type = "FOCUS",
                        isCompleted = true
                    )
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_MS = 25 * 60_000L
        private const val TICK_MS = 200L

        fun minutesOf(ms: Long): Int = (ms / 60_000.0).roundToInt()
    }
}
