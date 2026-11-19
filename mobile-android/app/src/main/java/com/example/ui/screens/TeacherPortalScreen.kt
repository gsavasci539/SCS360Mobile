package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.remote.DailyReportStudentOption
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.RoleSpeedDialFab
import com.example.ui.components.RoleSwitchBadge
import com.example.ui.components.SpeedDialAction
import com.example.ui.components.SubjectAttendanceStatisticsView
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.text.SimpleDateFormat
import java.util.*

enum class TeacherNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ATTENDANCE("Yoklama", Icons.Default.Checklist, Icons.Outlined.Checklist),
    MATERIALS("Materyal", Icons.Default.MenuBook, Icons.Outlined.MenuBook),
    MESSAGING("İletişim", Icons.Default.ChatBubble, Icons.Outlined.ChatBubble),
    DIARY("Karne", Icons.Default.EditCalendar, Icons.Outlined.EditCalendar),
    AI_CHATBOT("AI Asistan", Icons.Default.AutoAwesome, Icons.Outlined.AutoAwesome),
    SCHEDULE("Program", Icons.Default.Schedule, Icons.Outlined.Schedule)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherPortalScreen(
    state: Okul360UiState,
    onSaveAttendance: () -> Unit,
    onUpdateAttendanceStatus: (String, String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onSaveDailyReport: (DailyReportDraft) -> Unit,
    onMarkMedicineGiven: (String) -> Unit,
    onApproveLeave: (String) -> Unit,
    onUploadMaterial: (title: String, desc: String, subject: String, className: String, type: String, ext: String, size: String, dueDate: String?, isRequired: Boolean, summary: String) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onDownloadMaterial: (CourseMaterial) -> Unit = {},
    onDeleteMaterial: (String) -> Unit = {},
    onSendParentReminder: ((String, String) -> Unit)? = null,
    onSelectConversation: (String?) -> Unit = {},
    onOpenThemeConfig: () -> Unit = {},
    onRoleSelected: (UserRole) -> Unit = {},
    onOpenAiHomeworkGenerator: (() -> Unit)? = null,
    onOpenAiNarrativeGenerator: ((studentName: String, meal: String, sleep: String, mood: String, activities: String) -> Unit)? = null,
    onOpenAiAnalyticsHub: (() -> Unit)? = null,
    onOpenChatbot: (() -> Unit)? = null,
    onLogout: () -> Unit,
    onAttendanceClassSelected: (String) -> Unit = {},
    onAttendanceDateSelected: (String) -> Unit = {},
    onAttendanceRetry: () -> Unit = {},
    onAttendanceNoteChanged: (String, String?) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(TeacherNavTab.ATTENDANCE) }
    var teacherFabExpanded by remember { mutableStateOf(false) }

    val teacherActions = remember(onSaveAttendance) {
        listOf(
            SpeedDialAction(
                id = "teacher_save_attendance",
                title = "Yoklamayı Aç",
                description = "Günün kayıtlarını incele ve düzenle",
                icon = Icons.Default.FactCheck,
                containerColor = SecondaryEmerald,
                testTag = "teacher_fab_action_attendance",
                onClick = {
                    teacherFabExpanded = false
                    selectedTab = TeacherNavTab.ATTENDANCE
                }
            ),
            SpeedDialAction(
                id = "teacher_upload_material",
                title = "Ödev & Materyal",
                description = "Yeni ders dokümanı veya ödev paylaş",
                icon = Icons.Default.CloudUpload,
                containerColor = Color(0xFF2563EB), // Blue
                testTag = "teacher_fab_action_material",
                onClick = {
                    teacherFabExpanded = false
                    selectedTab = TeacherNavTab.MATERIALS
                }
            ),
            SpeedDialAction(
                id = "teacher_student_diary",
                title = "Gelişim & Karne",
                description = "Öğrenci günlük değerlendirmesi ve karne",
                icon = Icons.Default.EditNote,
                containerColor = Color(0xFF7C3AED), // Purple
                testTag = "teacher_fab_action_diary",
                onClick = {
                    teacherFabExpanded = false
                    selectedTab = TeacherNavTab.DIARY
                }
            )
        )
    }

    val isChatRoomActive = selectedTab == TeacherNavTab.MESSAGING && state.selectedConversationId != null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (!isChatRoomActive) {
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
                                        listOf(SecondaryEmerald, Color(0xFF0D9488))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Öğretmen Portalı",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = BrandNavy,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = state.currentUser.resolvedName,
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

                        // AI Intelligence Hub Quick Access
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDE9FE))
                                .clickable { onOpenAiAnalyticsHub?.invoke() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Zeka Merkezi",
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Theme Switcher Button
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .clickable { onOpenThemeConfig() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Tema Ayarları",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Logout button
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
        }
    },
    bottomBar = {
            if (!isChatRoomActive) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    TeacherNavTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab && tab != TeacherNavTab.AI_CHATBOT
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (tab == TeacherNavTab.AI_CHATBOT) {
                                    onOpenChatbot?.invoke()
                                } else {
                                    selectedTab = tab
                                }
                            },
                            icon = {
                                val unreadBadge = if (tab == TeacherNavTab.MESSAGING) {
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
                                selectedIconColor = SecondaryEmerald,
                                selectedTextColor = SecondaryEmerald,
                                indicatorColor = SecondaryEmerald.copy(alpha = 0.15f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
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
                TeacherNavTab.ATTENDANCE -> AttendanceScreen(
                    state = state, onClassSelected = onAttendanceClassSelected,
                    onStatusChanged = onUpdateAttendanceStatus, onMarkAllPresent = onMarkAllPresent,
                    onSaveAttendance = onSaveAttendance, onDateSelected = onAttendanceDateSelected,
                    onRetry = onAttendanceRetry, onNoteChanged = onAttendanceNoteChanged
                )
                TeacherNavTab.MATERIALS -> CourseMaterialsScreen(
                    state = state,
                    onUploadMaterial = onUploadMaterial,
                    onDownloadMaterial = onDownloadMaterial,
                    onDeleteMaterial = onDeleteMaterial,
                    onSendParentReminder = onSendParentReminder
                )
                TeacherNavTab.MESSAGING -> DirectMessagingScreen(
                    state = state,
                    onSelectConversation = onSelectConversation
                )
                TeacherNavTab.DIARY -> key(state.currentUser.id, state.currentUser.resolvedTenantId, state.workspaceOrganizationId,
                    state.selectedBranchId, state.attendanceClassId, state.serverBaseUrl) { TeacherDiaryFillTab(
                    state = state,
                    onOpenAiNarrativeGenerator = { studentName, m, sl, mo, act ->
                        onOpenAiNarrativeGenerator?.invoke(studentName, m, sl, mo, act)
                    },
                    onSaveReport = onSaveDailyReport
                ) }
                TeacherNavTab.SCHEDULE -> TeacherDayPane(state)
                TeacherNavTab.AI_CHATBOT -> {}
            }

            // Role-specific Floating Action Button for Teacher
            RoleSpeedDialFab(
                actions = teacherActions,
                expanded = teacherFabExpanded,
                onExpandedChange = { teacherFabExpanded = it },
                fabColor = SecondaryEmerald,
                fabLabel = "Öğretmen Hızlı Aksiyon",
                testTag = "teacher_speed_dial_fab",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            )
        }
    }
}

// -------------------------------------------------------------
// 1. TEACHER ATTENDANCE TAB
// -------------------------------------------------------------


@Composable
private fun StatBadgeItem(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = color)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun AttendanceChoiceChip(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (isSelected) activeColor else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

// -------------------------------------------------------------
// 2. TEACHER DIARY FILL TAB
// -------------------------------------------------------------
@Composable
private fun TeacherDiaryFillTab(
    state: Okul360UiState,
    onOpenAiNarrativeGenerator: (studentName: String, meal: String, sleep: String, mood: String, activities: String) -> Unit,
    onSaveReport: (DailyReportDraft) -> Unit
) {
    var currentStudent by remember { mutableStateOf<DailyReportStudentOption?>(null) }
    val selectedStudentId = currentStudent?.id
    val canWrite = com.example.ui.navigation.NativeNavigationPolicy.has(state.currentUser, "daily_reports.write", "academic.write")
    val attendanceClass = state.attendanceClassId.takeIf {
        com.example.ui.navigation.NativeNavigationPolicy.has(state.currentUser, "attendance.read")
    }
    val editable = canWrite && currentStudent != null && !state.dailyReportSaving
    var attempted by remember(selectedStudentId) { mutableStateOf(false) }
    var breakfast by remember(selectedStudentId, state.dailyReportSavedRevision) { mutableStateOf("") }
    var lunch by remember(selectedStudentId, state.dailyReportSavedRevision) { mutableStateOf("") }
    var snack by remember(selectedStudentId, state.dailyReportSavedRevision) { mutableStateOf("") }
    var sleepMinutes by remember(selectedStudentId, state.dailyReportSavedRevision) { mutableStateOf<Int?>(null) }
    var mood by remember(selectedStudentId, state.dailyReportSavedRevision) { mutableStateOf("") }
    var teacherNote by remember(selectedStudentId, state.dailyReportSavedRevision) { mutableStateOf("") }
    var narrativeStudent by remember(selectedStudentId) { mutableStateOf<String?>(null) }

    LaunchedEffect(state.activeDailyNarrative) {
        state.activeDailyNarrative?.let { narr ->
            val student = currentStudent
            if (student != null && narrativeStudent == student.id && narr.student_name == student.studentName) {
                teacherNote = narr.narrative_text
                narrativeStudent = null
            }
        }
    }

    val activityOptions = listOf(
        "Robotik Kodlama & Algoritma",
        "Suluboya & El Sanatları",
        "Açık Hava Bahçe Oyunları",
        "İngilizce Drama & Şarkı",
        "Müzik & Orff Ritim Atölyesi",
        "Montessori Matematik Kavramları"
    )
    val selectedActivities = remember(selectedStudentId, state.dailyReportSavedRevision) { mutableStateListOf<String>() }
    val draft = currentStudent?.let { student ->
        DailyReportDraft(student.id, student.classId, java.time.LocalDate.now(java.time.ZoneId.of("Europe/Istanbul")),
            breakfast, lunch, snack, sleepMinutes, mood, selectedActivities.toList(), teacherNote)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "📝 Günlük Karne Değerlendirmesi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrandNavy
            )
            Text(
                text = "Öğrenci seçerek günlük beslenme, uyku ve aktivite karnesini oluşturun",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // AI Analiz ve Pedagojik Rapor Kartı
        item {
            RoleContextualAiCard(
                screenName = "DAILY_REPORTS",
                state = state
            )
        }

        item {
            if (canWrite) DailyReportStudentPicker(state.selectedBranchId, attendanceClass, currentStudent,
                enabled = !state.dailyReportSaving, onSelect = { currentStudent = it })
            else Text("Günlük karne yazma yetkiniz yok.")
        }

        // Meal Consumption Form
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🥣 Öğün Tüketimleri", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                    
                    MealOptionRow("Kahvaltı", breakfast, editable) { breakfast = it }
                    MealOptionRow("Öğle Yemeği", lunch, editable) { lunch = it }
                    MealOptionRow("İkindi Beslenmesi", snack, editable) { snack = it }
                }
            }
        }

        // Sleep & Mood
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("😴 Öğle Uykusu / Dinlenme", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(0, 25, 50, 75, 100, 125, 150)) { minutes ->
                                FilterChip(selected = sleepMinutes == minutes, enabled = editable, onClick = { sleepMinutes = minutes },
                                    label = { Text(if (minutes == 0) "Uyumadı" else "$minutes dk") })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("😊 Gün İçi Duygu Durumu / Mod", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Çok Neşeli & Uyumlu", "Sakin", "Yorgun")) { m ->
                            val isSel = mood == m
                            FilterChip(
                                selected = isSel,
                                onClick = { mood = m },
                                enabled = editable,
                                label = { Text(m, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Activities Completed
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎨 Tamamlanan Kazanım Etkinlikleri", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                    activityOptions.forEach { act ->
                        val isChecked = selectedActivities.contains(act)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = editable) {
                                    if (isChecked) selectedActivities.remove(act) else selectedActivities.add(act)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                enabled = editable,
                                onCheckedChange = {
                                    if (it) selectedActivities.add(act) else selectedActivities.remove(act)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(act, fontSize = 13.sp, color = BrandNavy)
                        }
                    }
                }
            }
        }

        // Teacher Note & Submit
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("✍️ Öğretmen Özel Değerlendirme Notu", Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                        
                        FilledTonalButton(
                            onClick = {
                                val student = currentStudent ?: return@FilledTonalButton
                                narrativeStudent = student.id
                                val actStr = selectedActivities.joinToString(", ")
                                onOpenAiNarrativeGenerator(student.studentName, breakfast, sleepMinutes?.let { "$it Dakika" }.orEmpty(), mood, actStr)
                            },
                            enabled = canWrite && currentStudent != null && !state.dailyReportSaving,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFEDE9FE),
                                contentColor = Color(0xFF6D28D9)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✨ AI Not Üret", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedTextField(
                        value = teacherNote,
                        onValueChange = { teacherNote = it },
                        enabled = editable,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        label = { Text("Günün özeti ve veliye iletilecek not") }
                    )

                    if (attempted) state.dailyReportSaveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(
                        enabled = editable && draft?.hasObservation == true,
                        onClick = { draft?.let { attempted = true; onSaveReport(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.dailyReportSaving) "Kaydediliyor…" else "Karneyi Kaydet", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MealOptionRow(mealTitle: String, currentVal: String, enabled: Boolean, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(mealTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(listOf("Tümünü Yedi", "Yarısını Yedi", "Az Yedi", "Yemedi")) { opt ->
                val isSel = currentVal == opt
                Surface(
                    modifier = Modifier.clickable(enabled = enabled) { onSelect(opt) },
                    color = if (isSel) SecondaryEmerald else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = opt,
                        fontSize = 10.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. TEACHER NOTICES TAB (Velilerden Gelen Bildirimler)
// -------------------------------------------------------------
@Composable
private fun TeacherNoticesTab(
    state: Okul360UiState,
    onMarkMedicineGiven: (String) -> Unit,
    onApproveLeave: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "🔔 Velilerden Gelen Talepler",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrandNavy
            )
            Text(
                text = "Gün içi ilaç, izin ve teslim alma bildirimleri",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section: Medicine Notices
        item {
            Text("💊 İlaç Takip Bildirimleri", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
        }

        if (state.medicineNotices.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bugün için bekleyen ilaç bildirimi bulunmuyor.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(state.medicineNotices) { med ->
                val isGiven = med.status == "GIVEN"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    tonalElevation = 1.dp,
                    shadowElevation = 1.dp
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(med.student_name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                            Surface(
                                color = if (isGiven) SecondaryEmerald.copy(alpha = 0.15f) else AccentAmber.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (isGiven) "VERİLDİ" else "BEKLİYOR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGiven) SecondaryEmerald else AccentAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("İlaç: ${med.medicine_name} (${med.dose}) - Saat: ${med.time_to_give}", fontSize = 13.sp, color = BrandNavy)
                        Text("Talimat: ${med.instructions}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        if (!isGiven) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { onMarkMedicineGiven(med.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("İlaç Verildi Olarak İşaretle", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: Leave Notices
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("📅 İzin & Mazeret Talepleri", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentAmber)
        }

        if (state.leaveNotices.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bekleyen izin talebi bulunmuyor.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(state.leaveNotices) { leave ->
                val isApproved = leave.status == "APPROVED"
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(leave.student_name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                            Text("${leave.start_date} - ${leave.end_date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Mazeret: ${leave.reason_type}", fontSize = 13.sp, color = BrandNavy)
                        Text("Veli Notu: ${leave.parent_note}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        if (!isApproved) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onApproveLeave(leave.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("Talebi Onayla", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Section: Pickup Authorizations
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("🚗 Teslim Alma Yetkilileri", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF7C3AED))
        }

        if (state.pickupNotices.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Bugün için özel teslim alma bildirimi yok.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(14.dp))
                }
            }
        } else {
            items(state.pickupNotices) { pick ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(pick.student_name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                        Text("Teslim Alacak Kişi: ${pick.authorized_person_name} (${pick.relationship})", fontSize = 13.sp, color = BrandNavy)
                        Text("Telefon: ${pick.phone}", fontSize = 12.sp, color = PrimaryBlue)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. TEACHER SCHEDULE TAB
// -------------------------------------------------------------
// -------------------------------------------------------------
// 5. TEACHER CLASS & PROFILE TAB
// -------------------------------------------------------------
@Composable
private fun TeacherClassStudentsTab(
    students: List<Student>,
    currentUser: User,
    className: String,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var selectedStudentForProfile by remember { mutableStateOf<Student?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "👶 $className Öğrenci Listesi",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrandNavy
            )
            Text(
                text = "Toplam ${students.size} Öğrenci • Öğrenci profili, karne ve veli irtibat bilgileri",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(students) { std ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedStudentForProfile = std },
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(std.avatar_color_hex).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${std.first_name.take(1)}${std.last_name.take(1)}",
                                    fontWeight = FontWeight.Bold,
                                    color = Color(std.avatar_color_hex),
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(std.full_name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                                Text("No: ${std.student_number} • Kan: ${std.blood_type ?: "A Rh+"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { selectedStudentForProfile = std },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlueContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.School, contentDescription = "Profil", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${std.parent_phone}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.1f))
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (!std.allergies_or_diet.isNullOrBlank() && std.allergies_or_diet != "Yok") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ Alerji / Diyet: ${std.allergies_or_diet}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Teacher Profile Card & Logout
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👩‍🏫 Öğretmen Hesabı", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                    Text("Ad Soyad: ${currentUser.full_name}", fontSize = 13.sp, color = BrandNavy)
                    Text("E-Posta: ${currentUser.email}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Kurum: ${currentUser.institution_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
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

    selectedStudentForProfile?.let { std ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedStudentForProfile = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            StudentProfileScreen(
                student = std,
                onBack = { selectedStudentForProfile = null },
                onOpenReportCardViewer = {},
                onPrintReportCard = {
                    com.example.util.ReportCardPrintHelper.printStudentReportCard(context, std)
                }
            )
        }
    }
}
