package com.example.learnlog.ui.tasks

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.ItemTaskBinding
import com.example.learnlog.databinding.ItemTaskHeaderBinding
import com.example.learnlog.ui.common.Format
import com.example.learnlog.ui.common.Motion
import com.google.android.material.color.MaterialColors
import org.threeten.bp.LocalDateTime

/** Task cards with optional section headers. Shared by the Tasks and Planner screens. */
class TaskListAdapter(
    private val onTaskClick: (TaskEntity) -> Unit,
    private val onTaskLongClick: (TaskEntity) -> Unit,
    private val onCheckedChange: (TaskEntity, Boolean) -> Unit,
    private val onStartFocus: (TaskEntity) -> Unit,
    /** Planner shows tasks of one day, so the date part of the due label is redundant there. */
    private val showTimeOnly: Boolean = false
) : ListAdapter<TaskRow, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is TaskRow.Header -> TYPE_HEADER
        is TaskRow.Item -> TYPE_TASK
    }

    override fun getItemId(position: Int): Long = getItem(position).key.hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) HeaderVH(ItemTaskHeaderBinding.inflate(inflater, parent, false))
        else TaskVH(ItemTaskBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is TaskRow.Header -> (holder as HeaderVH).bind(row)
            is TaskRow.Item -> (holder as TaskVH).bind(row.task)
        }
    }

    fun taskAt(position: Int): TaskEntity? = (currentList.getOrNull(position) as? TaskRow.Item)?.task

    class HeaderVH(private val b: ItemTaskHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: TaskRow.Header) {
            val ctx = b.root.context
            b.headerTitle.text = ctx.getString(
                when (row.section) {
                    TaskSection.OVERDUE -> R.string.section_overdue
                    TaskSection.TODAY -> R.string.section_today
                    TaskSection.TOMORROW -> R.string.section_tomorrow
                    TaskSection.UPCOMING -> R.string.section_upcoming
                    TaskSection.NO_DATE -> R.string.section_no_date
                    TaskSection.DONE -> R.string.section_done
                }
            )
            b.headerTitle.setTextColor(
                if (row.section == TaskSection.OVERDUE) ContextCompat.getColor(ctx, R.color.ll_error)
                else MaterialColors.getColor(b.headerTitle, com.google.android.material.R.attr.colorOnSurface)
            )
            b.headerCount.text = row.count.toString()
        }
    }

    inner class TaskVH(private val b: ItemTaskBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(task: TaskEntity) {
            val ctx = b.root.context
            val now = LocalDateTime.now()
            val overdue = task.isOverdue(now)

            b.textTitle.text = task.title
            b.textTitle.paintFlags = if (task.completed) b.textTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            else b.textTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            b.root.alpha = if (task.completed) 0.62f else 1f

            b.priorityBar.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, when (task.priority) {
                    TaskEntity.PRIORITY_HIGH -> R.color.priority_high
                    TaskEntity.PRIORITY_MEDIUM -> R.color.priority_medium
                    else -> R.color.priority_low
                })
            )

            // Due pill: red when overdue
            b.textDue.isVisible = task.dueAt != null || !showTimeOnly
            b.textDue.text = when {
                overdue -> ctx.getString(R.string.overdue_by, Format.ago(ctx, task.dueAt!!, now))
                showTimeOnly && task.dueAt != null -> Format.time(task.dueAt)
                else -> Format.due(ctx, task.dueAt, now)
            }
            val dueFg = if (overdue) ContextCompat.getColor(ctx, R.color.ll_on_error_container)
            else MaterialColors.getColor(b.textDue, com.google.android.material.R.attr.colorOnSurfaceVariant)
            val dueBg = if (overdue) ContextCompat.getColor(ctx, R.color.ll_error_container)
            else ContextCompat.getColor(ctx, R.color.ll_surface_container)
            b.textDue.setTextColor(dueFg)
            b.textDue.compoundDrawableTintList = ColorStateList.valueOf(dueFg)
            b.textDue.backgroundTintList = ColorStateList.valueOf(dueBg)

            b.textSubject.isVisible = !task.subject.isNullOrBlank()
            b.textSubject.text = task.subject
            b.textDuration.text = Format.compact(task.durationMinutes)
            b.textStatus.isVisible = !task.completed && task.status == TaskEntity.STATUS_IN_PROGRESS

            b.checkComplete.setOnCheckedChangeListener(null)
            b.checkComplete.isChecked = task.completed
            b.checkComplete.contentDescription = ctx.getString(
                if (task.completed) R.string.action_mark_not_done else R.string.action_mark_done
            )
            b.checkComplete.setOnCheckedChangeListener { view, checked ->
                if (checked) Motion.pop(view)
                onCheckedChange(task, checked)
            }

            b.btnStartTimer.isVisible = !task.completed
            b.btnStartTimer.setOnClickListener { onStartFocus(task) }
            b.root.setOnClickListener { onTaskClick(task) }
            b.root.setOnLongClickListener { onTaskLongClick(task); true }
        }
    }

    private object Diff : DiffUtil.ItemCallback<TaskRow>() {
        override fun areItemsTheSame(oldItem: TaskRow, newItem: TaskRow) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: TaskRow, newItem: TaskRow) = oldItem == newItem
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_TASK = 1
    }
}
