package com.example.learnlog.ui.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.learnlog.MainActivity
import com.example.learnlog.R

class TimerNotificationManager(private val context: Context) {
    companion object {
        private const val TIMER_NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "timer_channel"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.timer_notification_channel)
            val importance = NotificationManager.IMPORTANCE_LOW // Low to avoid sound
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Shows timer countdown"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTimerNotification(remainingMillis: Long, isPaused: Boolean = false) {
        val minutes = remainingMillis / 1000 / 60
        val seconds = remainingMillis / 1000 % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        // Intent to open Timer tab when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_timer_tab", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "Paused" else "Running"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Timer $statusText")
            .setContentText("Time remaining: $timeText")
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(TIMER_NOTIFICATION_ID, notification)
    }

    fun updateTimerNotification(remainingMillis: Long, isPaused: Boolean = false) {
        showTimerNotification(remainingMillis, isPaused)
    }

    fun cancelTimerNotification() {
        notificationManager.cancel(TIMER_NOTIFICATION_ID)
    }
}
