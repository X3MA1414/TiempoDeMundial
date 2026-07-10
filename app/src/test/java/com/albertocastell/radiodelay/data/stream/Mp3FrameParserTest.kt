package com.tiempodemundial.radiodelay.data.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Mp3FrameParserTest {
    @Test
    fun `parses a fragmented mpeg1 layer3 frame`() {
        val output = mutableListOf<Mp3FrameParser.ParsedMp3Frame>()
        val parser = Mp3FrameParser(output::add)
        val frame = syntheticFrame()

        parser.consume(frame.copyOfRange(0, 100))
        parser.consume(frame.copyOfRange(100, frame.size))

        assertEquals(1, output.size)
        assertEquals(44_100, output.single().sampleRateHz)
        assertEquals(128, output.single().bitrateKbps)
        assertEquals(frame.size, output.single().bytes.size)
        assertTrue(output.single().durationUs in 26_000L..26_200L)
    }

    private fun syntheticFrame(): ByteArray {
        // MPEG-1, Layer III, 128 kbps, 44.1 kHz, no padding -> 417 bytes.
        return ByteArray(417).apply {
            this[0] = 0xFF.toByte()
            this[1] = 0xFB.toByte()
            this[2] = 0x90.toByte()
            this[3] = 0x64.toByte()
        }
    }
}
