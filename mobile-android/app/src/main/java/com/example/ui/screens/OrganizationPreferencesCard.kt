package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.NativeOrganizationPreferences
import com.example.data.remote.OrganizationPreferences
import com.example.data.remote.normalizeExtensions
import com.example.data.remote.noticePreferenceLabels
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun OrganizationPreferencesCard(organizationId: String, canWrite: Boolean) {
    var open by remember(organizationId) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kurum tercihleri", style = MaterialTheme.typography.titleMedium)
            Text("Kurum bilgileri, öğrenci belgeleri ve kayıtlı bildirim tercihleri")
            OutlinedButton(onClick = { open = true }) { Text("Kurum tercihlerini aç") }
        }
    }
    if (open) OrganizationPreferencesDialog(organizationId, canWrite) { open = false }
}

@Composable
private fun OrganizationPreferencesDialog(organizationId: String, canWrite: Boolean, onDismiss: () -> Unit) {
    val client = remember(organizationId) { NativeOrganizationPreferences(organizationId) }
    var snapshot by remember { mutableStateOf<OrganizationPreferences?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(client, retry) {
        error = null
        try {
            snapshot = client.load()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Kurum tercihleri yüklenemedi."
        }
    }
    val shown = snapshot
    if (shown == null) {
        AlertDialog(
            onDismissRequest = onDismiss, title = { Text("Kurum tercihleri") },
            text = { Text(error ?: "Kurum tercihleri yükleniyor…") },
            confirmButton = { if (error != null) TextButton(onClick = { retry++ }) { Text("Tekrar dene") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat") } },
        )
        return
    }

    var schoolName by remember(shown) { mutableStateOf(shown.values["school_name"] as String) }
    var year by remember(shown) { mutableStateOf((shown.values["academic_year"] as String?).orEmpty()) }
    var currency by remember(shown) { mutableStateOf(shown.values["default_currency"] as String) }
    var size by remember(shown) { mutableStateOf(shown.values["max_upload_size_mb"].toString()) }
    var extensions by remember(shown) { mutableStateOf(shown.values["allowed_file_types"] as String) }
    var notices by remember(shown) { mutableStateOf(noticePreferenceLabels.keys.associateWith { shown.values[it] as Boolean? }) }
    var saved by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val editable = canWrite && !busy

    Dialog(
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.padding(12.dp).widthIn(max = 560.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kurum tercihleri", style = MaterialTheme.typography.titleLarge)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Text("Genel", style = MaterialTheme.typography.titleSmall)
                OutlinedTextField(schoolName, { schoolName = it; saved = false }, label = { Text("Kurum adı") },
                    modifier = Modifier.fillMaxWidth(), enabled = editable, singleLine = true)
                OutlinedTextField(year, { year = it; saved = false }, label = { Text("Akademik yıl") },
                    modifier = Modifier.fillMaxWidth(), enabled = editable, singleLine = true)
                PreferenceChoice("Para birimi", currency, listOf("TRY", "EUR", "USD"), editable) {
                    currency = it
                    saved = false
                }
                Text("Öğrenci belgeleri", style = MaterialTheme.typography.titleSmall)
                Text("Sunucu üst sınırı ${shown.maxSizeMb} MB. İzinli uzantılar ${shown.extensions.joinToString(", ")}.")
                OutlinedTextField(size, { size = it; saved = false }, label = { Text("Maksimum boyut (MB)") },
                    modifier = Modifier.fillMaxWidth().testTag("student_document_max_size"), enabled = editable, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(extensions, { extensions = it; saved = false }, label = { Text("İzin verilen uzantılar") },
                    modifier = Modifier.fillMaxWidth(), enabled = editable)
                Text("Kayıtlı bildirim tercihleri", style = MaterialTheme.typography.titleSmall)
                Text("Gönderim için ilgili servis bağlantısı ve otomasyon kurulumu gerekir.")
                noticePreferenceLabels.forEach { (key, label) ->
                    val options = mapOf("Tanımlanmadı" to null, "Açık tercih" to true, "Kapalı tercih" to false)
                    val selected = options.entries.first { it.value == notices[key] }.key
                    PreferenceChoice(label, selected, options.keys.toList(), editable) {
                        notices = notices + (key to options[it])
                        saved = false
                    }
                }
                if (saved) Text("Kurum tercihleri kaydedildi.")
                if (canWrite) Button(
                    modifier = Modifier.fillMaxWidth(), enabled = editable, onClick = {
                        if (busy) return@Button
                        busy = true
                        saved = false
                        error = null
                        scope.launch {
                            try {
                                val maximum = size.toIntOrNull()
                                require(maximum != null && maximum in 1..shown.maxSizeMb) { "Geçerli bir belge boyutu girin." }
                                val fields = mapOf(
                                    "school_name" to schoolName.trim(), "academic_year" to year.trim().ifEmpty { null },
                                    "default_currency" to currency, "max_upload_size_mb" to maximum,
                                    "allowed_file_types" to shown.normalizeExtensions(extensions),
                                ) + notices
                                if (fields != shown.values) {
                                    snapshot = client.save(shown, fields)
                                    saved = true
                                }
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                error = failure.message ?: "Kurum tercihleri kaydedilemedi."
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text(if (busy) "Kaydediliyor…" else "Kaydet") }
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Kapat") }
            }
        }
    }
}

@Composable
internal fun PreferenceChoice(label: String, selected: String, options: List<String>, enabled: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label)
        OutlinedButton(onClick = { expanded = true }, enabled = enabled) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { value ->
                DropdownMenuItem(text = { Text(value) }, onClick = { expanded = false; onSelect(value) })
            }
        }
    }
}
