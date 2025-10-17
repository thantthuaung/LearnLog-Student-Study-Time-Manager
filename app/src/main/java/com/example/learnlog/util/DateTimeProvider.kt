package com.example.learnlog.util

import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateTimeProvider @Inject constructor() {
    fun now(): LocalDateTime = LocalDateTime.now()

    fun getZoneId(): ZoneId = ZoneId.systemDefault()

    fun nowWithTime(): LocalDateTime = LocalDateTime.now()
}
