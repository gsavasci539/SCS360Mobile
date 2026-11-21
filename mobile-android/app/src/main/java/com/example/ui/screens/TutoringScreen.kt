package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun TutoringScreen(state: Okul360UiState, onNavigateBack: () -> Unit) {
    NativeTutoringScreen(state, onNavigateBack)
}
