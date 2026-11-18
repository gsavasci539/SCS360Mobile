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
import com.example.data.model.ParentTeacherAppointment
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

private class AppointmentWrite(
    val submit: suspend () -> Unit,
    val matches: (ParentTeacherAppointment) -> Boolean,
)

@Composable
fun NativeAppointmentScreen(state: Okul360UiState, onBack: () -> Unit) {
    key(financePresentationScope(state)) { AppointmentContent(state, onBack) }
}

@Composable
private fun AppointmentContent(state: Okul360UiState, onBack: () -> Unit) {
    val branch = state.selectedBranchId.takeUnless { it == "all" }
    val api = remember { NativeAppointmentClient(branch) }
    val scope = rememberCoroutineScope()
    val user = state.currentUser
    val canRead = NativeNavigationPolicy.has(user, "appointments.read")
    val canWrite = NativeNavigationPolicy.has(user, "appointments.write")
    val roleSlugs = user.roles.filter { it.is_active }.map { it.slug }.toSet()
    val parent = "parent" in roleSlugs
    val parentOnly = parent && user.is_super_admin != true && roleSlugs.none {
        it in setOf("admin", "administrator", "school_admin", "manager", "principal", "org_admin", "branch_manager", "owner")
    }
    var rows by remember { mutableStateOf<List<ParentTeacherAppointment>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<AppointmentWrite?>(null) }
    var booking by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }

    fun run(write: AppointmentWrite? = null, readBackOnly: Boolean = false) {
        if (busy || !canRead || (write != null && !canWrite)) return
        busy = true
        failure = ""
        scope.launch {
            var received = false
            try {
                if (!readBackOnly && write != null) {
                    write.submit()
                    received = true
                }
                val confirmed = api.records()
                if (!api.current()) return@launch
                rows = confirmed
                loaded = true
                if (write != null) {
                    if (confirmed.any(write.matches)) {
                        pending = null
                        booking = false
                        revision++
                    } else {
                        pending = write
                        failure = "Değişiklik henüz listede görünmüyor."
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!api.current()) return@launch
                if (write == null) {
                    loaded = false
                    failure = "Randevu listesi yüklenemedi."
                } else if (!readBackOnly && !received && error is RegistryHttpException &&
                    error.code in setOf(401, 403, 404, 422)) {
                    failure = "İşlem reddedildi. Seçimlerinizi ve yetkinizi kontrol edin."
                } else {
                    if (!received && error is RegistryHttpException && error.code == 409) {
                        failure = "Saat dolu veya kayıt değişti. Sonucu listeden kontrol edin."
                    } else failure = "İşlem doğrulanamadı. Sonucu listeden kontrol edin."
                    pending = write
                }
            } finally {
                if (api.current()) busy = false
            }
        }
    }

    LaunchedEffect(canRead) { run() }
    if (!api.current()) return
    if (!canRead) {
        Column(Modifier.padding(16.dp)) {
            Text("Randevu kayıtlarını görme yetkiniz yok.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    val checkResult: (() -> Unit)? = pending?.let { write -> { run(write, readBackOnly = true) } }
    AppointmentsScreen(
        state.copy(appointments = rows), onBack,
        onBookAppointment = if (canWrite) ({ booking = true; failure = "" }) else null,
        onUpdateStatus = { id, status, note ->
            if (canWrite && (!parentOnly || status == "CANCELLED") && pending == null) {
                val decision = AppointmentDecision(id, status, note)
                run(AppointmentWrite({ api.decide(decision) }, decision::matches))
            }
        },
        canManage = canWrite && !parentOnly, canCancel = canWrite && parentOnly, busy = busy,
        loaded = loaded, failure = failure, onReload = { run() }, onCheckResult = checkResult,
        onClearPending = { pending = null; run() }, savedRevision = revision,
    )
    if (booking && canWrite) {
        AppointmentBookingDialog(
            api = api, parent = parent, busy = busy, failure = failure, checkResult = checkResult,
            onDismiss = { booking = false },
            onSubmit = { draft -> run(AppointmentWrite({ api.create(draft, parent) }, draft::matches)) },
        )
    }
}

@Composable
private fun AppointmentBookingDialog(
    api: NativeAppointmentClient,
    parent: Boolean,
    busy: Boolean,
    failure: String,
    checkResult: (() -> Unit)?,
    onDismiss: () -> Unit,
    onSubmit: (AppointmentDraft) -> Unit,
) {
    var students by remember { mutableStateOf<List<AppointmentChoice>>(emptyList()) }
    var teachers by remember { mutableStateOf<List<AppointmentChoice>>(emptyList()) }
    var slots by remember { mutableStateOf<List<AppointmentSlot>>(emptyList()) }
    var student by remember { mutableStateOf<AppointmentChoice?>(null) }
    var teacher by remember { mutableStateOf<AppointmentChoice?>(null) }
    var slot by remember { mutableStateOf<AppointmentSlot?>(null) }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var optionsError by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    val dateValid = remember(date) {
        Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date) && runCatching { LocalDate.parse(date) }.isSuccess
    }
    val timeValid = remember(time) {
        Regex("[0-9]{2}:[0-9]{2}").matches(time) && runCatching { LocalTime.parse(time) }.isSuccess
    }
    LaunchedEffect(reload) {
        try {
            students = api.options()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { optionsError = "Öğrenciler yüklenemedi." }
    }
    LaunchedEffect(student?.id, reload) {
        val selected = student ?: return@LaunchedEffect
        try {
            teachers = api.options(selected.id)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { optionsError = "Öğretmenler yüklenemedi." }
    }
    LaunchedEffect(teacher?.id, date, reload) {
        slots = emptyList()
        slot = null
        val selected = teacher ?: return@LaunchedEffect
        if (parent && dateValid) {
            try {
                slots = api.slots(selected.id, date)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { optionsError = "Müsait saatler yüklenemedi." }
        }
    }

    val editable = !busy && checkResult == null
    val valid = student != null && teacher != null && dateValid && optionsError.isEmpty() &&
        (if (parent) slot != null else timeValid)
    Dialog(onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(12.dp).heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Randevu oluştur", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { AppointmentPicker("Öğrenci", students, student, editable, { it.name }) {
                        if (student != it) {
                            student = it; teacher = null; teachers = emptyList(); slot = null; optionsError = ""
                        }
                    } }
                    item { AppointmentPicker("Öğretmen", teachers, teacher, editable, { it.name }) {
                        if (teacher != it) { teacher = it; slot = null; optionsError = "" }
                    } }
                    item { OutlinedTextField(date, { date = it; slot = null; optionsError = "" },
                        Modifier.fillMaxWidth(), enabled = editable, label = { Text("Tarih (YYYY-AA-GG)") }, singleLine = true) }
                    item {
                        if (parent) {
                            AppointmentPicker("Müsait saat", slots, slot, editable, { "${it.start} – ${it.end}" }) { slot = it }
                            if (teacher != null && dateValid && slots.isEmpty()) Text("Müsait saat görünmüyor.")
                        } else OutlinedTextField(time, { time = it }, Modifier.fillMaxWidth(),
                            enabled = editable, label = { Text("Saat (SS:DD)") }, singleLine = true)
                    }
                    item { OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), enabled = editable,
                        label = { Text("Görüşme notu (isteğe bağlı)") }) }
                    if (optionsError.isNotBlank()) item {
                        Text(optionsError, color = MaterialTheme.colorScheme.error)
                        TextButton(enabled = editable, onClick = { optionsError = ""; reload++ }) { Text("Seçenekleri yenile") }
                    }
                    if (failure.isNotBlank()) item { Text(failure, color = MaterialTheme.colorScheme.error) }
                }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Button(enabled = !busy && (checkResult != null || valid),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("appointment_submit"),
                        onClick = {
                            if (checkResult != null) checkResult()
                            else onSubmit(AppointmentDraft(requireNotNull(teacher), requireNotNull(student), date,
                                slot?.start ?: time, note, slot?.duration ?: 30))
                        }) { Text(if (checkResult == null) "Randevuyu kaydet" else "Sonucu kontrol et") }
                    TextButton(enabled = !busy, onClick = onDismiss) { Text("Kapat") }
                }
            }
        }
    }
}

@Composable
private fun <T> AppointmentPicker(label: String, options: List<T>, selected: T?, enabled: Boolean,
    name: (T) -> String, choose: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled && options.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (selected == null) "$label seçin" else "$label: ${name(selected)}")
        }
        DropdownMenu(expanded && enabled, onDismissRequest = { expanded = false }, modifier = Modifier.heightIn(max = 240.dp)) {
            for (option in options) {
                DropdownMenuItem(text = { Text(name(option)) }, onClick = { expanded = false; choose(option) })
            }
        }
    }
}
