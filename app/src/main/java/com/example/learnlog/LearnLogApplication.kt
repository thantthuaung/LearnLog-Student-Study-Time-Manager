package com.example.learnlog

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.learnlog.timer.TimerAlerts
import com.example.learnlog.util.ThemePrefs
import com.example.learnlog.worker.DailyRollupWorker
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class LearnLogApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        ThemePrefs.apply(this)
        TimerAlerts.ensureChannels(this)
        scheduleDailyRollupWork()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun scheduleDailyRollupWork() {
        val request = PeriodicWorkRequestBuilder<DailyRollupWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_rollup_work",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
