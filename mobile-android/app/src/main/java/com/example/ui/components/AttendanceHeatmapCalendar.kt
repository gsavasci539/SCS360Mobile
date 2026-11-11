package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class DayAttendanceStatus(
    val title: String,
    val color: Color,
    val description: String
) {
    PRESENT("Geldi", AttendancePresent, "Öğrenci zamanında derse katıldı"),
    LATE("Geç Geldi", AttendanceLate, "Öğrenci derse geç katıldı"),
    EXCUSED("İzinli / Raporlu", AttendanceExcused, "Öğrenci veli izni veya sağlık raporu ile gelmedi"),
    ABSENT("Devamsız", AttendanceAbsent, "Öğrenci mazeretsiz olarak katılmadı"),
    WEEKEND("Haftasonu / Tatil", Color(0xFFE2E8F0), "Eğitim faaliyeti yok"),
    FUTURE("Gelecek Gün", Color(0xFFF1F5F9), "Henüz yoklama işlenmedi")
}

data class HeatmapDay(
    val dateStr: String,
    val dayOfMonth: Int,
    val dayOfWeek: Int, // 1: Monday, ..., 7: Sunday
    val monthName: String,
    val status: DayAttendanceStatus,
    val checkInTime: String? = null,
    val temperature: Double? = null,
    val notes: String? = null
)

/**
 * Calendar-based interactive attendance heatmap visualization.
 * Displays academic year attendance trends with GitHub-style/Heatmap grid,
 * detailed statistics, and interactive day detail inspection.
 */
@Composable
fun AttendanceHeatmapCalendar(
    student: Student,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedDay by remember { mutableStateOf<HeatmapDay?>(null) }
    var selectedViewMode by remember { mutableStateOf(0) } // 0: Son 12 Hafta (Haftalık Izgara), 1: Aylık Görünüm

    // Generate academic attendance days based on student seed
    val heatmapDays = remember(student.id) {
        generateStudentAttendanceHeatmap(student)
    }

    val totalActiveDays = heatmapDays.count { it.status != DayAttendanceStatus.WEEKEND && it.status != DayAttendanceStatus.FUTURE }
    val presentDays = heatmapDays.count { it.status == DayAttendanceStatus.PRESENT }
    val lateDays = heatmapDays.count { it.status == DayAttendanceStatus.LATE }
    val excusedDays = heatmapDays.count { it.status == DayAttendanceStatus.EXCUSED }
    val absentDays = heatmapDays.count { it.status == DayAttendanceStatus.ABSENT }
    val attendanceRate = if (totalActiveDays > 0) ((presentDays + (lateDays * 0.8f)) / totalActiveDays * 100).toInt().coerceIn(0, 100) else 100

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("attendance_heatmap_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with Title & Rate Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecondaryEmerald.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = SecondaryEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Yıllık Devam Isı Haritası",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "2025-2026 Eğitim-Öğretim Yılı Devam Dağılımı",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Attendance Rate Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (attendanceRate >= 85) SecondaryEmeraldContainer else StatusWarningContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (attendanceRate >= 85) SecondaryEmerald else StatusWarning
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (attendanceRate >= 85) Icons.Default.Verified else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (attendanceRate >= 85) SecondaryEmerald else StatusWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "%$attendanceRate Devam",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (attendanceRate >= 85) OnSecondaryEmeraldContainer else OnAccentAmberContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stat Counter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeatmapMiniStat(
                    title = "Geldi",
                    count = presentDays,
                    color = AttendancePresent,
                    modifier = Modifier.weight(1f)
                )
                HeatmapMiniStat(
                    title = "Geç",
                    count = lateDays,
                    color = AttendanceLate,
                    modifier = Modifier.weight(1f)
                )
                HeatmapMiniStat(
                    title = "İzinli",
                    count = excusedDays,
                    color = AttendanceExcused,
                    modifier = Modifier.weight(1f)
                )
                HeatmapMiniStat(
                    title = "Devamsız",
                    count = absentDays,
                    color = AttendanceAbsent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Days of week label header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pzt", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Sal", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Çar", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Per", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Cum", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                Text("Cts", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant, fontWeight = FontWeight.Normal)
                Text("Paz", fontSize = 10.sp, color = MaterialTheme.colorScheme.outlineVariant, fontWeight = FontWeight.Normal)
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Heatmap Grid (Scrollable weekly columns or multi-week grid)
            val weeks = remember(heatmapDays) {
                heatmapDays.chunked(7)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    weeks.takeLast(10).forEach { weekDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekDays.forEach { day ->
                                val isSelected = selectedDay?.dateStr == day.dateStr
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(day.status.color)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.5.dp,
                                            color = if (isSelected) BrandNavy else Color.Black.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedDay = if (selectedDay?.dateStr == day.dateStr) null else day
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${day.dayOfMonth}",
                                        fontSize = 9.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                        color = if (day.status == DayAttendanceStatus.WEEKEND || day.status == DayAttendanceStatus.FUTURE)
                                            Color(0xFF64748B) else Color.White
                                    )
                                }
                            }
                            // Fill remaining days in row if partial week
                            if (weekDays.size < 7) {
                                repeat(7 - weekDays.size) {
                                    Spacer(modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Selected Day Inspection Tooltip Box
            AnimatedVisibility(
                visible = selectedDay != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedDay?.let { day ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = day.status.color.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, day.status.color.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${day.dayOfMonth} ${day.monthName} (${day.dateStr})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy
                                )
                                Text(
                                    text = "${day.status.title} • ${day.status.description}",
                                    fontSize = 11.sp,
                                    color = day.status.color,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (day.checkInTime != null) {
                                    Text(
                                        text = "Giriş: ${day.checkInTime} • Sıcaklık: ${day.temperature ?: 36.5}°C",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedDay = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Heatmap Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeatmapLegendItem(color = AttendancePresent, label = "Geldi")
                HeatmapLegendItem(color = AttendanceLate, label = "Geç")
                HeatmapLegendItem(color = AttendanceExcused, label = "İzinli")
                HeatmapLegendItem(color = AttendanceAbsent, label = "Devamsız")
                HeatmapLegendItem(color = Color(0xFFE2E8F0), label = "Tatil")
            }
        }
    }
}

@Composable
private fun HeatmapMiniStat(
    title: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
private fun HeatmapLegendItem(
    color: Color,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Generates realistic chronological attendance distribution for a student
 */
private fun generateStudentAttendanceHeatmap(student: Student): List<HeatmapDay> {
    val list = mutableListOf<HeatmapDay>()
    val cal = Calendar.getInstance()
    // Start from approx 70 days ago (10 weeks)
    cal.add(Calendar.DAY_OF_YEAR, -69)

    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr"))
    val monthFormat = SimpleDateFormat("MMMM", Locale("tr"))
    val seed = student.id.hashCode()

    for (i in 0 until 70) {
        val dateStr = dateFormat.format(cal.time)
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday, ...
        val monthName = monthFormat.format(cal.time)

        val isWeekend = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)

        val status = if (isWeekend) {
            DayAttendanceStatus.WEEKEND
        } else {
            val hash = (seed + i * 37) % 100
            when {
                hash < 82 -> DayAttendanceStatus.PRESENT
                hash < 90 -> DayAttendanceStatus.LATE
                hash < 95 -> DayAttendanceStatus.EXCUSED
                else -> DayAttendanceStatus.ABSENT
            }
        }

        val checkInTime = if (status == DayAttendanceStatus.PRESENT) {
            "08:${10 + (i % 25)}"
        } else if (status == DayAttendanceStatus.LATE) {
            "09:${15 + (i % 20)}"
        } else null

        val temp = if (status == DayAttendanceStatus.PRESENT || status == DayAttendanceStatus.LATE) {
            36.2 + ((i % 6) * 0.1)
        } else null

        list.add(
            HeatmapDay(
                dateStr = dateStr,
                dayOfMonth = dayOfMonth,
                dayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1,
                monthName = monthName,
                status = status,
                checkInTime = checkInTime,
                temperature = temp
            )
        )

        cal.add(Calendar.DAY_OF_YEAR, 1)
    }

    return list
}
