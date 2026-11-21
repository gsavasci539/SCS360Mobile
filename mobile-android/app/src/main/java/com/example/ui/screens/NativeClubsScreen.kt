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
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Composable
internal fun NativeClubsScreen(state: Okul360UiState, onBack: (() -> Unit)? = null, child: String? = null) {
    val user = state.currentUser
    val parent = child != null
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, if (parent) "parent.view_children" else "clubs.read") ||
        user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) {
        Column(Modifier.padding(16.dp)) {
            Text("Kulüp kayıtları için yetkili çalışma alanını açın.")
            onBack?.let { TextButton(onClick = it) { Text("Geri") } }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl, child) {
        val client = remember { NativeClubsClient() }
        ClubsWorkspace(client, state.selectedBranchId.takeUnless { it == "all" }.orEmpty(), child,
            !parent && NativeNavigationPolicy.has(user, "clubs.write"), onBack)
    }
}

@Composable
private fun ClubsWorkspace(client: NativeClubsClient, initialBranch: String, child: String?, canWrite: Boolean, onBack: (() -> Unit)?) {
    var branch by remember { mutableStateOf(initialBranch) }
    var kind by remember { mutableStateOf(ClubRecords.CLUBS) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var choosingBranch by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<JSONObject?>(null) }
    var leaving by remember { mutableStateOf<JSONObject?>(null) }
    var pendingPdf by remember { mutableStateOf<JSONObject?>(null) }
    val rows by clubLoad(kind, branch, page, search, revision) {
        if (search.isNotBlank()) delay(250)
        when {
            child == null -> client.list(kind, branch, page, search.trim())
            kind == ClubRecords.CLUBS -> client.parentActivities(child)
            else -> client.parentCertificates(child, page)
        }
    }
    val branches by clubLoad(canWrite, revision) { if (canWrite) client.options(null).branches else emptyList() }
    val coroutine = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    fun action(refresh: Boolean = true, work: suspend () -> String) {
        if (busy) return
        busy = true
        notice = null
        coroutine.launch {
            try {
                notice = work()
                if (refresh) revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                notice = "Kulüp işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    val pdf = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val row = pendingPdf
        pendingPdf = null
        if (uri != null && row != null) action(false) {
            val bytes = client.certificatePdf(row, child)
            withContext(Dispatchers.IO) {
                client.ensureCurrent()
                requireNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
            }
            "Öğrenci belgesi kaydedildi."
        }
    }
    val items = rows?.getOrNull().orEmpty()
    val paginated = child == null || kind == ClubRecords.CERTIFICATES
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(if (child == null) "Kulüpler ve Etkinlikler" else "Kulüp ve Aktiviteler", style = MaterialTheme.typography.headlineSmall)
            onBack?.let { TextButton(onClick = it, enabled = !busy) { Text("Geri") } }
            notice?.let { Text(it) }
            TextButton(onClick = { revision++ }, enabled = !busy) { Text("Kayıtları yenile") }
            if (canWrite) {
                val name = branches?.getOrNull()?.firstOrNull { it.id == branch }?.name
                TextButton(onClick = { choosingBranch = true }, enabled = !busy) { Text(name ?: "Şube seç") }
            }
        }
        items(ClubRecords.entries.filter { child == null || it == ClubRecords.CLUBS || it == ClubRecords.CERTIFICATES }) { tab ->
            TextButton(enabled = !busy && tab != kind, onClick = { kind = tab; page = 0; search = ""; notice = null }) { Text(tab.title) }
        }
        if (child == null && kind in setOf(ClubRecords.CLUBS, ClubRecords.EVENTS)) item {
            ClubField(search, "Kayıtlarda ara", 100, busy) { search = it; page = 0 }
        }
        if (canWrite) item {
            val label = when (kind) {
                ClubRecords.CLUBS -> "Kulüp oluştur"
                ClubRecords.MEMBERS -> "Öğrenci kaydet"
                ClubRecords.EVENTS -> "Etkinlik oluştur"
                ClubRecords.CERTIFICATES -> "Öğrenci belgesi oluştur"
            }
            Button(enabled = !busy && branch.isNotBlank(), onClick = { target = null; editing = true; notice = null }) { Text(label) }
        }
        if (items.isEmpty()) item { Text(when {
            rows == null -> "Kayıtlar yükleniyor…"
            rows?.isFailure == true -> "Kayıtlar yüklenemedi."
            else -> "Bu kapsamda kayıt bulunamadı."
        }) }
        items(if (paginated) items.take(20) else items, key = { it.getString("id") }) { row ->
            val picture = when (kind) {
                ClubRecords.CLUBS -> if (child == null) listOf(row.clubText("club_name"), row.clubText("branch_name"), row.clubText("advisor_name"),
                    "${row.getInt("current_members")} / ${row.getInt("max_members")} üye", row.clubText("meeting_day"),
                    row.clubText("meeting_time"), row.clubText("meeting_location"), row.clubText("description"))
                else listOf(row.clubText("name"), row.clubText("day"), row.clubText("time"), row.clubText("location"))
                ClubRecords.MEMBERS -> listOf(row.clubText("student_name"), row.clubText("club_name"), row.clubText("joined_date"), row.clubText("notes"))
                ClubRecords.EVENTS -> listOf(row.clubText("event_name"), row.clubText("club_name"), row.clubText("event_date"),
                    row.clubText("event_time"), row.clubText("location"), row.clubText("description"))
                ClubRecords.CERTIFICATES -> listOf(row.clubText("title"), row.clubText("student_name"), row.clubText("issue_date"), row.clubText("description"))
            }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    picture.filter(String::isNotBlank).forEach { Text(it) }
                    if (canWrite && kind == ClubRecords.CLUBS) TextButton(enabled = !busy, onClick = {
                        branch = row.getString("branch_id"); target = row; editing = true; notice = null
                    }) { Text("Kulübü düzenle") }
                    if (canWrite && branch.isNotBlank() && kind == ClubRecords.MEMBERS) TextButton(enabled = !busy, onClick = {
                        leaving = row; notice = null
                    }) { Text("Üyeliği sonlandır") }
                    if (kind == ClubRecords.CERTIFICATES) TextButton(enabled = !busy && pendingPdf == null, onClick = {
                        pendingPdf = row
                        pdf.launch("scs360-belge-${row.getString("id")}.pdf")
                    }) { Text("Belgeyi PDF indir") }
                }
            }
        }
        if (paginated && (page > 0 || items.size > 20)) item {
            Text("Sayfa ${page + 1}")
            if (page > 0) TextButton(enabled = !busy, onClick = { page-- }) { Text("Önceki sayfa") }
            if (items.size > 20) TextButton(enabled = !busy, onClick = { page++ }) { Text("Sonraki sayfa") }
        }
    }
    if (choosingBranch) ConsentFormDialog("Yetkili şube seçin", "native_club_branch", false, { choosingBranch = false }, fields = {
        item { TextButton(onClick = { branch = ""; page = 0; choosingBranch = false }) { Text("Tüm yetkili şubeler") } }
        if (branches == null) item { Text("Şubeler yükleniyor…") }
        if (branches?.isFailure == true) item { TextButton(onClick = { revision++ }) { Text("Şubeleri yeniden yükle") } }
        items(branches?.getOrNull().orEmpty(), key = { it.id }) { row ->
            TextButton(onClick = { branch = row.id; page = 0; choosingBranch = false }) { Text(row.name) }
        }
    }, confirm = {})
    if (editing && canWrite) ClubEditor(client, kind, branch, target, busy, notice, { editing = false }) { body, request ->
        action {
            client.save(kind, branch, body, request, target?.getString("id"))
            editing = false
            page = 0
            "Kulüp kaydı sunucudan doğrulandı."
        }
    }
    leaving?.let { row -> AlertDialog(onDismissRequest = { if (!busy) leaving = null },
        title = { Text("Üyeliği sonlandır") }, text = { Text(notice ?: row.clubText("student_name")) },
        confirmButton = { Button(enabled = !busy, onClick = { action {
            client.leave(row, branch); leaving = null; "Üyeliğin sonlandırıldığı doğrulandı."
        } }) { Text("Sonlandır") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { leaving = null }) { Text("Vazgeç") } }) }
}

@Composable
private fun ClubField(value: String, label: String, max: Int, busy: Boolean, update: (String) -> Unit) {
    OutlinedTextField(value, { update(it.take(max)) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(label) })
}

private data class ClubEditField(val key: String, val label: String, val max: Int, val required: Boolean = false)

@Composable
private fun ClubEditor(client: NativeClubsClient, kind: ClubRecords, branch: String, target: JSONObject?, busy: Boolean,
    notice: String?, close: () -> Unit, save: (JSONObject, UUID) -> Unit) {
    val fields = when (kind) {
        ClubRecords.CLUBS -> listOf(ClubEditField("club_name", "Kulüp adı", 255, true),
            ClubEditField("description", "Açıklama", 1200), ClubEditField("max_members", "Üye kapasitesi", 5, true),
            ClubEditField("meeting_day", "Toplantı günü", 20), ClubEditField("meeting_time", "Saat (SS:DD)", 5),
            ClubEditField("meeting_location", "Toplantı yeri", 255))
        ClubRecords.MEMBERS -> listOf(ClubEditField("notes", "Üyelik notu", 1200))
        ClubRecords.EVENTS -> listOf(ClubEditField("event_name", "Etkinlik adı", 255, true),
            ClubEditField("event_date", "Tarih (YYYY-AA-GG)", 10, true), ClubEditField("event_time", "Saat (SS:DD)", 5),
            ClubEditField("location", "Yer", 255), ClubEditField("description", "Açıklama", 1200),
            ClubEditField("max_participants", "Katılımcı kapasitesi", 5))
        ClubRecords.CERTIFICATES -> listOf(ClubEditField("title", "Belge başlığı", 255, true),
            ClubEditField("certificate_type", "Belge türü", 50, true), ClubEditField("description", "Açıklama", 1200))
    }
    var values by remember { mutableStateOf(fields.associate { field ->
        field.key to (target?.clubText(field.key) ?: if (field.key == "max_members") "30" else "")
    }) }
    var club by remember { mutableStateOf<ClubChoice?>(null) }
    var advisor by remember { mutableStateOf(target?.clubText("advisor_teacher_id").orEmpty()) }
    var student by remember { mutableStateOf<ClubChoice?>(null) }
    var event by remember { mutableStateOf<ClubChoice?>(null) }
    var picking by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var retry by remember { mutableIntStateOf(0) }
    val picker = picking
    val available by clubLoad(picker, search, retry) {
        if (search.isNotBlank()) delay(250)
        when (picker) {
            "club" -> client.list(ClubRecords.CLUBS, branch, 0, search.trim(), limit = 101).map {
                ClubChoice(it.getString("id"), it.getString("club_name"))
            }
            "event" -> client.list(ClubRecords.EVENTS, branch, 0, search.trim(), club?.id.orEmpty(), 101).map {
                ClubChoice(it.getString("id"), it.getString("event_name"), it.getString("event_date"))
            }
            "student" -> client.options(branch, search.trim()).students
            "advisor" -> client.options(branch).teachers
            else -> emptyList()
        }
    }
    fun choose(type: String) { search = ""; picking = type }
    if (picker != null) {
        val choices = available?.getOrNull().orEmpty()
        ConsentFormDialog("Yetkili kaydı seçin", "native_club_choice", false, { picking = null }, fields = {
            if (picker != "advisor") item { ClubField(search, "Ara", 100, false) { search = it } }
            if (picker == "advisor" || picker == "event" || (picker == "club" && kind == ClubRecords.CERTIFICATES)) item {
                TextButton(onClick = {
                    when (picker) {
                        "advisor" -> advisor = ""
                        "event" -> event = null
                        "club" -> { club = null; event = null }
                    }
                    picking = null
                }) { Text("Seçimi kaldır") }
            }
            if (choices.isEmpty()) item {
                when {
                    available == null -> Text("Seçenekler yükleniyor…")
                    available?.isFailure == true -> TextButton(onClick = { retry++ }) { Text("Seçenekleri tekrar yükle") }
                    else -> Text("Erişilebilir kayıt bulunamadı.")
                }
            }
            items(choices, key = { it.id }) { option -> TextButton(onClick = {
                when (picker) {
                    "club" -> { club = option; event = null }
                    "event" -> event = option
                    "student" -> student = option
                    "advisor" -> advisor = option.id
                }
                picking = null
            }) { Text(listOf(option.name, option.number).filter(String::isNotBlank).joinToString(" · ")) } }
            if (choices.size >= 100) item { Text("Daha fazla sonuç için aramayı daraltın.") }
        }, confirm = {})
        return
    }
    val form = values.mapValues { it.value.trim() }
    val body = JSONObject()
    for ((key, value) in form) {
        val field: Any? = if (key in setOf("max_members", "max_participants")) value.toIntOrNull() else value.ifBlank { null }
        body.put(key, field ?: JSONObject.NULL)
    }
    if (kind == ClubRecords.CLUBS) {
        body.put("branch_id", branch)
        body.put("advisor_teacher_id", advisor.ifBlank { null } ?: JSONObject.NULL)
    }
    if (kind == ClubRecords.EVENTS || kind == ClubRecords.MEMBERS) body.put("club_id", club?.id)
    if (kind == ClubRecords.CERTIFICATES || kind == ClubRecords.MEMBERS) body.put("student_id", student?.id)
    if (kind == ClubRecords.CERTIFICATES) {
        body.put("event_id", event?.id ?: JSONObject.NULL)
        body.put("template_id", "scs360")
    }
    val request = remember(body.toString()) { UUID.randomUUID() }
    val dateOk = form["event_date"].isNullOrBlank() || runCatching { LocalDate.parse(form.getValue("event_date")) }.isSuccess
    val time = form["event_time"] ?: form["meeting_time"]
    val timeOk = time.isNullOrBlank() || time.length == 5 && runCatching { LocalTime.parse(time) }.isSuccess
    val memberLimitOk = kind != ClubRecords.CLUBS || (form["max_members"]?.toIntOrNull() ?: 0) in 1..10000
    val participantLimitOk = form["max_participants"].isNullOrBlank() || (form["max_participants"]?.toIntOrNull() ?: 0) > 0
    val choicesOk = when (kind) {
        ClubRecords.CLUBS -> branch.isNotBlank()
        ClubRecords.MEMBERS -> club != null && student != null
        ClubRecords.EVENTS -> club != null
        ClubRecords.CERTIFICATES -> student != null
    }
    ConsentFormDialog(if (target == null) "Yeni kayıt" else "Kulübü düzenle", "native_club_editor", busy, close, fields = {
        if (kind == ClubRecords.CLUBS) item {
            TextButton(enabled = !busy, onClick = { choose("advisor") }) {
                Text(if (advisor.isBlank()) "Danışman seç" else "Danışmanı değiştir")
            }
        }
        if (kind != ClubRecords.CLUBS) item {
            TextButton(enabled = !busy, onClick = { choose("club") }) { Text(club?.name ?: "Kulüp seç") }
        }
        if (kind == ClubRecords.MEMBERS || kind == ClubRecords.CERTIFICATES) item {
            TextButton(enabled = !busy, onClick = { choose("student") }) { Text(student?.name ?: "Öğrenci seç") }
        }
        if (kind == ClubRecords.CERTIFICATES && club != null) item {
            TextButton(enabled = !busy, onClick = { choose("event") }) { Text(event?.name ?: "Etkinlik seç (isteğe bağlı)") }
        }
        items(fields, key = { it.key }) { field ->
            ClubField(values[field.key].orEmpty(), field.label, field.max, busy) { values = values + (field.key to it) }
        }
        notice?.let { item { Text(it) } }
    }, confirm = {
        Button(enabled = !busy && choicesOk && dateOk && timeOk && memberLimitOk && participantLimitOk &&
            fields.filter { it.required }.all { !form[it.key].isNullOrBlank() }, onClick = { save(body, request) }) { Text("Kaydet") }
    })
}

@Composable
private fun <T> clubLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> = key(keys.toList()) {
    produceState<Result<T>?>(null) {
        value = try { Result.success(load()) } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            Result.failure(failure)
        }
    }
}
