package com.tiempodemundial.radiodelay.data.controller

import com.tiempodemundial.radiodelay.domain.model.Delay
import com.tiempodemundial.radiodelay.domain.model.RadioPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-scoped state shared by the UI and the playback service. */
class PlaybackStateStore(initialDelay: Delay) {
    private val mutableState = MutableStateFlow(
        RadioPlaybackState(selectedDelay = initialDelay),
    )

    val state: StateFlow<RadioPlaybackState> = mutableState.asStateFlow()

    fun update(transform: (RadioPlaybackState) -> RadioPlaybackState) {
        mutableState.update(transform)
    }
}
