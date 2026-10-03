package com.example.learnlog.data.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.learnlog.data.converter.DateTimeConverter
import com.example.learnlog.data.converter.LocalDateTimeParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import org.threeten.bp.LocalDateTime

@Parcelize
@TypeParceler<LocalDateTime?, LocalDateTimeParceler>
@Entity(tableName = "tasks")
@TypeConverters(DateTimeConverter::class)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val subject: String?,
    val dueAt: LocalDateTime?,
    /** 0 = low, 1 = medium, 2 = high (index into R.array.priority_options). */
    val priority: Int,
    /** One of [STATUS_PENDING], [STATUS_IN_PROGRESS], [STATUS_COMPLETED]. */
    val status: String,
    val progress: Int = 0,
    val completed: Boolean = false,
    val notes: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    /** One of [TYPE_ASSIGNMENT], [TYPE_EXAM], [TYPE_PROJECT], [TYPE_STUDY]. */
    val type: String,
    val durationMinutes: Int = 30  // Planned study time
) : Parcelable {

    val isNew: Boolean get() = id == 0L

    fun isOverdue(now: LocalDateTime = LocalDateTime.now()): Boolean =
        !completed && dueAt != null && dueAt.isBefore(now)

    /**
     * Single place that flips completion, so `completed` and `status` can never disagree
     * (Insights reads `status`, the task list reads `completed`).
     */
    fun withCompleted(done: Boolean): TaskEntity = copy(
        completed = done,
        status = when {
            done -> STATUS_COMPLETED
            status == STATUS_COMPLETED -> STATUS_PENDING
            else -> status
        },
        progress = if (done) 100 else progress.coerceAtMost(99),
        updatedAt = LocalDateTime.now()
    )

    fun withInProgress(): TaskEntity =
        if (completed) this else copy(status = STATUS_IN_PROGRESS, updatedAt = LocalDateTime.now())

    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"

        const val TYPE_ASSIGNMENT = "ASSIGNMENT"
        const val TYPE_EXAM = "EXAM"
        const val TYPE_PROJECT = "PROJECT"
        const val TYPE_STUDY = "STUDY_SESSION"
        val TYPES = listOf(TYPE_ASSIGNMENT, TYPE_EXAM, TYPE_PROJECT, TYPE_STUDY)

        const val PRIORITY_LOW = 0
        const val PRIORITY_MEDIUM = 1
        const val PRIORITY_HIGH = 2

        /** Normalises legacy values such as "Assignment" / "Exam" to the canonical constants. */
        fun normalizeType(raw: String?): String {
            val upper = raw?.trim()?.uppercase()?.replace(' ', '_') ?: return TYPE_ASSIGNMENT
            return when (upper) {
                "EXAM", "TEST", "QUIZ" -> TYPE_EXAM
                "PROJECT" -> TYPE_PROJECT
                "STUDY", "STUDY_SESSION", "REVISION" -> TYPE_STUDY
                else -> TYPE_ASSIGNMENT
            }
        }
    }
}
