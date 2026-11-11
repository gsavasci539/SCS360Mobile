package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ClassGroup
import com.example.data.model.TimetableSlot
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyScheduleScreen(
    state: Okul360UiState,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Current day of week (1: Mon, 2: Tue, 3: Wed, 4: Thu, 5: Fri, 6: Sat)
    val calendar = remember { Calendar.getInstance() }
    val currentDayOfWeekJava = calendar.get(Calendar.DAY_OF_WEEK)
    val initialDayIndex = when (currentDayOfWeekJava) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 0 // Default to Monday on Sunday
    }

    var selectedDayIndex by remember { mutableIntStateOf(initialDayIndex) }
    var selectedClassId by remember { mutableStateOf(state.classes.firstOrNull()?.id ?: "class-1") }
    var selectedSlotForDetail by remember { mutableStateOf<TimetableSlot?>(null) }
    var showShareSuccessToast by remember { mutableStateOf(false) }

    val days = listOf(
        Pair(1, "Pazartesi"),
        Pair(2, "Salı"),
        Pair(3, "Çarşamba"),
        Pair(4, "Perşembe"),
        Pair(5, "Cuma"),
        Pair(6, "Cumartesi")
    )

    val currentClass = state.classes.find { it.id == selectedClassId } ?: state.classes.firstOrNull() ?: ClassGroup(name = "8-A Sınıfı")

    // Filter slots for selected day & class
    val targetDayOfWeek = days[selectedDayIndex].first
    val daySlots = remember(state.timetableSlots, selectedDayIndex, selectedClassId) {
        val slots = state.timetableSlots.filter { it.day_of_week == targetDayOfWeek }
        if (slots.isNotEmpty()) {
            slots.sortedBy { it.period_number }
        } else {
            getDefaultSlotsForDay(targetDayOfWeek, currentClass.name)
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Haftalık Ders Programı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandNavy
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = PrimaryBlue
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showShareSuccessToast = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = PrimaryBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Class Selector & Quick Info Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = currentClass.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "Sınıf Öğretmeni: ${currentClass.teacher_name}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (state.classes.size > 1) {
                        var classMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { classMenuExpanded = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Sınıf Değiştir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = classMenuExpanded,
                                onDismissRequest = { classMenuExpanded = false }
                            ) {
                                state.classes.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text("${cls.name} (${cls.grade_level})") },
                                        onClick = {
                                            selectedClassId = cls.id
                                            classMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Class, contentDescription = null, tint = PrimaryBlue)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Day Selector Scrollable TabRow
            ScrollableTabRow(
                selectedTabIndex = selectedDayIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue,
                edgePadding = 12.dp,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) }
            ) {
                days.forEachIndexed { index, (dayNum, dayTitle) ->
                    val isSelected = selectedDayIndex == index
                    val isToday = (dayNum == when (currentDayOfWeekJava) {
                        Calendar.MONDAY -> 1
                        Calendar.TUESDAY -> 2
                        Calendar.WEDNESDAY -> 3
                        Calendar.THURSDAY -> 4
                        Calendar.FRIDAY -> 5
                        Calendar.SATURDAY -> 6
                        else -> 1
                    })

                    Tab(
                        selected = isSelected,
                        onClick = { selectedDayIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = dayTitle,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                if (isToday) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(SecondaryEmerald)
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Daily Schedule Timetable List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Contextual AI Schedule & Conflict Engine
                item {
                    RoleContextualAiCard(
                        screenName = "SCHEDULE",
                        state = state
                    )
                }

                // Day summary banner
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PrimaryBlueContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${days[selectedDayIndex].second} • Toplam ${daySlots.size} Ders",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BrandNavy
                                )
                            }

                            Text(
                                text = "08:30 - 15:55",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                // Timetable Slots
                items(daySlots) { slot ->
                    // Check if lunch break should be shown before 5th period
                    if (slot.period_number == 5) {
                        LunchBreakItem()
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    TimetableSlotCard(
                        slot = slot,
                        onClick = { selectedSlotForDetail = slot }
                    )
                }
            }
        }
    }

    // Modal: Lesson Detail Dialog
    selectedSlotForDetail?.let { slot ->
        LessonDetailDialog(
            slot = slot,
            onDismiss = { selectedSlotForDetail = null }
        )
    }

    // Modal: Share Schedule Dialog
    if (showShareSuccessToast) {
        Dialog(onDismissRequest = { showShareSuccessToast = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SecondaryEmerald, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Ders Programı Hazırlandı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${currentClass.name} haftalık ders programı PDF formatında kaydedildi ve veli/öğrenci bildirim kanalına iletildi.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showShareSuccessToast = false },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tamam")
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableSlotCard(
    slot: TimetableSlot,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val subjectColor = Color(slot.color_hex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Period & Time Indicator Column
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(subjectColor.copy(alpha = 0.12f))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${slot.period_number}. Ders",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = subjectColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = slot.start_time,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = slot.end_time,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle Subject & Teacher & Classroom Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = slot.subject_name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandNavy
                        )
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(subjectColor.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = slot.subject_code,
                            color = subjectColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = slot.teacher_name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = slot.room_name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                }
            }

            // Right Arrow Action
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Detay",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun LunchBreakItem() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = AccentAmber.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = AccentAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Öğle Arası & Yemekhane",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandNavy
                    )
                    Text(
                        text = "Günün Menüsü: Mercimek Çorbası, Izgara Köfte, Salata",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "11:45 - 12:45",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = AccentAmber
            )
        }
    }
}

@Composable
fun LessonDetailDialog(
    slot: TimetableSlot,
    onDismiss: () -> Unit
) {
    val subjectColor = Color(slot.color_hex)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(subjectColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${slot.period_number}. Ders Detayı",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = subjectColor
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = slot.subject_name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandNavy
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Ders Saati:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "${slot.start_time} - ${slot.end_time}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Derslik / Atölye:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = slot.room_name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Ders Öğretmeni:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = slot.teacher_name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Gereksinim & Materyaller:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandNavy
                )

                Spacer(modifier = Modifier.height(6.dp))

                val materials = when (slot.subject_code) {
                    "MAT" -> listOf("Ders Kitabı ve Kareli Defter", "Geometri Seti (Cetvel, Pergel)", "Dönem Ödevi Dosyası")
                    "FEN" -> listOf("Fen Bilimleri Laboratuvar Önlüğü", "Deney Rapor Defteri", "Koruyucu Gözlük")
                    "KOD" -> listOf("Tablet / Bilgisayar Şarjı", "Robotik Proje Kiti (Arduino)")
                    "BED" -> listOf("Okul Spor Kıyafeti ve Temiz Spor Ayakkabı", "Yedek Tişört ve Su Matarası")
                    "GS" -> listOf("Resim Defteri (35x50)", "Sulu Boya / Kuru Boya Seti")
                    else -> listOf("Ders Kitabı ve Not Defteri", "Kalemlik ve Yazı Araçları")
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    materials.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = SecondaryEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = item, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getDefaultSlotsForDay(dayOfWeek: Int, className: String): List<TimetableSlot> {
    return when (dayOfWeek) {
        1 -> listOf(
            TimetableSlot("s-1", 1, 1, "Pazartesi", "08:30", "09:10", "Matematik", "MAT", "Mustafa Yılmaz", "Derslik 101", 0xFF2563EB, class_name = className),
            TimetableSlot("s-2", 2, 1, "Pazartesi", "09:20", "10:00", "Matematik", "MAT", "Mustafa Yılmaz", "Derslik 101", 0xFF2563EB, class_name = className),
            TimetableSlot("s-3", 3, 1, "Pazartesi", "10:15", "10:55", "Fen Bilimleri", "FEN", "Ayşe Kaya", "Fen Laboratuvarı", 0xFF059669, class_name = className),
            TimetableSlot("s-4", 4, 1, "Pazartesi", "11:05", "11:45", "Türkçe", "TRK", "Fatma Öğretmen", "Derslik 101", 0xFF7C3AED, class_name = className),
            TimetableSlot("s-5", 5, 1, "Pazartesi", "12:45", "13:25", "İngilizce", "ING", "John Smith", "Dil Atölyesi", 0xFFD97706, class_name = className),
            TimetableSlot("s-6", 6, 1, "Pazartesi", "13:35", "14:15", "Robotik & Kodlama", "KOD", "Ahmet Hoca", "Bilişim Lab", 0xFF0D9488, class_name = className),
            TimetableSlot("s-7", 7, 1, "Pazartesi", "14:25", "15:05", "Görsel Sanatlar", "GS", "Zeynep Hoca", "Sanat Atölyesi", 0xFFDB2777, class_name = className),
            TimetableSlot("s-8", 8, 1, "Pazartesi", "15:15", "15:55", "Beden Eğitimi & Spor", "BED", "Hakan Hoca", "Spor Salonu", 0xFFEA580C, class_name = className)
        )
        2 -> listOf(
            TimetableSlot("s-9", 1, 2, "Salı", "08:30", "09:10", "Türkçe", "TRK", "Fatma Öğretmen", "Derslik 101", 0xFF7C3AED, class_name = className),
            TimetableSlot("s-10", 2, 2, "Salı", "09:20", "10:00", "Türkçe", "TRK", "Fatma Öğretmen", "Derslik 101", 0xFF7C3AED, class_name = className),
            TimetableSlot("s-11", 3, 2, "Salı", "10:15", "10:55", "Sosyal Bilgiler", "SOS", "Murat Bey", "Derslik 101", 0xFF0284C7, class_name = className),
            TimetableSlot("s-12", 4, 2, "Salı", "11:05", "11:45", "Din Kültürü", "DİN", "Ali Hoca", "Derslik 101", 0xFF16A34A, class_name = className),
            TimetableSlot("s-13", 5, 2, "Salı", "12:45", "13:25", "Matematik", "MAT", "Mustafa Yılmaz", "Derslik 101", 0xFF2563EB, class_name = className),
            TimetableSlot("s-14", 6, 2, "Salı", "13:35", "14:15", "Fen Bilimleri", "FEN", "Ayşe Kaya", "Fen Laboratuvarı", 0xFF059669, class_name = className),
            TimetableSlot("s-15", 7, 2, "Salı", "14:25", "15:05", "Fen Bilimleri", "FEN", "Ayşe Kaya", "Fen Laboratuvarı", 0xFF059669, class_name = className),
            TimetableSlot("s-16", 8, 2, "Salı", "15:15", "15:55", "Müzik", "MÜZ", "Selin Öğretmen", "Müzik Odası", 0xFF9333EA, class_name = className)
        )
        3 -> listOf(
            TimetableSlot("s-17", 1, 3, "Çarşamba", "08:30", "09:10", "İngilizce", "ING", "John Smith", "Dil Atölyesi", 0xFFD97706, class_name = className),
            TimetableSlot("s-18", 2, 3, "Çarşamba", "09:20", "10:00", "İngilizce", "ING", "John Smith", "Dil Atölyesi", 0xFFD97706, class_name = className),
            TimetableSlot("s-19", 3, 3, "Çarşamba", "10:15", "10:55", "Matematik", "MAT", "Mustafa Yılmaz", "Derslik 101", 0xFF2563EB, class_name = className),
            TimetableSlot("s-20", 4, 3, "Çarşamba", "11:05", "11:45", "Matematik Uygulamaları", "MAT", "Mustafa Yılmaz", "Derslik 101", 0xFF2563EB, class_name = className),
            TimetableSlot("s-21", 5, 3, "Çarşamba", "12:45", "13:25", "Bilişim & Yazılım", "KOD", "Ahmet Hoca", "Bilişim Lab", 0xFF0D9488, class_name = className),
            TimetableSlot("s-22", 6, 3, "Çarşamba", "13:35", "14:15", "Türkçe", "TRK", "Fatma Öğretmen", "Derslik 101", 0xFF7C3AED, class_name = className),
            TimetableSlot("s-23", 7, 3, "Çarşamba", "14:25", "15:05", "Kulüp & Yetenek", "KLP", "Kulüp Rehberi", "Çok Amaçlı Salon", 0xFFCA8A04, class_name = className),
            TimetableSlot("s-24", 8, 3, "Çarşamba", "15:15", "15:55", "Rehberlik & Kariyer", "REH", "Rehber Öğretmen", "Derslik 101", 0xFF4F46E5, class_name = className)
        )
        4 -> listOf(
            TimetableSlot("s-25", 1, 4, "Perşembe", "08:30", "09:10", "Fen Bilimleri", "FEN", "Ayşe Kaya", "Fen Laboratuvarı", 0xFF059669, class_name = className),
            TimetableSlot("s-26", 2, 4, "Perşembe", "09:20", "10:00", "Fen Bilimleri", "FEN", "Ayşe Kaya", "Fen Laboratuvarı", 0xFF059669, class_name = className),
            TimetableSlot("s-27", 3, 4, "Perşembe", "10:15", "10:55", "Türkçe", "TRK", "Fatma Öğretmen", "Derslik 101", 0xFF7C3AED, class_name = className),
            TimetableSlot("s-28", 4, 4, "Perşembe", "11:05", "11:45", "Sosyal Bilgiler", "SOS", "Murat Bey", "Derslik 101", 0xFF0284C7, class_name = className),
            TimetableSlot("s-29", 5, 4, "Perşembe", "12:45", "13:25", "İngilizce", "ING", "John Smith", "Dil Atölyesi", 0xFFD97706, class_name = className),
            TimetableSlot("s-30", 6, 4, "Perşembe", "13:35", "14:15", "Matematik", "MAT", "Mustafa Yılmaz", "Derslik 101", 0xFF2563EB, class_name = className),
            TimetableSlot("s-31", 7, 4, "Perşembe", "14:25", "15:05", "Satranç & Akıl Oyunları", "STR", "Satranç Antrenörü", "Kütüphane", 0xFF0891B2, class_name = className),
            TimetableSlot("s-32", 8, 4, "Perşembe", "15:15", "15:55", "Drama & Hitabet", "DRM", "Tiyatro Kulübü", "Sahne Salonu", 0xFFBE185D, class_name = className)
        )
        5 -> listOf(
            TimetableSlot("s-33", 1, 5, "Cuma", "08:30", "09:10", "Matematik", "MAT", "Mustafa Yılmaz", "Derslik 101", 0xFF2563EB, class_name = className),
            TimetableSlot("s-34", 2, 5, "Cuma", "09:20", "10:00", "Türkçe", "TRK", "Fatma Öğretmen", "Derslik 101", 0xFF7C3AED, class_name = className),
            TimetableSlot("s-35", 3, 5, "Cuma", "10:15", "10:55", "Din Kültürü", "DİN", "Ali Hoca", "Derslik 101", 0xFF16A34A, class_name = className),
            TimetableSlot("s-36", 4, 5, "Cuma", "11:05", "11:45", "İngilizce", "ING", "John Smith", "Dil Atölyesi", 0xFFD97706, class_name = className),
            TimetableSlot("s-37", 5, 5, "Cuma", "12:45", "13:25", "Fen Bilimleri", "FEN", "Ayşe Kaya", "Fen Laboratuvarı", 0xFF059669, class_name = className),
            TimetableSlot("s-38", 6, 5, "Cuma", "13:35", "14:15", "Beden Eğitimi", "BED", "Hakan Hoca", "Spor Salonu", 0xFFEA580C, class_name = className),
            TimetableSlot("s-39", 7, 5, "Cuma", "14:25", "15:05", "Haftalık Değerlendirme", "DEG", "Sınıf Öğretmeni", "Derslik 101", 0xFF2563EB, class_name = className),
            TimetableSlot("s-40", 8, 5, "Cuma", "15:15", "15:55", "Hafta Sonu Kapanış", "KAP", "Tüm Sınıf", "Derslik 101", 0xFF475569, class_name = className)
        )
        else -> listOf(
            TimetableSlot("s-41", 1, 6, "Cumartesi", "09:30", "11:00", "LGS Soru Çözüm Kampı", "KMP", "Mustafa Yılmaz", "Konferans Salonu", 0xFF2563EB, class_name = className),
            TimetableSlot("s-42", 2, 6, "Cumartesi", "11:15", "12:45", "Robotik Yarışma Takımı", "KOD", "Ahmet Hoca", "Bilişim Lab", 0xFF0D9488, class_name = className)
        )
    }
}
