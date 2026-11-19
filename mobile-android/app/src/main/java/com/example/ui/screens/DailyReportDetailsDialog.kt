package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyReport
import com.example.data.remote.DailyReportEdit
import com.example.data.remote.DailyReportHttpException
import com.example.data.remote.NativeDailyReportClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun DailyReportDetailsDialog(id: String, actorId: String, canWrite: Boolean,
    onDismiss: () -> Unit, onChanged: () -> Unit) {
    val api = remember(id, actorId) { NativeDailyReportClient() }
    val coroutine = rememberCoroutineScope()
    var revision by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<DailyReport?>(null) }
    var edit by remember { mutableStateOf<DailyReportEdit?>(null) }
    var activityText by remember { mutableStateOf("") }
    var confirmingDelete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(id, revision) {
        report = null
        error = null
        try {
            report = api.detail(id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = dailyReportFailure(failure)
        }
    }

    fun mutate(operation: suspend () -> Unit) {
        if (!canWrite || busy) return
        busy = true
        error = null
        coroutine.launch {
            try {
                operation()
                onChanged()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                error = dailyReportFailure(failure)
            } finally {
                busy = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(12.dp)
            .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f), shape = MaterialTheme.shapes.large) {
            Column(Modifier.imePadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val current = report
                val draft = edit
                Text(if (confirmingDelete) "Karne silinsin mi?" else if (draft != null) "Karneyi düzenle" else "Karne ayrıntısı",
                    style = MaterialTheme.typography.titleLarge)
                Column(Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        current == null && error == null -> CircularProgressIndicator()
                        current == null -> TextButton(onClick = { revision++ }) { Text("Tekrar dene") }
                        confirmingDelete -> Text("${current.student_name} · ${current.date}")
                        draft != null && canWrite -> {
                            Text("${current.student_name} · ${current.date}")
                            ReportEditField("Beslenme", draft.meals, !busy) { edit = draft.copy(meals = it) }
                            ReportEditField("Uyku", draft.sleep, !busy) { edit = draft.copy(sleep = it) }
                            ReportEditField("Duygu durumu", draft.mood, !busy) { edit = draft.copy(mood = it) }
                            ReportEditField("Etkinlikler, her satıra bir tane", activityText, !busy) { activityText = it }
                            ReportEditField("Öğretmen notu", draft.note, !busy) { edit = draft.copy(note = it) }
                        }
                        else -> {
                            Text(current.student_name, style = MaterialTheme.typography.titleMedium)
                            Text(current.date)
                            current.meal_status?.let { Text("Beslenme · $it") }
                            current.sleep_status?.let { Text("Uyku · $it") }
                            if (current.mood_level.isNotBlank()) Text("Duygu durumu · ${current.mood_level}")
                            if (current.activities_done.isNotEmpty()) Text(current.activities_done.joinToString(" · "))
                            if (current.teacher_note.isNotBlank()) Text(current.teacher_note)
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (canWrite && current != null) {
                        when {
                            confirmingDelete -> Button(onClick = { mutate { api.delete(current) } }, enabled = !busy,
                                modifier = Modifier.fillMaxWidth()) { Text("Karneyi sil") }
                            draft != null -> Button(onClick = {
                                val activities = if (activityText == current.activities_done.joinToString("\n")) current.activities_done
                                    else activityText.lines().map(String::trim).filter(String::isNotEmpty)
                                mutate { api.update(current, draft.copy(activities = activities), actorId) }
                            }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Değişiklikleri kaydet") }
                            else -> {
                                TextButton(onClick = { edit = DailyReportEdit.from(current); activityText = current.activities_done.joinToString("\n") }) { Text("Düzenle") }
                                TextButton(onClick = { confirmingDelete = true }) { Text("Sil") }
                            }
                        }
                    }
                    TextButton(enabled = !busy, onClick = {
                        if (confirmingDelete || draft != null) { confirmingDelete = false; edit = null; error = null } else onDismiss()
                    }) { Text(if (confirmingDelete || draft != null) "İptal" else "Kapat") }
                }
            }
        }
    }
}

private fun dailyReportFailure(failure: Exception): String = when ((failure as? DailyReportHttpException)?.code) {
    403 -> "Bu işlem için yetkiniz yok."
    404 -> "Karne bulunamadı veya erişim kapsamınız değişti."
    412 -> "Karne değişti. Güncel kaydı açın."
    else -> failure.message?.takeIf { it.startsWith("Karne değişti.") } ?: "Karne işlemi doğrulanamadı. Tekrar deneyin."
}

@Composable
private fun ReportEditField(label: String, value: String, enabled: Boolean, change: (String) -> Unit) {
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), enabled = enabled, label = { Text(label) })
}
