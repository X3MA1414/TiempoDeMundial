package com.tiempodemundial.radiodelay.data.stream

import kotlin.math.max

/** Incremental MPEG Layer III frame parser with resynchronisation. */
class Mp3FrameParser(
    private val onFrame: (ParsedMp3Frame) -> Unit,
) {
    private var pending = ByteArray(INITIAL_CAPACITY)
    private var start = 0
    private var end = 0

    fun consume(source: ByteArray, count: Int = source.size) {
        require(count in 0..source.size)
        ensureCapacity(count)
        source.copyInto(pending, end, 0, count)
        end += count
        parseAvailableFrames()
        compactIfNeeded()
    }

    private fun parseAvailableFrames() {
        while (end - start >= HEADER_SIZE) {
            val id3Length = id3TagLengthAt(start)
            if (id3Length != null) {
                if (end - start < id3Length) return
                start += id3Length
                continue
            }

            val header = parseHeader(pending, start)
            if (header == null) {
                start += 1
                continue
            }
            if (end - start < header.frameLength) return

            val bytes = pending.copyOfRange(start, start + header.frameLength)
            onFrame(
                ParsedMp3Frame(
                    bytes = bytes,
                    durationUs = header.durationUs,
                    sampleRateHz = header.sampleRateHz,
                    bitrateKbps = header.bitrateKbps,
                ),
            )
            start += header.frameLength
        }
    }

    private fun id3TagLengthAt(offset: Int): Int? {
        if (end - offset < 3) return null
        if (pending[offset].toInt() != 'I'.code ||
            pending[offset + 1].toInt() != 'D'.code ||
            pending[offset + 2].toInt() != '3'.code
        ) return null
        if (end - offset < 10) return 10

        val flags = pending[offset + 5].toInt() and 0xFF
        val size = ((pending[offset + 6].toInt() and 0x7F) shl 21) or
            ((pending[offset + 7].toInt() and 0x7F) shl 14) or
            ((pending[offset + 8].toInt() and 0x7F) shl 7) or
            (pending[offset + 9].toInt() and 0x7F)
        val footer = if (flags and 0x10 != 0) 10 else 0
        return 10 + size + footer
    }

    private fun ensureCapacity(extra: Int) {
        val currentSize = end - start
        if (pending.size - end >= extra) return
        if (start > 0 && pending.size - currentSize >= extra) {
            pending.copyInto(pending, 0, start, end)
            start = 0
            end = currentSize
            return
        }

        val required = currentSize + extra
        val newCapacity = max(pending.size * 2, required)
        val replacement = ByteArray(newCapacity)
        pending.copyInto(replacement, 0, start, end)
        pending = replacement
        start = 0
        end = currentSize
    }

    private fun compactIfNeeded() {
        if (start == 0) return
        if (start < pending.size / 2 && end < pending.size) return
        val remaining = end - start
        pending.copyInto(pending, 0, start, end)
        start = 0
        end = remaining
    }

    data class ParsedMp3Frame(
        val bytes: ByteArray,
        val durationUs: Long,
        val sampleRateHz: Int,
        val bitrateKbps: Int,
    )

    private data class Header(
        val frameLength: Int,
        val durationUs: Long,
        val sampleRateHz: Int,
        val bitrateKbps: Int,
    )

    companion object {
        private const val INITIAL_CAPACITY = 64 * 1024
        private const val HEADER_SIZE = 4

        private val MPEG1_LAYER3_BITRATES = intArrayOf(
            0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0,
        )
        private val MPEG2_LAYER3_BITRATES = intArrayOf(
            0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0,
        )
        private val MPEG1_SAMPLE_RATES = intArrayOf(44_100, 48_000, 32_000)

        private fun parseHeader(bytes: ByteArray, offset: Int): Header? {
            val b0 = bytes[offset].toInt() and 0xFF
            val b1 = bytes[offset + 1].toInt() and 0xFF
            val b2 = bytes[offset + 2].toInt() and 0xFF

            if (b0 != 0xFF || b1 and 0xE0 != 0xE0) return null

            val versionBits = (b1 ushr 3) and 0x03
            if (versionBits == 0x01) return null

            val layerBits = (b1 ushr 1) and 0x03
            if (layerBits != 0x01) return null

            val bitrateIndex = (b2 ushr 4) and 0x0F
            val sampleRateIndex = (b2 ushr 2) and 0x03
            if (bitrateIndex == 0 || bitrateIndex == 0x0F || sampleRateIndex == 0x03) return null

            val isMpeg1 = versionBits == 0x03
            val bitrateKbps = if (isMpeg1) {
                MPEG1_LAYER3_BITRATES[bitrateIndex]
            } else {
                MPEG2_LAYER3_BITRATES[bitrateIndex]
            }
            if (bitrateKbps <= 0) return null

            val divisor = when (versionBits) {
                0x03 -> 1
                0x02 -> 2
                0x00 -> 4
                else -> return null
            }
            val sampleRate = MPEG1_SAMPLE_RATES[sampleRateIndex] / divisor
            val padding = (b2 ushr 1) and 0x01
            val coefficient = if (isMpeg1) 144_000 else 72_000
            val frameLength = coefficient * bitrateKbps / sampleRate + padding
            if (frameLength !in HEADER_SIZE..MAX_FRAME_SIZE) return null

            val samplesPerFrame = if (isMpeg1) 1_152 else 576
            val durationUs = samplesPerFrame * 1_000_000L / sampleRate
            return Header(frameLength, durationUs, sampleRate, bitrateKbps)
        }

        private const val MAX_FRAME_SIZE = 8 * 1024
    }
}
