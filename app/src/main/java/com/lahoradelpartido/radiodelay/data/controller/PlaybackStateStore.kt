package com.lahoradelpartido.radiodelay.data.controller

import com.lahoradelpartido.radiodelay.domain.model.Delay
import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.RadioPlaybackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-scoped state shared by the UI and the playback service. */
class PlaybackStateStore(
    initialDelay: Delay,
    initialEmission: Emission,
) {
    private val mutableState = MutableStateFlow(
        RadioPlaybackState(
            selectedDelay = initialDelay,
            selectedEmission = initialEmission,
        ),
    )

    val state: StateFlow<RadioPlaybackState> = mutableState.asStateFlow()

    fun update(transform: (RadioPlaybackState) -> RadioPlaybackState) {
        mutableState.update(transform)
    }
}
