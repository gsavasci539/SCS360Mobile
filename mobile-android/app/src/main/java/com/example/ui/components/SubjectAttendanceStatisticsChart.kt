package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassGroup
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.ui.theme.*

/**
 * Data model for subject-based attendance statistics
 */
data class SubjectAttendanceStat(
    val subjectCode: String,
    val subjectName: String,
    val teacherName: String,
    val totalHours: Int,
    val presentHours: Int,
    val absentHours: Int,
    val lateHours: Int,
    val color: Color
) {
    val attendanceRate: Float
        get() = if (totalHours > 0) (presentHours.toFloat() / totalHours.toFloat()) * 100f else 100f

    val absenceRate: Float
        get() = if (totalHours > 0) (absentHours.toFloat() / totalHours.toFloat()) * 100f else 0f
}

/**
 * Interactive graphical statistics component displaying subject/lesson-based attendance
 * and absence rates for teachers and school administrators.
 */
@Composable
fun SubjectAttendanceStatisticsView(
    classes: List<ClassGroup>,
    selectedClassId: String,
    onClassSelected: (String) -> Unit,
    attendanceRecords: List<AttendanceRecord> = emptyList(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var selectedPeriod by remember { mutableStateOf("TERM_1") } // "TERM_1", "TERM_2", "FULL_YEAR"
    var sortBy by remember { mutableStateOf("ABSENCE_DESC") } // "ABSENCE_DESC", "ATTENDANCE_DESC", "NAME"
    var selectedSubjectDetail by remember { mutableStateOf<SubjectAttendanceStat?>(null) }

    // Real subject statistics calculated directly from attendanceRecords
    val subjectStats = remember(attendanceRecords, selectedClassId) {
        val filtered = if (selectedClassId == "all") attendanceRecords else attendanceRecords.filter { it.class_id == selectedClassId }
        if (filtered.isEmpty()) {
            emptyList<SubjectAttendanceStat>()
        } else {
            val present = filtered.count { it.status.equals("PRESENT", ignoreCase = true) }
            val absent = filtered.count { it.status.equals("ABSENT", ignoreCase = true) }
            val late = filtered.count { it.status.equals("LATE", ignoreCase = true) }
            listOf(
                SubjectAttendanceStat(
                    subjectCode = "YOK",
                    subjectName = "Genel Yoklama",
                    teacherName = "Ders Öğretmeni",
                    totalHours = filtered.size,
                    presentHours = present,
                    absentHours = absent,
                    lateHours = late,
                    color = Color(0xFF2563EB)
                )
            )
        }
    }

    val sortedStats = remember(subjectStats, sortBy) {
        when (sortBy) {
            "ABSENCE_DESC" -> subjectStats.sortedByDescending { it.absenceRate }
            "ATTENDANCE_DESC" -> subjectStats.sortedByDescending { it.attendanceRate }
            "NAME" -> subjectStats.sortedBy { it.subjectName }
            else -> subjectStats
        }
    }

    val totalClassHours = subjectStats.sumOf { it.totalHours }
    val totalClassPresent = subjectStats.sumOf { it.presentHours }
    val totalClassAbsent = subjectStats.sumOf { it.absentHours }
    val overallAttendancePercent = if (totalClassHours > 0) (totalClassPresent.toFloat() / totalClassHours.toFloat()) * 100f else 100f

    val highestAbsentSubject = subjectStats.maxByOrNull { it.absenceRate }
    val bestAttendanceSubject = subjectStats.minByOrNull { it.absenceRate }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Class & Period Filter Selector
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏫 Sınıf & Dönem Filtresi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = BrandNavy
                    )

                    // Period Selector
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BrandNavy.copy(alpha = 0.06f))
                            .padding(2.dp)
                    ) {
                        listOf(
                            "TERM_1" to "1. Dönem",
                            "TERM_2" to "2. Dönem",
                            "FULL_YEAR" to "Tüm Yıl"
                        ).forEach { (key, label) ->
                            val isSel = selectedPeriod == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) PrimaryBlue else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedPeriod = key
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Class Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(classes) { cls ->
                        val isSelected = cls.id == selectedClassId
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onClassSelected(cls.id)
                            },
                            label = { Text(cls.name, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlue,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // Summary KPI Banner
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Genel Ders Katılımı",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "%${String.format(java.util.Locale.US, "%.1f", overallAttendancePercent)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (overallAttendancePercent >= 90f) SecondaryEmerald else if (overallAttendancePercent >= 80f) AccentAmber else MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = (if (overallAttendancePercent >= 90f) SecondaryEmerald else AccentAmber).copy(alpha = 0.12f),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (overallAttendancePercent >= 90f) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (overallAttendancePercent >= 90f) SecondaryEmerald else AccentAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (overallAttendancePercent >= 90f) "Yüksek Katılım" else "Dikkat",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (overallAttendancePercent >= 90f) SecondaryEmerald else AccentAmber,
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Mini Quick Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Toplam Ders", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$totalClassHours Saat", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandNavy)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFECFDF5)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Katılınan", fontSize = 10.5.sp, color = SecondaryEmerald)
                            Text("$totalClassPresent Saat", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SecondaryEmerald)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Devamsızlık", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.error)
                            Text("$totalClassAbsent Saat", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Highlight Insights
                if (highestAbsentSubject != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "En çok devamsızlık yapılan ders: ${highestAbsentSubject.subjectName} (%${String.format(java.util.Locale.US, "%.1f", highestAbsentSubject.absenceRate)})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Graphical Breakdown Header & Sort Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📊 Ders Bazlı Devamsızlık Çizelgesi",
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = BrandNavy
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sırala:", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(4.dp))
                Box {
                    var sortMenuExpanded by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable { sortMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (sortBy) {
                                    "ABSENCE_DESC" -> "Devamsızlık (Azalan)"
                                    "ATTENDANCE_DESC" -> "Katılım (Azalan)"
                                    else -> "Ders Adı"
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Devamsızlık Oranına Göre", fontSize = 12.sp) },
                            onClick = {
                                sortBy = "ABSENCE_DESC"
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Katılım Oranına Göre", fontSize = 12.sp) },
                            onClick = {
                                sortBy = "ATTENDANCE_DESC"
                                sortMenuExpanded = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ders İsmine Göre (A-Z)", fontSize = 12.sp) },
                            onClick = {
                                sortBy = "NAME"
                                sortMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Subject Graphical Progress Cards List
        sortedStats.forEach { stat ->
            SubjectProgressCard(
                stat = stat,
                onClick = { selectedSubjectDetail = stat }
            )
        }
    }

    // Detail Dialog for clicked subject
    if (selectedSubjectDetail != null) {
        val detail = selectedSubjectDetail!!
        AlertDialog(
            onDismissRequest = { selectedSubjectDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(detail.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(detail.subjectCode, color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(detail.subjectName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                        Text(detail.teacherName, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Toplam Ders Yükü", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${detail.totalHours} Saat", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Eksiksiz Katılım", fontSize = 12.sp, color = SecondaryEmerald)
                                Text("${detail.presentHours} Saat (%${String.format(java.util.Locale.US, "%.1f", detail.attendanceRate)})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SecondaryEmerald)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Özürsüz Devamsızlık", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Text("${detail.absentHours} Saat (%${String.format(java.util.Locale.US, "%.1f", detail.absenceRate)})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Derse Geç Kalma", fontSize = 12.sp, color = AccentAmber)
                                Text("${detail.lateHours} Kez", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentAmber)
                            }
                        }
                    }

                    Text(
                        text = "ℹ️ Bu ders için devamsızlık sınırı toplam 10 ders saatidir. Öğrencilerin kritik sınıra yaklaşması durumunda veli bilgilendirme sistemi otomatik SMS & bildirim gönderir.",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedSubjectDetail = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Kapat")
                }
            }
        )
    }
}

@Composable
private fun SubjectProgressCard(
    stat: SubjectAttendanceStat,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(stat.subjectCode) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = stat.attendanceRate / 100f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
    }

    val isHighAbsence = stat.absenceRate > 10f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(
            if (isHighAbsence) 1.5.dp else 1.dp,
            if (isHighAbsence) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(stat.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stat.subjectCode,
                            color = stat.color,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = stat.subjectName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = BrandNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stat.teacherName,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Rates
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "%${String.format(java.util.Locale.US, "%.1f", stat.attendanceRate)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = if (stat.attendanceRate >= 90f) SecondaryEmerald else if (stat.attendanceRate >= 80f) AccentAmber else MaterialTheme.colorScheme.error
                        )
                        Text(" katılım", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "${stat.absentHours} sa devamsızlık",
                        fontSize = 10.5.sp,
                        color = if (stat.absentHours > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (stat.absentHours > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Graphical Progress Bar Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progressWidth = size.width * animProgress.value
                    if (progressWidth > 0f) {
                        val barColor = if (stat.attendanceRate >= 90f) SecondaryEmerald else if (stat.attendanceRate >= 80f) AccentAmber else Color(0xFFEF4444)
                        drawRoundRect(
                            color = barColor,
                            topLeft = Offset.Zero,
                            size = Size(progressWidth, size.height),
                            cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom hour metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ders Yükü: ${stat.totalHours} Saat",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Katılınan: ${stat.presentHours} | Devamsız: ${stat.absentHours} | Geç: ${stat.lateHours}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandNavy
                )
            }
        }
    }
}
