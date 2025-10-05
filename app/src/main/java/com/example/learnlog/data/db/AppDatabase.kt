package com.example.learnlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.learnlog.data.converter.DateTimeConverter
import com.example.learnlog.data.converter.SessionTypeConverter
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.SubjectDao
import com.example.learnlog.data.model.SessionLog
import com.example.learnlog.data.model.Subject

@Database(
    entities = [
        SessionLog::class,
        Subject::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class, SessionTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionLogDao(): SessionLogDao
    abstract fun subjectDao(): SubjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "learnlog_database"
                ).run {
                    val dateTimeConverter = DateTimeConverter()
                    val sessionTypeConverter = SessionTypeConverter()
                    addTypeConverter(dateTimeConverter)
                    addTypeConverter(sessionTypeConverter)
                    fallbackToDestructiveMigration()
                    build()
                }.also { INSTANCE = it }
            }
        }
    }
}
