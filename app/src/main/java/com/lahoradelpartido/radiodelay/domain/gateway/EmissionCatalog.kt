package com.lahoradelpartido.radiodelay.domain.gateway

import com.lahoradelpartido.radiodelay.domain.model.Emission

/** Puerto que describe qué señales están sonando en este momento. */
interface EmissionCatalog {
    suspend fun currentEmissions(): List<Emission>
}
