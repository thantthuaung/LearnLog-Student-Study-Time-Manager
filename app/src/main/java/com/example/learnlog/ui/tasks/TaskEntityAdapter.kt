package com.example.learnlog.ui.tasks

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.ChronoUnit
import java.util.Locale

class TaskEntityAdapter(
    private val onTaskClick: (TaskEntity) -> Unit,
    private val onStartTimer: (TaskEntity) -> Unit,
    private val onCheckChanged: (TaskEntity, Boolean) -> Unit,
    private val onLongPress: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TaskEntityAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.textTaskTitle)
        private val subjectText: TextView = itemView.findViewById(R.id.textTaskSubject)
        private val dueDateText: TextView = itemView.findViewById(R.id.textTaskDue)
        private val statusText: TextView = itemView.findViewById(R.id.textTaskStatus)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressTask)
        private val thinProgressLine: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val priorityDot: View = itemView.findViewById(R.id.priorityDot)
        private val checkBox: CheckBox = itemView.findViewById(R.id.checkComplete)
        private val startTimerButton: Button = itemView.findViewById(R.id.btnStartTimerTask)
        private val editButton: Button = itemView.findViewById(R.id.btnEditTask)

        private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", Locale.getDefault())

        fun bind(task: TaskEntity) {
            titleText.text = task.title

            // Subject chip
            if (task.subject.isNullOrBlank()) {
                subjectText.visibility = View.GONE
            } else {
                subjectText.visibility = View.VISIBLE
                subjectText.text = task.subject
            }

            // Due date
            val dueText = if (task.dueAt != null) {
                dateFormatter.format(task.dueAt)
            } else {
                "No due date"
            }
            dueDateText.text = dueText

            // Priority dot color
            val priorityColor = when (task.priority) {
                0 -> R.color.low_priority // Low = green
                1 -> R.color.medium_priority // Medium = amber
                else -> R.color.high_priority // High = red
            }
            priorityDot.setBackgroundColor(ContextCompat.getColor(itemView.context, priorityColor))

            // Checkbox
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = task.completed
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                onCheckChanged(task, isChecked)
            }

            // Calculate and display status
            val statusInfo = calculateStatus(task)
            statusText.text = statusInfo.text
            statusText.setTextColor(ContextCompat.getColor(itemView.context, statusInfo.textColor))

            // Thin progress line (time-to-due) with animation
            thinProgressLine.visibility = View.VISIBLE
            val progressInfo = calculateProgress(task)

            // Animate progress
            val currentProgress = thinProgressLine.progress
            if (currentProgress != progressInfo.progress) {
                ValueAnimator.ofInt(currentProgress, progressInfo.progress).apply {
                    duration = 300
                    addUpdateListener { animator ->
                        thinProgressLine.progress = animator.animatedValue as Int
                    }
                    start()
                }
            } else {
                thinProgressLine.progress = progressInfo.progress
            }

            // Animate color change
            val newColor = ContextCompat.getColor(itemView.context, progressInfo.color)
            val currentColor = thinProgressLine.progressDrawable.colorFilter?.let {
                // Try to get current color, fallback to new color
                newColor
            } ?: newColor

            ValueAnimator().apply {
                setIntValues(currentColor, newColor)
                setEvaluator(ArgbEvaluator())
                duration = 300
                addUpdateListener { animator ->
                    thinProgressLine.progressDrawable.setColorFilter(
                        animator.animatedValue as Int,
                        PorterDuff.Mode.SRC_IN
                    )
                }
                start()
            }

            // Buttons
            startTimerButton.setOnClickListener { onStartTimer(task) }
            editButton.setOnClickListener { onTaskClick(task) }

            // Long press on card
            itemView.setOnLongClickListener {
                onLongPress(task)
                true
            }

            // Hide progress bar (using thin line instead)
            progressBar.visibility = View.GONE
        }

        private fun calculateStatus(task: TaskEntity): StatusInfo {
            val now = LocalDateTime.now()

            return when {
                task.completed -> StatusInfo(
                    text = itemView.context.getString(R.string.status_completed),
                    textColor = R.color.low_priority
                )
                task.dueAt != null && task.dueAt.isBefore(now) -> StatusInfo(
                    text = "${itemView.context.getString(R.string.status_overdue)} • ${task.status}",
                    textColor = R.color.high_priority
                )
                task.status == "IN_PROGRESS" -> StatusInfo(
                    text = itemView.context.getString(R.string.status_in_progress),
                    textColor = R.color.medium_priority
                )
                else -> StatusInfo(
                    text = itemView.context.getString(R.string.status_pending),
                    textColor = R.color.ink
                )
            }
        }

        private fun calculateProgress(task: TaskEntity): ProgressInfo {
            if (task.completed) {
                // Completed tasks show green with 100% progress
                return ProgressInfo(
                    progress = 100,
                    color = R.color.low_priority // Green
                )
            }

            val dueAt = task.dueAt ?: return ProgressInfo(0, R.color.medium_priority) // Yellow for no due date
            val createdAt = task.createdAt
            val now = LocalDateTime.now()

            // Calculate total time from creation to due
            val totalMinutes = ChronoUnit.MINUTES.between(createdAt, dueAt)
            if (totalMinutes <= 0) {
                return ProgressInfo(100, R.color.high_priority) // Red for overdue
            }

            // Calculate elapsed time
            val elapsedMinutes = ChronoUnit.MINUTES.between(createdAt, now)
            val progress = ((elapsedMinutes.toFloat() / totalMinutes.toFloat()) * 100).toInt().coerceIn(0, 100)

            // Calculate remaining hours
            val hoursRemaining = ChronoUnit.HOURS.between(now, dueAt)

            // Color based on status and urgency:
            // - Pending/In-Progress: Yellow (medium_priority)
            // - Overdue: Red (high_priority)
            val color = when {
                now.isAfter(dueAt) -> R.color.high_priority // Overdue = red
                else -> R.color.medium_priority // Pending/In-Progress = yellow
            }

            return ProgressInfo(progress, color)
        }
    }

    // Data classes moved outside of inner class
    private data class StatusInfo(val text: String, val textColor: Int)
    private data class ProgressInfo(val progress: Int, val color: Int)

    class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
        override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
            return oldItem == newItem
        }
    }
}
