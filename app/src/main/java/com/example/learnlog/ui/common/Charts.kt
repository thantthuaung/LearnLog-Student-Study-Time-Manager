package com.example.learnlog.ui.common

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.example.learnlog.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.google.android.material.color.MaterialColors

/**
 * Consistent chart styling: recessive grid, a single axis, brand font, colours that follow
 * the light/dark theme, and a tap tooltip instead of a number on every bar.
 */
object Charts {

    /** Fixed categorical order (validated for colour-blind separation in both themes). */
    val SERIES = intArrayOf(R.color.chart_1, R.color.chart_2, R.color.chart_3, R.color.chart_4, R.color.chart_5, R.color.chart_6)

    fun seriesColor(context: Context, index: Int): Int =
        ContextCompat.getColor(context, SERIES[index % SERIES.size])

    fun font(context: Context): Typeface? = ResourcesCompat.getFont(context, R.font.jakarta_medium)

    fun styleBarChart(chart: BarChart) {
        val ctx = chart.context
        val axis = ContextCompat.getColor(ctx, R.color.chart_axis)
        val grid = ContextCompat.getColor(ctx, R.color.chart_grid)
        val tf = font(ctx)
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            setPinchZoom(false)
            setScaleEnabled(false)
            isDoubleTapToZoomEnabled = false
            setNoDataText("")
            setExtraOffsets(0f, 8f, 0f, 4f)
            setFitBars(true)
            axisRight.isEnabled = false
            axisLeft.apply {
                axisMinimum = 0f
                setDrawAxisLine(false)
                gridColor = grid
                gridLineWidth = 1f
                textColor = axis
                textSize = 10f
                typeface = tf
                setLabelCount(4, false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float) = Format.compact(value.toInt())
                }
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                axisLineColor = grid
                textColor = axis
                textSize = 10f
                typeface = tf
                granularity = 1f
            }
            legend.apply {
                textColor = MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOnSurfaceVariant)
                typeface = tf
                textSize = 12f
            }
        }
    }

    fun stylePieChart(chart: PieChart) {
        val tf = font(chart.context)
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(false)
            setDrawEntryLabels(false)
            isDrawHoleEnabled = true
            holeRadius = 70f
            transparentCircleRadius = 0f
            setHoleColor(Color.TRANSPARENT)
            setCenterTextTypeface(ResourcesCompat.getFont(context, R.font.jakarta_bold))
            setCenterTextSize(18f)
            setCenterTextColor(MaterialColors.getColor(chart, com.google.android.material.R.attr.colorOnSurface))
            rotationAngle = 270f
            isRotationEnabled = false
            setNoDataText("")
            setEntryLabelTypeface(tf)
            setExtraOffsets(4f, 4f, 4f, 4f)
        }
    }

    /** Tooltip for bar charts. [describe] turns the tapped bar index into (title, value) text. */
    class Marker(context: Context, private val describe: (Int) -> Pair<String, String>) :
        MarkerView(context, R.layout.view_chart_marker) {
        private val title: TextView = findViewById(R.id.marker_title)
        private val value: TextView = findViewById(R.id.marker_value)

        override fun refreshContent(e: Entry, highlight: Highlight) {
            val (t, v) = describe(e.x.toInt())
            title.text = t
            value.text = v
            super.refreshContent(e, highlight)
        }

        override fun getOffset(): MPPointF = MPPointF(-(width / 2f), -height.toFloat() - 12f)

        override fun getOffsetForDrawingAtPoint(posX: Float, posY: Float): MPPointF {
            val offset = getOffset()
            val chart = chartView ?: return offset
            val x = when {
                posX + offset.x < 0 -> -posX
                posX + width + offset.x > chart.width -> chart.width - posX - width
                else -> offset.x
            }
            val y = if (posY + offset.y < 0) 12f else offset.y
            return MPPointF(x, y)
        }
    }
}
