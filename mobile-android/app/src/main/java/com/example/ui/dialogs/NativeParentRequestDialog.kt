package com.example.ui.dialogs

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
import com.example.data.remote.*
import com.example.data.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

@Composable
fun NativeParentRequestDialog(studentId: String, actor: User, kind: String, onDismiss: () -> Unit, onRecorded: () -> Unit) {
    val captured = remember(studentId, kind) { ApiClientProvider.interceptor.captureScope() }
    val client = remember(studentId, kind) { NativeParentPortalClient() }
    val coroutine = rememberCoroutineScope()
    val labels = remember(kind) { when (kind) {
        "MEDICINE" -> linkedMapOf("medicine_name" to "İlaç adı", "dosage" to "Doz", "time_to_give" to "Verilmesi istenen saat", "instructions" to "Açıklama")
        "EXCUSE" -> linkedMapOf("start_date" to "Başlangıç (YYYY-MM-DD)", "end_date" to "Bitiş (YYYY-MM-DD)", "reason_type" to "Mazeret türü (ILLNESS/FAMILY/OTHER)", "description" to "Açıklama")
        else -> linkedMapOf("authorized_person_name" to "Teslim alacak kişinin adı", "authorized_person_id" to "Kimlik / pasaport numarası", "relationship" to "Yakınlık", "phone" to "Telefon", "valid_from" to "Başlangıç (YYYY-MM-DD)", "valid_until" to "Bitiş (YYYY-MM-DD, isteğe bağlı)", "notes" to "Not")
    } }
    val values = remember(studentId, kind) { mutableStateMapOf<String, String>().apply { labels.keys.forEach { put(it, "") } } }
    val requestId = remember(studentId, kind) { UUID.randomUUID().toString() }
    var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }; var ack by remember { mutableStateOf<String?>(null) }
    val permission = when (kind) { "MEDICINE" -> "parent.medicine_request"; "EXCUSE" -> "parent.send_excuse"; else -> "dismissal.write" }
    val canWrite = actor.is_super_admin == true || permission in actor.permissions
    if (!captured.current()) { LaunchedEffect(captured) { onDismiss() }; return }
    val acknowledgement = ack
    val listState = key(acknowledgement != null) { androidx.compose.foundation.lazy.rememberLazyListState() }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp).imePadding(), shape = MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("parent_request"), state = listState, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Text(when (kind) { "MEDICINE" -> "İlaç talebi"; "EXCUSE" -> "Mazeret talebi"; else -> "Teslim alma yetkisi" }, style = MaterialTheme.typography.titleLarge) }
                if (!canWrite) item { Text("Bu hesap bu işlemi yapmaya yetkili değil.") }
                if (acknowledgement == null) {
                    items(labels.keys.toList()) { key -> OutlinedTextField(values[key].orEmpty(), { values[key] = it }, label = { Text(labels.getValue(key)) }, enabled = canWrite && !busy && !attempted, modifier = Modifier.fillMaxWidth().testTag("parent_request_$key")) }
                    item { if (error.isNotBlank()) Text(error) }
                    item { Button(enabled = canWrite && !busy && !attempted, modifier = Modifier.heightIn(min = 48.dp).testTag("parent_request_save"), onClick = {
                        if (!captured.current()) return@Button
                        busy = true; error = ""; attempted = true
                        coroutine.launch {
                            try {
                                val body = JSONObject().apply { values.forEach { (key, value) -> if (value.isNotBlank()) put(key, value.trim()) }; if (kind == "PICKUP") put("request_id", requestId) }
                                val result = client.submit(studentId, kind, body); captured.ensureCurrent(); ack = result.getString("id")
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (failure: Exception) {
                                if (captured.current()) {
                                    if ((failure as? RegistryHttpException)?.code in listOf(400, 403, 404, 409, 422)) attempted = false
                                    error = if (attempted) "Sonuç doğrulanamadı. Yeni talep açmadan önce talepleri yenileyip kontrol edin." else "Talep kaydedilemedi. Yetki ve form bilgilerini kontrol edin."
                                }
                            } finally { if (captured.current()) busy = false }
                        }
                    }) { Text("Sunucuya kaydet") } }
                } else item { Text("Talep sunucuda kaydedildi: $acknowledgement", Modifier.testTag("parent_request_ack")) }
                item { TextButton(enabled = !busy, modifier = Modifier.heightIn(min = 48.dp), onClick = { if (captured.current() && ack != null) onRecorded(); onDismiss() }) { Text("Kapat") } }
            }
        }
    }
}
