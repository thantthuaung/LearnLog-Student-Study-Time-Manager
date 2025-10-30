package com.example.learnlog.util

import android.view.View
import com.google.android.material.snackbar.Snackbar

/**
 * Show error with optional retry action
 */
fun View.showError(
    message: String,
    onRetry: (() -> Unit)? = null
) {
    val snackbar = Snackbar.make(this, message, Snackbar.LENGTH_LONG)

    if (onRetry != null) {
        snackbar.setAction("Retry") {
            onRetry()
        }
    }

    snackbar.show()
}

/**
 * Show success message
 */
fun View.showSuccess(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
}

/**
 * Show loading message
 */
fun View.showLoading(message: String = "Loading...") {
    Snackbar.make(this, message, Snackbar.LENGTH_SHORT).show()
}

/**
 * Handle safe operations with error feedback
 */
suspend fun <T> safeOperation(
    operation: suspend () -> T,
    onError: (String) -> Unit
): T? {
    return try {
        operation()
    } catch (e: Exception) {
        val message = when (e) {
            is java.io.IOException -> "Network error. Please check your connection."
            is android.database.sqlite.SQLiteException -> "Database error. Please try again."
            else -> "An unexpected error occurred: ${e.message}"
        }
        onError(message)
        null
    }
}

