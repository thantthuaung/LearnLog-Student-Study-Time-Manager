package com.example.learnlog.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.model.TimerPreset
import com.example.learnlog.databinding.ItemTimerPresetEditBinding

class TimerPresetEditAdapter(
    private val onEdit: (TimerPreset, Int) -> Unit,
    private val onDelete: (TimerPreset, Int) -> Unit,
    private val onSetDefault: (Int) -> Unit
) : ListAdapter<TimerPreset, TimerPresetEditAdapter.ViewHolder>(PresetDiffCallback()) {

    private var defaultIndex: Int = 0

    fun setDefaultIndex(index: Int) {
        val oldIndex = defaultIndex
        defaultIndex = index
        notifyItemChanged(oldIndex)
        notifyItemChanged(index)
    }

    fun moveItem(from: Int, to: Int) {
        val list = currentList.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        submitList(list)

        // Adjust default index if needed
        when {
            from == defaultIndex -> defaultIndex = to
            from < defaultIndex && to >= defaultIndex -> defaultIndex--
            from > defaultIndex && to <= defaultIndex -> defaultIndex++
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTimerPresetEditBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position, position == defaultIndex)
    }

    inner class ViewHolder(
        private val binding: ItemTimerPresetEditBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(preset: TimerPreset, position: Int, isDefault: Boolean) {
            binding.tvPresetLabel.text = preset.label
            binding.radioDefault.isChecked = isDefault

            binding.radioDefault.setOnClickListener {
                onSetDefault(position)
            }

            binding.btnEdit.setOnClickListener {
                onEdit(preset, position)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(preset, position)
            }
        }
    }

    private class PresetDiffCallback : DiffUtil.ItemCallback<TimerPreset>() {
        override fun areItemsTheSame(oldItem: TimerPreset, newItem: TimerPreset): Boolean {
            return oldItem.durationMinutes == newItem.durationMinutes && oldItem.label == newItem.label
        }

        override fun areContentsTheSame(oldItem: TimerPreset, newItem: TimerPreset): Boolean {
            return oldItem == newItem
        }
    }
}

class TimerPresetTouchHelper(
    private val adapter: TimerPresetEditAdapter
) : ItemTouchHelper.SimpleCallback(
    ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    0
) {
    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val from = viewHolder.bindingAdapterPosition
        val to = target.bindingAdapterPosition
        adapter.moveItem(from, to)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // Not used
    }
}

