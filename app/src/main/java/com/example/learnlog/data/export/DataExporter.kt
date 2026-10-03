package com.example.learnlog.data.export

import android.content.Context
import android.net.Uri
import com.example.learnlog.data.dao.NoteDao
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.TaskDao
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExporter @Inject constructor(
    private val taskDao: TaskDao,
    private val sessionLogDao: SessionLogDao,
    private val noteDao: NoteDao,
    @ApplicationContext private val context: Context
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()

    /** Full backup (tasks, sessions, notes) as one JSON file. Restorable with [DataImporter]. */
    suspend fun exportToJson(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        runCatching {
            val data = ExportData(
                exportDate = org.threeten.bp.Instant.now().toString(),
                tasks = taskDao.getAllOnce().map(TaskExport::fromEntity),
                sessions = sessionLogDao.getAllOnce().map(SessionExport::fromEntity),
                notes = noteDao.getAllOnce().map(NoteExport::fromEntity)
            )
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Couldn't open the file for writing")
            BufferedWriter(OutputStreamWriter(stream, Charsets.UTF_8)).use { gson.toJson(data, it) }
            ExportResult.Success(data.tasks.orEmpty().size, data.sessions.orEmpty().size, data.notes.orEmpty().size)
        }.getOrElse { ExportResult.Error(it.message ?: "Export failed") }
    }

    /** Spreadsheet-friendly export: a .zip holding tasks.csv, sessions.csv and notes.csv. */
    suspend fun exportToCsvZip(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        runCatching {
            val tasks = taskDao.getAllOnce()
            val sessions = sessionLogDao.getAllOnce()
            val notes = noteDao.getAllOnce()
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Couldn't open the file for writing")
            ZipOutputStream(stream).use { zip ->
                val writer = BufferedWriter(OutputStreamWriter(zip, Charsets.UTF_8))
                fun entry(name: String, header: List<String>, rows: List<List<Any?>>) {
                    zip.putNextEntry(ZipEntry(name))
                    writer.write(Csv.line(header))
                    rows.forEach { writer.write(Csv.line(it)) }
                    writer.flush()
                    zip.closeEntry()
                }
                entry(
                    "tasks.csv",
                    listOf("id", "title", "subject", "priority", "status", "type", "due_at", "created_at", "updated_at", "completed", "duration_minutes", "notes"),
                    tasks.map { TaskExport.fromEntity(it) }.map { t ->
                        listOf(t.id, t.title, t.subject, t.priority, t.status, t.type, t.dueAt, t.createdAt, t.updatedAt, t.completed, t.durationMinutes, t.notes)
                    }
                )
                entry(
                    "sessions.csv",
                    listOf("id", "task_id", "subject", "start_at", "end_at", "duration_s", "is_completed", "notes"),
                    sessions.map { SessionExport.fromEntity(it) }.map { s ->
                        listOf(s.id, s.taskId, s.subject, s.startAt, s.endAt, s.durationSeconds, s.isCompleted, s.notes)
                    }
                )
                entry(
                    "notes.csv",
                    listOf("id", "title", "content", "tags", "is_pinned", "created_at", "updated_at"),
                    notes.map { NoteExport.fromEntity(it) }.map { n ->
                        listOf(n.id, n.title, n.content, n.tags.orEmpty().joinToString("|"), n.isPinned, n.createdAt, n.updatedAt)
                    }
                )
            }
            ExportResult.Success(tasks.size, sessions.size, notes.size)
        }.getOrElse { ExportResult.Error(it.message ?: "Export failed") }
    }
}

/** Minimal RFC 4180 CSV helpers shared by export and import. */
internal object Csv {
    fun line(values: List<Any?>): String = values.joinToString(",") { escape(it?.toString() ?: "") } + "\r\n"

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    /** Parses a whole CSV document, honouring quoted fields with commas, quotes and newlines. */
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                !inQuotes && c == ',' -> { row.add(field.toString()); field.clear() }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(field.toString()); field.clear()
                    if (row.any { it.isNotEmpty() }) rows.add(row)
                    row = mutableListOf()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) { row.add(field.toString()); rows.add(row) }
        return rows
    }
}

sealed class ExportResult {
    data class Success(val taskCount: Int, val sessionCount: Int, val noteCount: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
