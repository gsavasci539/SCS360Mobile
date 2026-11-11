package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CourseMaterial
import com.example.data.model.UserRole
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

/**
 * Digital Course Materials, Worksheets, Study Sheets & Homework Documents Screen.
 * Allows teachers to publish digital documents and study sheets per subject,
 * and allows parents/students to view, search, preview, and download them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseMaterialsScreen(
    state: Okul360UiState,
    onUploadMaterial: (title: String, desc: String, subject: String, className: String, type: String, ext: String, size: String, dueDate: String?, isRequired: Boolean, topicSummary: String) -> Unit,
    onDownloadMaterial: (CourseMaterial) -> Unit,
    onDeleteMaterial: (String) -> Unit,
    onAcknowledgeMaterial: (String) -> Unit = {},
    onSendParentReminder: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("TÜMÜ") }
    var selectedType by remember { mutableStateOf("ALL") }
    var selectedClass by remember { mutableStateOf("ALL") }

    var showUploadModal by remember { mutableStateOf(false) }
    var previewMaterial by remember { mutableStateOf<CourseMaterial?>(null) }
    var materialToDelete by remember { mutableStateOf<CourseMaterial?>(null) }
    var selectedMaterialForAckList by remember { mutableStateOf<CourseMaterial?>(null) }

    val subjects = listOf("TÜMÜ", "Matematik", "Fen Bilimleri", "Türkçe", "İngilizce", "Robotik & Kodlama", "Sosyal Bilgiler")
    val materialTypes = listOf(
        "ALL" to "Tümü",
        "NOTE" to "📚 Ders Notları",
        "WORKSHEET" to "📄 Çalışma Föyleri",
        "HOMEWORK" to "📝 Ödev Dökümanları",
        "EXAM_PREP" to "🎯 Sınav Hazırlık",
        "PRESENTATION" to "🔬 Sunum & Slayt"
    )

    val filteredMaterials = remember(
        state.courseMaterials,
        searchQuery,
        selectedSubject,
        selectedType,
        selectedClass
    ) {
        state.courseMaterials.filter { item ->
            val matchQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.description.contains(searchQuery, ignoreCase = true) ||
                    item.subject_name.contains(searchQuery, ignoreCase = true) ||
                    item.teacher_name.contains(searchQuery, ignoreCase = true) ||
                    item.topic_summary.contains(searchQuery, ignoreCase = true)

            val matchSubject = selectedSubject == "TÜMÜ" || item.subject_name.equals(selectedSubject, ignoreCase = true)
            val matchType = selectedType == "ALL" || item.material_type.equals(selectedType, ignoreCase = true)
            val matchClass = selectedClass == "ALL" || item.class_name.equals(selectedClass, ignoreCase = true)

            matchQuery && matchSubject && matchType && matchClass
        }
    }

    val isTeacherOrAdmin = state.selectedRole == UserRole.TEACHER ||
            state.selectedRole == UserRole.ADMIN ||
            state.selectedRole == UserRole.MANAGER
    val isParent = state.selectedRole == UserRole.PARENT

    Scaffold(
        floatingActionButton = {
            if (isTeacherOrAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showUploadModal = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    text = { Text("Yeni Materyal Yükle", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("fab_upload_course_material")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
        ) {
            // Header Stats Banner
            item {
                CourseMaterialsOverviewCard(
                    totalCount = state.courseMaterials.size,
                    notesCount = state.courseMaterials.count { it.material_type == "NOTE" },
                    worksheetsCount = state.courseMaterials.count { it.material_type == "WORKSHEET" },
                    homeworksCount = state.courseMaterials.count { it.material_type == "HOMEWORK" },
                    totalDownloads = state.courseMaterials.sumOf { it.download_count }
                )
            }

            // Contextual AI Insights
            item {
                RoleContextualAiCard(
                    screenName = "COURSE_MATERIALS",
                    state = state
                )
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_materials_input"),
                    placeholder = { Text("Ders notu, föy, ödev veya konu ara...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Ara",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }

            // Subject Filter Tabs
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BRANŞ / DERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(subjects) { subj ->
                            val isSelected = selectedSubject.equals(subj, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSubject = subj },
                                label = {
                                    Text(
                                        text = subj,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Material Type Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "MATERYAL TÜRÜ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(materialTypes) { (typeKey, label) ->
                            val isSelected = selectedType == typeKey
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .clickable { selectedType = typeKey }
                                    .testTag("type_filter_$typeKey")
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Results count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paylaşılan Materyaller (${filteredMaterials.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (searchQuery.isNotEmpty() || selectedSubject != "TÜMÜ" || selectedType != "ALL") {
                        TextButton(
                            onClick = {
                                searchQuery = ""
                                selectedSubject = "TÜMÜ"
                                selectedType = "ALL"
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Filtreleri Sıfırla", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Empty state
            if (filteredMaterials.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "Materyal Bulunamadı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Arama kriterlerinize uygun ders notu veya föy bulunmuyor.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // List of Material Cards
                items(filteredMaterials, key = { it.id }) { material ->
                    CourseMaterialCard(
                        material = material,
                        canDelete = isTeacherOrAdmin,
                        isParent = isParent,
                        onPreview = { previewMaterial = material },
                        onDownload = { onDownloadMaterial(material) },
                        onDelete = { materialToDelete = material },
                        onAcknowledge = { onAcknowledgeMaterial(material.id) },
                        onViewAcks = { selectedMaterialForAckList = material },
                        onSendReminder = {
                            onSendParentReminder?.invoke(material.id, material.title)
                        }
                    )
                }
            }
        }
    }

    // Acknowledgment List Modal
    selectedMaterialForAckList?.let { material ->
        MaterialAcknowledgmentListDialog(
            material = material,
            allStudents = state.students.filter { it.class_name.equals(material.class_name, ignoreCase = true) }.ifEmpty { state.students },
            onDismiss = { selectedMaterialForAckList = null },
            onSendReminder = {
                onSendParentReminder?.invoke(material.id, material.title)
            }
        )
    }

    // Upload Material Modal
    if (showUploadModal) {
        UploadCourseMaterialDialog(
            subjects = subjects.filter { it != "TÜMÜ" },
            onDismiss = { showUploadModal = false },
            onUpload = { title, desc, subject, className, type, ext, size, dueDate, isRequired, summary ->
                onUploadMaterial(title, desc, subject, className, type, ext, size, dueDate, isRequired, summary)
                showUploadModal = false
            }
        )
    }

    // Document Preview Modal
    previewMaterial?.let { material ->
        DocumentPreviewDialog(
            material = material,
            onDismiss = { previewMaterial = null },
            onDownload = {
                onDownloadMaterial(material)
                previewMaterial = null
            }
        )
    }

    // Confirm Delete Dialog
    materialToDelete?.let { material ->
        AlertDialog(
            onDismissRequest = { materialToDelete = null },
            title = { Text("Materyali Sil", fontWeight = FontWeight.Bold) },
            text = { Text("'${material.title}' başlıklı dökümanı silmek istediğinize emin misiniz?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMaterial(material.id)
                        materialToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { materialToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

/**
 * Overview statistics card for course materials
 */
@Composable
private fun CourseMaterialsOverviewCard(
    totalCount: Int,
    notesCount: Int,
    worksheetsCount: Int,
    homeworksCount: Int,
    totalDownloads: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Dijital Ders Kütüphanesi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Föyler, ders notları ve ödev dökümanları",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "$totalCount Dosya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MaterialStatPill("📚 Notlar", "$notesCount", MaterialTheme.colorScheme.primary)
                MaterialStatPill("📄 Föyler", "$worksheetsCount", SecondaryEmerald)
                MaterialStatPill("📝 Ödevler", "$homeworksCount", AccentAmber)
                MaterialStatPill("📥 İndirme", "$totalDownloads", AccentIndigo)
            }
        }
    }
}

@Composable
private fun MaterialStatPill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Individual Card representing a Course Material or Worksheet
 */
@Composable
private fun CourseMaterialCard(
    material: CourseMaterial,
    canDelete: Boolean,
    isParent: Boolean,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onAcknowledge: () -> Unit,
    onViewAcks: () -> Unit,
    onSendReminder: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("material_card_${material.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top badges row: Subject & Type & Format
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Subject badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = material.subject_name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    // Class badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = material.class_name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // Type badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(material.typeColorHex).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(material.typeColorHex).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = material.typeDisplayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(material.typeColorHex),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                // File format extension pill
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (material.file_extension) {
                        "PDF" -> Color(0xFFDC2626).copy(alpha = 0.15f)
                        "DOCX" -> Color(0xFF2563EB).copy(alpha = 0.15f)
                        "PPTX" -> Color(0xFFD97706).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when (material.file_extension) {
                                "PDF" -> Icons.Default.PictureAsPdf
                                "DOCX" -> Icons.Default.Description
                                "PPTX" -> Icons.Default.Slideshow
                                else -> Icons.Default.AttachFile
                            },
                            contentDescription = null,
                            tint = when (material.file_extension) {
                                "PDF" -> Color(0xFFDC2626)
                                "DOCX" -> Color(0xFF2563EB)
                                "PPTX" -> Color(0xFFD97706)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = material.file_extension,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = when (material.file_extension) {
                                "PDF" -> Color(0xFFDC2626)
                                "DOCX" -> Color(0xFF2563EB)
                                "PPTX" -> Color(0xFFD97706)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            // Title & Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = material.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = material.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }

            // Topic summary box
            if (material.topic_summary.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Konu: ${material.topic_summary}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Due Date banner if Homework
            material.due_date?.let { due ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentAmberContainer.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Son Teslim Tarihi: $due",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF78350F)
                        )
                    }
                }
            }

            // Parent Acknowledgment & Teacher Tracking Section
            if (isParent) {
                if (material.is_acknowledged_by_current_parent) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SecondaryEmerald.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SecondaryEmerald.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SecondaryEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "✅ Görüldü Onayı Verildi (${material.acknowledged_at_display ?: "Bugün"})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryEmerald
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AccentAmberContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AssignmentLate,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Veli Kontrolü & Görüldü Onayı Bekleniyor",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF78350F)
                                )
                            }
                            FilledTonalButton(
                                onClick = onAcknowledge,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = SecondaryEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Okudum / Onayla", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Teacher / Admin Acknowledgment Tracker Bar
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "👨‍👩‍👧 Veli Onayı: ${material.acknowledgedCount}/${material.target_students_count}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandNavy,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = onViewAcks,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Liste", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                            TextButton(
                                onClick = onSendReminder,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Hatırlat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                        }
                    }
                }
            }

            // Metadata footer (Teacher, Size, Date, Downloads)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "👨‍🏫 ${material.teacher_name}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = material.file_size_display,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${material.download_count} indirme",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preview button
                OutlinedButton(
                    onClick = onPreview,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(40.dp).testTag("preview_btn_${material.id}")
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("İncele & Önizle", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(40.dp).testTag("delete_mat_${material.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Sil",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Download button
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(40.dp).testTag("download_btn_${material.id}")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("İndir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Interactive Document Preview Dialog
 */
@Composable
private fun DocumentPreviewDialog(
    material: CourseMaterial,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(1) }
    val totalPages = material.preview_pages_count.coerceAtLeast(4)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("document_preview_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = material.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${material.subject_name} • ${material.teacher_name}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Document Canvas Simulator
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // School Header in document
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCS360 EĞİTİM KURUMLARI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "2026-2027 EĞİTİM ÖĞRETİM YILI",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        HorizontalDivider(color = Color(0xFFCBD5E1))

                        Text(
                            text = "${material.subject_name.uppercase()} DERSİ - ${material.typeDisplayName.uppercase()}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "Konu Özeti & Anahtar Kazanımlar:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = material.topic_summary,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "Açıklama & Yönerge:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = material.description,
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )

                        // Sample problem set preview box
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "📄 Sayfa $currentPage / $totalPages - Örnek Çalışma Sorusu:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF2563EB)
                                )
                                Text(
                                    text = "Soru $currentPage: Aşağıda verilen cebirsel ifadelerin çarpanlara ayrılmış halini yazınız ve özdeşlik modellemesini tamamlayınız.\na) 4x² - 16y² = ?\nb) (3a + 5b)² = ?",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Page Navigation & Download Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Önceki")
                        }
                        Text(
                            text = "$currentPage / $totalPages",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Sonraki")
                        }
                    }

                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Dökümanı İndir (${material.file_size_display})", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Upload Course Material Dialog for Teachers
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadCourseMaterialDialog(
    subjects: List<String>,
    onDismiss: () -> Unit,
    onUpload: (title: String, desc: String, subject: String, className: String, type: String, ext: String, size: String, dueDate: String?, isRequired: Boolean, summary: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.firstOrNull() ?: "Matematik") }
    var selectedClass by remember { mutableStateOf("8-A") }
    var selectedType by remember { mutableStateOf("NOTE") }
    var selectedExt by remember { mutableStateOf("PDF") }
    var topicSummary by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var isRequired by remember { mutableStateOf(false) }

    val classes = listOf("8-A", "8-B", "7-A", "7-B", "6-A", "5-A", "Tüm Sınıflar")
    val types = listOf(
        "NOTE" to "📚 Ders Notu",
        "WORKSHEET" to "📄 Çalışma Föyü",
        "HOMEWORK" to "📝 Ödev Dökümanı",
        "EXAM_PREP" to "🎯 Sınav Hazırlık Testi",
        "PRESENTATION" to "🔬 Sunum & Slayt"
    )
    val extensions = listOf("PDF", "DOCX", "PPTX", "MP4")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .testTag("upload_course_material_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Yeni Ders Materyali Paylaş",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                HorizontalDivider()

                // Form Inputs
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Materyal Başlığı") },
                    placeholder = { Text("Örn: LGS Cebirsel İfadeler Konu Föyü") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Subject & Class Selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ders / Branş", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        var expandedSubj by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedSubj,
                            onExpandedChange = { expandedSubj = it }
                        ) {
                            OutlinedTextField(
                                value = selectedSubject,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedSubj) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedSubj,
                                onDismissRequest = { expandedSubj = false }
                            ) {
                                subjects.forEach { subj ->
                                    DropdownMenuItem(
                                        text = { Text(subj) },
                                        onClick = {
                                            selectedSubject = subj
                                            expandedSubj = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hedef Sınıf", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        var expandedClass by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expandedClass,
                            onExpandedChange = { expandedClass = it }
                        ) {
                            OutlinedTextField(
                                value = selectedClass,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedClass) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedClass,
                                onDismissRequest = { expandedClass = false }
                            ) {
                                classes.forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text(cls) },
                                        onClick = {
                                            selectedClass = cls
                                            expandedClass = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Material Type
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Materyal Türü", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(types) { (key, label) ->
                            val isSelected = selectedType == key
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedType = key },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // File Format Extension
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Dosya Formatı", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        extensions.forEach { ext ->
                            val isSelected = selectedExt == ext
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.clickable { selectedExt = ext }
                            ) {
                                Text(
                                    text = ext,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = topicSummary,
                    onValueChange = { topicSummary = it },
                    label = { Text("Kazanım & Konu Başlıkları") },
                    placeholder = { Text("Örn: Çarpanlara Ayırma, Özdeşlikler") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Açıklama & Öğrenci Yönergesi") },
                    placeholder = { Text("Öğrencilerin dikkat etmesi gereken noktalar...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )

                if (selectedType == "HOMEWORK") {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Son Teslim Tarihi") },
                        placeholder = { Text("Örn: 2026-08-30") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Action Buttons
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onUpload(
                                title,
                                description.ifBlank { "Ders materyali dökümanı." },
                                selectedSubject,
                                selectedClass,
                                selectedType,
                                selectedExt,
                                "${(1..5).random()}.${(1..9).random()} MB",
                                if (selectedType == "HOMEWORK" && dueDate.isNotBlank()) dueDate else null,
                                isRequired,
                                topicSummary.ifBlank { title }
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_upload_material_btn")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Materyali Yayınla ve Bildir", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Dialog showing detailed list of parent acknowledgments and status per student
 */
@Composable
private fun MaterialAcknowledgmentListDialog(
    material: CourseMaterial,
    allStudents: List<com.example.data.model.Student>,
    onDismiss: () -> Unit,
    onSendReminder: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACKED, PENDING

    val ackedStudentIds = remember(material) { material.parent_acknowledged_student_ids.toSet() }

    val displayedStudents = remember(allStudents, ackedStudentIds, selectedFilter) {
        when (selectedFilter) {
            "ACKED" -> allStudents.filter { ackedStudentIds.contains(it.id) }
            "PENDING" -> allStudents.filter { !ackedStudentIds.contains(it.id) }
            else -> allStudents
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 600.dp)
                .fillMaxHeight(0.80f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Veli Onay Takibi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandNavy
                        )
                        Text(
                            text = material.title,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                // Summary Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SecondaryEmerald.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${material.acknowledgedCount}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = SecondaryEmerald
                            )
                            Text(
                                text = "Onaylayan Veli",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = SecondaryEmerald
                            )
                        }
                    }

                    val pendingCount = (allStudents.size - material.acknowledgedCount).coerceAtLeast(0)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentAmberContainer.copy(alpha = 0.5f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "$pendingCount",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "Onay Bekleyen",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF78350F)
                            )
                        }
                    }
                }

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Tümü (${allStudents.size})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "ACKED",
                        onClick = { selectedFilter = "ACKED" },
                        label = { Text("Onaylayanlar (${material.acknowledgedCount})", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedFilter == "PENDING",
                        onClick = { selectedFilter = "PENDING" },
                        label = { Text("Bekleyenler (${(allStudents.size - material.acknowledgedCount).coerceAtLeast(0)})", fontSize = 11.sp) }
                    )
                }

                // Students List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedStudents, key = { it.id }) { student ->
                        val isAcked = ackedStudentIds.contains(student.id)
                        val ackRecord = material.acknowledgment_history.find { it.student_id == student.id }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isAcked) SecondaryEmerald.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = student.full_name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Veli: ${student.parent_name} (${student.parent_phone})",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isAcked) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = SecondaryEmerald.copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = SecondaryEmerald,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = ackRecord?.acknowledged_at?.takeLast(5) ?: "Onaylandı",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryEmerald
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = AccentAmberContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "Onay Bekliyor",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF78350F),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Action: Send Reminder
                Button(
                    onClick = {
                        onSendReminder()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Onay Vermeyen Velilere Push Hatırlatması Gönder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
