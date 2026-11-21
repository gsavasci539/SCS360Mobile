package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
internal fun CanteenWalletScreen(state: Okul360UiState, onNavigateBack: () -> Unit) {
    NativeCanteenScreen(state, onBack = onNavigateBack)
}
