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

            // Highlight today with prominent ring and blue fill
            if (day.isToday) {
                textDayNumber.setTypeface(null, Typeface.BOLD)
                textDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.nav_blue))
                cardDayCell.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.light_blue_background)
                )
                cardDayCell.strokeWidth = 4
                cardDayCell.strokeColor = ContextCompat.getColor(itemView.context, R.color.nav_blue)
            } else {
                textDayNumber.setTypeface(null, Typeface.NORMAL)
                cardDayCell.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, android.R.color.transparent)
                )
                cardDayCell.strokeWidth = 0
            }

            // Highlight selected date (override today if selected)
            if (day.isSelected && !day.isToday) {
                cardDayCell.strokeWidth = 3
                cardDayCell.strokeColor = ContextCompat.getColor(itemView.context, R.color.nav_blue)
                textDayNumber.setTextColor(ContextCompat.getColor(itemView.context, R.color.nav_blue))
                textDayNumber.setTypeface(null, Typeface.BOLD)
            }

            // Show task indicator dots with status-based colors
            taskIndicatorDots.removeAllViews()
            if (day.taskCount > 0) {
                val statusCounts = mapOf(
                    "OVERDUE" to day.overdueCount,
                    "IN_PROGRESS" to day.inProgressCount,
                    "COMPLETED" to day.completedCount,
                    "PENDING" to (day.taskCount - day.overdueCount - day.inProgressCount - day.completedCount)
                )

                // Show up to 3 dots representing different statuses
                var dotsAdded = 0
                val maxDots = 3

                // Priority: Overdue > In Progress > Pending > Completed
                if (statusCounts["OVERDUE"]!! > 0 && dotsAdded < maxDots) {
                    addStatusDot(R.drawable.status_dot_overdue)
                    dotsAdded++
                }
                if (statusCounts["IN_PROGRESS"]!! > 0 && dotsAdded < maxDots) {
                    addStatusDot(R.drawable.status_dot_in_progress)
                    dotsAdded++
                }
                if (statusCounts["PENDING"]!! > 0 && dotsAdded < maxDots) {
                    addStatusDot(R.drawable.status_dot_pending)
                    dotsAdded++
                }
                if (statusCounts["COMPLETED"]!! > 0 && dotsAdded < maxDots) {
                    addStatusDot(R.drawable.status_dot_completed)
                    dotsAdded++
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

        private fun addStatusDot(drawableRes: Int) {
            val dot = View(itemView.context)
            val size = 8 // 8dp
            val sizeInPx = (size * itemView.context.resources.displayMetrics.density).toInt()
            val params = LinearLayout.LayoutParams(sizeInPx, sizeInPx)
            params.setMargins(2, 0, 2, 0)
            dot.layoutParams = params
            dot.setBackgroundResource(drawableRes)
            taskIndicatorDots.addView(dot)
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

