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
import com.example.data.model.GuidanceIncident
import com.example.data.model.GuidanceRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GuidanceScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit = {},
    onCreateRecord: (GuidanceRecord) -> Unit = {},
    onDeleteRecord: (String) -> Unit = {}
) {
    val records = state.guidanceRecords
    val students = state.students
    var selectedTab by remember { mutableIntStateOf(0) }
    var hasConfidentialReadPerm by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    var incidents by remember {
        mutableStateOf(
            listOf(
                GuidanceIncident(
                    id = "ginc-1",
                    student_id = "std-1",
                    student_name = "Ali Demir",
                    class_name = "8-A",
                    reported_by = "Ayşe Kaya (Matematik Öğretmeni)",
                    counselor_name = "Psk. Dan. Zeynep Aydın",
                    title = "Sınav Kaygısı ve Akran İletişim Hassasiyeti",
                    severity = "MEDIUM",
                    is_confidential = false,
                    status = "RESOLVED",
                    resolution_note = "Öğrenci ve veli ile ortak çalışma planı oluşturuldu.",
                    resolved_at = "2026-11-02 14:10 UTC",
                    created_at = "2026-11-01"
                ),
                GuidanceIncident(
                    id = "ginc-2",
                    student_id = "std-2",
                    student_name = "Zeynep Yılmaz",
                    class_name = "10-B",
                    reported_by = "Nöbetçi Öğretmen",
                    counselor_name = "Psk. Dan. Zeynep Aydın",
                    title = "Gizli Bireysel Rehberlik Dosyası",
                    severity = "HIGH",
                    is_confidential = true,
                    status = "OPEN",
                    resolution_note = null,
                    resolved_at = null,
                    created_at = "2026-11-03"
                )
            )
        )
    }

    val filteredRecords = remember(records, searchQuery, hasConfidentialReadPerm) {
        records.filter { rec ->
            val allowedByConfidential = !rec.is_confidential || hasConfidentialReadPerm
            val matchesSearch = searchQuery.isBlank() ||
                rec.student_name.contains(searchQuery, ignoreCase = true) ||
                rec.notes.contains(searchQuery, ignoreCase = true) ||
                rec.counselor_name.contains(searchQuery, ignoreCase = true)
            allowedByConfidential && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Psychology, contentDescription = null) },
                text = { Text("Yeni Görüşme / Olay Ekle", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_guidance_fab")
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("PDR & Rehberlik Servisi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        FilterChip(
                            selected = hasConfidentialReadPerm,
                            onClick = { hasConfidentialReadPerm = !hasConfidentialReadPerm },
                            label = {
                                Text(
                                    if (hasConfidentialReadPerm) "Gizli Yetki: Açık (read_confidential)" else "Gizli Yetki: Kapalı",
                                    fontSize = 11.sp
                                )
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Öğrenci, danışman veya görüşme notu ara...") },
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
                    text = { Text("Görüşmeler (${filteredRecords.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Gözlem Notları", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Olaylar & Çözüm (${incidents.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0, 1 -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredRecords, key = { it.id }) { rec ->
                            val badgeColor = if (rec.is_confidential) StatusError else PrimaryBlue

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
                                                    .background(badgeColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (rec.is_confidential) Icons.Default.Lock else Icons.Default.Psychology,
                                                    contentDescription = null,
                                                    tint = badgeColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "${rec.student_name} (${rec.class_name})",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (rec.is_confidential) {
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Surface(color = StatusErrorContainer, shape = RoundedCornerShape(6.dp)) {
                                                            Text(
                                                                "GİZLİ KAYIT",
                                                                color = StatusError,
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                                Text(
                                                    text = "${rec.counselor_name} • ${rec.date} • ${rec.session_type}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeutralTextSecondary
                                                )
                                            }
                                        }

                                        IconButton(onClick = { onDeleteRecord(rec.id) }) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = StatusError)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = rec.notes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = NeutralTextPrimary
                                    )
                                    if (rec.recommendations.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Öneri & Takip: ${rec.recommendations}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(10.dp)
                                            )
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
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ScsContainerLight),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Tekrarlı çözüm işlemi (Idempotent Resolution) ilk çözüm zamanını ve danışman bilgisini korur.",
                                    fontSize = 12.sp,
                                    color = OnScsContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        items(incidents.filter { !it.is_confidential || hasConfidentialReadPerm }, key = { it.id }) { inc ->
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
                                        Text("${inc.student_name} (${inc.class_name}) • ${inc.title}", fontWeight = FontWeight.Bold)
                                        Text(
                                            inc.status,
                                            fontWeight = FontWeight.Bold,
                                            color = if (inc.status == "RESOLVED") StatusSuccess else StatusWarning,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        "Bildiren: ${inc.reported_by} • Danışman: ${inc.counselor_name}",
                                        fontSize = 12.sp,
                                        color = NeutralTextSecondary
                                    )
                                    inc.resolution_note?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Çözüm: $it (${inc.resolved_at})", fontSize = 12.sp, color = StatusSuccess)
                                    }
                                    if (inc.status != "RESOLVED") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                incidents = incidents.map {
                                                    if (it.id == inc.id && it.status != "RESOLVED") {
                                                        it.copy(
                                                            status = "RESOLVED",
                                                            resolution_note = "Danışman görüşmesiyle çözümlendi.",
                                                            resolved_at = it.resolved_at ?: "2026-11-03 11:30 UTC"
                                                        )
                                                    } else it
                                                }
                                            }
                                        ) {
                                            Text("Olayı Çözümlendi İşaretle", fontSize = 12.sp)
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

    if (showAddDialog) {
        val std = students.firstOrNull()
        var sessionType by remember { mutableStateOf("ACADEMIC_COACHING") }
        var notes by remember { mutableStateOf("") }
        var recommendations by remember { mutableStateOf("") }
        var isConfidential by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni PDR Görüşme Kaydı", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = sessionType,
                        onValueChange = { sessionType = it },
                        label = { Text("Görüşme Türü") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Görüşme Özeti & Gözlemler") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = recommendations,
                        onValueChange = { recommendations = it },
                        label = { Text("Takip / Öneriler") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isConfidential, onCheckedChange = { isConfidential = it })
                        Text("Gizli Kayıt (guidance.read_confidential gerekir)", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (notes.isNotBlank()) {
                            onCreateRecord(
                                GuidanceRecord(
                                    id = "gdr-${System.currentTimeMillis()}",
                                    student_id = std?.id ?: "std-1",
                                    student_name = std?.full_name ?: "Ali Demir",
                                    class_name = std?.class_name ?: "8-A",
                                    session_type = sessionType.trim(),
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                    notes = notes.trim(),
                                    recommendations = recommendations.trim(),
                                    is_confidential = isConfidential
                                )
                            )
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Kaydet")
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
