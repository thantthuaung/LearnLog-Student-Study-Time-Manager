package com.example.learnlog.ui.notes

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.Note
import com.example.learnlog.databinding.ItemNoteBinding
import com.google.android.material.color.MaterialColors
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

class NotesAdapter(
    private val onClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback) {

    private val thisYear = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val otherYear = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    private val time = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        NoteViewHolder(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) = holder.bind(getItem(position))

    inner class NoteViewHolder(private val b: ItemNoteBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(note: Note) {
            val ctx = b.root.context
            b.textViewTitle.text = note.title
            b.textViewContent.text = note.content.trim()
            b.textViewContent.isVisible = note.content.isNotBlank()

            val updated = note.updatedAt
            b.textViewDate.text = if (updated.toLocalDate() == LocalDate.now()) updated.format(time)
            else updated.format(if (updated.year == LocalDate.now().year) thisYear else otherYear)

            b.iconPin.setImageResource(if (note.isPinned) R.drawable.ic_pin_filled else R.drawable.ic_pin_outlined)
            b.iconPin.imageTintList = ColorStateList.valueOf(
                if (note.isPinned) ContextCompat.getColor(ctx, R.color.ll_primary)
                else MaterialColors.getColor(b.iconPin, com.google.android.material.R.attr.colorOutline)
            )
            b.iconPin.contentDescription = ctx.getString(if (note.isPinned) R.string.note_unpin else R.string.note_pin)
            b.iconPin.setOnClickListener { onPinClick(note) }

            // Pinned notes get a soft tint so they stand out in the grid
            b.noteCard.setCardBackgroundColor(
                ContextCompat.getColor(ctx, if (note.isPinned) R.color.ll_surface_low else R.color.ll_surface)
            )
            b.noteCard.strokeColor = ContextCompat.getColor(ctx, if (note.isPinned) R.color.ll_primary_container else R.color.ll_outline_variant)

            b.tagsGroup.removeAllViews()
            b.tagsGroup.isVisible = note.tags.isNotEmpty()
            note.tags.take(3).forEach { tag ->
                val pill = LayoutInflater.from(ctx).inflate(R.layout.view_tag_pill, b.tagsGroup, false) as TextView
                pill.text = "#$tag"
                b.tagsGroup.addView(pill)
            }
            b.root.setOnClickListener { onClick(note) }
        }
    }
}

object NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
}
