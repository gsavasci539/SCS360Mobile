package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RoleDefinition
import com.example.data.model.UserRole
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.dialogs.ConfirmDeleteDialog
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun RolesScreen(
    state: Okul360UiState,
    onRoleSelected: (UserRole) -> Unit,
    onTogglePermission: (roleId: String, permissionCode: String, isGranted: Boolean) -> Unit,
    onCreateRole: (name: String, code: String, desc: String) -> Unit,
    onDeleteRole: (String) -> Unit
) {
    var showAddRoleDialog by remember { mutableStateOf(false) }
    var roleToDelete by remember { mutableStateOf<RoleDefinition?>(null) }
    var selectedRoleDetails by remember { mutableStateOf<RoleDefinition?>(state.roleDefinitions.firstOrNull()) }

    val allAvailablePermissions = listOf(
        "STUDENTS_READ" to "Öğrenci Listesini Görüntüleme",
        "STUDENTS_WRITE" to "Yeni Öğrenci Ekleme ve Düzenleme",
        "ATTENDANCE_TAKE" to "Günlük Yoklama Alma ve Kaydetme",
        "FINANCE_READ" to "Mali Raporları ve Faturaları Görme",
        "FINANCE_COLLECT" to "Tahsilat Girişi & Makbuz Kesme",
        "ANNOUNCEMENTS_POST" to "Kurumsal Duyuru Yayınlama",
        "REPORTS_VIEW" to "Detaylı Yönetici Raporlarını İnceleme",
        "SETTINGS_ADMIN" to "Sistem ve Yetkilendirme Ayarları",
        "dismissal.read" to "Öğrenci Teslim Masası & Yetkilerini Görüntüleme",
        "dismissal.write" to "Teslim Yetkisi Tanımlama & Kimlikle Teslim Kaydı",
        "guidance.read_confidential" to "Rehberlik (PDR) Gizli Görüşme Kayıtlarını Okuma",
        "security.gate_verify" to "Kapı Geçiş, QR & SCS360 Barkod Doğrulama",
        "payroll.approve" to "Personel Bordro, Puantaj & Ek Ders Onaylama",
        "subscription.manage" to "Kurum Üyelik, Kota ve Banka Havale Bildirimi"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🛡️ Rol & Yetki Yönetimi",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = BrandNavy
                )
                Text(
                    text = "Kullanıcı rolleri ve erişim izinleri matrisi",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddRoleDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Yeni Rol", fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // AI Yetki & Güvenlik Analiz Kartı
            item {
                RoleContextualAiCard(
                    screenName = "SECURITY",
                    state = state,
                    initialExpanded = false
                )
            }

            // Role Simulator Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎭 Aktif Görünüm Rolü (Simülasyon)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = "Uygulamayı seçtiğiniz rolün yetki ve arayüzüyle deneyimleyin:",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(UserRole.entries.toList()) { role ->
                                val isSelected = state.selectedRole == role
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onRoleSelected(role) },
                                    label = { Text(role.title, fontSize = 11.sp) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Role Definitions List
            item {
                Text(
                    text = "Tanımlı Sistem Rolleri (${state.roleDefinitions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandNavy
                )
            }

            items(state.roleDefinitions) { roleDef ->
                val isCurrentSelected = selectedRoleDetails?.id == roleDef.id

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedRoleDetails = roleDef },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCurrentSelected) Color(0xFFF0FDF4) else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isCurrentSelected) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF10B981)) else null,
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(text = roleDef.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                                    Text(text = "Kod: ${roleDef.code} • ${roleDef.permissions.size} Yetki", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            if (!roleDef.is_system) {
                                IconButton(
                                    onClick = { roleToDelete = roleDef },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                                }
                            } else {
                                Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp)) {
                                    Text(text = "Sistem Rolü", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = roleDef.description, fontSize = 12.sp, color = Color(0xFF475569))
                    }
                }
            }

            // Permissions Matrix for selected role
            selectedRoleDetails?.let { role ->
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🔒 '${role.name}' Yetki Matrisi",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BrandNavy
                            )
                            Text(
                                text = "Bu role tanınan yetkileri dinamik olarak açıp kapatabilirsiniz:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            allAvailablePermissions.forEach { (code, title) ->
                                val isGranted = role.permissions.contains(code) || role.permissions.contains("ALL_ACCESS")

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(text = code, fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    }
                                    Switch(
                                        checked = isGranted,
                                        onCheckedChange = { checked ->
                                            onTogglePermission(role.id, code, checked)
                                        }
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Role Dialog
    if (showAddRoleDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddRoleDialog = false },
            title = { Text("Yeni Rol Tanımla", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Rol Adı (Örn: Rehberlik Uzmanı)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Rol Kodu (Örn: COUNSELOR)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Rol Açıklaması") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && code.isNotBlank()) {
                            onCreateRole(name, code, desc)
                            showAddRoleDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Rolü Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoleDialog = false }) { Text("Vazgeç") }
            }
        )
    }

    roleToDelete?.let { role ->
        ConfirmDeleteDialog(
            title = "Rolü Sil",
            message = "${role.name} rolünü silmek istiyor musunuz?",
            onDismiss = { roleToDelete = null },
            onConfirm = {
                onDeleteRole(role.id)
                roleToDelete = null
            }
        )
    }
}
