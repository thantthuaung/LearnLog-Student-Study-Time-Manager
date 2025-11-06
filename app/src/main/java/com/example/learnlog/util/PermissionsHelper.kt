package com.example.learnlog.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

/**
 * Helper for managing runtime permissions
 * Handles POST_NOTIFICATIONS permission for Android 13+
 */
object PermissionsHelper {

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Notifications don't require permission below Android 13
            true
        }
    }

    /**
     * Check if we should show rationale for notification permission
     */
    fun shouldShowNotificationRationale(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }
    }

    /**
     * Check if we should show rationale for notification permission (Fragment version)
     */
    fun shouldShowNotificationRationale(fragment: Fragment): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            fragment.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            false
        }
    }

    /**
     * Request notification permission using ActivityResultLauncher
     */
    fun requestNotificationPermission(
        launcher: ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Check if media permissions are granted (for custom notification sounds/ringtones)
     */
    fun hasMediaPermissions(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request media audio permission (for custom notification sounds)
     */
    fun requestMediaAudioPermission(
        launcher: ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            launcher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

/**
 * Permission request constants
 */
object PermissionConstants {
    const val NOTIFICATION_PERMISSION_RATIONALE =
        "LearnLog needs notification permission to remind you about study sessions and send timer completion alerts. This helps you stay on track with your study schedule."

    const val MEDIA_PERMISSION_RATIONALE =
        "LearnLog needs access to media files to allow you to set custom notification sounds for timer alerts."
}

