package com.example.learnlog.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.learnlog.MainActivity
import com.example.learnlog.R
import com.example.learnlog.data.preferences.UserPreferences
import kotlinx.coroutines.flow.first

/** Notification channels plus the end-of-session alert (notification, sound, vibration). */
object TimerAlerts {
    const val CHANNEL_ONGOING = "focus_timer_ongoing"
    const val CHANNEL_ALERTS = "focus_timer_alerts"
    private const val COMPLETE_ID = 2002

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ONGOING, context.getString(R.string.channel_timer_ongoing), NotificationManager.IMPORTANCE_LOW).apply {
                description = context.getString(R.string.channel_timer_ongoing_desc)
                setShowBadge(false)
            }
        )
        manager.createNotificationChannel(
            // Silent channel: sound and vibration are played by the app so the in-app settings apply
            NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_timer_alerts), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.channel_timer_alerts_desc)
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    fun openTimerIntent(context: Context, requestCode: Int = 0): PendingIntent =
        PendingIntent.getActivity(
            context, requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_OPEN_TIMER, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    suspend fun sessionComplete(context: Context, prefs: UserPreferences, minutes: Int, taskTitle: String?) {
        if (!prefs.notificationsEnabled.first()) return

        when (prefs.notificationSound.first()) {
            UserPreferences.SOUND_DEFAULT -> playTone(context, RingtoneManager.TYPE_NOTIFICATION)
            UserPreferences.SOUND_ALARM -> playTone(context, RingtoneManager.TYPE_ALARM)
        }
        if (prefs.notificationVibrate.first()) vibrate(context)

        if (!canPostNotifications(context)) return
        ensureChannels(context)
        val body = if (taskTitle != null) {
            context.getString(R.string.notif_complete_task, minutes, taskTitle)
        } else {
            context.getString(R.string.notif_complete_body, minutes)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_nav_timer_fill)
            .setColor(ContextCompat.getColor(context, R.color.ll_primary))
            .setContentTitle(context.getString(R.string.notif_complete_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openTimerIntent(context, 1))
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(COMPLETE_ID, notification) }
    }

    private fun playTone(context: Context, type: Int) {
        runCatching {
            val uri = RingtoneManager.getDefaultUri(type) ?: return
            RingtoneManager.getRingtone(context, uri)?.apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                play()
            }
        }
    }

    private fun vibrate(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator ?: return
        val pattern = longArrayOf(0, 280, 140, 280, 140, 420)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }

    /** Light haptic tick for UI interactions (task completed, timer started). */
    fun tick(context: Context) {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }
}
