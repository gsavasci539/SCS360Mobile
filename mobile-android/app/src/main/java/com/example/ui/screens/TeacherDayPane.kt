package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.remote.NativeTeacherDayClient
import com.example.data.remote.TeacherDayLesson
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException

private const val TEACHER_MEDICINE_PAGE_SIZE = 20
private val teacherMedicineStatuses = mapOf("PENDING" to "Bekliyor", "APPROVED" to "Onaylı", "REJECTED" to "Reddedildi", "COMPLETED" to "Tamamlandı")
private enum class TeacherDayView(val title: String) {
    WEEK("Haftalık Ders Çizelgesi"), TODAY("Bugünkü Program"), CLASSES("Sınıflarım"), TASKS("Görevler"), MEDICINE("İlaç talepleri")
}

@Composable
internal fun TeacherDayPane(state: Okul360UiState) {
    val user = state.currentUser
    val canReadClasses = NativeNavigationPolicy.has(user, "teacher.view_classes")
    val canReadSchedule = NativeNavigationPolicy.has(user, "teacher.view_schedule")
    val canReadTasks = NativeNavigationPolicy.has(user, "teacher.manage_tasks")
    val canReadMedicine = NativeNavigationPolicy.has(user, "health.read", "teacher.view_medicine_requests")
    val canReadWeek = canReadSchedule && canReadClasses && NativeNavigationPolicy.has(user, "classes.read", "class.read")
    key(user.id, user.resolvedTenantId, user.branch_ids, state.workspaceOrganizationId, state.selectedBranchId,
        state.serverBaseUrl, canReadSchedule, canReadClasses, canReadTasks, canReadWeek, canReadMedicine) {
        val views = listOfNotNull(TeacherDayView.WEEK.takeIf { canReadWeek }, TeacherDayView.TODAY.takeIf { canReadSchedule },
            TeacherDayView.CLASSES.takeIf { canReadClasses }, TeacherDayView.TASKS.takeIf { canReadTasks },
            TeacherDayView.MEDICINE.takeIf { canReadMedicine })
        var view by remember { mutableStateOf(views.firstOrNull()) }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (views.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    items(views) { item ->
                        FilterChip(selected = view == item, onClick = { view = item }, label = { Text(item.title) })
                    }
                }
            }
            when (view) {
                TeacherDayView.WEEK -> TeacherWeek()
                TeacherDayView.TODAY -> TeacherDayRows("Bugün ders programı yok.", { today() }) { LessonCard(it) }
                TeacherDayView.CLASSES -> TeacherDayRows("Henüz sınıf atanmamış.", { classes() }) { classroom ->
                    TeacherDayCard(classroom.name, "Aktif öğrenci sayısı ${classroom.studentCount}")
                }
                TeacherDayView.TASKS -> TeacherDayRows("Bekleyen görev yok.", { pending() }) { task ->
                    TeacherDayCard(task.classroom, "${task.kind} bekleyen öğrenci sayısı ${task.remaining}", task.date.toString())
                }
                TeacherDayView.MEDICINE -> TeacherMedicineRequests(state.selectedBranchId)
                null -> Text("Bu ekrandaki kayıtları görüntüleme yetkiniz bulunmuyor.")
            }
        }
    }
}

@Composable
private fun TeacherMedicineRequests(branch: String) {
    var page by remember { mutableIntStateOf(0) }
    TeacherDayRows("Bu kapsamda ilaç talebi bulunamadı.",
        { medicines(branch, page * TEACHER_MEDICINE_PAGE_SIZE, TEACHER_MEDICINE_PAGE_SIZE + 1) },
        page, { page = it }) { request ->
        val status = teacherMedicineStatuses[request.status] ?: request.status
        key(request.id) {
            TeacherDayCard(request.name, listOf(request.studentName, request.classroom, request.dose, request.timing, status).joinToString(" · "),
                request.instructions.orEmpty())
        }
    }
}

@Composable
private fun TeacherWeek() {
    var selectedId by remember { mutableStateOf<String?>(null) }
    TeacherDayRows("Henüz sınıf atanmamış.", { classes() }) { classroom ->
        FilterChip(selected = selectedId == classroom.id, onClick = { selectedId = classroom.id }, label = { Text(classroom.name) })
    }
    val classId = selectedId
    if (classId != null) {
        key(classId) {
            TeacherDayRows("Bu sınıf için kayıtlı haftalık çizelge yok.", { week(classId) }) { day ->
                Text(day.name, style = MaterialTheme.typography.titleSmall)
                day.lessons.forEach { LessonCard(it) }
            }
        }
    }
}

@Composable
private fun <T> TeacherDayRows(emptyLabel: String, load: suspend NativeTeacherDayClient.() -> List<T>,
    page: Int = 0, onPage: ((Int) -> Unit)? = null, row: @Composable (T) -> Unit) {
    var attempt by remember { mutableIntStateOf(0) }
    val result by produceState<Result<List<T>>?>(initialValue = null, key1 = attempt, key2 = page) {
        value = null
        try {
            value = Result.success(NativeTeacherDayClient().load())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            value = Result.failure(failure)
        }
    }
    val reply = result
    if (reply == null) {
        CircularProgressIndicator()
    } else if (reply.isFailure) {
        Text("Öğretmen verisi yüklenemedi.", color = MaterialTheme.colorScheme.error)
        TextButton(onClick = { attempt++ }) { Text("Tekrar dene") }
    } else {
        val rows = reply.getOrThrow()
        if (rows.isEmpty()) Text(emptyLabel)
        if (onPage == null) {
            rows.forEach { row(it) }
        } else {
            rows.take(TEACHER_MEDICINE_PAGE_SIZE).forEach { row(it) }
            if (page > 0 || rows.size > TEACHER_MEDICINE_PAGE_SIZE) {
                Text("Sayfa ${page + 1}")
                TextButton(enabled = page > 0, onClick = { onPage(page - 1) }) { Text("Önceki") }
                TextButton(enabled = rows.size > TEACHER_MEDICINE_PAGE_SIZE, onClick = { onPage(page + 1) }) { Text("Sonraki") }
            }
        }
    }
}

@Composable
private fun LessonCard(lesson: TeacherDayLesson) = TeacherDayCard(lesson.subject, lesson.time, lesson.location)

@Composable
private fun TeacherDayCard(title: String, detail: String, extra: String = "") {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            if (detail.isNotBlank()) Text(detail)
            if (extra.isNotBlank()) Text(extra, style = MaterialTheme.typography.bodySmall)
        }
    }
}
