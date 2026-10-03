package com.example.learnlog.timer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.learnlog.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the process alive while a focus session is running and shows a live countdown with
 * Pause/Resume and Finish actions. All timing lives in [FocusTimer]; this only mirrors it.
 */
@AndroidEntryPoint
class FocusTimerService : Service() {

    @Inject lateinit var timer: FocusTimer

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watcher: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> timer.pause()
            ACTION_RESUME -> timer.start()
            ACTION_FINISH -> timer.stop(save = true)
        }
        // Must reach startForeground promptly after startForegroundService()
        TimerAlerts.ensureChannels(this)
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(timer.state.value),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        )
        if (!timer.state.value.isActive) {
            stopSelfCleanly()
            return START_NOT_STICKY
        }
        if (watcher == null) {
            watcher = scope.launch {
                // Re-render only when something visible changes; the countdown itself is a chronometer
                timer.state
                    .map { Snapshot(it.phase, it.task?.title, it.totalMs, if (it.phase == FocusTimer.Phase.PAUSED) it.remainingMs / 1000 else 0) }
                    .distinctUntilChanged()
                    .collect {
                        val state = timer.state.value
                        if (!state.isActive) stopSelfCleanly()
                        else NotificationManagerCompat.from(this@FocusTimerService).runCatching { notify(NOTIFICATION_ID, buildNotification(state)) }
                    }
            }
        }
        return START_NOT_STICKY
    }

    private data class Snapshot(val phase: FocusTimer.Phase, val title: String?, val total: Long, val pausedSeconds: Long)

    private fun stopSelfCleanly() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(state: FocusTimer.State): Notification {
        val running = state.phase == FocusTimer.Phase.RUNNING
        val title = state.task?.title ?: getString(R.string.focus_session)
        val builder = NotificationCompat.Builder(this, TimerAlerts.CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_nav_timer_fill)
            .setColor(ContextCompat.getColor(this, R.color.ll_primary))
            .setContentTitle(title)
            .setContentIntent(TimerAlerts.openTimerIntent(this))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setProgress(1000, (state.progress * 1000).toInt(), false)

        if (running) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(System.currentTimeMillis() + state.remainingMs)
                .setShowWhen(true)
                .setContentText(getString(R.string.notif_focusing))
                .addAction(R.drawable.ic_pause, getString(R.string.pause), actionIntent(ACTION_PAUSE))
        } else {
            builder.setUsesChronometer(false)
                .setShowWhen(false)
                .setContentText(getString(R.string.notif_paused, formatClock(state.remainingMs)))
                .addAction(R.drawable.ic_play, getString(R.string.resume), actionIntent(ACTION_RESUME))
        }
        builder.addAction(R.drawable.ic_check, getString(R.string.finish), actionIntent(ACTION_FINISH))
        return builder.build()
    }

    private fun actionIntent(action: String): PendingIntent =
        PendingIntent.getService(
            this, action.hashCode(),
            Intent(this, FocusTimerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_PAUSE = "com.example.learnlog.timer.PAUSE"
        private const val ACTION_RESUME = "com.example.learnlog.timer.RESUME"
        private const val ACTION_FINISH = "com.example.learnlog.timer.FINISH"
        private const val ACTION_REFRESH = "com.example.learnlog.timer.REFRESH"

        /** Called from the UI (app in foreground), which Android requires for foreground services. */
        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, FocusTimerService::class.java)) }
        }

        fun refresh(context: Context) {
            runCatching { context.startService(Intent(context, FocusTimerService::class.java).setAction(ACTION_REFRESH)) }
        }

        fun formatClock(ms: Long): String {
            val totalSeconds = (ms + 999) / 1000
            val h = totalSeconds / 3600
            val m = (totalSeconds % 3600) / 60
            val s = totalSeconds % 60
            return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
        }
    }
}
