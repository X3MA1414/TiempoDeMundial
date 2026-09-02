package com.lahoradelpartido.radiodelay.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DelayTest {
    @Test
    fun `minutes and seconds are exposed independently`() {
        val delay = Delay.of(minutes = 2, seconds = 7)
        assertEquals(127, delay.totalSeconds)
        assertEquals(2, delay.minutes)
        assertEquals(7, delay.seconds)
        assertEquals("02:07", delay.formatted)
    }

    @Test
    fun `total seconds are clamped to the supported range`() {
        assertEquals(Delay.ZERO, Delay.ofSeconds(-20))
        assertEquals(Delay.MAX_SECONDS, Delay.ofSeconds(99_999).totalSeconds)
    }
}
