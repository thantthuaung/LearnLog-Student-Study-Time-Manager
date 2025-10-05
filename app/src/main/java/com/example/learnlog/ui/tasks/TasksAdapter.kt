package com.example.learnlog.ui.tasks

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import java.time.format.DateTimeFormatter

class TasksAdapter(
    private val items: List<TaskItem>,
    private val onEdit: (TaskItem) -> Unit,
    private val onDelete: (TaskItem) -> Unit,
    private val onMarkComplete: (TaskItem) -> Unit,
    private val onStartTimer: (TaskItem) -> Unit
) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {
    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.cardTask)
        val title: TextView = itemView.findViewById(R.id.textTaskTitle)
        val subject: TextView = itemView.findViewById(R.id.textTaskSubject)
        val due: TextView = itemView.findViewById(R.id.textTaskDue)
        val priority: TextView = itemView.findViewById(R.id.textTaskPriority)
        val type: TextView = itemView.findViewById(R.id.textTaskType)
        val status: TextView = itemView.findViewById(R.id.textTaskStatus)
        val progress: ProgressBar = itemView.findViewById(R.id.progressTask)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEditTask)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteTask)
        val btnMarkComplete: ImageButton = itemView.findViewById(R.id.btnMarkCompleteTask)
        val btnStartTimer: ImageButton = itemView.findViewById(R.id.btnStartTimerTask)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subject.text = item.subject
        holder.due.text = item.dueDateTime.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
        holder.priority.text = item.priority.name
        holder.type.text = item.type.name
        holder.status.text = item.status.name
        holder.progress.progress = item.progress
        // Color code by priority
        holder.priority.setTextColor(
            when (item.priority) {
                TaskPriority.HIGH -> Color.RED
                TaskPriority.MEDIUM -> Color.parseColor("#FFA500") // Orange
                TaskPriority.LOW -> Color.GREEN
            }
        )
        // Button actions
        holder.btnEdit.setOnClickListener { onEdit(item) }
        holder.btnDelete.setOnClickListener { onDelete(item) }
        holder.btnMarkComplete.setOnClickListener { onMarkComplete(item) }
        holder.btnStartTimer.setOnClickListener { onStartTimer(item) }
    }

    override fun getItemCount() = items.size
}
