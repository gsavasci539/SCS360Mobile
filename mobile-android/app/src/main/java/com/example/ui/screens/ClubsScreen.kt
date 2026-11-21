package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.Okul360UiState

@Composable
fun ClubsScreen(state: Okul360UiState, onNavigateBack: (() -> Unit)? = null, child: String? = null) {
    NativeClubsScreen(state, onNavigateBack, child)
}
