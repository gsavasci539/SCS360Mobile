package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ClassGroup
import com.example.data.model.TimetableSlot
import com.example.ui.screens.LessonDetailDialog
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeeklyTimetableComponent(
    classes: List<ClassGroup>,
    timetableSlots: List<TimetableSlot>,
    selectedClassId: String? = null,
    onClassSelected: ((String) -> Unit)? = null,
    showClassSelector: Boolean = true,
    teacherFilterName: String? = null,
    modifier: Modifier = Modifier
) {
    val calendar = remember { Calendar.getInstance() }
    val currentDayOfWeekJava = calendar.get(Calendar.DAY_OF_WEEK)
    val initialDayIndex = when (currentDayOfWeekJava) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 0
    }

    var selectedDayIndex by remember { mutableIntStateOf(initialDayIndex) }
    var internalClassId by remember {
        mutableStateOf(selectedClassId ?: classes.firstOrNull()?.id ?: "class-1")
    }
    var selectedSlotForDetail by remember { mutableStateOf<TimetableSlot?>(null) }

    val activeClassId = selectedClassId ?: internalClassId
    val currentClass = classes.find { it.id == activeClassId } ?: classes.firstOrNull() ?: ClassGroup(name = "8-A Sınıfı")

    val days = listOf(
        Pair(1, "Pazartesi"),
        Pair(2, "Salı"),
        Pair(3, "Çarşamba"),
        Pair(4, "Perşembe"),
        Pair(5, "Cuma"),
        Pair(6, "Cumartesi")
    )

    val targetDayOfWeek = days[selectedDayIndex].first

    // Current time formatting
    val currentTimeStr = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    val daySlots = remember(timetableSlots, selectedDayIndex, activeClassId, teacherFilterName) {
        var slots = timetableSlots.filter { it.day_of_week == targetDayOfWeek }
        if (!teacherFilterName.isNullOrBlank()) {
            slots = slots.filter { it.teacher_name.contains(teacherFilterName, ignoreCase = true) }
        }
        slots.sortedBy { it.period_number }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Class Selector (if enabled)
        if (showClassSelector && classes.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = classes.indexOfFirst { it.id == activeClassId }.coerceAtLeast(0),
                containerColor = Color.Transparent,
                contentColor = PrimaryBlue,
                edgePadding = 16.dp,
                divider = {}
            ) {
                classes.forEach { cls ->
                    val isSelected = cls.id == activeClassId
                    Tab(
                        selected = isSelected,
                        onClick = {
                            internalClassId = cls.id
                            onClassSelected?.invoke(cls.id)
                        },
                        text = {
                            Text(
                                text = cls.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }
        }

        // Day of Week Pills
        ScrollableTabRow(
            selectedTabIndex = selectedDayIndex,
            containerColor = Color.Transparent,
            contentColor = PrimaryBlue,
            edgePadding = 16.dp,
            divider = {}
        ) {
            days.forEachIndexed { index, day ->
                val isSelected = selectedDayIndex == index
                val isToday = (index == initialDayIndex)

                Tab(
                    selected = isSelected,
                    onClick = { selectedDayIndex = index },
                    text = {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) PrimaryBlue else if (isToday) PrimaryBlueContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                            border = if (isToday && !isSelected) BorderStroke(1.dp, PrimaryBlue) else null,
                            tonalElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else SecondaryEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = day.second,
                                    color = if (isSelected) Color.White else if (isToday) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
                )
            }
        }

        // Schedule Slots List
        if (daySlots.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EventBusy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bu gün için ders programı bulunmuyor.", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                daySlots.forEach { slot ->
                    val isLiveNow = (selectedDayIndex == initialDayIndex) && (currentTimeStr in slot.start_time..slot.end_time)

                    TimetableCard(
                        slot = slot,
                        isLiveNow = isLiveNow,
                        onClick = { selectedSlotForDetail = slot }
                    )
                }
            }
        }
    }

    // Detail Dialog
    selectedSlotForDetail?.let { slot ->
        LessonDetailDialog(
            slot = slot,
            onDismiss = { selectedSlotForDetail = null }
        )
    }
}

@Composable
private fun TimetableCard(
    slot: TimetableSlot,
    isLiveNow: Boolean,
    onClick: () -> Unit
) {
    val subjectColor = Color(slot.color_hex)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        tonalElevation = if (isLiveNow) 4.dp else 1.dp,
        shadowElevation = if (isLiveNow) 3.dp else 1.dp,
        border = if (isLiveNow) BorderStroke(2.dp, SecondaryEmerald) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period Number Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                Text(
                    text = "${slot.period_number}. Ders",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = slot.start_time,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = BrandNavy
                )
                Text(
                    text = slot.end_time,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Vertical Subject Color Indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(subjectColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Subject Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = slot.subject_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = BrandNavy
                    )
                    if (isLiveNow) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SecondaryEmerald
                        ) {
                            Text(
                                text = "ŞU AN CANLI",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = slot.teacher_name,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MeetingRoom,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = slot.room_name,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }
                }
            }

            // Arrow Action
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Detay",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getSampleSlotsForDay(dayOfWeek: Int, className: String): List<TimetableSlot> = emptyList()
