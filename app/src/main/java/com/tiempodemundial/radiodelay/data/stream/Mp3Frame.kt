package com.tiempodemundial.radiodelay.data.stream

data class Mp3Frame(
    val sequence: Long,
    val startTimeUs: Long,
    val durationUs: Long,
    val bytes: ByteArray,
) {
    val endTimeUs: Long get() = startTimeUs + durationUs
}
