package com.example.learnlog.ui.tasks

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.ItemTaskCardBinding
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class TasksAdapter(
    private val onToggleComplete: (TaskEntity, Boolean) -> Unit,
    private val onStartTimer: (TaskEntity) -> Unit,
    private val onEdit: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TasksAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(task: TaskEntity) {
            binding.taskTitle.text = task.title
            binding.taskSubject.text = task.subject
            binding.taskSubject.isVisible = !task.subject.isNullOrBlank()

            if (task.dueAt != null) {
                binding.taskDueDate.text = task.dueAt.format(DateTimeFormatter.ofPattern("EEE, MMM d • HH:mm", Locale.getDefault()))
                binding.taskDueDate.isVisible = true
            } else {
                binding.taskDueDate.isVisible = false
            }

            binding.taskCompleted.isChecked = task.completed
            if (task.completed) {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.root.alpha = 0.6f
            } else {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.root.alpha = 1.0f
            }

            // TODO: Set priority indicator color
            // TODO: Set progress bar

            binding.taskCompleted.setOnCheckedChangeListener { _, isChecked ->
                onToggleComplete(task, isChecked)
            }
            binding.btnStartTimer.setOnClickListener { onStartTimer(task) }
            binding.btnEdit.setOnClickListener { onEdit(task) }
        }
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
    override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
        return oldItem == newItem
    }
}
