package com.lahoradelpartido.radiodelay.domain.model

/** Immutable delay selected by the user. */
data class Delay private constructor(
    val totalSeconds: Int,
) {
    val minutes: Int get() = totalSeconds / 60
    val seconds: Int get() = totalSeconds % 60
    val formatted: String get() = "%02d:%02d".format(minutes, seconds)

    companion object {
        const val MAX_MINUTES = 5
        const val MAX_SECONDS = MAX_MINUTES * 60 + 59

        val ZERO = Delay(0)

        fun of(minutes: Int, seconds: Int): Delay {
            require(minutes in 0..MAX_MINUTES) { "Minutos fuera de rango" }
            require(seconds in 0..59) { "Segundos fuera de rango" }
            return ofSeconds(minutes * 60 + seconds)
        }

        fun ofSeconds(totalSeconds: Int): Delay =
            Delay(totalSeconds.coerceIn(0, MAX_SECONDS))
    }
}
