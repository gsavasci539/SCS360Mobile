package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
internal fun CanteenStaffPortalScreen(state: Okul360UiState, onBack: (() -> Unit)? = null, onLogout: (() -> Unit)? = null) {
    NativeCanteenScreen(state, onBack = onBack, onLogout = onLogout)
}
