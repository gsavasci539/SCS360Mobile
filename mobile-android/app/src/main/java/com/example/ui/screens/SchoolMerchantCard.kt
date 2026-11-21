package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.NativeSchoolMerchantClient
import com.example.data.remote.SchoolMerchantHttpException
import com.example.data.remote.SchoolMerchantRequest
import com.example.data.remote.SchoolMerchantStatus
import com.example.data.remote.newRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val merchantFieldNames = mapOf(
    "api_key" to "API anahtarı", "secret_key" to "Gizli anahtar", "merchant_id" to "Üye işyeri numarası",
    "merchant_key" to "Üye işyeri anahtarı", "merchant_salt" to "Merchant salt",
    "app_id" to "Uygulama kimliği", "app_secret" to "Uygulama gizli anahtarı",
)

@Composable
internal fun SchoolMerchantCard(organizationId: String) {
    var open by remember(organizationId) { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kurum tahsilat hesabı", style = MaterialTheme.typography.titleMedium)
            Text("Öğrenci tahsilatlarında kullanılan sağlayıcı ayarları")
            OutlinedButton(onClick = { open = true }) { Text("Ödeme entegrasyonunu aç") }
        }
    }
    if (open) SchoolMerchantDialog(organizationId) { open = false }
}

@Composable
private fun SchoolMerchantDialog(organizationId: String, close: () -> Unit) {
    val client = remember(organizationId) { NativeSchoolMerchantClient(organizationId) }
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<SchoolMerchantStatus?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf(false) }
    var providerId by remember { mutableStateOf("") }
    var environment by remember { mutableStateOf("") }
    var enabled by remember { mutableStateOf(false) }
    var replaceAccount by remember { mutableStateOf(false) }
    var credentials by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pending by remember { mutableStateOf<SchoolMerchantRequest?>(null) }

    fun applyStatus(status: SchoolMerchantStatus) {
        snapshot = status
        providerId = status.currentProvider.orEmpty()
        environment = status.environment.orEmpty()
        enabled = status.enabled
        replaceAccount = false
        credentials = emptyMap()
        pending = null
        conflict = false
    }

    LaunchedEffect(client, reload) {
        busy = true
        try {
            applyStatus(client.status())
            error = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "Ödeme hesabı yüklenemedi."
        } finally {
            busy = false
        }
    }

    fun save(request: SchoolMerchantRequest) {
        if (busy) return
        pending = request
        busy = true
        success = false
        error = null
        scope.launch {
            try {
                applyStatus(client.save(request))
                success = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                val status = (failure as? SchoolMerchantHttpException)?.code
                if (status in listOf(401, 403, 409)) {
                    pending = null
                    credentials = emptyMap()
                    conflict = true
                } else if (status == 422) {
                    pending = null
                }
                error = failure.message ?: "Kaydedilen ödeme hesabı doğrulanamadı."
            } finally {
                busy = false
            }
        }
    }

    val current = snapshot
    val provider = current?.providers?.firstOrNull { it.id == providerId }
    val editable = !busy && pending == null && !conflict
    Dialog(onDismissRequest = { if (!busy) close() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 560.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Kurum tahsilat hesabı", style = MaterialTheme.typography.titleLarge)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (success) Text("Ödeme ayarları kaydedildi.")
                if (current == null) {
                    if (busy || error == null) Text("Ödeme ayarları yükleniyor…")
                    else OutlinedButton(onClick = { reload++ }) { Text("Tekrar dene") }
                } else if (!current.canManage) {
                    Text("Ödeme hesabını kurum sahibi veya seçili kurumun platform yöneticisi yönetebilir.")
                } else {
                    Text(when {
                        !current.configured -> "Üye işyeri bilgileri eksik."
                        !current.enabled -> "Hesap kayıtlı ve kapalı."
                        !current.available -> "Sunucu yapılandırması kontrol edilmeli."
                        current.environment == "SANDBOX" -> "Sandbox yapılandırması kayıtlı."
                        else -> "Canlı ortam yapılandırması kayıtlı."
                    })
                    if (!current.callbackConfigured) Text("Sunucuda ödeme dönüş adresi gerekiyor.")
                    if (!current.portalConfigured) Text("Sunucuda ödeme sonuç sayfası adresi gerekiyor.")
                    if (!current.encryptionConfigured) Text("Sunucuda kurum ödeme şifreleme anahtarı gerekiyor.")
                    if (!current.productionCompatible) Text("Seçili sağlayıcı ortamı sunucu ortamıyla uyumsuz.")
                    Text("SCS360 üyelik ödemeleri ayrı hesaptadır. Sağlayıcı kabulü ayrıca doğrulanır.")
                    if (current.bankPosNote.isNotBlank()) Text(current.bankPosNote)
                    val request = pending
                    if (request != null) {
                        OutlinedButton(onClick = { save(request) }, enabled = !busy) { Text("Aynı kaydı tekrar dene") }
                    }
                    if (conflict) OutlinedButton(onClick = { reload++ }, enabled = !busy) { Text("Güncel ayarları yükle") }
                    PreferenceChoice("Ödeme sağlayıcısı", provider?.name ?: "Seçin", current.providers.map { it.name }, editable) { name ->
                        providerId = current.providers.first { it.name == name }.id
                        credentials = emptyMap()
                        success = false
                    }
                    val environments = mapOf("Sandbox" to "SANDBOX", "Canlı" to "LIVE")
                    PreferenceChoice("Ödeme ortamı", environments.entries.firstOrNull { it.value == environment }?.key ?: "Seçin",
                        environments.keys.toList(), editable) {
                        environment = environments.getValue(it)
                        success = false
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = enabled, onCheckedChange = { enabled = it; success = false }, enabled = editable)
                        Text("Online öğrenci ödemesi")
                    }
                    if (current.accountId != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = replaceAccount, onCheckedChange = { replaceAccount = it; success = false }, enabled = editable)
                        Text("Yeni üye işyeri hesabı kullan")
                    }
                    provider?.credentialFields?.forEach { field ->
                        val recorded = provider.id == current.currentProvider && !replaceAccount && field in current.savedCredentials
                        OutlinedTextField(
                            value = credentials[field].orEmpty(),
                            onValueChange = { value -> if (value.length <= 2048) credentials = credentials + (field to value); success = false },
                            label = { Text(merchantFieldNames[field] ?: field) },
                            placeholder = if (recorded) ({ Text("Kayıtlı") }) else null,
                            modifier = Modifier.fillMaxWidth(), enabled = editable, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            visualTransformation = PasswordVisualTransformation(),
                        )
                    }
                    Button(
                        enabled = editable && provider != null && environment.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val selected = provider ?: return@Button
                            try {
                                save(current.newRequest(selected, environment, enabled, replaceAccount, credentials))
                            } catch (failure: IllegalArgumentException) {
                                error = failure.message
                                success = false
                            }
                        }
                    ) { Text(if (busy) "Kaydediliyor…" else "Ödeme ayarlarını kaydet") }
                }
                TextButton(onClick = close, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Kapat") }
            }
        }
    }
}
