package com.example.learnlog.ui.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.learnlog.R
import com.example.learnlog.data.model.SessionType

class TimerNotificationManager(private val context: Context) {
    companion object {
        private const val TIMER_NOTIFICATION_ID = 1
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "timer_channel"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.timer_notification_channel)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showTimerNotification(timeLeft: String, type: SessionType) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.timer_notification_title))
            .setContentText(
                if (type == SessionType.FOCUS)
                    context.getString(R.string.timer_notification_focus)
                else
                    context.getString(R.string.timer_notification_break)
            )
            .setOngoing(true)
            .build()

        notificationManager.notify(TIMER_NOTIFICATION_ID, notification)
    }

    fun showSessionCompleteNotification(type: SessionType) {
        val message = if (type == SessionType.FOCUS) {
            context.getString(R.string.timer_session_complete)
        } else {
            context.getString(R.string.timer_break_complete)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(context.getString(R.string.timer_notification_title))
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TIMER_NOTIFICATION_ID, notification)
    }
}
