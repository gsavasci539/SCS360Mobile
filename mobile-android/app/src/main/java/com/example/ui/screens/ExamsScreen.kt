package com.example.ui.screens

import com.example.ui.components.ResponsiveTableScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExamItem
import com.example.ui.components.CompactResponsiveCard
import com.example.ui.components.CompactRowActionIcon
import com.example.ui.components.CompactTableRow
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StickyTableHeaderRow
import com.example.ui.components.TableColumnSpec
import com.example.ui.components.isCompactCardMode
import com.example.ui.components.rememberResolvedTableMode
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExamsScreen(
    state: Okul360UiState,
    onBack: () -> Unit,
    onCreateExam: (ExamItem) -> Unit,
    onUpdateExam: (ExamItem) -> Unit,
    onDeleteExam: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterType by remember { mutableStateOf("ALL") }
    var selectedClassFilter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedExamForDetail by remember { mutableStateOf<ExamItem?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val tableModeState = rememberResolvedTableMode()
    val isCardMode = isCompactCardMode(tableModeState.value)

    val filteredExams = remember(state.exams, selectedFilterType, selectedClassFilter, searchQuery) {
        state.exams.filter { exam ->
            val matchesType = selectedFilterType == "ALL" || exam.exam_type.equals(selectedFilterType, ignoreCase = true)
            val matchesClass = selectedClassFilter == "ALL" || exam.class_name.equals(selectedClassFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    exam.title.contains(searchQuery, ignoreCase = true) ||
                    exam.course_name.contains(searchQuery, ignoreCase = true) ||
                    exam.teacher_name.contains(searchQuery, ignoreCase = true)
            matchesType && matchesClass && matchesSearch
        }
    }

    val typeFilters = listOf(
        "ALL" to "Tümü",
        "YAZILI" to "Yazılı",
        "DENEME" to "Deneme",
        "QUIZ" to "Quiz",
        "PROJE" to "Proje"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sınavlar & Akademik Takvim", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Yazılı, Deneme ve Değerlendirme Takibi",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Sınav Ekle", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Yeni Sınav Tanımla") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Compact Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Ders, konu veya öğretmen ara...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(typeFilters) { (key, label) ->
                    FilterChip(
                        selected = selectedFilterType == key,
                        onClick = { selectedFilterType = key },
                        label = { Text(label, fontSize = 12.sp) },
                        leadingIcon = if (selectedFilterType == key) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Exams List (Summary Cards + AI Card + Exam Rows scroll together)
            if (filteredExams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.School,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Kriterlere uygun sınav kaydı bulunamadı.", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Yeni bir sınav oluşturmak için aşağıdaki butonu kullanabilirsiniz.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                ResponsiveTableScope() {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Summary Cards inside scrollable feed
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text("Toplam Sınav", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${state.exams.size}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = SecondaryEmerald.copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text("Tamamlanan", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${state.exams.count { it.is_completed }}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706).copy(alpha = 0.15f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text("Yaklaşan", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${state.exams.count { !it.is_completed }}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                    }
                                }
                            }
                        }
    
                        item {
                            RoleContextualAiCard(
                                screenName = "EXAMS",
                                state = state,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                onQuickActionClick = { actionType ->
                                    if (actionType == "CREATE_EXAM") {
                                        showCreateDialog = true
                                    }
                                }
                            )
                        }
    
                        stickyHeader {
                            StickyTableHeaderRow(
                                columns = listOf(
                                    TableColumnSpec("Sınav / Ders", 1.4f),
                                    TableColumnSpec("Sınıf / Salon", 0.95f),
                                    TableColumnSpec("Tarih / Saat", 1.1f),
                                    TableColumnSpec("İşlem", 0.75f, TextAlign.End)
                                ),
                                totalCount = filteredExams.size,
                                viewModeState = tableModeState
                            )
                        }
    
                        itemsIndexed(filteredExams, key = { _, it -> it.id }) { index, exam ->
                            if (isCardMode) {
                                ExamItemCard(
                                    exam = exam,
                                    onClick = { selectedExamForDetail = exam },
                                    onToggleComplete = {
                                        onUpdateExam(exam.copy(is_completed = !exam.is_completed))
                                    },
                                    onDelete = { onDeleteExam(exam.id) },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            } else {
                                CompactTableRow(
                                    index = index,
                                    onClick = { selectedExamForDetail = exam },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1.4f).padding(end = 6.dp)) {
                                        Text(exam.title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${exam.course_name} • ${exam.typeDisplayName} (%${exam.weight_percentage})", fontSize = 10.5.sp, color = Color(exam.typeColorHex), fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                    Column(modifier = Modifier.weight(0.95f).padding(end = 6.dp)) {
                                        Text(exam.class_name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
                                        Text(exam.classroom, fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Column(modifier = Modifier.weight(1.1f).padding(end = 6.dp)) {
                                        Text(exam.date, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(exam.time, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    Row(
                                        modifier = Modifier.weight(0.75f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CompactRowActionIcon(
                                            icon = if (exam.is_completed) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                            contentDescription = "Tamamlandı İşaretle",
                                            tint = if (exam.is_completed) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            containerColor = if (exam.is_completed) StatusGreen.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant,
                                            onClick = { onUpdateExam(exam.copy(is_completed = !exam.is_completed)) }
                                        )
                                        CompactRowActionIcon(
                                            icon = Icons.Outlined.Delete,
                                            contentDescription = "Sil",
                                            tint = MaterialTheme.colorScheme.error,
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                            onClick = { onDeleteExam(exam.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Exam Detail Dialog
    selectedExamForDetail?.let { exam ->
        AlertDialog(
            onDismissRequest = { selectedExamForDetail = null },
            icon = { Icon(Icons.Default.Assignment, contentDescription = null, tint = Color(exam.typeColorHex)) },
            title = { Text(exam.title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ders: ${exam.course_name} (${exam.typeDisplayName})", fontWeight = FontWeight.SemiBold)
                    Text("Sınıf & Şube: ${exam.class_name} • Derslik: ${exam.classroom}")
                    Text("Tarih & Saat: ${exam.date} | ${exam.time}")
                    Text("Süre: ${exam.duration_minutes} Dakika • Ağırlık: %${exam.weight_percentage}")
                    Text("Öğretmen: ${exam.teacher_name}")
                    if (exam.topics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sınav Kapsamı & Konuları:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        exam.topics.forEach { topic ->
                            Text("• $topic", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (exam.is_completed) "Durum: Sınav Yapıldı / Notlar Girildi" else "Durum: Henüz Yapılmadı / Bekliyor",
                        color = if (exam.is_completed) StatusGreen else Color(0xFFD97706),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedExamForDetail = null }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Create Exam Dialog
    if (showCreateDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newCourse by remember { mutableStateOf("Matematik") }
        var newType by remember { mutableStateOf("YAZILI") }
        var newClass by remember { mutableStateOf("8-A") }
        var newDate by remember { mutableStateOf("2026-05-25") }
        var newTime by remember { mutableStateOf("09:30 - 10:15") }
        var newClassroom by remember { mutableStateOf("Derslik 101") }
        var newTeacher by remember { mutableStateOf("Ahmet Yılmaz") }
        var newDuration by remember { mutableStateOf("45") }
        var newWeight by remember { mutableStateOf("30") }
        var newTopicsText by remember { mutableStateOf("1. Ünite Tekrarı, Soru Çözümü") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Yeni Sınav Oluştur", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Sınav Başlığı") },
                            placeholder = { Text("Örn: 2. Dönem 1. Yazılı") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newCourse,
                                onValueChange = { newCourse = it },
                                label = { Text("Ders") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newClass,
                                onValueChange = { newClass = it },
                                label = { Text("Sınıf") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newType,
                                onValueChange = { newType = it },
                                label = { Text("Tür (YAZILI/DENEME/QUIZ)") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newDuration,
                                onValueChange = { newDuration = it },
                                label = { Text("Süre (dk)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newDate,
                                onValueChange = { newDate = it },
                                label = { Text("Tarih") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = newTime,
                                onValueChange = { newTime = it },
                                label = { Text("Saat Aralığı") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = newClassroom,
                            onValueChange = { newClassroom = it },
                            label = { Text("Derslik / Salon") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newTeacher,
                            onValueChange = { newTeacher = it },
                            label = { Text("Gözetmen / Sorumlu Öğretmen") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = newTopicsText,
                            onValueChange = { newTopicsText = it },
                            label = { Text("Kapsam / Konular (virgülle ayırın)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            val topics = newTopicsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onCreateExam(
                                ExamItem(
                                    id = "exam-${System.currentTimeMillis()}",
                                    title = newTitle,
                                    course_name = newCourse,
                                    exam_type = newType.uppercase(),
                                    date = newDate,
                                    time = newTime,
                                    classroom = newClassroom,
                                    class_name = newClass,
                                    teacher_name = newTeacher,
                                    duration_minutes = newDuration.toIntOrNull() ?: 40,
                                    topics = topics,
                                    weight_percentage = newWeight.toIntOrNull() ?: 30,
                                    is_completed = false
                                )
                            )
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Kaydet & Yayınla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
fun ExamItemCard(
    exam: ExamItem,
    onClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompactResponsiveCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(exam.typeColorHex).copy(alpha = 0.14f)
                ) {
                    Text(
                        text = exam.typeDisplayName,
                        color = Color(exam.typeColorHex),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = exam.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CompactRowActionIcon(
                    icon = if (exam.is_completed) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = "Tamamlandı İşaretle",
                    tint = if (exam.is_completed) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    containerColor = if (exam.is_completed) StatusGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    onClick = onToggleComplete
                )
                CompactRowActionIcon(
                    icon = Icons.Outlined.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    onClick = onDelete
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${exam.course_name} • ${exam.class_name} (%${exam.weight_percentage})",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 6.dp)
            )
            Text(
                text = "${exam.date} • ${exam.time} • ${exam.classroom}",
                fontSize = 10.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
