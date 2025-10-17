package com.example.learnlog.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.DialogAddTaskBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.threeten.bp.LocalDateTime
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditTaskBottomSheet : BottomSheetDialogFragment() {
    private val viewModel: TasksViewModel by activityViewModels()
    private var task: TaskEntity? = null
    private var _binding: DialogAddTaskBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            task = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_TASK, TaskEntity::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_TASK)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentTask = task
        if (currentTask != null) {
            binding.dialogTitle.text = getString(R.string.edit_task)
            binding.titleInput.setText(currentTask.title)
            binding.subjectInput.setText(currentTask.subject ?: "")
            // TODO: Set date and time from task.dueAt
            // TODO: Set priority from task.priority
            binding.descriptionInput.setText(currentTask.notes ?: "")
            binding.btnSave.text = getString(R.string.update_task)
        } else {
            binding.dialogTitle.text = getString(R.string.add_task)
            binding.btnSave.text = getString(R.string.save_task)
        }

        binding.btnSave.setOnClickListener {
            val title = binding.titleInput.text.toString()
            if (title.isNotBlank()) {
                val subject = binding.subjectInput.text.toString()
                val notes = binding.descriptionInput.text.toString()
                val priority = 0 // TODO: Get from priorityInput

                val now = LocalDateTime.now()
                val existingTask = currentTask
                val newTask = if (existingTask != null) {
                    existingTask.copy(
                        title = title,
                        subject = subject,
                        notes = notes,
                        priority = priority,
                        updatedAt = now
                    )
                } else {
                    TaskEntity(
                        title = title,
                        subject = subject,
                        dueAt = null, // TODO: Get from pickers
                        priority = priority,
                        status = "PENDING",
                        progress = 0,
                        completed = false,
                        notes = notes,
                        createdAt = now,
                        updatedAt = now,
                        type = "TASK" // Default type
                    )
                }

                if (existingTask == null) {
                    viewModel.saveNew(newTask)
                } else {
                    viewModel.updateExisting(newTask)
                }
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TASK = "task"

        fun newInstance(task: TaskEntity? = null) = AddEditTaskBottomSheet().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_TASK, task)
            }
        }
    }
}
