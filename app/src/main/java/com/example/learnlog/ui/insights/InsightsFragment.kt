package com.example.learnlog.ui.insights

import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learnlog.R
import com.example.learnlog.databinding.FragmentInsightsBinding
import com.example.learnlog.ui.base.BaseFragment
import com.example.learnlog.ui.insights.compose.InsightsComposeCards
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

@AndroidEntryPoint
class InsightsFragment : BaseFragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InsightsViewModel by viewModels()

    private lateinit var topTasksAdapter: TopTasksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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

        binding.topBar.pageTitle.text = getString(R.string.page_insights_title)

        setupComposeView()
        setupTimeRangeChips()
        setupTopTasksRecyclerView()
        setupCharts()
        observeData()
        setupAnalyticsButton()
        setupResetButton()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                findNavController().navigate(R.id.settingsFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupAnalyticsButton() {
        binding.cardOpenAnalytics.setOnClickListener {
            findNavController().navigate(R.id.analyticsFragment)
        }
    }

    private fun setupResetButton() {
        binding.cardResetData.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun showResetConfirmationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Reset All Data")
            .setMessage("Are you sure you want to delete ALL data?\n\n" +
                    "This will permanently delete:\n" +
                    "• All tasks\n" +
                    "• All notes\n" +
                    "• All study sessions\n" +
                    "• All subjects\n" +
                    "• All statistics\n\n" +
                    "This action CANNOT be undone!")
            .setPositiveButton("DELETE ALL") { _, _ ->
                viewModel.resetAllData()
                android.widget.Toast.makeText(
                    requireContext(),
                    "All data has been deleted",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun setupComposeView() {
        binding.composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                MaterialTheme {
                    InsightsComposeCards(
                        insightsDataFlow = viewModel.insightsData,
                        quoteFlow = viewModel.quoteOfTheDay
                    )
                }
            }
        }
    }

    private fun setupTimeRangeChips() {
        with(binding) {
            timeRangeChips.setOnCheckedStateChangeListener { _, checkedIds ->
                when (checkedIds.firstOrNull()) {
                    R.id.todayChip -> viewModel.setTimeRange(DateRange.TODAY)
                    R.id.weekChip -> viewModel.setTimeRange(DateRange.WEEK)
                    R.id.monthChip -> viewModel.setTimeRange(DateRange.MONTH)
                    R.id.customChip -> showDateRangePicker()
                }
            }
            todayChip.isChecked = true
        }
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val startDate = Instant.ofEpochMilli(selection.first)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val endDate = Instant.ofEpochMilli(selection.second)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()

            viewModel.setCustomDateRange(
                LocalDate.of(startDate.year, startDate.monthValue, startDate.dayOfMonth),
                LocalDate.of(endDate.year, endDate.monthValue, endDate.dayOfMonth)
            )
        }

        picker.show(parentFragmentManager, "DATE_RANGE_PICKER")
    }

    private fun setupTopTasksRecyclerView() {
        topTasksAdapter = TopTasksAdapter { topTask ->
            // Drill down to tasks page (tasks will be visible by default)
            try {
                findNavController().navigate(R.id.tasksFragment)
            } catch (e: Exception) {
                // Navigation failed, ignore
            }
        }

        binding.topTasksRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = topTasksAdapter
        }
    }

    private fun setupCharts() {
        // Setup Pie Chart
        with(binding.subjectsPieChart) {
            description.isEnabled = false
            setUsePercentValues(false)
            setDrawEntryLabels(false)
            legend.isEnabled = true
            legend.textColor = resources.getColor(R.color.text_primary, null)
            setDrawHoleEnabled(true)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setHoleColor(Color.TRANSPARENT)
            setEntryLabelColor(Color.BLACK)
            setEntryLabelTextSize(12f)

            // Enable click listener for drill-down
            setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                    if (e is PieEntry) {
                        val subject = e.label
                        // Navigate to tasks page
                        try {
                            findNavController().navigate(R.id.tasksFragment)
                        } catch (ex: Exception) {
                            // Navigation failed, ignore
                        }
                    }
                }

                override fun onNothingSelected() {}
            })
        }

        // Setup Bar Chart
        with(binding.plannedVsActualChart) {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = resources.getColor(R.color.text_primary, null)
            }

            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = resources.getColor(R.color.light_gray, null)
                textColor = resources.getColor(R.color.text_primary, null)
                axisMinimum = 0f
            }

            axisRight.isEnabled = false
            legend.isEnabled = true
            legend.textColor = resources.getColor(R.color.text_primary, null)

            // Enable click for drill-down to planner
            setOnChartValueSelectedListener(object : com.github.mikephil.charting.listener.OnChartValueSelectedListener {
                override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                    // Get the date from the X position and navigate to planner on that date
                    // Implementation depends on having the date list available
                }

                override fun onNothingSelected() {}
            })
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.insightsData.collect { data ->
                updateTotalFocusTime(data.totalFocusMinutes)
                updateSubjectsPieChart(data.timeBySubject)
                updatePlannedVsActual(data)
                updateCompletionRate(data)
                updateStreakText(data.currentStreak)
                updateTopTasks(data.topTasks)
                updateSessionStats(data)
            }
        }
    }

    private fun updateTotalFocusTime(minutes: Int) {
        if (minutes > 0) {
            val hours = minutes / 60
            val mins = minutes % 60
            binding.totalFocusTimeText.text = if (hours > 0) {
                "${hours}h ${mins}m"
            } else {
                "${mins}m"
            }
            binding.totalFocusTimeText.visibility = View.VISIBLE
            binding.emptyStateTotal.visibility = View.GONE
        } else {
            binding.totalFocusTimeText.visibility = View.GONE
            binding.emptyStateTotal.visibility = View.VISIBLE
            binding.emptyStateTotal.text = getString(R.string.empty_focus_time)
        }
    }

    private fun updateSubjectsPieChart(subjectData: Map<String, Int>) {
        if (subjectData.isEmpty()) {
            binding.subjectsPieChart.isVisible = false
            binding.emptyStateSubject.isVisible = true
            return
        }

        binding.subjectsPieChart.isVisible = true
        binding.emptyStateSubject.isVisible = false

        val entries = subjectData.map { (subject, minutes) ->
            PieEntry(minutes.toFloat(), subject)
        }

        val colors = listOf(
            Color.rgb(64, 89, 128),   // Navy blue
            Color.rgb(149, 165, 124),  // Sage green
            Color.rgb(217, 184, 162),  // Beige
            Color.rgb(191, 134, 134),  // Rose
            Color.rgb(179, 48, 80),    // Deep rose
            Color.rgb(255, 193, 7),    // Amber
            Color.rgb(76, 175, 80)     // Green
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 14f
            valueTextColor = Color.WHITE
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val hours = value.toInt() / 60
                    val mins = value.toInt() % 60
                    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                }
            }
        }

        binding.subjectsPieChart.data = PieData(dataSet)
        binding.subjectsPieChart.invalidate()

        // Add center text showing total
        val totalMinutes = subjectData.values.sum()
        val totalHours = totalMinutes / 60
        val totalMins = totalMinutes % 60
        binding.subjectsPieChart.centerText = if (totalHours > 0) {
            "${totalHours}h\n${totalMins}m"
        } else {
            "${totalMins}m"
        }
    }

    private fun updatePlannedVsActual(data: InsightsData) {
        // Update summary texts
        val plannedHours = data.plannedMinutes / 60
        val plannedMins = data.plannedMinutes % 60
        binding.plannedTimeText.text = if (plannedHours > 0) {
            "${plannedHours}h ${plannedMins}m"
        } else {
            "${plannedMins}m"
        }

        val actualHours = data.actualMinutes / 60
        val actualMins = data.actualMinutes % 60
        binding.actualTimeText.text = if (actualHours > 0) {
            "${actualHours}h ${actualMins}m"
        } else {
            "${actualMins}m"
        }

        val adherence = if (data.plannedMinutes > 0) {
            ((data.actualMinutes.toFloat() / data.plannedMinutes) * 100).toInt()
        } else {
            0
        }
        binding.adherenceText.text = "$adherence%"

        // Update bar chart
        if (data.plannedVsActualByDay.isEmpty()) {
            binding.plannedVsActualChart.clear()
            return
        }

        val plannedEntries = mutableListOf<BarEntry>()
        val actualEntries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        data.plannedVsActualByDay.entries.forEachIndexed { index, (date, values) ->
            plannedEntries.add(BarEntry(index.toFloat(), values.plannedMinutes.toFloat()))
            actualEntries.add(BarEntry(index.toFloat(), values.actualMinutes.toFloat()))
            labels.add("${date.monthValue}/${date.dayOfMonth}")
        }

        val plannedDataSet = BarDataSet(plannedEntries, "Planned").apply {
            color = Color.rgb(255, 193, 7) // Amber
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }

        val actualDataSet = BarDataSet(actualEntries, "Actual").apply {
            color = Color.rgb(64, 89, 128) // Navy blue
            valueTextColor = Color.BLACK
            valueTextSize = 10f
        }

        val barData = BarData(plannedDataSet, actualDataSet).apply {
            barWidth = 0.35f
        }

        binding.plannedVsActualChart.apply {
            this.data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            groupBars(0f, 0.3f, 0.02f)
            invalidate()
        }
    }

    private fun updateCompletionRate(data: InsightsData) {
        val rate = (data.completionRate * 100).toInt()
        binding.completionRateText.text = "$rate%"
        binding.completionDetailsText.text = "${data.completedTasks} of ${data.totalTasks} tasks completed"
    }

    private fun updateStreakText(streak: Int) {
        if (streak > 0) {
            binding.streakText.text = streak.toString()
            binding.streakLabel.text = if (streak == 1) "Day Streak" else "Day Streak"
            binding.streakLabel.isVisible = true
            binding.emptyStateStreak.isVisible = false
        } else {
            binding.streakText.text = "0"
            binding.streakLabel.isVisible = false
            binding.emptyStateStreak.isVisible = true
        }
    }

    private fun updateTopTasks(topTasks: List<TopTask>) {
        if (topTasks.isEmpty()) {
            binding.topTasksRecyclerView.isVisible = false
            binding.emptyStateTopTasks.isVisible = true
        } else {
            binding.topTasksRecyclerView.isVisible = true
            binding.emptyStateTopTasks.isVisible = false
            topTasksAdapter.submitList(topTasks)
        }
    }

    private fun updateSessionStats(data: InsightsData) {
        binding.avgSessionText.text = "${data.avgSessionMinutes}m"
        binding.interruptionsText.text = data.totalInterruptions.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
