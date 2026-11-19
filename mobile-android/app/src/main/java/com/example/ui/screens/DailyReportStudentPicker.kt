package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.remote.DailyReportHttpException
import com.example.data.remote.DailyReportStudentOption
import com.example.data.remote.DailyReportStudentPage
import com.example.data.remote.NativeDailyReportClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
internal fun DailyReportStudentPicker(branchId: String?, classId: String?, selected: DailyReportStudentOption?,
    enabled: Boolean, onSelect: (DailyReportStudentOption) -> Unit) {
    var search by remember(branchId, classId) { mutableStateOf("") }
    var page by remember(branchId, classId) { mutableIntStateOf(1) }
    var retry by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(value = search, onValueChange = { search = it.take(150); page = 1 },
            label = { Text("Öğrenci adı veya numarası") }, enabled = enabled, singleLine = true, modifier = Modifier.fillMaxWidth())
        selected?.let { Text("Seçili öğrenci · ${it.label}", style = MaterialTheme.typography.bodyMedium) }
        key(branchId, classId, search, page, retry) {
            val result by produceState<Result<DailyReportStudentPage>?>(null) {
                try {
                    if (search.isNotBlank()) delay(200)
                    value = Result.success(NativeDailyReportClient().studentOptions(branchId, classId, search, page))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (failure: Exception) {
                    value = Result.failure(failure)
                }
            }
            val response = result
            val options = response?.getOrNull()
            when {
                response == null -> Text("Öğrenci seçenekleri yükleniyor…")
                options == null -> {
                    val code = (response.exceptionOrNull() as? DailyReportHttpException)?.code
                    Text(if (code == 403) "Öğrenci seçeneklerine erişim yetkiniz yok." else "Öğrenci seçenekleri yüklenemedi.",
                        color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { retry++ }, enabled = enabled) { Text("Tekrar dene") }
                }
                options.rows.isEmpty() -> Text("Eşleşen öğrenci yok.")
                else -> LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(options.rows, key = { it.id }) { option ->
                        FilterChip(selected = selected?.id == option.id, enabled = enabled, onClick = { onSelect(option) },
                            label = { Text(option.label) })
                    }
                }
            }
            if (page > 1 || (options?.totalPages ?: 0) > 1) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { page-- }, enabled = enabled && page > 1) { Text("Önceki") }
                    TextButton(onClick = { page++ }, enabled = enabled && page < (options?.totalPages ?: 0)) { Text("Sonraki") }
                }
            }
        }
    }
}
