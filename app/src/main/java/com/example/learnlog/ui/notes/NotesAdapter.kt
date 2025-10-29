package com.example.learnlog.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.Note
import com.example.learnlog.databinding.ItemNoteBinding
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class NotesAdapter(
    private val onClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback) {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding, onClick, onPinClick, dateFormatter, timeFormatter)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onClick: (Note) -> Unit,
        private val onPinClick: (Note) -> Unit,
        private val dateFormatter: DateTimeFormatter,
        private val timeFormatter: DateTimeFormatter
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentNote: Note? = null

        init {
            itemView.setOnClickListener {
                currentNote?.let { onClick(it) }
            }

            // Find the pin icon in the view hierarchy
            itemView.findViewById<android.widget.ImageView>(R.id.iconPin)?.setOnClickListener {
                currentNote?.let { onPinClick(it) }
            }
        }

        fun bind(note: Note) {
            currentNote = note

            binding.textViewTitle.text = note.title
            binding.textViewContent.text = note.content.let { content ->
                if (content.length > 150) "${content.take(150)}..." else content
            }

            // Show date and time
            val dateText = note.updatedAt.format(dateFormatter)
            val timeText = note.updatedAt.format(timeFormatter)
            binding.textViewDate.text = "$dateText • $timeText"

            // Show pin icon
            val pinIcon = itemView.findViewById<android.widget.ImageView>(R.id.iconPin)
            pinIcon?.isVisible = true
            pinIcon?.setImageResource(
                if (note.isPinned) R.drawable.ic_pin_filled
                else R.drawable.ic_pin_outlined
            )

            // Show tags if present
            val tagsView = itemView.findViewById<android.widget.TextView>(R.id.text_view_tags)
            if (note.tags.isNotEmpty()) {
                tagsView?.isVisible = true
                tagsView?.text = note.tags.joinToString(" • ")
            } else {
                tagsView?.isVisible = false
            }
        }
    }
}

object NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
        return oldItem == newItem
    }
}
