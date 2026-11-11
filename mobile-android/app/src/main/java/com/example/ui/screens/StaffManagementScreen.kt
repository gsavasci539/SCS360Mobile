package com.example.ui.screens

import com.example.ui.components.ResponsiveTableScope
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DutyLocation
import com.example.data.model.TeacherLeaveRequest
import com.example.ui.components.CompactResponsiveCard
import com.example.ui.components.CompactRowActionIcon
import com.example.ui.components.CompactTableRow
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StickyTableHeaderRow
import com.example.ui.components.TableColumnSpec
import com.example.ui.components.isCompactCardMode
import com.example.ui.components.rememberResolvedTableMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StaffManagementScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onSubmitLeaveRequest: (String, String, String, Int, String) -> Unit,
    onReviewLeaveRequest: (String, Boolean, String) -> Unit
) {
    var selectedTab by remember { mutableStateOf("DUTY") } // DUTY, LEAVES
    var showNewLeaveDialog by remember { mutableStateOf(false) }
    val tableModeState = rememberResolvedTableMode()
    val isCardMode = isCompactCardMode(tableModeState.value)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Personel İK & Nöbet Çizelgesi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Nöbet Yerleşimi & İzin / Mazeret Onayları",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("staff_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == "LEAVES") {
                ExtendedFloatingActionButton(
                    onClick = { showNewLeaveDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("İzin Talebi Oluştur") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(
                selectedTabIndex = if (selectedTab == "DUTY") 0 else 1,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == "DUTY",
                    onClick = { selectedTab = "DUTY" },
                    text = { Text("Nöbet Çizelgesi (${state.dutyRosters.size})") },
                    icon = { Icon(Icons.Default.Security, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == "LEAVES",
                    onClick = { selectedTab = "LEAVES" },
                    text = { Text("İzin Talepleri (${state.leaveRequests.size})") },
                    icon = { Icon(Icons.Default.EventBusy, contentDescription = null) }
                )
            }

            if (selectedTab == "DUTY") {
                ResponsiveTableScope() {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            RoleContextualAiCard(
                                screenName = "STAFF",
                                state = state,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
    
                        if (state.dutyRosters.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Badge,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Kayıtlı nöbet çizelgesi bulunamadı.",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            stickyHeader {
                                StickyTableHeaderRow(
                                    columns = listOf(
                                        TableColumnSpec("Öğretmen / Branş", 1.4f),
                                        TableColumnSpec("Konum / Vardiya", 1.25f),
                                        TableColumnSpec("Tarih / Durum", 1.0f, TextAlign.End)
                                    ),
                                    totalCount = state.dutyRosters.size,
                                    viewModeState = tableModeState
                                )
                            }
    
                            itemsIndexed(state.dutyRosters, key = { _, it -> it.id }) { index, duty ->
                                val locationEnum = try { DutyLocation.valueOf(duty.duty_location) } catch (e: Exception) { DutyLocation.GARDEN }
    
                                if (isCardMode) {
                                    CompactResponsiveCard(
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(locationEnum.emoji, fontSize = 16.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(duty.teacher_name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("${duty.teacher_branch} • ${duty.day_name} (${duty.date})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text("${locationEnum.label} • Saat: ${duty.shift_hours}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
    
                                            if (duty.is_active_today) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = EmeraldPrimary.copy(alpha = 0.16f)
                                                ) {
                                                    Text(
                                                        "Bugün Nöbetçi",
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldPrimary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    CompactTableRow(
                                        index = index,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1.4f).padding(end = 6.dp)) {
                                            Text(duty.teacher_name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(duty.teacher_branch, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        Column(modifier = Modifier.weight(1.25f).padding(end = 6.dp)) {
                                            Text("${locationEnum.emoji} ${locationEnum.label}", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                                            Text(duty.shift_hours, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(modifier = Modifier.weight(1.0f), horizontalAlignment = Alignment.End) {
                                            Text("${duty.day_name} (${duty.date})", fontSize = 11.sp, maxLines = 1)
                                            if (duty.is_active_today) {
                                                Text("Bugün Nöbetçi", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Leaves List
                ResponsiveTableScope() {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (state.leaveRequests.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.EventBusy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            "Kayıtlı izin veya mazeret talebi bulunmuyor.",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            stickyHeader {
                                StickyTableHeaderRow(
                                    columns = listOf(
                                        TableColumnSpec("Personel / Tür", 1.35f),
                                        TableColumnSpec("Tarih / Gerekçe", 1.35f),
                                        TableColumnSpec("Durum / Onay", 1.0f, TextAlign.End)
                                    ),
                                    totalCount = state.leaveRequests.size,
                                    viewModeState = tableModeState
                                )
                            }
    
                            itemsIndexed(state.leaveRequests, key = { _, it -> it.id }) { index, req ->
                                val statusColor = when (req.status) {
                                    "APPROVED" -> EmeraldPrimary
                                    "PENDING" -> AmberPrimary
                                    "REJECTED" -> CrimsonPrimary
                                    else -> Color.Gray
                                }
                                val statusLabel = when (req.status) {
                                    "APPROVED" -> "Onaylandı"
                                    "PENDING" -> "Bekliyor"
                                    "REJECTED" -> "Reddedildi"
                                    else -> req.status
                                }
    
                                if (isCardMode) {
                                    CompactResponsiveCard(
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Text(req.teacher_name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text("${req.branch} • ${req.leave_type} (${req.total_days} Gün)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                            }
    
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = statusColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    statusLabel,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = statusColor
                                                )
                                            }
                                        }
    
                                        Text("Tarih: ${req.start_date} -> ${req.end_date} • ${req.reason}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    
                                        if (req.status == "PENDING") {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                FilledTonalButton(
                                                    onClick = { onReviewLeaveRequest(req.id, true, "Zümre telafisi yapılarak izin onaylanmıştır.") },
                                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = EmeraldPrimary.copy(alpha = 0.2f), contentColor = EmeraldPrimary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Onayla", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                OutlinedButton(
                                                    onClick = { onReviewLeaveRequest(req.id, false, "Ders programı yoğunluğu dolayısıyla uygun görülmemiştir.") },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonPrimary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Reddet", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    CompactTableRow(
                                        index = index,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1.35f).padding(end = 6.dp)) {
                                            Text(req.teacher_name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${req.branch} • ${req.leave_type} (${req.total_days}g)", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Column(modifier = Modifier.weight(1.35f).padding(end = 6.dp)) {
                                            Text("${req.start_date} -> ${req.end_date}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                            Text(req.reason, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Row(
                                            modifier = Modifier.weight(1.0f),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(shape = RoundedCornerShape(5.dp), color = statusColor.copy(alpha = 0.15f)) {
                                                Text(statusLabel, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                            }
                                            if (req.status == "PENDING") {
                                                CompactRowActionIcon(
                                                    icon = Icons.Default.Check,
                                                    contentDescription = "Onayla",
                                                    tint = EmeraldPrimary,
                                                    containerColor = EmeraldPrimary.copy(alpha = 0.15f),
                                                    onClick = { onReviewLeaveRequest(req.id, true, "Zümre telafisi yapılarak izin onaylanmıştır.") }
                                                )
                                                CompactRowActionIcon(
                                                    icon = Icons.Default.Close,
                                                    contentDescription = "Reddet",
                                                    tint = CrimsonPrimary,
                                                    containerColor = CrimsonPrimary.copy(alpha = 0.15f),
                                                    onClick = { onReviewLeaveRequest(req.id, false, "Ders programı yoğunluğu dolayısıyla uygun görülmemiştir.") }
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

    // New Leave Request Dialog
    if (showNewLeaveDialog) {
        var leaveType by remember { mutableStateOf("Mazeret İzni") }
        var startDate by remember { mutableStateOf("2026-09-05") }
        var endDate by remember { mutableStateOf("2026-09-06") }
        var totalDaysInput by remember { mutableStateOf("2") }
        var reason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showNewLeaveDialog = false },
            title = { Text("Yeni İzin / Mazeret Talebi") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("İzin Türü:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Mazeret İzni", "Sevk / Sağlık", "İdari İzin", "Yıllık İzin")) { type ->
                            FilterChip(
                                selected = leaveType == type,
                                onClick = { leaveType = type },
                                label = { Text(type, fontSize = 11.sp) }
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Başlangıç") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("Bitiş") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = totalDaysInput,
                        onValueChange = { totalDaysInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Toplam Gün Sayısı") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("İzin Gerekçesi & Detayı") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = totalDaysInput.toIntOrNull() ?: 1
                        onSubmitLeaveRequest(leaveType, startDate, endDate, days, reason.ifBlank { "Mazeret izni talebi" })
                        showNewLeaveDialog = false
                    }
                ) {
                    Text("Talebi İlet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewLeaveDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}
