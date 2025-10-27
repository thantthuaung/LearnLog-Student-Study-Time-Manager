package com.example.learnlog.ui.planner

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.google.android.material.card.MaterialCardView

class CalendarAdapter(
    private val onDateClick: (CalendarDay) -> Unit
) : ListAdapter<CalendarDay, CalendarAdapter.CalendarViewHolder>(CalendarDayDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardDayCell: MaterialCardView = itemView.findViewById(R.id.cardDayCell)
        private val textDayNumber: TextView = itemView.findViewById(R.id.textDayNumber)
        private val taskIndicatorDots: LinearLayout = itemView.findViewById(R.id.taskIndicatorDots)
        private val overdueIndicator: View = itemView.findViewById(R.id.overdueIndicator)

        fun bind(day: CalendarDay) {
            textDayNumber.text = day.date.dayOfMonth.toString()

            // Set text color based on month
            if (!day.isCurrentMonth) {
                textDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_hint))
                textDayNumber.alpha = 0.4f
            } else {
                textDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
                textDayNumber.alpha = 1f
            }

            // Highlight today
            if (day.isToday) {
                textDayNumber.setTypeface(null, Typeface.BOLD)
                cardDayCell.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.light_blue_background)
                )
            } else {
                textDayNumber.setTypeface(null, Typeface.NORMAL)
                cardDayCell.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.transparent)
                )
            }

            // Highlight selected date
            if (day.isSelected) {
                cardDayCell.strokeWidth = 3
                cardDayCell.strokeColor = ContextCompat.getColor(itemView.context, R.color.nav_blue)
            } else {
                cardDayCell.strokeWidth = 0
            }

            // Show task indicator dots
            taskIndicatorDots.removeAllViews()
            if (day.taskCount > 0) {
                val maxDots = minOf(day.taskCount, 3)
                for (i in 0 until maxDots) {
                    val dot = View(itemView.context)
                    val size = itemView.context.resources.getDimensionPixelSize(R.dimen.task_indicator_dot_size)
                    val params = LinearLayout.LayoutParams(size, size)
                    params.setMargins(2, 0, 2, 0)
                    dot.layoutParams = params

                    // Color code dots
                    val dotColor = when {
                        day.overdueCount > 0 && i == 0 -> R.color.high_priority // Red for overdue
                        day.completedCount == day.taskCount -> R.color.text_hint // Gray for all completed
                        day.hasHighPriority && i == 0 -> R.color.high_priority // Red for high priority
                        else -> R.color.nav_blue // Blue for normal tasks
                    }
                    dot.setBackgroundResource(R.drawable.task_indicator_dot)
                    dot.backgroundTintList = ContextCompat.getColorStateList(itemView.context, dotColor)

                    taskIndicatorDots.addView(dot)
                }
            }

            // Show overdue indicator
            overdueIndicator.visibility = if (day.overdueCount > 0) View.VISIBLE else View.GONE

            // Click listener
            cardDayCell.setOnClickListener {
                onDateClick(day)
            }

            // Accessibility
            cardDayCell.contentDescription = buildContentDescription(day)
        }

        private fun buildContentDescription(day: CalendarDay): String {
            val dateStr = day.date.toString()
            return when {
                day.taskCount == 0 -> "$dateStr, no tasks"
                day.overdueCount > 0 -> "$dateStr, ${day.taskCount} tasks, ${day.overdueCount} overdue"
                else -> "$dateStr, ${day.taskCount} tasks"
            }
        }
    }

    class CalendarDayDiffCallback : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay): Boolean {
            return oldItem == newItem
        }
    }
}

