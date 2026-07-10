package com.tiempodemundial.radiodelay.domain.gateway

import com.tiempodemundial.radiodelay.domain.model.Delay
import com.tiempodemundial.radiodelay.domain.model.RadioPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface RadioController {
    val state: StateFlow<RadioPlaybackState>

    fun play()
    fun pause()
    fun setDelay(delay: Delay)
    fun returnToLive()
}
