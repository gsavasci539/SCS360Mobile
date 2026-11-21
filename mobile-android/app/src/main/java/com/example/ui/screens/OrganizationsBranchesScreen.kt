package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Branch
import com.example.data.model.Organization
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun OrganizationsBranchesScreen(
    state: Okul360UiState,
    onBack: () -> Unit,
    onReloadOrganizations: () -> Unit,
    onReloadBranches: () -> Unit,
    onSwitchBranch: (String) -> Unit,
    onSaveOrganization: suspend (Organization, Organization) -> String?,
    onSaveBranch: suspend (Branch?, Branch) -> String?,
) {
    val actor = state.currentUser
    val organizationId = if (actor.is_super_admin == true) state.workspaceOrganizationId else actor.organization_id
    val canReadOrg = NativeNavigationPolicy.has(actor, "org.read")
    val canReadBranches = NativeNavigationPolicy.has(actor, "branch.read")
    val canEditOrg = canReadOrg && NativeNavigationPolicy.has(actor, "org.write")
    val canEditBranches = canReadBranches && NativeNavigationPolicy.has(actor, "branch.write")
    val organization = state.organizations.singleOrNull { it.id == organizationId }
    val visibleBranches = state.branches.filter { it.organization_id == organizationId }
    var selected by remember(organizationId, canReadOrg, canReadBranches) { mutableIntStateOf(if (canReadOrg) 0 else 1) }
    var editingOrganization by remember(organizationId) { mutableStateOf<Organization?>(null) }
    var editingBranch by remember(organizationId) { mutableStateOf<Branch?>(null) }
    var addingBranch by remember(organizationId) { mutableStateOf(false) }
    val tabs = listOfNotNull(
        0.takeIf { canReadOrg }?.let { it to "Kurum" },
        1.takeIf { canReadBranches }?.let { it to "Şubeler" },
    )

    Scaffold(topBar = {
        TopAppBar(title = { Text("Kurum ve Şube Yönetimi") }, navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_org_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
        })
    }, floatingActionButton = {
        if (selected == 1 && canEditBranches && !organizationId.isNullOrBlank()) {
            FloatingActionButton(onClick = { addingBranch = true }, modifier = Modifier.testTag("fab_add_branch")) {
                Icon(Icons.Default.Add, contentDescription = "Şube ekle")
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (organizationId.isNullOrBlank() || tabs.isEmpty()) {
                Text(if (actor.is_super_admin == true) "Platform yönetiminden bir kurum seçin." else "Kurum yönetimi erişimi bulunamadı.",
                    modifier = Modifier.padding(16.dp))
                return@Column
            }
            ScrollableTabRow(selectedTabIndex = tabs.indexOfFirst { it.first == selected }.coerceAtLeast(0), edgePadding = 8.dp) {
                tabs.forEach { (index, title) ->
                    Tab(selected = selected == index, onClick = { selected = index },
                        text = { Text(title) }, modifier = Modifier.testTag("registry_tab_$index"))
                }
            }
            if (selected == 0) {
                LazyColumn(Modifier.fillMaxSize().testTag("registry_organization_list"), contentPadding = PaddingValues(16.dp)) {
                    item {
                        if (organization == null) {
                            Text("Kurum bilgisi alınamadı.")
                            TextButton(onClick = onReloadOrganizations) { Text("Kurum bilgisini yenile") }
                        } else {
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(organization.name, style = MaterialTheme.typography.titleLarge)
                                    if (organization.legal_name.isNotBlank()) Text(organization.legal_name)
                                    Text(if (organization.is_active) "Etkin" else "Kapalı")
                                    RegistryContactLine("Vergi dairesi", organization.tax_office)
                                    RegistryContactLine("Vergi numarası", organization.tax_number)
                                    RegistryContactLine("Telefon", organization.phone)
                                    RegistryContactLine("E-posta", organization.email)
                                    RegistryContactLine("Adres", organization.address)
                                    RegistryContactLine("Şehir", organization.city)
                                    RegistryContactLine("Ülke", organization.country)
                                    if (canEditOrg) {
                                        OutlinedButton(onClick = { editingOrganization = organization },
                                            modifier = Modifier.testTag("edit_org_contacts_btn")) { Text("Kurum bilgilerini düzenle") }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().testTag("registry_branches_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        TextButton(onClick = onReloadBranches) { Text("Şube listesini yenile") }
                        if (visibleBranches.isEmpty()) Text("Gösterilecek şube yok.")
                    }
                    items(visibleBranches, key = { it.id }) { branch ->
                        Card(Modifier.fillMaxWidth().testTag("branch_card_${branch.id}")) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(branch.name, style = MaterialTheme.typography.titleMedium)
                                if (branch.code.isNotBlank()) Text("Kod ${branch.code}")
                                Text("${branch.student_count} öğrenci · Kapasite ${branch.capacity}")
                                Text(if (branch.is_active) "Etkin" else "Pasif")
                                if (branch.address.isNotBlank()) Text(branch.address)
                                if (branch.city.isNotBlank()) Text(branch.city)
                                if (branch.phone.isNotBlank()) Text(branch.phone)
                                branch.email?.takeIf(String::isNotBlank)?.let { Text(it) }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (branch.id == state.selectedBranchId) {
                                        Text("Seçili şube")
                                    } else if (branch.is_active) {
                                        OutlinedButton(onClick = { onSwitchBranch(branch.id) }) { Text("Şubeyi seç") }
                                    }
                                    if (canEditBranches) {
                                        OutlinedButton(onClick = { editingBranch = branch },
                                            modifier = Modifier.testTag("edit_branch_${branch.id}")) { Text("Şubeyi düzenle") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingOrganization?.let { original ->
        RegistryLocationEditor("Kurum bilgileri", listOf(
            RegistryEditorField("name", "Kurum adı", original.name),
            RegistryEditorField("legal_name", "Resmî unvan", original.legal_name),
            RegistryEditorField("tax_office", "Vergi dairesi", original.tax_office),
            RegistryEditorField("tax_number", "Vergi numarası", original.tax_number),
            RegistryEditorField("phone", "Telefon", original.phone, KeyboardType.Phone),
            RegistryEditorField("email", "E-posta", original.email, KeyboardType.Email),
            RegistryEditorField("address", "Adres", original.address),
            RegistryEditorField("city", "Şehir", original.city),
            RegistryEditorField("country", "Ülke", original.country),
        ), onSave = { form, _ ->
            val country = form.getValue("country")
            if (country.isBlank() && original.country.isNotBlank()) "Ülke alanını doldurun."
            else onSaveOrganization(original, original.copy(
                name = form.getValue("name"), legal_name = form.getValue("legal_name"), tax_office = form.getValue("tax_office"),
                tax_number = form.getValue("tax_number"), phone = form.getValue("phone"), email = form.getValue("email"),
                address = form.getValue("address"), city = form.getValue("city"), country = country
            ))
        }) { editingOrganization = null }
    }
    if ((addingBranch || editingBranch != null) && !organizationId.isNullOrBlank()) {
        val original = editingBranch
        val initial = original ?: Branch(organization_id = organizationId)
        RegistryLocationEditor(if (original == null) "Yeni şube" else "Şube bilgileri", listOf(
            RegistryEditorField("name", "Şube adı", initial.name), RegistryEditorField("code", "Şube kodu", initial.code),
            RegistryEditorField("phone", "Telefon", initial.phone, KeyboardType.Phone),
            RegistryEditorField("email", "E-posta", initial.email.orEmpty(), KeyboardType.Email),
            RegistryEditorField("address", "Adres", initial.address), RegistryEditorField("city", "Şehir", initial.city),
            RegistryEditorField("capacity", "Kapasite", original?.capacity?.toString().orEmpty(), KeyboardType.Number),
        ), active = initial.is_active, onSave = { form, active ->
            val capacity = form.getValue("capacity").trim().ifBlank { "0" }.toIntOrNull()
            if (capacity == null || capacity < 0) "Kapasiteye sıfır veya pozitif bir tam sayı girin."
            else onSaveBranch(original, initial.copy(name = form.getValue("name"), code = form.getValue("code"),
                phone = form.getValue("phone"), email = form.getValue("email"), address = form.getValue("address"),
                city = form.getValue("city"), capacity = capacity, is_active = active == true))
        }) { addingBranch = false; editingBranch = null }
    }
}

@Composable
private fun RegistryContactLine(label: String, value: String) {
    Text("$label · ${value.ifBlank { "Belirtilmedi" }}")
}
