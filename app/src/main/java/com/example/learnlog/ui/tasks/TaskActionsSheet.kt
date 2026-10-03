package com.example.learnlog.ui.tasks

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.setFragmentResult
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.databinding.SheetTaskActionsBinding
import com.example.learnlog.ui.common.Format
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.color.MaterialColors

/** Long-press menu for a task. Reports the chosen [Action] through [RESULT_KEY]. */
class TaskActionsSheet : BottomSheetDialogFragment() {

    enum class Action { TOGGLE_DONE, FOCUS, EDIT, RESCHEDULE, DUPLICATE, DELETE }

    private var _binding: SheetTaskActionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = SheetTaskActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        @Suppress("DEPRECATION")
        val task = requireArguments().getParcelable<TaskEntity>(ARG_TASK)!!
        binding.sheetTitle.text = task.title
        binding.sheetSubtitle.text = listOfNotNull(Format.due(requireContext(), task.dueAt), task.subject).joinToString(" · ")

        if (task.completed) {
            add(Action.TOGGLE_DONE, task, R.drawable.ic_restart, R.string.action_mark_not_done)
        } else {
            add(Action.TOGGLE_DONE, task, R.drawable.ic_check_circle, R.string.action_mark_done)
            add(Action.FOCUS, task, R.drawable.ic_play, R.string.action_start_focus)
        }
        add(Action.EDIT, task, R.drawable.ic_edit, R.string.action_edit)
        add(Action.RESCHEDULE, task, R.drawable.ic_event, R.string.action_reschedule)
        add(Action.DUPLICATE, task, R.drawable.ic_layers, R.string.action_duplicate)
        add(Action.DELETE, task, R.drawable.ic_delete, R.string.action_delete, destructive = true)
    }

    private fun add(action: Action, task: TaskEntity, @DrawableRes icon: Int, @StringRes label: Int, destructive: Boolean = false) {
        val row = layoutInflater.inflate(R.layout.item_sheet_action, binding.sheetActions, false) as TextView
        row.setText(label)
        row.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0)
        val color = if (destructive) ContextCompat.getColor(requireContext(), R.color.ll_error)
        else MaterialColors.getColor(row, com.google.android.material.R.attr.colorOnSurfaceVariant)
        TextViewCompat.setCompoundDrawableTintList(row, ColorStateList.valueOf(color))
        if (destructive) row.setTextColor(color)
        row.setOnClickListener {
            setFragmentResult(RESULT_KEY, bundleOf(RESULT_ACTION to action.name, RESULT_TASK to task))
            dismiss()
        }
        binding.sheetActions.addView(row)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TaskActionsSheet"
        const val RESULT_KEY = "task_actions_result"
        const val RESULT_ACTION = "action"
        const val RESULT_TASK = "task"
        private const val ARG_TASK = "task"

        fun newInstance(task: TaskEntity) = TaskActionsSheet().apply {
            arguments = bundleOf(ARG_TASK to task)
        }
    }
}
