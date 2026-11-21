package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

private enum class SecuritySection(val title: String) { GATE("Kapı"), CREDENTIALS("Kimlikler"), AUDIT("Denetim") }

@Composable
internal fun NativeSecurityScreen(state: Okul360UiState, startWithCredentials: Boolean = false,
    onOpenDismissal: (() -> Unit)? = null, onBack: () -> Unit) {
    val user = state.currentUser
    val securityRead = NativeNavigationPolicy.has(user, "security.read")
    val securityWrite = securityRead && NativeNavigationPolicy.has(user, "security.write")
    val auditRead = NativeNavigationPolicy.has(user, "audit.read")
    if (!state.isLoggedIn || (!securityRead && !auditRead) ||
        (user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank())) {
        Text("Öğrenci kimliği ve kapı kayıtları için yetkili kurum çalışma alanını açın.", Modifier.padding(16.dp))
        return
    }
    val sections = buildList {
        if (securityRead) addAll(listOf(SecuritySection.GATE, SecuritySection.CREDENTIALS))
        if (auditRead) add(SecuritySection.AUDIT)
    }
    key(financePresentationScope(state), state.serverBaseUrl, startWithCredentials) {
        val client = remember { NativeSecurityClient(state.selectedBranchId) }
        val dismissal = onOpenDismissal.takeIf { NativeNavigationPolicy.has(user, "dismissal.read") }
        SecurityWorkspace(client, sections, securityWrite, startWithCredentials, dismissal, onBack)
    }
}

@Composable
private fun SecurityWorkspace(client: NativeSecurityClient, sections: List<SecuritySection>, canWrite: Boolean,
    startWithCredentials: Boolean, onOpenDismissal: (() -> Unit)?, onBack: () -> Unit) {
    var section by remember { mutableStateOf(if (startWithCredentials && SecuritySection.CREDENTIALS in sections)
        SecuritySection.CREDENTIALS else sections.first()) }
    var revision by remember { mutableIntStateOf(0) }
    var page by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var token by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("IN") }
    var method by remember { mutableStateOf("QR") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var dayField by remember { mutableStateOf<String?>(null) }
    var customDay by remember { mutableStateOf<LocalDate?>(null) }
    val gateRequest = remember(token, direction, method, location, notes) { UUID.randomUUID() }
    var picking by remember { mutableStateOf(false) }
    var student by remember { mutableStateOf<SecurityStudent?>(null) }
    var hoursField by remember { mutableStateOf("24") }
    val hours = hoursField.toIntOrNull()
    val issueRequest = remember(student, hours) { UUID.randomUUID() }
    var revoking by remember { mutableStateOf<SecurityCredential?>(null) }
    var savingQr by remember { mutableStateOf<SecurityCredential?>(null) }
    val coroutine = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver

    fun work(refresh: Boolean = true, execute: suspend () -> String) {
        if (busy) return
        message = null
        busy = true
        coroutine.launch {
            try {
                message = execute()
                if (refresh) { revision++; page = 0 }
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                message = "Güvenlik işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/svg+xml")) { uri ->
        val record = savingQr
        savingQr = null
        if (uri != null && record != null) work(refresh = false) {
            val bytes = client.download(record)
            withContext(Dispatchers.IO) {
                client.ensureCurrent()
                checkNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
            }
            "QR dosyası kaydedildi."
        }
    }

    val today by securityLoad(section, revision) {
        if (section == SecuritySection.GATE) client.options().today else null
    }
    val day = customDay ?: today?.getOrNull()
    val summary by securityLoad(day, section, revision) {
        if (section == SecuritySection.GATE && day != null) client.totals(day) else null
    }
    val events by securityLoad(day, section, page, revision) {
        if (section == SecuritySection.GATE && day != null) client.events(day, 21, page * 20) else emptyList()
    }
    val cards by securityLoad(section, student?.id, page, revision) {
        if (section == SecuritySection.CREDENTIALS) client.credentials(student?.id, 21, page * 20) else emptyList()
    }
    val audit by securityLoad(section, page, revision) {
        if (section == SecuritySection.AUDIT) client.audit(21, page * 20) else emptyList()
    }
    LazyColumn(Modifier.fillMaxSize().testTag("native_security_records"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Öğrenci Kimliği ve Kapı", style = MaterialTheme.typography.headlineSmall)
            TextButton(enabled = !busy, onClick = onBack) { Text("Geri") }
            if (onOpenDismissal != null) TextButton(enabled = !busy, onClick = onOpenDismissal) { Text("Öğrenci Teslim Masası") }
            TabRow(sections.indexOf(section)) {
                sections.forEach { item -> Tab(item == section, onClick = {
                    if (!busy) { section = item; page = 0; message = null }
                }, text = { Text(item.title) }) }
            }
            message?.let { Text(it) }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Kayıtları yenile") }
        }
        when (section) {
            SecuritySection.GATE -> {
                item {
                    summary?.getOrNull()?.let {
                        Text("Giriş ${it.entries} · Çıkış ${it.exits} · İçeride ${it.inside} · İnceleme ${it.review}")
                    }
                    if (today?.isFailure == true || summary?.isFailure == true) Text("Kapı özeti yüklenemedi.")
                    SecurityField(dayField ?: day?.toString().orEmpty(), "Kayıt tarihi (YYYY-AA-GG)", 10, busy) { dayField = it }
                    TextButton(enabled = !busy && securityDay(dayField.orEmpty()) != null, onClick = {
                        customDay = securityDay(dayField.orEmpty()); page = 0
                    }) { Text("Tarihi uygula") }
                }
                if (canWrite) item {
                    Text("Geçiş doğrulaması", style = MaterialTheme.typography.titleMedium)
                    listOf("IN" to "Giriş", "OUT" to "Çıkış").forEach { (value, label) ->
                        FilterChip(direction == value, { direction = value }, { Text(label) }, enabled = !busy)
                    }
                    listOf("QR" to "QR", "BARCODE" to "Barkod", "MANUAL" to "Elle giriş").forEach { (value, label) ->
                        FilterChip(method == value, { method = value }, { Text(label) }, enabled = !busy)
                    }
                    SecurityField(token, "Kimlik kodu", 255, busy, masked = true) { token = it }
                    SecurityField(location, "Kapı konumu", 100, busy) { location = it }
                    SecurityField(notes, "Not", 1000, busy) { notes = it }
                    Button(enabled = !busy && token.isNotBlank(), onClick = { work {
                        val record = client.verify(token.trim(), direction, method, location.trim(), notes.trim(), gateRequest)
                        if (record == null) "Bu kapıda doğrulanabilir öğrenci kimliği bulunamadı."
                        else {
                            token = ""
                            notes = ""
                            "Geçiş kaydı doğrulandı. ${record.person} · ${record.time}"
                        }
                    } }) { Text("Geçişi doğrula ve kaydet") }
                }
                val rows = events?.getOrNull().orEmpty()
                if (rows.isEmpty()) item { Text(securityMessage(events, "Seçili günde kapı kaydı yok.")) }
                items(rows.take(20), key = { it.id }) { row ->
                    SecurityCard(listOf(row.person, if (row.direction == "IN") "Giriş" else "Çıkış", row.status,
                        row.location, row.method, row.operator, row.time, row.notes))
                }
            }
            SecuritySection.CREDENTIALS -> {
                item {
                    TextButton(enabled = !busy, onClick = { picking = true }) { Text(student?.name ?: "Tüm öğrenciler · Öğrenci seç") }
                    if (student != null) TextButton(enabled = !busy, onClick = { student = null; page = 0 }) { Text("Tüm öğrencileri göster") }
                    val selected = student
                    if (canWrite && selected?.active == true) {
                        SecurityField(hoursField, "Geçerlilik süresi (saat)", 4, busy) { hoursField = it.filter(Char::isDigit) }
                        Button(enabled = !busy && hours != null && hours in 1..8760, onClick = { work {
                            client.issue(selected.id, requireNotNull(hours), issueRequest)
                            student = null
                            "Öğrenci kimliği sunucudan doğrulandı."
                        } }) { Text("Öğrenci kimliği oluştur") }
                    }
                }
                val rows = cards?.getOrNull().orEmpty()
                if (rows.isEmpty()) item { Text(securityMessage(cards, "Bu kapsamda öğrenci kimliği yok.")) }
                items(rows.take(20), key = { it.id }) { record ->
                    SecurityCard(listOf(record.student, record.status, record.expiresAt, record.barcode)) {
                        if (record.status == "ACTIVE") {
                            TextButton(enabled = !busy && savingQr == null, onClick = {
                                savingQr = record
                                filePicker.launch("SCS360-kimlik-${record.studentId}.svg")
                            }) { Text("QR kodunu indir") }
                        }
                        if (canWrite && record.status in setOf("ACTIVE", "EXPIRED", "BLOCKED")) {
                            TextButton(enabled = !busy, onClick = { revoking = record; message = null }) { Text("Kimliği iptal et") }
                        }
                    }
                }
            }
            SecuritySection.AUDIT -> {
                val rows = audit?.getOrNull().orEmpty()
                if (rows.isEmpty()) item { Text(securityMessage(audit, "Bu kapsamda denetim kaydı yok.")) }
                items(rows.take(20), key = { it.id }) { row -> SecurityCard(listOf(row.entity, row.action, row.actor, row.time)) }
            }
        }
        val size = when (section) {
            SecuritySection.GATE -> events?.getOrNull()?.size
            SecuritySection.CREDENTIALS -> cards?.getOrNull()?.size
            SecuritySection.AUDIT -> audit?.getOrNull()?.size
        } ?: 0
        if (page > 0 || size > 20) item {
            Text("Sayfa ${page + 1}")
            if (page > 0) TextButton(enabled = !busy, onClick = { page-- }) { Text("Önceki sayfa") }
            if (size > 20) TextButton(enabled = !busy, onClick = { page++ }) { Text("Sonraki sayfa") }
        }
    }

    if (picking) {
        var search by remember { mutableStateOf("") }
        val options by securityLoad(search) {
            if (search.isNotBlank()) delay(250)
            client.options(search.trim(), student?.id)
        }
        ConsentFormDialog("Öğrenci seç", "native_security_students", false, { picking = false }, fields = {
            item { SecurityField(search, "Öğrenci ara", 100, false) { search = it } }
            val result = options?.getOrNull()
            if (result == null || result.students.isEmpty()) item { Text(securityMessage(options, "Öğrenci bulunamadı.")) }
            if (result != null) {
                items(result.students, key = { it.id }) { choice ->
                    TextButton(onClick = { student = choice; picking = false; page = 0 }) { Text(choice.name) }
                }
                if (result.more) item { Text("Daha fazla sonuç için aramayı daraltın.") }
            }
        }, confirm = {})
    }
    revoking?.let { credential ->
        AlertDialog({ if (!busy) revoking = null }, title = { Text("Öğrenci kimliğini iptal et") },
            text = { Text(message ?: credential.student) }, confirmButton = {
                Button(enabled = !busy && canWrite, onClick = { work {
                    client.revoke(credential); revoking = null; "Kimliğin iptali doğrulandı."
                } }) { Text("İptali onayla") }
            }, dismissButton = { TextButton(enabled = !busy, onClick = { revoking = null; message = null }) { Text("Geri dön") } })
    }
}

@Composable
private fun SecurityCard(lines: List<String>, actions: @Composable () -> Unit = {}) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lines.filter(String::isNotBlank).forEach { Text(it) }
            actions()
        }
    }
}

@Composable
private fun SecurityField(value: String, label: String, max: Int, busy: Boolean, masked: Boolean = false, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(max)) }, enabled = !busy, modifier = Modifier.fillMaxWidth(), label = { Text(label) },
        singleLine = max < 1000, visualTransformation = if (masked) PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None)
}

private fun securityDay(value: String) = runCatching { LocalDate.parse(value.trim()) }.getOrNull()
private fun securityMessage(result: Result<*>?, empty: String) = when {
    result == null -> "Güvenlik kayıtları yükleniyor…"
    result.isFailure -> "Güvenlik kayıtları yüklenemedi."
    else -> empty
}

@Composable
private fun <T> securityLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> =
    key(keys.toList()) {
        produceState<Result<T>?>(null) {
            value = try { Result.success(load()) } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                Result.failure(failure)
            }
        }
    }
