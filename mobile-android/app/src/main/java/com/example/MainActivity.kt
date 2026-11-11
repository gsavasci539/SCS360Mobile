package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.ui.context.ChatProvider
import com.example.ui.navigation.Okul360App
import com.example.ui.theme.Okul360Theme
import com.example.ui.viewmodel.Okul360ViewModel
import com.example.util.NotificationHelper

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
    private val viewModel: Okul360ViewModel by viewModels()

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        try {
            // Initialize Notification Channels
            NotificationHelper.createNotificationChannels(this)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        try {
            // Request Notification Permission on Android 13+ (API 33)
            checkAndRequestNotificationPermission()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        try {
            handleNotificationIntent(intent)
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            Okul360Theme(
                themeName = state.selectedThemeName
            ) {
                ChatProvider(
                    role = state.selectedRole,
                    userName = state.currentUser.resolvedName
                ) {
                    Okul360App(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val targetScreen = intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_SCREEN)
        if (!targetScreen.isNullOrEmpty()) {
            // Can be used to jump to specific tab or screen
        }
    }
}


