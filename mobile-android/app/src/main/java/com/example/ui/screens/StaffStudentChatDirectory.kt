package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.User
import com.example.data.remote.NativeStudentChatClient
import com.example.data.remote.StaffChatArea
import com.example.data.remote.StaffChatContact
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
internal fun StaffStudentChatDirectory(
    actor: User, area: StaffChatArea, branchId: String?, onClose: () -> Unit,
) {
    val api = remember { NativeStudentChatClient() }
    var selected by remember { mutableStateOf<StaffChatContact?>(null) }
    var search by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(1) }
    var revision by remember { mutableIntStateOf(0) }
    var rows by remember { mutableStateOf<List<StaffChatContact>>(emptyList()) }
    var hasMore by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf(false) }
    LaunchedEffect(search, page, revision) {
        loading = true
        error = false
        if (search.isNotBlank() && page == 1) delay(250)
        try {
            val result = api.contacts(area, branchId, search.trim(), page)
            if (!api.current()) return@LaunchedEffect
            val previous = if (page == 1) emptyList() else rows
            rows = (previous + result.rows).distinctBy { it.studentId to it.parentId }
            hasMore = result.hasMore
            loaded = true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (api.current()) error = true
        } finally {
            loading = false
        }
    }
    if (!api.current()) return
    Dialog(
        onDismissRequest = { if (selected == null) onClose() else selected = null },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(Modifier.fillMaxSize()) {
            val contact = selected
            if (contact != null) {
                StaffStudentConversation(contact, actor, area) { selected = null }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().imePadding().testTag("staff_chat_contacts"),
                    contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Sohbetlere dön") }
                        Text("Öğrenciye özel sohbet", style = MaterialTheme.typography.titleLarge)
                    }
                    item {
                        OutlinedTextField(search, { value ->
                            if (value.length <= 120) {
                                rows = emptyList()
                                loaded = false
                                hasMore = false
                                page = 1
                                search = value
                            }
                        }, label = { Text("Öğrenci, veli veya sınıf ara") }, singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("staff_chat_search"))
                    }
                    items(rows, key = { "${it.studentId}/${it.parentId}" }) { row ->
                        ElevatedCard(onClick = { selected = row }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .testTag("staff_chat_contact_${row.studentId}_${row.parentId}")) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(row.studentName, style = MaterialTheme.typography.titleMedium)
                                Text(row.parentName)
                                row.className?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                    if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (error) item {
                        Text("Öğrenci ve veli listesi yüklenemedi.", color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { revision++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Yeniden dene") }
                    } else if (loaded && rows.isEmpty()) item { Text("Eşleşen öğrenci ve bağlı veli hesabı bulunamadı.") }
                    if (hasMore && !loading && !error) item {
                        TextButton(onClick = { page++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Daha fazla göster") }
                    }
                }
            }
        }
    }
}
