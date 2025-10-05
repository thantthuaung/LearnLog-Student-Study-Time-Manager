package com.example.learnlog

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LearnLogApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}
