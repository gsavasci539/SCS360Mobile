package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Branch
import com.example.data.model.Organization
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizationsBranchesScreen(
    state: Okul360UiState,
    onBack: () -> Unit,
    onAddBranch: (Branch) -> Unit,
    onSwitchBranch: (String) -> Unit,
    onUpdateOrganizationContacts: (orgId: String, name: String, taxOffice: String, taxNumber: String, phone: String, email: String, address: String) -> Unit = { _, _, _, _, _, _, _ -> },
    onUpdateBranchDetails: (Branch) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Kurum & Organizasyon", "Şubeler & Kampüsler")
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var editingOrganization by remember { mutableStateOf<Organization?>(null) }
    var editingBranch by remember { mutableStateOf<Branch?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kurum & Şube Yönetimi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "/organizations • /branches",
                            fontSize = 11.sp,
                            color = NeutralGrayDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_org_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddBranchDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Yeni Şube Ekle") },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_branch")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PrimaryBlue
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = if (index == 0) Icons.Outlined.CorporateFare else Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                OrganizationInfoTab(
                    organizations = state.organizations,
                    state = state,
                    onEditOrganization = { org -> editingOrganization = org }
                )
            } else {
                BranchesListTab(
                    branches = state.branches,
                    selectedBranchId = state.selectedBranchId,
                    onSelectBranch = onSwitchBranch,
                    onEditBranch = { br -> editingBranch = br }
                )
            }
        }
    }

    if (showAddBranchDialog) {
        AddBranchDialog(
            onDismiss = { showAddBranchDialog = false },
            onConfirm = { branch ->
                onAddBranch(branch)
                showAddBranchDialog = false
            }
        )
    }

    editingOrganization?.let { org ->
        var name by remember(org) { mutableStateOf(org.name) }
        var taxOffice by remember(org) { mutableStateOf(org.tax_office) }
        var taxNumber by remember(org) { mutableStateOf(org.tax_number) }
        var phone by remember(org) { mutableStateOf(org.phone) }
        var email by remember(org) { mutableStateOf(org.email) }
        var address by remember(org) { mutableStateOf(org.address) }

        AlertDialog(
            onDismissRequest = { editingOrganization = null },
            title = {
                Column {
                    Text("Kurum İletişim & Vergi Bilgileri", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Kurum oluşturma/silme/durum değiştirme yalnız Root yetkisindedir; iletişim alanlarını boş bırakarak temizleyebilirsiniz.", fontSize = 11.sp, color = NeutralGrayDark)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Kurum Unvanı") }, singleLine = true)
                    OutlinedTextField(value = taxOffice, onValueChange = { taxOffice = it }, label = { Text("Vergi Dairesi (Boş bırakılırsa temizlenir)") }, singleLine = true)
                    OutlinedTextField(value = taxNumber, onValueChange = { taxNumber = it }, label = { Text("Vergi Numarası (Boş bırakılırsa temizlenir)") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefon (Boş bırakılırsa temizlenir)") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-Posta (Boş bırakılırsa temizlenir)") }, singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Merkez Adres (Boş bırakılırsa temizlenir)") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateOrganizationContacts(org.id, name, taxOffice, taxNumber, phone, email, address)
                        editingOrganization = null
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingOrganization = null }) { Text("İptal") }
            }
        )
    }

    editingBranch?.let { branch ->
        var name by remember(branch) { mutableStateOf(branch.name) }
        var address by remember(branch) { mutableStateOf(branch.address) }
        var phone by remember(branch) { mutableStateOf(branch.phone) }
        var email by remember(branch) { mutableStateOf(branch.email ?: "") }

        AlertDialog(
            onDismissRequest = { editingBranch = null },
            title = { Text("${branch.name} • Şube İletişim Düzenle", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Şube Adı") }, singleLine = true)
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Adres (Boş bırakılırsa temizlenir)") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telefon (Boş bırakılırsa temizlenir)") }, singleLine = true)
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("E-Posta (Boş bırakılırsa temizlenir)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateBranchDetails(
                            branch.copy(
                                name = name.ifBlank { branch.name },
                                address = address.trim(),
                                phone = phone.trim(),
                                email = email.trim().takeIf { it.isNotBlank() }
                            )
                        )
                        editingBranch = null
                    }
                ) {
                    Text("Şubeyi Güncelle")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBranch = null }) { Text("İptal") }
            }
        )
    }
}

@Composable
private fun OrganizationInfoTab(
    organizations: List<Organization>,
    state: Okul360UiState,
    onEditOrganization: (Organization) -> Unit
) {
    val org = organizations.firstOrNull() ?: Organization()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Kurumsal Büyüme & Şubeleşme Analiz Kartı
        item {
            RoleContextualAiCard(
                screenName = "DASHBOARD",
                state = state,
                initialExpanded = false
            )
        }

        item {
            // Main Org Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(PrimaryBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = org.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandNavy
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(if (org.is_active) "AKTİF" else "PASİF", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (org.is_active) StatusGreen.copy(alpha = 0.12f) else StatusRed.copy(alpha = 0.12f),
                                        labelColor = if (org.is_active) StatusGreen else StatusRed
                                    ),
                                    border = null
                                )
                            }
                            Text(
                                text = "Kurum Kodu: ${org.code}",
                                fontSize = 13.sp,
                                color = NeutralGrayDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Detail rows
                    DetailItem(icon = Icons.Outlined.AccountBalance, title = "Vergi Dairesi", value = org.tax_office.ifBlank { "Belirtilmedi (Temizlendi)" })
                    DetailItem(icon = Icons.Outlined.Badge, title = "Vergi Numarası", value = org.tax_number.ifBlank { "Belirtilmedi (Temizlendi)" })
                    DetailItem(icon = Icons.Outlined.Phone, title = "Merkez Telefon", value = org.phone.ifBlank { "Belirtilmedi (Temizlendi)" })
                    DetailItem(icon = Icons.Outlined.Email, title = "Kurumsal E-Posta", value = org.email.ifBlank { "Belirtilmedi (Temizlendi)" })
                    DetailItem(icon = Icons.Outlined.Place, title = "Merkez Adres", value = org.address.ifBlank { "Belirtilmedi (Temizlendi)" })

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onEditOrganization(org) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_org_contacts_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kurum İletişim & Vergi Bilgilerini Düzenle (org.write)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatBadge(
                            title = "Toplam Şube",
                            value = "${org.branch_count}",
                            color = PrimaryBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatBadge(
                            title = "Kayıtlı Öğrenci",
                            value = "${org.student_count}",
                            color = AccentTeal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Yetki & Lisans Bilgileri",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandNavy
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "• İzin Seviyesi: org.read / org.write\n• Çoklu Şube Desteği (Multi-Campus Tenant): Aktif\n• API Erişimi: Doğrudan & Yerel Senkronizasyon Destekli",
                        fontSize = 13.sp,
                        color = NeutralGrayDark,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun BranchesListTab(
    branches: List<Branch>,
    selectedBranchId: String,
    onSelectBranch: (String) -> Unit,
    onEditBranch: (Branch) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Kayıtlı Kampüsler (${branches.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = NeutralGrayDark,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        items(branches, key = { it.id }) { branch ->
            val isSelected = branch.id == selectedBranchId
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) PrimaryBlue.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PrimaryBlue)) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("branch_card_${branch.id}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryBlue else NeutralGrayLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = branch.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy
                            )
                            Text(
                                text = branch.address.ifBlank { "Adres belirtilmedi" },
                                fontSize = 12.sp,
                                color = NeutralGrayDark
                            )
                        }
                        IconButton(onClick = { onEditBranch(branch) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Şube Düzenle", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }
                        if (isSelected) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Seçili", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = PrimaryBlue.copy(alpha = 0.15f),
                                    labelColor = PrimaryBlue
                                ),
                                border = null
                            )
                        } else {
                            OutlinedButton(
                                onClick = { onSelectBranch(branch.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Seç", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Person, contentDescription = null, modifier = Modifier.size(15.dp), tint = PrimaryBlue)
                            Text(text = "${branch.student_count} Öğrenci", fontSize = 11.5.sp, color = NeutralGrayDark, maxLines = 1)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(15.dp), tint = AccentTeal)
                            Text(text = "${branch.class_count} Sınıf", fontSize = 11.5.sp, color = NeutralGrayDark, maxLines = 1)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(Icons.Outlined.Phone, contentDescription = null, modifier = Modifier.size(15.dp), tint = NeutralGrayDark)
                            Text(text = branch.phone, fontSize = 11.5.sp, color = NeutralGrayDark, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
        Text(text = title, fontSize = 13.sp, color = NeutralGrayDark, modifier = Modifier.width(130.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = BrandNavy, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatBadge(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 11.sp, color = NeutralGrayDark)
        }
    }
}

@Composable
private fun AddBranchDialog(
    onDismiss: () -> Unit,
    onConfirm: (Branch) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yeni Şube Ekle", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Şube / Kampüs Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Adres & İlçe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefon Numarası") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("İletişim E-Postası") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newBranch = Branch(
                            id = "branch-${System.currentTimeMillis()}",
                            name = name,
                            code = name.take(3).uppercase(),
                            address = address,
                            phone = phone,
                            student_count = 0,
                            class_count = 0,
                            is_active = true
                        )
                        onConfirm(newBranch)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
