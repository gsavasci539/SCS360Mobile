package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.remote.NativeAnnouncement
import com.example.data.remote.NativeAnnouncementClient
import com.example.ui.navigation.NativeNavigationPolicy
import com.example.ui.screens.ConsentFormDialog
import com.example.ui.viewmodel.Okul360UiState
import kotlinx.coroutines.CancellationException

@Composable
fun AnnouncementsSummaryWidget(state: Okul360UiState, onNavigateToAnnouncements: () -> Unit, modifier: Modifier = Modifier) {
    val user = state.currentUser
    if (!state.isLoggedIn || !NativeNavigationPolicy.has(user, "announcement.read") ||
        user.is_super_admin == true && state.workspaceOrganizationId.isNullOrBlank()) return
    key(financePresentationScope(state), state.serverBaseUrl) {
        var retry by remember { mutableIntStateOf(0) }
        var detail by remember { mutableStateOf<NativeAnnouncement?>(null) }
        val result by produceState<Result<List<NativeAnnouncement>>?>(null, retry) {
            value = null
            value = try {
                Result.success(NativeAnnouncementClient().published(state.selectedBranchId, 4))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Result.failure(failure)
            }
        }
        OutlinedCard(modifier.fillMaxWidth().testTag("announcements_summary_widget")) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Yayımlanan duyurular", style = MaterialTheme.typography.titleMedium)
                val rows = result?.getOrNull()
                when {
                    result == null -> Text("Duyurular yükleniyor…")
                    rows == null -> {
                        Text(result?.exceptionOrNull()?.message ?: "Duyurular yüklenemedi.")
                        OutlinedButton(onClick = { retry++ }) { Text("Duyuruları yeniden yükle") }
                    }
                    rows.isEmpty() -> Text("Bu kapsamda yayımlanmış duyuru bulunamadı.")
                    else -> rows.forEach { item ->
                        key(item.id) {
                            OutlinedCard(onClick = { detail = item }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                                    Text(item.content, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
                TextButton(onClick = onNavigateToAnnouncements) { Text("Duyuru kayıtlarını aç") }
            }
        }
        detail?.let { item ->
            ConsentFormDialog(item.title, "dashboard_announcement_details", false, { detail = null }, {
                item { Text("Kayıt tarihi · ${item.createdAt.take(10)}") }
                item { Text(item.content) }
            }) {}
        }
    }
}
