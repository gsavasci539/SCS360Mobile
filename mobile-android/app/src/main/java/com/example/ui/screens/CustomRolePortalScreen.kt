package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.User
import com.example.ui.navigation.*

@Composable
fun CustomRolePortalScreen(user: User, onTab: (MainTab) -> Unit, onScreen: (SubScreen) -> Unit, onLogout: () -> Unit) {
    val tabs = MainTab.entries.filter { it !in listOf(MainTab.HOME, MainTab.OTHER) && NativeNavigationPolicy.tab(user, it) }
    val screens = SubScreen.entries.filter { it != SubScreen.NONE && NativeNavigationPolicy.screen(user, it) }
    LazyColumn(Modifier.fillMaxSize().testTag("custom_role_portal"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(user.resolvedName, style = MaterialTheme.typography.titleLarge) }
        item { Text(user.roles.filter { it.is_active }.joinToString { it.slug }) }
        item { Text("Hesabınıza verilen izinlerle erişebileceğiniz ekranlar") }
        items(tabs, key = { "tab_${it.name}" }) { tab -> OutlinedButton(onClick = { onTab(tab) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("custom_tab_${tab.name}")) { Text(tab.title) } }
        items(screens, key = { "screen_${it.name}" }) { screen -> OutlinedButton(onClick = { onScreen(screen) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("custom_screen_${screen.name}")) { Text(screen.title) } }
        item { TextButton(onClick = onLogout, modifier = Modifier.heightIn(min = 48.dp)) { Text("Çıkış yap") } }
    }
}
