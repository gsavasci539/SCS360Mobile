package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun DismissalScreen(
    students: List<Student>,
    authorizations: List<PickupAuthorization>,
    dismissalLogs: List<DismissalLog>,
    onCreateAuthorization: (PickupAuthorization) -> Unit,
    onCancelAuthorization: (String) -> Unit,
    onRecordHandover: (studentId: String, nationalId: String, notes: String) -> Unit,
    onNavigateToSecurity: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var hasWritePermission by remember { mutableStateOf(true) }
    var showCreateAuthDialog by remember { mutableStateOf(false) }

    // Handover form state
    var selectedStudentId by remember(students) { mutableStateOf(students.firstOrNull()?.id ?: "std-1") }
    var nationalIdInput by remember { mutableStateOf("12345678902") }
    var handoverNotes by remember { mutableStateOf("Kimlik kontrolü yapıldı, imza karşılığı teslim edildi.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeutralLightBackground)
    ) {
        // Header with Cross-Navigation to Gate Security & Read/Write Permission Toggle
        Surface(color = ScsPetrolGreen, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Öğrenci Teslim Masası (/dismissal)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "T.C. Kimlik doğrulamalı yetişkin teslimi ve tarih çakışma korumalı teslim yetkileri",
                            color = ScsMintPage,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onNavigateToSecurity,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.testTag("nav_to_gate_security_btn")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kapı Güvenliği", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Aktif Yetki: ${authorizations.count { it.status == "ACTIVE" }} • Bugün Teslim: ${dismissalLogs.count { it.status == "COMPLETED" }}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilterChip(
                        selected = hasWritePermission,
                        onClick = { hasWritePermission = !hasWritePermission },
                        label = {
                            Text(
                                if (hasWritePermission) "Yetki: dismissal.write (Tam)" else "Yetki: dismissal.read (Salt Okunur)",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ScsOrbitGold,
                            selectedLabelColor = ScsPetrolGreen
                        )
                    )
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Teslim Masası & Doğrulama (${dismissalLogs.size})", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Teslim Alma Yetkileri (${authorizations.size})", fontWeight = FontWeight.SemiBold) }
            )
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasWritePermission) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        "Yetkili Yetişkin Kimlik Doğrulaması & Teslim Kaydı",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        "Güvenlik Kuralı: Yanlış T.C. Kimlik numarası yalnızca isim benzerliğiyle kabul edilmez. Teslim alan kişi adı veritabanındaki aktif yetki kaydından sabitlenir.",
                                        fontSize = 12.sp,
                                        color = NeutralTextSecondary
                                    )

                                    // Student selector chips
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        students.take(4).forEach { std ->
                                            FilterChip(
                                                selected = selectedStudentId == std.id,
                                                onClick = { selectedStudentId = std.id },
                                                label = { Text("${std.full_name} (${std.class_name})", fontSize = 12.sp) }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = nationalIdInput,
                                        onValueChange = { nationalIdInput = it.filter { ch -> ch.isDigit() }.take(11) },
                                        label = { Text("Teslim Alan T.C. Kimlik No (11 Hane)") },
                                        modifier = Modifier.fillMaxWidth().testTag("dismissal_national_id_input"),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = handoverNotes,
                                        onValueChange = { handoverNotes = it },
                                        label = { Text("Teslim Açıklaması / Not") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )

                                    Button(
                                        onClick = {
                                            onRecordHandover(selectedStudentId, nationalIdInput.trim(), handoverNotes.trim())
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("verify_and_dismiss_btn")
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Kimliği Doğrula ve Öğrenciyi Teslim Et")
                                    }
                                }
                            }
                        }
                    } else {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = StatusWarningContainer),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, tint = StatusWarning)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Salt Okunur Mod (dismissal.read): Yeni teslim kaydı veya yetki iptali için yazma yetkisi (dismissal.write) gereklidir.",
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            "Gerçekleşen Teslim Kayıtları (UTC)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    items(dismissalLogs, key = { it.id }) { log ->
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
                                        Text("${log.student_name} (${log.class_name})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            "Teslim Alan: ${log.pickup_person_name} (${log.relation}) • TCKN: ${log.national_id_masked}",
                                            fontSize = 12.sp,
                                            color = NeutralTextSecondary
                                        )
                                    }
                                    Surface(
                                        color = if (log.status == "COMPLETED") StatusSuccessContainer else StatusErrorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (log.status == "COMPLETED") "TESLİM EDİLDİ" else "REDDEDİLDİ",
                                            color = if (log.status == "COMPLETED") StatusSuccess else StatusError,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Doğrulayan: ${log.verified_by} • Zaman: ${log.dismissed_at_utc}",
                                    fontSize = 11.sp,
                                    color = NeutralTextSecondary
                                )
                                if (log.notes.isNotBlank()) {
                                    Text(log.notes, fontSize = 12.sp, color = NeutralTextPrimary)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Öğrenci Teslim Alma Yetkileri",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (hasWritePermission) {
                                Button(
                                    onClick = { showCreateAuthDialog = true },
                                    modifier = Modifier.testTag("add_pickup_auth_btn")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Yeni Yetki Ekle")
                                }
                            }
                        }
                    }

                    items(authorizations, key = { it.id }) { auth ->
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
                                        Text(
                                            "${auth.authorized_person_name} (${auth.relation})",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            "Öğrenci: ${auth.student_name} (${auth.class_name}) • TCKN: ${auth.national_id} • Tel: ${auth.phone}",
                                            fontSize = 12.sp,
                                            color = NeutralTextSecondary
                                        )
                                    }
                                    Surface(
                                        color = if (auth.status == "ACTIVE") StatusSuccessContainer else NeutralLightSurfaceVariant,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (auth.status == "ACTIVE") "AKTİF" else "İPTAL EDİLDİ",
                                            color = if (auth.status == "ACTIVE") StatusSuccess else NeutralTextSecondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Geçerlilik: ${auth.valid_from} → ${auth.valid_until} • Kaydeden: ${auth.created_by}",
                                        fontSize = 11.sp,
                                        color = NeutralTextSecondary
                                    )
                                    if (hasWritePermission && auth.status == "ACTIVE") {
                                        TextButton(
                                            onClick = { onCancelAuthorization(auth.id) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
                                        ) {
                                            Text("Yetkiyi İptal Et (Geçmiş Korunur)", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateAuthDialog) {
        var personName by remember { mutableStateOf("") }
        var relation by remember { mutableStateOf("Anne / Baba / Yakın") }
        var nationalId by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("+90 532 ") }
        var validFrom by remember { mutableStateOf("2026-11-03") }
        var validUntil by remember { mutableStateOf("2027-06-30") }
        val targetStudent = students.find { it.id == selectedStudentId } ?: students.firstOrNull()

        AlertDialog(
            onDismissRequest = { showCreateAuthDialog = false },
            title = { Text("Yeni Teslim Alma Yetkisi Tanımla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Öğrenci: ${targetStudent?.full_name ?: "Ali Demir"} (${targetStudent?.class_name ?: "8-A"})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(value = personName, onValueChange = { personName = it }, label = { Text("Teslim Alacak Kişi Ad Soyad") }, singleLine = true)
                    OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("Yakınlık Derecesi") }, singleLine = true)
                    OutlinedTextField(
                        value = nationalId,
                        onValueChange = { nationalId = it.filter { ch -> ch.isDigit() }.take(11) },
                        label = { Text("T.C. Kimlik Numarası (11 Hane)") },
                        singleLine = true
                    )
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefon") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = validFrom,
                            onValueChange = { validFrom = it },
                            label = { Text("Başlangıç") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = validUntil,
                            onValueChange = { validUntil = it },
                            label = { Text("Bitiş") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (personName.isNotBlank() && nationalId.length == 11) {
                            onCreateAuthorization(
                                PickupAuthorization(
                                    id = "pauth-${System.currentTimeMillis()}",
                                    student_id = targetStudent?.id ?: "std-1",
                                    student_name = targetStudent?.full_name ?: "Ali Demir",
                                    class_name = targetStudent?.class_name ?: "8-A",
                                    authorized_person_name = personName.trim(),
                                    relation = relation.trim(),
                                    national_id = nationalId.trim(),
                                    phone = phone.trim(),
                                    valid_from = validFrom.trim(),
                                    valid_until = validUntil.trim(),
                                    status = "ACTIVE",
                                    created_by = "Okul İdaresi (Doğrulandı)",
                                    nonce = "nonce-${System.currentTimeMillis()}"
                                )
                            )
                            showCreateAuthDialog = false
                        }
                    }
                ) {
                    Text("Yetkiyi Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAuthDialog = false }) { Text("İptal") }
            }
        )
    }
}
