package com.tiempodemundial.radiodelay.domain.usecase

import com.tiempodemundial.radiodelay.domain.gateway.RadioController
import com.tiempodemundial.radiodelay.domain.model.RadioPlaybackState
import kotlinx.coroutines.flow.StateFlow

class ObserveRadioStateUseCase(
    private val controller: RadioController,
) {
    operator fun invoke(): StateFlow<RadioPlaybackState> = controller.state
}
