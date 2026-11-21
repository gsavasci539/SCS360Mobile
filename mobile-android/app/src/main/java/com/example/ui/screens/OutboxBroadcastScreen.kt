package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun OutboxBroadcastScreen(state: Okul360UiState, onBack: () -> Unit) {
    NativeOutboxScreen(state, onBack)
}
