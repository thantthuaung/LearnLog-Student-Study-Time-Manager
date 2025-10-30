package com.example.learnlog.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.databinding.HeaderTasksBinding

class HeaderAdapter(
    private val onSearchTextChanged: (String) -> Unit
) : RecyclerView.Adapter<HeaderAdapter.HeaderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
        val binding = HeaderTasksBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HeaderViewHolder(binding, onSearchTextChanged)
    }

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {}
    override fun getItemCount(): Int = 1

    class HeaderViewHolder(
        binding: HeaderTasksBinding,
        onSearchTextChanged: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.searchInput.doAfterTextChanged { text ->
                onSearchTextChanged(text?.toString() ?: "")
            }
        }
    }
}

