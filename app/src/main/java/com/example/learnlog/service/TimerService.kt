package com.example.learnlog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
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
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var timerJob: Job? = null
    private val _remainingMillis = MutableStateFlow(0L)
    val remainingMillis: StateFlow<Long> = _remainingMillis

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private var totalDurationMs = 0L

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "timer_service_channel"
        const val ACTION_PAUSE = "com.example.learnlog.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.learnlog.ACTION_RESUME"
        const val ACTION_STOP = "com.example.learnlog.ACTION_STOP"
    }

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }
        return START_STICKY
    }

    fun startTimer(durationMs: Long) {
        totalDurationMs = durationMs
        _remainingMillis.value = durationMs
        _isRunning.value = true

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (_remainingMillis.value > 0 && isActive) {
                delay(1000)
                if (_isRunning.value) {
                    _remainingMillis.value -= 1000
                    updateNotification()
                }
            }
            if (_remainingMillis.value <= 0) {
                onTimerComplete()
            }
        }

        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun pauseTimer() {
        _isRunning.value = false
        updateNotification()
    }

    fun resumeTimer() {
        _isRunning.value = true
        updateNotification()
    }

    fun stopTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerComplete() {
        _isRunning.value = false
        // Broadcast completion
        val completeIntent = Intent("com.example.learnlog.TIMER_COMPLETE")
        sendBroadcast(completeIntent)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows timer progress"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseResumeIntent = if (_isRunning.value) {
            Intent(this, TimerService::class.java).apply { action = ACTION_PAUSE }
        } else {
            Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }
        }
        val pauseResumePendingIntent = PendingIntent.getService(
            this, 1, pauseResumeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val timeText = formatTime(_remainingMillis.value)
        val statusText = if (_isRunning.value) "Running" else "Paused"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Timer $statusText")
            .setContentText(timeText)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .addAction(
                if (_isRunning.value) R.drawable.ic_pause else R.drawable.ic_play,
                if (_isRunning.value) "Pause" else "Resume",
                pauseResumePendingIntent
            )
            .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
            .setProgress(100, getProgress(), false)
            .build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun getProgress(): Int {
        return if (totalDurationMs > 0) {
            ((totalDurationMs - _remainingMillis.value) * 100 / totalDurationMs).toInt()
        } else {
            0
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }
}

