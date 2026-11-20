package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.data.model.ParentAttendanceMonth
import com.example.data.remote.NativeParentPortalClient
import com.example.data.remote.RegistryHttpException
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentRose
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryEmerald
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
internal fun AttendanceMonthlyCalendarView(studentId: String, initialDate: LocalDate?, modifier: Modifier = Modifier) {
    var period by remember(studentId, initialDate) {
        mutableStateOf(initialDate?.let(YearMonth::from) ?: YearMonth.now(ZoneId.of("Europe/Istanbul")))
    }
    var selectedDay by remember(studentId, initialDate, period) { mutableStateOf(initialDate?.takeIf { YearMonth.from(it) == period }) }
    var retry by remember(studentId) { mutableIntStateOf(0) }
    val month = period
    val result = key(studentId, month, retry) {
        produceState<Result<ParentAttendanceMonth>?>(null) {
            try {
                value = Result.success(NativeParentPortalClient().attendance(studentId, month))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                value = Result.failure(failure)
            }
        }.value
    }

    LazyColumn(modifier.fillMaxSize().testTag("parent_monthly_attendance"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Yoklama dönemi · ${month.monthValue.toString().padStart(2, '0')}/${month.year}",
                style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { period = month.minusMonths(1) }, enabled = month > YearMonth.of(1, 1)) { Text("Önceki ay") }
                TextButton(onClick = { period = month.plusMonths(1) }, enabled = month < YearMonth.of(9999, 12)) { Text("Sonraki ay") }
            }
        }

        val data = result?.getOrNull()
        when {
            result == null -> item { Text("Yoklama kayıtları yükleniyor…") }
            data == null -> item {
                val denied = (result.exceptionOrNull() as? RegistryHttpException)?.code == 403
                Text(if (denied) "Yoklama kayıtlarını görüntüleme yetkiniz yok." else "Yoklama kayıtları yüklenemedi.",
                    color = MaterialTheme.colorScheme.error)
                if (!denied) TextButton(onClick = { retry++ }) { Text("Tekrar dene") }
            }
            else -> {
                item {
                    val summary = data.summary
                    if (summary.total == 0) Text("Bu ay için yoklama kaydı yok.")
                    else Text("Mevcut ${summary.present} · Geç ${summary.late} · Devamsız ${summary.absent} · İzinli ${summary.excused}")
                }
                selectedDay?.let { day ->
                    val record = data.calendar[day.dayOfMonth.toString()]
                    item {
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("$day · ${attendanceLabel(record?.status)}", style = MaterialTheme.typography.titleSmall)
                                record?.check_in_time?.takeIf(String::isNotBlank)?.let { Text("Giriş · $it") }
                                record?.check_out_time?.takeIf(String::isNotBlank)?.let { Text("Çıkış · $it") }
                                record?.note?.takeIf(String::isNotBlank)?.let { Text(it) }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz").forEach { weekday ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(weekday, maxLines = 1) }
                        }
                    }
                }
                val offset = month.atDay(1).dayOfWeek.value - 1
                val numberOfCells = ((offset + month.lengthOfMonth() + 6) / 7) * 7
                val weeks = (0 until numberOfCells).chunked(7)
                items(weeks, key = { it.first() }) { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        week.forEach { cell ->
                            val day = cell - offset + 1
                            if (day !in 1..month.lengthOfMonth()) Spacer(Modifier.weight(1f))
                            else {
                                val date = month.atDay(day)
                                val status = data.calendar[day.toString()]?.status
                                val statusColor = attendanceColor(status) ?: MaterialTheme.colorScheme.surfaceVariant
                                Surface(onClick = { selectedDay = date },
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp).semantics {
                                        contentDescription = "$date · ${attendanceLabel(status)}"
                                    },
                                    shape = MaterialTheme.shapes.small, color = statusColor.copy(alpha = 0.16f),
                                    border = if (selectedDay == date) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null) {
                                    Box(contentAlignment = Alignment.Center) { Text(day.toString()) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun attendanceLabel(status: String?) = when (status) {
    "PRESENT" -> "Mevcut"
    "LATE" -> "Geç"
    "ABSENT" -> "Devamsız"
    "EXCUSED" -> "İzinli"
    null, "" -> "Kayıt yok"
    else -> "Durum belirtilmedi"
}

private fun attendanceColor(status: String?): Color? = when (status) {
    "PRESENT" -> SecondaryEmerald
    "LATE" -> AccentAmber
    "ABSENT" -> AccentRose
    "EXCUSED" -> PrimaryBlue
    else -> null
}
