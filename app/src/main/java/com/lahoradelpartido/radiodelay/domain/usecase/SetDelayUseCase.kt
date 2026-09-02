package com.lahoradelpartido.radiodelay.domain.usecase

import com.lahoradelpartido.radiodelay.domain.gateway.RadioController
import com.lahoradelpartido.radiodelay.domain.model.Delay

class SetDelayUseCase(
    private val controller: RadioController,
) {
    operator fun invoke(delay: Delay) = controller.setDelay(delay)
}
