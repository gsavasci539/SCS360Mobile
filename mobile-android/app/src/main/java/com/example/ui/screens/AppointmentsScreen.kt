package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.ParentTeacherAppointment
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onBookAppointment: (() -> Unit)?,
    onUpdateStatus: (String, String, String?) -> Unit,
    canManage: Boolean,
    canCancel: Boolean,
    busy: Boolean,
    loaded: Boolean,
    failure: String,
    onReload: () -> Unit,
    onCheckResult: (() -> Unit)?,
    onClearPending: () -> Unit,
    savedRevision: Int,
    onManageSlots: (() -> Unit)? = null,
    onDeleteAppointment: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, APPROVED, COMPLETED
    var selectedAppointmentForDetail by remember { mutableStateOf<ParentTeacherAppointment?>(null) }
    var deleteCandidate by remember { mutableStateOf<ParentTeacherAppointment?>(null) }
    var showFeedbackDialog by remember { mutableStateOf<ParentTeacherAppointment?>(null) }
    var feedbackText by remember { mutableStateOf("") }
    LaunchedEffect(savedRevision) { if (savedRevision > 0) showFeedbackDialog = null }

    val filteredAppointments = remember(state.appointments, selectedFilter) {
        when (selectedFilter) {
            "PENDING" -> state.appointments.filter { it.status == "PENDING" }
            "APPROVED" -> state.appointments.filter { it.status in setOf("APPROVED", "CONFIRMED") }
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
                    if (onManageSlots != null) IconButton(onClick = onManageSlots,
                        enabled = !busy && onCheckResult == null) {
                        Icon(Icons.Default.Schedule, contentDescription = "Uygunluk saatlerim")
                    }
                    if (onBookAppointment != null) IconButton(enabled = loaded && !busy && onCheckResult == null,
                        onClick = onBookAppointment, modifier = Modifier.testTag("book_apt_top_btn")) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Randevu Al", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (onBookAppointment != null && loaded && !busy && onCheckResult == null) ExtendedFloatingActionButton(
                onClick = onBookAppointment,
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
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (failure.isNotBlank()) Text(failure, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
            TextButton(enabled = !busy, onClick = onCheckResult ?: onReload, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(if (onCheckResult == null) "Listeyi yenile" else "Sonucu kontrol et")
            }
            if (onCheckResult != null) {
                TextButton(enabled = !busy, onClick = onClearPending, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Text("Kontrolü kapat")
                }
            }
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
                        label = { Text("Onaylanan (${state.appointments.count { it.status in setOf("APPROVED", "CONFIRMED") }})") },
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

            if (!loaded) {
                if (busy) Text("Randevular yükleniyor", Modifier.padding(16.dp))
            } else if (filteredAppointments.isEmpty()) {
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
                    items(filteredAppointments, key = { it.id }) { apt ->
                        AppointmentCard(
                            appointment = apt,
                            onCardClick = { selectedAppointmentForDetail = apt },
                            canManage = canManage && !busy && onCheckResult == null,
                            canCancel = canCancel && !busy && onCheckResult == null,
                            onApprove = { onUpdateStatus(apt.id, "APPROVED", null) },
                            onReject = { onUpdateStatus(apt.id, "REJECTED", null) },
                            onComplete = { onUpdateStatus(apt.id, "COMPLETED", null) },
                            onCancel = { onUpdateStatus(apt.id, "CANCELLED", null) },
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

    // Feedback Dialog
    showFeedbackDialog?.let { apt ->
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = null },
            title = { Text("Görüşme Notu & Geri Bildirim") },
            text = {
                Column {
                    Text(
                        apt.student_name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        enabled = !busy && onCheckResult == null,
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
                    enabled = !busy,
                    onClick = {
                        if (onCheckResult != null) onCheckResult() else onUpdateStatus(apt.id, apt.status, feedbackText)
                    }
                ) {
                    Text(if (onCheckResult == null) "Kaydet" else "Sonucu kontrol et")
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
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Öğretmen: ${apt.teacher_name}", fontWeight = FontWeight.Bold)
                    Text("Öğrenci: ${apt.student_name}")
                    Text("Tarih ve saat: ${apt.date} • ${apt.time_slot}")
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
            },
            dismissButton = {
                if (onDeleteAppointment != null) {
                    TextButton(enabled = !busy && onCheckResult == null, onClick = {
                        selectedAppointmentForDetail = null
                        deleteCandidate = apt
                    }) { Text("Randevuyu sil") }
                }
            }
        )
    }
    deleteCandidate?.let { appointment ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Randevu silinsin mi?") },
            text = { Text("${appointment.student_name} • ${appointment.date} ${appointment.time_slot}") },
            confirmButton = {
                TextButton(enabled = onDeleteAppointment != null && !busy && onCheckResult == null, onClick = {
                    deleteCandidate = null
                    onDeleteAppointment?.invoke(appointment.id)
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Vazgeç") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppointmentCard(
    appointment: ParentTeacherAppointment,
    canManage: Boolean,
    canCancel: Boolean,
    onCardClick: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    onAddFeedback: () -> Unit,
    onOpenMeet: () -> Unit
) {
    val statusColor = when (appointment.status) {
        "APPROVED", "CONFIRMED" -> EmeraldPrimary
        "PENDING" -> AmberPrimary
        "REJECTED" -> CrimsonPrimary
        "COMPLETED" -> OceanPrimary
        else -> Color.Gray
    }

    val statusText = when (appointment.status) {
        "APPROVED", "CONFIRMED" -> "Onaylandı"
        "PENDING" -> "Onay Bekliyor"
        "REJECTED" -> "Reddedildi"
        "COMPLETED" -> "Tamamlandı"
        "CANCELLED" -> "İptal edildi"
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
            }
            Text("Öğrenci: ${appointment.student_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (appointment.appointment_type == "ONLINE_MEET" && !appointment.meeting_link.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = onOpenMeet,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Meet'e Katıl", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                if (canManage && appointment.status == "PENDING") {
                    FilledTonalButton(
                        onClick = onApprove,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmeraldPrimary.copy(alpha = 0.2f), contentColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Onayla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = onReject,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Reddet", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else if (canManage && appointment.status in setOf("APPROVED", "CONFIRMED")) {
                    FilledTonalButton(
                        onClick = onComplete,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tamamlandı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onAddFeedback, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.EditNote, contentDescription = "Not Ekle", tint = MaterialTheme.colorScheme.primary)
                    }
                } else if (canCancel && appointment.status in setOf("PENDING", "APPROVED", "CONFIRMED")) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = 48.dp)) { Text("İptal et") }
                }
            }
        }
    }
}
