package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.RoleSpeedDialFab
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.components.SpeedDialAction
import com.example.ui.dialogs.AddClassDialog
import com.example.ui.dialogs.AddStudentDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

enum class ManagerNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    OVERVIEW("Şube Özeti", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
    CLASSES("Sınıflar", Icons.Default.School, Icons.Outlined.School),
    STUDENTS("Öğrenciler", Icons.Default.People, Icons.Outlined.People),
    ANNOUNCEMENTS("Duyurular", Icons.Default.Campaign, Icons.Outlined.Campaign),
    AI_CHATBOT("AI Asistan", Icons.Default.AutoAwesome, Icons.Outlined.AutoAwesome),
    PROFILE("Şube & Profil", Icons.Default.Business, Icons.Outlined.Business)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchManagerPortalScreen(
    state: Okul360UiState,
    onAddStudent: (Student) -> Unit,
    onAddClass: (com.example.data.model.ClassGroup) -> Unit,
    onCreateAnnouncement: (title: String, content: String, cat: String, tgt: String) -> Unit,
    onDeleteAnnouncement: (String) -> Unit = {},
    onRoleSelected: (UserRole) -> Unit = {},
    onOpenChatbot: (() -> Unit)? = null,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(ManagerNavTab.OVERVIEW) }
    var showAddStudentModal by remember { mutableStateOf(false) }
    var showAddClassModal by remember { mutableStateOf(false) }
    var showNewAnnouncementModal by remember { mutableStateOf(false) }
    var managerFabExpanded by remember { mutableStateOf(false) }

    val managerActions = remember {
        listOf(
            SpeedDialAction(
                id = "manager_add_student",
                title = "Yeni Öğrenci Kaydı",
                description = "Şubeye yeni öğrenci ve veli kaydet",
                icon = Icons.Default.PersonAdd,
                containerColor = Color(0xFF0284C7), // Sky Blue
                testTag = "manager_fab_action_student",
                onClick = {
                    managerFabExpanded = false
                    showAddStudentModal = true
                }
            ),
            SpeedDialAction(
                id = "manager_add_class",
                title = "Yeni Sınıf Aç",
                description = "Şubeye yeni şube / sınıf grubu ekle",
                icon = Icons.Default.School,
                containerColor = Color(0xFF059669), // Emerald
                testTag = "manager_fab_action_class",
                onClick = {
                    managerFabExpanded = false
                    showAddClassModal = true
                }
            ),
            SpeedDialAction(
                id = "manager_publish_announcement",
                title = "Şube Duyurusu",
                description = "Tüm öğretmen ve velilere anlık bildiri",
                icon = Icons.Default.Campaign,
                containerColor = Color(0xFFD97706), // Amber
                testTag = "manager_fab_action_announcement",
                onClick = {
                    managerFabExpanded = false
                    showNewAnnouncementModal = true
                }
            )
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0369A1), Color(0xFF0284C7))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CorporateFare,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Şube Müdürlüğü",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = BrandNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${state.currentUser.full_name} • ${state.currentUser.branch_name}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        RoleSwitchBadge(
                            selectedRole = state.selectedRole,
                            onRoleSelected = onRoleSelected
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                                .clickable { onLogout() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Çıkış",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                ManagerNavTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab && tab != ManagerNavTab.AI_CHATBOT
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (tab == ManagerNavTab.AI_CHATBOT) {
                                onOpenChatbot?.invoke()
                            } else {
                                selectedTab = tab
                            }
                        },
                        icon = {
                            val unreadBadge = if (tab == ManagerNavTab.ANNOUNCEMENTS) {
                                state.announcements.count { it.read_count == 0 }.let { if (it > 0) it else 3 }
                            } else 0
                            if (unreadBadge > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(containerColor = AccentRose, contentColor = Color.White) {
                                            Text("$unreadBadge", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 9.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryBlue,
                            selectedTextColor = PrimaryBlue,
                            indicatorColor = PrimaryBlue.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                ManagerNavTab.OVERVIEW -> ManagerOverviewTab(
                    state = state,
                    onOpenAddStudent = { showAddStudentModal = true },
                    onOpenAddClass = { showAddClassModal = true },
                    onOpenAnnouncement = { showNewAnnouncementModal = true }
                )
                ManagerNavTab.CLASSES -> ManagerClassesTab(
                    state = state,
                    onOpenAddClass = { showAddClassModal = true }
                )
                ManagerNavTab.STUDENTS -> ManagerStudentsTab(
                    state = state,
                    onOpenAddStudent = { showAddStudentModal = true }
                )
                ManagerNavTab.ANNOUNCEMENTS -> AdminAnnouncementManagementScreen(
                    state = state,
                    onCreateAnnouncement = onCreateAnnouncement,
                    onDeleteAnnouncement = onDeleteAnnouncement
                )
                ManagerNavTab.PROFILE -> ManagerProfileTab(
                    state = state,
                    onLogout = onLogout
                )
                ManagerNavTab.AI_CHATBOT -> {}
            }

            // Role-specific Floating Action Button for Branch Manager
            RoleSpeedDialFab(
                actions = managerActions,
                expanded = managerFabExpanded,
                onExpandedChange = { managerFabExpanded = it },
                fabColor = Color(0xFF0284C7),
                fabLabel = "Müdür Hızlı Aksiyon",
                testTag = "manager_speed_dial_fab",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            )
        }
    }

    if (showAddStudentModal) AddStudentDialog(state = state, onDismiss = { showAddStudentModal = false }, onConfirm = onAddStudent)
    if (showAddClassModal) AddClassDialog(state = state, onDismiss = { showAddClassModal = false }, onConfirm = onAddClass)

    if (showNewAnnouncementModal) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Genel") }
        var target by remember { mutableStateOf("ALL") }

        AlertDialog(
            onDismissRequest = { showNewAnnouncementModal = false },
            title = { Text("📢 Şube Duyurusu Yayınla", fontWeight = FontWeight.Bold, color = BrandNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Duyuru Başlığı") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Duyuru Metni") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && content.isNotBlank()) {
                            onCreateAnnouncement(title, content, category, target)
                            showNewAnnouncementModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Yayınla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAnnouncementModal = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun ManagerOverviewTab(
    state: Okul360UiState,
    onOpenAddStudent: () -> Unit,
    onOpenAddClass: () -> Unit,
    onOpenAnnouncement: () -> Unit
) {
    val totalStudents = state.students.size
    val activeClasses = state.classes.size

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "🏢 ${state.currentUser.branch_name} Durum Raporu",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrandNavy
            )
            Text("Şube operasyon, doluluk ve devam durumu", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Summary KPI Grid
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ManagerKPICard("Öğrenci", "$totalStudents", PrimaryBlue, Icons.Default.School, Modifier.weight(1f))
                ManagerKPICard("Sınıf", "$activeClasses", SecondaryEmerald, Icons.Default.Class, Modifier.weight(1f))
                ManagerKPICard("Katılım", "%97", AccentAmber, Icons.Default.Checklist, Modifier.weight(1f))
            }
        }

        // AI Şube Operasyonel Sağlık Analizi
        item {
            RoleContextualAiCard(
                screenName = "DASHBOARD",
                state = state,
                initialExpanded = true
            )
        }

        // Quick Actions
        item {
            Text("⚡ Hızlı Şube İşlemleri", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenAddStudent,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Öğrenci Kaydı", fontSize = 11.5.sp)
                }

                Button(
                    onClick = onOpenAddClass,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                ) {
                    Icon(Icons.Default.AddHomeWork, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sınıf Aç", fontSize = 11.5.sp)
                }

                Button(
                    onClick = onOpenAnnouncement,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Duyuru", fontSize = 11.5.sp)
                }
            }
        }

        // Class Overview List
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("🏫 Şube Sınıfları & Doluluk", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
        }

        items(state.classes) { cls ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(cls.name, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = BrandNavy)
                        Text("Öğretmen: ${cls.teacher_name} • Derslik: ${cls.room_number ?: "101"}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${cls.student_count}/${cls.capacity} Öğrenci",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManagerKPICard(label: String, value: String, color: Color, icon: ImageVector, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandNavy)
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ManagerClassesTab(state: Okul360UiState, onOpenAddClass: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🏫 Şube Sınıfları", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandNavy)
                Button(
                    onClick = onOpenAddClass,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yeni Sınıf", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(state.classes) { cls ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(cls.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                    Text("Seviye: ${cls.grade_level} • Öğretmen: ${cls.teacher_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Kapasite: ${cls.student_count}/${cls.capacity} Öğrenci", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlue)
                }
            }
        }
    }
}

@Composable
private fun ManagerStudentsTab(state: Okul360UiState, onOpenAddStudent: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("👨‍🎓 Şube Öğrencileri (${state.students.size})", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandNavy)
                Button(
                    onClick = onOpenAddStudent,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yeni Öğrenci", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(state.students) { std ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(std.full_name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                        Text("${std.class_name} • Veli: ${std.parent_name}", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("No: ${std.student_number}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                }
            }
        }
    }
}

@Composable
private fun ManagerAnnouncementsTab(state: Okul360UiState, onOpenNewAnnouncement: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📢 Şube Duyuruları", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandNavy)
                Button(
                    onClick = onOpenNewAnnouncement,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yeni Duyuru", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(state.announcements) { ann ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(ann.title, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = BrandNavy)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(ann.content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Yayınlayan: ${ann.author_name} • ${ann.created_at}", fontSize = 11.sp, color = PrimaryBlue)
                }
            }
        }
    }
}

@Composable
private fun ManagerProfileTab(state: Okul360UiState, onLogout: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("🏢 Şube Müdürlüğü Hesabı", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandNavy)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ad Soyad: ${state.currentUser.full_name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandNavy)
                    Text("E-Posta: ${state.currentUser.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Kurum: ${state.currentUser.institution_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Şube: ${state.currentUser.branch_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Oturumu Kapat")
                    }
                }
            }
        }
    }
}
