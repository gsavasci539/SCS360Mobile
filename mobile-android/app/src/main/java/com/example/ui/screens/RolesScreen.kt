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
import androidx.compose.ui.unit.dp
import com.example.data.remote.InstitutionRole
import com.example.data.remote.InstitutionPermission
import com.example.data.remote.NativeInstitutionRoleClient
import com.example.data.remote.RegistryHttpException
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun RolesScreen(state: Okul360UiState, onBack: () -> Unit) {
    val user = state.currentUser
    val organization = if (user.is_super_admin == true) state.workspaceOrganizationId else user.organization_id
    if (!state.isLoggedIn || organization.isNullOrBlank() || !NativeNavigationPolicy.has(user, "users.read", "role.read", "role.write")) {
        Column(Modifier.padding(16.dp)) {
            Text("Rol yönetimi için yetkili kurum çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        InstitutionRoles(remember { NativeInstitutionRoleClient(organization) }, organization,
            NativeNavigationPolicy.has(user, "users.write", "role.write"), onBack)
    }
}

private data class RoleMatrix(val saved: Set<String>, val draft: Set<String>)

@Composable
private fun InstitutionRoles(client: NativeInstitutionRoleClient, organization: String, canWrite: Boolean, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<InstitutionRole?>(null) }
    var openRoles by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var matrixRevision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    val catalog by key(revision) { produceState<Result<Pair<List<InstitutionRole>, List<InstitutionPermission>>>?>(null) {
        value = roleResult { client.roles() to client.permissions() }
        if (selected == null) selected = value?.getOrNull()?.first?.firstOrNull()
    } }
    val target = selected
    val editable = canWrite && target != null && target.organization == organization && !target.system
    val coroutine = rememberCoroutineScope()
    var matrix by remember(target?.id, matrixRevision) { mutableStateOf<Result<RoleMatrix>?>(null) }
    LaunchedEffect(target?.id, matrixRevision) {
        matrix = if (target == null) null else roleResult {
            val assigned = client.assigned(target.id)
            RoleMatrix(assigned, assigned)
        }
    }
    val edit = matrix?.getOrNull()
    LazyColumn(Modifier.fillMaxSize().testTag("institution_roles"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Rol ve yetkiler", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack, enabled = !busy) { Text("Geri") }
            if (canWrite) TextButton(onClick = { creating = true; message = null }, enabled = !busy) { Text("Rol ekle") }
            if (catalog?.isFailure == true) {
                Text("Kurum rol kataloğu yüklenemedi.")
                TextButton(onClick = { revision++ }) { Text("Kataloğu yeniden yükle") }
            }
            if (catalog == null) Text("Kurum rol kataloğu yükleniyor…")
            if (catalog?.getOrNull()?.first?.isEmpty() == true) Text("Kurum rolü bulunamadı.")
            if (!catalog?.getOrNull()?.first.isNullOrEmpty()) {
                TextButton(onClick = { openRoles = true }, enabled = !busy) { Text(target?.name ?: "Rol seç") }
            }
            target?.description?.let { Text(it) }
            if (target?.system == true) Text("Sistem rolü salt okunur.")
            message?.let { Text(it) }
            if (target != null && matrix == null) Text("Rol izinleri yükleniyor…")
            if (matrix?.isFailure == true) Text("Rol izinleri alınamadı.")
            if (target != null) TextButton(onClick = { matrixRevision++; message = null }, enabled = !busy) {
                Text("Güncel izinleri yeniden yükle")
            }
            if (editable && target != null && edit != null) {
                Button(enabled = !busy && edit.saved != edit.draft, onClick = {
                    busy = true
                    message = null
                    coroutine.launch {
                        try {
                            val stored = client.replace(target, edit.saved, edit.draft)
                            matrix = Result.success(RoleMatrix(stored, stored))
                            message = "Rol izinleri doğrulandı."
                        } catch (failure: Exception) {
                            if (failure is CancellationException) throw failure
                            message = if (failure is RegistryHttpException && failure.code == 409)
                                "Rol izinleri değişti. Güncel izinleri yeniden yükleyin." else "Rol izinleri doğrulanamadı."
                        } finally { busy = false }
                    }
                }) { Text("Matrisi kaydet") }
            }
            if (edit != null) OutlinedTextField(value = search, onValueChange = { search = it.take(100) },
                modifier = Modifier.fillMaxWidth(), label = { Text("İzinlerde ara") }, singleLine = true)
        }
        if (edit != null) {
            val permissions = catalog?.getOrNull()?.second.orEmpty().filter {
                search.isBlank() || it.name.contains(search.trim(), ignoreCase = true) || it.code.contains(search.trim(), ignoreCase = true)
            }
            items(permissions, key = { it.id }) { permission ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Checkbox(checked = permission.id in edit.draft, enabled = editable && !busy && permission.assignable,
                        onCheckedChange = { checked ->
                            val next = if (checked) edit.draft + permission.id else edit.draft - permission.id
                            matrix = Result.success(edit.copy(draft = next))
                        })
                    Column(Modifier.weight(1f).padding(top = 8.dp)) {
                        Text(permission.name)
                        Text(permission.code, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
    if (openRoles) AlertDialog(onDismissRequest = { openRoles = false }, title = { Text("Kurum rolü seçin") }, text = {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 340.dp)) {
            items(catalog?.getOrNull()?.first.orEmpty(), key = { it.id }) { item ->
                TextButton(onClick = { selected = item; openRoles = false; search = ""; message = null }) {
                    Text("${item.name} · ${item.slug}")
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { openRoles = false }) { Text("Kapat") } })
    if (creating) NewInstitutionRole(busy, onClose = { creating = false }) { name, slug, description, requestId, reportError ->
        busy = true
        coroutine.launch {
            try {
                selected = client.create(name, slug, description, requestId)
                creating = false
                revision++
                message = "Kurum rolü doğrulandı."
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                reportError("Kurum rolü doğrulanamadı.")
            } finally { busy = false }
        }
    }
}

@Composable
private fun NewInstitutionRole(busy: Boolean, onClose: () -> Unit,
    onSave: (String, String, String?, UUID, reportError: (String) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var slug by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val normalized = slug.trim().lowercase(Locale.ROOT)
    val ready = name.isNotBlank() && normalized.matches(Regex("[a-z][a-z0-9_-]*"))
    val memo = remember { mutableMapOf<List<String>, UUID>() }
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, title = { Text("Yeni kurum rolü") }, text = {
        Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it.take(100) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Rol adı") })
            OutlinedTextField(slug, { slug = it.take(100) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Rol kodu") })
            OutlinedTextField(description, { description = it.take(2000) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Açıklama") })
            error?.let { Text(it) }
        }
    }, confirmButton = {
        Button(enabled = ready && !busy, onClick = {
            error = null
            val cleanName = name.trim()
            val details = description.trim()
            val id = memo.getOrPut(listOf(cleanName, normalized, details), UUID::randomUUID)
            onSave(cleanName, normalized, details.ifBlank { null }, id) { error = it }
        }) { Text("Rolü kaydet") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text("Vazgeç") } })
}

private suspend fun <T> roleResult(load: suspend () -> T): Result<T> = try {
    Result.success(load())
} catch (failure: Exception) {
    if (failure is CancellationException) throw failure
    Result.failure(failure)
}
