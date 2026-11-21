package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

private val surveyAudiences = mapOf("PARENTS" to "Veliler", "STAFF" to "Personel", "ALL" to "Veliler ve personel")
private val surveyTypes = mapOf("TEXT" to "Metin", "RATING" to "Puan (1–5)", "MULTIPLE_CHOICE" to "Seçmeli")

@Composable
internal fun NativeSurveysScreen(state: Okul360UiState, onBack: (() -> Unit)? = null, child: String? = null) {
    val user = state.currentUser
    val organization = if (user.is_super_admin == true) state.workspaceOrganizationId else user.resolvedTenantId
    val canManage = child == null && NativeNavigationPolicy.has(user, "surveys.read")
    val canRespond = NativeNavigationPolicy.has(user, if (child == null) "surveys.respond" else "parent.respond_survey")
    val canView = if (child == null) canManage || canRespond else NativeNavigationPolicy.has(user, "parent.view_children")
    if (!state.isLoggedIn || organization.isNullOrBlank() || !canView) {
        Column(Modifier.padding(16.dp)) {
            Text("Anketleri görüntülemek için yetkili çalışma alanını açın.")
            onBack?.let { TextButton(onClick = it) { Text("Geri") } }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl, child) {
        SurveyWorkspace(remember { NativeSurveyClient(organization, child) }, canManage, canRespond,
            canManage && NativeNavigationPolicy.has(user, "surveys.write"), onBack)
    }
}

@Composable
private fun SurveyWorkspace(client: NativeSurveyClient, canManage: Boolean, canRespond: Boolean,
    canWrite: Boolean, onBack: (() -> Unit)?) {
    var participation by remember { mutableStateOf(!canManage) }
    var selected by remember { mutableStateOf<NativeSurveyRecord?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var responsePage by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<SurveyQuestionRecord?>(null) }
    var confirmed by remember { mutableStateOf(emptySet<String>()) }
    var answers by remember { mutableStateOf(emptyMap<String, Any>()) }
    val chosen = selected
    val lists by surveyLoad(page, participation, revision) { client.list(page, participation) }
    val detail by surveyLoad(chosen?.id, participation, revision) {
        if (chosen != null && !participation) client.detail(chosen.id) else chosen
    }
    val questions by surveyLoad(chosen?.id, participation, revision) {
        if (chosen != null && !participation) client.questions(chosen.id) else chosen?.questions.orEmpty()
    }
    val results by surveyLoad(chosen?.id, participation, revision) {
        if (chosen != null && !participation) client.results(chosen.id) else null
    }
    val responses by surveyLoad(chosen?.id, participation, responsePage, revision) {
        if (chosen != null && !participation) client.responses(chosen.id, responsePage) else emptyList()
    }
    val record = detail?.getOrNull()
    val coroutine = rememberCoroutineScope()
    fun write(action: suspend () -> String) {
        if (busy) return
        busy = true
        notice = null
        coroutine.launch {
            try {
                notice = action()
                revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                notice = "Anket işlemi doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(record?.title ?: "Anketler", style = MaterialTheme.typography.headlineSmall)
            if (chosen != null) TextButton(enabled = !busy, onClick = { selected = null; notice = null }) { Text("Anketlere dön") }
            else onBack?.let { TextButton(onClick = it, enabled = !busy) { Text("Geri") } }
            notice?.let { Text(it) }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Anket kayıtlarını yenile") }
        }
        if (chosen == null) {
            if (canManage && canRespond) item {
                TextButton(enabled = !busy, onClick = { participation = !participation; page = 0; notice = null }) {
                    Text(if (participation) "Yönetim kayıtlarını göster" else "Yanıtlayabileceğim anketler")
                }
            }
            if (canWrite && !participation) item {
                Button(enabled = !busy, onClick = { form = "draft"; notice = null }) { Text("Anket taslağı oluştur") }
            }
            val rows = lists?.getOrNull().orEmpty()
            if (rows.isEmpty()) item { Text(surveyLoadMessage(lists, "Bu kapsamda anket kaydı yok.")) }
            items(rows.take(20), key = { it.id }) { row ->
                SurveyCard {
                    Text(row.title, style = MaterialTheme.typography.titleMedium)
                    Text("${row.startDate} – ${row.endDate}")
                    if (participation) {
                        if (row.submitted || row.id in confirmed) Text("Katılım kayıtlı")
                    } else Text("${surveyStatus(row.status)} · ${row.responseCount} yanıt")
                    TextButton(onClick = { selected = row; answers = emptyMap(); responsePage = 0; notice = null }) { Text("Anketi aç") }
                }
            }
            if (page > 0 || rows.size > 20) item {
                Text("Sayfa ${page + 1}")
                if (page > 0) TextButton(enabled = !busy, onClick = { page-- }) { Text("Önceki sayfa") }
                if (rows.size > 20) TextButton(enabled = !busy, onClick = { page++ }) { Text("Sonraki sayfa") }
            }
        } else if (record == null) item { Text(surveyLoadMessage(detail, "Anket bulunamadı.")) }
        else {
            item {
                SurveyCard {
                    if (record.description.isNotBlank()) Text(record.description)
                    Text("${record.startDate} – ${record.endDate}")
                    Text(surveyAudiences[record.audience] ?: record.audience)
                    if (record.anonymous) Text("Anonim anket")
                    if (!participation) {
                        Text(surveyStatus(record.status))
                        if (canWrite && record.status == "DRAFT") {
                            TextButton(enabled = !busy, onClick = { form = "draft"; notice = null }) { Text("Taslağı düzenle") }
                            TextButton(enabled = !busy, onClick = { form = "question"; notice = null }) { Text("Soru ekle") }
                            Button(enabled = !busy && questions?.getOrNull()?.isNotEmpty() == true, onClick = { form = "publish" }) { Text("Yayımla") }
                        }
                        if (canWrite && record.status == "ACTIVE") TextButton(enabled = !busy, onClick = { form = "close" }) { Text("Anketi kapat") }
                    }
                }
            }
            val prompts = questions?.getOrNull().orEmpty()
            val alreadySubmitted = record.submitted || record.id in confirmed
            if (prompts.isEmpty()) item { Text(surveyLoadMessage(questions, "İlgili soru kaydı bulunamadı.")) }
            if (participation && alreadySubmitted) item { Text("Anket katılımı kayıtlı.") }
            if (!participation || !alreadySubmitted) items(prompts, key = { it.id }) { question ->
                SurveyCard {
                    Text(question.text, style = MaterialTheme.typography.titleMedium)
                    if (question.required) Text("Zorunlu")
                    if (participation && canRespond) SurveyAnswer(question, answers[question.id], busy) { value ->
                        answers = if (value == null) answers - question.id else answers + (question.id to value)
                    }
                    if (!participation) {
                        Text(surveyTypes[question.type] ?: question.type)
                        question.options.forEach { Text(it) }
                        if (canWrite && record.status == "DRAFT") TextButton(enabled = !busy, onClick = { deleting = question }) { Text("Soruyu kaldır") }
                    }
                }
            }
            if (participation && canRespond && !alreadySubmitted && prompts.isNotEmpty()) item {
                Button(enabled = !busy && prompts.all { !it.required || answers[it.id]?.toString()?.isNotBlank() == true },
                    onClick = { write {
                        val body = JSONObject()
                        answers.forEach { (id, value) -> body.put(id, if (value is String) value.trim() else value) }
                        client.submit(record.id, body)
                        confirmed = confirmed + record.id
                        "Anket katılımı sunucudan doğrulandı."
                    } }) { Text("Yanıtları gönder") }
            }
            if (!participation) {
                item { SurveyResults(results) }
                val saved = responses?.getOrNull().orEmpty()
                item { Text("Yanıt kayıtları", style = MaterialTheme.typography.titleMedium) }
                if (saved.isEmpty()) item { Text(surveyLoadMessage(responses, "Kayıtlı yanıt yok.")) }
                items(saved.take(20), key = { it.getString("id") }) { row ->
                    SurveyCard {
                        if (!record.anonymous) row.optString("respondent_name").takeUnless { it.isBlank() || it == "null" }?.let { Text(it) }
                        row.optString("submitted_at").takeIf(String::isNotBlank)?.let { Text(it) }
                        val storedAnswers = row.optJSONArray("answers")
                        for (index in 0 until (storedAnswers?.length() ?: 0)) {
                            val answer = storedAnswers!!.getJSONObject(index)
                            Text("${answer.optString("question_text")} · ${if (answer.isNull("value")) "Yanıt okunamadı" else answer.getString("value")}")
                        }
                    }
                }
                if (responsePage > 0 || saved.size > 20) item {
                    if (responsePage > 0) TextButton(onClick = { responsePage-- }) { Text("Önceki yanıtlar") }
                    if (saved.size > 20) TextButton(onClick = { responsePage++ }) { Text("Sonraki yanıtlar") }
                }
            }
        }
    }
    if (form == "draft" && canWrite) SurveyDraftDialog(record, busy, notice, { form = null }) { draft, request ->
        write { client.save(draft, request, chosen?.id); form = null; page = 0; "Anket taslağı sunucudan doğrulandı." }
    }
    if (form == "question" && canWrite && record != null) SurveyQuestionDialog(questions?.getOrNull().orEmpty().size, busy, notice, { form = null }) { draft, request ->
        write { client.saveQuestion(record.id, draft, request); form = null; "Anket sorusu sunucudan doğrulandı." }
    }
    if (canWrite && record != null && form in setOf("publish", "close")) {
        val publish = form == "publish"
        val label = if (publish) "Anketi yayımla" else "Anketi kapat"
        SurveyConfirmation(label, notice ?: record.title, busy, { form = null }) {
            write { client.status(record.id, publish); form = null; "Anket durumu sunucudan doğrulandı." }
        }
    }
    deleting?.let { question ->
        if (record != null && canWrite) SurveyConfirmation("Soruyu kaldır", notice ?: question.text, busy, { deleting = null }) {
            write { client.deleteQuestion(record.id, question.id); deleting = null; "Sorunun kaldırıldığı doğrulandı." }
        }
    }
}

@Composable
private fun SurveyAnswer(question: SurveyQuestionRecord, value: Any?, busy: Boolean, update: (Any?) -> Unit) {
    when (question.type) {
        "RATING" -> (1..5).forEach { number ->
            SurveyRadio(number.toString(), value == number, busy) { update(number) }
        }
        "MULTIPLE_CHOICE" -> question.options.forEach { option ->
            SurveyRadio(option, value == option, busy) { update(option) }
        }
        "TEXT" -> SurveyField(value?.toString().orEmpty(), "Yanıt", 2000, busy) { update(it.takeIf(String::isNotEmpty)) }
    }
    if (value != null && !question.required) TextButton(enabled = !busy, onClick = { update(null) }) { Text("Yanıtı temizle") }
}

@Composable
private fun SurveyRadio(title: String, selected: Boolean, busy: Boolean, choose: () -> Unit) {
    TextButton(onClick = choose, enabled = !busy) { Text(if (selected) "✓ $title" else title) }
}

@Composable
private fun SurveyDraftDialog(record: NativeSurveyRecord?, busy: Boolean, notice: String?, close: () -> Unit,
    save: (SurveyDraft, UUID) -> Unit) {
    var draft by remember { mutableStateOf(record?.let {
        SurveyDraft(it.title, it.description, it.audience, it.startDate, it.endDate, it.anonymous)
    } ?: SurveyDraft()) }
    val normalized = draft.copy(title = draft.title.trim(), description = draft.description.trim(),
        startDate = draft.startDate.trim(), endDate = draft.endDate.trim())
    val request = remember(normalized) { UUID.randomUUID() }
    val datesOk = runCatching { !LocalDate.parse(normalized.endDate).isBefore(LocalDate.parse(normalized.startDate)) }.getOrDefault(false)
    ConsentFormDialog(if (record == null) "Anket taslağı oluştur" else "Anket taslağını düzenle", "native_survey_draft", busy, close, fields = {
        item { SurveyField(draft.title, "Anket adı", 255, busy) { draft = draft.copy(title = it) } }
        item { SurveyField(draft.description, "Açıklama", 5000, busy) { draft = draft.copy(description = it) } }
        item { Text("Hedef kitle") }
        items(surveyAudiences.entries.toList(), key = { it.key }) { (code, label) ->
            SurveyRadio(label, draft.audience == code, busy) { draft = draft.copy(audience = code) }
        }
        item { SurveyField(draft.startDate, "Başlangıç (YYYY-AA-GG)", 10, busy) { draft = draft.copy(startDate = it) } }
        item { SurveyField(draft.endDate, "Bitiş (YYYY-AA-GG)", 10, busy) { draft = draft.copy(endDate = it) } }
        item { SurveyRadio("Anonim yanıtla", draft.anonymous, busy) { draft = draft.copy(anonymous = !draft.anonymous) } }
        notice?.let { item { Text(it) } }
    }, confirm = {
        Button(enabled = !busy && normalized.title.isNotBlank() && datesOk, onClick = { save(normalized, request) }) { Text("Kaydet") }
    })
}

@Composable
private fun SurveyQuestionDialog(order: Int, busy: Boolean, notice: String?, close: () -> Unit, save: (SurveyQuestionDraft, UUID) -> Unit) {
    var draft by remember { mutableStateOf(SurveyQuestionDraft(order = order)) }
    var options by remember { mutableStateOf("") }
    val choices = if (draft.type == "MULTIPLE_CHOICE") options.lines().map(String::trim).filter(String::isNotBlank) else emptyList()
    val normalized = draft.copy(text = draft.text.trim(), options = choices)
    val request = remember(normalized) { UUID.randomUUID() }
    val choicesOk = draft.type != "MULTIPLE_CHOICE" || choices.size in 2..25 && choices.distinct().size == choices.size &&
        choices.all { it.length <= 200 }
    ConsentFormDialog("Anket sorusu ekle", "native_survey_question", busy, close, fields = {
        item { SurveyField(draft.text, "Soru", 2000, busy) { draft = draft.copy(text = it) } }
        items(surveyTypes.entries.toList(), key = { it.key }) { (code, label) ->
            SurveyRadio(label, draft.type == code, busy) { draft = draft.copy(type = code) }
        }
        if (draft.type == "MULTIPLE_CHOICE") item {
            SurveyField(options, "Her satıra bir seçenek", 5025, busy) { options = it }
        }
        item { SurveyRadio("Zorunlu soru", draft.required, busy) { draft = draft.copy(required = !draft.required) } }
        notice?.let { item { Text(it) } }
    }, confirm = {
        Button(enabled = !busy && normalized.text.isNotBlank() && choicesOk && order < 100,
            onClick = { save(normalized, request) }) { Text("Soruyu kaydet") }
    })
}

@Composable
private fun SurveyResults(result: Result<JSONObject?>?) {
    val data = result?.getOrNull()
    Text("Anket sonuçları", style = MaterialTheme.typography.titleMedium)
    if (data == null) {
        Text(surveyLoadMessage(result, "Anket sonucu yok."))
        return
    }
    Text("${data.getInt("total_responses")} yanıt")
    val rows = data.getJSONArray("question_results")
    for (index in 0 until rows.length()) {
        val row = rows.getJSONObject(index)
        SurveyCard {
            Text(row.getString("question_text"))
            Text("${row.getInt("response_count")} geçerli yanıt")
            if (!row.isNull("average_rating")) Text("Ortalama ${row.getString("average_rating")} / 5")
            row.optJSONObject("option_counts")?.let { counts ->
                for (option in counts.keys()) Text("$option · ${counts.getInt(option)}")
            }
        }
    }
}

@Composable
private fun SurveyField(value: String, label: String, max: Int, busy: Boolean, update: (String) -> Unit) {
    OutlinedTextField(value, { update(it.take(max)) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(label) })
}

@Composable
private fun SurveyCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun SurveyConfirmation(label: String, title: String, busy: Boolean, close: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text(label) }, text = { Text(title) },
        confirmButton = { Button(enabled = !busy, onClick = confirm) { Text(label) } },
        dismissButton = { TextButton(enabled = !busy, onClick = close) { Text("Vazgeç") } })
}

private fun surveyStatus(status: String) = when (status) {
    "DRAFT" -> "Taslak"
    "ACTIVE" -> "Yayında"
    "CLOSED" -> "Kapalı"
    else -> status
}

private fun surveyLoadMessage(result: Result<*>?, empty: String) = when {
    result == null -> "Anket kayıtları yükleniyor…"
    result.isFailure -> "Anket kayıtları yüklenemedi."
    else -> empty
}

@Composable
private fun <T> surveyLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> = key(keys.toList()) {
    produceState<Result<T>?>(null) {
        value = try { Result.success(load()) } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            Result.failure(failure)
        }
    }
}
