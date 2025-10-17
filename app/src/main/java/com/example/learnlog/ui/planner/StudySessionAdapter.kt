package com.example.learnlog.ui.planner

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.model.StudySession
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.absoluteValue

class StudySessionAdapter(
    private val onMarkComplete: (StudySession) -> Unit,
    private val onSkip: (StudySession) -> Unit,
    private val onStartTimer: (StudySession) -> Unit
) : RecyclerView.Adapter<StudySessionAdapter.SessionViewHolder>() {
    private var items: List<StudySession> = emptyList()

    fun submitList(newItems: List<StudySession>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_study_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val item = items[position]
        holder.subject.text = item.subject
        holder.type.text = item.type.name
        // Use SimpleDateFormat for API < 26 compatibility
        val startFormat = SimpleDateFormat("EEE, MMM d, HH:mm", Locale.getDefault())
        val endFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTime = org.threeten.bp.DateTimeUtils.toDate(item.startTime.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant())
        val endTime = org.threeten.bp.DateTimeUtils.toDate(item.endTime.atZone(org.threeten.bp.ZoneId.systemDefault()).toInstant())
        holder.time.text = holder.itemView.context.getString(
            R.string.session_time_format,
            startFormat.format(startTime),
            endFormat.format(endTime)
        )
        holder.status.text = item.status.name
        // Color-code by subject
        holder.card.setCardBackgroundColor(getColorForSubject(item.subject))
        holder.btnComplete.setOnClickListener { onMarkComplete(item) }
        holder.btnSkip.setOnClickListener { onSkip(item) }
        holder.btnStartTimer.setOnClickListener { onStartTimer(item) }
    }

    override fun getItemCount() = items.size

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: CardView = itemView.findViewById(R.id.cardSession)
        val subject: TextView = itemView.findViewById(R.id.textSessionSubject)
        val type: TextView = itemView.findViewById(R.id.textSessionType)
        val time: TextView = itemView.findViewById(R.id.textSessionTime)
        val status: TextView = itemView.findViewById(R.id.textSessionStatus)
        val btnComplete: ImageButton = itemView.findViewById(R.id.btnCompleteSession)
        val btnSkip: ImageButton = itemView.findViewById(R.id.btnSkipSession)
        val btnStartTimer: ImageButton = itemView.findViewById(R.id.btnStartTimerSession)
    }

    private fun getColorForSubject(subject: String): Int {
        // Simple hash to color for demo
        val colors = listOf(
            Color.parseColor("#BBDEFB"), // Blue
            Color.parseColor("#C8E6C9"), // Green
            Color.parseColor("#FFF9C4"), // Yellow
            Color.parseColor("#FFCDD2"), // Red
            Color.parseColor("#D1C4E9")  // Purple
        )
        return colors[subject.hashCode().absoluteValue % colors.size]
    }
}
