package com.example.learnlog.ui.analytics

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentAnalyticsBinding
import com.example.learnlog.databinding.ItemTopTaskBinding
import com.example.learnlog.ui.common.Charts
import com.example.learnlog.ui.common.Format
import com.example.learnlog.ui.common.Motion
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by viewModels()
    private var renderedDays = -1

    private val dayLabel = DateTimeFormatter.ofPattern("EEE d", Locale.getDefault())
    private val shortLabel = DateTimeFormatter.ofPattern("d/M", Locale.getDefault())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.rangeChips.check(
            when (viewModel.days) {
                30 -> R.id.chip_last_30_days
                90 -> R.id.chip_last_90_days
                else -> R.id.chip_last_7_days
            }
        )
        binding.rangeChips.setOnCheckedStateChangeListener { _, ids ->
            viewModel.setDays(
                when (ids.firstOrNull()) {
                    R.id.chip_last_30_days -> 30
                    R.id.chip_last_90_days -> 90
                    else -> 7
                }
            )
        }
        Charts.styleBarChart(binding.weeklyChart)
        if (savedInstanceState == null) {
            Motion.staggerIn(listOf(binding.tiles, binding.dailyCard, binding.subjectCard, binding.topTasksCard), startDelay = 60)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.analyticsData.collect { if (it.loaded) render(it) }
            }
        }
    }

    private fun render(data: AnalyticsData) {
        binding.emptyState.root.isVisible = data.isEmpty
        binding.contentLayout.isVisible = !data.isEmpty
        if (data.isEmpty) {
            binding.emptyState.emptyIcon.setImageResource(R.drawable.ic_empty_insights)
            binding.emptyState.emptyTitle.setText(R.string.empty_analytics_title)
            binding.emptyState.emptyMessage.setText(R.string.empty_analytics_message)
            binding.emptyState.emptyAction.isVisible = false
            return
        }

        Motion.countTo(binding.tvTotalHours, data.totalMinutes) { Format.compact(it) }
        Motion.countTo(binding.tvTotalSessions, data.sessionCount) { it.toString() }
        binding.tvAvgSession.text = Format.compact(data.averageMinutes)
        binding.tvBestDay.text = data.bestDay?.let { (date, minutes) ->
            "${getString(R.string.best_day)}: ${date.format(dayLabel)} · ${Format.compact(minutes)}"
        }.orEmpty()

        renderDaily(data)
        renderSubjects(data)
        renderTopTasks(data)
    }

    private fun renderDaily(data: AnalyticsData) {
        val ctx = requireContext()
        val entries = data.perDay.mapIndexed { i, (_, minutes) -> BarEntry(i.toFloat(), minutes.toFloat()) }
        val set = BarDataSet(entries, getString(R.string.daily_minutes)).apply {
            color = Charts.seriesColor(ctx, 0)
            setDrawValues(data.days == 7)
            valueTextColor = ContextCompat.getColor(ctx, R.color.chart_axis)
            valueTextSize = 10f
            valueTypeface = Charts.font(ctx)
            highLightAlpha = 40
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = if (value > 0) Format.compact(value.toInt()) else ""
            }
        }
        val chart = binding.weeklyChart
        chart.data = BarData(set).apply { barWidth = if (data.days == 7) 0.55f else 0.7f }
        chart.xAxis.apply {
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val date = data.perDay.getOrNull(value.toInt())?.first ?: return ""
                    return date.format(if (data.days == 7) dayLabel else shortLabel)
                }
            }
            setLabelCount(if (data.days == 7) 7 else 6, false)
        }
        chart.marker = Charts.Marker(ctx) { i ->
            val (date, minutes) = data.perDay.getOrNull(i) ?: return@Marker "" to ""
            date.format(dayLabel) to Format.minutes(ctx, minutes)
        }.also { it.chartView = chart }
        if (renderedDays != data.days) chart.animateY(600)
        renderedDays = data.days
        chart.invalidate()
    }

    private fun renderSubjects(data: AnalyticsData) {
        val ctx = requireContext()
        val rows = binding.subjectRows
        rows.removeAllViews()
        val max = data.subjects.maxOfOrNull { it.second } ?: 1
        data.subjects.forEachIndexed { i, (name, minutes) ->
            val row = ItemTopTaskBinding.inflate(layoutInflater, rows, false)
            row.rank.isVisible = false
            row.taskTitle.text = name
            row.subjectChip.text = "${minutes * 100 / data.totalMinutes.coerceAtLeast(1)}% of focus"
            row.timeSpent.text = Format.compact(minutes)
            row.timeProgress.setIndicatorColor(Charts.seriesColor(ctx, i))
            (row.timeProgress.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
            row.timeProgress.setProgressCompat(minutes * 100 / max, true)
            row.root.setPadding(0, row.root.paddingTop, 0, row.root.paddingBottom)
            rows.addView(row.root)
        }
    }

    private fun renderTopTasks(data: AnalyticsData) {
        val container = binding.topTasksContainer
        container.removeAllViews()
        binding.emptyTopTasks.isVisible = data.topTasks.isEmpty()
        val max = data.topTasks.maxOfOrNull { it.second } ?: 1
        data.topTasks.forEachIndexed { i, (title, minutes) ->
            val row = ItemTopTaskBinding.inflate(layoutInflater, container, false)
            row.rank.text = (i + 1).toString()
            row.taskTitle.text = title
            row.subjectChip.isVisible = false
            row.timeSpent.text = Format.compact(minutes)
            row.timeProgress.setProgressCompat(minutes * 100 / max, true)
            container.addView(row.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        renderedDays = -1
    }
}
