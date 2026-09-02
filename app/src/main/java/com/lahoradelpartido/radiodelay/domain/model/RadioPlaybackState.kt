package com.lahoradelpartido.radiodelay.domain.model

data class RadioPlaybackState(
    val selectedDelay: Delay = Delay.ZERO,
    val effectiveDelay: Delay = Delay.ZERO,
    val availableDelaySeconds: Int = 0,
    val selectedEmission: Emission = Emission.NATIONAL,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isWaitingForSelectedDelay: Boolean = false,
    val serviceActive: Boolean = false,
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val errorMessage: String? = null,
)
