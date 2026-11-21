package com.example.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.remote.TransportChoices
import com.example.data.remote.TransportRouteDraft
import com.example.data.remote.TransportStopDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.util.UUID

@Composable
internal fun TransportRouteEditor(initial: TransportRouteDraft, existing: Boolean, fixedBranch: Boolean,
    busy: Boolean, message: String?, close: () -> Unit,
    loadChoices: suspend (String, String, String) -> TransportChoices, save: (TransportRouteDraft, UUID) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    var search by remember { mutableStateOf("") }
    var choosingBranch by remember { mutableStateOf(false) }
    var choosingDriver by remember { mutableStateOf(false) }
    val request = remember(draft) { UUID.randomUUID() }
    val choices by key(draft.branch, draft.driver, search) {
        produceState<Result<TransportChoices>?>(null) {
            value = try {
                if (search.isNotBlank()) delay(250)
                Result.success(loadChoices(draft.branch, search.trim(), draft.driver))
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                Result.failure(failure)
            }
        }
    }
    val data = choices?.getOrNull()
    val valid = draft.name.isNotBlank() && draft.branch.isNotBlank() && (draft.capacity.toIntOrNull() ?: 0) > 0
    /*
     * Branch and driver options are separate from the form fields so a long result list does not
     * push the save button out of reach with a keyboard open.
     */
    if (choosingBranch || choosingDriver) {
        val isBranch = choosingBranch
        val rows = if (isBranch) data?.branches else data?.drivers
        ConsentFormDialog(if (isBranch) "Rota şubesi seç" else "Sürücü seç", "native_transport_route_options", busy,
            { choosingBranch = false; choosingDriver = false; search = "" }, fields = {
                if (!isBranch) item { TransportEditorField(search, "Sürücü ara", 100, busy) { search = it } }
                if (rows.isNullOrEmpty()) item { Text(when {
                    choices == null -> "Seçenekler yükleniyor…"
                    choices?.isFailure == true -> "Seçenekler yüklenemedi."
                    else -> "Bu kapsamda seçenek yok."
                }) }
                if (rows != null) items(rows, key = { it.id }) { choice ->
                    TextButton(enabled = !busy, onClick = {
                        draft = if (isBranch) draft.copy(branch = choice.id, driver = "") else draft.copy(driver = choice.id)
                        choosingBranch = false
                        choosingDriver = false
                        search = ""
                    }) { Text(choice.name) }
                }
                if (!isBranch && data?.moreDrivers == true) item { Text("Diğer sürücüler için aramayı daraltın.") }
            }, confirm = {})
        return
    }
    ConsentFormDialog(if (existing) "Servis rotasını düzenle" else "Servis rotası oluştur",
        "native_transport_route_editor", busy, close, fields = {
            item {
                val name = data?.branches?.firstOrNull { it.id == draft.branch }?.name
                TextButton(enabled = !busy && !fixedBranch, onClick = { choosingBranch = true }) {
                    Text(name ?: if (draft.branch.isBlank()) "Şube seç" else "Seçili şube")
                }
            }
            item { TransportEditorField(draft.name, "Rota adı", 255, busy) { draft = draft.copy(name = it) } }
            item { TransportEditorField(draft.number, "Rota numarası", 50, busy) { draft = draft.copy(number = it) } }
            item { TransportEditorField(draft.plate, "Araç plakası", 20, busy) { draft = draft.copy(plate = it) } }
            item { TransportEditorField(draft.capacity, "Araç kapasitesi", 10, busy) { draft = draft.copy(capacity = it.filter(Char::isDigit)) } }
            item {
                val driverName = data?.drivers?.firstOrNull { it.id == draft.driver }?.name
                TextButton(enabled = !busy && draft.branch.isNotBlank(), onClick = { choosingDriver = true }) {
                    Text(driverName ?: if (draft.driver.isBlank()) "Sürücü seç" else "Atanan sürücü")
                }
                if (draft.driver.isNotBlank()) TextButton(enabled = !busy, onClick = { draft = draft.copy(driver = "") }) {
                    Text("Sürücü atamasını kaldır")
                }
            }
            item {
                listOf("ACTIVE" to "Aktif", "MAINTENANCE" to "Bakımda", "OUT_OF_SERVICE" to "Hizmet dışı").forEach { (status, label) ->
                    FilterChip(draft.status == status, { draft = draft.copy(status = status) }, { Text(label) }, enabled = !busy)
                }
            }
            item { TransportEditorField(draft.description, "Rota açıklaması", 1000, busy) { draft = draft.copy(description = it) } }
            message?.let { item { Text(it) } }
        }, confirm = {
            Button(enabled = valid && !busy, onClick = { save(draft.copy(name = draft.name.trim(), number = draft.number.trim(),
                plate = draft.plate.trim(), description = draft.description.trim()), request) }) { Text("Rotayı kaydet") }
        })
}

@Composable
internal fun TransportStopEditor(initial: TransportStopDraft, existing: Boolean, busy: Boolean, message: String?,
    close: () -> Unit, save: (TransportStopDraft, UUID) -> Unit) {
    var draft by remember { mutableStateOf(initial) }
    val request = remember(draft) { UUID.randomUUID() }
    val latitude = draft.latitude.replace(',', '.')
    val longitude = draft.longitude.replace(',', '.')
    val valid = draft.name.isNotBlank() && (draft.order.toIntOrNull() ?: 0) > 0 &&
        (latitude.isBlank() || latitude.toDoubleOrNull()?.let { it in -90.0..90.0 } == true) &&
        (longitude.isBlank() || longitude.toDoubleOrNull()?.let { it in -180.0..180.0 } == true) &&
        (draft.arrival.isBlank() || runCatching { LocalTime.parse(draft.arrival) }.isSuccess)
    ConsentFormDialog(if (existing) "Rota durağını düzenle" else "Rota durağı ekle", "native_transport_stop_editor",
        busy, close, fields = {
            item { TransportEditorField(draft.name, "Durak adı", 255, busy) { draft = draft.copy(name = it) } }
            item { TransportEditorField(draft.order, "Durak sırası", 10, busy) { draft = draft.copy(order = it.filter(Char::isDigit)) } }
            item { TransportEditorField(draft.latitude, "Enlem", 25, busy) { draft = draft.copy(latitude = it) } }
            item { TransportEditorField(draft.longitude, "Boylam", 25, busy) { draft = draft.copy(longitude = it) } }
            item { TransportEditorField(draft.arrival, "Tahmini varış (SS:DD)", 24, busy) { draft = draft.copy(arrival = it) } }
            message?.let { item { Text(it) } }
        }, confirm = {
            Button(enabled = valid && !busy, onClick = { save(draft.copy(name = draft.name.trim(), latitude = latitude,
                longitude = longitude), request) }) { Text("Durağı kaydet") }
        })
}

@Composable
private fun TransportEditorField(value: String, label: String, max: Int, busy: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(max)) }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
        label = { Text(label) }, singleLine = max < 1000)
}
