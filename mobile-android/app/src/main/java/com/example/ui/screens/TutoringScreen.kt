package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun TutoringScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit = {},
    onCreateSession: (TutoringSession) -> Unit = {}
) {
    val sessions = state.tutoringSessions
    val students = state.students
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    var localSessions by remember(sessions) { mutableStateOf(sessions) }
    var tutoringRequests by remember {
        mutableStateOf(
            listOf(
                TutoringRequest(
                    id = "treq-1",
                    student_id = "std-1",
                    student_name = "Ali Demir",
                    class_name = "8-A",
                    teacher_id = "usr-teacher",
                    teacher_name = "Ayşe Kaya",
                    subject = "Matematik",
                    topic = "Üslü ve Köklü İfadeler Soru Çözümü",
                    requested_date = "2026-11-06",
                    requested_time = "15:30 - 16:15",
                    status = "PENDING"
                ),
                TutoringRequest(
                    id = "treq-2",
                    student_id = "std-2",
                    student_name = "Zeynep Yılmaz",
                    class_name = "10-B",
                    teacher_id = null,
                    teacher_name = "Atanmamış (Havuz Talebi)",
                    subject = "Fizik",
                    topic = "Optik ve Mercekler Birebir Tekrar",
                    requested_date = "2026-11-07",
                    requested_time = "16:20 - 17:00",
                    status = "APPROVED",
                    decided_by = "Zümre Başkanı",
                    decided_at = "2026-11-03 10:15 UTC"
                )
            )
        )
    }
    var availabilitySlots by remember {
        mutableStateOf(
            listOf(
                TeacherAvailabilitySlot(
                    id = "tslot-1",
                    teacher_name = "Ayşe Kaya",
                    subject = "Matematik",
                    day_of_week = "Perşembe",
                    time_slot = "15:30 - 16:15",
                    room_name = "Etüt Odası B-1",
                    capacity = 1,
                    is_open = true
                ),
                TeacherAvailabilitySlot(
                    id = "tslot-2",
                    teacher_name = "Mehmet Öz",
                    subject = "Fen Bilimleri",
                    day_of_week = "Cuma",
                    time_slot = "16:20 - 17:00",
                    room_name = "Laboratuvar Etüt 2",
                    capacity = 2,
                    is_open = true
                )
            )
        )
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                text = { Text("Etüt Planla", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_tutoring_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NeutralLightBackground)
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Etüt Dersleri (${localSessions.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Talepler & Karar (${tutoringRequests.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Müsaitlik (${availabilitySlots.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ScsContainerLight),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Tamamlanmış (COMPLETED) etüt dersinin zamanı değiştirilemez. İptal edilen dersler geçmişten silinmez, CANCELLED statüsüyle saklanır.",
                                    fontSize = 12.sp,
                                    color = OnScsContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        items(localSessions, key = { it.id }) { session ->
                            val statusColor = when (session.status) {
                                "COMPLETED" -> StatusSuccess
                                "CANCELLED" -> StatusError
                                else -> PrimaryBlue
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(statusColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.MenuBook,
                                                    contentDescription = null,
                                                    tint = statusColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${session.subject} • ${session.topic}",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Öğrenci: ${session.student_name} • Öğrt: ${session.teacher_name}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeutralTextSecondary
                                                )
                                            }
                                        }
                                        Surface(
                                            color = statusColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = when (session.status) {
                                                    "COMPLETED" -> "Tamamlandı (Kilitli)"
                                                    "CANCELLED" -> "İptal (Geçmiş Korundu)"
                                                    else -> "Planlandı"
                                                },
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${session.date} • ${session.start_time}-${session.end_time} • ${session.room}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = NeutralTextPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (session.status == "SCHEDULED") {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        localSessions = localSessions.map {
                                                            if (it.id == session.id) it.copy(status = "COMPLETED") else it
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text("Yoklama & Tamamla", fontSize = 11.sp)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        localSessions = localSessions.map {
                                                            if (it.id == session.id) it.copy(status = "CANCELLED") else it
                                                        }
                                                    }
                                                ) {
                                                    Text("İptal", fontSize = 11.sp, color = StatusError)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StatusInfoContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Talep onayı ile ders oluşturma ayrı işlemlerdir. Atanmamış talebin iki öğretmen tarafından aynı anda alınması ve çakışmalar engellenir.",
                                    fontSize = 12.sp,
                                    color = NeutralTextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        items(tutoringRequests, key = { it.id }) { req ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${req.student_name} (${req.class_name}) • ${req.subject}", fontWeight = FontWeight.Bold)
                                        Text(req.status, fontWeight = FontWeight.Bold, color = ScsPetrolGreen, fontSize = 12.sp)
                                    }
                                    Text("Konu: ${req.topic}", fontSize = 12.sp)
                                    Text("Talep Zamanı: ${req.requested_date} • ${req.requested_time} • Öğretmen: ${req.teacher_name}", fontSize = 12.sp, color = NeutralTextSecondary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (req.status == "PENDING") {
                                            Button(
                                                onClick = {
                                                    tutoringRequests = tutoringRequests.map {
                                                        if (it.id == req.id) it.copy(status = "APPROVED", decided_by = "Ayşe Kaya", decided_at = "2026-11-03 11:20 UTC") else it
                                                    }
                                                }
                                            ) {
                                                Text("1. Talebi Onayla", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    tutoringRequests = tutoringRequests.map {
                                                        if (it.id == req.id) it.copy(status = "REJECTED", decided_by = "Ayşe Kaya", decided_at = "2026-11-03 11:20 UTC") else it
                                                    }
                                                }
                                            ) {
                                                Text("Reddet", fontSize = 12.sp, color = StatusError)
                                            }
                                        } else if (req.status == "APPROVED") {
                                            Button(
                                                onClick = {
                                                    val created = TutoringSession(
                                                        id = "tut-${System.currentTimeMillis()}",
                                                        student_id = req.student_id,
                                                        student_name = req.student_name,
                                                        teacher_name = req.teacher_name,
                                                        subject = req.subject,
                                                        topic = req.topic,
                                                        date = req.requested_date,
                                                        start_time = "15:30",
                                                        end_time = "16:15",
                                                        room = "Etüt Odası B-1",
                                                        status = "SCHEDULED"
                                                    )
                                                    onCreateSession(created)
                                                    localSessions = listOf(created) + localSessions
                                                    tutoringRequests = tutoringRequests.map {
                                                        if (it.id == req.id) it.copy(status = "SCHEDULED") else it
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ScsAccentGreen)
                                            ) {
                                                Text("2. Etüt Dersine Dönüştür / Planla", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availabilitySlots, key = { it.id }) { slot ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${slot.teacher_name} (${slot.subject})", fontWeight = FontWeight.Bold)
                                        Text("${slot.day_of_week} • ${slot.time_slot} • ${slot.room_name} (Kapasite: ${slot.capacity})", fontSize = 12.sp, color = NeutralTextSecondary)
                                    }
                                    Switch(
                                        checked = slot.is_open,
                                        onCheckedChange = { checked ->
                                            availabilitySlots = availabilitySlots.map {
                                                if (it.id == slot.id) it.copy(is_open = checked) else it
                                            }
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

    if (showAddDialog) {
        var subject by remember { mutableStateOf("Matematik") }
        var topic by remember { mutableStateOf("") }
        var teacherName by remember { mutableStateOf("Ayşe Kaya") }
        var startTime by remember { mutableStateOf("15:30") }
        var endTime by remember { mutableStateOf("16:15") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Birebir Etüt Planla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Ders / Branş") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Konu Başlığı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = teacherName,
                        onValueChange = { teacherName = it },
                        label = { Text("Öğretmen Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Başlangıç") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("Bitiş") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (topic.isNotBlank()) {
                            val std = students.firstOrNull()
                            val newSession = TutoringSession(
                                id = "tut-${System.currentTimeMillis()}",
                                student_id = std?.id ?: "std-1",
                                student_name = std?.full_name ?: "Ali Demir",
                                teacher_name = teacherName.trim(),
                                subject = subject.trim(),
                                topic = topic.trim(),
                                start_time = startTime.trim(),
                                end_time = endTime.trim(),
                                status = "SCHEDULED"
                            )
                            onCreateSession(newSession)
                            localSessions = listOf(newSession) + localSessions
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Planla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
