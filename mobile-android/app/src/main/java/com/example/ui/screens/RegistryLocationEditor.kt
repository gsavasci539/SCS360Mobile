package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

internal data class RegistryEditorField(
    val key: String,
    val label: String,
    val initial: String = "",
    val keyboard: KeyboardType = KeyboardType.Text,
)

@Composable
internal fun RegistryLocationEditor(title: String, fields: List<RegistryEditorField>, active: Boolean? = null,
    onSave: suspend (Map<String, String>, Boolean?) -> String?, onClose: () -> Unit) {
    val values = remember { mutableStateMapOf(*fields.map { it.key to it.initial }.toTypedArray()) }
    var enabled by remember { mutableStateOf(active) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = { if (!busy) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 540.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                fields.forEach { field ->
                    OutlinedTextField(value = values.getValue(field.key), onValueChange = { values[field.key] = it },
                        enabled = !busy, label = { Text(field.label) }, singleLine = field.key != "address",
                        keyboardOptions = KeyboardOptions(keyboardType = field.keyboard),
                        modifier = Modifier.fillMaxWidth().testTag("registry_field_${field.key}"))
                }
                enabled?.let { selected ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Switch(checked = selected, onCheckedChange = { enabled = it }, enabled = !busy)
                        Text("Şube etkin")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("registry_save_error")) }
                Button(onClick = {
                    if (busy) return@Button
                    val draft = values.toMap()
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            error = onSave(draft, enabled)
                            if (error == null) onClose()
                        } finally {
                            busy = false
                        }
                    }
                }, enabled = !busy && values["name"].orEmpty().trim().length >= 2,
                    modifier = Modifier.fillMaxWidth().testTag("registry_save")) {
                    Text(if (busy) "Doğrulanıyor…" else "Kaydet")
                }
                TextButton(onClick = onClose, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Vazgeç") }
            }
        }
    }
}