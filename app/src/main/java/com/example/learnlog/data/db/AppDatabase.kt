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
import com.google.firebase.auth.FirebaseAuth

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
        private const val LEGACY_NAME = "learnlog_database"
        private const val GUEST = "guest"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** The account the open database belongs to, or null if none has been opened yet. */
        @Volatile
        var openedForUid: String? = null
            private set

        private fun currentUid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: GUEST

        /** True when the database already open in this process belongs to a different account. */
        fun isOpenForAnotherAccount(uid: String): Boolean =
            openedForUid != null && openedForUid != uid

        /**
         * Each signed-in account gets its own database file, so one person's tasks and sessions
         * never show up for the next person who signs in on the same device.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context.applicationContext, currentUid()).also { INSTANCE = it }
            }
        }

        private fun build(context: Context, uid: String): AppDatabase {
            val name = "learnlog_$uid"
            adoptLegacyDatabase(context, uid, name)
            openedForUid = uid
            return Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        // The old timer inserted a row when a session started and left it behind if
                        // the session was abandoned; those rows inflated Analytics.
                        db.execSQL("DELETE FROM session_logs WHERE is_completed = 0")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }

        /** Before per-account files existed, everything lived in one shared database. The first
         *  signed-in account to open the app after the update inherits it. */
        private fun adoptLegacyDatabase(context: Context, uid: String, name: String) {
            if (uid == GUEST) return
            val legacy = context.getDatabasePath(LEGACY_NAME)
            val target = context.getDatabasePath(name)
            if (!legacy.exists() || target.exists()) return
            for (suffix in listOf("", "-wal", "-shm")) {
                val from = java.io.File(legacy.path + suffix)
                if (from.exists()) from.renameTo(java.io.File(target.path + suffix))
            }
        }

        private fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean =
            query("PRAGMA table_info(`$table`)").use { c ->
                val nameIdx = c.getColumnIndex("name")
                while (c.moveToNext()) if (c.getString(nameIdx) == column) return true
                false
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `tasks`")
                db.execSQL("""
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN taskId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
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
            override fun migrate(db: SupportSQLiteDatabase) {
                // Planned study time per task
                if (!db.hasColumn("tasks", "durationMinutes")) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN durationMinutes INTEGER NOT NULL DEFAULT 30")
                }
                // `type` was added to TaskEntity without its own migration; tables created by
                // MIGRATION_1_2 lack it and would fail Room's schema validation.
                if (!db.hasColumn("tasks", "type")) {
                    db.execSQL("ALTER TABLE tasks ADD COLUMN type TEXT NOT NULL DEFAULT 'ASSIGNMENT'")
                }
            }
        }
    }
}
