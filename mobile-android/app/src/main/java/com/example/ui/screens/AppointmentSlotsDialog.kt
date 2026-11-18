package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.ManagedAppointmentSlot
import com.example.data.remote.NativeAppointmentClient
import com.example.data.remote.RegistryHttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private class SlotWrite(
    val submit: suspend () -> Unit,
    val confirmed: (List<ManagedAppointmentSlot>) -> Boolean,
)

@Composable
internal fun AppointmentSlotsDialog(api: NativeAppointmentClient, teacherId: String, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var date by remember { mutableStateOf(LocalDate.now(ZoneId.of("Europe/Istanbul")).toString()) }
    var start by remember { mutableStateOf("") }
    var end by remember { mutableStateOf("") }
    var rows by remember { mutableStateOf<List<ManagedAppointmentSlot>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<SlotWrite?>(null) }
    var remove by remember { mutableStateOf<ManagedAppointmentSlot?>(null) }
    val dateValid = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date) &&
        runCatching { LocalDate.parse(date) }.isSuccess
    val duration = runCatching { ChronoUnit.MINUTES.between(LocalTime.parse(start), LocalTime.parse(end)) }.getOrNull()
    val validTimes = Regex("[0-9]{2}:[0-9]{2}").let { it.matches(start) && it.matches(end) } &&
        duration != null && duration in 15L..60L

    fun refresh(write: SlotWrite? = null, submit: Boolean = true) {
        if (!dateValid || busy) return
        val selectedDate = date
        busy = true
        error = ""
        scope.launch {
            var posted = false
            try {
                if (write != null && submit) {
                    write.submit()
                    posted = true
                }
                val loaded = api.managedSlots(teacherId, selectedDate)
                if (!api.current()) return@launch
                rows = loaded
                if (write != null && !write.confirmed(loaded)) {
                    pending = write
                    error = "Değişiklik listede doğrulanamadı."
                } else if (write != null) {
                    pending = null
                    start = ""
                    end = ""
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (!api.current()) return@launch
                val rejected = submit && !posted && failure is RegistryHttpException &&
                    failure.code in setOf(401, 403, 409, 422)
                if (write != null && !rejected) pending = write
                error = when {
                    write == null -> "Uygunluk saatleri yüklenemedi."
                    rejected -> "İşlem reddedildi. Saat aralığını ve yetkinizi kontrol edin."
                    else -> "Sonuç doğrulanamadı. Listeyi tekrar kontrol edin."
                }
            } finally {
                if (api.current()) busy = false
            }
        }
    }

    LaunchedEffect(Unit) { refresh() }
    val editable = !busy && pending == null
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().padding(12.dp).heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f),
            shape = MaterialTheme.shapes.large) {
            Column(Modifier.imePadding().padding(16.dp)) {
                Text("Uygunluk saatlerim", style = MaterialTheme.typography.titleMedium)
                LazyColumn(Modifier.fillMaxWidth().weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(date, { date = it; rows = null; error = "" }, Modifier.fillMaxWidth(),
                            enabled = editable, label = { Text("Tarih (YYYY-AA-GG)") }, singleLine = true)
                        TextButton(enabled = editable && dateValid, onClick = { refresh() }) { Text("Saatleri getir") }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (rows?.isEmpty() == true) Text("Bu tarihte kayıtlı saat yok.")
                        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    items(rows.orEmpty(), key = { it.id }) { slot ->
                        Text("${slot.start} – ${slot.end}${if (slot.available) "" else " · Kapalı"}")
                        TextButton(enabled = editable, onClick = { remove = slot }) { Text("Saati sil") }
                    }
                    item {
                        OutlinedTextField(start, { start = it }, Modifier.fillMaxWidth(), enabled = editable,
                            label = { Text("Başlangıç (SS:DD)") }, singleLine = true)
                        OutlinedTextField(end, { end = it }, Modifier.fillMaxWidth(), enabled = editable,
                            label = { Text("Bitiş (SS:DD)") }, singleLine = true, supportingText = { Text("15–60 dakika") })
                        Button(enabled = editable && dateValid && validTimes, onClick = {
                            val day = date
                            val from = start
                            val to = end
                            refresh(SlotWrite({ api.createSlot(teacherId, day, from, to) },
                                { slots -> slots.any { it.start == from && it.end == to && it.available } }))
                        }) { Text("Saat ekle") }
                    }
                }
                pending?.let { write ->
                    TextButton(enabled = !busy, onClick = { refresh(write, submit = false) }) { Text("Sonucu kontrol et") }
                }
                TextButton(enabled = !busy, onClick = onDismiss) { Text("Kapat") }
            }
        }
    }
    remove?.let { slot ->
        AlertDialog(onDismissRequest = { remove = null }, title = { Text("Saat silinsin mi?") },
            text = { Text("${slot.start} – ${slot.end}") },
            confirmButton = {
                TextButton(enabled = editable, onClick = {
                    remove = null
                    refresh(SlotWrite({ api.deleteSlot(slot.id) }, { slots -> slots.none { it.id == slot.id } }))
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { remove = null }) { Text("Vazgeç") } })
    }
}
