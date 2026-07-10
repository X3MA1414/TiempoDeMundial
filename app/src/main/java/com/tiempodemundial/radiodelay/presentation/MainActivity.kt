package com.tiempodemundial.radiodelay.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tiempodemundial.radiodelay.RadioDelayApplication
import com.tiempodemundial.radiodelay.presentation.theme.TiempoDeMundialTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RadioViewModel by viewModels {
        RadioViewModel.Factory((application as RadioDelayApplication).container)
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Playback remains usable even if the user declines. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionWhenNeeded()

        setContent {
            TiempoDeMundialTheme {
                val state = viewModel.state.collectAsStateWithLifecycle().value
                RadioScreen(
                    state = state,
                    onTogglePlayback = viewModel::togglePlayback,
                    onDelaySelected = viewModel::chooseDelay,
                    onReturnToLive = viewModel::goLive,
                )
            }
        }
    }

    private fun requestNotificationPermissionWhenNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
