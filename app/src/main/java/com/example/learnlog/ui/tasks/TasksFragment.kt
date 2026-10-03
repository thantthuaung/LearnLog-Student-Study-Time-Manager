package com.example.learnlog.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentTasksBinding
import com.example.learnlog.ui.base.BaseFragment
import com.example.learnlog.ui.common.Format
import com.example.learnlog.ui.common.Motion
import com.example.learnlog.ui.common.Motion.animateFirstLayout
import com.example.learnlog.ui.common.Reselectable
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class TasksFragment : BaseFragment(), Reselectable {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TasksViewModel by viewModels()
    private val ops: TaskOpsViewModel by viewModels()
    private lateinit var interactions: TaskInteractions
    private lateinit var adapter: TaskListAdapter

    private val chipFilters by lazy {
        mapOf(
            R.id.chip_all to TaskFilter.ALL,
            R.id.chip_today to TaskFilter.TODAY,
            R.id.chip_upcoming to TaskFilter.UPCOMING,
            R.id.chip_overdue to TaskFilter.OVERDUE,
            R.id.chip_done to TaskFilter.DONE
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        interactions = TaskInteractions(this, ops) { binding.fabAddTask }
        interactions.register()

        setupTopBar(
            binding.topBar.root,
            title = getString(R.string.page_tasks_title),
            subtitle = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())),
            actionIcon = R.drawable.ic_sort,
            actionDescription = getString(R.string.sort_title)
        ) { showSortDialog() }

        binding.heroGreeting.text = getString(R.string.greeting_named, Format.greeting(requireContext()), firstName)
        setupList()
        setupSearchAndFilters()
        binding.fabAddTask.setOnClickListener { interactions.create() }
        binding.emptyState.emptyAction.setOnClickListener { interactions.create() }
        observe()

        if (savedInstanceState == null) Motion.staggerIn(listOf(binding.hero, binding.searchLayout), startDelay = 40)
    }

    private fun setupList() {
        adapter = TaskListAdapter(
            onTaskClick = interactions::edit,
            onTaskLongClick = interactions::showActions,
            onCheckedChange = interactions::setCompleted,
            onStartFocus = interactions::focus
        )
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter
        binding.recyclerViewTasks.itemAnimator?.changeDuration = 180
        ItemTouchHelper(
            TaskSwipeCallback(requireContext(), adapter,
                onSwipeComplete = { interactions.setCompleted(it, !it.completed) },
                onSwipeDelete = interactions::delete)
        ).attachToRecyclerView(binding.recyclerViewTasks)

        // Shrink the FAB to an icon while scrolling down; extend again near the top
        binding.recyclerViewTasks.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                if (dy > 8 && binding.fabAddTask.isExtended) binding.fabAddTask.shrink()
                else if ((dy < -8 || !rv.canScrollVertically(-1)) && !binding.fabAddTask.isExtended) binding.fabAddTask.extend()
            }
        })
    }

    private fun setupSearchAndFilters() {
        binding.searchInput.doAfterTextChanged { viewModel.setSearchQuery(it?.toString().orEmpty()) }
        binding.filterChips.setOnCheckedStateChangeListener { _, ids ->
            ids.firstOrNull()?.let { chipFilters[it] }?.let(viewModel::setFilter)
        }
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> if (state.loaded) render(state) }
            }
        }
    }

    private fun render(state: TasksUiState) {
        adapter.submitList(state.rows) {
            if (state.rows.isNotEmpty()) binding.recyclerViewTasks.animateFirstLayout()
        }

        // Filter chips: label with live counts and keep the selection in sync
        chipFilters.forEach { (id, filter) ->
            val chip = binding.root.findViewById<Chip>(id)
            val base = getString(
                when (filter) {
                    TaskFilter.ALL -> R.string.filter_all
                    TaskFilter.TODAY -> R.string.filter_today
                    TaskFilter.UPCOMING -> R.string.filter_upcoming
                    TaskFilter.OVERDUE -> R.string.filter_overdue
                    TaskFilter.DONE -> R.string.filter_done
                }
            )
            val count = state.counts[filter] ?: 0
            chip.text = if (count > 0) getString(R.string.filter_with_count, base, count) else base
            if (filter == state.filter && !chip.isChecked) chip.isChecked = true
        }

        // Hero card
        val percent = if (state.todayTotal > 0) state.todayDone * 100 / state.todayTotal else 0
        binding.heroProgressText.text = if (state.todayTotal > 0) {
            getString(R.string.today_progress, state.todayDone, state.todayTotal)
        } else getString(R.string.today_progress_none)
        binding.heroRing.setProgressCompat(if (state.todayTotal > 0) percent else 0, true)
        Motion.countTo(binding.heroPercent, percent) { "$it%" }
        binding.heroFocus.text = getString(R.string.today_focus, Format.minutes(requireContext(), state.todayFocusMinutes))

        // Empty states
        val empty = state.rows.isEmpty()
        binding.emptyContainer.isVisible = empty
        binding.recyclerViewTasks.isVisible = !empty
        if (empty) {
            val es = binding.emptyState
            when {
                !state.hasAnyTasks -> {
                    es.emptyIcon.setImageResource(R.drawable.ic_empty_tasks)
                    es.emptyTitle.setText(R.string.empty_tasks_title)
                    es.emptyMessage.setText(R.string.empty_tasks_message)
                    es.emptyAction.setText(R.string.tasks_new)
                    es.emptyAction.isVisible = true
                }
                state.isSearching -> {
                    es.emptyIcon.setImageResource(R.drawable.ic_search)
                    es.emptyTitle.setText(R.string.empty_search_title)
                    es.emptyMessage.setText(R.string.empty_search_message)
                    es.emptyAction.isVisible = false
                }
                else -> {
                    es.emptyIcon.setImageResource(R.drawable.ic_checklist)
                    es.emptyTitle.setText(R.string.empty_filter_title)
                    es.emptyMessage.setText(R.string.empty_filter_message)
                    es.emptyAction.isVisible = false
                }
            }
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(getString(R.string.sort_due), getString(R.string.sort_priority), getString(R.string.sort_title_az))
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.sort_title)
            .setSingleChoiceItems(options, viewModel.currentSort.ordinal) { dialog, which ->
                viewModel.setSort(TaskSort.values()[which])
                dialog.dismiss()
            }
            .show()
    }

    override fun onReselected() {
        _binding ?: return
        binding.recyclerViewTasks.smoothScrollToPosition(0)
        binding.appBar.setExpanded(true, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
