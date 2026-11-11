package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.Student
import com.example.ui.components.*
import com.example.ui.dialogs.ConfirmDeleteDialog
import com.example.ui.dialogs.EditStudentDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StudentsScreen(
    state: Okul360UiState,
    onSearchChange: (String) -> Unit,
    onFilterClassChange: (String) -> Unit,
    onFilterAcademicYearChange: (String) -> Unit = {},
    onSelectStudent: (Student) -> Unit,
    onOpenAddStudent: () -> Unit,
    onUpdateStudent: (Student) -> Unit,
    onDeleteStudent: (String) -> Unit,
    onPrintReportCard: ((Student) -> Unit)? = null,
    onPrintClassReportCards: ((String) -> Unit)? = null,
    onExportCsv: (() -> Unit)? = null,
    onImportExcel: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    onViewReportCard: ((Student) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var isFabExpanded by remember { mutableStateOf(false) }
    val tableViewMode = rememberResolvedTableMode()
    val isCardView = isCompactCardMode(tableViewMode.value)

    val studentColumns = remember {
        listOf(
            TableColumnSpec("Öğrenci / No", weight = 1.4f),
            TableColumnSpec("Sınıf / Veli", weight = 1.2f),
            TableColumnSpec("Durum", weight = 0.75f, alignment = TextAlign.Center),
            TableColumnSpec("Hızlı İşlem", weight = 1.15f, alignment = TextAlign.End, minWidth = 264.dp)
        )
    }

    val studentActions = remember(onOpenAddStudent, onExportCsv, state.filterClassId) {
        listOf(
            SpeedDialAction(
                id = "student_add_new",
                title = "Yeni Öğrenci Ekle",
                description = "Profil, veli ve sınıf bilgisi kaydet",
                icon = Icons.Default.PersonAdd,
                containerColor = PrimaryBlue,
                testTag = "student_fab_action_add",
                onClick = {
                    isFabExpanded = false
                    onOpenAddStudent()
                }
            ),
            SpeedDialAction(
                id = "student_print_cards",
                title = "Toplu Karne / Rapor",
                description = "Sınıf karne ve gelişim raporu yazdır",
                icon = Icons.Default.Print,
                containerColor = SecondaryEmerald,
                testTag = "student_fab_action_print",
                onClick = {
                    isFabExpanded = false
                    if (state.filterClassId.isNotBlank() && state.filterClassId != "all" && onPrintClassReportCards != null) {
                        onPrintClassReportCards(state.filterClassId)
                    } else if (onPrintReportCard != null) {
                        state.students.firstOrNull()?.let { onPrintReportCard(it) }
                    }
                }
            ),
            SpeedDialAction(
                id = "student_export_csv",
                title = "Excel / CSV Dışa Aktar",
                description = "Öğrenci listesini dışa aktar",
                icon = Icons.Default.FileDownload,
                containerColor = Color(0xFFD97706), // Amber
                testTag = "student_fab_action_export",
                onClick = {
                    isFabExpanded = false
                    onExportCsv?.invoke()
                }
            ),
            SpeedDialAction(
                id = "student_import_excel",
                title = "Excel'den Öğrenci İçe Aktar",
                description = "Excel dosyasından toplu öğrenci ekle",
                icon = Icons.Default.FileUpload,
                containerColor = Color(0xFF16A34A),
                testTag = "student_fab_action_import",
                onClick = {
                    isFabExpanded = false
                    onImportExcel?.invoke()
                }
            )
        )
    }

    Scaffold(
        floatingActionButton = {
            RoleSpeedDialFab(
                actions = studentActions,
                expanded = isFabExpanded,
                onExpandedChange = { isFabExpanded = it },
                fabColor = PrimaryBlue,
                fabLabel = "Öğrenci Hızlı Aksiyon",
                testTag = "students_speed_dial_fab",
                modifier = Modifier.padding(16.dp)
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Compact Search Bar at Top
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Öğrenci adı, no veya veli ara...", fontSize = 12.5.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Ara",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(17.dp)
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(17.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            // Loading & Empty State Handling
            if (state.isDataLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Okul360CircularLoadingIndicator(
                        size = 52.dp,
                        label = "Öğrenci kayıtları yükleniyor..."
                    )
                }
            } else if (state.students.isEmpty()) {
                val isFiltering = state.searchQuery.isNotBlank() || state.filterClassId != "all" || state.filterAcademicYear != "all"
                StudentEmptyState(
                    isSearchingOrFiltering = isFiltering,
                    onResetFilters = {
                        onSearchChange("")
                        onFilterClassChange("all")
                        onFilterAcademicYearChange("all")
                    },
                    onAddStudent = onOpenAddStudent
                )
            } else {
                PullRefreshWrapper(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onRefresh?.invoke() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    ResponsiveTableScope() {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 110.dp, top = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize().testTag("students_list")
                        ) {
                            // Compact Filter Strip & Batch Actions inside scrollable feed
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Class Filter Chips (Sınıf Seçimi)
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = state.filterClassId == "all",
                                                onClick = { onFilterClassChange("all") },
                                                label = { Text("Tüm Sınıflar (${state.students.size})", fontSize = 11.sp) },
                                                leadingIcon = if (state.filterClassId == "all") {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                                } else null
                                            )
                                        }
    
                                        items(state.classes) { cls ->
                                            FilterChip(
                                                selected = state.filterClassId == cls.id,
                                                onClick = { onFilterClassChange(cls.id) },
                                                label = { Text(cls.name, fontSize = 11.sp) },
                                                leadingIcon = if (state.filterClassId == cls.id) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp)) }
                                                } else null
                                            )
                                        }
                                    }
    
                                    // Academic Year Filter Chips + Export Actions on one compact row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            item {
                                                FilterChip(
                                                    selected = state.filterAcademicYear == "all",
                                                    onClick = { onFilterAcademicYearChange("all") },
                                                    label = { Text("Tüm Yıllar", fontSize = 10.5.sp) },
                                                    leadingIcon = if (state.filterAcademicYear == "all") {
                                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                                    } else null
                                                )
                                            }
    
                                            items(state.academicYears) { year ->
                                                FilterChip(
                                                    selected = state.filterAcademicYear == year,
                                                    onClick = { onFilterAcademicYearChange(year) },
                                                    label = { Text(year, fontSize = 10.5.sp) },
                                                    leadingIcon = if (state.filterAcademicYear == year) {
                                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                                    } else null
                                                )
                                            }
                                        }
    
                                        Spacer(modifier = Modifier.width(6.dp))
    
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onExportCsv?.invoke()
                                                },
                                                modifier = Modifier.height(30.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(7.dp)
                                            ) {
                                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(13.dp), tint = SecondaryEmerald)
                                                Spacer(Modifier.width(3.dp))
                                                Text("CSV", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            }
    
                                            FilledTonalButton(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    val selectedClassName = if (state.filterClassId == "all") "Tüm Sınıflar" else {
                                                        state.classes.find { it.id == state.filterClassId }?.name ?: "Sınıf"
                                                    }
                                                    onPrintClassReportCards?.invoke(selectedClassName)
                                                },
                                                modifier = Modifier.height(30.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                shape = RoundedCornerShape(7.dp)
                                            ) {
                                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(13.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text("Karne PDF", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
    
                            item {
                                RoleContextualAiCard(
                                    screenName = "STUDENTS",
                                    state = state
                                )
                            }
    
                            // Pinned Sticky Column Header
                            stickyHeader {
                                StickyTableHeaderRow(
                                    columns = studentColumns,
                                    totalCount = state.students.size,
                                    viewModeState = tableViewMode
                                )
                            }
    
                            itemsIndexed(state.students, key = { _, s -> s.id }) { index, student ->
                                val callParentAction = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${student.parent_phone}")
                                    }
                                    context.startActivity(intent)
                                }
                                val whatsAppParentAction = {
                                    val cleanPhone = student.parent_phone.replace("[^0-9]".toRegex(), "")
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://wa.me/$cleanPhone")
                                    }
                                    context.startActivity(intent)
                                }
    
                                if (isCardView) {
                                    StudentItemCard(
                                        student = student,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectStudent(student)
                                        },
                                        onEditClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            studentToEdit = student
                                        },
                                        onDeleteClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            studentToDelete = student
                                        },
                                        onPrintReportCard = { onPrintReportCard?.invoke(student) },
                                        onViewReportCard = { onViewReportCard?.invoke(student) },
                                        onCallParent = callParentAction,
                                        onWhatsAppParent = whatsAppParentAction
                                    )
                                } else {
                                    StudentCompactTableRow(
                                        index = index,
                                        student = student,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectStudent(student)
                                        },
                                        onEditClick = { studentToEdit = student },
                                        onDeleteClick = { studentToDelete = student },
                                        onViewReportCard = { onViewReportCard?.invoke(student) },
                                        onCallParent = callParentAction,
                                        onWhatsAppParent = whatsAppParentAction
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    studentToEdit?.let { std ->
        EditStudentDialog(
            student = std,
            classes = state.classes,
            onDismiss = { studentToEdit = null },
            onConfirm = { updated ->
                onUpdateStudent(updated)
                studentToEdit = null
            }
        )
    }

    studentToDelete?.let { std ->
        ConfirmDeleteDialog(
            title = "Öğrenciyi Sil",
            message = "${std.full_name} isimli öğrenciyi ve tüm kayıtlarını silmek istiyor musunuz?",
            onDismiss = { studentToDelete = null },
            onConfirm = {
                onDeleteStudent(std.id)
                studentToDelete = null
            }
        )
    }
}

@Composable
private fun StudentCompactTableRow(
    index: Int,
    student: Student,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onViewReportCard: () -> Unit,
    onCallParent: () -> Unit,
    onWhatsAppParent: () -> Unit
) {
    val attendanceStatusEnum = when (student.today_attendance_status) {
        "PRESENT" -> AttendanceStatus.PRESENT
        "ABSENT" -> AttendanceStatus.ABSENT
        "LATE" -> AttendanceStatus.LATE
        "EXCUSED" -> AttendanceStatus.EXCUSED
        else -> null
    }

    CompactTableRow(index = index, onClick = onClick) {
        // Col 1: Öğrenci / No (weight = 1.4f)
        Row(
            modifier = Modifier.weight(1.4f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(student.avatar_color_hex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${student.first_name.take(1)}${student.last_name.take(1)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.full_name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BrandNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${student.student_number}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Col 2: Sınıf / Veli (weight = 1.2f)
        Column(modifier = Modifier.weight(1.2f)) {
            Text(
                text = "${student.class_name} (${student.academic_year})",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = PrimaryBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = student.parent_name,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Col 3: Durum (weight = 0.75f)
        Box(
            modifier = Modifier.weight(0.75f),
            contentAlignment = Alignment.Center
        ) {
            if (attendanceStatusEnum != null) {
                StatusBadge(
                    label = attendanceStatusEnum.label,
                    color = Color(attendanceStatusEnum.colorHex)
                )
            } else {
                Text("-", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Col 4: Hızlı İşlem (weight = 1.15f)
        Row(
            modifier = Modifier.weight(1.15f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactRowActionIcon(
                icon = Icons.Default.AutoStories,
                contentDescription = "Karne",
                tint = Color(0xFF7C3AED),
                onClick = onViewReportCard
            )
            Spacer(modifier = Modifier.width(3.dp))
            CompactRowActionIcon(
                icon = Icons.Default.Phone,
                contentDescription = "Ara",
                tint = PrimaryBlue,
                onClick = onCallParent
            )
            Spacer(modifier = Modifier.width(3.dp))
            CompactRowActionIcon(
                icon = Icons.AutoMirrored.Filled.Chat,
                contentDescription = "WhatsApp",
                tint = SecondaryEmerald,
                onClick = onWhatsAppParent
            )
            Spacer(modifier = Modifier.width(3.dp))
            CompactRowActionIcon(
                icon = Icons.Default.Edit,
                contentDescription = "Düzenle",
                tint = BrandNavy,
                onClick = onEditClick
            )
            Spacer(modifier = Modifier.width(3.dp))
            CompactRowActionIcon(
                icon = Icons.Default.Delete,
                contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.error,
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
fun StudentItemCard(
    student: Student,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPrintReportCard: () -> Unit,
    onCallParent: () -> Unit,
    onWhatsAppParent: () -> Unit,
    onViewReportCard: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val attendanceStatusEnum = when (student.today_attendance_status) {
        "PRESENT" -> AttendanceStatus.PRESENT
        "ABSENT" -> AttendanceStatus.ABSENT
        "LATE" -> AttendanceStatus.LATE
        "EXCUSED" -> AttendanceStatus.EXCUSED
        else -> null
    }

    CompactResponsiveCard(
        onClick = onClick,
        modifier = modifier
    ) {
        // Top Row: Avatar + Name & Student No/Class + Status Badge + Edit/Delete
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(student.avatar_color_hex)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${student.first_name.take(1)}${student.last_name.take(1)}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = student.full_name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = BrandNavy,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (attendanceStatusEnum != null) {
                        StatusBadge(
                            label = attendanceStatusEnum.label,
                            color = Color(attendanceStatusEnum.colorHex)
                        )
                    }
                }
                Text(
                    text = "#${student.student_number} • ${student.class_name} • ${student.academic_year}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                CompactRowActionIcon(
                    icon = Icons.Default.Edit,
                    contentDescription = "Düzenle",
                    tint = PrimaryBlue,
                    onClick = onEditClick
                )
                CompactRowActionIcon(
                    icon = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = onDeleteClick
                )
            }
        }

        // Bottom Compact Row: Parent Info Pill + Fast Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SupervisorAccount,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Veli: ${student.parent_name}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onViewReportCard != null) {
                    CompactRowActionIcon(
                        icon = Icons.Default.AutoStories,
                        contentDescription = "Karne & Notlar",
                        tint = Color(0xFF7C3AED),
                        containerColor = Color(0xFFEDE9FE),
                        onClick = onViewReportCard
                    )
                }
                CompactRowActionIcon(
                    icon = Icons.Default.Print,
                    contentDescription = "Karne / PDF Yazdır",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = onPrintReportCard
                )
                CompactRowActionIcon(
                    icon = Icons.Default.Phone,
                    contentDescription = "Ara",
                    tint = PrimaryBlue,
                    containerColor = PrimaryBlueContainer,
                    onClick = onCallParent
                )
                CompactRowActionIcon(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "WhatsApp",
                    tint = SecondaryEmerald,
                    containerColor = SecondaryEmeraldContainer,
                    onClick = onWhatsAppParent
                )
            }
        }
    }
}
