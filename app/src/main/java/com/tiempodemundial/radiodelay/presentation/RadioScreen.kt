package com.tiempodemundial.radiodelay.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tiempodemundial.radiodelay.domain.model.ConnectionStatus
import com.tiempodemundial.radiodelay.domain.model.Delay
import com.tiempodemundial.radiodelay.domain.model.RadioPlaybackState
import com.tiempodemundial.radiodelay.presentation.components.DelayPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    state: RadioPlaybackState,
    onTogglePlayback: () -> Unit,
    onDelaySelected: (Delay) -> Unit,
    onReturnToLive: () -> Unit,
) {
    var showDelayPicker by remember { mutableStateOf(false) }

    if (showDelayPicker) {
        DelayPickerDialog(
            initialDelay = state.selectedDelay,
            onDismiss = { showDelayPicker = false },
            onConfirm = onDelaySelected,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tiempo de Mundial", fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConnectionBanner(state)
            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "RETRASO SELECCIONADO",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = state.selectedDelay.formatted,
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = "min : s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { showDelayPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Text("  Elegir retraso")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            BufferCard(state)
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onTogglePlayback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
            ) {
                if (state.isBuffering && state.isPlaying) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                    )
                }
                Text(if (state.isPlaying) "  Pausar" else "  Reproducir")
            }

            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onReturnToLive,
                enabled = state.selectedDelay != Delay.ZERO,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Radio, contentDescription = null)
                Text("  Volver al directo")
            }

            state.errorMessage?.let { message ->
                Spacer(Modifier.height(20.dp))
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ConnectionBanner(state: RadioPlaybackState) {
    val text = when (state.connectionStatus) {
        ConnectionStatus.IDLE -> "Preparado"
        ConnectionStatus.CONNECTING -> "Conectando con la emisión…"
        ConnectionStatus.CONNECTED -> "Emisión conectada"
        ConnectionStatus.RECONNECTING -> "Reconectando…"
        ConnectionStatus.ERROR -> "Problema de conexión"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = when (state.connectionStatus) {
            ConnectionStatus.ERROR -> MaterialTheme.colorScheme.error
            ConnectionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

@Composable
private fun BufferCard(state: RadioPlaybackState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Audio disponible", style = MaterialTheme.typography.titleMedium)
                Text(
                    Delay.ofSeconds(state.availableDelaySeconds).formatted,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (state.isWaitingForSelectedDelay) {
                Spacer(Modifier.height(10.dp))
                val target = state.selectedDelay.totalSeconds.coerceAtLeast(1)
                LinearProgressIndicator(
                    progress = {
                        (state.availableDelaySeconds.toFloat() / target).coerceIn(0f, 1f)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Acumulando audio. El retraso ${state.selectedDelay.formatted} " +
                        "se aplicará automáticamente cuando esté disponible.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else if (state.isPlaying) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Retraso aplicado: ${state.effectiveDelay.formatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
