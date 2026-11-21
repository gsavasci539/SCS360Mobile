package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun SurveysScreen(state: Okul360UiState, onNavigateBack: (() -> Unit)? = null, child: String? = null) {
    NativeSurveysScreen(state, onNavigateBack, child)
}
