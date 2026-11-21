package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
internal fun DigitalIdCardScreen(state: Okul360UiState, onBack: () -> Unit) {
    NativeSecurityScreen(state, startWithCredentials = true, onBack = onBack)
}
