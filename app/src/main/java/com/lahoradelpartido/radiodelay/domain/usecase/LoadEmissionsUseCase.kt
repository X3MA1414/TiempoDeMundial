package com.lahoradelpartido.radiodelay.domain.usecase

import com.lahoradelpartido.radiodelay.domain.gateway.EmissionCatalog
import com.lahoradelpartido.radiodelay.domain.model.Emission

class LoadEmissionsUseCase(
    private val catalog: EmissionCatalog,
) {
    suspend operator fun invoke(): List<Emission> = catalog.currentEmissions()
}
