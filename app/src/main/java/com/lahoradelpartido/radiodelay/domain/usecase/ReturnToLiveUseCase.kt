package com.lahoradelpartido.radiodelay.domain.usecase

import com.lahoradelpartido.radiodelay.domain.gateway.RadioController

class ReturnToLiveUseCase(
    private val controller: RadioController,
) {
    operator fun invoke() = controller.returnToLive()
}
