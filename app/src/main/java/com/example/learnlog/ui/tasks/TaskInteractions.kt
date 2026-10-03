package com.example.learnlog.ui.tasks

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.R
import com.example.learnlog.data.entity.TaskEntity
import com.example.learnlog.data.repository.TaskRepository
import com.example.learnlog.timer.TimerAlerts
import com.example.learnlog.ui.planner.RescheduleTaskBottomSheet
import com.example.learnlog.ui.timer.TaskTimerBottomSheet
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/** Task writes shared by the Tasks and Planner screens. */
@HiltViewModel
class TaskOpsViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {
    fun setCompleted(task: TaskEntity, done: Boolean) = viewModelScope.launch { repository.updateTask(task.withCompleted(done)) }
    fun delete(task: TaskEntity) = viewModelScope.launch { repository.deleteTask(task.id) }
    fun restore(task: TaskEntity) = viewModelScope.launch { repository.insertTask(task) }
    fun duplicate(task: TaskEntity) = viewModelScope.launch {
        repository.saveTask(task.copy(id = 0, title = "${task.title} (copy)").withCompleted(false))
    }
    fun reschedule(task: TaskEntity, due: LocalDateTime) = viewModelScope.launch {
        repository.updateTask(task.copy(dueAt = due))
    }
}

/**
 * Everything that happens when a user interacts with a task card: open the editor or the
 * actions sheet, complete with a celebration, delete with undo, reschedule, start focusing.
 * Call [register] from onViewCreated.
 */
class TaskInteractions(
    private val fragment: Fragment,
    private val ops: TaskOpsViewModel,
    /** View the snackbar should sit above (e.g. the FAB). */
    private val snackbarAnchor: () -> View?
) {
    private val context get() = fragment.requireContext()
    private val fm get() = fragment.childFragmentManager

    fun register() {
        val owner = fragment.viewLifecycleOwner
        fm.setFragmentResultListener(TaskActionsSheet.RESULT_KEY, owner) { _, bundle -> onAction(bundle) }
        fm.setFragmentResultListener(AddEditTaskBottomSheet.RESULT_KEY, owner) { _, bundle -> onEdited(bundle) }
    }

    fun create(template: TaskEntity? = null) {
        if (fm.findFragmentByTag(AddEditTaskBottomSheet.TAG) != null) return
        AddEditTaskBottomSheet.newInstance(template).show(fm, AddEditTaskBottomSheet.TAG)
    }

    fun edit(task: TaskEntity) = create(task)

    fun showActions(task: TaskEntity) {
        if (fm.findFragmentByTag(TaskActionsSheet.TAG) != null) return
        TaskActionsSheet.newInstance(task).show(fm, TaskActionsSheet.TAG)
    }

    fun setCompleted(task: TaskEntity, done: Boolean) {
        if (task.completed == done) return
        ops.setCompleted(task, done)
        if (done) TimerAlerts.tick(context)
        snack(if (done) R.string.task_completed_toast else R.string.task_reopened) { ops.restore(task) }
    }

    fun delete(task: TaskEntity) {
        ops.delete(task)
        snack(R.string.task_deleted) { ops.restore(task) }
    }

    fun focus(task: TaskEntity) {
        if (fm.findFragmentByTag(TaskTimerBottomSheet.TAG) != null) return
        TaskTimerBottomSheet.newInstance(task).show(fm, TaskTimerBottomSheet.TAG)
    }

    fun reschedule(task: TaskEntity) {
        RescheduleTaskBottomSheet.newInstance(task) { date, time ->
            val due = date.atTime(time)
            ops.reschedule(task, due)
            val label = due.format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()))
            snack(fragment.getString(R.string.task_rescheduled, label))
        }.show(fm, "reschedule")
    }

    private fun onAction(bundle: Bundle) {
        @Suppress("DEPRECATION")
        val task = bundle.getParcelable<TaskEntity>(TaskActionsSheet.RESULT_TASK) ?: return
        when (TaskActionsSheet.Action.valueOf(bundle.getString(TaskActionsSheet.RESULT_ACTION)!!)) {
            TaskActionsSheet.Action.TOGGLE_DONE -> setCompleted(task, !task.completed)
            TaskActionsSheet.Action.FOCUS -> focus(task)
            TaskActionsSheet.Action.EDIT -> edit(task)
            TaskActionsSheet.Action.RESCHEDULE -> reschedule(task)
            TaskActionsSheet.Action.DUPLICATE -> {
                ops.duplicate(task)
                snack(R.string.task_duplicated)
            }
            TaskActionsSheet.Action.DELETE -> delete(task)
        }
    }

    private fun onEdited(bundle: Bundle) {
        @Suppress("DEPRECATION")
        val deleted = bundle.getParcelable<TaskEntity>(AddEditTaskBottomSheet.RESULT_DELETED)
        when {
            deleted != null -> snack(R.string.task_deleted) { ops.restore(deleted) }
            bundle.getBoolean(AddEditTaskBottomSheet.RESULT_IS_NEW) -> snack(R.string.task_added)
            else -> snack(R.string.task_saved)
        }
    }

    private fun snack(messageRes: Int, undo: (() -> Unit)? = null) = snack(fragment.getString(messageRes), undo)

    private fun snack(message: String, undo: (() -> Unit)? = null) {
        val root = fragment.view ?: return
        Snackbar.make(root, message, if (undo != null) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT).apply {
            undo?.let { action -> setAction(R.string.action_undo) { action() } }
            val anchor = snackbarAnchor()?.takeIf { it.isVisible }
                ?: fragment.activity?.findViewById<View>(R.id.bottom_nav_container)?.takeIf { it.isVisible }
            anchor?.let { anchorView = it }
        }.show()
    }
}
