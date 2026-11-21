package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.remote.InstitutionAccount
import com.example.data.remote.NativeInstitutionRoleClient
import com.example.data.remote.NativeInstitutionUsersClient
import com.example.data.remote.RegistryHttpException
import com.example.data.remote.UserAccess
import com.example.data.remote.UserAccessChoice
import com.example.data.remote.UserAccessOptions
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private val accountStatuses = mapOf("ACTIVE" to "Aktif", "PASSIVE" to "Pasif", "BLOCKED" to "Engelli")

@Composable
fun UsersScreen(state: Okul360UiState, onBack: () -> Unit, onOpenParents: (() -> Unit)? = null) {
    val actor = state.currentUser
    val organization = if (actor.is_super_admin == true) state.workspaceOrganizationId else actor.organization_id
    if (!state.isLoggedIn || organization.isNullOrBlank() || !NativeNavigationPolicy.has(actor, "users.read")) {
        Column(Modifier.padding(16.dp)) {
            Text("Kullanıcı yönetimi için yetkili kurum çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    val write = NativeNavigationPolicy.has(actor, "users.write")
    val parentLink = onOpenParents?.takeIf { NativeNavigationPolicy.has(actor, "parents.read", "parent.read") }
    key(financePresentationScope(state), state.serverBaseUrl) {
        val client = remember { NativeInstitutionUsersClient(organization, state.selectedBranchId) }
        val catalog = if (NativeNavigationPolicy.canCompleteRoleCatalogue(actor, state.workspaceOrganizationId))
            remember { NativeInstitutionRoleClient(organization) } else null
        val selectedBranch = state.selectedBranchId.takeUnless { it.isBlank() || it == "all" }
        NativeUserWorkspace(client, catalog, selectedBranch, write, onBack, parentLink)
    }
}

@Composable
private fun NativeUserWorkspace(
    client: NativeInstitutionUsersClient, catalog: NativeInstitutionRoleClient?, selectedBranch: String?,
    canWrite: Boolean, onBack: () -> Unit, onOpenParents: (() -> Unit)?
) {
    var search by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf(false) }
    var account by remember { mutableStateOf<InstitutionAccount?>(null) }
    var assigning by remember { mutableStateOf<InstitutionAccount?>(null) }
    var removing by remember { mutableStateOf<InstitutionAccount?>(null) }
    val coroutine = rememberCoroutineScope()
    val roster by key(page, search, revision) { produceState<Result<Pair<List<InstitutionAccount>, Boolean>>?>(null) {
        value = userResult {
            if (search.isNotBlank()) delay(250)
            client.list(page, search)
        }
    } }

    fun action(operation: suspend () -> String, showError: (String) -> Unit = { message = it }) {
        if (busy) return
        busy = true
        message = null
        coroutine.launch {
            try {
                message = operation()
                revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                showError(if (failure is RegistryHttpException && failure.code == 409)
                    "Kayıt değişti veya bu e-posta kullanılıyor. Güncel kaydı açın." else "Sunucu işlemi doğrulanamadı.")
            } finally { busy = false }
        }
    }

    val rows = roster?.getOrNull()?.first.orEmpty()
    LazyColumn(Modifier.fillMaxSize().testTag("institution_users_list"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Kullanıcılar ve erişim", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack, enabled = !busy) { Text("Geri") }
            message?.let { Text(it) }
            if (canWrite) Button(onClick = { account = null; editing = true }, enabled = !busy,
                modifier = Modifier.testTag("add_user_fab")) { Text("Yeni kullanıcı") }
            if (onOpenParents != null) OutlinedButton(onClick = onOpenParents, enabled = !busy) { Text("Veli ve giriş hesabı yönetimi") }
            if (catalog != null) OutlinedButton(onClick = {
                action(operation = {
                    val count = catalog.completeCatalogue()
                    if (count == 0) "Standart kurum rolleri doğrulandı." else "$count standart kurum rolü eklendi ve doğrulandı."
                })
            }, enabled = !busy, modifier = Modifier.testTag("ensure_standard_roles_btn")) { Text("Eksik standart rolleri ekle") }
            AccountField(search, { search = it; page = 1 }, "Ad veya e-posta ile ara", 100, enabled = !busy)
            TextButton(onClick = { revision++ }, enabled = !busy) { Text("Listeyi yenile") }
            if (roster == null) Text("Kullanıcılar yükleniyor…")
            else if (roster?.isFailure == true) Text("Kullanıcılar alınamadı.")
            else if (rows.isEmpty()) Text("Bu kapsamda kullanıcı bulunamadı.")
        }
        items(rows, key = { it.id }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium)
                    Text(row.email)
                    Text(accountStatuses[row.status] ?: row.status)
                    if (row.roleNames.isNotEmpty()) Text(row.roleNames.joinToString(", "))
                    if (canWrite) {
                        TextButton(onClick = { assigning = row }, enabled = !busy) { Text("Rol ve şube erişimi") }
                        TextButton(onClick = { account = row; editing = true }, enabled = !busy) { Text("Kullanıcıyı düzenle") }
                        TextButton(onClick = { removing = row; message = null }, enabled = !busy) { Text("Hesabı pasife al") }
                    }
                }
            }
        }
        if (page > 1 || roster?.getOrNull()?.second == true) item {
            if (page > 1) TextButton(onClick = { page-- }, enabled = !busy) { Text("Önceki sayfa") }
            if (roster?.getOrNull()?.second == true) TextButton(onClick = { page++ }, enabled = !busy) { Text("Sonraki sayfa") }
        }
    }
    if (editing) key(account?.id) {
        AccountEditor(client, account, selectedBranch, busy, onClose = { editing = false }) { form, showError ->
            action({
                val isCreating = account == null
                val stored = client.save(form, account?.id)
                editing = false
                if (isCreating) { assigning = stored; page = 1 }
                if (form.has("password") && !isCreating) "Kullanıcı kaydı doğrulandı ve parola güncellemesi onaylandı."
                else "Kullanıcı kaydı doğrulandı."
            }, showError)
        }
    }
    assigning?.let { target ->
        key(target.id) {
            AccountAccessEditor(client, target, busy, onClose = { assigning = null }) { baseline, desired, showError ->
                action({
                    client.replaceAccess(target.id, baseline, desired)
                    assigning = null
                    "Rol ve şube erişimi doğrulandı."
                }, showError)
            }
        }
    }
    removing?.let { target ->
        AlertDialog(onDismissRequest = { if (!busy) removing = null }, title = { Text("Hesabı pasife al") },
            text = { Text(message ?: target.name) },
            confirmButton = {
                Button(enabled = !busy, onClick = {
                    action(operation = {
                        client.deactivate(target.id)
                        removing = null
                        "Kullanıcı hesabı pasife alındı."
                    })
                }) { Text("Pasife al") }
            }, dismissButton = { TextButton(onClick = { removing = null }, enabled = !busy) { Text("Vazgeç") } })
    }
}

@Composable
private fun AccountEditor(client: NativeInstitutionUsersClient, existing: InstitutionAccount?, preferredBranch: String?, busy: Boolean,
    onClose: () -> Unit, onSave: (JSONObject, (String) -> Unit) -> Unit
) {
    var firstName by remember { mutableStateOf(existing?.firstName.orEmpty()) }
    var lastName by remember { mutableStateOf(existing?.lastName.orEmpty()) }
    var email by remember { mutableStateOf(existing?.email.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var position by remember { mutableStateOf(existing?.position.orEmpty()) }
    var department by remember { mutableStateOf(existing?.department.orEmpty()) }
    var status by remember { mutableStateOf(existing?.status ?: "ACTIVE") }
    var password by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val options by key(retry) { produceState<Result<UserAccessOptions>?>(null) {
        if (existing == null) {
            value = userResult { client.options() }
            val ready = value?.getOrNull()
            if (branch == null) branch = ready?.branches?.firstOrNull { it.id == preferredBranch }?.id
                ?: ready?.takeUnless { it.allowAllBranches }?.branches?.singleOrNull()?.id
        }
    } }
    val valid = firstName.isNotBlank() && lastName.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() &&
        (existing != null && password.isEmpty() || password.length >= 8) &&
        (existing != null || options?.getOrNull()?.let { it.allowAllBranches || branch != null } == true)
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text(if (existing == null) "Yeni kullanıcı" else "Kullanıcı bilgileri") },
        text = { Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountField(firstName, { firstName = it }, "Ad", 100, !busy)
            AccountField(lastName, { lastName = it }, "Soyad", 100, !busy)
            AccountField(email, { email = it }, "E-posta", 255, !busy)
            AccountField(phone, { phone = it }, "Telefon", 20, !busy)
            AccountField(position, { position = it }, "Pozisyon", 100, !busy)
            AccountField(department, { department = it }, "Birim", 100, !busy)
            AccountField(password, { password = it }, if (existing == null) "Başlangıç parolası" else "Yeni parola (isteğe bağlı)", 4096, !busy, secret = true)
            if (password.isNotEmpty() && password.length < 8) Text("Parola en az 8 karakter olmalı.")
            if (existing != null) accountStatuses.forEach { (code, label) ->
                FilterChip(selected = status == code, onClick = { status = code }, enabled = !busy, label = { Text(label) })
            } else {
                val available = options?.getOrNull()
                if (options == null) Text("Şube seçenekleri yükleniyor…")
                if (options?.isFailure == true) TextButton(onClick = { retry++ }) { Text("Şubeleri yeniden yükle") }
                available?.let {
                    Text("Başlangıç şubesi")
                    if (it.allowAllBranches) FilterChip(selected = branch == null, onClick = { branch = null }, enabled = !busy,
                        label = { Text("Kurum geneli") })
                    it.branches.forEach { choice ->
                        FilterChip(selected = branch == choice.id, onClick = { branch = choice.id }, enabled = !busy,
                            label = { Text(choice.name) })
                    }
                }
            }
            error?.let { Text(it) }
        } },
        confirmButton = {
            Button(enabled = valid && !busy, onClick = {
                error = null
                val form = JSONObject().put("first_name", firstName.trim()).put("last_name", lastName.trim())
                    .put("email", email.trim().lowercase(Locale.ROOT)).put("phone", phone.trim().ifBlank { null } ?: JSONObject.NULL)
                    .put("position", position.trim().ifBlank { null } ?: JSONObject.NULL)
                    .put("department", department.trim().ifBlank { null } ?: JSONObject.NULL)
                if (password.isNotEmpty()) form.put("password", password)
                if (existing != null) form.put("status", status) else branch?.let { form.put("branch_id", it) }
                onSave(form) { error = it }
            }) { Text("Kaydet") }
        }, dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("Vazgeç") } })
}

@Composable
private fun AccountAccessEditor(client: NativeInstitutionUsersClient, target: InstitutionAccount, busy: Boolean, onClose: () -> Unit,
    onSave: (UserAccess, UserAccess, (String) -> Unit) -> Unit
) {
    var retry by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf<UserAccess?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val result by key(retry) { produceState<Result<Pair<UserAccess, UserAccessOptions>>?>(null) {
        draft = null
        value = userResult {
            client.read(target.id)
            client.access(target.id) to client.options()
        }
        draft = value?.getOrNull()?.first
    } }
    val loaded = result?.getOrNull()
    val edited = draft
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text("Rol ve şube erişimi · ${target.name}") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (result == null) Text("Rol ve şube kayıtları yükleniyor…")
                if (result?.isFailure == true) Text("Rol ve şube kayıtları alınamadı.")
                error?.let { Text(it) }
                TextButton(onClick = { error = null; retry++ }, enabled = !busy) { Text("Güncel erişimi yeniden yükle") }
                if (loaded != null && edited != null) {
                    Text("Roller", style = MaterialTheme.typography.titleSmall)
                    loaded.second.roles.forEach { choice ->
                        AccountAccessToggle(choice, choice.id in edited.roleIds, busy) { checked ->
                            draft = edited.copy(roleIds = if (checked) edited.roleIds + choice.id else edited.roleIds - choice.id)
                        }
                    }
                    Text("Şubeler", style = MaterialTheme.typography.titleSmall)
                    loaded.second.branches.forEach { choice ->
                        AccountAccessToggle(choice, choice.id in edited.branchIds, busy) { checked ->
                            draft = edited.copy(branchIds = if (checked) edited.branchIds + choice.id else edited.branchIds - choice.id)
                        }
                    }
                    if (!loaded.second.allowAllBranches) Text("En az bir yetkili şube seçin.")
                }
            }
        },
        confirmButton = {
            val ready = loaded != null && edited != null && edited != loaded.first &&
                (loaded.second.allowAllBranches || edited.branchIds.isNotEmpty())
            Button(enabled = ready && !busy, modifier = Modifier.testTag("save_user_access_btn"), onClick = {
                if (loaded != null && edited != null) {
                    error = null
                    onSave(loaded.first, edited) { error = it }
                }
            }) { Text("Erişimi kaydet") }
        },
        dismissButton = { TextButton(onClick = onClose, enabled = !busy) { Text("Vazgeç") } })
}

@Composable
private fun AccountAccessToggle(choice: UserAccessChoice, selected: Boolean, busy: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Checkbox(checked = selected, enabled = !busy && choice.assignable, onCheckedChange = onChange)
        Text(choice.name, modifier = Modifier.weight(1f).padding(top = 10.dp))
    }
}

@Composable
private fun AccountField(value: String, change: (String) -> Unit, label: String, max: Int, enabled: Boolean, secret: Boolean = false) {
    OutlinedTextField(value, { change(it.take(max)) }, Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(label) }, singleLine = true,
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None)
}

private suspend fun <T> userResult(work: suspend () -> T): Result<T> = try {
    Result.success(work())
} catch (failure: Exception) {
    if (failure is CancellationException) throw failure
    Result.failure(failure)
}
