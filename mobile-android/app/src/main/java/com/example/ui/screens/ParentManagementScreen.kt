package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Student
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable
fun ParentManagementScreen(state: Okul360UiState) {
    val presentation = financePresentationScope(state)
    key(presentation) { ParentManagementContent(state) }
}

@Composable
private fun ParentManagementContent(state: Okul360UiState) {
    val organization = state.workspaceOrganizationId ?: state.currentUser.organization_id ?: state.currentUser.tenant_id.orEmpty()
    val client = remember { runCatching { NativeParentClient(organization) } }
    val captured = remember { ApiClientProvider.interceptor.captureScope() }
    val coroutine = rememberCoroutineScope()
    val canWrite = NativeNavigationPolicy.has(state.currentUser, "parents.write")
    val canAccounts = canWrite && NativeNavigationPolicy.has(state.currentUser, "users.write")
    val canStudents = NativeNavigationPolicy.has(state.currentUser, "student.write", "students.write") && NativeNavigationPolicy.has(state.currentUser, "student.read", "students.read")
    var rows by remember { mutableStateOf<List<NativeParent>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf<NativeParent?>(null) }
    var form by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<NativeParent?>(null) }
    var linking by remember { mutableStateOf<NativeParent?>(null) }
    var uncertain by remember { mutableStateOf(false) }
    fun run(action: suspend (NativeParentClient) -> Unit) {
        if (busy || !captured.current()) return
        busy = true; error = ""
        coroutine.launch {
            try { action(client.getOrThrow()); captured.ensureCurrent() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (captured.current()) error = failure.message ?: "İşlem doğrulanamadı; listeyi yenileyin" }
            finally { if (captured.current()) busy = false }
        }
    }
    LaunchedEffect(revision) { run { rows = it.list(query) } }
    if (!captured.current()) return
    if (!NativeNavigationPolicy.has(state.currentUser, "parents.read")) { Text("Veli listesini görme yetkiniz yok."); return }
    LazyColumn(Modifier.fillMaxSize().testTag("parent_management"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Veliler", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(query, { query = it }, label = { Text("Ad, soyad veya telefon ara") }, modifier = Modifier.fillMaxWidth().testTag("parents_search")) }
        item { Button(enabled = !busy, onClick = { uncertain = false; revision++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ara / listeyi yenile") } }
        item { if (error.isNotBlank()) Text(error, Modifier.testTag("parents_error")) }
        item { if (uncertain) Text("Oluşturma sonucu belirsiz. Yeni kayıt açmadan önce listeyi yenileyip kaydı kontrol edin.") }
        item { Button(enabled = canWrite && !busy && !uncertain, onClick = { editing = null; form = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("parent_new")) { Text("Veli ekle") } }
        if (busy) item { CircularProgressIndicator() }
        if (!busy && rows.isEmpty() && error.isBlank()) item { Text("Veli kaydı bulunamadı.") }
        items(rows, key = { it.id }) { parent ->
            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(parent.name, style = MaterialTheme.typography.titleMedium)
                Text(listOf(parent.value("relation_type"), parent.value("phone"), parent.value("email")).filter { it.isNotBlank() }.joinToString(" · "))
                Text(if (parent.value("user_id").isBlank()) "Giriş hesabı bağlı değil" else "Giriş hesabı: ${parent.value("user_id")}")
                TextButton(enabled = canWrite && !busy, onClick = { run { editing = it.get(parent.id); form = true } }) { Text("Düzenle") }
                TextButton(enabled = (canAccounts || canStudents) && !busy, onClick = { linking = parent }) { Text("Hesap / öğrenci bağları") }
                TextButton(enabled = canWrite && !busy, onClick = { deleting = parent }) { Text("Sil") }
            } }
        }
    }
    if (form) ParentContactForm(editing, busy, error, onDismiss = { if (!busy) form = false }, onSave = { body ->
        if (canWrite && !busy) run { api ->
            try { api.save(body, editing); form = false; revision++ }
            catch (failure: Exception) { if (editing == null && failure !is IllegalArgumentException && (failure as? RegistryHttpException)?.code != 422) uncertain = true; throw failure }
        }
    }, canSave = canWrite && !(editing == null && uncertain))
    deleting?.let { parent -> AlertDialog(onDismissRequest = { if (!busy) deleting = null }, title = { Text("Veliyi sil") }, text = { Text("${parent.name} kaydı pasife alınacak. ${error}") },
        confirmButton = { TextButton(enabled = !busy && canWrite, onClick = { run { it.delete(parent.id); deleting = null; revision++ } }) { Text("Sil") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null }) { Text("Vazgeç") } }) }
    linking?.let { parent -> ParentLinksDialog(parent, client.getOrThrow(), canAccounts, canStudents,
        onDismiss = { linking = null; revision++ }) }
}

@Composable
private fun ParentContactForm(original: NativeParent?, busy: Boolean, error: String, onDismiss: () -> Unit, onSave: (JSONObject) -> Unit, canSave: Boolean) {
    val labels = linkedMapOf("first_name" to "Ad", "last_name" to "Soyad", "relation_type" to "Yakınlık (MOTHER/FATHER/GUARDIAN/OTHER)", "phone" to "Telefon", "sms_phone" to "SMS telefonu", "whatsapp_phone" to "WhatsApp telefonu", "email" to "E-posta", "address" to "Adres")
    val values = remember(original?.id) { mutableStateMapOf<String, String>().apply { labels.keys.forEach { put(it, original?.value(it) ?: if (it == "relation_type") "GUARDIAN" else "") } } }
    var notifications by remember(original?.id) { mutableStateOf(original?.let { JSONObject(it.fields).optBoolean("receive_notifications", true) } ?: true) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp).imePadding(), shape = MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("parent_form"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(if (original == null) "Veli ekle" else "Veliyi düzenle", style = MaterialTheme.typography.titleLarge) }
                items(labels.keys.toList()) { key -> OutlinedTextField(values[key].orEmpty(), { values[key] = it }, enabled = !busy, label = { Text(labels.getValue(key)) }, modifier = Modifier.fillMaxWidth().testTag("parent_$key")) }
                item { Row { Checkbox(notifications, { notifications = it }, enabled = !busy); Text("Bildirimleri al") } }
                item { if (error.isNotBlank()) Text(error) }
                item { Button(enabled = canSave && !busy, modifier = Modifier.heightIn(min = 48.dp).testTag("parent_save"), onClick = { onSave(JSONObject().apply { values.forEach { (key, value) -> put(key, value) }; put("receive_notifications", notifications) }) }) { Text("Kaydet") } }
                item { TextButton(enabled = !busy, onClick = onDismiss) { Text("Kapat") } }
            }
        }
    }
}

@Composable
private fun ParentLinksDialog(parent: NativeParent, client: NativeParentClient, canAccounts: Boolean, canStudents: Boolean, onDismiss: () -> Unit) {
    val coroutine = rememberCoroutineScope()
    var accounts by remember { mutableStateOf<List<RegistryChoice>>(emptyList()) }
    var students by remember { mutableStateOf<List<Student>>(emptyList()) }
    var selected by remember { mutableStateOf<Student?>(null) }
    var linked by remember { mutableStateOf(false) }
    var primary by remember { mutableStateOf(false) }
    var emergency by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }
    fun run(action: suspend () -> Unit) {
        if (busy || !client.current()) return
        busy = true; error = ""; info = ""
        coroutine.launch { try { action() } catch (cancelled: CancellationException) { throw cancelled } catch (failure: Exception) { if (client.current()) error = failure.message ?: "Bağlantı doğrulanamadı" } finally { if (client.current()) busy = false } }
    }
    LaunchedEffect(parent.id) { run { if (canAccounts) accounts = client.accounts(parent.id); if (canStudents) students = NativeRegistryClient().students(null, null, null) } }
    if (!client.current()) return
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp).imePadding()) {
            LazyColumn(Modifier.padding(16.dp).testTag("parent_links"), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(parent.name, style = MaterialTheme.typography.titleLarge) }
                item { if (error.isNotBlank()) Text(error); if (info.isNotBlank()) Text(info) }
                if (canAccounts) {
                    item { Text("Uygun veli giriş hesapları") }
                    items(accounts, key = { "account_${it.id}" }) { account -> TextButton(enabled = !busy, onClick = { run { client.linkAccount(parent.id, account.id); info = "Giriş hesabı sunucuda bağlandı" } }) { Text(account.name) } }
                    item { TextButton(enabled = !busy, onClick = { run { client.linkAccount(parent.id, null); info = "Giriş hesabı bağlantısı kaldırıldı" } }) { Text("Giriş hesabı bağlantısını kaldır") } }
                }
                if (canStudents) {
                    item { Text("Öğrenci seçin") }
                    items(students, key = { "student_${it.id}" }) { student -> TextButton(enabled = !busy, onClick = { run { selected = student; linked = parent.id in client.studentParents(student.id) } }) { Text(student.full_name) } }
                    selected?.let { student ->
                        item { Text("${student.full_name}: ${if (linked) "Bağlı" else "Bağlı değil"}") }
                        if (!linked) {
                            item { Row { Checkbox(primary, { primary = it }); Text("Birincil veli") } }
                            item { Row { Checkbox(emergency, { emergency = it }); Text("Acil durumda aranacak") } }
                        }
                        item { Button(enabled = !busy, onClick = { run { if (linked) client.unlinkStudent(student.id, parent.id) else client.linkStudent(student.id, parent.id, primary, emergency); linked = parent.id in client.studentParents(student.id); info = "Öğrenci ilişkisi sunucudan yeniden okundu" } }) { Text(if (linked) "Öğrenci bağını kaldır" else "Öğrenciye bağla") } }
                    }
                }
                item { TextButton(enabled = !busy, onClick = onDismiss) { Text("Kapat / listeyi yenile") } }
            }
        }
    }
}
