package com.example.ui.screens

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.util.UUID

private val materialTypes = linkedMapOf("PDF" to "PDF", "DOCUMENT" to "Belge", "IMAGE" to "Görsel", "VIDEO" to "Video",
    "AUDIO" to "Ses", "PRESENTATION" to "Sunum", "OTHER" to "Arşiv")
private val materialVisibility = linkedMapOf("PUBLIC" to "Kurumdaki tüm veliler", "CLASS_ONLY" to "Yalnız seçili sınıf",
    "TEACHER_ONLY" to "Yalnız öğretmen ve yönetim")
private val educationManagement = setOf("admin", "administrator", "school_admin", "manager", "principal", "org_admin", "branch_manager", "owner")

@Composable
internal fun NativeMaterialsScreen(state: Okul360UiState, child: String? = null, onBack: (() -> Unit)? = null) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, if (child == null) "material.read" else "parent.view_children") ||
        (user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank())) {
        Column(Modifier.padding(16.dp)) {
            Text("Ders materyallerini görüntülemek için yetkili çalışma alanını açın.")
            onBack?.let { TextButton(onClick = it) { Text("Geri") } }
        }
        return
    }
    val roles = user.roles.filter { it.is_active }.map { it.slug.lowercase() }
    val restricted = roles.none { it in educationManagement } && roles.any { it == "teacher" || it == "parent" }
    val canWrite = child == null && NativeNavigationPolicy.has(user, "material.write")
    key(financePresentationScope(state), state.serverBaseUrl, child) {
        val client = remember { NativeMaterialsClient(state.selectedBranchId, child) }
        MaterialsWorkspace(client, user.id, !restricted || user.is_super_admin == true, canWrite, child != null, onBack)
    }
}

@Composable
private fun MaterialsWorkspace(client: NativeMaterialsClient, actor: String, broadWrite: Boolean, canWrite: Boolean,
    isParent: Boolean, onBack: (() -> Unit)?) {
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<RecordedMaterial?>(null) }
    var deleting by remember { mutableStateOf<RecordedMaterial?>(null) }
    var download by remember { mutableStateOf<RecordedMaterial?>(null) }
    val resolver = LocalContext.current.contentResolver
    val coroutine = rememberCoroutineScope()
    val result by produceState<Result<List<RecordedMaterial>>?>(null, page, revision) {
        value = null
        value = loadMaterials { client.list(page, actor, broadWrite) }
    }
    fun runAction(refresh: Boolean = true, action: suspend () -> String) {
        if (busy) return
        busy = true
        notice = null
        coroutine.launch {
            try {
                notice = action()
                if (refresh) revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                notice = "Materyal işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val target = download
        download = null
        if (target != null && uri != null) runAction {
            val bytes = client.download(target)
            withContext(Dispatchers.IO) {
                client.ensureCurrent()
                checkNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
            }
            "Materyal dosyası kaydedildi."
        }
    }
    val rows = result?.getOrNull().orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Ders Materyalleri", style = MaterialTheme.typography.headlineSmall)
            onBack?.let { TextButton(onClick = it, enabled = !busy) { Text("Geri") } }
            notice?.let { Text(it) }
            if (canWrite) Button(enabled = !busy, onClick = { notice = null; editing = null; editor = true }) { Text("Materyal yükle") }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Materyalleri yenile") }
        }
        if (rows.isEmpty()) item {
            Text(when {
                result == null -> "Materyaller yükleniyor…"
                result?.isFailure == true -> "Materyal listesi alınamadı. ${result?.exceptionOrNull()?.message.orEmpty()}"
                else -> "Bu kapsamda materyal kaydı yok."
            })
        }
        items(if (isParent) rows else rows.take(20), key = { it.id }) { row ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.draft.title, style = MaterialTheme.typography.titleMedium)
                    listOf(materialTypes[row.draft.type].orEmpty(), row.className, row.draft.topic, row.draft.description,
                        materialVisibility[row.draft.visibility].orEmpty(), row.fileName,
                        row.downloads?.let { "$it indirme" }.orEmpty()).filter(String::isNotBlank).forEach { Text(it) }
                    TextButton(enabled = !busy && download == null, onClick = {
                        download = row
                        saveFile.launch(row.fileName.ifBlank { "materyal" })
                    }) { Text("Dosyayı indir") }
                    if (canWrite && row.canWrite) {
                        TextButton(enabled = !busy, onClick = { notice = null; editing = row; editor = true }) { Text("Düzenle") }
                        TextButton(enabled = !busy, onClick = { notice = null; deleting = row }) { Text("Kaldır") }
                    }
                }
            }
        }
        if (!isParent && (page > 0 || rows.size > 20)) item {
            Text("Sayfa ${page + 1}")
            if (page > 0) TextButton(enabled = !busy, onClick = { page-- }) { Text("Önceki sayfa") }
            if (rows.size > 20) TextButton(enabled = !busy, onClick = { page++ }) { Text("Sonraki sayfa") }
        }
    }
    val current = editing
    if (canWrite && editor) MaterialEditor(client, current, busy, notice, { editor = false }) { draft, file, request ->
        runAction {
            if (current == null) client.upload(draft, checkNotNull(file), request) else client.update(draft, current)
            if (current == null) page = 0
            editor = false
            "Materyal kaydı sunucudan doğrulandı."
        }
    }
    deleting?.let { row ->
        AlertDialog(onDismissRequest = { if (!busy) deleting = null }, title = { Text("Materyali kaldır") },
            text = { Text(notice ?: row.draft.title) },
            confirmButton = { Button(enabled = !busy, onClick = { runAction {
                client.delete(row.id)
                deleting = null
                "Materyalin kaldırıldığı doğrulandı."
            } }) { Text("Kaldır") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null }) { Text("Vazgeç") } })
    }
}

@Composable
private fun MaterialEditor(client: NativeMaterialsClient, record: RecordedMaterial?, busy: Boolean, notice: String?,
    close: () -> Unit, save: (MaterialDraft, HomeworkFile?, UUID) -> Unit) {
    var draft by remember(record?.id) { mutableStateOf(record?.draft ?: MaterialDraft()) }
    var file by remember { mutableStateOf<HomeworkFile?>(null) }
    var fileLoading by remember { mutableStateOf(false) }
    var fileError by remember { mutableStateOf<String?>(null) }
    var classRefresh by remember { mutableIntStateOf(0) }
    val resolver = LocalContext.current.contentResolver
    val coroutine = rememberCoroutineScope()
    val classes by produceState<Result<List<MaterialClass>>?>(null, classRefresh) {
        value = null
        value = loadMaterials { client.classes() }
    }
    val choices = classes?.getOrNull().orEmpty()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) coroutine.launch {
            fileLoading = true
            file = null
            fileError = null
            val result = loadMaterials { readMaterialFile(resolver, uri) }
            file = result.getOrNull()
            fileError = result.exceptionOrNull()?.message
            fileLoading = false
        }
    }
    val original = draft
    val submitted = original.copy(title = original.title.trim(), description = original.description.trim(),
        topic = original.topic.trim(), start = original.start.trim(), end = original.end.trim())
    val request = remember(submitted, file) { UUID.randomUUID() }
    val needsClass = draft.visibility == "CLASS_ONLY" || client.requiresClass
    val classAllowed = if (draft.classId.isBlank()) !needsClass else choices.any { it.id == draft.classId }
    ConsentFormDialog(if (record == null) "Materyal yükle" else "Materyali düzenle", "native_material_editor", busy || fileLoading, close, fields = {
        item { MaterialField(draft.title, "Başlık", 255, busy) { draft = draft.copy(title = it) } }
        item { MaterialField(draft.description, "Açıklama", 1000, busy) { draft = draft.copy(description = it) } }
        item { MaterialField(draft.topic, "Ünite veya konu", 255, busy) { draft = draft.copy(topic = it) } }
        item { Text("Dosya türü", style = MaterialTheme.typography.titleSmall) }
        if (record == null) items(materialTypes.entries.toList(), key = { it.key }) { (code, label) ->
            MaterialOption(label, draft.type == code, !busy) { draft = draft.copy(type = code) }
        } else item { Text(materialTypes[draft.type].orEmpty()) }
        item { Text("Görünürlük", style = MaterialTheme.typography.titleSmall) }
        items(materialVisibility.entries.toList(), key = { it.key }) { (code, label) ->
            MaterialOption(label, draft.visibility == code, !busy) { draft = draft.copy(visibility = code) }
        }
        item {
            Text("Sınıf", style = MaterialTheme.typography.titleSmall)
            if (!needsClass) MaterialOption("Sınıf seçilmedi", draft.classId.isBlank(), !busy) { draft = draft.copy(classId = "") }
            if (classes == null) Text("Sınıflar yükleniyor…")
            if (classes?.isFailure == true) TextButton(onClick = { classRefresh++ }) { Text("Sınıfları yeniden yükle") }
        }
        items(choices, key = { it.id }) { row ->
            MaterialOption(row.name, draft.classId == row.id, !busy) { draft = draft.copy(classId = row.id) }
        }
        item { MaterialField(draft.start, "Başlangıç (YYYY-AA-GG)", 10, busy) { draft = draft.copy(start = it) } }
        item { MaterialField(draft.end, "Bitiş (YYYY-AA-GG)", 10, busy) { draft = draft.copy(end = it) } }
        if (record == null) item {
            Button(onClick = { picker.launch(arrayOf("*/*")) }, enabled = !busy && !fileLoading) { Text("Cihazdan dosya seç") }
            file?.let { Text(it.name) }
            fileError?.let { Text(it) }
            if (fileLoading) Text("Dosya okunuyor…")
        }
        notice?.let { item { Text(it) } }
    }, confirm = {
        Button(enabled = !busy && !fileLoading && submitted.title.isNotEmpty() && classAllowed &&
            validMaterialDates(submitted.start, submitted.end) && (file != null || record != null),
            onClick = { save(submitted, file, request) }) { Text("Kaydet") }
    })
}

@Composable
private fun MaterialField(value: String, label: String, limit: Int, busy: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(limit)) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(label) })
}

@Composable
private fun MaterialOption(label: String, selected: Boolean, enabled: Boolean, select: () -> Unit) {
    TextButton(onClick = select, enabled = enabled) { Text(if (selected) "✓ $label" else label) }
}

private suspend fun <T> loadMaterials(action: suspend () -> T): Result<T> = try {
    Result.success(action())
} catch (failure: Exception) {
    if (failure is CancellationException) throw failure
    Result.failure(failure)
}

private suspend fun readMaterialFile(resolver: ContentResolver, uri: Uri): HomeworkFile = withContext(Dispatchers.IO) {
    val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    }?.replace('\\', '/')?.substringAfterLast('/')?.trim().orEmpty()
    check(name.isNotBlank() && name.length <= 255 && name.none { it.code < 32 }) { "Geçerli bir dosya adı gerekli." }
    val bytes = checkNotNull(resolver.openInputStream(uri)).source().buffer().use {
        check(!it.request(MATERIALS_FILE_LIMIT + 1)) { "Dosya 10 MB sınırını aşıyor." }
        it.readByteArray()
    }
    check(bytes.isNotEmpty()) { "Dosya boş." }
    HomeworkFile(name, bytes)
}
