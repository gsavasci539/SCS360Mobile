package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppointmentType
import com.example.data.model.ParentTeacherAppointment
import com.example.data.model.TeacherAppointmentSlot
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onBookAppointment: (String, String, String, String, String, String, String) -> Unit,
    onUpdateStatus: (String, String, String?) -> Unit
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, APPROVED, COMPLETED
    var showBookDialog by remember { mutableStateOf(false) }
    var selectedAppointmentForDetail by remember { mutableStateOf<ParentTeacherAppointment?>(null) }
    var showFeedbackDialog by remember { mutableStateOf<ParentTeacherAppointment?>(null) }
    var feedbackText by remember { mutableStateOf("") }

    val filteredAppointments = remember(state.appointments, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> state.appointments.filter { it.status == "PENDING" }
            "APPROVED" -> state.appointments.filter { it.status == "APPROVED" }
            "COMPLETED" -> state.appointments.filter { it.status == "COMPLETED" }
            else -> state.appointments
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Veli - Öğretmen Randevuları", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Toplam ${state.appointments.size} randevu • ${state.appointments.count { it.status == "PENDING" }} onay bekliyor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("appointments_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showBookDialog = true }, modifier = Modifier.testTag("book_apt_top_btn")) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Randevu Al", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showBookDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Randevu Talep Et") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_book_appointment")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Tümü (${state.appointments.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Bekleyen (${state.appointments.count { it.status == "PENDING" }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AmberPrimary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "APPROVED",
                        onClick = { selectedFilter = "APPROVED" },
                        label = { Text("Onaylanan (${state.appointments.count { it.status == "APPROVED" }})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "COMPLETED",
                        onClick = { selectedFilter = "COMPLETED" },
                        label = { Text("Tamamlanan (${state.appointments.count { it.status == "COMPLETED" }})") }
                    )
                }
            }

            if (filteredAppointments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Bu filtrede randevu kaydı bulunmuyor",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Available Slots Summary Banner inside scrollable feed
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Açık Öğretmen Randevu Slotları",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "${state.appointmentSlots.count { !it.is_booked }} adet rezerve edilebilir müsait saat aralığı",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        RoleContextualAiCard(
                            screenName = "APPOINTMENTS",
                            state = state
                        )
                    }

                    items(filteredAppointments, key = { it.id }) { apt ->
                        AppointmentCard(
                            appointment = apt,
                            onCardClick = { selectedAppointmentForDetail = apt },
                            onApprove = { onUpdateStatus(apt.id, "APPROVED", null) },
                            onReject = { onUpdateStatus(apt.id, "REJECTED", null) },
                            onComplete = { onUpdateStatus(apt.id, "COMPLETED", null) },
                            onAddFeedback = {
                                showFeedbackDialog = apt
                                feedbackText = apt.teacher_feedback_notes ?: ""
                            },
                            onOpenMeet = {
                                apt.meeting_link?.let { link ->
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                    context.startActivity(intent)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Book Appointment Dialog
    if (showBookDialog) {
        BookAppointmentDialog(
            slots = state.appointmentSlots.filter { !it.is_booked },
            onDismiss = { showBookDialog = false },
            onConfirm = { teacherId, teacherName, subject, date, timeSlot, type, notes ->
                onBookAppointment(teacherId, teacherName, subject, date, timeSlot, type, notes)
                showBookDialog = false
            }
        )
    }

    // Feedback Dialog
    showFeedbackDialog?.let { apt ->
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = null },
            title = { Text("Görüşme Notu & Geri Bildirim") },
            text = {
                Column {
                    Text(
                        "${apt.parent_name} • ${apt.student_name} (${apt.class_name})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        label = { Text("Öğretmen Görüşme Özeti") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateStatus(apt.id, apt.status, feedbackText)
                        showFeedbackDialog = null
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = null }) {
                    Text("İptal")
                }
            }
        )
    }

    // Detail Dialog
    selectedAppointmentForDetail?.let { apt ->
        AlertDialog(
            onDismissRequest = { selectedAppointmentForDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Randevu Detayı")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Öğretmen: ${apt.teacher_name} (${apt.teacher_subject})", fontWeight = FontWeight.Bold)
                    Text("Veli: ${apt.parent_name} (${apt.parent_phone})")
                    Text("Öğrenci: ${apt.student_name} - ${apt.class_name}")
                    Text("Tarih & Saat: ${apt.date} • ${apt.time_slot}")
                    Text("Görüşme Türü: ${apt.appointment_type}")
                    if (!apt.location_room.isNullOrBlank()) {
                        Text("Konum: ${apt.location_room}", color = MaterialTheme.colorScheme.primary)
                    }
                    if (!apt.meeting_link.isNullOrBlank()) {
                        Text("Online Link: ${apt.meeting_link}", color = MaterialTheme.colorScheme.tertiary)
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("Veli Notu:", fontWeight = FontWeight.SemiBold)
                    Text(apt.parent_topic_notes, style = MaterialTheme.typography.bodyMedium)
                    if (!apt.teacher_feedback_notes.isNullOrBlank()) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Öğretmen Notu:", fontWeight = FontWeight.SemiBold, color = EmeraldPrimary)
                        Text(apt.teacher_feedback_notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedAppointmentForDetail = null }) {
                    Text("Kapat")
                }
            }
        )
    }
}

@Composable
fun AppointmentCard(
    appointment: ParentTeacherAppointment,
    onCardClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit,
    onAddFeedback: () -> Unit,
    onOpenMeet: () -> Unit
) {
    val statusColor = when (appointment.status) {
        "APPROVED" -> EmeraldPrimary
        "PENDING" -> AmberPrimary
        "REJECTED" -> CrimsonPrimary
        "COMPLETED" -> OceanPrimary
        else -> Color.Gray
    }

    val statusText = when (appointment.status) {
        "APPROVED" -> "Onaylandı"
        "PENDING" -> "Onay Bekliyor"
        "REJECTED" -> "Reddedildi"
        "COMPLETED" -> "Tamamlandı"
        else -> appointment.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            appointment.teacher_name.take(1),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = appointment.teacher_name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = appointment.teacher_subject,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("${appointment.date} • ${appointment.time_slot}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.weight(1f))
                Text("Öğrenci: ${appointment.student_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (appointment.parent_topic_notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Konu: ${appointment.parent_topic_notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Buttons
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (appointment.appointment_type == "ONLINE_MEET" && !appointment.meeting_link.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onOpenMeet,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Meet'e Katıl", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (appointment.status == "PENDING") {
                    FilledTonalButton(
                        onClick = onApprove,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmeraldPrimary.copy(alpha = 0.2f), contentColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Onayla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Reddet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else if (appointment.status == "APPROVED") {
                    FilledTonalButton(
                        onClick = onComplete,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tamamlandı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onAddFeedback, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.EditNote, contentDescription = "Not Ekle", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentDialog(
    slots: List<TeacherAppointmentSlot>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String) -> Unit
) {
    var selectedSlot by remember { mutableStateOf(slots.firstOrNull()) }
    var selectedType by remember { mutableStateOf("FACE_TO_FACE") } // FACE_TO_FACE, ONLINE_MEET, PHONE_CALL
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Yeni Görüşme Talebi", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Müsait Öğretmen & Saat Seçimi:", fontWeight = FontWeight.Medium, fontSize = 13.sp)

                if (slots.isEmpty()) {
                    Text("Şu an açık bir randevu aralığı bulunmuyor.", color = CrimsonPrimary, fontSize = 12.sp)
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(slots) { slot ->
                            val isSelected = selectedSlot?.id == slot.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.clickable { selectedSlot = slot }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(slot.teacher_name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(slot.subject, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${slot.date} • ${slot.time_slot}", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Text("Görüşme Türü:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppointmentType.values().forEach { type ->
                        val isSelected = selectedType == type.name
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type.name }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(type.iconEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(type.label.split(" ").first(), fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Görüşme Konusu & Veli Talebi") },
                    placeholder = { Text("Örn: LGS deneme netleri ve çalışma programı...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedSlot?.let { slot ->
                        onConfirm(
                            slot.teacher_id,
                            slot.teacher_name,
                            slot.subject,
                            slot.date,
                            slot.time_slot,
                            selectedType,
                            notes.ifBlank { "Genel durum değerlendirmesi" }
                        )
                    }
                },
                enabled = selectedSlot != null
            ) {
                Text("Talep Gönder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}
