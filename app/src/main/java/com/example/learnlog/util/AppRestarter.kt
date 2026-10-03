package com.example.learnlog.util

import android.app.Activity
import android.content.Intent
import com.example.learnlog.MainActivity
import kotlin.system.exitProcess

/**
 * Restarts the app in a fresh process. Used when the signed-in account changes so every
 * singleton (database, repositories, timer) is rebuilt for the new account.
 */
object AppRestarter {
    fun restart(activity: Activity) {
        val intent = Intent(activity, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(intent)
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        activity.finishAffinity()
        exitProcess(0)
    }
}
