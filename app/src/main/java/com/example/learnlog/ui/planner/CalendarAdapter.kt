package com.example.learnlog.ui.planner

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.databinding.ItemCalendarDayBinding
import com.google.android.material.color.MaterialColors
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

class CalendarAdapter(
    private val onDateClick: (CalendarDay) -> Unit
) : ListAdapter<CalendarDay, CalendarAdapter.DayVH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DayVH(ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: DayVH, position: Int) = holder.bind(getItem(position))

    inner class DayVH(private val b: ItemCalendarDayBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(day: CalendarDay) {
            val ctx = b.root.context
            val number = b.textDayNumber
            number.text = day.date.dayOfMonth.toString()

            when {
                day.isSelected -> {
                    number.setBackgroundResource(R.drawable.bg_calendar_selected)
                    number.setTextColor(ContextCompat.getColor(ctx, R.color.ll_on_primary))
                }
                day.isToday -> {
                    number.setBackgroundResource(R.drawable.bg_calendar_today)
                    number.setTextColor(ContextCompat.getColor(ctx, R.color.ll_primary))
                }
                else -> {
                    number.background = null
                    number.setTextColor(MaterialColors.getColor(number,
                        if (day.isCurrentMonth) com.google.android.material.R.attr.colorOnSurface
                        else com.google.android.material.R.attr.colorOutline))
                }
            }

            // Up to three dots: overdue, open, done
            val colors = buildList {
                if (day.overdueCount > 0) add(R.color.ll_error)
                if (day.taskCount - day.overdueCount - day.completedCount > 0) add(R.color.ll_primary)
                if (day.completedCount > 0) add(R.color.ll_tertiary)
            }
            listOf(b.dot1, b.dot2, b.dot3).forEachIndexed { i, dot ->
                val color = colors.getOrNull(i)
                dot.visibility = if (color != null) View.VISIBLE else View.GONE
                if (color != null) dot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, color))
            }
            b.dots.alpha = if (day.isCurrentMonth) 1f else 0.4f

            b.dayCell.setOnClickListener { onDateClick(day) }
            b.dayCell.contentDescription = buildString {
                append(day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)))
                if (day.taskCount > 0) append(", ").append(ctx.resources.getQuantityString(R.plurals.tasks_count, day.taskCount, day.taskCount))
                if (day.overdueCount > 0) append(", ").append(day.overdueCount).append(' ').append(ctx.getString(R.string.section_overdue).lowercase())
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<CalendarDay>() {
        override fun areItemsTheSame(oldItem: CalendarDay, newItem: CalendarDay) = oldItem.date == newItem.date
        override fun areContentsTheSame(oldItem: CalendarDay, newItem: CalendarDay) = oldItem == newItem
    }
}
