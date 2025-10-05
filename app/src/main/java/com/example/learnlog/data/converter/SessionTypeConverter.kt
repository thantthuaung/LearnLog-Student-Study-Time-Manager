package com.example.learnlog.data.converter

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.example.learnlog.data.model.SessionType

@ProvidedTypeConverter
class SessionTypeConverter {
    @TypeConverter
    fun fromString(value: String?): SessionType {
        return try {
            value?.let { enumValueOf<SessionType>(it) } ?: SessionType.FOCUS
        } catch (e: Exception) {
            SessionType.FOCUS
        }
    }

    @TypeConverter
    fun toString(sessionType: SessionType?): String {
        return sessionType?.name ?: SessionType.FOCUS.name
    }
}
