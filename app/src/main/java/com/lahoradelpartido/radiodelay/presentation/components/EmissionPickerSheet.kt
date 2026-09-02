package com.lahoradelpartido.radiodelay.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.EmissionKind

/**
 * Hoja para elegir qué señal se escucha.
 *
 * COPE puede estar emitiendo un programa distinto en cada señal, así que la
 * lista se lee de la parrilla en vivo y las retransmisiones deportivas se
 * marcan para encontrarlas de un vistazo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmissionPickerSheet(
    emissions: List<Emission>,
    selected: Emission,
    isLoading: Boolean,
    loadFailed: Boolean,
    onRefresh: () -> Unit,
    onSelect: (Emission) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Elegir emisión",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Señales que COPE está emitiendo ahora mismo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    TextButton(onClick = onRefresh) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Actualizar")
                    }
                }
            }

            if (loadFailed) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "No se pudo leer la parrilla de COPE. Se muestran las señales conocidas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items = emissions, key = Emission::streamUrl) { emission ->
                    EmissionRow(
                        emission = emission,
                        isSelected = emission.streamUrl == selected.streamUrl,
                        onSelect = {
                            onSelect(emission)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmissionRow(
    emission: Emission,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (isSelected) colors.primaryContainer else colors.surfaceContainer
    val borderColor = if (isSelected) colors.primary else colors.outlineVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(container)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp),
            )
            .selectable(selected = isSelected, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SignalBadge(emission)
        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = emission.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (emission.isSports) {
                    Spacer(Modifier.width(8.dp))
                    SportsTag()
                }
            }
            if (emission.subtitle.isNotBlank()) {
                Text(
                    text = emission.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (emission.schedule.isNotBlank()) {
                Text(
                    text = emission.schedule,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (isSelected) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = "Emisión seleccionada",
                tint = colors.primary,
            )
        }
    }
}

/** Distintivo circular con el número de señal (`net1`, `net2`, …). */
@Composable
private fun SignalBadge(emission: Emission) {
    val colors = MaterialTheme.colorScheme
    val tint = when (emission.kind) {
        EmissionKind.SPORTS -> colors.primary
        EmissionKind.GENERAL -> colors.onSurfaceVariant
        EmissionKind.EXTRA -> colors.secondary
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.4f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emission.id.filter(Char::isDigit).ifBlank { "?" },
            style = MaterialTheme.typography.titleMedium,
            color = tint,
        )
    }
}

@Composable
private fun SportsTag() {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.primary.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.SportsSoccer,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(12.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "DEPORTE",
            style = MaterialTheme.typography.labelSmall,
            color = colors.primary,
        )
    }
}
