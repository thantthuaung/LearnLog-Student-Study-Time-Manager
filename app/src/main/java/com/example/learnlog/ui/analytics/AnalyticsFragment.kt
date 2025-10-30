package com.example.learnlog.ui.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentAnalyticsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {
    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupFilters()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupFilters() {
        binding.chipLast7Days.setOnClickListener {
            viewModel.setDateRange(AnalyticsViewModel.DateRange.LAST_7_DAYS)
        }
        binding.chipLast30Days.setOnClickListener {
            viewModel.setDateRange(AnalyticsViewModel.DateRange.LAST_30_DAYS)
        }
        binding.chipLast90Days.setOnClickListener {
            viewModel.setDateRange(AnalyticsViewModel.DateRange.LAST_90_DAYS)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.analyticsData.collect { data ->
                updateUI(data)
            }
        }
    }

    private fun updateUI(data: AnalyticsData) {
        // Update summary cards
        binding.tvTotalHours.text = String.format("%.1fh", data.totalMinutes / 60.0)
        binding.tvTotalSessions.text = data.sessionCount.toString()
        binding.tvAvgSession.text = if (data.sessionCount > 0) {
            String.format("%.0f min", data.totalMinutes.toDouble() / data.sessionCount)
        } else {
            "0 min"
        }

        // Update weekly chart
        setupWeeklyChart(data.weeklyData)

        // Update subject breakdown
        setupSubjectChart(data.subjectBreakdown)

        // Update top tasks
        updateTopTasks(data.topTasks)

        // Show/hide empty state
        binding.emptyState.visibility = if (data.isEmpty) View.VISIBLE else View.GONE
        binding.contentLayout.visibility = if (data.isEmpty) View.GONE else View.VISIBLE
    }

    private fun setupWeeklyChart(weeklyData: Map<String, Int>) {
        val entries = weeklyData.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = BarDataSet(entries, "Minutes").apply {
            color = resources.getColor(R.color.primary_blue, null)
            valueTextSize = 10f
        }

        binding.weeklyChart.apply {
            data = BarData(dataSet)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(weeklyData.keys.toList())
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
            }
            description.isEnabled = false
            legend.isEnabled = false
            animateY(300)
            invalidate()
        }
    }

    private fun setupSubjectChart(subjectData: Map<String, Int>) {
        val entries = subjectData.entries.mapIndexed { index, entry ->
            BarEntry(index.toFloat(), entry.value.toFloat())
        }

        val dataSet = BarDataSet(entries, "Minutes per Subject").apply {
            colors = listOf(
                resources.getColor(R.color.primary_blue, null),
                resources.getColor(R.color.primary_light_blue, null),
                resources.getColor(R.color.primary_blue, null).and(0x80FFFFFF.toInt())
            )
            valueTextSize = 10f
        }

        binding.subjectChart.apply {
            data = BarData(dataSet)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(subjectData.keys.toList())
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                labelRotationAngle = -45f
            }
            description.isEnabled = false
            legend.isEnabled = false
            animateY(300)
            invalidate()
        }
    }

    private fun updateTopTasks(topTasks: List<Pair<String, Int>>) {
        binding.topTasksContainer.removeAllViews()
        topTasks.take(5).forEach { (taskName, _) ->
            val taskView = layoutInflater.inflate(
                R.layout.item_top_task,
                binding.topTasksContainer,
                false
            )
            // TODO: Set task name and minutes from taskView TextViews
            binding.topTasksContainer.addView(taskView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

