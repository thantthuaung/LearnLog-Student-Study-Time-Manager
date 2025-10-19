package com.example.learnlog.ui.insights

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentInsightsBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InsightsFragment : Fragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InsightsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set page title in header
        binding.topBar.pageTitle.text = getString(R.string.page_insights_title)

        setupTimeRangeChips()
        setupCharts()
        observeData()
    }

    private fun setupTimeRangeChips() {
        with(binding) {
            todayChip.text = getString(R.string.insights_today)
            weekChip.text = getString(R.string.insights_week)
            monthChip.text = getString(R.string.insights_month)

            timeRangeChips.setOnCheckedStateChangeListener { _, checkedIds ->
                when (checkedIds.firstOrNull()) {
                    R.id.todayChip -> viewModel.setTimeRange(TimeRange.TODAY)
                    R.id.weekChip -> viewModel.setTimeRange(TimeRange.WEEK)
                    R.id.monthChip -> viewModel.setTimeRange(TimeRange.MONTH)
                }
            }
            todayChip.isChecked = true
        }
    }

    private fun setupCharts() {
        with(binding.subjectsPieChart) {
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(true)
            legend.isEnabled = true
            setDrawHoleEnabled(true)
            holeRadius = 40f
        }

        with(binding.dailyLineChart) {
            description.isEnabled = false
            xAxis.setDrawGridLines(false)
            axisLeft.setDrawGridLines(false)
            axisRight.isEnabled = false
            legend.isEnabled = true
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.insightsData.collect { data ->
                updateTotalFocusTime(data.totalFocusMinutes)
                updateSubjectsPieChart(data.timeBySubject)
                updateStreakText(data.currentStreak)
            }
        }
    }

    private fun updateTotalFocusTime(minutes: Int) {
        val hours = minutes / 60
        val mins = minutes % 60
        binding.totalFocusTimeText.text = getString(R.string.total_focus_time_format, "$hours h $mins m")
    }

    private fun updateSubjectsPieChart(subjectData: Map<String, Int>) {
        val entries = subjectData.map { (subject, minutes) ->
            PieEntry(minutes.toFloat(), subject)
        }

        val dataSet = PieDataSet(entries, getString(R.string.time_by_subject)).apply {
            colors = listOf(
                Color.rgb(64, 89, 128),
                Color.rgb(149, 165, 124),
                Color.rgb(217, 184, 162),
                Color.rgb(191, 134, 134),
                Color.rgb(179, 48, 80)
            )
            valueTextSize = 14f
        }

        binding.subjectsPieChart.data = PieData(dataSet)
        binding.subjectsPieChart.invalidate()
    }

    private fun updateStreakText(streak: Int) {
        binding.streakText.text = getString(R.string.streak_days_format, streak)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
