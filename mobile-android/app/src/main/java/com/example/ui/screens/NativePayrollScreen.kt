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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import java.text.NumberFormat

private val recordsStatus = mapOf("DRAFT" to "Taslak", "APPROVED" to "Onaylandı", "PAID" to "Ödendi")
private val hoursStatus = mapOf("PRESENT" to "Çalıştı", "ABSENT" to "Gelmedi", "LEAVE" to "İzinli", "SICK" to "Raporlu")
private val payrollZone = ZoneId.of("Europe/Istanbul")

@Composable
internal fun NativePayrollScreen(state: Okul360UiState, onBack: () -> Unit) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "payroll.read") ||
        user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) {
        Column(Modifier.padding(16.dp)) {
            Text("Personel bordrosu için yetkili çalışma alanını açın.")
            TextButton(onClick = onBack) { Text("Geri") }
        }
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        PayrollWorkspace(remember { NativePayrollClient(state.selectedBranchId) },
            NativeNavigationPolicy.has(user, "payroll.write"), onBack)
    }
}

private fun payrollAmount(value: String): String = value.toBigDecimalOrNull()?.let {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("tr-TR")).format(it)
}.orEmpty()

@Composable
private fun PayrollWorkspace(client: NativePayrollClient, canWrite: Boolean, onBack: () -> Unit) {
    var period by remember { mutableStateOf("MONTHLY") }
    var monthInput by remember { mutableStateOf("") }
    var staff by remember { mutableStateOf<Pair<String, String>?>(null) }
    var kind by remember { mutableStateOf(PayrollRecords.RECORDS) }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    var selecting by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<JSONObject?>(null) }
    var approving by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    val metadata by payrollLoad(revision) { client.options() }
    val today = metadata?.getOrNull()?.payrollText("today").orEmpty()
    LaunchedEffect(today) { if (monthInput.isBlank() && today.length >= 7) monthInput = today.take(7) }
    val month = runCatching { YearMonth.parse(monthInput.trim()) }.getOrNull()?.takeIf { it.year in 2020..2100 }
    val staffId = staff?.first.orEmpty()
    val records by payrollLoad(kind, month, period, staffId, page, revision) {
        if (month == null) emptyList() else client.list(kind, page, period, month, staffId)
    }
    val summary by payrollLoad(month, period, staffId, revision) {
        if (month == null) null else client.summary(period, month, staffId)
    }
    val coroutine = rememberCoroutineScope()
    fun write(action: suspend () -> String) {
        if (busy) return
        notice = null
        busy = true
        coroutine.launch {
            try {
                notice = action()
                revision++
            } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                notice = "Personel kaydı doğrulanamadı. ${failure.message.orEmpty()}"
            } finally { busy = false }
        }
    }
    val rows = records?.getOrNull().orEmpty()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Personel Bordro ve Puantaj", style = MaterialTheme.typography.headlineSmall)
            TextButton(enabled = !busy, onClick = onBack) { Text("Geri") }
            notice?.let { Text(it) }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Personel kayıtlarını yenile") }
            TextButton(enabled = !busy, onClick = { selecting = true }) { Text(staff?.second ?: "Tüm yetkili personel") }
            PayrollField(monthInput, "Dönem ayı (YYYY-AA)", 7, busy) { monthInput = it; page = 0 }
            TextButton(enabled = !busy && period != "MONTHLY", onClick = { period = "MONTHLY"; page = 0 }) { Text("Aylık") }
            TextButton(enabled = !busy && period != "QUARTERLY", onClick = { period = "QUARTERLY"; page = 0 }) { Text("Üç aylık") }
            if (metadata?.isFailure == true) Text("Dönem ve personel verileri alınamadı.")
            if (month == null && monthInput.isNotEmpty()) Text("2020–2100 arasında geçerli bir dönem ayı girin.")
            summary?.getOrNull()?.let { result ->
                Text("Bordro ${result.getInt("total_payroll")} · Onay bekleyen ${result.getInt("pending_approvals")}")
                Text("Toplam net ${payrollAmount(result.payrollText("net_salary_total"))}")
                Text("Puantaj ${result.getInt("total_timesheets")} · Ek ders ${payrollAmount(result.payrollText("additional_course_total"))}")
            }
        }
        items(PayrollRecords.entries) { tab ->
            TextButton(enabled = tab != kind && !busy, onClick = { kind = tab; page = 0; notice = null }) { Text(tab.label) }
        }
        if (canWrite) item {
            Button(enabled = month != null && staff != null && !busy, onClick = { editing = true; target = null; notice = null }) {
                Text(when (kind) {
                    PayrollRecords.RECORDS -> "Bordro kaydet"
                    PayrollRecords.TIMESHEETS -> "Puantaj kaydet"
                    PayrollRecords.ADDITIONAL -> "Ek ders kaydet"
                })
            }
            if (staff == null) Text("Kayıt için personel seçin.")
        }
        if (rows.isEmpty() && month != null) item { Text(when {
            records == null -> "Personel kayıtları yükleniyor…"
            records?.isFailure == true -> "Personel kayıtları alınamadı."
            else -> "Bu kapsamda personel kaydı yok."
        }) }
        items(rows.take(20), key = { it.getString("id") }) { row ->
            val state = row.payrollText("status")
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.payrollText("staff_name").ifBlank { "Personel kaydı" }, style = MaterialTheme.typography.titleMedium)
                    val fields = when (kind) {
                        PayrollRecords.RECORDS -> listOf("Net ${payrollAmount(row.payrollText("net_salary"))}",
                            recordsStatus[state].orEmpty(), row.payrollText("payment_date"), row.payrollText("payment_method"),
                            row.payrollText("approved_by_name"), row.payrollText("approved_at"))
                        PayrollRecords.TIMESHEETS -> listOf(row.getString("date"), "${row.getString("work_hours")} saat",
                            hoursStatus[state].orEmpty(), row.payrollText("check_in_time"), row.payrollText("check_out_time"))
                        PayrollRecords.ADDITIONAL -> listOf("${row.getString("total_hours")} saat",
                            payrollAmount(row.payrollText("hourly_rate")), "Toplam ${payrollAmount(row.payrollText("total_amount"))}",
                            row.payrollText("approved_at"))
                    }
                    (fields + row.payrollText("notes")).filter(String::isNotBlank).forEach { Text(it) }
                    val editable = when (kind) {
                        PayrollRecords.RECORDS -> state == "DRAFT"
                        PayrollRecords.TIMESHEETS -> true
                        PayrollRecords.ADDITIONAL -> row.payrollText("approved_at").isBlank()
                    }
                    if (canWrite && editable) TextButton(enabled = !busy, onClick = { target = row; editing = true; notice = null }) {
                        Text("Kaydı düzenle")
                    }
                    if (canWrite && kind == PayrollRecords.RECORDS && state == "DRAFT") {
                        TextButton(enabled = !busy, onClick = { approving = row; notice = null }) { Text("Bordroyu onayla") }
                    }
                }
            }
        }
        if (page > 0 || rows.size > 20) item {
            if (page > 0) TextButton(enabled = !busy, onClick = { page-- }) { Text("Önceki sayfa") }
            if (rows.size > 20) TextButton(enabled = !busy, onClick = { page++ }) { Text("Sonraki sayfa") }
        }
    }
    if (selecting) PayrollStaffPicker(client, staffId, { selecting = false }) { selection ->
        staff = selection; page = 0; selecting = false
    }
    val record = target
    val actor = record?.let { it.getString("staff_id") to it.payrollText("staff_name").ifBlank { "Personel kaydı" } } ?: staff
    if (canWrite && editing && actor != null && month != null) PayrollEditor(kind, actor, period, month, today,
        record, busy, notice, { editing = false }) { body ->
        write { client.save(kind, body); editing = false; page = 0; "Personel kaydı sunucudan doğrulandı." }
    }
    approving?.let { row ->
        AlertDialog(onDismissRequest = { if (!busy) approving = null }, title = { Text("Bordroyu onayla") },
            text = { Text(notice ?: "${row.payrollText("staff_name").ifBlank { "Personel kaydı" }} · ${payrollAmount(row.payrollText("net_salary"))}") },
            confirmButton = { Button(enabled = !busy, onClick = { write {
                client.approve(row); approving = null; "Bordro onayı sunucudan doğrulandı."
            } }) { Text("Onayla") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { approving = null }) { Text("Vazgeç") } })
    }
}

@Composable
private fun PayrollStaffPicker(client: NativePayrollClient, current: String, close: () -> Unit, select: (Pair<String, String>?) -> Unit) {
    var search by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    val result by payrollLoad(search, refresh) {
        if (search.isNotBlank()) delay(250)
        client.options(search.trim(), current)
    }
    val metadata = result?.getOrNull()
    val array = metadata?.optJSONArray("staff") ?: JSONArray()
    ConsentFormDialog("Yetkili personel seçin", "native_payroll_staff", false, close, fields = {
        item { PayrollField(search, "Ad veya soyad ara", 100, false) { search = it } }
        item { TextButton(onClick = { select(null) }) { Text("Tüm yetkili personel") } }
        if (array.length() == 0) item { when {
            result == null -> Text("Personel yükleniyor…")
            result?.isFailure == true -> TextButton(onClick = { refresh++ }) { Text("Personeli yeniden yükle") }
            else -> Text("Bu kapsamda personel bulunamadı.")
        } }
        items(array.length()) { index ->
            val row = array.getJSONObject(index)
            TextButton(onClick = { select(row.getString("id") to row.getString("name")) }) {
                Text(row.getString("name"))
            }
        }
        if (metadata?.optBoolean("more_staff") == true) item { Text("Daha fazla sonuç için aramayı daraltın.") }
    }, confirm = {})
}

private data class PayrollFormField(val key: String, val label: String, val maxLength: Int, val numeric: Boolean = false,
    val required: Boolean = false)

@Composable
private fun PayrollEditor(kind: PayrollRecords, actor: Pair<String, String>, period: String, month: YearMonth, today: String,
    initial: JSONObject?, busy: Boolean, notice: String?, close: () -> Unit, save: (JSONObject) -> Unit) {
    val fields = when (kind) {
        PayrollRecords.RECORDS -> listOf(PayrollFormField("base_salary", "Taban maaş", 16, true, true),
            PayrollFormField("additional_course_payment", "Ek ders ödemesi", 16, true),
            PayrollFormField("bonus", "Prim", 16, true), PayrollFormField("advance_payment", "Avans", 16, true),
            PayrollFormField("deductions", "Kesinti", 16, true), PayrollFormField("payment_date", "Kayıt tarihi (YYYY-AA-GG)", 10),
            PayrollFormField("payment_method", "Ödeme yöntemi", 50), PayrollFormField("bank_account", "Banka hesap bilgisi", 50),
            PayrollFormField("notes", "Notlar", 2000))
        PayrollRecords.TIMESHEETS -> listOf(PayrollFormField("date", "Puantaj tarihi (YYYY-AA-GG)", 10, required = true),
            PayrollFormField("check_in_time", "Giriş saati (SS:DD)", 8), PayrollFormField("check_out_time", "Çıkış saati (SS:DD)", 8),
            PayrollFormField("work_hours", "Çalışma saati", 5, true, true), PayrollFormField("notes", "Notlar", 2000))
        PayrollRecords.ADDITIONAL -> listOf(PayrollFormField("total_hours", "Ek ders saati", 6, true, true),
            PayrollFormField("hourly_rate", "Saatlik ücret", 11, true), PayrollFormField("notes", "Notlar", 2000))
    }
    val first = month.atDay(1)
    val last = month.plusMonths(if (period == "QUARTERLY") 2 else 0).atEndOfMonth()
    var values by remember { mutableStateOf(fields.associate { field ->
        val old = initial?.payrollText(field.key).orEmpty()
        val value = when {
            field.key in setOf("check_in_time", "check_out_time") && old.isNotBlank() ->
                OffsetDateTime.parse(old).atZoneSameInstant(payrollZone).toLocalTime().toString()
            initial != null -> old
            field.key == "date" -> today.takeIf { runCatching { LocalDate.parse(it) in first..last }.getOrDefault(false) } ?: first.toString()
            else -> ""
        }
        field.key to value
    }) }
    var status by remember { mutableStateOf(initial?.payrollText("status").takeUnless { it.isNullOrBlank() } ?: "PRESENT") }
    val normalized = values.mapValues { it.value.trim() }
    val selectedDate = normalized["date"]?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val payDate = normalized["payment_date"].orEmpty()
    val validDate = if (kind == PayrollRecords.TIMESHEETS) selectedDate != null && selectedDate in first..last
        else payDate.isEmpty() || runCatching { LocalDate.parse(payDate) }.isSuccess
    fun timeValue(key: String) = normalized[key]?.takeIf(String::isNotBlank)?.let { raw -> runCatching { LocalTime.parse(raw) }.getOrNull() }
    val start = timeValue("check_in_time")
    val end = timeValue("check_out_time")
    val validTimes = (normalized["check_in_time"].isNullOrBlank() || start != null) &&
        (normalized["check_out_time"].isNullOrBlank() || end != null) && (start == null || end == null || !end.isBefore(start))
    fun number(raw: String) = raw.replace(',', '.').takeIf { it.matches(Regex("\\d+(\\.\\d{1,2})?")) }?.toBigDecimalOrNull()
    val numeric = fields.filter { it.numeric }
    val validNumbers = numeric.all {
        val value = normalized[it.key].orEmpty()
        val maximum = when (it.key) {
            "work_hours" -> "24".toBigDecimal()
            "total_hours" -> "999.99".toBigDecimal()
            "hourly_rate" -> "99999999.99".toBigDecimal()
            else -> "9999999999999.99".toBigDecimal()
        }
        (value.isBlank() && !it.required) || number(value)?.let { amount -> amount <= maximum } == true
    }
    val body = JSONObject().put("staff_id", actor.first)
    if (kind == PayrollRecords.TIMESHEETS) body.put("timesheet_date", selectedDate?.toString() ?: JSONObject.NULL).put("status", status)
        .put("check_in_time", start?.let { selectedDate?.atTime(it)?.atZone(payrollZone)?.toOffsetDateTime()?.toString() } ?: JSONObject.NULL)
        .put("check_out_time", end?.let { selectedDate?.atTime(it)?.atZone(payrollZone)?.toOffsetDateTime()?.toString() } ?: JSONObject.NULL)
    else body.put("period", period).put("year", month.year).put("month", month.monthValue)
    for (field in fields) {
        if (field.key in setOf("date", "check_in_time", "check_out_time")) continue
        val value = normalized[field.key].orEmpty()
        val wire = if (field.numeric) number(value)?.toPlainString() ?: if (field.key != "hourly_rate") "0" else null
        else value.ifBlank { null }
        body.put(field.key, wire ?: JSONObject.NULL)
    }
    ConsentFormDialog("Personel kaydı", "native_payroll_editor", busy, close, fields = {
        item { Text(actor.second) }
        if (kind != PayrollRecords.TIMESHEETS) item { Text("$month · ${if (period == "QUARTERLY") "Üç aylık" else "Aylık"}") }
        if (kind == PayrollRecords.TIMESHEETS) items(hoursStatus.entries.toList(), key = { it.key }) { (code, label) ->
            TextButton(enabled = !busy, onClick = { status = code }) { Text(if (status == code) "✓ $label" else label) }
        }
        items(fields, key = { it.key }) { field ->
            PayrollField(values[field.key].orEmpty(), field.label, field.maxLength, busy) { values = values + (field.key to it) }
        }
        notice?.let { item { Text(it) } }
    }, confirm = {
        Button(enabled = !busy && validDate && validTimes && validNumbers &&
            fields.filter { it.required }.all { !normalized[it.key].isNullOrBlank() }, onClick = { save(body) }) { Text("Kaydet") }
    })
}

@Composable
private fun PayrollField(value: String, label: String, max: Int, busy: Boolean, update: (String) -> Unit) {
    OutlinedTextField(value, { update(it.take(max)) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(label) })
}

@Composable
private fun <T> payrollLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> = key(keys.toList()) {
    produceState<Result<T>?>(null) {
        value = try { Result.success(load()) } catch (failure: Exception) {
            if (failure is CancellationException) throw failure
            Result.failure(failure)
        }
    }
}
