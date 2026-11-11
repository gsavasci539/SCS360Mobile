package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.data.model.AppUser
import com.example.data.model.Branch
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersScreen(
    state: Okul360UiState,
    onAddUser: (String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteUser: (String) -> Unit = {},
    onResetPassword: (String, String) -> Unit = { _, _ -> },
    onUpdateUserAccess: (userId: String, roles: List<String>, branchIds: List<String>, status: String) -> Unit = { _, _, _, _ -> },
    onEnsureStandardRoles: () -> Unit = {},
    onLinkParentAccount: (studentId: String, parentUserId: String?, receiveNotifications: Boolean, smsPhone: String?, whatsappPhone: String?) -> Unit = { _, _, _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showParentLinkDialog by remember { mutableStateOf(false) }
    var selectedUserForAccess by remember { mutableStateOf<AppUser?>(null) }

    // Non-root users cannot see or modify the Root (SUPERADMIN) account
    val visibleUsers = remember(state.users, state.selectedRole, searchQuery, selectedRoleFilter) {
        state.users.filter { u ->
            val isRoot = u.role.contains("SUPERADMIN", ignoreCase = true) || u.email.equals("root@scs360.com.tr", ignoreCase = true)
            val allowedByRootRule = state.selectedRole == UserRole.SUPERADMIN || !isRoot
            val matchesRole = selectedRoleFilter == "ALL" || u.role.contains(selectedRoleFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                u.full_name.contains(searchQuery, ignoreCase = true) ||
                u.email.contains(searchQuery, ignoreCase = true)
            allowedByRootRule && matchesRole && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                text = { Text("Yeni Kullanıcı Ekle", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_user_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NeutralLightBackground)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kullanıcılar, Rol & Şube Erişimi",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NeutralTextPrimary
                    )
                    Text(
                        text = "Kullanıcı kartına tıklayarak Genel ve Kurum rollerini ile şube erişimlerini tek işlemde (atomik) atayabilirsiniz. Şube seçilmezse rol tüm kurum genelinde çalışır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NeutralTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onEnsureStandardRoles,
                            modifier = Modifier.weight(1f).testTag("ensure_standard_roles_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eksik Standart Rolleri Ekle (11)", fontSize = 11.sp, maxLines = 1)
                        }
                        FilledTonalButton(
                            onClick = { showParentLinkDialog = true },
                            modifier = Modifier.weight(1f).testTag("open_parent_link_dialog_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.FamilyRestroom, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Veli Giriş Hesabı Bağla", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_search_input"),
                    placeholder = { Text("Ad soyad veya e-posta ile ara...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ara") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "Tümü (${visibleUsers.size})",
                        "ADMIN" to "Yönetici",
                        "TEACHER" to "Öğretmen",
                        "ACCOUNTANT" to "Muhasebe",
                        "PARENT" to "Veli"
                    )
                    filters.forEach { (roleKey, label) ->
                        FilterChip(
                            selected = selectedRoleFilter == roleKey,
                            onClick = { selectedRoleFilter = roleKey },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }

            items(visibleUsers, key = { it.id }) { user ->
                val isRootAccount = user.role.contains("SUPERADMIN", ignoreCase = true) ||
                    user.email.equals("root@scs360.com.tr", ignoreCase = true)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedUserForAccess = user }
                        .testTag("user_card_${user.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = user.full_name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = user.full_name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = NeutralTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isRootAccount) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = ScsOrbitGold, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                "ROOT KORUMALI",
                                                color = ScsPetrolGreen,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${user.email} • Şube: ${user.branch_name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NeutralTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = user.role,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = if (user.is_active) "Aktif" else "Pasif / Engelli",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (user.is_active) StatusSuccess else StatusWarning,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { selectedUserForAccess = user },
                                modifier = Modifier.testTag("user_access_btn_${user.id}")
                            ) {
                                Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rol & Şube Erişimi Ata", fontSize = 12.sp)
                            }

                            if (!isRootAccount) {
                                IconButton(
                                    onClick = { onDeleteUser(user.id) },
                                    modifier = Modifier.testTag("delete_user_${user.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Kullanıcıyı Sil",
                                        tint = StatusError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedUserForAccess?.let { targetUser ->
        val isRoot = targetUser.role.contains("SUPERADMIN", ignoreCase = true) ||
            targetUser.email.equals("root@scs360.com.tr", ignoreCase = true)

        val allRoles = listOf(
            "OWNER" to "Kurum Sahibi (Kurum Rolü • OWNER)",
            "ADMIN" to "Kurum Yöneticisi (Kurum Rolü • ADMIN)",
            "BRANCH_ADMIN" to "Şube Yöneticisi (Kurum Rolü • BRANCH_ADMIN)",
            "TEACHER" to "Öğretmen (Kurum Rolü • TEACHER)",
            "PARENT" to "Veli (Kurum Rolü • PARENT)",
            "ACCOUNTANT" to "Muhasebe (Kurum Rolü • ACCOUNTANT)",
            "STAFF" to "Personel (Kurum Rolü • STAFF)",
            "CANTEEN" to "Kantin (Kurum Rolü • CANTEEN)",
            "SECURITY" to "Güvenlik (Kurum Rolü • SECURITY)",
            "LIBRARIAN" to "Kütüphane (Kurum Rolü • LIBRARIAN)",
            "BUS_DRIVER" to "Servis (Kurum Rolü • BUS_DRIVER)"
        )
        var selectedRoles by remember(targetUser) {
            mutableStateOf(
                targetUser.role.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()
                    .ifEmpty { setOf("TEACHER") }
            )
        }
        var selectedBranchIds by remember(targetUser) {
            mutableStateOf(setOf(targetUser.branch_id).filter { it.isNotBlank() }.toSet())
        }
        var selectedStatus by remember(targetUser) {
            mutableStateOf(if (targetUser.is_active) "ACTIVE" else "INACTIVE")
        }

        AlertDialog(
            onDismissRequest = { selectedUserForAccess = null },
            title = {
                Column {
                    Text("${targetUser.full_name} • Erişim & Şube Ata", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(targetUser.email, fontSize = 12.sp, color = NeutralTextSecondary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isRoot) {
                        Surface(color = StatusWarningContainer, shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Root (is_super_admin) hesabı korumalıdır; pasife alınamaz, engellenemez veya silinemez.",
                                fontSize = 12.sp,
                                color = NeutralTextPrimary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Text("Hesap Durumu:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ACTIVE" to "Aktif", "INACTIVE" to "Pasif", "BLOCKED" to "Engelli").forEach { (st, lbl) ->
                                FilterChip(
                                    selected = selectedStatus == st,
                                    onClick = { selectedStatus = st },
                                    label = { Text(lbl, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    Text("Atanacak Roller (Çoklu Seçim):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    allRoles.forEach { (roleCode, roleTitle) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isRoot) {
                                    selectedRoles = if (selectedRoles.contains(roleCode)) {
                                        (selectedRoles - roleCode).ifEmpty { setOf(roleCode) }
                                    } else {
                                        selectedRoles + roleCode
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = selectedRoles.contains(roleCode),
                                onCheckedChange = { checked ->
                                    if (!isRoot) {
                                        selectedRoles = if (checked) {
                                            selectedRoles + roleCode
                                        } else {
                                            (selectedRoles - roleCode).ifEmpty { setOf(roleCode) }
                                        }
                                    }
                                },
                                enabled = !isRoot
                            )
                            Text(roleTitle, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider()
                    Text(
                        "Şube Kapsamı (Seçilmezse Tüm Kurum Genelinde Geçerlidir):",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    val branchOptions = if (state.branches.isNotEmpty()) state.branches else listOf(
                        Branch(id = "branch-1", name = "Merkez Kampüs"),
                        Branch(id = "branch-2", name = "Anadolu Lisesi Kampüsü")
                    )
                    branchOptions.forEach { br ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedBranchIds = if (selectedBranchIds.contains(br.id)) {
                                        selectedBranchIds - br.id
                                    } else {
                                        selectedBranchIds + br.id
                                    }
                                }
                        ) {
                            Checkbox(
                                checked = selectedBranchIds.contains(br.id),
                                onCheckedChange = { checked ->
                                    selectedBranchIds = if (checked) selectedBranchIds + br.id else selectedBranchIds - br.id
                                }
                            )
                            Text("${br.name} (${br.id})", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateUserAccess(
                            targetUser.id,
                            selectedRoles.toList(),
                            selectedBranchIds.distinct(),
                            if (isRoot) "ACTIVE" else selectedStatus
                        )
                        selectedUserForAccess = null
                    },
                    modifier = Modifier.testTag("save_user_access_btn")
                ) {
                    Text("Atomik Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedUserForAccess = null }) { Text("Kapat") }
            }
        )
    }

    if (showAddDialog) {
        var fullName by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("+90 5") }
        var selectedRole by remember { mutableStateOf("TEACHER") }
        var password by remember { mutableStateOf("Scs360!2026") }
        val branchId = state.branches.firstOrNull()?.id ?: "branch-1"

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Yeni Kullanıcı Oluştur", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Ad Soyad") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-Posta Adresi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefon") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Geçici Şifre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Başlangıç Rolü:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ADMIN", "TEACHER", "ACCOUNTANT", "PARENT").forEach { r ->
                            FilterChip(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r },
                                label = { Text(r, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (fullName.isNotBlank() && email.isNotBlank()) {
                            onAddUser(fullName.trim(), email.trim(), selectedRole, phone.trim(), branchId, password)
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

    if (showParentLinkDialog) {
        val students = state.students
        var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "std-1") }
        val selectedStudent = students.find { it.id == selectedStudentId } ?: students.firstOrNull()
        var selectedUserId by remember(selectedStudent) { mutableStateOf(selectedStudent?.linked_parent_user_id ?: "") }
        var receiveNotifications by remember(selectedStudent) { mutableStateOf(selectedStudent?.receive_notifications ?: true) }
        var smsPhone by remember(selectedStudent) { mutableStateOf(selectedStudent?.sms_phone ?: selectedStudent?.parent_phone ?: "") }
        var whatsappPhone by remember(selectedStudent) { mutableStateOf(selectedStudent?.whatsapp_phone ?: selectedStudent?.parent_phone ?: "") }

        AlertDialog(
            onDismissRequest = { showParentLinkDialog = false },
            title = {
                Column {
                    Text("Veli Giriş Hesabı Bağla (PUT /parents/{id}/account)", fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                    Text("Veli kaydını giriş hesabına bağlar ve PARENT rolünü güvenceye alır. Aynı kullanıcı iki farklı veli kaydına bağlanamaz (409).", fontSize = 11.sp, color = NeutralTextSecondary)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Öğrenci / Veli Kaydı Seçin:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    students.take(4).forEach { std ->
                        FilterChip(
                            selected = selectedStudentId == std.id,
                            onClick = { selectedStudentId = std.id },
                            label = { Text("${std.parent_name} (${std.full_name})", fontSize = 11.sp) }
                        )
                    }
                    Text("Bağlanacak Giriş Kullanıcısı (Boş bırakılırsa bağ kaldırılır):", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedUserId.isBlank(),
                            onClick = { selectedUserId = "" },
                            label = { Text("Bağlantıyı Kaldır (null)", fontSize = 11.sp) }
                        )
                        visibleUsers.take(2).forEach { u ->
                            FilterChip(
                                selected = selectedUserId == u.id,
                                onClick = { selectedUserId = u.id },
                                label = { Text(u.full_name, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = receiveNotifications, onCheckedChange = { receiveNotifications = it })
                        Text("Bildirim Al (receive_notifications)", fontSize = 12.sp)
                    }
                    OutlinedTextField(
                        value = smsPhone,
                        onValueChange = { smsPhone = it },
                        label = { Text("SMS Telefonu (Boş bırakılırsa temizlenir)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = whatsappPhone,
                        onValueChange = { whatsappPhone = it },
                        label = { Text("WhatsApp Telefonu (Boş bırakılırsa temizlenir)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onLinkParentAccount(
                            selectedStudentId,
                            selectedUserId.ifBlank { null },
                            receiveNotifications,
                            smsPhone.ifBlank { null },
                            whatsappPhone.ifBlank { null }
                        )
                        showParentLinkDialog = false
                    }
                ) {
                    Text("Hesap Bağını Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParentLinkDialog = false }) { Text("Kapat") }
            }
        )
    }
}
