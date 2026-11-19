package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyReport
import com.example.data.model.DailyReportDraft
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportsScreen(
    state: Okul360UiState,
    onNavigateBack: (() -> Unit)? = null,
    onCreateReport: ((DailyReportDraft) -> Unit)? = null,
    onReload: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember(state.currentUser.id, state.workspaceOrganizationId, state.selectedBranchId) { mutableStateOf(false) }
    var selectedReportId by remember(state.currentUser.id, state.currentUser.resolvedTenantId, state.workspaceOrganizationId,
        state.selectedBranchId, state.serverBaseUrl) { mutableStateOf<String?>(null) }
    val canCreate = onCreateReport != null && NativeNavigationPolicy.has(state.currentUser, "daily_reports.write", "academic.write")
    val classNames = remember(state.classes) { state.classes.associate { it.id to it.name } }
    LaunchedEffect(state.selectedBranchId, state.workspaceOrganizationId, state.currentUser.id) {
        if (!state.dailyReportsLoading && !state.dailyReportsLoaded) onReload?.invoke()
    }

    val filteredReports = remember(state.dailyReports, classNames, searchQuery, selectedClassFilter) {
        state.dailyReports.map { it.copy(class_name = classNames[it.class_id].orEmpty()) }.filter { report ->
            val matchesSearch = searchQuery.isBlank() ||
                    report.student_name.contains(searchQuery, ignoreCase = true) ||
                    report.class_name.contains(searchQuery, ignoreCase = true) ||
                    report.teacher_note.contains(searchQuery, ignoreCase = true)
            val matchesClass = selectedClassFilter == "ALL" || report.class_name.equals(selectedClassFilter, ignoreCase = true)
            matchesSearch && matchesClass
        }
    }

    val classList = remember(state.classes) {
        listOf("ALL") + state.classes.map { it.name }.distinct()
    }

    Scaffold(
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Günlük Karne & Gelişim Bülteni",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = BrandNavy
                            )
                            Text(
                                text = "Anaokulu, kreş ve etüt anlık beslenme & uyku",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("daily_reports_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = PrimaryBlue)
                        }
                    },
                    actions = {
                        if (canCreate) IconButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.testTag("daily_reports_add_btn")
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = "Rapor Ekle", tint = PrimaryBlue)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        },
        floatingActionButton = {
            if (canCreate) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Günlük Rapor Yaz") },
                    containerColor = SecondaryEmerald,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("daily_report_fab")
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            // Header summary if no topBar
            if (onNavigateBack == null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Günlük Gelişim Karnesi & Bülten",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                )
                                Text(
                                    text = "Anaokulu, kreş ve etüt öğrencileri için anlık beslenme, uyku ve aktivite takibi",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            IconButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue.copy(alpha = 0.1f))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Yeni Rapor", tint = PrimaryBlue)
                            }
                        }
                    }
                }
            }

            // AI Analiz & Pedagojik Öneri Kartı
            item {
                RoleContextualAiCard(
                    screenName = "DAILY_REPORTS",
                    state = state,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onQuickActionClick = { actionType ->
                        if (actionType == "OPEN_AI_NARRATIVE") {
                            showCreateDialog = true
                        }
                    }
                )
            }

            // Search and Class Filters
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Öğrenci adı veya notlarda ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Temizle")
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_report_search_input"),
                        singleLine = true
                    )

                    // Class Filter Chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(classList) { cls ->
                            val isSelected = selectedClassFilter == cls
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedClassFilter = cls },
                                label = { Text(if (cls == "ALL") "Tüm Şubeler" else cls, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            if (state.dailyReportsLoading) {
                item { CircularProgressIndicator(Modifier.padding(16.dp)) }
            } else if (state.dailyReportsError != null) {
                item {
                    Text(state.dailyReportsError, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
                    if (onReload != null) TextButton(onClick = onReload) { Text("Tekrar dene") }
                }
            } else if (!state.dailyReportsLoaded) {
                item { Text("Karne listesi henüz yüklenmedi.", Modifier.padding(horizontal = 16.dp)) }
            } else if (filteredReports.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Kayıtlı Günlük Karne Bulunamadı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            if (canCreate) Button(
                                onClick = { showCreateDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("İlk Raporu Yaz")
                            }
                        }
                    }
                }
            } else {
                items(filteredReports, key = { it.id }) { report ->
                    DailyReportCard(
                        report = report,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        onOpen = { selectedReportId = report.id },
                    )
                }
            }
        }
    }

    selectedReportId?.let { id ->
        DailyReportDetailsDialog(id, state.currentUser.id,
            NativeNavigationPolicy.has(state.currentUser, "daily_reports.write", "academic.write"),
            onDismiss = { selectedReportId = null },
            onChanged = { selectedReportId = null; onReload?.invoke() })
    }

    if (showCreateDialog && canCreate) {
        val startingRevision = remember { state.dailyReportSavedRevision }
        LaunchedEffect(state.dailyReportSavedRevision) {
            if (state.dailyReportSavedRevision != startingRevision) showCreateDialog = false
        }
        AddDailyReportDialog(
            students = state.students.filter { it.class_id.isNotBlank() },
            saving = state.dailyReportSaving,
            error = state.dailyReportSaveError,
            onDismiss = { showCreateDialog = false },
            onConfirm = { onCreateReport?.invoke(it) }
        )
    }
}

@Composable
fun AddDailyReportDialog(
    students: List<com.example.data.model.Student>,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (DailyReportDraft) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf("") }
    val student = students.find { it.id == selectedStudentId }
    var studentDropdownExpanded by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }

    var selectedMood by remember(selectedStudentId) { mutableStateOf("") }
    var breakfastStatus by remember(selectedStudentId) { mutableStateOf("") }
    var lunchStatus by remember(selectedStudentId) { mutableStateOf("") }
    var snackStatus by remember(selectedStudentId) { mutableStateOf("") }
    var sleepMinutes by remember(selectedStudentId) { mutableStateOf<Int?>(null) }
    var teacherNote by remember(selectedStudentId) { mutableStateOf("") }

    val allActivities = listOf(
        "Robotik Kodlama", "Resim & El Becerisi", "Bahçe Oyunları",
        "Müzik & Ritim", "İngilizce Çemberi", "Satranç", "Drama & Masal"
    )
    var selectedActivities by remember(selectedStudentId) { mutableStateOf(emptySet<String>()) }
    val draft = student?.let { DailyReportDraft(it.id, it.class_id, LocalDate.now(ZoneId.of("Europe/Istanbul")),
        breakfastStatus, lunchStatus, snackStatus, sleepMinutes, selectedMood, selectedActivities.toList(), teacherNote) }

    val moodOptions = listOf(
        "Çok Neşeli & Uyumlu" to "🌟",
        "Mutlu & Sakin" to "😊",
        "Enerjik & Hareketli" to "⚡",
        "Biraz Yorgun" to "🥱",
        "Hassas & Duygusal" to "🥺"
    )

    val mealOptions = listOf("Tümünü Yedi", "Yarısını Yedi", "Az Yedi", "Yemedi")
    val sleepOptions = listOf(0, 30, 60, 90, 120)

    Dialog(
        onDismissRequest = { if (!saving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(12.dp)
            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Yeni Günlük Karne & Bülten", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Student Picker
                    Text("Öğrenci Seçimi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { studentDropdownExpanded = true },
                            enabled = !saving,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(student?.full_name ?: "Öğrenci Seçin")
                        }
                        DropdownMenu(
                            expanded = studentDropdownExpanded,
                            onDismissRequest = { studentDropdownExpanded = false }
                        ) {
                            students.forEach { std ->
                                DropdownMenuItem(
                                    text = { Text("${std.full_name} (${std.class_name})") },
                                    onClick = {
                                        selectedStudentId = std.id
                                        studentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Mood Selection
                    Text("Günün Duygu Durumu (Mod)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(moodOptions) { (mood, emoji) ->
                            FilterChip(
                                selected = selectedMood == mood,
                                enabled = !saving,
                                onClick = { selectedMood = mood },
                                label = { Text("$emoji $mood", fontSize = 11.5.sp) }
                            )
                        }
                    }

                    // Meals Selection
                    Text("Beslenme Durumu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("Kahvaltı:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(mealOptions) { opt ->
                            FilterChip(
                                selected = breakfastStatus == opt,
                                enabled = !saving,
                                onClick = { breakfastStatus = opt },
                                label = { Text(opt.replace(" Yedi", ""), fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("Öğle Yemeği:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(mealOptions) { opt ->
                            FilterChip(
                                selected = lunchStatus == opt,
                                enabled = !saving,
                                onClick = { lunchStatus = opt },
                                label = { Text(opt.replace(" Yedi", ""), fontSize = 10.sp) }
                            )
                        }
                    }

                    Text("İkindi Beslenmesi:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(mealOptions) { opt ->
                            FilterChip(
                                selected = snackStatus == opt,
                                enabled = !saving,
                                onClick = { snackStatus = opt },
                                label = { Text(opt.replace(" Yedi", ""), fontSize = 10.sp) }
                            )
                        }
                    }

                    // Sleep Selection
                    Text("Öğle Uykusu", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(sleepOptions) { minutes ->
                            FilterChip(
                                selected = sleepMinutes == minutes,
                                enabled = !saving,
                                onClick = { sleepMinutes = minutes },
                                label = { Text(if (minutes == 0) "Uyumadı" else "$minutes dk", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Activities
                    Text("Günün Etkinlikleri", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allActivities.chunked(2).forEach { rowActs ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowActs.forEach { act ->
                                    val isSelected = act in selectedActivities
                                    FilterChip(
                                        selected = isSelected,
                                        enabled = !saving,
                                        onClick = {
                                            selectedActivities = if (isSelected) selectedActivities - act else selectedActivities + act
                                        },
                                        label = { Text(act, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }

                    // Teacher Note
                    OutlinedTextField(
                        value = teacherNote,
                        onValueChange = { teacherNote = it },
                        enabled = !saving,
                        label = { Text("Öğretmen Gözlemi ve Veli Notu") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                }
                if (attempted) error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, enabled = !saving) { Text("İptal") }
                    Button(
                        enabled = !saving && draft?.hasObservation == true,
                        onClick = { draft?.let { attempted = true; onConfirm(it) } },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald),
                    ) {
                        Text(if (saving) "Kaydediliyor…" else "Kaydet")
                    }
                }
            }
        }
    }
}

@Composable
fun DailyReportCard(
    report: DailyReport,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = report.student_name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "${report.class_name} • ${report.date}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "😊", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = report.mood_level,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Meals & Sleep Breakdown
            report.meal_status?.let { Text("Beslenme · $it") }
            report.sleep_status?.let { Text("Uyku · $it") }

            Spacer(modifier = Modifier.height(12.dp))

            // Activities Done
            Text(
                text = "Günün Etkinlikleri",
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(report.activities_done) { act ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryBlueContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = act,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = OnPrimaryBlueContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Teacher Note
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Öğretmen notu",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "\"${report.teacher_note}\"",
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            TextButton(onClick = onOpen) { Text("Karneyi aç") }
        }
    }
}
