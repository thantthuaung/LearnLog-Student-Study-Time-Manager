package com.example.learnlog.data.converter

import androidx.room.TypeConverter
import com.example.learnlog.data.model.StudyType

class SessionTypeConverter {
    @TypeConverter
    fun fromString(value: String?): StudyType? {
        return value?.let { StudyType.valueOf(it) }
    }

    @TypeConverter
    fun toString(type: StudyType?): String? {
        return type?.name
    }
}
