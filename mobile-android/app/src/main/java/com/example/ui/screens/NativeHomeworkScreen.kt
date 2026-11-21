package com.example.ui.screens

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Composable
internal fun NativeHomeworkScreen(state: Okul360UiState, onBack: () -> Unit) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "homework.read") ||
        (user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank())) {
        Column(Modifier.padding(16.dp)) {
            Text("Ödev kayıtları için yetkili kurum çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        val client = remember { NativeHomeworkClient(state.selectedBranchId) }
        HomeworkWorkspace(client, NativeNavigationPolicy.has(user, "homework.write"), onBack)
    }
}

@Composable
private fun HomeworkWorkspace(client: NativeHomeworkClient, canWrite: Boolean, onBack: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    var search by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var submissionsPage by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var grade by remember { mutableStateOf<RecordedHomeworkSubmission?>(null) }
    var pendingDownload by remember { mutableStateOf<RecordedHomeworkSubmission?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val coroutine = rememberCoroutineScope()
    val resolver = LocalContext.current.contentResolver
    val chosen = selected
    val list by homeworkLoad(chosen, search, page, revision) {
        if (chosen == null) {
            if (search.isNotBlank()) delay(250)
            client.list(search.trim(), page)
        } else null
    }
    val detail by homeworkLoad(chosen, revision) { chosen?.let { client.detail(it) } }
    val current = detail?.getOrNull()
    val submissions by homeworkLoad(chosen, submissionsPage, revision) {
        if (chosen != null) client.submissions(chosen, 21, submissionsPage * 20) else emptyList()
    }
    fun write(refresh: Boolean = true, action: suspend () -> String) {
        if (busy) return
        busy = true
        notice = null
        coroutine.launch {
            try {
                notice = action()
                if (refresh) revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                notice = "Ödev işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val target = pendingDownload
        pendingDownload = null
        if (uri != null && target != null) write(false) {
            val bytes = client.download(target)
            withContext(Dispatchers.IO) {
                client.ensureCurrent()
                checkNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
            }
            "Ödev dosyası kaydedildi."
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(current?.title ?: "Ödev ve Değerlendirme", style = MaterialTheme.typography.headlineSmall)
            TextButton(enabled = !busy, onClick = {
                if (chosen == null) onBack() else { selected = null; notice = null }
            }) { Text(if (chosen == null) "Geri" else "Ödevlere dön") }
            notice?.let { Text(it) }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Ödev kayıtlarını yenile") }
        }
        if (chosen == null) {
            item {
                HomeworkField(search, "Ödev ara", 100, busy) { search = it; page = 1 }
                if (canWrite) Button(enabled = !busy, onClick = { notice = null; editing = true }) { Text("Ödev oluştur") }
            }
            val result = list?.getOrNull()
            val rows = result?.first.orEmpty()
            if (rows.isEmpty()) item { Text(homeworkMessage(list, "Bu kapsamda ödev yok.")) }
            items(rows, key = { it.id }) { row ->
                HomeworkCard(listOf(row.title, row.className, row.subject, row.day, homeworkStatus(row.status))) {
                    TextButton(onClick = { selected = row.id; submissionsPage = 0; notice = null }) { Text("${row.title} ödevini aç") }
                }
            }
            if (page > 1 || (result?.second ?: 0) > page) item {
                Text("Sayfa $page")
                if (page > 1) TextButton(onClick = { page-- }) { Text("Önceki sayfa") }
                if ((result?.second ?: 0) > page) TextButton(onClick = { page++ }) { Text("Sonraki sayfa") }
            }
        } else if (current != null) {
            item {
                HomeworkCard(listOf(current.className, current.subject, current.day, homeworkStatus(current.status),
                    current.description, current.attachment)) {
                    if (canWrite) {
                        TextButton(enabled = !busy, onClick = { editing = true; notice = null }) { Text("Ödevi düzenle") }
                        TextButton(enabled = !busy, onClick = { deleting = true; notice = null }) { Text("Ödevi kaldır") }
                        if (current.status == "ACTIVE") Button(enabled = !busy, onClick = { submitting = true; notice = null }) {
                            Text("Öğrenci teslimi ekle")
                        }
                    }
                }
                Text("Teslimler", style = MaterialTheme.typography.titleLarge)
            }
            val rows = submissions?.getOrNull().orEmpty()
            if (rows.isEmpty()) item { Text(homeworkMessage(submissions, "Bu ödeve kayıtlı teslim yok.")) }
            items(rows.take(20), key = { it.id }) { row ->
                HomeworkCard(listOf(row.student, homeworkStatus(row.status), row.time, row.text,
                    row.score?.toPlainString()?.let { "Puan $it / 100" }.orEmpty(), row.feedback, row.fileName)) {
                    if (row.fileUrl.isNotBlank()) TextButton(enabled = !busy && pendingDownload == null, onClick = {
                        pendingDownload = row
                        saveFile.launch(row.fileName.ifBlank { "teslim-dosyasi" })
                    }) { Text("Teslim dosyasını indir") }
                    if (canWrite) TextButton(enabled = !busy, onClick = { grade = row; notice = null }) { Text("Teslimi değerlendir") }
                }
            }
            if (submissionsPage > 0 || rows.size > 20) item {
                Text("Sayfa ${submissionsPage + 1}")
                if (submissionsPage > 0) TextButton(onClick = { submissionsPage-- }) { Text("Önceki teslimler") }
                if (rows.size > 20) TextButton(onClick = { submissionsPage++ }) { Text("Sonraki teslimler") }
            }
        } else item { Text(homeworkMessage(detail, "Ödev kaydı bulunamadı.")) }
    }

    if (canWrite && editing) {
        val initial = current?.let { HomeworkDraft(it.title, it.subject, it.classId, it.day, it.description, it.status, it.attachment) } ?: HomeworkDraft()
        HomeworkEditor(client, initial, busy, notice, { editing = false }) { draft, request ->
            write { client.save(draft, chosen, request); editing = false; "Ödev sunucudan doğrulandı." }
        }
    }
    if (canWrite && deleting && current != null) {
        AlertDialog({ if (!busy) deleting = false }, title = { Text("Ödevi kaldır") }, text = { Text(notice ?: current.title) },
            confirmButton = { Button(enabled = !busy, onClick = { write {
                client.delete(current.id); deleting = false; selected = null; "Ödevin kaldırıldığı doğrulandı."
            } }) { Text("Kaldır") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = false }) { Text("İptal") } })
    }
    if (canWrite && submitting && current != null) HomeworkSubmitDialog(client, current, busy, notice, { submitting = false }) { student, text, file ->
        write { client.submit(current.id, student, text, file); submitting = false; submissionsPage = 0; "Öğrenci teslimi doğrulandı." }
    }
    val marked = grade
    if (canWrite && current != null && marked != null) {
        var points by remember(marked.id) { mutableStateOf(marked.score?.stripTrailingZeros()?.toPlainString().orEmpty()) }
        var feedback by remember(marked.id) { mutableStateOf(marked.feedback) }
        val score = points.replace(',', '.').takeIf { it.matches(Regex("\\d{1,3}(\\.\\d{1,2})?")) }?.toBigDecimalOrNull()
        ConsentFormDialog("Ödev teslimini değerlendir", "native_homework_grade", busy, { grade = null }, fields = {
            item { Text(marked.student) }
            item { HomeworkField(points, "Puan (0–100)", 6, busy) { points = it } }
            item { HomeworkField(feedback, "Öğretmen geri bildirimi", 10000, busy) { feedback = it } }
            notice?.let { item { Text(it) } }
        }, confirm = {
            Button(enabled = !busy && score != null && score in BigDecimal.ZERO..BigDecimal(100), onClick = { write {
                client.grade(current.id, marked, requireNotNull(score), feedback.trim())
                grade = null
                "Teslim puanı ve geri bildirim doğrulandı."
            } }) { Text("Değerlendirmeyi kaydet") }
        })
    }
}

@Composable
private fun HomeworkEditor(client: NativeHomeworkClient, initial: HomeworkDraft, busy: Boolean, notice: String?,
    close: () -> Unit, save: (HomeworkDraft, UUID) -> Unit) {
    var form by remember { mutableStateOf(initial) }
    var selecting by remember { mutableStateOf(false) }
    val choices by homeworkLoad(Unit) { client.classes() }
    val classes = choices?.getOrNull().orEmpty()
    val request = remember(form) { UUID.randomUUID() }
    val valid = form.title.isNotBlank() && (!client.requiresClass || form.classroom.isNotBlank()) &&
        (form.day.isBlank() || runCatching { LocalDate.parse(form.day) }.isSuccess)
    if (selecting) {
        ConsentFormDialog("Ödev sınıfı seç", "native_homework_classes", false, { selecting = false }, fields = {
            if (classes.isEmpty()) item { Text(homeworkMessage(choices, "Yetkili sınıf bulunamadı.")) }
            items(classes, key = { it.id }) { row ->
                TextButton(onClick = { form = form.copy(classroom = row.id); selecting = false }) { Text(row.name) }
            }
            if (!client.requiresClass) item { TextButton(onClick = {
                form = form.copy(classroom = ""); selecting = false
            }) { Text("Sınıfsız kayıt") } }
        }, confirm = {})
        return
    }
    ConsentFormDialog("Ödev bilgileri", "native_homework_editor", busy, close, fields = {
        item { HomeworkField(form.title, "Ödev adı", 255, busy) { form = form.copy(title = it) } }
        item { HomeworkField(form.subject, "Ders", 100, busy) { form = form.copy(subject = it) } }
        item { TextButton(enabled = !busy, onClick = { selecting = true }) {
            Text(classes.firstOrNull { it.id == form.classroom }?.name ?: "Sınıf seç")
        } }
        item { HomeworkField(form.day, "Son tarih (YYYY-AA-GG)", 10, busy) { form = form.copy(day = it) } }
        item { HomeworkField(form.description, "Ödev açıklaması", 10000, busy) { form = form.copy(description = it) } }
        item { HomeworkField(form.attachment, "Ödev bağlantısı", 1000, busy) { form = form.copy(attachment = it) } }
        listOf("ACTIVE", "DRAFT", "CLOSED").forEach { code ->
            item { FilterChip(form.status == code, { form = form.copy(status = code) }, { Text(homeworkStatus(code)) }, enabled = !busy) }
        }
        notice?.let { item { Text(it) } }
    }, confirm = { Button(enabled = !busy && valid, onClick = {
        save(form.copy(title = form.title.trim(), subject = form.subject.trim(), description = form.description.trim(),
            attachment = form.attachment.trim()), request)
    }) { Text("Ödevi kaydet") } })
}

@Composable
private fun HomeworkSubmitDialog(client: NativeHomeworkClient, homework: RecordedHomework, busy: Boolean, notice: String?,
    close: () -> Unit, save: (String, String, HomeworkFile?) -> Unit) {
    var choosing by remember { mutableStateOf(true) }
    var studentsSearch by remember { mutableStateOf("") }
    var student by remember { mutableStateOf<HomeworkStudentChoice?>(null) }
    var text by remember { mutableStateOf("") }
    var file by remember { mutableStateOf<HomeworkFile?>(null) }
    var fileBusy by remember { mutableStateOf(false) }
    var fileError by remember { mutableStateOf<String?>(null) }
    val resolver = LocalContext.current.contentResolver
    val coroutine = rememberCoroutineScope()
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            fileBusy = true
            fileError = null
            coroutine.launch {
                try {
                    val loaded = withContext(Dispatchers.IO) {
                        client.ensureCurrent()
                        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            if (it.moveToFirst()) it.getString(0) else null
                        }.orEmpty().replace('\\', '/').substringAfterLast('/')
                        check(name.isNotBlank() && name.length <= 255)
                        val bytes = checkNotNull(resolver.openInputStream(uri)).source().buffer().use {
                            check(!it.request(HOMEWORK_FILE_LIMIT + 1))
                            it.readByteArray()
                        }
                        check(bytes.isNotEmpty())
                        HomeworkFile(name, bytes)
                    }
                    client.ensureCurrent()
                    file = loaded
                } catch (failure: Exception) {
                    if (failure is CancellationException) throw failure
                    fileError = "Dosya okunamadı. En fazla 10 MB dosya seçin."
                } finally { fileBusy = false }
            }
        }
    }
    if (choosing) {
        val options by homeworkLoad(studentsSearch) {
            if (studentsSearch.isNotBlank()) delay(250)
            client.students(homework.id, studentsSearch.trim())
        }
        val result = options?.getOrNull()
        val rows = result?.first.orEmpty()
        ConsentFormDialog("Teslim öğrencisi seç", "native_homework_students", false, close, fields = {
            item { HomeworkField(studentsSearch, "Öğrenci ara", 100, false) { studentsSearch = it } }
            if (rows.isEmpty()) item { Text(homeworkMessage(options, "Bu ödeve teslim yapabilecek öğrenci bulunamadı.")) }
            items(rows, key = { it.id }) { row -> TextButton(onClick = { student = row; choosing = false }) { Text(row.name) } }
            if (result?.second == true) item { Text("Daha fazla sonuç için aramayı daraltın.") }
        }, confirm = {})
        return
    }
    ConsentFormDialog("Öğrenci ödev teslimi", "native_homework_submission", busy || fileBusy, close, fields = {
        item {
            Text(homework.title)
            TextButton(enabled = !busy && !fileBusy, onClick = { choosing = true }) { Text(student?.name ?: "Öğrenci seç") }
        }
        item { HomeworkField(text, "Teslim metni", 10000, busy || fileBusy) { text = it } }
        item {
            TextButton(enabled = !busy && !fileBusy, onClick = { pick.launch(arrayOf("application/pdf", "image/jpeg", "image/png",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) }) { Text("Dosya seç") }
            file?.let {
                Text(it.name)
                TextButton(enabled = !busy && !fileBusy, onClick = { file = null }) { Text("Dosyayı kaldır") }
            }
        }
        fileError?.let { item { Text(it) } }
        notice?.let { item { Text(it) } }
    }, confirm = {
        Button(enabled = !busy && !fileBusy && student != null && (text.isNotBlank() || file != null),
            onClick = { save(requireNotNull(student).id, text.trim(), file) }) { Text("Teslimi kaydet") }
    })
}

@Composable
private fun HomeworkField(value: String, label: String, max: Int, busy: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(max)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth(),
        singleLine = max < 1000, enabled = !busy)
}

@Composable
private fun HomeworkCard(content: List<String>, actions: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            content.filter(String::isNotBlank).forEach { Text(it) }
            actions()
        }
    }
}

private fun homeworkStatus(code: String) = when (code) {
    "ACTIVE" -> "Etkin"
    "CLOSED" -> "Kapalı"
    "DRAFT" -> "Taslak"
    "GRADED" -> "Değerlendirildi"
    "SUBMITTED" -> "Teslim edildi"
    else -> code
}

private fun homeworkMessage(result: Result<*>?, empty: String) = when {
    result == null -> "Ödev kayıtları yükleniyor…"
    result.isFailure -> "Ödev kayıtları yüklenemedi."
    else -> empty
}

@Composable
private fun <T> homeworkLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> = key(keys.toList()) {
    produceState<Result<T>?>(null) {
        value = try { Result.success(load()) } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            Result.failure(failure)
        }
    }
}
