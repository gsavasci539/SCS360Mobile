package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
fun SecurityAuditScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit = {},
    onVerifyGateAccess: (studentId: String, direction: String, method: String, codeInput: String) -> Unit = { _, _, _, _ -> },
    onIssueIdCard: (studentId: String) -> Unit = {},
    onRevokeIdCard: (cardId: String) -> Unit = {},
    onDownloadQrSvg: (qrRecordId: String) -> Unit = {},
    onNavigateToDismissal: () -> Unit = {}
) {
    SecurityAuditScreen(
        logs = state.auditLogs,
        gateLogs = state.gateAccessLogs,
        idCards = state.studentIdCards,
        students = state.students,
        onVerifyGateAccess = onVerifyGateAccess,
        onIssueIdCard = onIssueIdCard,
        onRevokeIdCard = onRevokeIdCard,
        onDownloadQrSvg = onDownloadQrSvg,
        onNavigateToDismissal = onNavigateToDismissal
    )
}

@Composable
fun SecurityAuditScreen(
    logs: List<AuditLogEntry>,
    gateLogs: List<GateAccessLog> = emptyList(),
    idCards: List<StudentIdCardRecord> = emptyList(),
    students: List<Student> = emptyList(),
    onVerifyGateAccess: (studentId: String, direction: String, method: String, codeInput: String) -> Unit = { _, _, _, _ -> },
    onIssueIdCard: (studentId: String) -> Unit = {},
    onRevokeIdCard: (cardId: String) -> Unit = {},
    onDownloadQrSvg: (qrRecordId: String) -> Unit = {},
    onNavigateToDismissal: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedModule by remember { mutableStateOf("ALL") }

    // Gate verification form state
    var selectedStudentId by remember(students) { mutableStateOf(students.firstOrNull()?.id ?: "std-1") }
    var selectedDirection by remember { mutableStateOf("IN") }
    var selectedMethod by remember { mutableStateOf("QR") }
    var codeInput by remember { mutableStateOf("SCS360-8A-1042-9F4A") }

    // Calculate inside-campus student count from each student's latest gate movement
    val latestMovementByStudent = remember(gateLogs) {
        gateLogs.groupBy { it.student_id }.mapValues { (_, list) -> list.first().direction }
    }
    val insideCampusCount = latestMovementByStudent.values.count { it == "IN" }
    val anomalyReviewCount = gateLogs.count { it.requires_anomaly_review }

    val filteredAuditLogs = remember(logs, searchQuery, selectedModule) {
        logs.filter { log ->
            val matchesSearch = searchQuery.isBlank() ||
                log.user_name.contains(searchQuery, ignoreCase = true) ||
                log.details.contains(searchQuery, ignoreCase = true) ||
                log.ip_address.contains(searchQuery, ignoreCase = true) ||
                log.action.contains(searchQuery, ignoreCase = true)
            val matchesModule = selectedModule == "ALL" || log.module.equals(selectedModule, ignoreCase = true)
            matchesSearch && matchesModule
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeutralLightBackground)
    ) {
        // Top Header with Live Campus Gate Metrics & Cross-Link to Dismissal Desk
        Surface(color = ScsPetrolGreen, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Kapı Güvenliği, Kimlik & Denetim Merkezi",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "QR / SCS360 Barkod / Manuel doğrulama, içerideki öğrenci hesabı ve KVKK logları",
                            color = ScsMintPage,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = onNavigateToDismissal,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScsOrbitGold,
                            contentColor = ScsPetrolGreen
                        ),
                        modifier = Modifier.testTag("nav_to_dismissal_desk_btn")
                    ) {
                        Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Teslim Masası", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GateMetricCard("Kampüs İçinde", "$insideCampusCount Öğrenci", ScsMintPage, Modifier.weight(1f))
                    GateMetricCard("Anomali İnceleme", "$anomalyReviewCount Kayıt", ScsOrbitGold, Modifier.weight(1f))
                    GateMetricCard("Aktif Kimlik", "${idCards.count { it.status == "ACTIVE" }} Kart", Color.White, Modifier.weight(1f))
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Kapı Geçişleri (${gateLogs.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Kimlik & Barkod (${idCards.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Denetim Logları (${logs.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
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
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Turnike & Kapı Geçiş Doğrulaması (UTC)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "Yeni kapı geçiş kaydı otomatik olarak yetişkin teslim (adult_pickup=false) veya servis izni (bus_permission=false) varmış gibi işaretlenmez.",
                                    fontSize = 11.sp,
                                    color = NeutralTextSecondary
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("QR" to "QR Okutma", "BARCODE" to "SCS360 Barkod", "MANUAL" to "Manuel Doğrulama").forEach { (key, label) ->
                                        FilterChip(
                                            selected = selectedMethod == key,
                                            onClick = { selectedMethod = key },
                                            label = { Text(label, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = selectedDirection == "IN",
                                        onClick = { selectedDirection = "IN" },
                                        label = { Text("Kampüse Giriş (IN)") }
                                    )
                                    FilterChip(
                                        selected = selectedDirection == "OUT",
                                        onClick = { selectedDirection = "OUT" },
                                        label = { Text("Kampüsten Çıkış (OUT)") }
                                    )
                                }

                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    students.take(5).forEach { std ->
                                        FilterChip(
                                            selected = selectedStudentId == std.id,
                                            onClick = { selectedStudentId = std.id },
                                            label = { Text("${std.full_name} (${std.class_name})", fontSize = 11.sp) }
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = codeInput,
                                    onValueChange = { codeInput = it },
                                    label = { Text("QR Kayıt Kimliği / SCS360 Barkod / Açıklama") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        onVerifyGateAccess(selectedStudentId, selectedDirection, selectedMethod, codeInput.trim())
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("verify_gate_access_btn")
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Kapı Geçişini Doğrula ve Kaydet")
                                }
                            }
                        }
                    }

                    items(gateLogs, key = { it.id }) { glog ->
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
                                        Text("${glog.student_name} (${glog.class_name})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            "${glog.gate_name} • Yöntem: ${glog.verification_method} • Görevli: ${glog.actor_name}",
                                            fontSize = 12.sp,
                                            color = NeutralTextSecondary
                                        )
                                    }
                                    Surface(
                                        color = if (glog.direction == "IN") StatusSuccessContainer else StatusInfoContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (glog.direction == "IN") "GİRİŞ (IN)" else "ÇIKIŞ (OUT)",
                                            color = if (glog.direction == "IN") StatusSuccess else StatusInfo,
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
                                        "Zaman: ${glog.timestamp_utc} • Yetişkin Teslim: ${if (glog.adult_pickup_verified) "Evet" else "Hayır"} • Servis İzni: ${if (glog.bus_permission_verified) "Evet" else "Hayır"}",
                                        fontSize = 11.sp,
                                        color = NeutralTextSecondary
                                    )
                                    if (glog.requires_anomaly_review) {
                                        Surface(color = StatusWarningContainer, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                "ARDILLIK İNCELEMESİ",
                                                color = StatusWarning,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
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
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("SCS360 Öğrenci Kimlik Kartı & Barkod Üretimi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "Kimlik sorgulama/okuma işlemi otomatik olarak yeni kimlik üretmez. Yeni kimlik oluşturma ayrı yetkili işlemdir; rastgele benzersiz SCS360 barkodu üretir ve QR SVG indirmesinde gizli token yerine kayıt kimliği kullanılır.",
                                    fontSize = 12.sp,
                                    color = NeutralTextSecondary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onIssueIdCard(selectedStudentId) },
                                    modifier = Modifier.fillMaxWidth().testTag("issue_student_id_card_btn")
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Seçili Öğrenci İçin Yeni SCS360 Kimliği Üret")
                                }
                            }
                        }
                    }

                    items(idCards, key = { it.id }) { card ->
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
                                        Text("${card.student_name} (${card.class_name} • No: ${card.student_number})", fontWeight = FontWeight.Bold)
                                        Text("Barkod: ${card.barcode} • QR ID: ${card.qr_record_id}", fontSize = 12.sp, color = ScsPetrolGreen, fontWeight = FontWeight.SemiBold)
                                        Text("Basım: ${card.issued_at} • Yetkili: ${card.issued_by}", fontSize = 11.sp, color = NeutralTextSecondary)
                                    }
                                    Surface(
                                        color = if (card.status == "ACTIVE") StatusSuccessContainer else StatusErrorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            if (card.status == "ACTIVE") "GEÇERLİ" else "İPTAL EDİLDİ",
                                            color = if (card.status == "ACTIVE") StatusSuccess else StatusError,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    OutlinedButton(onClick = { onDownloadQrSvg(card.qr_record_id) }) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Yetkili QR SVG İndir", fontSize = 12.sp)
                                    }
                                    if (card.status == "ACTIVE") {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        TextButton(
                                            onClick = { onRevokeIdCard(card.id) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = StatusError)
                                        ) {
                                            Text("Kimliği İptal Et", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(color = Color.White, shadowElevation = 1.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Kullanıcı, IP adresi veya işlem ara...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val modules = listOf(
                                    "ALL" to "Tümü",
                                    "AUTH" to "Oturum & Giriş",
                                    "FINANCE" to "Finans & Kasa",
                                    "STUDENT" to "Öğrenci İşleri",
                                    "SECURITY" to "Yetki & Güvenlik"
                                )
                                modules.forEach { (key, label) ->
                                    FilterChip(
                                        selected = selectedModule == key,
                                        onClick = { selectedModule = key },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredAuditLogs, key = { it.id }) { log ->
                            val actionColor = when {
                                log.action.contains("DELETE") -> StatusError
                                log.action.contains("PAYMENT") -> StatusSuccess
                                log.action.contains("LOGIN") -> PrimaryBlue
                                else -> AccentAmber
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(actionColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                log.action.contains("DELETE") -> Icons.Default.DeleteForever
                                                log.action.contains("PAYMENT") -> Icons.Default.Payments
                                                log.action.contains("LOGIN") -> Icons.Default.Login
                                                else -> Icons.Default.Security
                                            },
                                            contentDescription = null,
                                            tint = actionColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = log.user_name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = log.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeutralTextSecondary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = log.details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeutralTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Surface(
                                                    color = actionColor.copy(alpha = 0.12f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = log.action,
                                                        color = actionColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                                Surface(
                                                    color = NeutralLightSurfaceVariant,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = log.module,
                                                        color = NeutralTextSecondary,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "IP: ${log.ip_address}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = NeutralTextSecondary
                                            )
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
}

@Composable
private fun GateMetricCard(title: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
            Text(value, color = valueColor, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}
