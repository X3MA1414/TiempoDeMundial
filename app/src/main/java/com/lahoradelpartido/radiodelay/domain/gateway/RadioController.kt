package com.lahoradelpartido.radiodelay.domain.gateway

import com.lahoradelpartido.radiodelay.domain.model.Delay
import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.RadioPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface RadioController {
    val state: StateFlow<RadioPlaybackState>

    fun play()
    fun pause()
    fun setDelay(delay: Delay)
    fun returnToLive()
    fun selectEmission(emission: Emission)
}
