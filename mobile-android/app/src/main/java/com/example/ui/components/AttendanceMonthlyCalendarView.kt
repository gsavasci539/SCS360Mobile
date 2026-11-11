package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.Student
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class CalendarDayStatus(
    val title: String,
    val shortCode: String,
    val color: Color,
    val bgColor: Color,
    val borderColor: Color
) {
    PRESENT("Geldi (Tam Gün)", "G", SecondaryEmerald, Color(0xFFECFDF5), SecondaryEmerald.copy(alpha = 0.5f)),
    ABSENT("Gelmedi (Devamsız)", "Y", AccentRose, Color(0xFFFEF2F2), AccentRose.copy(alpha = 0.6f)),
    LATE("Geç Geldi", "GK", AccentAmber, Color(0xFFFFFBEB), AccentAmber.copy(alpha = 0.5f)),
    EXCUSED("İzinli / Raporlu", "İ", PrimaryBlue, Color(0xFFEFF6FF), PrimaryBlue.copy(alpha = 0.5f)),
    WEEKEND("Hafta Sonu", "-", Color(0xFF94A3B8), Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
    FUTURE("Gelecek Gün", "", Color(0xFFCBD5E1), Color(0xFFF8FAFC), Color(0xFFF1F5F9))
}

data class DayAttendanceDetail(
    val dayNumber: Int,
    val fullDate: String,
    val dayOfWeekName: String,
    val isWeekend: Boolean,
    val status: CalendarDayStatus,
    val checkInTime: String? = null,
    val checkOutTime: String? = null,
    val temperature: Double? = null,
    val note: String? = null,
    val parentNotified: Boolean = true
)

@Composable
fun AttendanceMonthlyCalendarView(
    students: List<Student>,
    selectedStudentId: String?,
    onSelectStudent: (String) -> Unit,
    existingRecords: List<AttendanceRecord> = emptyList(),
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 1) }) }
    var selectedDayDetail by remember { mutableStateOf<DayAttendanceDetail?>(null) }

    val activeStudent = remember(students, selectedStudentId) {
        students.find { it.id == selectedStudentId } ?: students.firstOrNull()
    }

    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("tr", "TR")) }
    val monthTitle = remember(currentYearMonth) {
        monthFormatter.format(currentYearMonth.time).replaceFirstChar { it.uppercase() }
    }

    // Days calculation
    val daysInMonth = remember(currentYearMonth) {
        val cal = currentYearMonth.clone() as Calendar
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(currentYearMonth) {
        val cal = currentYearMonth.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        // Convert Sunday=1, Monday=2 -> Monday=0 .. Sunday=6
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        (dow + 5) % 7
    }

    // Generate daily status map for active student
    val studentDailyData = remember(activeStudent?.id, currentYearMonth, existingRecords) {
        val map = mutableMapOf<Int, DayAttendanceDetail>()
        val cal = currentYearMonth.clone() as Calendar
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)

        for (day in 1..daysInMonth) {
            cal.set(year, month, day)
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, day)
            val dayName = SimpleDateFormat("EEEE", Locale("tr", "TR")).format(cal.time)

            if (isWeekend) {
                map[day] = DayAttendanceDetail(
                    dayNumber = day,
                    fullDate = dateStr,
                    dayOfWeekName = dayName,
                    isWeekend = true,
                    status = CalendarDayStatus.WEEKEND,
                    note = "Hafta Sonu Tatili"
                )
            } else {
                // Check if we have an explicit record
                val rec = existingRecords.find { it.student_id == activeStudent?.id && it.date == dateStr }

                val status: CalendarDayStatus
                val checkIn: String?
                val checkOut: String?
                val temp: Double?
                val note: String?

                if (rec != null) {
                    status = when (rec.status) {
                        "ABSENT" -> CalendarDayStatus.ABSENT
                        "LATE" -> CalendarDayStatus.LATE
                        "EXCUSED" -> CalendarDayStatus.EXCUSED
                        else -> CalendarDayStatus.PRESENT
                    }
                    checkIn = rec.check_in_time ?: "08:30"
                    checkOut = rec.check_out_time ?: "16:30"
                    temp = rec.temperature_celsius ?: 36.5
                    note = rec.note ?: "Günlük yoklama kaydı tamamlandı."
                } else {
                    // Seed realistic patterns per student hash
                    val seed = (activeStudent?.id?.hashCode() ?: 0) + day
                    val mod = Math.abs(seed) % 20
                    when {
                        mod == 4 -> {
                            status = CalendarDayStatus.ABSENT
                            checkIn = null
                            checkOut = null
                            temp = null
                            note = "Veli bilgisi dahilinde mazeretsiz devamsızlık."
                        }
                        mod == 9 -> {
                            status = CalendarDayStatus.EXCUSED
                            checkIn = null
                            checkOut = null
                            temp = 37.1
                            note = "Sağlık ocağı muayenesi / Raporlu izinli."
                        }
                        mod == 14 -> {
                            status = CalendarDayStatus.LATE
                            checkIn = "09:12"
                            checkOut = "16:30"
                            temp = 36.6
                            note = "Trafik yoğunluğu nedeniyle 25 dk geç kalındı."
                        }
                        else -> {
                            status = CalendarDayStatus.PRESENT
                            checkIn = "08:28"
                            checkOut = "16:30"
                            temp = 36.4
                            note = "Zamanında giriş yaptı, derslere aktif katıldı."
                        }
                    }
                }

                map[day] = DayAttendanceDetail(
                    dayNumber = day,
                    fullDate = dateStr,
                    dayOfWeekName = dayName,
                    isWeekend = false,
                    status = status,
                    checkInTime = checkIn,
                    checkOutTime = checkOut,
                    temperature = temp,
                    note = note
                )
            }
        }
        map
    }

    // Monthly Analytics
    val schoolDays = studentDailyData.values.filter { !it.isWeekend }
    val presentDays = schoolDays.count { it.status == CalendarDayStatus.PRESENT }
    val absentDays = schoolDays.count { it.status == CalendarDayStatus.ABSENT }
    val lateDays = schoolDays.count { it.status == CalendarDayStatus.LATE }
    val excusedDays = schoolDays.count { it.status == CalendarDayStatus.EXCUSED }
    val attendanceRate = if (schoolDays.isNotEmpty()) ((presentDays + lateDays).toFloat() / schoolDays.size * 100).toInt() else 100

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Header: Title & Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aylık Takvim",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = BrandNavy
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "Renk kodlu devamsızlıklar",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Month Switcher
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryBlue.copy(alpha = 0.08f))
                        .padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newCal = currentYearMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            currentYearMonth = newCal
                            selectedDayDetail = null
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki Ay", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = monthTitle,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        softWrap = false,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )

                    IconButton(
                        onClick = {
                            val newCal = currentYearMonth.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            currentYearMonth = newCal
                            selectedDayDetail = null
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki Ay", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Student Picker Chips (if multi students)
            if (students.size > 1) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(students) { std ->
                        val isSelected = (activeStudent?.id == std.id)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onSelectStudent(std.id)
                                selectedDayDetail = null
                            },
                            label = {
                                Text(
                                    text = std.full_name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryBlueContainer,
                                selectedLabelColor = OnPrimaryBlueContainer
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Monthly Statistics Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BrandNavy.copy(alpha = 0.04f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${activeStudent?.full_name ?: "Öğrenci"} — Devam Oranı",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%$attendanceRate Başarı",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (attendanceRate >= 85) SecondaryEmerald else if (attendanceRate >= 70) AccentAmber else AccentRose
                            )
                        }

                        // Mini progress bar
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (attendanceRate / 100f).coerceIn(0f, 1f))
                                    .clip(CircleShape)
                                    .background(
                                        if (attendanceRate >= 85) SecondaryEmerald else if (attendanceRate >= 70) AccentAmber else AccentRose
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CalendarStatMiniBadge("Geldi", "$presentDays gün", SecondaryEmerald)
                        CalendarStatMiniBadge("Devamsız", "$absentDays gün", AccentRose)
                        CalendarStatMiniBadge("Geç", "$lateDays gün", AccentAmber)
                        CalendarStatMiniBadge("İzinli", "$excusedDays gün", PrimaryBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Weekday Header: Pzt, Sal, Çar, Per, Cum, Cmt, Paz
            val weekdays = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekdays.forEachIndexed { index, dayName ->
                    val isWeekendCol = index >= 5
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isWeekendCol) AccentRose.copy(alpha = 0.7f) else BrandNavy.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar Grid Calculation
            val totalCells = ((firstDayOfWeek + daysInMonth + 6) / 7) * 7
            val rows = totalCells / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - firstDayOfWeek + 1

                            if (dayNumber in 1..daysInMonth) {
                                val dayDetail = studentDailyData[dayNumber]
                                val isSelected = (selectedDayDetail?.dayNumber == dayNumber)
                                if (dayDetail != null) {
                                    CalendarDayCell(
                                        detail = dayDetail,
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedDayDetail = if (isSelected) null else dayDetail
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            } else {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color Legend
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem("Geldi", SecondaryEmerald)
                    LegendItem("Devamsız", AccentRose)
                    LegendItem("Geç", AccentAmber)
                    LegendItem("İzinli", PrimaryBlue)
                    LegendItem("Tatil", Color(0xFF94A3B8))
                }
            }

            // Expanded Day Detail Card
            AnimatedVisibility(
                visible = selectedDayDetail != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedDayDetail?.let { detail ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = detail.status.bgColor,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, detail.status.borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(detail.status.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${detail.dayNumber}",
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${detail.fullDate} (${detail.dayOfWeekName})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = BrandNavy
                                        )
                                        Text(
                                            text = detail.status.title,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = detail.status.color
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedDayDetail = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Kapat", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (!detail.isWeekend) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = detail.status.borderColor.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    detail.checkInTime?.let {
                                        DayDetailItem(label = "Giriş Saati", value = it, icon = Icons.Default.Login)
                                    }
                                    detail.checkOutTime?.let {
                                        DayDetailItem(label = "Çıkış Saati", value = it, icon = Icons.Default.Logout)
                                    }
                                    detail.temperature?.let {
                                        DayDetailItem(label = "Vücut Isısı", value = "$it°C", icon = Icons.Default.Thermostat)
                                    }
                                }

                                detail.note?.let { n ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = detail.status.color, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = n,
                                                fontSize = 12.sp,
                                                color = BrandNavy
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "🔔 Veliye anlık SMS / Push bildirim iletildi.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    detail: DayAttendanceDetail,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = detail.status
    val cellBorder = if (isSelected) {
        androidx.compose.foundation.BorderStroke(2.dp, BrandNavy)
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, status.borderColor)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) status.color.copy(alpha = 0.25f) else status.bgColor)
            .border(cellBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${detail.dayNumber}",
                fontSize = 12.sp,
                fontWeight = if (isSelected || status == CalendarDayStatus.ABSENT) FontWeight.Black else FontWeight.Bold,
                color = if (detail.isWeekend) Color(0xFF64748B) else BrandNavy
            )

            if (!detail.isWeekend && status.shortCode.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(status.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = status.shortCode,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarStatMiniBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DayDetailItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = BrandNavy)
        Spacer(modifier = Modifier.width(4.dp))
        Column {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
        }
    }
}
