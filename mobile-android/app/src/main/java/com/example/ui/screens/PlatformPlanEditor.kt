package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SubscriptionPlan
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Locale

private val PlanCode = Regex("[A-Z0-9_-]{2,50}")
private val PlanPrice = Regex("[0-9]{1,10}(?:[.,][0-9]{1,2})?")
private val PlanPeriods = mapOf("Aylık" to "MONTHLY", "Yıllık" to "YEARLY")
private val PlanCurrencies = listOf("TRY", "USD", "EUR")

internal fun platformPlanDraft(
    id: String, code: String, name: String, price: String, currency: String, period: String,
    branches: String, students: String, users: String, active: Boolean,
): SubscriptionPlan {
    val normalized = code.trim().uppercase(Locale.ROOT)
    require(PlanCode.matches(normalized)) { "Geçerli bir paket kodu girin." }
    require(name.trim().length in 2..120) { "Paket adı 2 ile 120 karakter arasında olmalıdır." }
    require(PlanPrice.matches(price.trim())) { "En fazla iki ondalık basamaklı paket fiyatını girin." }
    require(currency in PlanCurrencies && period in PlanPeriods.values) { "Para birimini ve dönemi seçin." }
    fun capacity(value: String): Int = requireNotNull(value.trim().toIntOrNull()?.takeIf { it > 0 }) {
        "Tüm kapasite alanlarına pozitif tam sayı girin."
    }
    return SubscriptionPlan(id = id, code = normalized, name = name.trim(),
        price = price.trim().replace(',', '.').toDouble(), currency = currency, billing_period = period,
        branch_limit = capacity(branches), student_limit = capacity(students), user_limit = capacity(users), is_active = active)
}

@Composable
internal fun PlatformPlanEditor(original: SubscriptionPlan?, onSave: suspend (SubscriptionPlan) -> String?, onDismiss: () -> Unit) {
    var code by remember(original) { mutableStateOf(original?.code.orEmpty()) }
    var name by remember(original) { mutableStateOf(original?.name.orEmpty()) }
    var price by remember(original) { mutableStateOf(original?.price?.let { BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() }.orEmpty()) }
    var currency by remember(original) { mutableStateOf(original?.currency ?: "TRY") }
    var period by remember(original) { mutableStateOf(original?.billing_period ?: "MONTHLY") }
    var active by remember(original) { mutableStateOf(original?.is_active ?: true) }
    var branches by remember(original) { mutableStateOf(original?.branch_limit?.toString().orEmpty()) }
    var students by remember(original) { mutableStateOf(original?.student_limit?.toString().orEmpty()) }
    var users by remember(original) { mutableStateOf(original?.user_limit?.toString().orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(12.dp).widthIn(max = 540.dp).fillMaxWidth(), shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (original == null) "Paket oluştur" else "Paketi düzenle", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(code, { code = it }, label = { Text("Paket kodu") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(name, { name = it }, label = { Text("Paket adı") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(price, { price = it }, label = { Text("Fiyat") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                PreferenceChoice("Para birimi", currency, PlanCurrencies, !busy) { currency = it }
                PreferenceChoice("Dönem", PlanPeriods.entries.first { it.value == period }.key, PlanPeriods.keys.toList(), !busy) { period = PlanPeriods.getValue(it) }
                OutlinedTextField(branches, { branches = it }, label = { Text("Şube sınırı") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(students, { students = it }, label = { Text("Öğrenci sınırı") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(users, { users = it }, label = { Text("Kullanıcı sınırı") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                PreferenceChoice("Satış durumu", if (active) "Açık" else "Kapalı", listOf("Açık", "Kapalı"), !busy) { active = it == "Açık" }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    if (busy) return@Button
                    val draft = try {
                        platformPlanDraft(original?.id.orEmpty(), code, name, price, currency, period, branches, students, users, active)
                    } catch (failure: IllegalArgumentException) {
                        error = failure.message
                        return@Button
                    }
                    error = null
                    busy = true
                    scope.launch {
                        try {
                            error = onSave(draft)
                            if (error == null) onDismiss()
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } finally {
                            busy = false
                        }
                    }
                }) { Text(if (busy) "Kaydediliyor…" else "Paketi kaydet") }
                TextButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Vazgeç") }
            }
        }
    }
}
