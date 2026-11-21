package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.remote.InventoryChoice
import com.example.data.remote.InventoryRecords
import com.example.data.remote.NativeInventoryClient
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ResourcesScreen(state: Okul360UiState, onBack: () -> Unit) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "inventory.read") ||
        user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) {
        Column(Modifier.padding(16.dp)) {
            Text("Envanter için yetkili kurum çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    val context = LocalContext.current.applicationContext
    key(financePresentationScope(state), state.serverBaseUrl) {
        InventoryWorkspace(remember { NativeInventoryClient(state.selectedBranchId, context, user.id,
            user.organization_id, state.workspaceOrganizationId) },
            NativeNavigationPolicy.has(user, "inventory.write"), onBack)
    }
}

@Composable
private fun InventoryWorkspace(client: NativeInventoryClient, canWrite: Boolean, onBack: () -> Unit) {
    var kind by remember { mutableStateOf(InventoryRecords.ITEMS) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<InventoryRecords?>(null) }
    var confirmation by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val records by inventoryLoad(kind, page, revision) { client.list(kind, page) }
    val summary by inventoryLoad(revision) { client.summary() }
    val rows = records?.getOrNull().orEmpty()
    val coroutine = rememberCoroutineScope()

    fun change(request: suspend () -> Unit) {
        if (busy) return
        error = null
        message = null
        busy = true
        coroutine.launch {
            try {
                request()
                revision++
                editor = null
                confirmation = null
                message = "Envanter kaydı doğrulandı."
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                error = "Envanter kaydı doğrulanamadı."
            } finally { busy = false }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Demirbaş ve Envanter", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack, enabled = !busy) { Text("Geri") }
            message?.let { Text(it) }
            TextButton(onClick = { revision++ }, enabled = !busy) { Text("Envanteri yenile") }
            summary?.getOrNull()?.let {
                Text("Toplam ${it.getInt("total_items")} · Zimmetli ${it.getInt("assigned_count")}")
                Text("Kullanılabilir ${it.getInt("available_count")} · Açık bakım ${it.getInt("pending_maintenance")}")
            }
            if (summary?.isFailure == true) Text("Envanter özeti yüklenemedi.")
        }
        items(InventoryRecords.entries) { recordType ->
            TextButton(enabled = !busy && kind != recordType, onClick = { kind = recordType; page = 0; message = null }) {
                Text(recordType.label)
            }
        }
        if (canWrite) item {
            Button(enabled = !busy, onClick = { editor = kind; error = null; message = null }) {
                Text(when (kind) {
                    InventoryRecords.ITEMS -> "Demirbaş oluştur"
                    InventoryRecords.LOCATIONS -> "Konum oluştur"
                    InventoryRecords.ASSIGNMENTS -> "Zimmet oluştur"
                    InventoryRecords.MAINTENANCE -> "Bakım talebi oluştur"
                })
            }
        }
        if (rows.isEmpty()) item {
            Text(when {
                records == null -> "Envanter yükleniyor…"
                records?.isFailure == true -> "Envanter kayıtları yüklenemedi."
                else -> "Bu kapsamda kayıt yok."
            })
        }
        items(rows.take(20), key = { it.getString("id") }) { row ->
            val details = when (kind) {
                InventoryRecords.ITEMS -> listOf("item_name", "item_code", "branch_name", "location_name", "category", "status")
                InventoryRecords.LOCATIONS -> listOf("location_name", "branch_name", "location_type", "description")
                InventoryRecords.ASSIGNMENTS -> listOf("item_name", "assigned_to_name", "assigned_date", "return_date", "notes", "status")
                InventoryRecords.MAINTENANCE -> listOf("item_name", "issue_description", "priority", "assigned_to_name", "status", "resolved_at")
            }
            val status = row.inventoryText("status")
            val action = when {
                kind == InventoryRecords.ASSIGNMENTS && status == "ASSIGNED" -> "İade al"
                kind == InventoryRecords.MAINTENANCE && status != "RESOLVED" -> "Bakımı tamamla"
                else -> null
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val values = details.map { row.inventoryText(it) }.filter(String::isNotBlank)
                    values.firstOrNull()?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                    values.drop(1).forEach { Text(inventoryLabels[it] ?: it) }
                    if (canWrite && action != null) {
                        TextButton(enabled = !busy, onClick = { confirmation = row; error = null }) { Text(action) }
                    }
                }
            }
        }
        if (page > 0 || rows.size > 20) item {
            if (page > 0) TextButton(onClick = { page-- }, enabled = !busy) { Text("Önceki sayfa") }
            if (rows.size > 20) TextButton(onClick = { page++ }, enabled = !busy) { Text("Sonraki sayfa") }
        }
    }
    editor?.let { recordType ->
        InventoryEditor(client, recordType, busy, error, onClose = { editor = null }) { branch, form ->
            change { client.save(recordType, branch, form) }
        }
    }
    confirmation?.let { row ->
        AlertDialog(onDismissRequest = { if (!busy) confirmation = null },
            title = { Text(if (kind == InventoryRecords.ASSIGNMENTS) "İadeyi kaydet" else "Bakımı tamamla") },
            text = { Text(error ?: row.inventoryText("item_name")) },
            confirmButton = {
                Button(enabled = !busy, onClick = { change { client.finish(kind, row) } }) { Text("Onayla") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { confirmation = null }) { Text("Vazgeç") } })
    }
}

private val inventoryLabels = mapOf("ACTIVE" to "Etkin", "ASSIGNED" to "Zimmetli", "RETURNED" to "İade edildi",
    "PENDING" to "Açık", "RESOLVED" to "Tamamlandı", "LOW" to "Düşük", "MEDIUM" to "Normal",
    "HIGH" to "Yüksek", "URGENT" to "Acil")

private enum class InventoryPicker(val title: String) {
    BRANCH("Şube"), LOCATION("Konum"), ITEM("Demirbaş"), USER("Sorumlu"), PRIORITY("Öncelik")
}

@Composable
private fun InventoryEditor(
    client: NativeInventoryClient, kind: InventoryRecords, busy: Boolean, error: String?,
    onClose: () -> Unit, onSave: (String, JSONObject) -> Unit
) {
    var branch by remember { mutableStateOf<InventoryChoice?>(null) }
    var location by remember { mutableStateOf<InventoryChoice?>(null) }
    var item by remember { mutableStateOf<InventoryChoice?>(null) }
    var user by remember { mutableStateOf<InventoryChoice?>(null) }
    var priority by remember { mutableStateOf("MEDIUM") }
    var picker by remember { mutableStateOf<InventoryPicker?>(null) }
    var search by remember { mutableStateOf("") }
    var reload by remember { mutableIntStateOf(0) }
    var title by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val branchId = client.activeBranch ?: branch?.id
    val needle = search.trim()
    val itemSearch = if (picker == InventoryPicker.ITEM) needle else ""
    val userSearch = if (picker == InventoryPicker.USER) needle else ""
    val options by inventoryLoad(branchId, itemSearch, userSearch, reload) {
        if (itemSearch.isNotEmpty() || userSearch.isNotEmpty()) delay(250)
        client.options(branchId, itemSearch, userSearch)
    }
    val choices = options?.getOrNull()
    val dateValid = date.isBlank() || runCatching { LocalDate.parse(date.trim()) }.isSuccess
    val isReady = branchId != null && choices != null && when (kind) {
        InventoryRecords.ITEMS -> title.isNotBlank() && code.isNotBlank()
        InventoryRecords.LOCATIONS -> title.isNotBlank()
        InventoryRecords.ASSIGNMENTS -> item?.assigned == false && user != null && dateValid
        InventoryRecords.MAINTENANCE -> item != null && notes.isNotBlank()
    }

    fun showPicker(choice: InventoryPicker) { search = ""; picker = choice }

    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text(kind.label) },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (client.activeBranch == null) InventoryPickerButton("Şube", branch?.name, busy) { showPicker(InventoryPicker.BRANCH) }
                else choices?.branches?.singleOrNull()?.let { Text(it.name) }
                if (options?.isFailure == true) {
                    Text("Seçenekler alınamadı.")
                    TextButton(onClick = { reload++ }, enabled = !busy) { Text("Seçenekleri yeniden yükle") }
                }
                if (branchId != null && picker == null) when (kind) {
                    InventoryRecords.ITEMS -> {
                        InventoryField(code, "Demirbaş kodu", 50, busy) { code = it }
                        InventoryField(title, "Demirbaş adı", 255, busy) { title = it }
                        InventoryPickerButton("Konum", location?.name, busy) { showPicker(InventoryPicker.LOCATION) }
                        if (location != null) TextButton(enabled = !busy, onClick = { location = null }) { Text("Konumu kaldır") }
                    }
                    InventoryRecords.LOCATIONS -> InventoryField(title, "Konum adı", 255, busy) { title = it }
                    InventoryRecords.ASSIGNMENTS -> {
                        InventoryPickerButton("Demirbaş", item?.name, busy) { showPicker(InventoryPicker.ITEM) }
                        if (item?.assigned == true) Text("Demirbaş zaten zimmetli.")
                        InventoryPickerButton("Sorumlu", user?.name, busy) { showPicker(InventoryPicker.USER) }
                        InventoryField(date, "Planlanan iade (YYYY-AA-GG)", 10, busy) { date = it }
                        if (!dateValid) Text("Geçerli bir tarih girin.")
                        InventoryField(notes, "Zimmet notu", 2000, busy) { notes = it }
                    }
                    InventoryRecords.MAINTENANCE -> {
                        InventoryPickerButton("Demirbaş", item?.name, busy) { showPicker(InventoryPicker.ITEM) }
                        InventoryPickerButton("Sorumlu", user?.name, busy) { showPicker(InventoryPicker.USER) }
                        if (user != null) TextButton(enabled = !busy, onClick = { user = null }) { Text("Sorumluyu kaldır") }
                        InventoryPickerButton("Öncelik", inventoryLabels[priority], busy) { showPicker(InventoryPicker.PRIORITY) }
                        InventoryField(notes, "Sorun açıklaması", 2000, busy) { notes = it }
                    }
                }
                error?.let { Text(it) }
            }
        },
        confirmButton = { Button(enabled = isReady && !busy && picker == null, onClick = {
            val selected = requireNotNull(branchId)
            val body = when (kind) {
                InventoryRecords.ITEMS -> JSONObject().put("branch_id", selected).put("item_code", code.trim())
                    .put("item_name", title.trim()).put("location_id", location?.id ?: JSONObject.NULL)
                InventoryRecords.LOCATIONS -> JSONObject().put("branch_id", selected).put("location_name", title.trim())
                InventoryRecords.ASSIGNMENTS -> JSONObject().put("item_id", requireNotNull(item).id)
                    .put("assigned_to", requireNotNull(user).id)
                    .put("return_date", date.trim().ifEmpty { null } ?: JSONObject.NULL)
                    .put("notes", notes.trim().ifEmpty { null } ?: JSONObject.NULL)
                InventoryRecords.MAINTENANCE -> JSONObject().put("item_id", requireNotNull(item).id)
                    .put("issue_description", notes.trim()).put("priority", priority).put("assigned_to", user?.id ?: JSONObject.NULL)
            }
            onSave(selected, body)
        }) { Text("Kaydet") } },
        dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text("Vazgeç") } })

    picker?.let { target ->
        val rows = when (target) {
            InventoryPicker.BRANCH -> choices?.branches
            InventoryPicker.LOCATION -> choices?.locations
            InventoryPicker.ITEM -> choices?.items
            InventoryPicker.USER -> choices?.users
            InventoryPicker.PRIORITY -> listOf("LOW", "MEDIUM", "HIGH", "URGENT").map { InventoryChoice(it, inventoryLabels.getValue(it), "", false) }
        }
        AlertDialog(onDismissRequest = { picker = null }, title = { Text(target.title) },
            text = {
                Column {
                    if (target == InventoryPicker.ITEM || target == InventoryPicker.USER) {
                        InventoryField(search, "Ara", 100, false) { search = it }
                    }
                    if (target == InventoryPicker.ITEM && choices?.moreItems == true || target == InventoryPicker.USER && choices?.moreUsers == true) {
                        Text("Diğer kayıtları bulmak için arayın.")
                    }
                    if (rows == null) Text(if (options?.isFailure == true) "Seçenekler alınamadı." else "Seçenekler yükleniyor…")
                    else if (rows.isEmpty()) Text("Seçilebilir kayıt bulunamadı.")
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 230.dp)) {
                        items(rows.orEmpty(), key = { it.id }) { choice ->
                            TextButton(onClick = {
                                when (target) {
                                    InventoryPicker.BRANCH -> { branch = choice; location = null; item = null; user = null }
                                    InventoryPicker.LOCATION -> location = choice
                                    InventoryPicker.ITEM -> item = choice
                                    InventoryPicker.USER -> user = choice
                                    InventoryPicker.PRIORITY -> priority = choice.id
                                }
                                picker = null
                            }) {
                                Text(listOf(choice.code, choice.name, if (choice.assigned) "(zimmetli)" else "")
                                    .filter(String::isNotBlank).joinToString(" "))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picker = null }) { Text("Kapat") } })
    }
}

@Composable
private fun InventoryPickerButton(label: String, value: String?, busy: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = !busy) { Text(if (value == null) "$label seç" else "$label · $value") }
}

@Composable
private fun InventoryField(value: String, label: String, limit: Int, busy: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(limit)) }, label = { Text(label) }, enabled = !busy,
        modifier = Modifier.fillMaxWidth(), singleLine = limit < 1000)
}

@Composable
private fun <T> inventoryLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> =
    key(keys.toList()) {
        produceState<Result<T>?>(null) {
            value = try {
                Result.success(load())
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                Result.failure(failure)
            }
        }
    }

private fun JSONObject.inventoryText(key: String) = if (isNull(key)) "" else getString(key)
