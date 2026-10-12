package com.example.learnlog.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Light / dark / system appearance. Defaults to light (white) regardless of the phone setting. Kept in SharedPreferences (not DataStore) because it must
 * be read synchronously before the first Activity draws.
 */
object ThemePrefs {
    private const val FILE = "ui_prefs"
    private const val KEY_MODE = "theme_mode"

    const val SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    const val LIGHT = AppCompatDelegate.MODE_NIGHT_NO
    const val DARK = AppCompatDelegate.MODE_NIGHT_YES

    fun get(context: Context): Int =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getInt(KEY_MODE, LIGHT)

    fun set(context: Context, mode: Int) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putInt(KEY_MODE, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun apply(context: Context) = AppCompatDelegate.setDefaultNightMode(get(context))
}
