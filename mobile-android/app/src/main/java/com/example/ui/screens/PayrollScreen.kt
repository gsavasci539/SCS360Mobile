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
import com.example.data.model.PayrollRecord
import com.example.data.model.StaffExtraLessonRecord
import com.example.data.model.StaffTimesheetRecord
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.util.Locale

@Composable
fun PayrollScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit = {},
    onCreatePayroll: (PayrollRecord) -> Unit = {}
) {
    val records = state.payrollRecords
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPeriodMode by remember { mutableStateOf("2026-11 (Aylık)") }
    var showAddDialog by remember { mutableStateOf(false) }

    var localPayrolls by remember(records) { mutableStateOf(records) }
    var timesheets by remember {
        mutableStateOf(
            listOf(
                StaffTimesheetRecord(
                    id = "ts-1",
                    staff_id = "stf-1",
                    staff_name = "Ahmet Yılmaz",
                    branch_name = "Merkez Kampüs",
                    work_date = "2026-11-03",
                    check_in_utc = "2026-11-03 05:25 UTC (08:25 TR)",
                    check_out_utc = "2026-11-03 14:10 UTC (17:10 TR)",
                    total_hours = 8.75,
                    status = "APPROVED"
                ),
                StaffTimesheetRecord(
                    id = "ts-2",
                    staff_id = "stf-2",
                    staff_name = "Ayşe Kaya",
                    branch_name = "Merkez Kampüs",
                    work_date = "2026-11-03",
                    check_in_utc = "2026-11-03 05:30 UTC (08:30 TR)",
                    check_out_utc = "2026-11-03 14:30 UTC (17:30 TR)",
                    total_hours = 9.0,
                    status = "DRAFT"
                )
            )
        )
    }
    var extraLessons by remember {
        mutableStateOf(
            listOf(
                StaffExtraLessonRecord(
                    id = "exl-1",
                    staff_id = "stf-1",
                    staff_name = "Ahmet Yılmaz",
                    period_month = "2026-11",
                    lesson_date = "2026-11-02",
                    class_name = "8-A LGS Hafta Sonu Etüdü",
                    hours = 4.0,
                    hourly_rate = 450.0,
                    total_amount = 1800.0,
                    status = "APPROVED",
                    approved_at_utc = "2026-11-02 16:00 UTC"
                ),
                StaffExtraLessonRecord(
                    id = "exl-2",
                    staff_id = "stf-2",
                    staff_name = "Ayşe Kaya",
                    period_month = "2026-11",
                    lesson_date = "2026-11-03",
                    class_name = "12-A YKS Deneme Çözümü",
                    hours = 3.0,
                    hourly_rate = 500.0,
                    total_amount = 1500.0,
                    status = "DRAFT",
                    approved_at_utc = null
                )
            )
        )
    }

    val totalNet = remember(localPayrolls) { localPayrolls.sumOf { it.net_salary } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Payments, contentDescription = null) },
                text = { Text("Yeni Bordro Oluştur", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_payroll_fab")
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bordro, Puantaj & Ek Ders Hakediş",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Şube kapsam korumalı maaşlar, UTC puantaj ve onay kilidi",
                                style = MaterialTheme.typography.bodySmall,
                                color = NeutralTextSecondary
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "₺%,.0f", totalNet),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("2026-11 (Aylık)", "2026-Q4 (Üç Aylık)", "2026-12 → 2027-02").forEach { period ->
                            FilterChip(
                                selected = selectedPeriodMode == period,
                                onClick = { selectedPeriodMode = period },
                                label = { Text(period, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Bordrolar (${localPayrolls.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Puantaj UTC (${timesheets.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Ek Ders (${extraLessons.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
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
                                    "Onaylanan (PAID/APPROVED) bordro ve ek ders kayıtlarının değiştirilmesi engellenir; tekrar onay işlemi ilk onay zamanını korur.",
                                    fontSize = 12.sp,
                                    color = OnScsContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        items(localPayrolls, key = { it.id }) { rec ->
                            val isPaid = rec.status == "PAID"
                            val statusColor = if (isPaid) StatusSuccess else AccentAmber

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
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(statusColor.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (isPaid) Icons.Default.Lock else Icons.Default.AccountBalanceWallet,
                                                    contentDescription = null,
                                                    tint = statusColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = rec.staff_name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${rec.role_title} • ${rec.period_month}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = NeutralTextSecondary
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = String.format(Locale.US, "₺%,.0f", rec.net_salary),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = NeutralTextPrimary
                                            )
                                            Text(
                                                text = if (isPaid) "Onaylı & Kilitli" else "Taslak (Onay Bekliyor)",
                                                color = statusColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = NeutralLightBorder)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Baz: ₺${rec.base_salary.toInt()} • Ek Ders: +₺${rec.bonus_allowance.toInt()} • Kesinti: -₺${rec.deductions.toInt()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = NeutralTextSecondary
                                        )
                                        if (!isPaid) {
                                            Button(
                                                onClick = {
                                                    localPayrolls = localPayrolls.map {
                                                        if (it.id == rec.id) it.copy(status = "PAID") else it
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Bordroyu Onayla & Kilitle", fontSize = 11.sp)
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
                        items(timesheets, key = { it.id }) { ts ->
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
                                        Text("${ts.staff_name} (${ts.branch_name})", fontWeight = FontWeight.Bold)
                                        Text("${ts.total_hours} Saat • ${ts.status}", fontWeight = FontWeight.Bold, color = ScsPetrolGreen, fontSize = 12.sp)
                                    }
                                    Text("Giriş: ${ts.check_in_utc}", fontSize = 12.sp, color = NeutralTextSecondary)
                                    Text("Çıkış: ${ts.check_out_utc}", fontSize = 12.sp, color = NeutralTextSecondary)
                                    if (ts.status == "DRAFT") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                timesheets = timesheets.map {
                                                    if (it.id == ts.id) it.copy(status = "APPROVED") else it
                                                }
                                            }
                                        ) {
                                            Text("Puantajı Onayla", fontSize = 12.sp)
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
                        items(extraLessons, key = { it.id }) { exl ->
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
                                        Text("${exl.staff_name} • ${exl.class_name}", fontWeight = FontWeight.Bold)
                                        Text("₺${String.format(Locale.US, "%,.0f", exl.total_amount)}", fontWeight = FontWeight.ExtraBold, color = ScsPetrolGreen)
                                    }
                                    Text(
                                        "Tarih: ${exl.lesson_date} • ${exl.hours} Saat × ₺${exl.hourly_rate.toInt()} • Durum: ${exl.status}",
                                        fontSize = 12.sp,
                                        color = NeutralTextSecondary
                                    )
                                    if (exl.status == "DRAFT") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                extraLessons = extraLessons.map {
                                                    if (it.id == exl.id) it.copy(status = "APPROVED", approved_at_utc = it.approved_at_utc ?: "2026-11-03 11:45 UTC") else it
                                                }
                                            }
                                        ) {
                                            Text("Ek Dersi Onayla & Kilitle", fontSize = 12.sp)
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
        var staffName by remember { mutableStateOf("") }
        var roleTitle by remember { mutableStateOf("Branş Öğretmeni") }
        var baseSalary by remember { mutableStateOf("42500") }
        var bonus by remember { mutableStateOf("4500") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Personel Bordro Kaydı", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = staffName,
                        onValueChange = { staffName = it },
                        label = { Text("Personel Ad Soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roleTitle,
                        onValueChange = { roleTitle = it },
                        label = { Text("Görev / Ünvan") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = baseSalary,
                        onValueChange = { baseSalary = it },
                        label = { Text("Baz Maaş (₺)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bonus,
                        onValueChange = { bonus = it },
                        label = { Text("Ek Ders / Prim (₺)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (staffName.isNotBlank()) {
                            val base = baseSalary.toDoubleOrNull() ?: 40000.0
                            val bns = bonus.toDoubleOrNull() ?: 0.0
                            val newPayroll = PayrollRecord(
                                id = "pay-${System.currentTimeMillis()}",
                                staff_name = staffName.trim(),
                                role_title = roleTitle.trim(),
                                base_salary = base,
                                bonus_allowance = bns,
                                deductions = 0.0,
                                net_salary = base + bns,
                                status = "PENDING"
                            )
                            onCreatePayroll(newPayroll)
                            localPayrolls = listOf(newPayroll) + localPayrolls
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
