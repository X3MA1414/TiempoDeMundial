package com.lahoradelpartido.radiodelay.domain.model

/**
 * Una señal concreta de la emisora.
 *
 * COPE reparte su programación entre varias señales simultáneas: mientras una
 * emite el programa generalista, otra puede estar dando el partido. Cada
 * [Emission] representa una de esas señales tal y como está sonando ahora.
 */
data class Emission(
    val id: String,
    val title: String,
    val subtitle: String,
    val schedule: String,
    val streamUrl: String,
    val kind: EmissionKind,
) {
    val isSports: Boolean get() = kind == EmissionKind.SPORTS

    companion object {
        /**
         * Señal principal. Se usa como punto de partida cuando todavía no se ha
         * consultado la parrilla o cuando la consulta falla.
         */
        val NATIONAL = Emission(
            id = "net1",
            title = "COPE",
            subtitle = "Emisión nacional",
            schedule = "24 h",
            streamUrl = "https://net1-cope-flucast.flumotion.com/cope/net1.mp3.m3u",
            kind = EmissionKind.GENERAL,
        )

        /** Señal donde COPE emite habitualmente el carrusel deportivo. */
        val SPORTS = Emission(
            id = "net2",
            title = "COPE Deportes",
            subtitle = "Segunda señal, carrusel deportivo",
            schedule = "Según partido",
            streamUrl = "https://net2-cope-flucast.flumotion.com/cope/net2.mp3.m3u",
            kind = EmissionKind.SPORTS,
        )

        /**
         * Señales adicionales que COPE abre en jornadas con varios encuentros a
         * la vez. No aparecen en la parrilla publicada, así que se ofrecen
         * siempre y puede que estén en silencio fuera de esos días.
         */
        val EXTRA_SIGNALS = listOf(
            Emission(
                id = "net3",
                title = "Señal extra 3",
                subtitle = "Partido alternativo",
                schedule = "Solo jornadas con varios partidos",
                streamUrl = "https://net3-cope-flucast.flumotion.com/cope/net3.mp3.m3u",
                kind = EmissionKind.EXTRA,
            ),
            Emission(
                id = "net4",
                title = "Señal extra 4",
                subtitle = "Partido alternativo",
                schedule = "Solo jornadas con varios partidos",
                streamUrl = "https://net4-cope-flucast.flumotion.com/cope/net4.mp3.m3u",
                kind = EmissionKind.EXTRA,
            ),
        )
    }
}

enum class EmissionKind {
    /** Retransmisión deportiva: es la que casi siempre busca el oyente. */
    SPORTS,

    /** Programa generalista que suena a la vez en otra señal. */
    GENERAL,

    /** Señal auxiliar sin programa publicado en la parrilla. */
    EXTRA,
}
