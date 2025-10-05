package com.example.learnlog.ui.tasks

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.model.Task
import com.example.learnlog.data.model.TaskPriority
import com.example.learnlog.data.model.TaskStatus
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class TaskAdapter(
    private val onTimerClick: (Task) -> Unit,
    private val onDetailsClick: (Task) -> Unit,
    private val onStatusChange: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var tasks: List<Task> = emptyList()

    fun submitList(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    fun getTaskAt(position: Int): Task = tasks[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount(): Int = tasks.size

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.textTaskTitle)
        private val subjectText: TextView = itemView.findViewById(R.id.textTaskSubject)
        private val dueDateText: TextView = itemView.findViewById(R.id.textTaskDue)
        private val statusText: TextView = itemView.findViewById(R.id.textTaskStatus)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressTask)
        private val priorityIndicator: View = itemView.findViewById(R.id.textTaskPriority)
        private val startTimerButton: MaterialButton = itemView.findViewById(R.id.btnStartTimerTask)
        private val viewDetailsButton: MaterialButton = itemView.findViewById(R.id.btnEditTask)

        private val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

        fun bind(task: Task) {
            titleText.text = task.title
            subjectText.text = task.subject
            dueDateText.text = "Due: ${dateFormat.format(task.dueDate)}"
            statusText.text = task.status.name
            progressBar.progress = task.progress

            // Set priority indicator color
            priorityIndicator.setBackgroundColor(getPriorityColor(task.priority))

            // Set status background and text color
            setupStatus(task.status)

            // Setup button clicks
            startTimerButton.setOnClickListener { onTimerClick(task) }
            viewDetailsButton.setOnClickListener { onDetailsClick(task) }

            // Setup swipe gesture for status change
            setupSwipeGesture(task)
        }

        private fun getPriorityColor(priority: TaskPriority): Int {
            return when (priority) {
                TaskPriority.HIGH -> Color.RED
                TaskPriority.MEDIUM -> Color.rgb(255, 165, 0) // Orange
                TaskPriority.LOW -> Color.GREEN
            }
        }

        private fun setupStatus(status: TaskStatus) {
            val (backgroundColor, textColor) = when (status) {
                TaskStatus.PENDING -> Color.LTGRAY to Color.BLACK
                TaskStatus.IN_PROGRESS -> Color.rgb(173, 216, 230) to Color.BLACK // Light blue
                TaskStatus.COMPLETED -> Color.rgb(144, 238, 144) to Color.BLACK // Light green
            }
            statusText.setBackgroundColor(backgroundColor)
            statusText.setTextColor(textColor)
        }

        private fun setupSwipeGesture(task: Task) {
            // TODO: Implement swipe gesture using ItemTouchHelper
        }
    }
}
