package com.lahoradelpartido.radiodelay.domain.usecase

import com.lahoradelpartido.radiodelay.domain.gateway.RadioController
import com.lahoradelpartido.radiodelay.domain.model.RadioPlaybackState
import kotlinx.coroutines.flow.StateFlow

class ObserveRadioStateUseCase(
    private val controller: RadioController,
) {
    operator fun invoke(): StateFlow<RadioPlaybackState> = controller.state
}
