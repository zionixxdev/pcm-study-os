package dev.zionixx.pcmstudyos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small Canvas charts instead of a charting library: no extra dependency, no
 * layout overhead, and cheap enough for a 3 GB Android Go phone.
 */

@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        if (values.isEmpty()) return@Canvas
        val gap = 3.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(1f)
        values.forEachIndexed { index, value ->
            val barHeight = (value / max) * size.height
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
            )
            if (barHeight > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight.coerceAtLeast(barWidth / 2)),
                    cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
                )
            }
        }
    }
}

@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    maxOverride: Float? = null,
) {
    val max = maxOverride ?: (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - (value / max).coerceIn(0f, 1f) * size.height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - (value / max).coerceIn(0f, 1f) * size.height
            drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
fun RingProgress(
    percent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    stroke: Dp = 7.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.height(size).fillMaxWidth()) {
            val diameter = minOf(this.size.width, this.size.height) - stroke.toPx()
            val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (percent.coerceIn(0, 100) / 100f),
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
fun DonutChart(
    slices: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    stroke: Dp = 18.dp,
) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(modifier = modifier.height(size).fillMaxWidth()) {
        val total = slices.sumOf { it.second.toDouble() }.toFloat()
        val diameter = minOf(this.size.width, this.size.height) - stroke.toPx()
        val topLeft = Offset((this.size.width - diameter) / 2f, (this.size.height - diameter) / 2f)
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = stroke.toPx()),
        )
        if (total <= 0f) return@Canvas
        var start = -90f
        for ((color, value) in slices) {
            val sweep = 360f * (value / total)
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep - 1.5f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = stroke.toPx(), cap = StrokeCap.Butt),
            )
            start += sweep
        }
    }
}
