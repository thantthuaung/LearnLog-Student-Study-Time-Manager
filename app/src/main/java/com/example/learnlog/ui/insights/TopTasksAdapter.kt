package com.example.learnlog.ui.insights

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.databinding.ItemTopTaskBinding
import com.example.learnlog.ui.common.Format

/** Ranked list of the tasks with the most focus time; bars are relative to the leader. */
class TopTasksAdapter(
    private val onTaskClick: (TopTask) -> Unit
) : ListAdapter<TopTask, TopTasksAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemTopTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val max = currentList.maxOfOrNull { it.totalMinutes } ?: 0
        holder.bind(getItem(position), position + 1, max)
    }

    inner class ViewHolder(private val b: ItemTopTaskBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(task: TopTask, rank: Int, maxMinutes: Int) {
            b.rank.text = rank.toString()
            b.taskTitle.text = task.title
            b.subjectChip.isVisible = !task.subject.isNullOrBlank()
            b.subjectChip.text = task.subject
            b.timeSpent.text = Format.compact(task.totalMinutes)
            b.timeProgress.setProgressCompat(if (maxMinutes > 0) task.totalMinutes * 100 / maxMinutes else 0, true)
            b.root.setOnClickListener { onTaskClick(task) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TopTask>() {
        override fun areItemsTheSame(oldItem: TopTask, newItem: TopTask) = oldItem.taskId == newItem.taskId
        override fun areContentsTheSame(oldItem: TopTask, newItem: TopTask) = oldItem == newItem
    }
}
