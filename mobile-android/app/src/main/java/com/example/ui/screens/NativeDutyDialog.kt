package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DutyLocation
import com.example.data.model.TeacherDutyRoster
import com.example.data.remote.DutyDraft
import com.example.data.remote.DutyOption
import com.example.data.remote.NativeStaffClient
import com.example.data.remote.RegistryHttpException
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal val dutyDays = DayOfWeek.entries.zip(
    listOf("Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"),
).toMap()

@Composable
internal fun NativeDutyDialog(
    api: NativeStaffClient,
    selectedBranchId: String?,
    onDismiss: () -> Unit,
    onSaved: (List<TeacherDutyRoster>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var branches by remember { mutableStateOf<List<DutyOption>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<DutyOption>>(emptyList()) }
    var branch by remember { mutableStateOf<DutyOption?>(null) }
    var teacher by remember { mutableStateOf<DutyOption?>(null) }
    var day by remember { mutableStateOf<DayOfWeek?>(null) }
    var location by remember { mutableStateOf<DutyLocation?>(null) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var loadingBranches by remember { mutableStateOf(true) }
    var loadingTeachers by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var uncertain by remember { mutableStateOf<DutyDraft?>(null) }
    var optionsError by remember { mutableStateOf("") }
    var saveError by remember { mutableStateOf("") }
    var reloadOptions by remember { mutableIntStateOf(0) }
    LaunchedEffect(reloadOptions) {
        loadingBranches = true
        optionsError = ""
        try {
            branches = api.dutyOptions().filter { selectedBranchId == null || it.id == selectedBranchId }
            if (branch !in branches) branch = branches.singleOrNull()
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            optionsError = "Şube seçenekleri yüklenemedi."
        } finally {
            loadingBranches = false
        }
    }
    LaunchedEffect(branch?.id, reloadOptions) {
        teacher = null
        teachers = emptyList()
        val currentBranch = branch ?: return@LaunchedEffect
        loadingTeachers = true
        try {
            teachers = api.dutyOptions(currentBranch.id)
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            optionsError = "Öğretmen seçenekleri yüklenemedi."
        } finally {
            loadingTeachers = false
        }
    }
    val validTimes = remember(start, end) {
        val pattern = Regex("[0-9]{2}:[0-9]{2}")
        pattern.matches(start) && pattern.matches(end) &&
            runCatching { LocalTime.parse(start) < LocalTime.parse(end) }.getOrDefault(false)
    }
    val editable = !busy && uncertain == null
    val canSave = editable && !loadingBranches && !loadingTeachers && optionsError.isEmpty() &&
        branch != null && teacher != null && day != null && location != null && validTimes

    fun submit(draft: DutyDraft, checkOnly: Boolean) {
        if (busy) return
        busy = true
        saveError = ""
        scope.launch {
            try {
                val rows = if (checkOnly) api.confirmDuty(draft, selectedBranchId)
                else api.createDuty(draft, selectedBranchId)
                if (api.current()) {
                    if (rows != null) onSaved(rows)
                    else saveError = "Kayıt henüz listede görünmüyor. Sonucu yeniden kontrol edebilirsiniz."
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                if (api.current()) {
                    if (!checkOnly && failure is RegistryHttpException && failure.code in setOf(401, 403, 404, 422)) {
                        saveError = "Nöbet kaydedilemedi. Şube, öğretmen ve saatleri kontrol edin."
                    } else {
                        uncertain = draft
                        saveError = "Nöbetin kaydedilip kaydedilmediği doğrulanamadı. Sonucu kontrol edin."
                    }
                }
            } finally {
                busy = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.fillMaxWidth().padding(12.dp)
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(Modifier.imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nöbet oluştur", style = MaterialTheme.typography.titleMedium)
                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f, fill = false).testTag("staff_duty_fields"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        DutyPicker("Şube", branches, branch, editable && !loadingBranches, { it.name }) {
                            if (branch != it) {
                                teacher = null
                                teachers = emptyList()
                                branch = it
                            }
                        }
                    }
                    item { DutyPicker("Öğretmen", teachers, teacher, editable && !loadingTeachers, { it.name }) { teacher = it } }
                    item { DutyPicker("Gün", dutyDays.keys.toList(), day, editable, { dutyDays.getValue(it) }) { day = it } }
                    item { DutyPicker("Nöbet yeri", DutyLocation.entries, location, editable, { it.label }) { location = it } }
                    item {
                        OutlinedTextField(start, { start = it }, Modifier.fillMaxWidth(), enabled = editable,
                            label = { Text("Başlangıç (SS:DD)") }, singleLine = true)
                    }
                    item {
                        OutlinedTextField(end, { end = it }, Modifier.fillMaxWidth(), enabled = editable,
                            label = { Text("Bitiş (SS:DD)") }, singleLine = true)
                    }
                    if (loadingBranches || loadingTeachers || busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (optionsError.isNotEmpty()) item {
                        Text(optionsError, color = MaterialTheme.colorScheme.error)
                        TextButton(enabled = editable, onClick = { reloadOptions++ }) { Text("Seçenekleri yenile") }
                    }
                    if (!loadingBranches && branches.isEmpty() && optionsError.isEmpty()) item { Text("Yetkili şube bulunamadı.") }
                    if (branch != null && !loadingTeachers && teachers.isEmpty() && optionsError.isEmpty()) item {
                        Text("Bu şubede nöbet için uygun öğretmen bulunamadı.")
                    }
                    if (saveError.isNotEmpty()) item { Text(saveError, color = MaterialTheme.colorScheme.error) }
                }
                val pending = uncertain
                Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Button(
                        enabled = if (pending == null) canSave else !busy,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("staff_duty_save"),
                        onClick = {
                            if (pending != null) submit(pending, checkOnly = true)
                            else submit(DutyDraft(requireNotNull(branch), requireNotNull(teacher), requireNotNull(day),
                                requireNotNull(location), start, end), checkOnly = false)
                        },
                    ) { Text(if (pending == null) "Kaydet" else "Sonucu kontrol et") }
                    TextButton(enabled = !busy, onClick = onDismiss) { Text("Kapat") }
                }
            }
        }
    }
}

@Composable
private fun <T> DutyPicker(
    label: String,
    options: List<T>,
    selected: T?,
    enabled: Boolean,
    name: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) { Text(if (selected == null) "$label seçin" else "$label: ${name(selected)}") }
        DropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 240.dp)) {
            for (option in options) {
                DropdownMenuItem(text = { Text(name(option)) }, onClick = { expanded = false; onSelect(option) })
            }
        }
    }
}
