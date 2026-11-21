package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.remote.*
import com.example.ui.components.financePresentationScope
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Composable
internal fun TransportScreen(state: Okul360UiState, onNavigateBack: () -> Unit) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "bus.read") ||
        (user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank())) {
        Text("Servis kayıtları için yetkili kurum çalışma alanını açın.", Modifier.padding(16.dp))
        return
    }
    key(financePresentationScope(state), state.serverBaseUrl) {
        val client = remember { NativeTransportClient(state.selectedBranchId) }
        var routeId by remember { mutableStateOf<String?>(null) }
        val canWrite = NativeNavigationPolicy.has(user, "bus.write")
        val selected = routeId
        if (selected == null) TransportRoutes(client, onNavigateBack) { routeId = it }
        else key(selected) { TransportRoutePane(client, selected, canWrite, onBack = { routeId = null }) }
    }
}

@Composable
private fun TransportRoutes(client: NativeTransportClient, onBack: () -> Unit, select: (String) -> Unit) {
    var search by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var revision by remember { mutableIntStateOf(0) }
    val results by transportLoad(search, page, revision) {
        if (search.isNotBlank()) delay(250)
        client.routes(search.trim(), 21, page * 20)
    }
    LazyColumn(Modifier.fillMaxSize().testTag("native_transport_routes"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Servis ve Ulaşım", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) { Text("Geri") }
            TransportField(search, "Rota adı veya plaka", 100) { search = it; page = 0 }
            TextButton(onClick = { revision++ }) { Text("Rotaları yenile") }
        }
        val routes = results?.getOrNull().orEmpty()
        if (routes.isEmpty()) item { Text(transportMessage(results, "Bu kapsamda servis rotası yok.")) }
        items(routes.take(20), key = { it.id }) { route ->
            TransportRecord(listOf(route.name, route.branchName, route.plate, route.driver, transportStatus(route.status))) {
                TextButton(onClick = { select(route.id) }) { Text("${route.name} rotasını aç") }
            }
        }
        if (page > 0 || routes.size > 20) item { TransportPager(page, routes.size, false) { page = it } }
    }
}

@Composable
private fun TransportRoutePane(client: NativeTransportClient, id: String, canWrite: Boolean, onBack: () -> Unit) {
    var revision by remember { mutableIntStateOf(0) }
    var stopsPage by remember { mutableIntStateOf(0) }
    var attendancePage by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) }
    var chosenDay by remember { mutableStateOf<LocalDate?>(null) }
    var dayText by remember { mutableStateOf<String?>(null) }
    var student by remember { mutableStateOf<TransportStudent?>(null) }
    var picking by remember { mutableStateOf(false) }
    var editor by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val coroutine = rememberCoroutineScope()
    val routeResponse by transportLoad(id, revision) { client.detail(id) }
    val route = routeResponse?.getOrNull()
    val options by transportLoad(route?.id) { route?.let { client.options(it, "") } }
    val day = chosenDay ?: options?.getOrNull()?.today
    val location by transportLoad(route?.id, tab, revision) { if (tab == 0) route?.let { client.latest(it) } else null }
    val stops by transportLoad(route?.id, tab, stopsPage, revision) {
        if (route != null && tab == 0) client.stops(route, 21, stopsPage * 20) else emptyList()
    }
    val records by transportLoad(route?.id, tab, day, student?.id, attendancePage, revision) {
        if (route != null && tab == 1 && day != null) client.attendance(route, day, 21, attendancePage * 20, student?.id)
        else emptyList()
    }
    LazyColumn(Modifier.fillMaxSize().testTag("native_transport_detail"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(route?.name ?: "Servis rotası", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack, enabled = !busy) { Text("Rotalara dön") }
            if (route != null) Text(listOf(route.branchName, route.number, route.plate, route.driver,
                "Kapasite ${route.capacity}", transportStatus(route.status)).filter(String::isNotBlank).joinToString(" · "))
            route?.description?.takeIf(String::isNotBlank)?.let { Text(it) }
            message?.let { Text(it) }
            TextButton(enabled = !busy, onClick = { revision++ }) { Text("Rota kayıtlarını yenile") }
        }
        if (route == null) item { Text(transportMessage(routeResponse, "Servis rotası bulunamadı.")) }
        else {
            item {
                TabRow(tab) {
                    Tab(tab == 0, onClick = { if (!busy) tab = 0 }, text = { Text("Durak ve konum") })
                    Tab(tab == 1, onClick = { if (!busy) tab = 1 }, text = { Text("Katılım") })
                }
            }
            if (tab == 0) {
                val latest = location?.getOrNull()
                item {
                    Text("Son konum kaydı", style = MaterialTheme.typography.titleMedium)
                    if (latest == null) Text(transportMessage(location, "Kayıtlı araç konumu yok."))
                    else {
                        Text(latest.time)
                        if (latest.latitude != null && latest.longitude != null) Text("${latest.latitude}, ${latest.longitude}")
                        latest.speed?.let { Text("Hız ${it} km/sa") }
                    }
                    Text("Rota durakları", style = MaterialTheme.typography.titleMedium)
                }
                val rows = stops?.getOrNull().orEmpty()
                if (rows.isEmpty()) item { Text(transportMessage(stops, "Rota için kayıtlı durak yok.")) }
                items(rows.take(20), key = { it.id }) { stop ->
                    TransportRecord(listOf("${stop.order}. ${stop.name}", stop.eta.takeIf(String::isNotBlank)?.let { "Tahmini varış $it" }.orEmpty()))
                }
                if (stopsPage > 0 || rows.size > 20) item { TransportPager(stopsPage, rows.size, busy) { stopsPage = it } }
            } else {
                item {
                    val date = dayText ?: day?.toString().orEmpty()
                    TransportField(date, "Katılım günü (YYYY-AA-GG)", 10, busy) { dayText = it }
                    TextButton(enabled = !busy && transportDate(date) != null, onClick = {
                        chosenDay = transportDate(date); attendancePage = 0
                    }) { Text("Tarihi uygula") }
                    if (options?.isFailure == true) Text("Okul günü yüklenemedi. Tarih seçebilir veya listeyi yenileyebilirsiniz.")
                    TextButton(enabled = !busy, onClick = { picking = true }) { Text(student?.name ?: "Tüm öğrenciler · Öğrenci seç") }
                    if (student != null) TextButton(enabled = !busy, onClick = { student = null; attendancePage = 0 }) {
                        Text("Tüm öğrencileri göster")
                    }
                    if (canWrite && student?.active == true && day != null) {
                        Button(enabled = !busy, onClick = { editor = true; message = null }) { Text("Katılım ve biniş/iniş kaydet") }
                    }
                }
                val rows = records?.getOrNull().orEmpty()
                if (rows.isEmpty()) item { Text(transportMessage(records, "Seçili kapsamda katılım kaydı yok.")) }
                items(rows.take(20), key = { it.id }) { row ->
                    TransportRecord(listOf(row.name, transportStatus(row.status), row.stop,
                        row.pickup.takeIf(String::isNotBlank)?.let { "Biniş $it" }.orEmpty(),
                        row.dropoff.takeIf(String::isNotBlank)?.let { "İniş $it" }.orEmpty(), row.notes))
                }
                if (attendancePage > 0 || rows.size > 20) item { TransportPager(attendancePage, rows.size, busy) { attendancePage = it } }
            }
        }
    }
    if (route != null && picking) {
        var search by remember { mutableStateOf("") }
        val choices by transportLoad(search) {
            if (search.isNotBlank()) delay(250)
            client.options(route, search.trim())
        }
        ConsentFormDialog("Servis öğrencisi seç", "native_transport_students", false, { picking = false }, fields = {
            item { TransportField(search, "Öğrenci ara", 100) { search = it } }
            val result = choices?.getOrNull()
            if (result == null || result.students.isEmpty()) item { Text(transportMessage(choices, "Öğrenci bulunamadı.")) }
            if (result != null) {
                items(result.students, key = { it.id }) { choice ->
                    TextButton(onClick = { student = choice; attendancePage = 0; picking = false }) { Text(choice.name) }
                }
                if (result.more) item { Text("Daha fazla sonuç için aramayı daraltın.") }
            }
        }, confirm = {})
    }
    val selected = student
    if (route != null && editor && canWrite && selected != null && day != null) {
        val existing = records?.getOrNull()?.firstOrNull { it.studentId == selected.id }
        TransportAttendanceEditor(selected, day, existing, busy, message, onClose = { editor = false }) { status, notes, event, time, request ->
            if (!busy) {
                busy = true
                message = null
                coroutine.launch {
                    try {
                        if (event == null) client.saveAttendance(route, selected.id, day, status, notes)
                        else client.checkIn(route, selected.id, event, LocalDateTime.of(day, requireNotNull(time)), request)
                        message = "Servis kaydı sunucudan doğrulandı."
                        revision++
                        attendancePage = 0
                        editor = false
                    } catch (failure: Exception) {
                        if (failure is CancellationException) throw failure
                        message = "Servis kaydı doğrulanamadı. ${failure.message.orEmpty()}"
                    } finally { busy = false }
                }
            }
        }
    }
}

@Composable
private fun TransportAttendanceEditor(student: TransportStudent, day: LocalDate, existing: TransportAttendance?,
    busy: Boolean, message: String?, onClose: () -> Unit, onSave: (String, String, String?, LocalTime?, UUID) -> Unit) {
    var status by remember { mutableStateOf(existing?.status ?: "PRESENT") }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var event by remember { mutableStateOf<String?>(null) }
    var time by remember { mutableStateOf("") }
    val timestamp = runCatching { LocalTime.parse(time) }.getOrNull()
    val request = remember(event, time) { UUID.randomUUID() }
    ConsentFormDialog("Servis katılımı", "native_transport_attendance", busy, onClose, fields = {
        item {
            Text("${student.name} · $day")
            listOf(null to "Katılım durumu", "BOARDED" to "Biniş", "DROPPED" to "İniş").forEach { (value, label) ->
                FilterChip(event == value, { event = value }, { Text(label) }, enabled = !busy)
            }
            if (event == null) {
                listOf("PRESENT", "ABSENT", "LATE").forEach { value ->
                    FilterChip(status == value, { status = value }, { Text(transportStatus(value)) }, enabled = !busy)
                }
                TransportField(notes, "Katılım notu", 500, busy) { notes = it }
            } else TransportField(time, "Okul saati (SS:DD)", 5, busy) { time = it }
            message?.let { Text(it) }
        }
    }, confirm = {
        Button(enabled = !busy && (event == null || timestamp != null),
            onClick = { onSave(status, notes.trim(), event, timestamp, request) }) { Text("Servis kaydını doğrula") }
    })
}

@Composable
private fun TransportRecord(lines: List<String>, actions: @Composable () -> Unit = {}) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            lines.filter(String::isNotBlank).forEach { Text(it) }
            actions()
        }
    }
}

@Composable
private fun TransportField(value: String, label: String, max: Int, busy: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value, { onChange(it.take(max)) }, enabled = !busy, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, singleLine = max < 500)
}

@Composable
private fun TransportPager(page: Int, size: Int, busy: Boolean, change: (Int) -> Unit) {
    Text("Sayfa ${page + 1}")
    if (page > 0) TextButton(enabled = !busy, onClick = { change(page - 1) }) { Text("Önceki sayfa") }
    if (size > 20) TextButton(enabled = !busy, onClick = { change(page + 1) }) { Text("Sonraki sayfa") }
}

private fun transportDate(value: String) = runCatching { LocalDate.parse(value.trim()) }.getOrNull()
private fun transportStatus(status: String) = when (status) {
    "ACTIVE" -> "Aktif"
    "MAINTENANCE" -> "Bakımda"
    "OUT_OF_SERVICE" -> "Hizmet dışı"
    "PRESENT" -> "Katıldı"
    "ABSENT" -> "Katılmadı"
    "LATE" -> "Geç"
    else -> status
}
private fun transportMessage(result: Result<*>?, empty: String) = when {
    result == null -> "Servis kayıtları yükleniyor…"
    result.isFailure -> "Servis kayıtları yüklenemedi."
    else -> empty
}

@Composable
private fun <T> transportLoad(vararg keys: Any?, load: suspend () -> T): State<Result<T>?> =
    key(keys.toList()) {
        produceState<Result<T>?>(null) {
            value = try { Result.success(load()) } catch (failure: Exception) {
                if (failure is CancellationException) throw failure
                Result.failure(failure)
            }
        }
    }
