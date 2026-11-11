package com.example.ui.screens

import com.example.ui.components.ResponsiveTableScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassGroup
import com.example.data.model.Student
import com.example.ui.components.CompactResponsiveCard
import com.example.ui.components.CompactRowActionIcon
import com.example.ui.components.CompactTableRow
import com.example.ui.components.RoleContextualAiCard
import com.example.ui.components.StickyTableHeaderRow
import com.example.ui.components.TableColumnSpec
import com.example.ui.components.isCompactCardMode
import com.example.ui.components.rememberResolvedTableMode
import com.example.ui.dialogs.*
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.viewmodel.Okul360UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ClassesScreen(
    state: Okul360UiState,
    onAddClass: (name: String, gradeLevel: String, teacherName: String, capacity: Int, roomNumber: String) -> Unit,
    onUpdateClass: (ClassGroup) -> Unit,
    onDeleteClass: (String) -> Unit,
    onAssignStudent: (studentId: String, classId: String) -> Unit,
    onRemoveStudent: (studentId: String) -> Unit,
    onSelectClassDetail: (ClassGroup?) -> Unit,
    onTransferStudent: (studentId: String, currentClassId: String, targetClassId: String) -> Unit = { _, _, _ -> }
) {
    var showAddClassDialog by remember { mutableStateOf(false) }
    var classToEdit by remember { mutableStateOf<ClassGroup?>(null) }
    var classToDelete by remember { mutableStateOf<ClassGroup?>(null) }
    var showAssignDialog by remember { mutableStateOf(false) }
    var studentToRemove by remember { mutableStateOf<Student?>(null) }
    var studentToTransfer by remember { mutableStateOf<Student?>(null) }
    var selectedViewTab by remember { mutableIntStateOf(0) } // 0: Sınıflar, 1: Doluluk & Kapasite
    val tableModeState = rememberResolvedTableMode()
    val isCardMode = isCompactCardMode(tableModeState.value)

    val selectedClass = state.selectedClassDetail

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (selectedClass != null) {
            // DETAIL VIEW FOR SELECTED CLASS
            val enrolledStudents = state.students.filter { it.class_id == selectedClass.id }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onSelectClassDetail(null) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Geri Dön")
                        }
                        Text(
                            text = selectedClass.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = BrandNavy,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        Row {
                            IconButton(onClick = { classToEdit = selectedClass }) {
                                Icon(Icons.Default.Edit, contentDescription = "Düzenle", tint = PrimaryBlue)
                            }
                            IconButton(onClick = { classToDelete = selectedClass }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Öğretmen: ${selectedClass.teacher_name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = "Derslik: ${selectedClass.room_number} • Kademe: ${selectedClass.grade_level}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${enrolledStudents.size} / ${selectedClass.capacity} Öğrenci",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Assign Student
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sınıftaki Öğrenciler (${enrolledStudents.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BrandNavy
                )
                Button(
                    onClick = { showAssignDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Öğrenci Ata", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (enrolledStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Bu sınıfa henüz atanmış öğrenci bulunmuyor.\n'Öğrenci Ata' butonuyla yeni öğrenci ekleyebilirsiniz.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                ResponsiveTableScope() {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stickyHeader {
                            StickyTableHeaderRow(
                                columns = listOf(
                                    TableColumnSpec("Öğrenci / No", 1.5f),
                                    TableColumnSpec("Veli Bilgisi", 1.2f),
                                    TableColumnSpec("İşlem", 0.7f, TextAlign.End)
                                ),
                                totalCount = enrolledStudents.size,
                                viewModeState = tableModeState
                            )
                        }
    
                        itemsIndexed(enrolledStudents, key = { _, it -> it.id }) { index, std ->
                            if (isCardMode) {
                                CompactResponsiveCard(
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(std.avatar_color_hex)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = std.first_name.take(1) + std.last_name.take(1),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = std.full_name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(text = "No: ${std.student_number} • Veli: ${std.parent_name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            CompactRowActionIcon(
                                                icon = Icons.Default.SwapHoriz,
                                                contentDescription = "Sınıf Değiştir / Transfer",
                                                tint = PrimaryBlue,
                                                containerColor = PrimaryBlue.copy(alpha = 0.1f),
                                                onClick = { studentToTransfer = std }
                                            )
                                            CompactRowActionIcon(
                                                icon = Icons.Default.PersonRemove,
                                                contentDescription = "Sınıftan Çıkar",
                                                tint = MaterialTheme.colorScheme.error,
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                                onClick = { studentToRemove = std }
                                            )
                                        }
                                    }
                                }
                            } else {
                                CompactTableRow(
                                    index = index,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1.5f).padding(end = 6.dp)) {
                                        Text(std.full_name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("No: ${std.student_number}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        text = std.parent_name,
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.2f).padding(end = 6.dp)
                                    )
                                    Row(
                                        modifier = Modifier.weight(0.7f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CompactRowActionIcon(
                                            icon = Icons.Default.SwapHoriz,
                                            contentDescription = "Sınıf Değiştir / Transfer",
                                            tint = PrimaryBlue,
                                            containerColor = PrimaryBlue.copy(alpha = 0.1f),
                                            onClick = { studentToTransfer = std }
                                        )
                                        CompactRowActionIcon(
                                            icon = Icons.Default.PersonRemove,
                                            contentDescription = "Sınıftan Çıkar",
                                            tint = MaterialTheme.colorScheme.error,
                                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                            onClick = { studentToRemove = std }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // LIST VIEW OF ALL CLASSES
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "🏫 Sınıf Yönetimi",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = BrandNavy,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Toplam ${state.classes.size} Aktif Derslik / Şube",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showAddClassDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Yeni Sınıf", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, softWrap = false)
                }
            }

            TabRow(
                selectedTabIndex = selectedViewTab,
                containerColor = Color(0xFFF1F5F9),
                contentColor = PrimaryBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedViewTab == 0,
                    onClick = { selectedViewTab = 0 },
                    text = { Text("Sınıf Listesi (${state.classes.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedViewTab == 1,
                    onClick = { selectedViewTab = 1 },
                    text = { Text("Kapasite & Doluluk", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            if (selectedViewTab == 1) {
                val totalCap = remember(state.classes) { state.classes.sumOf { it.capacity } }
                val totalEnr = remember(state.students) { state.students.size }
                val overallRate = if (totalCap > 0) ((totalEnr.toDouble() / totalCap) * 100).toInt() else 0

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Genel Okul Kontenjan ve Doluluk Analizi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Toplam Kapasite", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$totalCap Öğrenci", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("Kayıtlı Öğrenci", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("$totalEnr Öğrenci", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                    Column {
                                        Text("Ortalama Doluluk", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("%$overallRate", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (overallRate > 90) Color(0xFFDC2626) else Color(0xFF059669))
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { (overallRate / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                    color = if (overallRate > 90) Color(0xFFDC2626) else Color(0xFF059669),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }

                    items(state.classes) { cls ->
                        val enrolled = state.students.count { it.class_id == cls.id }
                        val pct = if (cls.capacity > 0) ((enrolled.toDouble() / cls.capacity) * 100).toInt() else 0
                        val barColor = when {
                            pct >= 100 -> Color(0xFFDC2626)
                            pct >= 85 -> Color(0xFFD97706)
                            else -> Color(0xFF059669)
                        }

                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cls.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandNavy)
                                        Text("Derslik: ${cls.room_number} • Rehber: ${cls.teacher_name}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = barColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (pct >= 100) "DOLU (%100)" else "%$pct DOLU",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = barColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("$enrolled / ${cls.capacity} Öğrenci", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Boş Kontenjan: ${maxOf(0, cls.capacity - enrolled)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { (pct / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                ResponsiveTableScope() {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            RoleContextualAiCard(
                                screenName = "CLASSES",
                                state = state,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
    
                        stickyHeader {
                            StickyTableHeaderRow(
                                columns = listOf(
                                    TableColumnSpec("Sınıf / Derslik", 1.3f),
                                    TableColumnSpec("Rehber Öğretmen", 1.25f),
                                    TableColumnSpec("Kontenjan", 0.95f, TextAlign.End)
                                ),
                                totalCount = state.classes.size,
                                viewModeState = tableModeState
                            )
                        }
    
                        itemsIndexed(state.classes, key = { _, it -> it.id }) { index, cls ->
                            val enrolledCount = state.students.count { it.class_id == cls.id }
                            val progress = if (cls.capacity > 0) (enrolledCount.toFloat() / cls.capacity.toFloat()).coerceIn(0f, 1f) else 0f
    
                            if (isCardMode) {
                                CompactResponsiveCard(
                                    onClick = { onSelectClassDetail(cls) },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(PrimaryBlue.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.School, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = cls.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(text = "${cls.grade_level} • ${cls.room_number} • ${cls.teacher_name}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
    
                                        Surface(
                                            color = if (enrolledCount >= cls.capacity) Color(0xFFFEE2E2) else Color(0xFFECFDF5),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$enrolledCount / ${cls.capacity} Öğrenci",
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (enrolledCount >= cls.capacity) Color(0xFFDC2626) else Color(0xFF059669)
                                            )
                                        }
                                    }
    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = if (enrolledCount >= cls.capacity) Color(0xFFDC2626) else PrimaryBlue,
                                            trackColor = Color(0xFFE2E8F0)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Detay",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = PrimaryBlue
                                            )
                                            Icon(
                                                Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                CompactTableRow(
                                    index = index,
                                    onClick = { onSelectClassDetail(cls) },
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1.3f).padding(end = 6.dp)) {
                                        Text(cls.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BrandNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${cls.grade_level} • ${cls.room_number}", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                    Text(
                                        text = cls.teacher_name,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1.25f).padding(end = 6.dp)
                                    )
                                    Row(
                                        modifier = Modifier.weight(0.95f),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = if (enrolledCount >= cls.capacity) Color(0xFFFEE2E2) else Color(0xFFECFDF5),
                                            shape = RoundedCornerShape(5.dp)
                                        ) {
                                            Text(
                                                text = "$enrolledCount/${cls.capacity}",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (enrolledCount >= cls.capacity) Color(0xFFDC2626) else Color(0xFF059669)
                                            )
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddClassDialog) {
        AddClassDialog(
            onDismiss = { showAddClassDialog = false },
            onConfirm = { name, grade, teacher, cap, room ->
                onAddClass(name, grade, teacher, cap, room)
                showAddClassDialog = false
            }
        )
    }

    classToEdit?.let { cls ->
        EditClassDialog(
            classGroup = cls,
            onDismiss = { classToEdit = null },
            onConfirm = { updated ->
                onUpdateClass(updated)
                classToEdit = null
            }
        )
    }

    classToDelete?.let { cls ->
        ConfirmDeleteDialog(
            title = "Sınıfı Sil",
            message = "${cls.name} sınıfını silmek istediğinize emin misiniz?",
            onDismiss = { classToDelete = null },
            onConfirm = {
                onDeleteClass(cls.id)
                classToDelete = null
                onSelectClassDetail(null)
            }
        )
    }

    if (showAssignDialog && selectedClass != null) {
        AssignStudentDialog(
            students = state.students,
            currentClassId = selectedClass.id,
            onDismiss = { showAssignDialog = false },
            onConfirm = { stdId ->
                onAssignStudent(stdId, selectedClass.id)
                showAssignDialog = false
            }
        )
    }

    studentToRemove?.let { std ->
        ConfirmDeleteDialog(
            title = "Öğrenciyi Sınıftan Çıkar",
            message = "${std.full_name} isimli öğrenciyi bu sınıftan çıkarmak istiyor musunuz?",
            onDismiss = { studentToRemove = null },
            onConfirm = {
                onRemoveStudent(std.id)
                studentToRemove = null
            }
        )
    }

    studentToTransfer?.let { std ->
        val otherClasses = state.classes.filter { it.id != selectedClass?.id }
        var targetClassId by remember { mutableStateOf(otherClasses.firstOrNull()?.id ?: "") }
        var classMenuExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { studentToTransfer = null },
            title = { Text("Öğrenci Sınıf Transferi", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Öğrenci: ${std.full_name} (#${std.student_number})", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Mevcut Sınıf: ${selectedClass?.name ?: std.class_name}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Hedef Sınıf Seçin:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { classMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedName = otherClasses.find { it.id == targetClassId }?.name ?: "Hedef Sınıf Seçin"
                            Text(selectedName)
                        }
                        DropdownMenu(
                            expanded = classMenuExpanded,
                            onDismissRequest = { classMenuExpanded = false }
                        ) {
                            otherClasses.forEach { cls ->
                                DropdownMenuItem(
                                    text = { Text("${cls.name} (${cls.room_number})") },
                                    onClick = {
                                        targetClassId = cls.id
                                        classMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (targetClassId.isNotBlank() && selectedClass != null) {
                            onTransferStudent(std.id, selectedClass.id, targetClassId)
                        }
                        studentToTransfer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Transferi Tamamla")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToTransfer = null }) {
                    Text("İptal")
                }
            }
        )
    }
}
