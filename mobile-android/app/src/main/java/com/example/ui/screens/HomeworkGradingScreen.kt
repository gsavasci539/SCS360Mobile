package com.example.ui.screens

import com.example.ui.components.ResponsiveTableScope
import androidx.compose.animation.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HomeworkSubmission
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
fun HomeworkGradingScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onSubmitHomework: (String, String, String, String?, String?) -> Unit,
    onGradeSubmission: (String, Int, String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SUBMITTED, GRADED
    var showSubmitDialog by remember { mutableStateOf(false) }
    var gradingSubmission by remember { mutableStateOf<HomeworkSubmission?>(null) }
    var gradeScoreInput by remember { mutableStateOf("95") }
    var teacherFeedbackInput by remember { mutableStateOf("") }
    val tableModeState = rememberResolvedTableMode()
    val isCardMode = isCompactCardMode(tableModeState.value)

    val filteredSubmissions = remember(state.homeworkSubmissions, selectedFilter) {
        when (selectedFilter) {
            "SUBMITTED" -> state.homeworkSubmissions.filter { it.status == "SUBMITTED" }
            "GRADED" -> state.homeworkSubmissions.filter { it.status == "GRADED" }
            else -> state.homeworkSubmissions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Ödev & Değerlendirme Paneli", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Öğrenci Teslimleri & Öğretmen Puanlama",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("homework_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showSubmitDialog = true }) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Ödev Yükle", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showSubmitDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Ödev Teslim Et") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Tüm Teslimler (${state.homeworkSubmissions.size})", fontSize = 12.sp) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "SUBMITTED",
                        onClick = { selectedFilter = "SUBMITTED" },
                        label = { Text("Puan Bekleyen (${state.homeworkSubmissions.count { it.status == "SUBMITTED" }})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = AmberPrimary
                        )
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == "GRADED",
                        onClick = { selectedFilter = "GRADED" },
                        label = { Text("Notlandırılan (${state.homeworkSubmissions.count { it.status == "GRADED" }})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                }
            }

            ResponsiveTableScope() {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        RoleContextualAiCard(
                            screenName = "HOMEWORK_GRADING",
                            state = state,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
    
                    if (filteredSubmissions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AssignmentLate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "Kayıtlı ödev teslimi bulunamadı.",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        stickyHeader {
                            StickyTableHeaderRow(
                                columns = listOf(
                                    TableColumnSpec("Ödev / Öğrenci", 1.5f),
                                    TableColumnSpec("Teslim / Dosya", 1.2f),
                                    TableColumnSpec("Puan / İşlem", 1.0f, TextAlign.End)
                                ),
                                totalCount = filteredSubmissions.size,
                                viewModeState = tableModeState
                            )
                        }
    
                        itemsIndexed(filteredSubmissions, key = { _, it -> it.id }) { index, sub ->
                            if (isCardMode) {
                                CompactResponsiveCard(
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                            Text(sub.assignment_title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${sub.student_name} (${sub.class_name}) • ${sub.submitted_at}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
    
                                        if (sub.status == "GRADED" && sub.grade_score != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = EmeraldPrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    "${sub.grade_score} Puan",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    color = EmeraldPrimary
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = AmberPrimary.copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "Puanlanacak",
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AmberPrimary
                                                )
                                            }
                                        }
                                    }
    
                                    Text("Not: ${sub.submission_text}", fontSize = 11.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (sub.attachment_file_name != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("${sub.attachment_file_name} (${sub.attachment_file_size ?: ""})", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
    
                                        Button(
                                            onClick = {
                                                gradingSubmission = sub
                                                gradeScoreInput = (sub.grade_score ?: 95).toString()
                                                teacherFeedbackInput = sub.teacher_feedback ?: ""
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (sub.status == "GRADED") "Notu Güncelle" else "Puanla", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            } else {
                                CompactTableRow(
                                    index = index,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1.5f).padding(end = 6.dp)) {
                                        Text(sub.assignment_title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${sub.student_name} (${sub.class_name})", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    Column(modifier = Modifier.weight(1.2f).padding(end = 6.dp)) {
                                        Text(sub.submitted_at, fontSize = 11.sp, maxLines = 1)
                                        Text(sub.attachment_file_name ?: sub.submission_text, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Row(
                                        modifier = Modifier.weight(1.0f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (sub.status == "GRADED" && sub.grade_score != null) {
                                            Text("${sub.grade_score}p", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = EmeraldPrimary)
                                        } else {
                                            Text("Bekliyor", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AmberPrimary)
                                        }
                                        CompactRowActionIcon(
                                            icon = Icons.Default.Grade,
                                            contentDescription = "Puanla",
                                            tint = MaterialTheme.colorScheme.primary,
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            onClick = {
                                                gradingSubmission = sub
                                                gradeScoreInput = (sub.grade_score ?: 95).toString()
                                                teacherFeedbackInput = sub.teacher_feedback ?: ""
                                            }
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

    // Submit Homework Dialog
    if (showSubmitDialog) {
        var selectedMaterial by remember { mutableStateOf(state.courseMaterials.firstOrNull()) }
        var notes by remember { mutableStateOf("") }
        var fileName by remember { mutableStateOf("odev_teslim.pdf") }

        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            title = { Text("Ödev Teslim Et") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ödev / Materyal Seçimi:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.courseMaterials) { mat ->
                            val isSelected = selectedMaterial?.id == mat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMaterial = mat },
                                label = { Text(mat.title.take(20) + "...") }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Çözüm Açıklaması / Notunuz") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    OutlinedTextField(
                        value = fileName,
                        onValueChange = { fileName = it },
                        label = { Text("Ek Dosya Adı") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedMaterial?.let { mat ->
                            onSubmitHomework(mat.id, mat.title, notes.ifBlank { "Ödev sorularının tamamı çözüldü." }, fileName, "2.4 MB")
                        }
                        showSubmitDialog = false
                    }
                ) {
                    Text("Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Teacher Grading Dialog
    gradingSubmission?.let { sub ->
        AlertDialog(
            onDismissRequest = { gradingSubmission = null },
            title = { Text("Ödev Değerlendirme & Puanlama") },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("${sub.student_name} • ${sub.assignment_title}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = gradeScoreInput,
                        onValueChange = { gradeScoreInput = it.filter { ch -> ch.isDigit() }.take(3) },
                        label = { Text("Puan (0 - 100)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = teacherFeedbackInput,
                        onValueChange = { teacherFeedbackInput = it },
                        label = { Text("Öğretmen Geri Bildirimi") },
                        placeholder = { Text("Tebrikler, çözüm adımların çok düzenli...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val score = gradeScoreInput.toIntOrNull() ?: 90
                        onGradeSubmission(sub.id, score, teacherFeedbackInput.ifBlank { "Ödev başarıyla değerlendirildi." })
                        gradingSubmission = null
                    }
                ) {
                    Text("Notu Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { gradingSubmission = null }) {
                    Text("İptal")
                }
            }
        )
    }
}
