package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodel.Okul360UiState
import java.math.BigDecimal
import java.time.LocalDate

/** User-entered draft only; submission never closes this dialog before server review. */
@Composable
fun FinanceInvoiceDraftDialog(state: Okul360UiState, onDismiss: () -> Unit,
    onCreate: (String, String, Double, String, String) -> Unit) {
    var student by remember { mutableStateOf("") }; var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }; var due by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val canWrite = state.currentUser.permissions.contains("finance.write") ||
        (state.currentUser.is_super_admin == true && !state.workspaceOrganizationId.isNullOrBlank())
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 600.dp).fillMaxWidth().imePadding()
            .heightIn(max = (LocalConfiguration.current.screenHeightDp * .9f).dp), shape = MaterialTheme.shapes.large) {
            LazyColumn(Modifier.padding(16.dp).testTag("invoice_draft_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("Tahakkuk taslağı", style = MaterialTheme.typography.titleLarge) }
                item { Text("Öğrenci, tutar ve vade bilgisini siz girin. Bu form resmi fatura veya makbuz üretmez.") }
                if (!canWrite) item { Text("Tahakkuk oluşturma yetkiniz yok; form salt okunur.") }
                item { Text("Öğrenci seçin") }
                state.students.forEach { row -> item(key = row.id) {
                    TextButton(onClick = { student = row.id }, enabled = canWrite && !submitted,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("invoice_student_${row.id}")) {
                        Text((if (student == row.id) "✓ " else "") + row.full_name)
                    }
                } }
                if (state.students.isEmpty()) item { Text("Erişilebilir öğrenci listesi henüz yüklenmedi.") }
                item { OutlinedTextField(title, { title = it }, label = { Text("Açıklama") }, enabled = canWrite && !submitted,
                    modifier = Modifier.fillMaxWidth().testTag("invoice_title")) }
                item { OutlinedTextField(amount, { amount = it }, label = { Text("Tutar") }, enabled = canWrite && !submitted,
                    modifier = Modifier.fillMaxWidth().testTag("invoice_amount")) }
                item { OutlinedTextField(due, { due = it }, label = { Text("Vade (YYYY-AA-GG)") }, enabled = canWrite && !submitted,
                    modifier = Modifier.fillMaxWidth().testTag("invoice_due")) }
                error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (submitted) item { Text("İstek gönderildi. Sonucu sunucu listesinden kontrol edin; bu taslak yeni bir istekle tekrarlanmaz.") }
                item { Button(enabled = canWrite && !submitted, modifier = Modifier.heightIn(min = 48.dp).testTag("invoice_submit"), onClick = {
                    val value = amount.trim().replace(',', '.').toBigDecimalOrNull()
                    val date = runCatching { LocalDate.parse(due).toString() }.getOrNull()
                    if (state.students.none { it.id == student } || value == null || value <= BigDecimal.ZERO ||
                        value > BigDecimal("9999999999.99") || value.stripTrailingZeros().scale() > 2 || date == null || title.isBlank()) {
                        error = "Öğrenci, açıklama, geçerli kuruşlu tutar ve vade girin."
                    } else { error = null; submitted = true; onCreate(student, title.trim(), value.toDouble(), date, title.trim()) }
                }) { Text("Taslağı sunucuya gönder") } }
                item { TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("invoice_close")) { Text("Kapat") } }
            }
        }
    }
}
