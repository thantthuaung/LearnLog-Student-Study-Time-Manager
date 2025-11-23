package com.example.learnlog.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.learnlog.data.Note
import com.example.learnlog.data.converter.*
import com.example.learnlog.data.dao.*
import com.example.learnlog.data.entity.*
import com.example.learnlog.data.model.Subject

@Database(
    entities = [
        TaskEntity::class,
        SessionLogEntity::class,
        Subject::class,
        Note::class,
        DailyRollupEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(
    DateTimeConverter::class,
    SessionTypeConverter::class,
    StringListConverter::class,
    DateConverter::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun sessionLogDao(): SessionLogDao
    abstract fun subjectDao(): SubjectDao
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun dailyRollupDao(): DailyRollupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "learnlog_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("DROP TABLE IF EXISTS `tasks`")
                database.execSQL("""
                    CREATE TABLE `tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subject` TEXT,
                        `dueAt` INTEGER,
                        `priority` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `progress` INTEGER NOT NULL,
                        `completed` INTEGER NOT NULL,
                        `notes` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `session_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `task_id` INTEGER,
                        `subject` TEXT,
                        `start_time` INTEGER NOT NULL,
                        `end_time` INTEGER NOT NULL,
                        `duration_minutes` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `is_completed` INTEGER NOT NULL,
                        `notes` TEXT
                    )
                """)
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to notes table
                database.execSQL("ALTER TABLE notes ADD COLUMN taskId INTEGER DEFAULT NULL")
                database.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `daily_rollups` (
                        `date` TEXT PRIMARY KEY NOT NULL,
                        `totalMinutes` INTEGER NOT NULL,
                        `sessionCount` INTEGER NOT NULL,
                        `subjectBreakdown` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add durationMinutes column to tasks table for planned time tracking
                database.execSQL("ALTER TABLE tasks ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 30")
            }
        }
    }
}
