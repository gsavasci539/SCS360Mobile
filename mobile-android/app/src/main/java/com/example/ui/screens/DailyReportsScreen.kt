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
import com.example.data.remote.DailyReportPage
import com.example.data.remote.DailyReportStudentOption
import com.example.data.remote.NativeDailyReportClient
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyReportsScreen(
    state: Okul360UiState,
    onNavigateBack: (() -> Unit)? = null,
    onCreateReport: ((DailyReportDraft) -> Unit)? = null,
    onReload: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val requestScope = listOf(state.currentUser.id, state.currentUser.resolvedTenantId, state.workspaceOrganizationId,
        state.selectedBranchId, state.serverBaseUrl)
    var searchQuery by remember(requestScope) { mutableStateOf("") }
    var dateInput by remember(requestScope) { mutableStateOf("") }
    var selectedClassId by remember(requestScope) { mutableStateOf<String?>(null) }
    var page by remember(requestScope) { mutableIntStateOf(1) }
    var revision by remember(requestScope) { mutableIntStateOf(0) }
    var showCreateDialog by remember(requestScope) { mutableStateOf(false) }
    var selectedReportId by remember(requestScope) { mutableStateOf<String?>(null) }
    val canCreate = onCreateReport != null && NativeNavigationPolicy.has(state.currentUser, "daily_reports.write", "academic.write")
    val canRead = NativeNavigationPolicy.has(state.currentUser, "daily_reports.read", "academic.read")
    val classNames = remember(state.classes) { state.classes.associate { it.id to it.name } }
    val branch = state.selectedBranchId?.takeUnless { it.isBlank() || it == "all" }
    val classes = remember(state.classes, branch) { state.classes.filter { branch == null || it.branch_id == branch } }
    val searchTerm = searchQuery.trim()
    val date = parseDailyReportDate(dateInput)
    val validDate = dateInput.isBlank() || date != null
    val classroom = selectedClassId
    val requestedPage = page
    val result = if (canRead && validDate) {
        key(requestScope, classroom, searchTerm, date, requestedPage, revision, state.dailyReportSavedRevision) {
            produceState<Result<DailyReportPage>?>(null) {
                try {
                    val client = NativeDailyReportClient()
                    if (searchTerm.isNotEmpty()) delay(220)
                    value = Result.success(client.listPage(branch, classroom, searchTerm, date, requestedPage))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    value = Result.failure(failure)
                }
            }.value
        }
    } else null
    val reportPage = result?.getOrNull()
    val lastPage = (reportPage?.totalPages ?: 1).coerceAtLeast(1)
    LaunchedEffect(reportPage, page) {
        if (reportPage != null && page > lastPage) page = lastPage
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
                .testTag("daily_report_list")
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
                            if (canCreate) IconButton(
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

            if (!canRead) {
                item { Text("Kayıtlı karneleri görüntüleme yetkiniz yok.", modifier = Modifier.padding(16.dp)) }
                return@LazyColumn
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
                        onValueChange = { searchQuery = it.take(150); page = 1 },
                        placeholder = { Text("Öğrenci adı veya notlarda ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = ""; page = 1 }) {
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
                    DailyReportDateField(dateInput, valid = validDate, enabled = true, testTag = "daily_report_filter_date") {
                        dateInput = it
                        page = 1
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(selected = selectedClassId == null,
                                onClick = { selectedClassId = null; page = 1 },
                                label = { Text("Tüm sınıflar", fontSize = 12.sp) })
                        }
                        items(classes, key = { it.id }) { classroom ->
                            FilterChip(
                                selected = selectedClassId == classroom.id,
                                onClick = { selectedClassId = classroom.id; page = 1 },
                                label = { Text(classroom.name, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            if (!validDate) return@LazyColumn
            if (result == null || (reportPage != null && page > lastPage)) {
                item { Text("Günlük karneler yükleniyor…", Modifier.padding(16.dp)) }
            } else if (reportPage == null) {
                item {
                    Text("Günlük karneler yüklenemedi.", Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { revision++ }) { Text("Tekrar dene") }
                }
            } else if (reportPage.rows.isEmpty()) {
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
                items(reportPage.rows, key = { it.id }) { report ->
                    DailyReportCard(
                        report = report.copy(class_name = classNames[report.class_id].orEmpty()),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        onOpen = { selectedReportId = report.id },
                    )
                }
                if (lastPage > 1) item {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("${reportPage.total} kayıt · Sayfa $page/$lastPage")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { page-- }, enabled = page > 1) { Text("Önceki") }
                            TextButton(onClick = { page++ }, enabled = page < lastPage) { Text("Sonraki") }
                        }
                    }
                }
            }
        }
    }

    selectedReportId?.takeIf { canRead }?.let { id ->
        DailyReportDetailsDialog(id, state.currentUser.id,
            NativeNavigationPolicy.has(state.currentUser, "daily_reports.write", "academic.write"),
            onDismiss = { selectedReportId = null },
            onChanged = { selectedReportId = null; revision++; onReload?.invoke() })
    }

    if (showCreateDialog && canCreate) {
        val startingRevision = remember { state.dailyReportSavedRevision }
        LaunchedEffect(state.dailyReportSavedRevision) {
            if (state.dailyReportSavedRevision != startingRevision) showCreateDialog = false
        }
        AddDailyReportDialog(
            branchId = state.selectedBranchId,
            saving = state.dailyReportSaving,
            error = state.dailyReportSaveError,
            onDismiss = { showCreateDialog = false },
            onConfirm = { onCreateReport?.invoke(it) }
        )
    }
}

@Composable
fun AddDailyReportDialog(
    branchId: String?,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (DailyReportDraft) -> Unit
) {
    var student by remember(branchId) { mutableStateOf<DailyReportStudentOption?>(null) }
    var dateInput by remember { mutableStateOf(currentDailyReportDate()) }
    val reportDate = parseDailyReportDate(dateInput)
    val selectedStudentId = student?.id
    var attempted by remember(selectedStudentId) { mutableStateOf(false) }

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
    val selected = student
    val draft = if (selected != null && reportDate != null) {
        DailyReportDraft(selected.id, selected.classId, reportDate, breakfastStatus, lunchStatus, snackStatus,
            sleepMinutes, selectedMood, selectedActivities.toList(), teacherNote)
    } else null

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
                    DailyReportStudentPicker(branchId, null, student, enabled = !saving, onSelect = { student = it })
                    DailyReportDateField(dateInput, valid = reportDate != null, enabled = !saving) {
                        dateInput = it
                        attempted = false
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = report.student_name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
                Text(
                    text = listOf(report.class_name, report.date).filter(String::isNotBlank).joinToString(" • "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (report.mood_level.isNotBlank()) Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7)
                ) {
                    Text(report.mood_level, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
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
