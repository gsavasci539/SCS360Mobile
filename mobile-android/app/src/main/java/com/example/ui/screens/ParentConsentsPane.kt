package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.GuardianConsentDocument
import com.example.data.remote.NativeConsentClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val PARENT_CONSENT_PAGE_SIZE = 20

@Composable
internal fun ParentConsentsPane(studentId: String, canDecide: Boolean) {
    val client = remember(studentId) { NativeConsentClient() }
    val actions = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var records by remember { mutableStateOf<List<GuardianConsentDocument>?>(null) }
    var selection by remember { mutableStateOf<GuardianConsentDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    fun reload(targetPage: Int = page) {
        records = null
        error = null
        page = targetPage
        revision++
    }
    LaunchedEffect(studentId, revision) {
        try {
            records = client.guardianDocuments(studentId, page * PARENT_CONSENT_PAGE_SIZE, PARENT_CONSENT_PAGE_SIZE + 1)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            error = failure.message ?: "İzin belgeleri yüklenemedi."
        }
    }
    LazyColumn(Modifier.fillMaxSize().testTag("parent_consent_records"),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("İzin ve onay belgeleri", style = MaterialTheme.typography.titleLarge) }
        val rows = records
        when {
            error != null -> item {
                Text(error.orEmpty())
                OutlinedButton(onClick = { reload() }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Yeniden yükle") }
            }
            rows == null -> item { Text("İzin belgeleri yükleniyor…") }
            rows.isEmpty() -> item { Text("İzin belgesi bulunamadı.") }
            else -> items(rows.take(PARENT_CONSENT_PAGE_SIZE), key = { it.document.id }) { row ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(row.document.title, style = MaterialTheme.typography.titleMedium)
                        Text("Sürüm ${row.document.version}")
                        Text("Yürürlük ${row.document.effectiveDate}")
                        Text(when (row.status) {
                            "APPROVED" -> "Onaylandı"
                            "REJECTED" -> "Reddedildi"
                            "PENDING" -> "Bekliyor"
                            "REVIEW_REQUIRED" -> "İnceleme gerekli"
                            else -> row.status
                        })
                        row.decidedAt?.let { Text(it) }
                        TextButton(onClick = { actionError = null; selection = row }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Belgenin tam metnini oku")
                        }
                    }
                }
            }
        }
        if (rows != null && (page > 0 || rows.size > PARENT_CONSENT_PAGE_SIZE)) item {
            Column {
                Text("Sayfa ${page + 1}")
                OutlinedButton(enabled = page > 0, onClick = { reload(page - 1) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Önceki") }
                OutlinedButton(enabled = rows.size > PARENT_CONSENT_PAGE_SIZE, onClick = { reload(page + 1) },
                    modifier = Modifier.heightIn(min = 48.dp)) { Text("Sonraki") }
            }
        }
    }
    selection?.let { row ->
        var signedName by remember(row) { mutableStateOf("") }
        var acknowledged by remember(row) { mutableStateOf(false) }
        val maySubmit = canDecide && row.canDecide
        fun submit(status: String) {
            if (!maySubmit || busy || !acknowledged || signedName.trim().length < 2) return
            busy = true
            actionError = null
            actions.launch {
                try {
                    client.saveGuardianDecision(studentId, row, status, signedName.trim())
                    selection = null
                    reload()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    actionError = failure.message ?: "Veli kararı doğrulanamadı."
                } finally {
                    busy = false
                }
            }
        }
        ConsentFormDialog(row.document.title, "parent_consent_document", busy, { selection = null }, {
            item { Text(row.document.content) }
            if (maySubmit) {
                item {
                    OutlinedTextField(signedName, { signedName = it.take(200) }, enabled = !busy, label = { Text("Adınız ve soyadınız") })
                }
                item {
                    Row {
                        Checkbox(checked = acknowledged, onCheckedChange = { acknowledged = it }, enabled = !busy,
                            modifier = Modifier.testTag("parent_consent_ack"))
                        Text("Bu sürümün tam metnini okudum.")
                    }
                }
            }
            actionError?.let { item { Text(it) } }
        }) {
            if (maySubmit) Column {
                val enabled = !busy && acknowledged && signedName.trim().length >= 2
                Button(enabled = enabled, onClick = { submit("APPROVED") }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Onaylıyorum") }
                OutlinedButton(enabled = enabled, onClick = { submit("REJECTED") }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Onay vermiyorum") }
            }
        }
    }
}
