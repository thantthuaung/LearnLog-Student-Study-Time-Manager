package com.example.learnlog.data.export

import android.content.Context
import android.net.Uri
import com.example.learnlog.data.dao.SessionLogDao
import com.example.learnlog.data.dao.TaskDao
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    @ApplicationContext private val context: Context
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    suspend fun exportToJson(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        try {
            val tasks = taskDao.getAll().first().map { TaskExport.fromEntity(it) }
            val sessions = sessionLogDao.getAllSessions().first().map { SessionExport.fromEntity(it) }

            val exportData = ExportData(
                exportDate = org.threeten.bp.Instant.now().toString(),
                tasks = tasks,
                sessions = sessions,
                plannerEntries = emptyList() // TODO: Add planner if needed
            )

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8)).use { writer ->
                    gson.toJson(exportData, writer)
                }
            }

            ExportResult.Success(tasks.size, sessions.size, 0)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Export failed")
        }
    }

    suspend fun exportToCsv(uri: Uri): ExportResult = withContext(Dispatchers.IO) {
        try {
            val tasks = taskDao.getAll().first()
            val sessions = sessionLogDao.getAllSessions().first()

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    // Export tasks
                    zipOut.putNextEntry(ZipEntry("tasks.csv"))
                    BufferedWriter(OutputStreamWriter(zipOut, Charsets.UTF_8)).use { writer ->
                        writer.write("id,title,subject,priority,status,type,due_at,created_at,updated_at\n")
                        tasks.forEach { task ->
                            val export = TaskExport.fromEntity(task)
                            writer.write("${export.id},\"${escape(export.title)}\",\"${escape(export.subject ?: "")}\",${export.priority},${export.status},${export.type},\"${export.dueAt ?: ""}\",\"${export.createdAt}\",\"${export.updatedAt}\"\n")
                        }
                    }
                    zipOut.closeEntry()

                    // Export sessions
                    zipOut.putNextEntry(ZipEntry("sessions.csv"))
                    BufferedWriter(OutputStreamWriter(zipOut, Charsets.UTF_8)).use { writer ->
                        writer.write("id,task_id,subject,start_at,end_at,duration_s,notes\n")
                        sessions.forEach { session ->
                            val export = SessionExport.fromEntity(session)
                            writer.write("${export.id},${export.taskId ?: ""},\"${escape(export.subject ?: "")}\",\"${export.startAt}\",\"${export.endAt}\",${export.durationSeconds},\"${escape(export.notes ?: "")}\"\n")
                        }
                    }
                    zipOut.closeEntry()
                }
            }

            ExportResult.Success(tasks.size, sessions.size, 0)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Export failed")
        }
    }

    private fun escape(value: String): String {
        return value.replace("\"", "\"\"")
    }
}

sealed class ExportResult {
    data class Success(val taskCount: Int, val sessionCount: Int, val plannerCount: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

