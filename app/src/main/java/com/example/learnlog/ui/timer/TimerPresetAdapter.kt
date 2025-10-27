package com.example.learnlog.ui.timer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.data.model.TimerPreset
import com.example.learnlog.databinding.ItemTimerPresetBinding

class TimerPresetAdapter(
    private val onPresetClick: (TimerPreset) -> Unit,
    private val onPresetLongClick: (TimerPreset) -> Boolean = { false }
) : ListAdapter<TimerPreset, TimerPresetAdapter.PresetViewHolder>(PresetDiffCallback()) {

    private var selectedIndex = -1

    fun setSelectedIndex(index: Int) {
        val oldIndex = selectedIndex
        selectedIndex = index
        if (oldIndex != -1) notifyItemChanged(oldIndex)
        if (selectedIndex != -1 && selectedIndex < itemCount) notifyItemChanged(selectedIndex)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val binding = ItemTimerPresetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PresetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PresetViewHolder(
        private val binding: ItemTimerPresetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(preset: TimerPreset) {
            binding.presetLabel.text = preset.label

            // Visual selection indicator
            val isSelected = adapterPosition == selectedIndex

            if (isSelected) {
                binding.presetCard.strokeWidth = 4
                binding.presetCard.strokeColor = binding.root.context.getColor(R.color.nav_blue)
                binding.presetCard.cardElevation = 8f
            } else {
                binding.presetCard.strokeWidth = 2
                binding.presetCard.strokeColor = binding.root.context.getColor(R.color.light_gray)
                binding.presetCard.cardElevation = 3f
            }

            binding.root.setOnClickListener { onPresetClick(preset) }
            binding.root.setOnLongClickListener { onPresetLongClick(preset) }
        }
    }

    private class PresetDiffCallback : DiffUtil.ItemCallback<TimerPreset>() {
        override fun areItemsTheSame(oldItem: TimerPreset, newItem: TimerPreset): Boolean {
            return oldItem.durationMinutes == newItem.durationMinutes
        }

        override fun areContentsTheSame(oldItem: TimerPreset, newItem: TimerPreset): Boolean {
            return oldItem == newItem
        }
    }
}

