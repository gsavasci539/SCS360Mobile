package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun CourseMaterialsScreen(state: Okul360UiState, child: String? = null, onBack: (() -> Unit)? = null) {
    NativeMaterialsScreen(state, child, onBack)
}
