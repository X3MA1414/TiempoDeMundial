package com.tiempodemundial.radiodelay.domain.usecase

import com.tiempodemundial.radiodelay.domain.gateway.RadioController
import com.tiempodemundial.radiodelay.domain.model.Delay

class SetDelayUseCase(
    private val controller: RadioController,
) {
    operator fun invoke(delay: Delay) = controller.setDelay(delay)
}
