package com.example.learnlog.ui.insights

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.databinding.ItemTopTaskBinding

class TopTasksAdapter(
    private val onTaskClick: (TopTask) -> Unit
) : ListAdapter<TopTask, TopTasksAdapter.ViewHolder>(TopTaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTopTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onTaskClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemTopTaskBinding,
        private val onTaskClick: (TopTask) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(topTask: TopTask) {
            binding.taskTitle.text = topTask.title
            binding.subjectChip.text = topTask.subject ?: "General"

            val hours = topTask.totalMinutes / 60
            val mins = topTask.totalMinutes % 60
            binding.timeSpent.text = if (hours > 0) {
                "${hours}h ${mins}m"
            } else {
                "${mins}m"
            }

            // Progress is relative to the max in the list
            // The adapter will be told the max via another method
            binding.timeProgress.progress = 0

            binding.root.setOnClickListener {
                onTaskClick(topTask)
            }
        }

        fun bindWithMax(topTask: TopTask, maxMinutes: Int) {
            bind(topTask)
            val progress = if (maxMinutes > 0) {
                ((topTask.totalMinutes.toFloat() / maxMinutes) * 100).toInt()
            } else 0
            binding.timeProgress.progress = progress
        }
    }

    private class TopTaskDiffCallback : DiffUtil.ItemCallback<TopTask>() {
        override fun areItemsTheSame(oldItem: TopTask, newItem: TopTask): Boolean {
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: TopTask, newItem: TopTask): Boolean {
            return oldItem == newItem
        }
    }
}

