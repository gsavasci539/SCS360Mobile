package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data Model for Attendance Pie Slice
 */
data class AttendancePieSlice(
    val id: String,                    // "PRESENT", "ABSENT", "LATE", "EXCUSED"
    val label: String,                 // "Katıldı", "Gelmedi", "Geç Kaldı", "İzinli"
    val count: Int,                    // Öğrenci sayısı
    val color: Color,                  // Ana renk
    val secondaryColor: Color,         // Arka plan / vurgulu renk
    val icon: ImageVector,
    val description: String
)

/**
 * Interactive Attendance Status Pie/Donut Chart Component for DashboardPage
 * Visualizes:
 * - Katıldı (Geldi / Present) - Zümrüt Yeşili
 * - Gelmedi (Devamsız / Absent) - Kırmızı
 * - Geç Kaldı (Late) - Kehribar Sarısı
 * - İzinli (Excused) - Mavi (varsa)
 *
 * Supports:
 * - Tap on slice to pop-out with animation
 * - Dynamic center status badge (% and student count)
 * - Interactive legend chips that synchronize with chart selection
 * - Callback for selected slice to filter contextual action lists
 */
@Composable
fun TodayAttendancePieChart(
    presentCount: Int,
    absentCount: Int,
    lateCount: Int,
    excusedCount: Int = 0,
    selectedSliceId: String? = null,
    onSliceSelected: (AttendancePieSlice?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    val totalStudents = (presentCount + absentCount + lateCount + excusedCount).coerceAtLeast(1)
    val overallAttendanceRate = (((presentCount + lateCount * 0.5) / totalStudents.toDouble()) * 100).toInt().coerceIn(0, 100)

    val slices = remember(presentCount, absentCount, lateCount, excusedCount) {
        val list = mutableListOf(
            AttendancePieSlice(
                id = "PRESENT",
                label = "Katıldı",
                count = presentCount,
                color = Color(0xFF10B981),
                secondaryColor = Color(0xFFECFDF5),
                icon = Icons.Default.CheckCircle,
                description = "Derse katılan öğrenciler"
            ),
            AttendancePieSlice(
                id = "ABSENT",
                label = "Gelmedi",
                count = absentCount,
                color = Color(0xFFEF4444),
                secondaryColor = Color(0xFFFEF2F2),
                icon = Icons.Default.Cancel,
                description = "Gelmeyen / devamsız öğrenciler"
            ),
            AttendancePieSlice(
                id = "LATE",
                label = "Geç Kaldı",
                count = lateCount,
                color = Color(0xFFF59E0B),
                secondaryColor = Color(0xFFFFFBEB),
                icon = Icons.Default.Schedule,
                description = "İlk derse geç giriş yapanlar"
            )
        )
        if (excusedCount > 0) {
            list.add(
                AttendancePieSlice(
                    id = "EXCUSED",
                    label = "İzinli",
                    count = excusedCount,
                    color = Color(0xFF3B82F6),
                    secondaryColor = Color(0xFFEFF6FF),
                    icon = Icons.Default.EventAvailable,
                    description = "Raporlu veya izinli öğrenciler"
                )
            )
        }
        list
    }

    // Animation progress for chart entry
    var animTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(presentCount, absentCount, lateCount, excusedCount) {
        animTarget = 1f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = animTarget,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "attendancePieAnim"
    )

    val currentSelectedSlice = slices.find { it.id == selectedSliceId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_attendance_pie_chart_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Pie / Donut Chart with Center KPI Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(165.dp)
                    .pointerInput(slices, totalStudents) {
                        detectTapGestures { tapOffset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = tapOffset.x - center.x
                            val dy = tapOffset.y - center.y
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f

                            // Start angle is -90 degrees (top center)
                            val normalizedAngle = (angle + 90f) % 360f

                            var currentAngle = 0f
                            var clickedSlice: AttendancePieSlice? = null
                            for (slice in slices) {
                                val sweep = (slice.count.toFloat() / totalStudents.toFloat() * 360f)
                                if (normalizedAngle in currentAngle..(currentAngle + sweep)) {
                                    clickedSlice = slice
                                    break
                                }
                                currentAngle += sweep
                            }

                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (clickedSlice != null) {
                                if (selectedSliceId == clickedSlice.id) {
                                    onSliceSelected(null)
                                } else {
                                    onSliceSelected(clickedSlice)
                                }
                            } else {
                                onSliceSelected(null)
                            }
                        }
                    }
                    .testTag("attendance_pie_canvas")
            ) {
                val strokeWidth = 26.dp.toPx()
                val diameter = size.minDimension - strokeWidth - 14.dp.toPx()
                val arcSize = Size(diameter, diameter)
                val arcTopLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )

                var startAngle = -90f

                for (slice in slices) {
                    val sweepAngle = ((slice.count.toFloat() / totalStudents.toFloat() * 360f)) * animatedProgress
                    val isSelected = selectedSliceId == slice.id

                    val actualStrokeWidth = if (isSelected) strokeWidth + 8.dp.toPx() else strokeWidth

                    val offsetTopLeft = if (isSelected) {
                        val midAngle = Math.toRadians((startAngle + sweepAngle / 2f).toDouble())
                        Offset(
                            arcTopLeft.x + (cos(midAngle) * 7.dp.toPx()).toFloat(),
                            arcTopLeft.y + (sin(midAngle) * 7.dp.toPx()).toFloat()
                        )
                    } else {
                        arcTopLeft
                    }

                    if (sweepAngle > 0.5f) {
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle + 1.2f,
                            sweepAngle = (sweepAngle - 2.4f).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = offsetTopLeft,
                            size = arcSize,
                            style = Stroke(width = actualStrokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    startAngle += sweepAngle
                }
            }

            // Center Info Badge inside the Donut
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSliceSelected(null)
                    }
                    .testTag("attendance_pie_center_badge")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (currentSelectedSlice != null) {
                        val pct = ((currentSelectedSlice.count.toDouble() / totalStudents) * 100).toInt()
                        Text(
                            text = "%$pct",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentSelectedSlice.color,
                            maxLines = 1
                        )
                        Text(
                            text = currentSelectedSlice.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${currentSelectedSlice.count} Öğr.",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1
                        )
                    } else {
                        Text(
                            text = "%$overallAttendanceRate",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (overallAttendanceRate >= 90) Color(0xFF059669) else Color(0xFFD97706),
                            maxLines = 1
                        )
                        Text(
                            text = "Katılım",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1
                        )
                        Text(
                            text = "$presentCount / $totalStudents",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Interactive Legend Chips Row (Katıldı, Gelmedi, Geç Kaldı)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            slices.forEach { slice ->
                val isSelected = selectedSliceId == slice.id
                val pct = ((slice.count.toDouble() / totalStudents) * 100).toInt()

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) slice.color else slice.secondaryColor,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) slice.color else slice.color.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (selectedSliceId == slice.id) {
                                onSliceSelected(null)
                            } else {
                                onSliceSelected(slice)
                            }
                        }
                        .testTag("attendance_chip_${slice.id}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else slice.color)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = slice.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${slice.count} (%$pct)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSelected) Color.White else slice.color,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
