package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.InstitutionIntegration
import com.example.data.remote.IntegrationSettingsRecord
import com.example.data.remote.NativeIntegrationSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun OrganizationIntegrationsCard(organizationId: String, canWrite: Boolean) {
    var open by remember(organizationId) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Servis bağlantıları", style = MaterialTheme.typography.titleMedium)
            Text("SMTP, SMS ve WhatsApp kayıtları")
            OutlinedButton(onClick = { open = true }) { Text("Servis bağlantılarını aç") }
        }
    }
    if (open) IntegrationSettingsDialog(organizationId, canWrite) { open = false }
}

@Composable
private fun IntegrationSettingsDialog(organizationId: String, canWrite: Boolean, onDismiss: () -> Unit) {
    val client = remember(organizationId) { NativeIntegrationSettings(organizationId) }
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<Map<InstitutionIntegration, IntegrationSettingsRecord>?>(null) }
    var selected by remember { mutableStateOf(InstitutionIntegration.SMTP) }
    var drafts by remember { mutableStateOf<Map<InstitutionIntegration, Map<String, String>>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    LaunchedEffect(client, reload) {
        error = null
        try {
            records = client.load()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Servis bağlantıları alınamadı."
        }
    }
    val loaded = records
    val current = loaded?.get(selected)
    val patch = drafts[selected].orEmpty()
    val editable = loaded != null && canWrite && !busy && current?.reviewRequired != true

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Servis bağlantıları") },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (loaded == null) {
                    if (error == null) Text("Servis bağlantıları yükleniyor…")
                    else OutlinedButton(onClick = { reload++ }) { Text("Tekrar dene") }
                } else {
                    if (saved) Text("Servis bağlantısı kaydedildi.")
                    PreferenceChoice("Sağlayıcı", selected.label, InstitutionIntegration.entries.map { it.label }, !busy) { label ->
                        selected = InstitutionIntegration.entries.first { it.label == label }
                        saved = false
                        error = null
                    }
                    Text(when {
                        current == null -> "Kayıt bulunmuyor."
                        current.reviewRequired -> "Kayıt inceleme gerektiriyor."
                        current.active -> "Kayıt durumu etkin."
                        else -> "Kayıt durumu pasif."
                    })
                    Text("Gönderim kurulumu sağlayıcı hesabıyla doğrulanır.")
                    selected.fields.forEach { (key, label) ->
                        val original = current?.publicFields?.get(key)?.toString().orEmpty()
                        val blocked = key in current?.blockedFields.orEmpty()
                        OutlinedTextField(
                            value = patch[key] ?: original,
                            onValueChange = { value ->
                                val revised = if (value == original) patch - key else patch + (key to value)
                                drafts = drafts + (selected to revised)
                                saved = false
                                error = null
                            },
                            label = { Text(label) },
                            supportingText = if (blocked) ({ Text("Alan inceleme gerektiriyor.") }) else null,
                            modifier = Modifier.fillMaxWidth(), enabled = editable && !blocked, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = if (key == "port") KeyboardType.Number else KeyboardType.Text),
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (canWrite && selected.fields.isNotEmpty() && loaded != null) Button(
                enabled = editable && patch.isNotEmpty(),
                onClick = {
                    if (busy) return@Button
                    val provider = selected
                    busy = true
                    saved = false
                    error = null
                    scope.launch {
                        try {
                            records = client.save(provider, current, patch)
                            drafts = drafts - provider
                            saved = true
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (failure: Exception) {
                            error = failure.message ?: "Servis bağlantısı doğrulanamadı."
                        } finally {
                            busy = false
                        }
                    }
                }
            ) { Text(if (busy) "Kaydediliyor…" else "Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Kapat") } },
    )
}
