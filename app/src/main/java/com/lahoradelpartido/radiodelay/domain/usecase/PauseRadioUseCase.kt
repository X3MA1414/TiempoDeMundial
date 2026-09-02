package com.lahoradelpartido.radiodelay.domain.usecase

import com.lahoradelpartido.radiodelay.domain.gateway.RadioController

class PauseRadioUseCase(
    private val controller: RadioController,
) {
    operator fun invoke() = controller.pause()
}
