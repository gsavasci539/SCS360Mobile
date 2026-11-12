package com.example.ui.dialogs

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
import com.example.data.model.*
import com.example.data.remote.NativeRegistryClient
import com.example.data.remote.RegistryChoice
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException

fun registryCanWrite(state: Okul360UiState, kind: String): Boolean {
    val user = state.currentUser
    if (user.is_super_admin == true) return !state.workspaceOrganizationId.isNullOrBlank()
    val keys = when (kind) { "student" -> setOf("student.write", "students.write"); "class" -> setOf("class.write", "classes.write"); else -> setOf("health.write") }
    return user.permissions.any { it == "*" || it in keys }
}
private fun organization(state: Okul360UiState) = if (state.currentUser.is_super_admin == true) state.workspaceOrganizationId.orEmpty()
    else state.currentUser.organization_id ?: state.currentUser.tenant_id.orEmpty()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrySelect(label: String, selected: String, choices: List<RegistryChoice>, enabled: Boolean, tag: String, choose: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open && enabled, onExpandedChange = { if (enabled) open = it }) {
        OutlinedTextField(value = choices.find { it.id == selected }?.name.orEmpty(), onValueChange = {}, readOnly = true,
            enabled = enabled, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            modifier = Modifier.fillMaxWidth().menuAnchor().testTag(tag))
        ExposedDropdownMenu(expanded = open && enabled, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 300.dp)) {
            choices.forEach { option -> DropdownMenuItem(text = { Text(option.name) }, onClick = { choose(option.id); open = false },
                modifier = Modifier.testTag("${tag}_${option.id.ifBlank { "none" }}")) }
        }
    }
}

@Composable
private fun RegistryShell(saving: Boolean, dismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = { if (!saving) dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(12.dp).imePadding()
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp), shape = MaterialTheme.shapes.large) { content() }
    }
}

@Composable
fun RegistryStudentDialog(initial: Student?, state: Okul360UiState, onDismiss: () -> Unit, onConfirm: (Student) -> Unit) {
    val org = organization(state)
    var branchChoices by remember { mutableStateOf<List<RegistryChoice>>(emptyList()) }
    var branchId by remember { mutableStateOf(initial?.branch_id ?: state.selectedBranchId.takeIf { it != "all" }.orEmpty()) }
    var classId by remember { mutableStateOf(initial?.class_id.orEmpty()) }
    var first by remember { mutableStateOf(initial?.first_name.orEmpty()) }
    var last by remember { mutableStateOf(initial?.last_name.orEmpty()) }
    var number by remember { mutableStateOf(initial?.student_number.orEmpty()) }
    var birth by remember { mutableStateOf(initial?.birth_date.orEmpty()) }
    var enrolled by remember { mutableStateOf(initial?.enrollment_date.orEmpty()) }
    var gender by remember { mutableStateOf(initial?.gender.orEmpty()) }
    var status by remember { mutableStateOf(initial?.status ?: "ACTIVE") }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }
    var health by remember { mutableStateOf(initial?.health_notes.orEmpty()) }
    var submitted by remember { mutableStateOf(false) }
    val revision = remember { state.registrySavedRevision }
    LaunchedEffect(state.registrySavedRevision) { if (state.registrySavedRevision > revision) onDismiss() }
    var classChoices by remember { mutableStateOf<List<RegistryChoice>>(emptyList()) }
    var branchesReady by remember { mutableStateOf(false) }
    var classesReady by remember { mutableStateOf(false) }
    var branchError by remember { mutableStateOf<String?>(null) }
    var classError by remember { mutableStateOf<String?>(null) }
    val choicesError = branchError ?: classError
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(org, initial?.id, retry) {
        branchesReady = false; branchChoices = emptyList(); branchError = null
        if (org.isNotBlank()) try {
            branchChoices = NativeRegistryClient().studentBranches(org, initial?.id, initial?.branch_id)
            branchesReady = true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { branchError = "Şube seçenekleri yüklenemedi" }
    }
    LaunchedEffect(org, branchId, initial?.id, retry) {
        classesReady = false; classChoices = emptyList(); classError = null
        if (org.isNotBlank() && branchId.isNotBlank()) try {
            val choices = NativeRegistryClient().studentClasses(org, branchId, initial?.id,
                initial?.class_id?.takeIf { initial.branch_id == branchId && it.isNotBlank() })
            classChoices = listOf(RegistryChoice("", "Sınıf ataması yok")) + choices
            classesReady = true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { classError = "Sınıf seçenekleri yüklenemedi" }
    }
    val edit = registryCanWrite(state, "student") && !state.registrySaving
    RegistryShell(state.registrySaving, onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().testTag("registry_student_form"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(if (initial == null) "Yeni Öğrenci" else "Öğrenciyi Düzenle", style = MaterialTheme.typography.titleLarge) }
            item { RegistrySelect("Şube", branchId, branchChoices, edit && branchesReady, "student_branch") { branchId = it; classId = "" } }
            item { OutlinedTextField(first, { first = it }, enabled = edit, label = { Text("Ad") }, modifier = Modifier.fillMaxWidth().testTag("student_first")) }
            item { OutlinedTextField(last, { last = it }, enabled = edit, label = { Text("Soyad") }, modifier = Modifier.fillMaxWidth().testTag("student_last")) }
            item { RegistrySelect("Sınıf", classId, classChoices, edit && classesReady, "student_class") { classId = it } }
            item { OutlinedTextField(number, { number = it }, enabled = edit, label = { Text("Öğrenci numarası (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth().testTag("student_number")) }
            item { OutlinedTextField(birth, { birth = it }, enabled = edit, label = { Text("Doğum tarihi (YYYY-AA-GG, isteğe bağlı)") }, modifier = Modifier.fillMaxWidth().testTag("student_birth")) }
            item { OutlinedTextField(enrolled, { enrolled = it }, enabled = edit, label = { Text("Kayıt tarihi (YYYY-AA-GG)") }, supportingText = { if (initial == null) Text("Boş bırakılırsa sunucunun bugünkü okul tarihi kullanılır.") }, modifier = Modifier.fillMaxWidth().testTag("student_enrolled")) }
            item { RegistrySelect("Cinsiyet", gender, listOf(RegistryChoice("", "Belirtilmedi"), RegistryChoice("MALE", "Erkek"), RegistryChoice("FEMALE", "Kız"), RegistryChoice("OTHER", "Diğer")), edit, "student_gender") { gender = it } }
            item { RegistrySelect("Durum", status, listOf(RegistryChoice("ACTIVE", "Aktif"), RegistryChoice("PASSIVE", "Pasif"), RegistryChoice("GRADUATED", "Mezun"), RegistryChoice("LEFT", "Ayrıldı")), edit, "student_status") { status = it } }
            item { OutlinedTextField(address, { address = it }, enabled = edit, label = { Text("Adres") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(notes, { notes = it }, enabled = edit, label = { Text("Not") }, modifier = Modifier.fillMaxWidth().testTag("student_notes")) }
            if (registryCanWrite(state, "health")) item { OutlinedTextField(health, { health = it }, enabled = edit, label = { Text("Sağlık notu") }, modifier = Modifier.fillMaxWidth().testTag("student_health")) }
            if (choicesError != null) item { Text(choicesError!!, color = MaterialTheme.colorScheme.error); TextButton(onClick = { retry++ }) { Text("Seçenekleri yeniden yükle") } }
            item { Text("Veli bağlantıları bu formda değiştirilmez. Bilinmeyen sağlık bilgileri doldurulmaz.") }
            if (submitted && state.registryError != null) item { Text(state.registryError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("registry_error")) }
            item { Button(enabled = edit && branchesReady && classesReady && choicesError == null && branchChoices.any { it.id == branchId } && classChoices.any { it.id == classId } && first.isNotBlank() && last.isNotBlank(), onClick = {
                submitted = true
                onConfirm((initial ?: Student()).copy(organization_id = org, branch_id = branchId, class_id = classId,
                    first_name = first, last_name = last, student_number = number, birth_date = birth, enrollment_date = enrolled.ifBlank { null },
                    gender = gender, status = status, address = address.ifBlank { null }, notes = notes.ifBlank { null },
                    health_notes = if (registryCanWrite(state, "health")) health.ifBlank { null } else initial?.health_notes))
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("registry_student_save")) { Text(if (state.registrySaving) "Kaydediliyor…" else "Kaydet") } }
            item { TextButton(onClick = onDismiss, enabled = !state.registrySaving, modifier = Modifier.heightIn(min = 48.dp)) { Text("Kapat") } }
        }
    }
}

@Composable
fun RegistryClassDialog(initial: ClassGroup?, state: Okul360UiState, onDismiss: () -> Unit, onConfirm: (ClassGroup) -> Unit) {
    val org = organization(state)
    val branches = state.branches.filter { it.organization_id == org && (it.is_active || it.id == initial?.branch_id) }
    var branchId by remember { mutableStateOf(initial?.branch_id ?: state.selectedBranchId.takeIf { id -> branches.any { it.id == id } }.orEmpty()) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var code by remember { mutableStateOf(initial?.code.orEmpty()) }
    var year by remember { mutableStateOf(initial?.academic_year.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var capacity by remember { mutableStateOf(initial?.capacity?.toString() ?: "20") }
    var active by remember { mutableStateOf(initial?.is_active ?: true) }
    var teacherId by remember { mutableStateOf(initial?.teacher_id.orEmpty()) }
    var options by remember { mutableStateOf<List<RegistryChoice>>(emptyList()) }
    var optionsReady by remember { mutableStateOf(false) }
    var optionsError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var submitted by remember { mutableStateOf(false) }
    val revision = remember { state.registrySavedRevision }
    LaunchedEffect(state.registrySavedRevision) { if (state.registrySavedRevision > revision) onDismiss() }
    LaunchedEffect(org, branchId, retry) {
        optionsReady = false; optionsError = null; options = emptyList()
        if (org.isNotBlank() && branchId.isNotBlank()) try {
            options = NativeRegistryClient().teachers(org, branchId); optionsReady = true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { optionsError = "Öğretmen seçenekleri yüklenemedi" }
    }
    val edit = registryCanWrite(state, "class") && !state.registrySaving
    val choices = buildList {
        add(RegistryChoice("", "Öğretmen ataması yok")); addAll(options)
        if (teacherId.isNotBlank() && teacherId == initial?.teacher_id && options.none { it.id == teacherId })
            add(RegistryChoice(teacherId, initial.teacher_name.ifBlank { "Mevcut öğretmen ataması" }))
    }
    RegistryShell(state.registrySaving, onDismiss) {
        LazyColumn(Modifier.fillMaxWidth().testTag("registry_class_form"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(if (initial == null) "Yeni Sınıf" else "Sınıfı Düzenle", style = MaterialTheme.typography.titleLarge) }
            item { RegistrySelect("Şube", branchId, branches.map { RegistryChoice(it.id, it.name) }, edit && initial == null, "class_branch") { branchId = it; teacherId = "" } }
            item { OutlinedTextField(name, { name = it }, enabled = edit, label = { Text("Sınıf adı") }, modifier = Modifier.fillMaxWidth().testTag("class_name")) }
            item { OutlinedTextField(code, { code = it }, enabled = edit, label = { Text("Sınıf kodu (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth().testTag("class_code")) }
            item { OutlinedTextField(capacity, { capacity = it }, enabled = edit, label = { Text("Kapasite") }, modifier = Modifier.fillMaxWidth().testTag("class_capacity")) }
            item { OutlinedTextField(year, { year = it }, enabled = edit, label = { Text("Akademik yıl (isteğe bağlı)") }, modifier = Modifier.fillMaxWidth().testTag("class_year")) }
            item { RegistrySelect("Öğretmen", teacherId, choices, edit && optionsReady, "class_teacher") { teacherId = it } }
            if (optionsError != null) item { Text(optionsError!!, color = MaterialTheme.colorScheme.error); TextButton(onClick = { retry++ }) { Text("Yeniden dene") } }
            item { OutlinedTextField(description, { description = it }, enabled = edit, label = { Text("Açıklama") }, modifier = Modifier.fillMaxWidth().testTag("class_description")) }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Aktif"); Switch(active, { active = it }, enabled = edit) } }
            if (submitted && state.registryError != null) item { Text(state.registryError, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("registry_error")) }
            item { Button(enabled = edit && optionsReady && branches.any { it.id == branchId } && name.isNotBlank() && (capacity.toIntOrNull() ?: 0) > 0, onClick = {
                submitted = true
                onConfirm((initial ?: ClassGroup()).copy(organization_id = org, branch_id = branchId, name = name, code = code,
                    academic_year = year, description = description.ifBlank { null }, capacity = capacity.toInt(), teacher_id = teacherId.ifBlank { null }, is_active = active))
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("registry_class_save")) { Text(if (state.registrySaving) "Kaydediliyor…" else "Kaydet") } }
            item { TextButton(onClick = onDismiss, enabled = !state.registrySaving, modifier = Modifier.heightIn(min = 48.dp)) { Text("Kapat") } }
        }
    }
}
