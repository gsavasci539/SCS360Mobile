package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
internal fun SecurityAuditScreen(
    state: Okul360UiState,
    onNavigateBack: () -> Unit,
    onNavigateToDismissal: () -> Unit,
) {
    NativeSecurityScreen(state, onOpenDismissal = onNavigateToDismissal, onBack = onNavigateBack)
}
