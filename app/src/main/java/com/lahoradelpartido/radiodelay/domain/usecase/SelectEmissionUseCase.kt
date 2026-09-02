package com.lahoradelpartido.radiodelay.domain.usecase

import com.lahoradelpartido.radiodelay.domain.gateway.RadioController
import com.lahoradelpartido.radiodelay.domain.model.Emission

class SelectEmissionUseCase(
    private val controller: RadioController,
) {
    operator fun invoke(emission: Emission) = controller.selectEmission(emission)
}
