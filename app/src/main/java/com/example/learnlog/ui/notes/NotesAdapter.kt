package com.example.learnlog.ui.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.data.Note
import com.example.learnlog.databinding.ItemNoteBinding
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class NotesAdapter(private val onClick: (Note) -> Unit) :
    ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback) {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding, onClick, dateFormatter)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)
    }

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onClick: (Note) -> Unit,
        private val dateFormatter: DateTimeFormatter
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentNote: Note? = null

        init {
            itemView.setOnClickListener {
                currentNote?.let {
                    onClick(it)
                }
            }
        }

        fun bind(note: Note) {
            currentNote = note
            binding.textViewTitle.text = note.title
            binding.textViewContent.text = note.content
            binding.textViewDate.text = note.createdAt.format(dateFormatter)
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
