package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

@Composable
internal fun DismissalScreen(state: Okul360UiState, openSecurity: () -> Unit) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "dismissal.read") ||
        (user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank())) {
        Text("Öğrenci teslim kayıtları için yetkili kurum çalışma alanını açın.", Modifier.padding(16.dp))
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        val client = remember { NativeDismissalClient(state.selectedBranchId) }
        DismissalWorkspace(client, NativeNavigationPolicy.has(user, "dismissal.write"),
            openSecurity.takeIf { NativeNavigationPolicy.has(user, "security.read") })
    }
}

@Composable
private fun DismissalWorkspace(client: NativeDismissalClient, canWrite: Boolean, openSecurity: (() -> Unit)?) {
    var student by remember { mutableStateOf<DismissalStudent?>(null) }
    var picking by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DismissalGrant?>(null) }
    var revoking by remember { mutableStateOf<DismissalGrant?>(null) }
    var tab by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var dayInput by remember { mutableStateOf<String?>(null) }
    var chosenDay by remember { mutableStateOf<LocalDate?>(null) }
    val child = student?.id
    var identity by remember(child) { mutableStateOf("") }
    val deliveryId = remember(child, identity) { UUID.randomUUID() }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val coroutine = rememberCoroutineScope()

    fun mutate(action: suspend () -> String) {
        if (!canWrite || busy) return
        busy = true
        notice = null
        coroutine.launch {
            try {
                notice = action()
                page = 0
                revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                notice = "Teslim işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    val today by dismissalLoad(revision) { client.options("", null).today }
    val day = chosenDay ?: today?.getOrNull()
    val summary by dismissalLoad(day, child, revision) { day?.let { client.totals(child, it) } }
    val grants by dismissalLoad(tab, page, child, revision) {
        if (tab == 1) client.grants(child, 21, page * 20) else emptyList()
    }
    val logs by dismissalLoad(tab, page, child, day, revision) {
        if (tab == 0 && day != null) client.records(child, day, 21, page * 20) else emptyList()
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Öğrenci Teslim Masası", style = MaterialTheme.typography.headlineSmall)
            openSecurity?.let { TextButton(onClick = it) { Text("Kapı Güvenliği") } }
            summary?.getOrNull()?.let { Text("Etkin yetki ${it.active} · Seçili günde teslim ${it.pickups}") }
            if (today?.isFailure == true || summary?.isFailure == true) Text("Teslim özeti yüklenemedi.")
            TextButton(enabled = !busy, onClick = { picking = true }) { Text(student?.name ?: "Tüm öğrenciler · Öğrenci seç") }
            if (student != null) TextButton(enabled = !busy, onClick = { student = null; page = 0; notice = null }) {
                Text("Tüm öğrencileri göster")
            }
            notice?.let { Text(it) }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Kayıtları yenile") }
        }
        item {
            TabRow(tab) {
                Tab(tab == 0, onClick = { if (!busy) { tab = 0; page = 0 } }, text = { Text("Geçmiş") })
                Tab(tab == 1, onClick = { if (!busy) { tab = 1; page = 0 } }, text = { Text("Yetkiler") })
            }
        }
        if (tab == 0) {
            item {
                OutlinedTextField(dayInput ?: day?.toString().orEmpty(), { dayInput = it.take(10) },
                    label = { Text("Teslim tarihi (YYYY-AA-GG)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                TextButton(enabled = !busy && dismissalDate(dayInput.orEmpty()) != null, onClick = {
                    chosenDay = dismissalDate(dayInput.orEmpty()); page = 0
                }) { Text("Tarihi uygula") }
                if (canWrite && student?.active == true && child != null) {
                    OutlinedTextField(identity, { identity = it.take(50) }, label = { Text("Teslim alanın kimlik bilgisi") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy,
                        visualTransformation = PasswordVisualTransformation())
                    Button(enabled = !busy && identity.isNotBlank(), onClick = { mutate {
                        val recorded = client.verify(child, identity.trim(), deliveryId)
                        if (recorded == null) "Girilen kimlikle geçerli teslim yetkisi bulunamadı."
                        else {
                            identity = ""
                            "Teslim kaydı doğrulandı. ${recorded.student} · ${recorded.time}"
                        }
                    } }) { Text("Kimliği doğrula ve teslimi kaydet") }
                }
            }
            val rows = logs?.getOrNull().orEmpty()
            if (rows.isEmpty()) item { Text(dismissalMessage(logs, "Seçili günde teslim kaydı yok.")) }
            items(rows.take(20), key = { it.id }) { row ->
                DismissalCard(listOf(row.student, row.adult, row.time, row.operator, row.method, row.notes))
            }
        } else {
            if (canWrite && student?.active == true) item {
                Button(enabled = !busy && today?.getOrNull() != null, onClick = { editing = null; dialog = true }) {
                    Text("Teslim yetkisi oluştur")
                }
            }
            val rows = grants?.getOrNull().orEmpty()
            if (rows.isEmpty()) item { Text(dismissalMessage(grants, "Bu kapsamda teslim yetkisi yok.")) }
            items(rows.take(20), key = { it.id }) { row ->
                val grant = row.form
                val status = when (row.effectiveStatus) {
                    "ACTIVE" -> "Etkin"
                    "FUTURE" -> "İleri tarihli"
                    "EXPIRED" -> "Süresi doldu"
                    "REVOKED" -> "İptal edildi"
                    else -> "İnceleme gerekli"
                }
                val dates = listOf(grant.from, grant.until).filter(String::isNotBlank).joinToString(" – ")
                DismissalCard(listOf(row.student, grant.name, status, dates, grant.relationship, grant.notes)) {
                    if (canWrite && row.status == "ACTIVE") {
                        TextButton(enabled = !busy, onClick = { editing = row; dialog = true }) { Text("Yetkiyi düzenle") }
                        TextButton(enabled = !busy, onClick = { revoking = row }) { Text("Yetkiyi iptal et") }
                    }
                }
            }
        }
        val size = if (tab == 0) logs?.getOrNull()?.size else grants?.getOrNull()?.size
        if (page > 0 || (size ?: 0) > 20) item {
            Text("Sayfa ${page + 1}")
            if (page > 0) TextButton(enabled = !busy, onClick = { page-- }) { Text("Önceki sayfa") }
            if ((size ?: 0) > 20) TextButton(enabled = !busy, onClick = { page++ }) { Text("Sonraki sayfa") }
        }
    }
    if (picking) {
        var search by remember { mutableStateOf("") }
        val result by dismissalLoad(search) {
            if (search.isNotBlank()) delay(250)
            client.options(search.trim(), child)
        }
        ConsentFormDialog("Öğrenci seç", "native_dismissal_students", false, { picking = false }, fields = {
            item { OutlinedTextField(search, { search = it.take(100) }, Modifier.fillMaxWidth(), label = { Text("Öğrenci ara") }) }
            val options = result?.getOrNull()
            if (options == null || options.students.isEmpty()) item { Text(dismissalMessage(result, "Öğrenci bulunamadı.")) }
            if (options != null) {
                items(options.students, key = { it.id }) { choice ->
                    TextButton(onClick = { student = choice; picking = false; page = 0; notice = null }) { Text(choice.name) }
                }
                if (options.more) item { Text("Daha fazla sonuç için aramayı daraltın.") }
            }
        }, confirm = {})
    }
    val target = editing?.studentId ?: child
    if (dialog && canWrite && target != null) {
        DismissalEditor(editing?.student ?: student?.name.orEmpty(),
            editing?.form ?: DismissalGrantForm(from = today?.getOrNull()?.toString().orEmpty()), busy, notice,
            { dialog = false; notice = null }) { form, nonce ->
            mutate { client.save(target, form, nonce, editing?.id); dialog = false; "Teslim yetkisi sunucudan doğrulandı." }
        }
    }
    revoking?.let { record ->
        AlertDialog(onDismissRequest = { if (!busy) revoking = null }, title = { Text("Teslim yetkisini iptal et") },
            text = { Text(notice ?: record.form.name) },
            confirmButton = { Button(enabled = !busy, onClick = { mutate {
                client.revoke(record); revoking = null; "Teslim yetkisinin iptali doğrulandı."
            } }) { Text("Yetkiyi iptal et") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { revoking = null; notice = null }) { Text("Geri dön") } })
    }
}

@Composable
private fun DismissalCard(lines: List<String>, actions: @Composable () -> Unit = {}) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lines.filter(String::isNotBlank).forEach { Text(it) }
            actions()
        }
    }
}

@Composable
private fun DismissalEditor(student: String, initial: DismissalGrantForm, busy: Boolean, notice: String?,
    close: () -> Unit, save: (DismissalGrantForm, UUID) -> Unit) {
    var form by remember { mutableStateOf(initial) }
    val nonce = remember(form) { UUID.randomUUID() }
    val from = dismissalDate(form.from)
    val until = dismissalDate(form.until)
    val valid = form.name.isNotBlank() && form.identity.isNotBlank() && from != null &&
        (form.until.isBlank() || (until != null && !until.isBefore(from)))
    ConsentFormDialog("Teslim yetkisi", "native_dismissal_grant", busy, close, fields = {
        item { Text(student) }
        item { GrantField(form.name, "Teslim alacak kişi", 255, busy) { form = form.copy(name = it) } }
        item { GrantField(form.identity, "Kimlik bilgisi", 50, busy) { form = form.copy(identity = it) } }
        item { GrantField(form.relationship, "Yakınlık", 100, busy) { form = form.copy(relationship = it) } }
        item { GrantField(form.phone, "Telefon", 20, busy) { form = form.copy(phone = it) } }
        item { GrantField(form.from, "Başlangıç (YYYY-AA-GG)", 10, busy) { form = form.copy(from = it) } }
        item { GrantField(form.until, "Bitiş (isteğe bağlı)", 10, busy) { form = form.copy(until = it) } }
        item { GrantField(form.photo, "Fotoğraf bağlantısı", 500, busy) { form = form.copy(photo = it) } }
        item { GrantField(form.notes, "Not", 1000, busy) { form = form.copy(notes = it) } }
        notice?.let { item { Text(it) } }
    }, confirm = { Button(enabled = !busy && valid, onClick = {
        save(DismissalGrantForm(form.name.trim(), form.identity.trim(), form.relationship.trim(), form.phone.trim(),
            from.toString(), until?.toString().orEmpty(), form.photo.trim(), form.notes.trim()), nonce)
    }) { Text("Yetkiyi kaydet") } })
}

@Composable
private fun GrantField(value: String, label: String, max: Int, busy: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.take(max)) }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = max < 1000)
}

private fun dismissalDate(value: String): LocalDate? = runCatching { LocalDate.parse(value.trim()) }.getOrNull()
private fun dismissalMessage(result: Result<*>?, empty: String) = when {
    result == null -> "Kayıtlar yükleniyor…"
    result.isFailure -> "Teslim kayıtları yüklenemedi."
    else -> empty
}

@Composable
private fun <T> dismissalLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> =
    key(keys.toList()) {
        produceState<Result<T>?>(null) {
            value = try { Result.success(load()) } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                Result.failure(failure)
            }
        }
    }
