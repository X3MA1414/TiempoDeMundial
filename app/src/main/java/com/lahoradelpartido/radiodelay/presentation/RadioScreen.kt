package com.lahoradelpartido.radiodelay.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lahoradelpartido.radiodelay.domain.model.ConnectionStatus
import com.lahoradelpartido.radiodelay.domain.model.Delay
import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.RadioPlaybackState
import com.lahoradelpartido.radiodelay.presentation.components.DelayDial
import com.lahoradelpartido.radiodelay.presentation.components.DelayPickerSheet
import com.lahoradelpartido.radiodelay.presentation.components.EmissionPickerSheet
import com.lahoradelpartido.radiodelay.presentation.theme.appBackgroundBrush

/** Atajos de retraso. Cubren los desfases habituales frente a la televisión. */
private val QUICK_DELAYS = listOf(0, 10, 20, 30, 45, 60, 90, 120, 180)

@Composable
fun RadioScreen(
    state: RadioUiState,
    onTogglePlayback: () -> Unit,
    onDelaySelected: (Delay) -> Unit,
    onReturnToLive: () -> Unit,
    onEmissionSelected: (Emission) -> Unit,
    onRefreshEmissions: () -> Unit,
) {
    var showDelayPicker by remember { mutableStateOf(false) }
    var showEmissionPicker by remember { mutableStateOf(false) }
    val playback = state.playback

    if (showDelayPicker) {
        DelayPickerSheet(
            initialDelay = playback.selectedDelay,
            onDismiss = { showDelayPicker = false },
            onConfirm = onDelaySelected,
        )
    }

    if (showEmissionPicker) {
        EmissionPickerSheet(
            emissions = state.emissions.emissions.ifEmpty { listOf(playback.selectedEmission) },
            selected = playback.selectedEmission,
            isLoading = state.emissions.isLoading,
            loadFailed = state.emissions.loadFailed,
            onRefresh = onRefreshEmissions,
            onSelect = onEmissionSelected,
            onDismiss = { showEmissionPicker = false },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBackgroundBrush()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderBar(playback.connectionStatus)
            Spacer(Modifier.height(18.dp))

            EmissionCard(
                emission = playback.selectedEmission,
                isLoading = state.emissions.isLoading,
                onClick = {
                    onRefreshEmissions()
                    showEmissionPicker = true
                },
            )
            Spacer(Modifier.height(22.dp))

            DelayDial(
                delayLabel = playback.selectedDelay.formatted,
                progress = dialProgress(playback),
                caption = dialCaption(playback),
                isWaiting = playback.isWaitingForSelectedDelay,
                onClick = { showDelayPicker = true },
                modifier = Modifier.fillMaxWidth(0.78f),
            )
            Spacer(Modifier.height(20.dp))

            QuickDelayRow(
                selected = playback.selectedDelay,
                onSelect = onDelaySelected,
                onOpenPicker = { showDelayPicker = true },
            )
            Spacer(Modifier.height(24.dp))

            TransportControls(
                state = playback,
                onTogglePlayback = onTogglePlayback,
                onReturnToLive = onReturnToLive,
            )
            Spacer(Modifier.height(22.dp))

            BufferStrip(playback)

            AnimatedVisibility(
                visible = playback.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = playback.errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderBar(status: ConnectionStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "LA HORA DEL",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "PARTIDO",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        ConnectionPill(status)
    }
}

@Composable
private fun ConnectionPill(status: ConnectionStatus) {
    val colors = MaterialTheme.colorScheme
    val (label, tint) = when (status) {
        ConnectionStatus.IDLE -> "En espera" to colors.onSurfaceVariant
        ConnectionStatus.CONNECTING -> "Conectando" to colors.secondary
        ConnectionStatus.CONNECTED -> "En antena" to colors.primary
        ConnectionStatus.RECONNECTING -> "Reconectando" to colors.secondary
        ConnectionStatus.ERROR -> "Sin señal" to colors.error
    }

    val transition = rememberInfiniteTransition(label = "connectionPulse")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "connectionPulseAlpha",
    )
    val dotAlpha = if (status == ConnectionStatus.CONNECTED) 1f else pulse

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .border(1.dp, tint.copy(alpha = 0.32f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = dotAlpha)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}

@Composable
private fun EmissionCard(
    emission: Emission,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceContainer.copy(alpha = 0.85f))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (emission.isSports) {
                    Icons.Rounded.SportsSoccer
                } else {
                    Icons.Rounded.GraphicEq
                },
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = "EMISIÓN",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = emission.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = emission.subtitle.ifBlank { emission.schedule },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(10.dp))

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = colors.primary,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Cambiar de emisión",
                tint = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QuickDelayRow(
    selected: Delay,
    onSelect: (Delay) -> Unit,
    onOpenPicker: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QUICK_DELAYS.forEach { totalSeconds ->
            val delay = Delay.ofSeconds(totalSeconds)
            DelayChip(
                label = if (totalSeconds == 0) "Directo" else delay.formatted,
                isSelected = selected.totalSeconds == totalSeconds,
                onClick = { onSelect(delay) },
            )
        }
        DelayChip(
            label = "Otro…",
            isSelected = QUICK_DELAYS.none { it == selected.totalSeconds },
            onClick = onOpenPicker,
        )
    }
}

@Composable
private fun DelayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val background = if (isSelected) colors.primary else Color.Transparent
    val content = if (isSelected) colors.onPrimary else colors.onSurfaceVariant
    val border = if (isSelected) colors.primary else colors.outlineVariant

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
private fun TransportControls(
    state: RadioPlaybackState,
    onTogglePlayback: () -> Unit,
    onReturnToLive: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(colors.primary)
                .clickable(onClick = onTogglePlayback),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isBuffering && state.isPlaying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    strokeWidth = 3.dp,
                    color = colors.onPrimary,
                )
            } else {
                Icon(
                    imageVector = if (state.isPlaying) {
                        Icons.Rounded.Pause
                    } else {
                        Icons.Rounded.PlayArrow
                    },
                    contentDescription = if (state.isPlaying) "Pausar" else "Reproducir",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(42.dp),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        TextButton(
            onClick = onReturnToLive,
            enabled = state.selectedDelay != Delay.ZERO,
        ) {
            Text("Volver al directo")
        }
    }
}

@Composable
private fun BufferStrip(state: RadioPlaybackState) {
    val colors = MaterialTheme.colorScheme
    val capacity = Delay.MAX_SECONDS.toFloat()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceContainer.copy(alpha = 0.7f))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Audio guardado",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
            Text(
                text = Delay.ofSeconds(state.availableDelaySeconds).formatted,
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary,
            )
        }

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { (state.availableDelaySeconds / capacity).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = colors.primary,
            trackColor = colors.outlineVariant,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Máximo ${Delay.ofSeconds(Delay.MAX_SECONDS).formatted}. " +
                "El histórico se guarda solo en memoria mientras la app está activa.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}

/**
 * Mientras se espera a acumular el retraso pedido, el anillo mide el avance
 * hacia ese objetivo; el resto del tiempo mide el histórico frente al máximo.
 */
private fun dialProgress(state: RadioPlaybackState): Float {
    val target = if (state.isWaitingForSelectedDelay) {
        state.selectedDelay.totalSeconds
    } else {
        Delay.MAX_SECONDS
    }
    if (target <= 0) return 0f
    return state.availableDelaySeconds.toFloat() / target
}

private fun dialCaption(state: RadioPlaybackState): String = when {
    state.isWaitingForSelectedDelay ->
        "Acumulando audio: ${Delay.ofSeconds(state.availableDelaySeconds).formatted} " +
            "de ${state.selectedDelay.formatted}"

    state.isPlaying && state.effectiveDelay == Delay.ZERO -> "Sonando en directo"
    state.isPlaying -> "Aplicado ${state.effectiveDelay.formatted}"
    state.selectedDelay == Delay.ZERO -> "Toca para retrasar la emisión"
    else -> "Se aplicará al reproducir"
}
