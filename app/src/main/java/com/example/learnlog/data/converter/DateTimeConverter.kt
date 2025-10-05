package com.example.learnlog.data.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

@ProvidedTypeConverter
class DateTimeConverter {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return try {
            value?.let { LocalDateTime.parse(it, formatter) }
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return try {
            dateTime?.format(formatter)
        } catch (e: Exception) {
            null
        }
    }
}
