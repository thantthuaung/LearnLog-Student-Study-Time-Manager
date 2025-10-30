package com.example.learnlog.ui.tasks

import android.content.Context
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.ColorDrawable
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R

class TaskSwipeCallback(
    private val context: Context,
    private val onSwipeLeft: (Int) -> Unit,
    private val onSwipeRight: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

    private val completeBackground = ColorDrawable(ContextCompat.getColor(context, R.color.success_green))
    private val deleteBackground = ColorDrawable(ContextCompat.getColor(context, R.color.error_red))
    private val completeIcon = ContextCompat.getDrawable(context, R.drawable.ic_check)
    private val deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete)

    init {
        completeIcon?.colorFilter = PorterDuffColorFilter(
            ContextCompat.getColor(context, android.R.color.white),
            PorterDuff.Mode.SRC_IN
        )
        deleteIcon?.colorFilter = PorterDuffColorFilter(
            ContextCompat.getColor(context, android.R.color.white),
            PorterDuff.Mode.SRC_IN
        )
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        when (direction) {
            ItemTouchHelper.RIGHT -> onSwipeRight(position) // Mark as complete
            ItemTouchHelper.LEFT -> onSwipeLeft(position)   // Delete task
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val itemView = viewHolder.itemView
        val iconMargin = (itemView.height - (completeIcon?.intrinsicHeight ?: 0)) / 2
        val iconTop = itemView.top + (itemView.height - (completeIcon?.intrinsicHeight ?: 0)) / 2
        val iconBottom = iconTop + (completeIcon?.intrinsicHeight ?: 0)

        when {
            // Swipe right - Complete
            dX > 0 -> {
                completeBackground.setBounds(
                    itemView.left,
                    itemView.top,
                    itemView.left + dX.toInt(),
                    itemView.bottom
                )
                completeBackground.draw(c)

                completeIcon?.let { icon ->
                    val iconLeft = itemView.left + iconMargin
                    val iconRight = itemView.left + iconMargin + icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }
            }
            // Swipe left - Delete
            dX < 0 -> {
                deleteBackground.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                deleteBackground.draw(c)

                deleteIcon?.let { icon ->
                    val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                    val iconRight = itemView.right - iconMargin
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
    }
}
