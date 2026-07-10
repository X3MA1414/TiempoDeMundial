package com.tiempodemundial.radiodelay.domain.usecase

import com.tiempodemundial.radiodelay.domain.gateway.RadioController

class PlayRadioUseCase(
    private val controller: RadioController,
) {
    operator fun invoke() = controller.play()
}
