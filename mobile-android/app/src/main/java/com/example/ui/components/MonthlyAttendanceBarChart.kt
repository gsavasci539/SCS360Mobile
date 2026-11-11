package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.theme.*
import kotlin.math.roundToInt

data class MonthlyAttendanceData(
    val monthName: String,
    val monthCode: String,
    val attendancePercentage: Float, // 0.0f to 100.0f
    val presentDays: Int,
    val absentDays: Int,
    val excusedDays: Int,
    val totalSchoolDays: Int
)

@Composable
fun MonthlyAttendanceBarChart(
    student: Student,
    modifier: Modifier = Modifier
) {
    // Generate deterministic realistic monthly attendance data based on student id
    val monthlyData = remember(student.id) {
        generateMonthlyDataForStudent(student)
    }

    var selectedIndex by remember { mutableStateOf<Int?>(monthlyData.lastIndex) }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(student.id) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    val selectedData = selectedIndex?.let { if (it in monthlyData.indices) monthlyData[it] else null }
    val averagePercentage = remember(monthlyData) {
        (monthlyData.map { it.attendancePercentage }.average()).toFloat()
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Aylık Devam Analizi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "2025-2026 Akademik Dönem",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Average Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (averagePercentage >= 90f) SecondaryEmeraldContainer else PrimaryBlueContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = if (averagePercentage >= 90f) SecondaryEmerald else PrimaryBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Ort. %${averagePercentage.roundToInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (averagePercentage >= 90f) SecondaryEmerald else PrimaryBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Compose-Charts Canvas Bar Chart
            val barColorHigh = SecondaryEmerald
            val barColorMid = PrimaryBlue
            val barColorLow = StatusWarning
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            val targetLineColor = StatusWarning.copy(alpha = 0.85f)
            val selectionRingColor = MaterialTheme.colorScheme.onSurface

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(monthlyData) {
                            detectTapGestures { offset ->
                                val count = monthlyData.size
                                val chartWidth = size.width
                                val step = chartWidth / count
                                val index = (offset.x / step).toInt().coerceIn(0, count - 1)
                                selectedIndex = index
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height
                    val bottomPadding = 24.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val availableHeight = height - bottomPadding - topPadding

                    val count = monthlyData.size
                    val step = width / count
                    val barWidth = (step * 0.55f).coerceAtMost(32.dp.toPx())

                    // Draw 100%, 85% (Target) and 50% grid lines
                    val y100 = topPadding
                    val y85 = topPadding + availableHeight * (1f - 0.85f)
                    val y50 = topPadding + availableHeight * 0.5f

                    // 100% Line
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y100),
                        end = Offset(width, y100),
                        strokeWidth = 1.dp.toPx()
                    )

                    // 85% Target Guideline (dashed)
                    drawLine(
                        color = targetLineColor,
                        start = Offset(0f, y85),
                        end = Offset(width, y85),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // 50% Line
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y50),
                        end = Offset(width, y50),
                        strokeWidth = 1.dp.toPx()
                    )

                    // Draw each month's bar
                    monthlyData.forEachIndexed { i, data ->
                        val centerX = step * i + step / 2
                        val barHeight = (data.attendancePercentage / 100f) * availableHeight * animatedProgress.value
                        val barTop = topPadding + availableHeight - barHeight
                        val barLeft = centerX - barWidth / 2

                        val isSelected = i == selectedIndex

                        val barBrush = when {
                            data.attendancePercentage >= 90f -> Brush.verticalGradient(
                                colors = listOf(Color(0xFF34D399), barColorHigh)
                            )
                            data.attendancePercentage >= 80f -> Brush.verticalGradient(
                                colors = listOf(Color(0xFF60A5FA), barColorMid)
                            )
                            else -> Brush.verticalGradient(
                                colors = listOf(Color(0xFFFBBF24), barColorLow)
                            )
                        }

                        // Bar background (track)
                        drawRoundRect(
                            color = gridLineColor.copy(alpha = 0.2f),
                            topLeft = Offset(barLeft, topPadding),
                            size = Size(barWidth, availableHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Animated Bar fill
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(barLeft, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Selection indicator ring
                        if (isSelected) {
                            drawRoundRect(
                                color = selectionRingColor,
                                topLeft = Offset(barLeft - 2.dp.toPx(), barTop - 2.dp.toPx()),
                                size = Size(barWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            // Month Labels Row below canvas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                monthlyData.forEachIndexed { i, data ->
                    val isSelected = i == selectedIndex
                    Text(
                        text = data.monthCode,
                        fontSize = 10.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedIndex = i }
                            .padding(vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selected Month Detailed Card
            if (selectedData != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedData.monthName} Ayı Detayı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "%${selectedData.attendancePercentage.roundToInt()} Katılım",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedData.attendancePercentage >= 90f) SecondaryEmerald else PrimaryBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AttendanceMetricChip(
                                label = "Katılım",
                                value = "${selectedData.presentDays} Gün",
                                color = SecondaryEmerald
                            )
                            AttendanceMetricChip(
                                label = "Devamsız",
                                value = "${selectedData.absentDays} Gün",
                                color = if (selectedData.absentDays > 0) StatusError else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AttendanceMetricChip(
                                label = "İzinli/Raporlu",
                                value = "${selectedData.excusedDays} Gün",
                                color = StatusWarning
                            )
                            AttendanceMetricChip(
                                label = "Toplam",
                                value = "${selectedData.totalSchoolDays} Gün",
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend / Footnote
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(targetLineColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "%85 MEB Devam Kriteri", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(
                    text = "Aylara dokunarak detayları inceleyebilirsiniz",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AttendanceMetricChip(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun generateMonthlyDataForStudent(student: Student): List<MonthlyAttendanceData> {
    val hash = student.id.hashCode()
    val baseRate = 88f + (kotlin.math.abs(hash % 10)) // 88% - 97%

    val months = listOf(
        Pair("Eylül", "Eyl"),
        Pair("Ekim", "Eki"),
        Pair("Kasım", "Kas"),
        Pair("Aralık", "Ara"),
        Pair("Ocak", "Oca"),
        Pair("Şubat", "Şub"),
        Pair("Mart", "Mar"),
        Pair("Nisan", "Nis"),
        Pair("Mayıs", "May"),
        Pair("Haziran", "Haz")
    )

    return months.mapIndexed { idx, (name, code) ->
        val variation = when ((idx + hash) % 5) {
            0 -> 3f
            1 -> -4f
            2 -> 2f
            3 -> -2f
            else -> 1f
        }
        val rate = (baseRate + variation).coerceIn(75f, 100f)
        val totalDays = 21
        val present = ((rate / 100f) * totalDays).roundToInt().coerceIn(15, totalDays)
        val remaining = totalDays - present
        val excused = if (remaining > 0 && idx % 2 == 0) 1 else 0
        val absent = (remaining - excused).coerceAtLeast(0)

        MonthlyAttendanceData(
            monthName = name,
            monthCode = code,
            attendancePercentage = (present.toFloat() / totalDays.toFloat()) * 100f,
            presentDays = present,
            absentDays = absent,
            excusedDays = excused,
            totalSchoolDays = totalDays
        )
    }
}
