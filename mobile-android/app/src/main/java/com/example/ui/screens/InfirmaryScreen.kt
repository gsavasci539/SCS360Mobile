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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InfirmaryScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit = {},
    onCreateVisit: (InfirmaryVisit) -> Unit = {}
) {
    val visits = state.infirmaryVisits
    val students = state.students
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddVisitDialog by remember { mutableStateOf(false) }
    var showUpsertHealthFileDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var showRecordDoseDialog by remember { mutableStateOf(false) }

    var closedFollowUpVisitIds by remember { mutableStateOf(setOf<String>()) }
    var healthFiles by remember {
        mutableStateOf(
            listOf(
                StudentHealthFile(
                    id = "hf-1",
                    student_id = "std-1",
                    student_name = "Ali Demir",
                    class_name = "8-A",
                    blood_type = "A Rh+",
                    chronic_conditions = "Mevsimsel alerjik rinit",
                    allergies = "Penisilin, Fıstık",
                    emergency_contact_name = "Mehmet Demir",
                    emergency_contact_phone = "+90 532 111 2233",
                    insurance_info = "SGK + Özel Tamamlayıcı Sağlık",
                    updated_at = "2026-10-12"
                ),
                StudentHealthFile(
                    id = "hf-2",
                    student_id = "std-2",
                    student_name = "Zeynep Yılmaz",
                    class_name = "10-B",
                    blood_type = "0 Rh+",
                    chronic_conditions = "Astım başlangıcı",
                    allergies = "Toz poleni",
                    emergency_contact_name = "Fatma Yılmaz",
                    emergency_contact_phone = "+90 533 222 3344",
                    insurance_info = "SGK Genel Sağlık Sigortası",
                    updated_at = "2026-10-18"
                )
            )
        )
    }
    var medicationSchedules by remember {
        mutableStateOf(
            listOf(
                MedicationSchedule(
                    id = "msched-1",
                    student_id = "std-1",
                    student_name = "Ali Demir",
                    class_name = "8-A",
                    medication_name = "Ventolin İnhaler / Şurup",
                    dosage = "1 Ölçek (5 ml)",
                    planned_times = listOf("12:45"),
                    start_date = "2026-11-01",
                    end_date = "2026-11-15",
                    approval_status = "APPROVED",
                    approved_by = "Hemşire Seda Yılmaz",
                    parent_notes = "Öğle yemeği sonrası tok karnına."
                ),
                MedicationSchedule(
                    id = "msched-2",
                    student_id = "std-2",
                    student_name = "Zeynep Yılmaz",
                    class_name = "10-B",
                    medication_name = "Aerius 2.5mg Şurup",
                    dosage = "1 Kaşık (5 ml)",
                    planned_times = listOf("10:30", "15:30"),
                    start_date = "2026-11-03",
                    end_date = "2026-11-10",
                    approval_status = "PENDING",
                    approved_by = null,
                    parent_notes = "Veli portalından yeni talep edildi."
                )
            )
        )
    }
    var doseLogs by remember {
        mutableStateOf(
            listOf(
                MedicationDoseLog(
                    id = "mdose-1",
                    schedule_id = "msched-1",
                    student_id = "std-1",
                    student_name = "Ali Demir",
                    medication_name = "Ventolin İnhaler / Şurup",
                    scheduled_date = "2026-11-03",
                    planned_time = "12:45",
                    status = "ADMINISTERED",
                    administered_by = "Hemşire Seda Yılmaz",
                    administered_at_utc = "2026-11-03 09:47 UTC",
                    note = "Sorunsuz içildi, veli bilgi notu işlendi."
                )
            )
        )
    }

    val filteredVisits = remember(visits, searchQuery) {
        visits.filter {
            searchQuery.isBlank() ||
                it.student_name.contains(searchQuery, ignoreCase = true) ||
                it.complaint.contains(searchQuery, ignoreCase = true) ||
                it.class_name.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showAddVisitDialog = true
                        1 -> showUpsertHealthFileDialog = true
                        2 -> showAddScheduleDialog = true
                        3 -> showRecordDoseDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                text = {
                    Text(
                        when (selectedTab) {
                            0 -> "Revir Ziyareti Ekle"
                            1 -> "Sağlık Dosyası Güncelle"
                            2 -> "İlaç Programı Ekle"
                            else -> "Günlük Doz İşle"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.testTag("add_infirmary_visit_fab")
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NeutralLightBackground)
                .padding(paddingValues)
        ) {
            Surface(color = Color.White, shadowElevation = 1.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InfirmarySummaryBox("Ziyaret", "${visits.size}", StatusError, Modifier.weight(1f))
                        InfirmarySummaryBox("Sağlık Dosyası", "${healthFiles.size}", PrimaryBlue, Modifier.weight(1f))
                        InfirmarySummaryBox("İlaç Programı", "${medicationSchedules.size}", AccentAmber, Modifier.weight(1f))
                        InfirmarySummaryBox("Doz Kaydı", "${doseLogs.size}", StatusSuccess, Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Öğrenci, sınıf veya şikayet ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Ziyaretler (${visits.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Sağlık Dosyası (${healthFiles.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("İlaç Programı (${medicationSchedules.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Doz Kaydı (${doseLogs.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredVisits, key = { it.id }) { visit ->
                            val followUpClosed = closedFollowUpVisitIds.contains(visit.id)
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
                                                    .background(StatusError.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.LocalHospital,
                                                    contentDescription = null,
                                                    tint = StatusError,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${visit.student_name} (${visit.class_name})",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${visit.visit_time} • Çıkış: ${visit.discharged_to}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeutralTextSecondary
                                                )
                                            }
                                        }
                                        Surface(
                                            color = StatusError.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${visit.fever_temperature}°C",
                                                color = StatusError,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Şikayet: ${visit.complaint}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NeutralTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Müdahale: ${visit.intervention_treatment}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeutralTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (visit.parent_notified) "Veli Bilgilendirildi (Manuel Not)" else "Veli Bilgilendirme Bekliyor",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (visit.parent_notified) StatusSuccess else AccentAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (followUpClosed) {
                                            Surface(color = StatusSuccessContainer, shape = RoundedCornerShape(6.dp)) {
                                                Text(
                                                    "TAKİP KAPATILDI",
                                                    color = StatusSuccess,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = { closedFollowUpVisitIds = closedFollowUpVisitIds + visit.id },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Takibi Kapat", fontSize = 11.sp)
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
                                colors = CardDefaults.cardColors(containerColor = ScsContainerLight),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Sağlık dosyası oluşturma/güncelleme sırasında sigorta bilgisi ve mevcut alanlar boş bırakılsa dahi önceki veriler korunur.",
                                    fontSize = 12.sp,
                                    color = OnScsContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        items(healthFiles, key = { it.id }) { hf ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("${hf.student_name} (${hf.class_name})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Surface(color = StatusErrorContainer, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                hf.blood_type,
                                                color = StatusError,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text("Kronik Durum: ${hf.chronic_conditions}", fontSize = 12.sp)
                                    Text("Alerjiler: ${hf.allergies}", fontSize = 12.sp, color = StatusError)
                                    Text("Sigorta Bilgisi: ${hf.insurance_info}", fontSize = 12.sp, color = NeutralTextSecondary)
                                    Text("Acil İletişim: ${hf.emergency_contact_name} (${hf.emergency_contact_phone})", fontSize = 11.sp, color = NeutralTextSecondary)
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
                        items(medicationSchedules, key = { it.id }) { sched ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${sched.student_name} • ${sched.medication_name}", fontWeight = FontWeight.Bold)
                                            Text(
                                                "Doz: ${sched.dosage} • Saatler: ${sched.planned_times.joinToString(", ")} • ${sched.start_date} → ${sched.end_date}",
                                                fontSize = 12.sp,
                                                color = NeutralTextSecondary
                                            )
                                        }
                                        Surface(
                                            color = when (sched.approval_status) {
                                                "APPROVED" -> StatusSuccessContainer
                                                "REJECTED" -> StatusErrorContainer
                                                else -> StatusWarningContainer
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                sched.approval_status,
                                                color = when (sched.approval_status) {
                                                    "APPROVED" -> StatusSuccess
                                                    "REJECTED" -> StatusError
                                                    else -> StatusWarning
                                                },
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Veli Notu: ${sched.parent_notes}", fontSize = 12.sp)
                                    if (sched.approval_status == "PENDING") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    medicationSchedules = medicationSchedules.map {
                                                        if (it.id == sched.id) it.copy(approval_status = "APPROVED", approved_by = "Hemşire Seda Yılmaz") else it
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = StatusSuccess)
                                            ) {
                                                Text("Veli İlaç Talebini Onayla", fontSize = 12.sp)
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    medicationSchedules = medicationSchedules.map {
                                                        if (it.id == sched.id) it.copy(approval_status = "REJECTED", approved_by = "Hemşire Seda Yılmaz") else it
                                                    }
                                                }
                                            ) {
                                                Text("Reddet", fontSize = 12.sp, color = StatusError)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
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
                                    "İlaç doz kayıtları (scheduled_date + planned_time) anahtarıyla tutulur; farklı günlerdeki dozlar birbirinin üzerine yazmaz.",
                                    fontSize = 12.sp,
                                    color = NeutralTextPrimary,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        items(doseLogs, key = { it.id }) { dose ->
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
                                        Text("${dose.student_name} • ${dose.medication_name}", fontWeight = FontWeight.Bold)
                                        Text("${dose.scheduled_date} (${dose.planned_time})", fontWeight = FontWeight.Bold, color = ScsPetrolGreen, fontSize = 12.sp)
                                    }
                                    Text(
                                        "Durum: ${dose.status} • Uygulayan: ${dose.administered_by} • ${dose.administered_at_utc}",
                                        fontSize = 12.sp,
                                        color = NeutralTextSecondary
                                    )
                                    if (dose.note.isNotBlank()) {
                                        Text(dose.note, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddVisitDialog) {
        val std = students.firstOrNull()
        var complaint by remember { mutableStateOf("") }
        var treatment by remember { mutableStateOf("") }
        var temperature by remember { mutableStateOf("36.8") }
        var notifyParentNote by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddVisitDialog = false },
            title = { Text("Yeni Revir Muayene Kaydı", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = complaint,
                        onValueChange = { complaint = it },
                        label = { Text("Şikayet / Belirti (Örn: Baş ağrısı)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = treatment,
                        onValueChange = { treatment = it },
                        label = { Text("Uygulanan Müdahale / İlaç") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Ateş Ölçümü (°C)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = notifyParentNote, onCheckedChange = { notifyParentNote = it })
                        Text("Veli telefonla bilgilendirildi notu düş (Sahte SMS gönderilmez)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (complaint.isNotBlank()) {
                            onCreateVisit(
                                InfirmaryVisit(
                                    id = "inf-${System.currentTimeMillis()}",
                                    student_id = std?.id ?: "std-1",
                                    student_name = std?.full_name ?: "Ali Demir",
                                    class_name = std?.class_name ?: "8-A",
                                    visit_time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                                    complaint = complaint.trim(),
                                    fever_temperature = temperature.toDoubleOrNull() ?: 36.8,
                                    intervention_treatment = treatment.ifBlank { "İstirahat ve gözlem" },
                                    parent_notified = notifyParentNote,
                                    discharged_to = "CLASSROOM"
                                )
                            )
                            showAddVisitDialog = false
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddVisitDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    if (showUpsertHealthFileDialog) {
        var bloodType by remember { mutableStateOf("A Rh+") }
        var chronic by remember { mutableStateOf("") }
        var allergies by remember { mutableStateOf("") }
        var insurance by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showUpsertHealthFileDialog = false },
            title = { Text("Öğrenci Sağlık Dosyası Güncelle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = bloodType, onValueChange = { bloodType = it }, label = { Text("Kan Grubu") }, singleLine = true)
                    OutlinedTextField(value = chronic, onValueChange = { chronic = it }, label = { Text("Kronik Rahatsızlık") }, singleLine = true)
                    OutlinedTextField(value = allergies, onValueChange = { allergies = it }, label = { Text("Alerjiler") }, singleLine = true)
                    OutlinedTextField(value = insurance, onValueChange = { insurance = it }, label = { Text("Sigorta Bilgisi (Boşsa mevcut korunur)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val existing = healthFiles.firstOrNull()
                        val updated = StudentHealthFile(
                            id = existing?.id ?: "hf-${System.currentTimeMillis()}",
                            student_id = existing?.student_id ?: "std-1",
                            student_name = existing?.student_name ?: "Ali Demir",
                            class_name = existing?.class_name ?: "8-A",
                            blood_type = bloodType.ifBlank { existing?.blood_type ?: "A Rh+" },
                            chronic_conditions = chronic.ifBlank { existing?.chronic_conditions ?: "Yok" },
                            allergies = allergies.ifBlank { existing?.allergies ?: "Yok" },
                            insurance_info = insurance.ifBlank { existing?.insurance_info ?: "SGK" },
                            updated_at = "2026-11-03"
                        )
                        healthFiles = listOf(updated) + healthFiles.drop(1)
                        showUpsertHealthFileDialog = false
                    }
                ) {
                    Text("Dosyayı Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpsertHealthFileDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showAddScheduleDialog) {
        var medName by remember { mutableStateOf("") }
        var dosage by remember { mutableStateOf("1 Ölçek (5 ml)") }
        var plannedTime by remember { mutableStateOf("13:00") }

        AlertDialog(
            onDismissRequest = { showAddScheduleDialog = false },
            title = { Text("Yeni İlaç Programı Tanımla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = medName, onValueChange = { medName = it }, label = { Text("İlaç Adı") }, singleLine = true)
                    OutlinedTextField(value = dosage, onValueChange = { dosage = it }, label = { Text("Dozaj") }, singleLine = true)
                    OutlinedTextField(value = plannedTime, onValueChange = { plannedTime = it }, label = { Text("Planlanan Saat (SS:DD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (medName.isNotBlank()) {
                            medicationSchedules = listOf(
                                MedicationSchedule(
                                    id = "msched-${System.currentTimeMillis()}",
                                    medication_name = medName.trim(),
                                    dosage = dosage.trim(),
                                    planned_times = listOf(plannedTime.trim()),
                                    approval_status = "APPROVED"
                                )
                            ) + medicationSchedules
                            showAddScheduleDialog = false
                        }
                    }
                ) {
                    Text("Programı Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddScheduleDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showRecordDoseDialog) {
        var scheduledDate by remember { mutableStateOf("2026-11-03") }
        var plannedTime by remember { mutableStateOf("12:45") }
        var note by remember { mutableStateOf("Zamanında uygulandı.") }

        AlertDialog(
            onDismissRequest = { showRecordDoseDialog = false },
            title = { Text("Günlük İlaç Dozu İşle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = scheduledDate, onValueChange = { scheduledDate = it }, label = { Text("Planlanan Tarih (YYYY-MM-DD)") }, singleLine = true)
                    OutlinedTextField(value = plannedTime, onValueChange = { plannedTime = it }, label = { Text("Planlanan Saat (SS:DD)") }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Hemşire Notu") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sched = medicationSchedules.firstOrNull()
                        doseLogs = listOf(
                            MedicationDoseLog(
                                id = "mdose-${System.currentTimeMillis()}",
                                schedule_id = sched?.id ?: "msched-1",
                                student_id = sched?.student_id ?: "std-1",
                                student_name = sched?.student_name ?: "Ali Demir",
                                medication_name = sched?.medication_name ?: "Ventolin Şurup",
                                scheduled_date = scheduledDate.trim(),
                                planned_time = plannedTime.trim(),
                                status = "ADMINISTERED",
                                administered_by = "Hemşire Seda Yılmaz",
                                administered_at_utc = "2026-11-03 10:00 UTC",
                                note = note.trim()
                            )
                        ) + doseLogs
                        showRecordDoseDialog = false
                    }
                ) {
                    Text("Dozu Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRecordDoseDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun InfirmarySummaryBox(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, fontSize = 10.sp, color = NeutralTextSecondary, maxLines = 1)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = color)
        }
    }
}
