package com.lahoradelpartido.radiodelay.domain.usecase

import com.lahoradelpartido.radiodelay.domain.gateway.RadioController

class PlayRadioUseCase(
    private val controller: RadioController,
) {
    operator fun invoke() = controller.play()
}
