package com.example.learnlog.data.export

import android.content.Context
import android.net.Uri
import com.example.learnlog.data.dao.DailyRollupDao
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.TaskDao
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataImporter @Inject constructor(
    private val taskDao: TaskDao,
    private val sessionLogDao: SessionLogDao,
    private val dailyRollupDao: DailyRollupDao,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    suspend fun previewImport(uri: Uri): ImportPreview = withContext(Dispatchers.IO) {
        try {
            val data = parseJson(uri)
            ImportPreview.Success(data.tasks.size, data.sessions.size, data.plannerEntries.size)
        } catch (e: Exception) {
            try {
                // Try CSV in ZIP
                val (taskCount, sessionCount) = parseCsvZip(uri)
                ImportPreview.Success(taskCount, sessionCount, 0)
            } catch (e2: Exception) {
                ImportPreview.Error(e2.message ?: "Invalid file format")
            }
        }
    }

    suspend fun importData(uri: Uri, mode: ImportMode): ImportResult = withContext(Dispatchers.IO) {
        try {
            val data = try {
                parseJson(uri)
            } catch (e: Exception) {
                // Try CSV
                parseCsvToExportData(uri)
            }

            when (mode) {
                ImportMode.MERGE -> mergeData(data)
                ImportMode.REPLACE -> replaceData(data)
            }

            // Refresh rollups for affected days
            refreshRollups(data)

            ImportResult.Success(data.tasks.size, data.sessions.size, data.plannerEntries.size)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "Import failed")
        }
    }

    private suspend fun parseJson(uri: Uri): ExportData {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                return gson.fromJson(reader, ExportData::class.java)
            }
        } ?: throw Exception("Cannot open file")
    }

    private suspend fun parseCsvZip(uri: Uri): Pair<Int, Int> {
        var taskCount = 0
        var sessionCount = 0

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "tasks.csv" -> {
                            BufferedReader(InputStreamReader(zipIn, Charsets.UTF_8)).use { reader ->
                                reader.readLine() // Skip header
                                while (reader.readLine() != null) taskCount++
                            }
                        }
                        "sessions.csv" -> {
                            BufferedReader(InputStreamReader(zipIn, Charsets.UTF_8)).use { reader ->
                                reader.readLine() // Skip header
                                while (reader.readLine() != null) sessionCount++
                            }
                        }
                    }
                    entry = zipIn.nextEntry
                }
            }
        }

        return taskCount to sessionCount
    }

    private suspend fun parseCsvToExportData(uri: Uri): ExportData {
        val tasks = mutableListOf<TaskExport>()
        val sessions = mutableListOf<SessionExport>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipIn ->
                var entry = zipIn.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "tasks.csv" -> {
                            BufferedReader(InputStreamReader(zipIn, Charsets.UTF_8)).use { reader ->
                                reader.readLine() // Skip header
                                var line = reader.readLine()
                                while (line != null) {
                                    val parts = parseCsvLine(line)
                                    if (parts.size >= 9) {
                                        tasks.add(TaskExport(
                                            id = parts[0].toLongOrNull() ?: 0,
                                            title = parts[1],
                                            subject = parts[2].ifBlank { null },
                                            priority = parts[3].toIntOrNull() ?: 0,
                                            status = parts[4],
                                            type = parts[5].ifBlank { "ASSIGNMENT" },
                                            dueAt = parts[6].ifBlank { null },
                                            createdAt = parts[7],
                                            updatedAt = parts[8],
                                            notes = null
                                        ))
                                    }
                                    line = reader.readLine()
                                }
                            }
                        }
                        "sessions.csv" -> {
                            BufferedReader(InputStreamReader(zipIn, Charsets.UTF_8)).use { reader ->
                                reader.readLine() // Skip header
                                var line = reader.readLine()
                                while (line != null) {
                                    val parts = parseCsvLine(line)
                                    if (parts.size >= 6) {
                                        sessions.add(SessionExport(
                                            id = parts[0].toLongOrNull() ?: 0,
                                            taskId = parts[1].toLongOrNull(),
                                            subject = parts[2].ifBlank { null },
                                            startAt = parts[3],
                                            endAt = parts[4],
                                            durationSeconds = parts[5].toIntOrNull() ?: 0,
                                            notes = parts.getOrNull(6)
                                        ))
                                    }
                                    line = reader.readLine()
                                }
                            }
                        }
                    }
                    entry = zipIn.nextEntry
                }
            }
        }

        return ExportData(
            exportDate = org.threeten.bp.Instant.now().toString(),
            tasks = tasks,
            sessions = sessions,
            plannerEntries = emptyList()
        )
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (c in line) {
            when {
                c == '"' && !inQuotes -> inQuotes = true
                c == '"' && inQuotes -> inQuotes = false
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(c)
            }
        }
        result.add(current.toString())

        return result
    }

    private suspend fun mergeData(data: ExportData) {
        // Merge tasks (update if newer)
        data.tasks.forEach { taskExport ->
            val existing = taskDao.getById(taskExport.id)
            if (existing == null) {
                taskDao.insert(TaskExport.toEntity(taskExport))
            } else {
                val exportUpdated = org.threeten.bp.Instant.parse(taskExport.updatedAt)
                val existingUpdated = existing.updatedAt.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant()
                if (exportUpdated.isAfter(existingUpdated)) {
                    taskDao.update(TaskExport.toEntity(taskExport))
                }
            }
        }

        // Merge sessions (insert if not exists)
        data.sessions.forEach { sessionExport ->
            val existing = sessionLogDao.getSessionById(sessionExport.id)
            if (existing == null) {
                sessionLogDao.insertSession(SessionExport.toEntity(sessionExport))
            }
        }
    }

    private suspend fun replaceData(data: ExportData) {
        // Clear existing data
        data.tasks.forEach { taskDao.deleteById(it.id) }

        // Insert new data
        data.tasks.forEach { taskDao.insert(TaskExport.toEntity(it)) }
        data.sessions.forEach { sessionLogDao.insertSession(SessionExport.toEntity(it)) }
    }

    private suspend fun refreshRollups(data: ExportData) {
        // Trigger rollup refresh for affected dates
        val dates = data.sessions.map { session ->
            org.threeten.bp.Instant.parse(session.startAt)
                .atZone(org.threeten.bp.ZoneId.systemDefault())
                .toLocalDate()
                .toString()
        }.distinct()

        // Delete old rollups for these dates so they get recomputed
        dates.forEach { date ->
            // Note: DailyRollupDao doesn't have delete by date, but we could add it
            // For now, the next rollup job will update them
        }
    }
}

enum class ImportMode {
    MERGE, REPLACE
}

sealed class ImportPreview {
    data class Success(val taskCount: Int, val sessionCount: Int, val plannerCount: Int) : ImportPreview()
    data class Error(val message: String) : ImportPreview()
}

sealed class ImportResult {
    data class Success(val taskCount: Int, val sessionCount: Int, val plannerCount: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

