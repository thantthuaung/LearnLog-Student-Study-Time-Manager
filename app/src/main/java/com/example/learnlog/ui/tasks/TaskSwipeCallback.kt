package com.example.learnlog.ui.tasks

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class TaskSwipeCallback(
    private val onSwipeLeft: (Int) -> Unit,
    private val onSwipeRight: (Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

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
}
