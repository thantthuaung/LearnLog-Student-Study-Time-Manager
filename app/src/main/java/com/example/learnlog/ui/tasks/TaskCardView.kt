package com.example.learnlog.ui.tasks

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.example.learnlog.R
import com.google.android.material.card.MaterialCardView
import android.widget.TextView

class TaskCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr) {
    private val titleView: TextView
    private val subjectView: TextView
    private val dueView: TextView
    private val statusView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.item_task_card, this, true)
        titleView = findViewById(R.id.task_title)
        subjectView = findViewById(R.id.task_subject)
        dueView = findViewById(R.id.task_due_date)
        statusView = findViewById(R.id.task_status)
        radius = 16f
        elevation = 4f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 0
            bottomMargin = 16
        }
    }

    fun bind(task: TaskItem) {
        titleView.text = task.title
        subjectView.text = task.subject
        dueView.text = "Due: " + task.dueDateTime.toString()
        statusView.text = task.status.name
    }
}
