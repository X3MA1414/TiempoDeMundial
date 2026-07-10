package com.albertocastell.radiodelay.data.stream

import com.albertocastell.radiodelay.domain.model.Delay
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class Mp3CircularBufferTest {
    @Test
    fun `reader starts near the requested delay`() {
        val buffer = Mp3CircularBuffer(maxDurationUs = 10_000_000L)
        repeat(5) { value ->
            buffer.append(
                Mp3FrameParser.ParsedMp3Frame(
                    bytes = byteArrayOf(value.toByte()),
                    durationUs = 1_000_000L,
                    sampleRateHz = 44_100,
                    bitrateKbps = 128,
                ),
            )
        }

        val reader = buffer.openReader(Delay.ofSeconds(2))
        val result = ByteArray(1)
        reader.read(result, 0, 1)

        assertArrayEquals(byteArrayOf(3), result)
        reader.close()
    }
}
