package com.example.learnlog.ui.planner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.model.StudySession
import com.example.learnlog.data.model.SessionStatus
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudySessionAdapter(
    private val onTimerClick: (StudySession) -> Unit,
    private val onRescheduleClick: (StudySession) -> Unit
) : RecyclerView.Adapter<StudySessionAdapter.ViewHolder>() {

    private var sessions: List<StudySession> = emptyList()

    fun submitList(newSessions: List<StudySession>) {
        sessions = newSessions
        notifyDataSetChanged()
    }

    fun getTaskAt(position: Int): StudySession = sessions[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_study_session, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sessions[position])
    }

    override fun getItemCount(): Int = sessions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val subjectText: TextView = itemView.findViewById(R.id.textSessionSubject)
        private val typeText: TextView = itemView.findViewById(R.id.textSessionType)
        private val timeText: TextView = itemView.findViewById(R.id.textSessionTime)
        private val statusText: TextView = itemView.findViewById(R.id.textSessionStatus)
        private val startTimerButton: View = itemView.findViewById(R.id.btnStartTimerSession)
        private val completeButton: View = itemView.findViewById(R.id.btnCompleteSession)
        private val skipButton: View = itemView.findViewById(R.id.btnSkipSession)

        fun bind(session: StudySession) {
            subjectText.text = session.subject
            typeText.text = session.type.name
            // Format time as "HH:mm - HH:mm"
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val start = session.startTime
            val end = Date(start.time + session.duration * 60 * 1000)
            timeText.text = itemView.context.getString(R.string.session_time_format, timeFormat.format(start), timeFormat.format(end))
            statusText.text = session.status.name
            startTimerButton.setOnClickListener { onTimerClick(session) }
            // Optionally, set completeButton and skipButton click listeners if needed
            // completeButton.setOnClickListener { ... }
            // skipButton.setOnClickListener { ... }
            // Set background or other UI updates as needed
        }
    }
}
