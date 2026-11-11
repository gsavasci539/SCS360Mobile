package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.ui.components.*
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.viewmodel.Okul360UiState
import java.time.LocalDate

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttendanceScreen(
    state: Okul360UiState,
    onClassSelected: (String) -> Unit,
    onStatusChanged: (String, String) -> Unit,
    onMarkAllPresent: () -> Unit,
    onSaveAttendance: () -> Unit,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit = {},
    onRetry: () -> Unit = {},
    onNoteChanged: (String, String?) -> Unit = { _, _ -> }
) {
    var viewMode by remember { mutableStateOf(0) }
    var dateDialog by remember { mutableStateOf(false) }
    var noteStudent by remember { mutableStateOf<AttendanceRecord?>(null) }
    var noteDraft by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val locked = state.isAttendanceSaving || state.attendanceSavePending
    val editable = state.attendanceCanWrite && state.attendanceReady && !locked
    val canSave = state.attendanceCanWrite && state.attendanceReady && !state.isAttendanceSaving && state.attendanceRecords.any { it.status != "UNRECORDED" }
    val currentClass = state.classes.find { it.id == state.attendanceClassId }
    val mode = rememberResolvedTableMode()
    val cards = isCompactCardMode(mode.value)
    val columns = remember { listOf(TableColumnSpec("Öğrenci / Not", 1.4f, minWidth = 160.dp),
        TableColumnSpec("Kayıt bilgisi", 1f), TableColumnSpec("Durum", 2.5f, minWidth = 264.dp)) }
    val actions = buildList {
        if (canSave) add(SpeedDialAction("attendance_save", if (state.attendanceSavePending) "Aynı isteği yeniden dene" else "Yoklamayı Kaydet",
            "Sınıf ve günün yoklamasını sunucuya kaydet", Icons.Default.CloudUpload, SecondaryEmerald,
            testTag = "attendance_fab_action_save", onClick = { expanded = false; onSaveAttendance() }))
        if (editable) add(SpeedDialAction("attendance_all", "Tümünü Geldi İşaretle", "Kaydetmeden önce taslağı düzenle",
            Icons.Default.DoneAll, PrimaryBlue, testTag = "attendance_fab_action_all_present",
            onClick = { expanded = false; onMarkAllPresent() }))
    }
    Scaffold(modifier = modifier.fillMaxSize(), floatingActionButton = {
        if (actions.isNotEmpty()) RoleSpeedDialFab(actions = actions, expanded = expanded,
            onExpandedChange = { expanded = it }, fabColor = SecondaryEmerald, fabLabel = "Yoklama",
            testTag = "attendance_speed_dial_fab")
    }) { padding ->
        ResponsiveTableScope {
            LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("attendance_list"),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Yoklama ve Devamsızlık", style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.widthIn(max = 440.dp))
                        TextButton(onClick = { dateDialog = true }, enabled = !locked,
                            modifier = Modifier.testTag("attendance_date_selector")) { Text(state.attendanceDate) }
                    }
                }
                item {
                    ScrollableTabRow(selectedTabIndex = viewMode, edgePadding = 0.dp) {
                        listOf("Günlük", "Takvim", "İstatistik").forEachIndexed { index, title ->
                            Tab(selected = viewMode == index, onClick = { viewMode = index }, text = { Text(title) })
                        }
                    }
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(state.classes, key = { it.id }) { cls ->
                            FilterChip(selected = cls.id == state.attendanceClassId, enabled = !locked,
                                onClick = { onClassSelected(cls.id) }, label = { Text(cls.name) })
                        }
                    }
                }
                if (state.attendanceError != null) item {
                    Text(state.attendanceError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("attendance_error"))
                    TextButton(onClick = onRetry, enabled = !state.isAttendanceSaving, modifier = Modifier.testTag("attendance_retry")) {
                        Text(if (state.attendanceSavePending) "Aynı isteği yeniden dene" else "Yeniden yükle")
                    }
                }
                if (viewMode != 0) item {
                    Text(if (viewMode == 1) "Aylık kayıtlar bu ekranda henüz sunucudan yüklenmiyor. Gün seçerek mevcut yoklamayı düzenleyin."
                        else "Yüklenen kayıtlar ders bilgisi taşımıyor. Ders bazlı istatistik henüz doğrulanmadı.")
                } else {
                    item {
                        Text(currentClass?.name ?: "Yoklama için sınıf seçin", fontWeight = FontWeight.Bold)
                        if (state.isAttendanceLoading) LinearProgressIndicator(Modifier.fillMaxWidth().testTag("attendance_loading"))
                        if (state.attendanceReady) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                AttendanceStatus.entries.forEach { status ->
                                    Text("${status.label}: ${state.attendanceRecords.count { it.status == status.name }}")
                                }
                                Text("İşaretlenmedi: ${state.attendanceRecords.count { it.status == "UNRECORDED" }}")
                            }
                            Text("İşaretlenmeyen öğrenciler için kayıt oluşturulmaz. Durumlar kaydedilene kadar taslaktır.",
                                style = MaterialTheme.typography.bodySmall)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onSaveAttendance, enabled = canSave, modifier = Modifier.testTag("attendance_save")) {
                                Text(if (state.isAttendanceSaving) "Kaydediliyor…" else if (state.attendanceSavePending) "Aynı isteği yeniden dene" else "Kaydet")
                            }
                            FilledTonalButton(onClick = onMarkAllPresent, enabled = editable && state.attendanceRecords.isNotEmpty(),
                                modifier = Modifier.testTag("attendance_mark_all")) { Text("Tümü Geldi") }
                        }
                        if (!state.isAttendanceLoading && state.attendanceRecords.isEmpty()) {
                            Text(if (state.attendanceReady) "Bu sınıfta bu gün için gösterilecek öğrenci veya kayıt yok." else "Önce sınıf ve günün kayıtlarını yükleyin.")
                            TextButton(onClick = onRetry) { Text("Yükle") }
                        }
                    }
                    if (state.attendanceRecords.isNotEmpty()) stickyHeader {
                        StickyTableHeaderRow(columns, state.attendanceRecords.size, viewModeState = mode)
                    }
                    itemsIndexed(state.attendanceRecords, key = { _, row -> row.student_id }) { index, row ->
                        AttendanceStudentRow(row, onStatusSelect = { onStatusChanged(row.student_id, it) }, index = index,
                            isCardView = cards, editable = editable,
                            onNoteSelect = { noteStudent = row; noteDraft = row.note.orEmpty() })
                    }
                }
            }
        }
    }
    if (dateDialog) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = LocalDate.parse(state.attendanceDate).toEpochDay() * 86400000L)
        DatePickerDialog(onDismissRequest = { dateDialog = false }, confirmButton = {
            TextButton(onClick = { picker.selectedDateMillis?.let { onDateSelected(LocalDate.ofEpochDay(it / 86400000L).toString()) }; dateDialog = false }) { Text("Seç") }
        }, dismissButton = { TextButton(onClick = { dateDialog = false }) { Text("Vazgeç") } }) { DatePicker(picker) }
    }
    noteStudent?.let { student ->
        AlertDialog(onDismissRequest = { noteStudent = null }, title = { Text(student.student_name) }, text = {
            OutlinedTextField(value = noteDraft, onValueChange = { if (it.length <= 5000) noteDraft = it },
                minLines = 3, maxLines = 6, label = { Text("Yoklama notu") }, modifier = Modifier.testTag("attendance_note_field"))
        }, confirmButton = { TextButton(onClick = { onNoteChanged(student.student_id, noteDraft); noteStudent = null }, enabled = editable) { Text("Taslağa ekle") } },
            dismissButton = { TextButton(onClick = { noteStudent = null }) { Text("Vazgeç") } })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceStudentRow(record: AttendanceRecord, onStatusSelect: (String) -> Unit, modifier: Modifier = Modifier,
    index: Int = 0, isCardView: Boolean = true, editable: Boolean = true, onNoteSelect: () -> Unit = {}) {
    val info = listOfNotNull(record.check_in_time?.let { "Saat: $it" }, record.temperature_celsius?.let { "$it°C" })
        .joinToString(" · ").ifBlank { "Saat bilgisi yok" }
    val details: @Composable () -> Unit = {
        Column {
            Text(record.student_name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(record.note ?: "Not yok", maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onNoteSelect, enabled = editable, modifier = Modifier.testTag("attendance_note_${record.student_id}")) { Text("Notu düzenle") }
        }
    }
    if (isCardView) CompactResponsiveCard(modifier = modifier) {
        details()
        Text(info, style = MaterialTheme.typography.bodySmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            AttendanceStatus.entries.forEach { status -> AttendanceStatusButton(record, status, editable, onStatusSelect) }
        }
    } else CompactTableRow(index = index, modifier = modifier) {
        Box(Modifier.weight(1.4f)) { details() }
        Text(info, Modifier.weight(1f), maxLines = 3, overflow = TextOverflow.Ellipsis)
        FlowRow(Modifier.weight(2.5f), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            AttendanceStatus.entries.forEach { status -> AttendanceStatusButton(record, status, editable, onStatusSelect) }
        }
    }
}

@Composable
private fun AttendanceStatusButton(row: AttendanceRecord, status: AttendanceStatus, enabled: Boolean, onSelect: (String) -> Unit) {
    FilledTonalButton(onClick = { onSelect(status.name) }, enabled = enabled, contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (row.status == status.name) Color(status.colorHex) else MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.size(48.dp).testTag("attendance_status_${row.student_id}_${status.name}")) { Text(status.shortCode, maxLines = 1) }
}
