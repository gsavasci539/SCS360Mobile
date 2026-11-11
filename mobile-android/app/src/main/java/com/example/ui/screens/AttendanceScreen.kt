package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassGroup
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AttendanceScreen(
    state: Okul360UiState,
    onClassSelected: (String) -> Unit,
    onStatusChanged: (studentId: String, status: String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onSaveAttendance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentClass = state.classes.find { it.id == state.attendanceClassId } ?: state.classes.firstOrNull()
    val records = state.attendanceRecords

    var viewMode by remember { mutableStateOf("DAILY") } // "DAILY", "MONTHLY_CALENDAR", "SUBJECT_STATS"
    var selectedCalendarStudentId by remember { mutableStateOf<String?>(null) }
    val tableViewMode = rememberResolvedTableMode()
    val isCardView = isCompactCardMode(tableViewMode.value)

    val attendanceColumns = remember {
        listOf(
            TableColumnSpec("Öğrenci", weight = 1.35f),
            TableColumnSpec("Giriş / Ateş", weight = 0.9f),
            TableColumnSpec("Durum (G/Y/GÇ/İZ)", weight = 1.35f, alignment = TextAlign.End)
        )
    }

    val classStudents = remember(state.students, state.attendanceClassId) {
        state.students.filter { it.class_id == state.attendanceClassId || state.attendanceClassId.isBlank() }
            .ifEmpty { state.students }
    }

    val presentCount = records.count { it.status == "PRESENT" }
    val absentCount = records.count { it.status == "ABSENT" }
    val lateCount = records.count { it.status == "LATE" }
    val excusedCount = records.count { it.status == "EXCUSED" }
    var isFabExpanded by remember { mutableStateOf(false) }

    val attendanceActions = remember(onSaveAttendance, onMarkAllPresent, viewMode) {
        listOf(
            SpeedDialAction(
                id = "attendance_save_notify",
                title = "Yoklamayı Kaydet & SMS",
                description = "Yoklamayı onayla ve velilere anında bildir",
                icon = Icons.Default.CloudUpload,
                containerColor = SecondaryEmerald,
                testTag = "attendance_fab_action_save",
                onClick = {
                    isFabExpanded = false
                    onSaveAttendance()
                }
            ),
            SpeedDialAction(
                id = "attendance_mark_all_present",
                title = "Tümünü 'Var' Yap",
                description = "Tüm sınıf listesini tek tıkla 'Var' işaretle",
                icon = Icons.Default.DoneAll,
                containerColor = PrimaryBlue,
                testTag = "attendance_fab_action_all_present",
                onClick = {
                    isFabExpanded = false
                    onMarkAllPresent()
                }
            ),
            SpeedDialAction(
                id = "attendance_toggle_view",
                title = if (viewMode == "DAILY") "Aylık Takvim Görünümü" else "Günlük Liste Görünümü",
                description = if (viewMode == "DAILY") "Aylık devamsızlık haritasını aç" else "Günlük yoklama listesine dön",
                icon = if (viewMode == "DAILY") Icons.Default.CalendarMonth else Icons.Default.FormatListBulleted,
                containerColor = Color(0xFF7C3AED), // Purple
                testTag = "attendance_fab_action_toggle_view",
                onClick = {
                    isFabExpanded = false
                    viewMode = if (viewMode == "DAILY") "MONTHLY_CALENDAR" else "DAILY"
                }
            )
        )
    }

    Scaffold(
        floatingActionButton = {
            RoleSpeedDialFab(
                actions = attendanceActions,
                expanded = isFabExpanded,
                onExpandedChange = { isFabExpanded = it },
                fabColor = SecondaryEmerald,
                fabLabel = "Yoklama Hızlı Aksiyon",
                testTag = "attendance_speed_dial_fab",
                modifier = Modifier.padding(16.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Compact Top Control Bar: Title + Segmented Tab Switcher + Class Chips
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Title + Segmented Tab Control
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 Yoklama & Devamsızlık",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            color = BrandNavy,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.attendanceDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Full-width balanced Segmented Tab Control (Compact & elegant)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Günlük Tab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable { viewMode = "DAILY" }
                                    .testTag("tab_attendance_daily"),
                                color = if (viewMode == "DAILY") PrimaryBlue else Color.Transparent,
                                shape = RoundedCornerShape(7.dp),
                                shadowElevation = if (viewMode == "DAILY") 1.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null,
                                        tint = if (viewMode == "DAILY") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Günlük",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (viewMode == "DAILY") FontWeight.Bold else FontWeight.Medium,
                                        color = if (viewMode == "DAILY") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Takvim Tab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable { viewMode = "MONTHLY_CALENDAR" }
                                    .testTag("tab_attendance_calendar"),
                                color = if (viewMode == "MONTHLY_CALENDAR") PrimaryBlue else Color.Transparent,
                                shape = RoundedCornerShape(7.dp),
                                shadowElevation = if (viewMode == "MONTHLY_CALENDAR") 1.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = if (viewMode == "MONTHLY_CALENDAR") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Takvim",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (viewMode == "MONTHLY_CALENDAR") FontWeight.Bold else FontWeight.Medium,
                                        color = if (viewMode == "MONTHLY_CALENDAR") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Grafik Tab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(7.dp))
                                    .clickable { viewMode = "SUBJECT_STATS" }
                                    .testTag("tab_attendance_stats"),
                                color = if (viewMode == "SUBJECT_STATS") PrimaryBlue else Color.Transparent,
                                shape = RoundedCornerShape(7.dp),
                                shadowElevation = if (viewMode == "SUBJECT_STATS") 1.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = if (viewMode == "SUBJECT_STATS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "İstatistik",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (viewMode == "SUBJECT_STATS") FontWeight.Bold else FontWeight.Medium,
                                        color = if (viewMode == "SUBJECT_STATS") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Class Selector Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(state.classes) { cls ->
                            val isSelected = state.attendanceClassId == cls.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onClassSelected(cls.id) },
                                label = {
                                    Text(
                                        text = cls.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        softWrap = false,
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            if (viewMode == "MONTHLY_CALENDAR") {
                // Monthly Calendar View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 100.dp)
                ) {
                    item {
                        AttendanceMonthlyCalendarView(
                            students = classStudents,
                            selectedStudentId = selectedCalendarStudentId ?: classStudents.firstOrNull()?.id,
                            onSelectStudent = { selectedCalendarStudentId = it },
                            existingRecords = state.attendanceRecords
                        )
                    }
                }
            } else if (viewMode == "SUBJECT_STATS") {
                // Subject-based Attendance Statistics Graphical View
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        SubjectAttendanceStatisticsView(
                            classes = state.classes,
                            selectedClassId = state.attendanceClassId.ifBlank { state.classes.firstOrNull()?.id ?: "" },
                            onClassSelected = onClassSelected,
                            attendanceRecords = state.attendanceRecords
                        )
                    }
                }
            } else {
                // Daily Attendance Mode: Summary Banner + AI Card + Sticky Header + Student List
                if (records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                } else {
                    ResponsiveTableScope() {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 120.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            // Summary Banner & Quick Actions (Scrolls with table)
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                Text(
                                                    text = currentClass?.name ?: "Sınıf",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = BrandNavy,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Tarih: ${state.attendanceDate} • Öğretmen: ${currentClass?.teacher_name ?: "-"}",
                                                    fontSize = 10.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
    
                                            Spacer(modifier = Modifier.width(6.dp))
    
                                            FilledTonalButton(
                                                onClick = onMarkAllPresent,
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = PrimaryBlueContainer,
                                                    contentColor = OnPrimaryBlueContainer
                                                ),
                                                shape = RoundedCornerShape(7.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Tümü Geldi", fontSize = 11.sp, fontWeight = FontWeight.Bold, softWrap = false, maxLines = 1)
                                            }
                                        }
    
                                        Spacer(modifier = Modifier.height(6.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                        Spacer(modifier = Modifier.height(4.dp))
    
                                        // Attendance Breakdown Counts
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            AttendanceCountItem(label = "Geldi", count = presentCount, color = SecondaryEmerald)
                                            AttendanceCountItem(label = "Gelmedi", count = absentCount, color = AccentRose)
                                            AttendanceCountItem(label = "Geç", count = lateCount, color = AccentAmber)
                                            AttendanceCountItem(label = "İzinli", count = excusedCount, color = PrimaryBlue)
                                        }
                                    }
                                }
                            }
    
                            item {
                                RoleContextualAiCard(
                                    screenName = "ATTENDANCE",
                                    state = state,
                                    onQuickActionClick = { actionType ->
                                        if (actionType == "MARK_ALL_PRESENT") {
                                            onMarkAllPresent()
                                        }
                                    }
                                )
                            }
    
                            // Pinned Sticky Column Header
                            stickyHeader {
                                StickyTableHeaderRow(
                                    columns = attendanceColumns,
                                    totalCount = records.size,
                                    viewModeState = tableViewMode
                                )
                            }
    
                            itemsIndexed(records, key = { _, r -> r.student_id }) { index, record ->
                                AttendanceStudentRow(
                                    index = index,
                                    isCardView = isCardView,
                                    record = record,
                                    onStatusSelect = { newStatus ->
                                        onStatusChanged(record.student_id, newStatus)
                                    }
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
fun AttendanceCountItem(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$count",
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AttendanceStudentRow(
    record: AttendanceRecord,
    onStatusSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    index: Int = 0,
    isCardView: Boolean = true
) {
    val checkInInfo = if (record.check_in_time != null && record.status != "ABSENT") {
        "${record.check_in_time} • ${record.temperature_celsius ?: 36.5}°C"
    } else {
        "Giriş Yok"
    }

    if (isCardView) {
        CompactResponsiveCard(modifier = modifier) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                    Text(
                        text = record.student_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = checkInInfo,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                AttendanceQuickStatusSelector(
                    currentStatus = record.status,
                    onStatusSelect = onStatusSelect
                )
            }
        }
    } else {
        CompactTableRow(index = index, modifier = modifier) {
            Text(
                text = record.student_name,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = BrandNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1.35f)
            )
            Text(
                text = checkInInfo,
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.9f)
            )
            Box(
                modifier = Modifier.weight(1.35f),
                contentAlignment = Alignment.CenterEnd
            ) {
                AttendanceQuickStatusSelector(
                    currentStatus = record.status,
                    onStatusSelect = onStatusSelect
                )
            }
        }
    }
}

@Composable
private fun AttendanceQuickStatusSelector(
    currentStatus: String,
    onStatusSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        AttendanceStatus.values().forEach { statusEnum ->
            val isSelected = currentStatus == statusEnum.name
            val statusColor = Color(statusEnum.colorHex)

            Surface(
                modifier = Modifier
                    .size(width = 30.dp, height = 28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onStatusSelect(statusEnum.name) },
                color = if (isSelected) statusColor else statusColor.copy(alpha = 0.08f),
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    if (isSelected) statusColor else statusColor.copy(alpha = 0.25f)
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusEnum.shortCode,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                        color = if (isSelected) Color.White else statusColor
                    )
                }
            }
        }
    }
}
