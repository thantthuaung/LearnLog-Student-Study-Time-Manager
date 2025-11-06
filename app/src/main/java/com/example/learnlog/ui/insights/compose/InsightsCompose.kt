package com.example.learnlog.ui.insights.compose

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.learnlog.data.repository.CachedQuote
import com.example.learnlog.ui.insights.InsightsData
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.cos
import kotlin.math.sin

/**
 * Composable cards for Insights screen
 * Demonstrates Jetpack Compose integration with existing XML UI
 */

@Composable
fun InsightsComposeCards(
    insightsDataFlow: StateFlow<InsightsData>,
    quoteFlow: StateFlow<CachedQuote?>,
    modifier: Modifier = Modifier
) {
    val insightsData by insightsDataFlow.collectAsState()
    val quote by quoteFlow.collectAsState()

    Column(modifier = modifier) {
        // Card 1: Total Focus Time
        TotalFocusTimeCard(
            totalMinutes = insightsData.totalFocusMinutes,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Card 2: Time by Subject (Donut Chart)
        TimeBySubjectCard(
            timeBySubject = insightsData.timeBySubject,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Quote of the Day (if available)
        quote?.let {
            MotivationQuoteCard(
                quote = it,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
fun TotalFocusTimeCard(
    totalMinutes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOTAL FOCUS TIME",
                fontSize = 14.sp,
                color = Color(0xFF666666),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (totalMinutes > 0) {
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                val timeText = when {
                    hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                    hours > 0 -> "${hours}h"
                    else -> "${minutes}m"
                }

                Text(
                    text = timeText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3) // nav_blue
                )
            } else {
                Text(
                    text = "0m",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2196F3)
                )
                Text(
                    text = "No focus time yet",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TimeBySubjectCard(
    timeBySubject: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "TIME BY SUBJECT",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (timeBySubject.isNotEmpty()) {
                DonutChart(
                    data = timeBySubject,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                ChartLegend(data = timeBySubject)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No sessions recorded yet",
                        fontSize = 14.sp,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum().toFloat()
    if (total == 0f) return

    // Predefined colors for subjects
    val colors = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFFC107), // Amber
        Color(0xFFFF5722), // Deep Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFF9800), // Orange
        Color(0xFF795548)  // Brown
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = minOf(canvasWidth, canvasHeight) / 2.5f
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val strokeWidth = radius * 0.5f

        var startAngle = -90f

        data.entries.forEachIndexed { index, (_, minutes) ->
            val sweepAngle = (minutes / total) * 360f
            val color = colors[index % colors.size]

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            startAngle += sweepAngle
        }

        // Draw center hole (makes it a donut)
        drawCircle(
            color = Color.White,
            radius = radius - strokeWidth,
            center = Offset(centerX, centerY)
        )

        // Draw total in center
        val hours = (total / 60).toInt()
        val mins = (total % 60).toInt()
        // Text drawing would require TextMeasurer or alternative approach
    }
}

@Composable
fun ChartLegend(
    data: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFFC107), Color(0xFFFF5722),
        Color(0xFF9C27B0), Color(0xFF00BCD4), Color(0xFFFF9800), Color(0xFF795548)
    )

    Column(modifier = modifier) {
        data.entries.forEachIndexed { index, (subject, minutes) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .padding(end = 8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(color = colors[index % colors.size])
                    }
                }

                Text(
                    text = subject.ifEmpty { "No Subject" },
                    fontSize = 14.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )

                val hours = minutes / 60
                val mins = minutes % 60
                val timeText = when {
                    hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                    hours > 0 -> "${hours}h"
                    else -> "${mins}m"
                }

                Text(
                    text = timeText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun MotivationQuoteCard(
    quote: CachedQuote,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "\"${quote.content}\"",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF333333),
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "— ${quote.author}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

