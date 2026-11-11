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
import com.example.data.model.AssetAssignmentRecord
import com.example.data.model.AssetMaintenanceRecord
import com.example.data.model.SchoolResource
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    state: Okul360UiState,
    onAddResource: (name: String, category: String, location: String, status: String, qty: Int, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteResource: (String) -> Unit = {}
) {
    val resources = state.resources
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showAddAssignmentDialog by remember { mutableStateOf(false) }
    var showAddMaintenanceDialog by remember { mutableStateOf(false) }

    var assignments by remember {
        mutableStateOf(
            listOf(
                AssetAssignmentRecord(
                    id = "asgn-1",
                    resource_id = "res-1",
                    resource_name = "Akıllı Tahta (Promethean 75\")",
                    location_name = "A Blok - 8A Dersliği",
                    assigned_staff_name = "Ayşe Kaya (Matematik Öğretmeni)",
                    assigned_at = "2026-09-01",
                    status = "ACTIVE",
                    condition_note = "Kalem seti ve kumanda ile eksiksiz zimmetlendi."
                ),
                AssetAssignmentRecord(
                    id = "asgn-2",
                    resource_id = "res-2",
                    resource_name = "Dizüstü Bilgisayar (MacBook Air M3)",
                    location_name = "PDR & Rehberlik Ofisi",
                    assigned_staff_name = "Zeynep Aydın (Rehberlik)",
                    assigned_at = "2026-09-10",
                    status = "ACTIVE",
                    condition_note = "Şarj adaptörü ve taşıma çantası dahil."
                )
            )
        )
    }

    var maintenances by remember {
        mutableStateOf(
            listOf(
                AssetMaintenanceRecord(
                    id = "maint-1",
                    resource_id = "res-3",
                    resource_name = "Kimya Laboratuvar Mikroskobu",
                    issue_description = "Mercek kalibrasyonu ve aydınlatma LED değişimi",
                    technician_Or_vendor = "Optik Teknik Servis A.Ş.",
                    cost = 1850.0,
                    scheduled_date = "2026-11-05",
                    status = "IN_PROGRESS"
                )
            )
        )
    }

    val filtered = remember(resources, selectedCategory) {
        if (selectedCategory == "ALL") resources
        else resources.filter { it.type.equals(selectedCategory, ignoreCase = true) }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> showAddDialog = true
                        1 -> showAddAssignmentDialog = true
                        else -> showAddMaintenanceDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = {
                    Text(
                        when (selectedTab) {
                            0 -> "Demirbaş / Konum Ekle"
                            1 -> "Personel Zimmet Ekle"
                            else -> "Bakım Kaydı Aç"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.testTag("add_resource_fab")
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
                    Text(
                        text = "Demirbaş, Konum, Zimmet & Bakım Yönetimi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeutralTextPrimary
                    )
                    Text(
                        text = "Kurum/şube/personel sahipliği denetimli envanter, zimmet ve periyodik bakım akışları",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralTextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InventoryStatChip("Demirbaş / Konum", "${resources.size}", PrimaryBlue, Modifier.weight(1f))
                        InventoryStatChip("Aktif Zimmet", "${assignments.count { it.status == "ACTIVE" }}", StatusSuccess, Modifier.weight(1f))
                        InventoryStatChip("Bakımda", "${maintenances.count { it.status != "COMPLETED" }}", AccentAmber, Modifier.weight(1f))
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Demirbaş & Konum (${filtered.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Zimmet (${assignments.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Bakım (${maintenances.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            when (selectedTab) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "ALL" to "Tümü (${resources.size})",
                                    "ROOM" to "Derslik/Salon",
                                    "LAB" to "Laboratuvar",
                                    "DEVICE" to "Cihaz/Ekipman"
                                ).forEach { (cat, label) ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }

                        items(filtered, key = { it.id }) { res ->
                            val isActive = res.status == "ACTIVE"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("resource_card_${res.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = res.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = NeutralTextPrimary
                                            )
                                            Surface(
                                                color = if (isActive) StatusSuccessContainer else StatusWarningContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isActive) "Aktif" else res.status,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isActive) StatusSuccess else StatusWarning,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Tür: ${res.type} • Kapasite/Seri No: ${res.capacity_or_serial}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = NeutralTextSecondary
                                        )
                                        if (!res.notes.isNullOrBlank()) {
                                            Text(
                                                text = res.notes,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PrimaryBlue
                                            )
                                        }
                                    }

                                    IconButton(onClick = { onDeleteResource(res.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = StatusError)
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
                        items(assignments, key = { it.id }) { asgn ->
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
                                        Text(asgn.resource_name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            if (asgn.status == "ACTIVE") "ZİMMETTE" else "İADE EDİLDİ",
                                            fontWeight = FontWeight.Bold,
                                            color = if (asgn.status == "ACTIVE") StatusSuccess else NeutralTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Text("Personel: ${asgn.assigned_staff_name} • Konum: ${asgn.location_name}", fontSize = 12.sp, color = NeutralTextSecondary)
                                    Text("Teslim Tarihi: ${asgn.assigned_at} • Not: ${asgn.condition_note}", fontSize = 12.sp)
                                    if (asgn.status == "ACTIVE") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(
                                            onClick = {
                                                assignments = assignments.map {
                                                    if (it.id == asgn.id) it.copy(status = "RETURNED") else it
                                                }
                                            }
                                        ) {
                                            Text("Zimmeti İade Al", fontSize = 12.sp)
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
                        items(maintenances, key = { it.id }) { maint ->
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
                                        Text(maint.resource_name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text(
                                            "₺${String.format(Locale.US, "%,.0f", maint.cost)} • ${maint.status}",
                                            fontWeight = FontWeight.Bold,
                                            color = if (maint.status == "COMPLETED") StatusSuccess else AccentAmber,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text("İşlem: ${maint.issue_description}", fontSize = 12.sp)
                                    Text("Servis: ${maint.technician_Or_vendor} • Tarih: ${maint.scheduled_date}", fontSize = 12.sp, color = NeutralTextSecondary)
                                    if (maint.status != "COMPLETED") {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = {
                                                maintenances = maintenances.map {
                                                    if (it.id == maint.id) it.copy(status = "COMPLETED") else it
                                                }
                                            }
                                        ) {
                                            Text("Bakımı Tamamla", fontSize = 12.sp)
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
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("LAB") }
        var location by remember { mutableStateOf("") }
        var capacity by remember { mutableStateOf("30") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Demirbaş / Konum Ekle", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Kaynak Adı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Bina / Kat / Konum") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = capacity,
                        onValueChange = { capacity = it },
                        label = { Text("Kapasite veya Seri No") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notlar") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddResource(name.trim(), category, location.trim(), "ACTIVE", capacity.trim().toIntOrNull() ?: 1, notes.trim())
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    if (showAddAssignmentDialog) {
        var resName by remember { mutableStateOf("") }
        var staffName by remember { mutableStateOf("") }
        var locName by remember { mutableStateOf("Merkez Kampüs - A Blok") }
        var note by remember { mutableStateOf("Eksiksiz teslim edildi.") }

        AlertDialog(
            onDismissRequest = { showAddAssignmentDialog = false },
            title = { Text("Yeni Personel Zimmet Kaydı", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = resName, onValueChange = { resName = it }, label = { Text("Demirbaş Adı") }, singleLine = true)
                    OutlinedTextField(value = staffName, onValueChange = { staffName = it }, label = { Text("Zimmetlenen Personel") }, singleLine = true)
                    OutlinedTextField(value = locName, onValueChange = { locName = it }, label = { Text("Konum") }, singleLine = true)
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Teslim Notu") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resName.isNotBlank() && staffName.isNotBlank()) {
                            assignments = listOf(
                                AssetAssignmentRecord(
                                    id = "asgn-${System.currentTimeMillis()}",
                                    resource_name = resName.trim(),
                                    assigned_staff_name = staffName.trim(),
                                    location_name = locName.trim(),
                                    assigned_at = "2026-11-03",
                                    condition_note = note.trim()
                                )
                            ) + assignments
                            showAddAssignmentDialog = false
                        }
                    }
                ) {
                    Text("Zimmetle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAssignmentDialog = false }) { Text("İptal") }
            }
        )
    }

    if (showAddMaintenanceDialog) {
        var resName by remember { mutableStateOf("") }
        var issue by remember { mutableStateOf("") }
        var cost by remember { mutableStateOf("1200") }

        AlertDialog(
            onDismissRequest = { showAddMaintenanceDialog = false },
            title = { Text("Yeni Bakım / Arıza Kaydı", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = resName, onValueChange = { resName = it }, label = { Text("Demirbaş Adı") }, singleLine = true)
                    OutlinedTextField(value = issue, onValueChange = { issue = it }, label = { Text("Arıza / Bakım Açıklaması") }, singleLine = true)
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Tahmini Maliyet (₺)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resName.isNotBlank()) {
                            maintenances = listOf(
                                AssetMaintenanceRecord(
                                    id = "maint-${System.currentTimeMillis()}",
                                    resource_name = resName.trim(),
                                    issue_description = issue.ifBlank { "Periyodik bakım" },
                                    cost = cost.toDoubleOrNull() ?: 0.0,
                                    scheduled_date = "2026-11-05"
                                )
                            ) + maintenances
                            showAddMaintenanceDialog = false
                        }
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMaintenanceDialog = false }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun InventoryStatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = color.copy(alpha = 0.09f),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 11.sp, color = NeutralTextSecondary)
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = color)
        }
    }
}
