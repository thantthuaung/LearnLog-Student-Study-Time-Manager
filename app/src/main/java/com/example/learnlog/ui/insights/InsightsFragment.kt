package com.example.learnlog.ui.insights

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.util.Pair as AndroidPair
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentInsightsBinding
import com.example.learnlog.databinding.ItemLegendRowBinding
import com.example.learnlog.ui.base.BaseFragment
import com.example.learnlog.ui.common.Charts
import com.example.learnlog.ui.common.Format
import com.example.learnlog.ui.common.Motion
import com.example.learnlog.ui.common.Reselectable
import com.example.learnlog.ui.insights.compose.QuoteOfTheDayCard
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneOffset
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class InsightsFragment : BaseFragment(), Reselectable {

    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InsightsViewModel by viewModels()
    private lateinit var topTasksAdapter: TopTasksAdapter
    private var lastData: InsightsData? = null
    private var ignoreChipChange = false

    private val shortDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    private val dayLabel = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTopBar(
            binding.topBar.root,
            title = getString(R.string.page_insights_title),
            actionIcon = R.drawable.ic_analytics,
            actionDescription = getString(R.string.open_analytics)
        ) { openAnalytics() }

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { QuoteOfTheDayCard(viewModel.quote) }
        }

        setupRangeChips()
        Charts.stylePieChart(binding.subjectsPieChart)
        Charts.styleBarChart(binding.plannedVsActualChart)
        binding.plannedVsActualChart.legend.isEnabled = false // the summary row above names both series

        topTasksAdapter = TopTasksAdapter { findNavController().navigate(R.id.tasksFragment) }
        binding.topTasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.topTasksRecyclerView.adapter = topTasksAdapter
        binding.cardOpenAnalytics.setOnClickListener { openAnalytics() }

        if (savedInstanceState == null) {
            Motion.staggerIn(listOf(binding.hero, binding.tiles, binding.subjectCard, binding.plannedCard, binding.topTasksCard), startDelay = 30)
        }
        observe()
    }

    private fun openAnalytics() = findNavController().navigate(R.id.action_global_analytics)

    private fun setupRangeChips() {
        val chipFor = mapOf(
            DateRange.TODAY to R.id.chip_today, DateRange.WEEK to R.id.chip_week,
            DateRange.MONTH to R.id.chip_month, DateRange.CUSTOM to R.id.chip_custom
        )
        ignoreChipChange = true
        binding.rangeChips.check(chipFor.getValue(viewModel.selectedRange))
        ignoreChipChange = false

        binding.rangeChips.setOnCheckedStateChangeListener { _, ids ->
            if (ignoreChipChange) return@setOnCheckedStateChangeListener
            when (ids.firstOrNull()) {
                R.id.chip_today -> viewModel.setTimeRange(DateRange.TODAY)
                R.id.chip_week -> viewModel.setTimeRange(DateRange.WEEK)
                R.id.chip_month -> viewModel.setTimeRange(DateRange.MONTH)
                R.id.chip_custom -> if (viewModel.selectedRange != DateRange.CUSTOM) showDateRangePicker()
            }
        }
        // Tapping Custom again re-opens the picker
        binding.chipCustom.setOnClickListener { if (binding.chipCustom.isChecked) showDateRangePicker() }
    }

    private fun showDateRangePicker() {
        val previous = viewModel.selectedRange
        MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.insights_pick_range)
            .setSelection(AndroidPair(
                LocalDate.now().minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
                LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
            ))
            .build()
            .apply {
                addOnPositiveButtonClickListener { selection ->
                    val start = Instant.ofEpochMilli(selection.first).atZone(ZoneOffset.UTC).toLocalDate()
                    val end = Instant.ofEpochMilli(selection.second).atZone(ZoneOffset.UTC).toLocalDate()
                    viewModel.setCustomDateRange(start, end)
                }
                // Cancelled: go back to the previously selected range
                addOnNegativeButtonClickListener { restoreChip(previous) }
                addOnCancelListener { restoreChip(previous) }
            }
            .show(childFragmentManager, "range")
    }

    private fun restoreChip(range: DateRange) {
        if (range == DateRange.CUSTOM) return
        ignoreChipChange = true
        binding.rangeChips.check(
            when (range) {
                DateRange.TODAY -> R.id.chip_today
                DateRange.WEEK -> R.id.chip_week
                else -> R.id.chip_month
            }
        )
        ignoreChipChange = false
    }

    private fun observe() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.insightsData.collect { data -> if (data.isLoaded) render(data) }
            }
        }
    }

    private fun render(data: InsightsData) {
        val ctx = requireContext()
        val rangeLabel = when (data.dateRange) {
            DateRange.TODAY -> getString(R.string.insights_today)
            else -> if (data.rangeStart == data.rangeEnd) data.rangeStart.format(shortDate)
            else "${data.rangeStart.format(shortDate)} – ${data.rangeEnd.format(shortDate)}"
        }
        setTopBarSubtitle(binding.topBar.root, getString(R.string.insights_range_label, rangeLabel))

        // Hero
        Motion.countTo(binding.totalFocusTimeText, data.totalFocusMinutes) { Format.compact(it) }
        binding.heroEmpty.isVisible = data.totalFocusMinutes == 0
        binding.sessionCountText.text = data.sessionCount.toString()
        binding.avgSessionText.text = Format.compact(data.avgSessionMinutes)
        val adherence = if (data.plannedMinutes > 0) data.actualMinutes * 100 / data.plannedMinutes else null
        binding.adherenceHeroText.text = adherence?.let { "$it%" } ?: "–"

        // Tiles
        Motion.countTo(binding.streakText, data.currentStreak) { it.toString() }
        binding.bestStreakText.text = resources.getQuantityString(R.plurals.best_streak, data.bestStreak, data.bestStreak)
        Motion.countTo(binding.completionRateText, (data.completionRate * 100).toInt()) { "$it%" }
        binding.completionDetailsText.text = if (data.totalTasks > 0) getString(R.string.completion_detail, data.completedTasks, data.totalTasks)
        else getString(R.string.completion_none)

        renderSubjects(data)
        renderPlannedVsActual(data)

        binding.emptyStateTopTasks.isVisible = data.topTasks.isEmpty()
        binding.topTasksRecyclerView.isVisible = data.topTasks.isNotEmpty()
        topTasksAdapter.submitList(data.topTasks)

        lastData = data
    }

    private fun renderSubjects(data: InsightsData) {
        val ctx = requireContext()
        val empty = data.timeBySubject.isEmpty()
        binding.subjectContent.isVisible = !empty
        binding.emptyStateSubject.isVisible = empty
        if (empty) return

        val total = data.timeBySubject.sumOf { it.second }.coerceAtLeast(1)
        val colors = data.timeBySubject.mapIndexed { i, (name, _) ->
            if (name == "Other") ContextCompat.getColor(ctx, R.color.chart_other) else Charts.seriesColor(ctx, i)
        }
        val entries = data.timeBySubject.map { (name, minutes) -> PieEntry(minutes.toFloat(), name) }
        val set = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(false)
            sliceSpace = 2.5f  // surface gap between slices
            selectionShift = 4f
        }
        binding.subjectsPieChart.data = PieData(set)
        binding.subjectsPieChart.centerText = Format.compact(total)
        if (lastData?.timeBySubject != data.timeBySubject) binding.subjectsPieChart.animateY(700)
        binding.subjectsPieChart.invalidate()

        // Legend rows carry the labels and values (colour is never the only cue)
        val legend = binding.subjectLegend
        legend.removeAllViews()
        data.timeBySubject.forEachIndexed { i, (name, minutes) ->
            val row = ItemLegendRowBinding.inflate(layoutInflater, legend, false)
            row.legendDot.backgroundTintList = android.content.res.ColorStateList.valueOf(colors[i])
            row.legendLabel.text = name
            row.legendValue.text = Format.compact(minutes)
            row.legendShare.text = "${minutes * 100 / total}%"
            legend.addView(row.root)
        }
    }

    private fun renderPlannedVsActual(data: InsightsData) {
        val ctx = requireContext()
        binding.plannedTimeText.text = Format.compact(data.plannedMinutes)
        binding.actualTimeText.text = Format.compact(data.actualMinutes)
        binding.adherenceText.text = if (data.plannedMinutes > 0) "${data.actualMinutes * 100 / data.plannedMinutes}%" else "–"

        val days = data.plannedVsActualByDay.entries.toList()
        val hasData = days.any { it.value.plannedMinutes > 0 || it.value.actualMinutes > 0 }
        binding.plannedVsActualChart.isVisible = hasData
        binding.emptyPlanned.isVisible = !hasData
        if (!hasData) return

        val actual = days.mapIndexed { i, e -> BarEntry(i.toFloat(), e.value.actualMinutes.toFloat()) }
        val planned = days.mapIndexed { i, e -> BarEntry(i.toFloat(), e.value.plannedMinutes.toFloat()) }
        val actualSet = BarDataSet(actual, getString(R.string.actual)).apply {
            color = Charts.seriesColor(ctx, 0); setDrawValues(false); highLightAlpha = 40
        }
        val plannedSet = BarDataSet(planned, getString(R.string.planned)).apply {
            color = Charts.seriesColor(ctx, 1); setDrawValues(false); highLightAlpha = 40
        }
        val labels = days.map { it.key.format(if (days.size == 1) shortDate else dayLabel) }

        val chart = binding.plannedVsActualChart
        val groupSpace = 0.3f
        val barSpace = 0.04f
        val barWidth = (1f - groupSpace) / 2 - barSpace
        chart.data = BarData(actualSet, plannedSet).apply { this.barWidth = barWidth }
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            axisMinimum = 0f
            axisMaximum = days.size.toFloat()
            setCenterAxisLabels(true)
            setLabelCount(minOf(days.size, 7), false)
        }
        chart.groupBars(0f, groupSpace, barSpace)
        chart.marker = Charts.Marker(ctx) { i ->
            val e = days.getOrNull(i) ?: return@Marker "" to ""
            e.key.format(dayLabel) to "${getString(R.string.actual)} ${Format.compact(e.value.actualMinutes)} · ${getString(R.string.planned)} ${Format.compact(e.value.plannedMinutes)}"
        }.also { it.chartView = chart }
        if (lastData?.plannedVsActualByDay != data.plannedVsActualByDay) chart.animateY(600)
        chart.invalidate()
    }

    override fun onReselected() {
        _binding?.scroll?.smoothScrollTo(0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        lastData = null
    }
}
