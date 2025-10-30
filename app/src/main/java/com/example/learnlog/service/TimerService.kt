package com.example.learnlog.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.learnlog.MainActivity
import com.example.learnlog.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TimerService : Service() {

    private val binder = TimerBinder()
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private var totalDurationMillis = 0L
    private var taskId: Long? = null
    private var taskTitle: String? = null

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val durationMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0)
                taskId = intent.getLongExtra(EXTRA_TASK_ID, -1).takeIf { it != -1L }
                taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE)
                startTimer(durationMillis)
            }
            ACTION_PAUSE_TIMER -> pauseTimer()
            ACTION_RESUME_TIMER -> resumeTimer()
            ACTION_STOP_TIMER -> stopTimer()
        }
        return START_STICKY
    }

    private fun startTimer(durationMillis: Long) {
        totalDurationMillis = durationMillis
        _remainingMillis.value = durationMillis
        _isRunning.value = true
        _isPaused.value = false

        startForeground(NOTIFICATION_ID, createNotification())

        job?.cancel()
        job = scope.launch {
            while (_remainingMillis.value > 0 && _isRunning.value && !_isPaused.value) {
                delay(1000)
                _remainingMillis.value = (_remainingMillis.value - 1000).coerceAtLeast(0)
                updateNotification()
            }

            if (_remainingMillis.value == 0L) {
                // Timer completed
                onTimerComplete()
            }
        }
    }

    private fun pauseTimer() {
        _isPaused.value = true
        updateNotification()
    }

    private fun resumeTimer() {
        if (!_isRunning.value || !_isPaused.value) return

        _isPaused.value = false

        job?.cancel()
        job = scope.launch {
            while (_remainingMillis.value > 0 && _isRunning.value && !_isPaused.value) {
                delay(1000)
                _remainingMillis.value = (_remainingMillis.value - 1000).coerceAtLeast(0)
                updateNotification()
            }

            if (_remainingMillis.value == 0L) {
                onTimerComplete()
            }
        }
        updateNotification()
    }

    private fun stopTimer() {
        job?.cancel()
        _isRunning.value = false
        _isPaused.value = false
        _remainingMillis.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerComplete() {
        _isRunning.value = false
        _isPaused.value = false

        // Broadcast completion
        val intent = Intent(ACTION_TIMER_COMPLETE).apply {
            putExtra(EXTRA_DURATION_MILLIS, totalDurationMillis)
            taskId?.let { putExtra(EXTRA_TASK_ID, it) }
            taskTitle?.let { putExtra(EXTRA_TASK_TITLE, it) }
        }
        sendBroadcast(intent)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Focus timer notifications"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val minutes = _remainingMillis.value / 1000 / 60
        val seconds = _remainingMillis.value / 1000 % 60
        val timeText = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds)

        // Open app intent
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_timer_tab", true)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause/Resume intent
        val pauseResumeAction = if (_isPaused.value) {
            val resumeIntent = Intent(this, TimerService::class.java).apply {
                action = ACTION_RESUME_TIMER
            }
            val resumePendingIntent = PendingIntent.getService(
                this, 1, resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action(
                R.drawable.ic_play,
                "Resume",
                resumePendingIntent
            )
        } else {
            val pauseIntent = Intent(this, TimerService::class.java).apply {
                action = ACTION_PAUSE_TIMER
            }
            val pausePendingIntent = PendingIntent.getService(
                this, 1, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "Pause",
                pausePendingIntent
            )
        }

        // Stop intent
        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP_TIMER
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action(
            R.drawable.ic_stop,
            "Stop",
            stopPendingIntent
        )

        val title = if (taskTitle != null) "Timer: $taskTitle" else "Focus Timer"
        val statusText = if (_isPaused.value) "Paused" else "Running"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("$statusText - $timeText remaining")
            .setSmallIcon(R.drawable.ic_timer)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPendingIntent)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        scope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_channel"

        const val ACTION_START_TIMER = "com.example.learnlog.START_TIMER"
        const val ACTION_PAUSE_TIMER = "com.example.learnlog.PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.example.learnlog.RESUME_TIMER"
        const val ACTION_STOP_TIMER = "com.example.learnlog.STOP_TIMER"
        const val ACTION_TIMER_COMPLETE = "com.example.learnlog.TIMER_COMPLETE"

        const val EXTRA_DURATION_MILLIS = "duration_millis"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TITLE = "task_title"
    }
}

