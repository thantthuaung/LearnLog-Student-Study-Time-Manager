package com.example.learnlog.data.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.learnlog.data.db.AppDatabase
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ImportMode { MERGE, REPLACE }

sealed class ImportResult {
    data class Success(val taskCount: Int, val sessionCount: Int, val noteCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

/**
 * Restores a JSON backup or a CSV .zip produced by [DataExporter] (current or v1 format).
 *
 * MERGE adds what is missing. Records are matched by content (creation time + title, or start
 * time + duration for sessions), not by database id, so a backup from another device can't
 * overwrite unrelated rows that happen to share an id.
 * REPLACE wipes tasks, sessions and notes first; everything happens in one transaction.
 */
@Singleton
class DataImporter @Inject constructor(
    private val db: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    suspend fun importData(uri: Uri, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        runCatching {
            val data = read(uri)
            val counts = db.withTransaction {
                when (mode) {
                    ImportMode.REPLACE -> replace(data)
                    ImportMode.MERGE -> merge(data)
                }
            }
            ImportResult.Success(counts.first, counts.second, counts.third)
        }.getOrElse { ImportResult.Error(it.message ?: "This file couldn't be imported") }
    }

    private fun read(uri: Uri): ExportData {
        val stream = context.contentResolver.openInputStream(uri) ?: error("Couldn't open the file")
        BufferedInputStream(stream).use { input ->
            input.mark(4)
            val b0 = input.read()
            val b1 = input.read()
            input.reset()
            return if (b0 == 'P'.code && b1 == 'K'.code) readCsvZip(input)
            else gson.fromJson(input.reader(Charsets.UTF_8), ExportData::class.java)
                ?: error("The file is empty")
        }
    }

    private suspend fun replace(data: ExportData): Triple<Int, Int, Int> {
        db.taskDao().deleteAll()
        db.sessionLogDao().deleteAll()
        db.noteDao().deleteAll()
        db.dailyRollupDao().clearAll()
        val tasks = data.tasks.orEmpty()
        val sessions = data.sessions.orEmpty()
        val notes = data.notes.orEmpty()
        db.taskDao().insertAll(tasks.map { it.toEntity() })
        db.sessionLogDao().insertAll(sessions.map { it.toEntity() })
        db.noteDao().insertAll(notes.map { it.toEntity() })
        return Triple(tasks.size, sessions.size, notes.size)
    }

    private suspend fun merge(data: ExportData): Triple<Int, Int, Int> {
        val taskDao = db.taskDao()
        val existingTasks = taskDao.getAllOnce().associateBy { taskKey(it.title, it.createdAt.toIso()) }
        val idMap = mutableMapOf<Long, Long>()
        var addedTasks = 0
        for (t in data.tasks.orEmpty()) {
            val match = existingTasks[taskKey(t.title, t.createdAt)]
            if (match != null) {
                idMap[t.id] = match.id
                if (t.updatedAt.fromIso().isAfter(match.updatedAt)) taskDao.update(t.toEntity(id = match.id))
            } else {
                idMap[t.id] = taskDao.insert(t.toEntity(id = 0))
                addedTasks++
            }
        }

        val sessionDao = db.sessionLogDao()
        val existingSessions = sessionDao.getAllOnce().map { it.startTime.toIso() to it.durationMinutes }.toHashSet()
        var addedSessions = 0
        for (s in data.sessions.orEmpty()) {
            val key = s.startAt to s.durationSeconds / 60
            if (key in existingSessions) continue
            sessionDao.insertSession(s.toEntity(id = 0, taskId = s.taskId?.let { idMap[it] }))
            existingSessions += key
            addedSessions++
        }

        val noteDao = db.noteDao()
        val existingNotes = noteDao.getAllOnce().map { taskKey(it.title, it.createdAt.toIso()) }.toHashSet()
        var addedNotes = 0
        for (n in data.notes.orEmpty()) {
            if (taskKey(n.title, n.createdAt) in existingNotes) continue
            noteDao.insertNote(n.toEntity(id = 0, taskId = n.taskId?.let { idMap[it] }))
            addedNotes++
        }
        return Triple(addedTasks, addedSessions, addedNotes)
    }

    /** Identity across devices: same title created at the same instant (to the second). */
    private fun taskKey(title: String, createdAtIso: String) = title.trim() + "|" + createdAtIso.take(19)

    // ───────────── CSV (.zip) ─────────────

    private fun readCsvZip(input: java.io.InputStream): ExportData {
        val tables = mutableMapOf<String, List<Map<String, String>>>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/')
                if (name.endsWith(".csv")) {
                    val rows = Csv.parse(zip.readBytes().toString(Charsets.UTF_8))
                    if (rows.isNotEmpty()) {
                        val header = rows.first().map { it.trim().lowercase() }
                        tables[name] = rows.drop(1).map { r -> header.indices.associate { header[it] to r.getOrElse(it) { "" } } }
                    }
                }
                entry = zip.nextEntry
            }
        }
        if (tables.isEmpty()) error("No CSV files found in this archive")

        fun Map<String, String>.str(key: String) = this[key]?.takeIf { it.isNotBlank() }
        val tasks = tables["tasks.csv"].orEmpty().mapNotNull { r ->
            TaskExport(
                id = r.str("id")?.toLongOrNull() ?: return@mapNotNull null,
                title = r.str("title") ?: return@mapNotNull null,
                subject = r.str("subject"),
                priority = r.str("priority")?.toIntOrNull() ?: 1,
                status = r.str("status") ?: "PENDING",
                dueAt = r.str("due_at"),
                createdAt = r.str("created_at") ?: return@mapNotNull null,
                updatedAt = r.str("updated_at") ?: r.str("created_at")!!,
                notes = r.str("notes"),
                type = r.str("type"),
                completed = r.str("completed")?.toBooleanStrictOrNull(),
                durationMinutes = r.str("duration_minutes")?.toIntOrNull()
            )
        }
        val sessions = tables["sessions.csv"].orEmpty().mapNotNull { r ->
            SessionExport(
                id = r.str("id")?.toLongOrNull() ?: return@mapNotNull null,
                taskId = r.str("task_id")?.toLongOrNull(),
                subject = r.str("subject"),
                startAt = r.str("start_at") ?: return@mapNotNull null,
                endAt = r.str("end_at") ?: r.str("start_at")!!,
                durationSeconds = r.str("duration_s")?.toIntOrNull() ?: 0,
                notes = r.str("notes"),
                isCompleted = r.str("is_completed")?.toBooleanStrictOrNull()
            )
        }
        val notes = tables["notes.csv"].orEmpty().mapNotNull { r ->
            NoteExport(
                id = r.str("id")?.toLongOrNull() ?: return@mapNotNull null,
                title = r.str("title") ?: return@mapNotNull null,
                content = r["content"].orEmpty(),
                taskId = null,
                tags = r.str("tags")?.split('|')?.filter { it.isNotBlank() },
                isPinned = r.str("is_pinned")?.toBooleanStrictOrNull() ?: false,
                createdAt = r.str("created_at") ?: return@mapNotNull null,
                updatedAt = r.str("updated_at") ?: r.str("created_at")!!
            )
        }
        return ExportData(exportDate = "", tasks = tasks, sessions = sessions, notes = notes)
    }
}
