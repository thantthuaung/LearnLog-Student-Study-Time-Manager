package com.example.learnlog.ui.tasks

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.databinding.FiltersTasksBinding

class FilterAdapter(
    private val onFilterChanged: (TaskFilter) -> Unit,
    private val onSortClicked: (View) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = FiltersTasksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FilterViewHolder(binding, onFilterChanged, onSortClicked)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind()
    }

    override fun getItemCount(): Int = 1

    class FilterViewHolder(
        private val binding: FiltersTasksBinding,
        private val onFilterChanged: (TaskFilter) -> Unit,
        private val onSortClicked: (View) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.chipGroupFilter.setOnCheckedChangeListener { group, checkedId ->
                val filter = when (checkedId) {
                    R.id.chipDue -> TaskFilter.DUE
                    R.id.chipCompleted -> TaskFilter.COMPLETED
                    else -> TaskFilter.ALL
                }
                onFilterChanged(filter)
            }
            binding.chipSort.setOnClickListener {
                onSortClicked(it)
            }
        }
    }
}

