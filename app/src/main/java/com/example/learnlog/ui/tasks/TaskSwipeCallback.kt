package com.example.learnlog.ui.tasks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity

/**
 * Swipe right to toggle done, left to delete. Section headers can't be swiped.
 * Draws a rounded coloured backdrop with an icon that grows as the swipe progresses.
 */
class TaskSwipeCallback(
    context: Context,
    private val adapter: TaskListAdapter,
    private val onSwipeComplete: (TaskEntity) -> Unit,
    private val onSwipeDelete: (TaskEntity) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val density = context.resources.displayMetrics.density
    private val completePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.ll_tertiary) }
    private val deletePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ContextCompat.getColor(context, R.color.ll_error) }
    private val completeIcon = ContextCompat.getDrawable(context, R.drawable.ic_check)!!.mutate().apply { setTint(ContextCompat.getColor(context, R.color.ll_on_tertiary)) }
    private val reopenIcon = ContextCompat.getDrawable(context, R.drawable.ic_restart)!!.mutate().apply { setTint(ContextCompat.getColor(context, R.color.ll_on_tertiary)) }
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)!!.mutate().apply { setTint(ContextCompat.getColor(context, R.color.ll_on_error)) }
    private val rect = RectF()

    override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int =
        if (viewHolder.itemViewType == TaskListAdapter.TYPE_TASK) super.getSwipeDirs(recyclerView, viewHolder) else 0

    override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder) = 0.35f

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.bindingAdapterPosition
        val task = adapter.taskAt(position) ?: return
        if (direction == ItemTouchHelper.RIGHT) {
            // The list re-binds from the database; restore the row visually in the meantime
            adapter.notifyItemChanged(position)
            onSwipeComplete(task)
        } else {
            onSwipeDelete(task)
        }
    }

    override fun onChildDraw(
        c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        val item = viewHolder.itemView
        if (dX != 0f && actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            // Item bounds are the card itself (margins sit outside), so draw exactly beneath it
            val iconInset = 22 * density
            val radius = 18 * density
            val top = item.top.toFloat()
            val bottom = item.bottom.toFloat()
            val progress = (kotlin.math.abs(dX) / item.width).coerceIn(0f, 1f)
            val size = ((20 + 8 * (progress * 2.5f).coerceAtMost(1f)) * density).toInt()
            val cy = ((top + bottom) / 2).toInt()
            if (dX > 0) {
                rect.set(item.left.toFloat(), top, item.left + dX + radius, bottom)
                c.drawRoundRect(rect, radius, radius, completePaint)
                val task = adapter.taskAt(viewHolder.bindingAdapterPosition)
                val icon = if (task?.completed == true) reopenIcon else completeIcon
                val left = (item.left + iconInset).toInt()
                icon.setBounds(left, cy - size / 2, left + size, cy + size / 2)
                if (dX > iconInset + size) icon.draw(c)
            } else {
                rect.set(item.right + dX - radius, top, item.right.toFloat(), bottom)
                c.drawRoundRect(rect, radius, radius, deletePaint)
                val right = (item.right - iconInset).toInt()
                deleteIcon.setBounds(right - size, cy - size / 2, right, cy + size / 2)
                if (-dX > iconInset + size) deleteIcon.draw(c)
            }
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
