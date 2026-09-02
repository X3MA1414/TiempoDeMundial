package com.lahoradelpartido.radiodelay.data.preferences

import android.content.Context
import com.lahoradelpartido.radiodelay.domain.model.Delay

interface DelayPreferences {
    fun read(): Delay
    fun write(delay: Delay)
}

class SharedPreferencesDelayPreferences(
    context: Context,
) : DelayPreferences {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun read(): Delay = Delay.ofSeconds(
        preferences.getInt(KEY_DELAY_SECONDS, 0),
    )

    override fun write(delay: Delay) {
        preferences.edit().putInt(KEY_DELAY_SECONDS, delay.totalSeconds).apply()
    }

    private companion object {
        const val FILE_NAME = "radio_delay_preferences"
        const val KEY_DELAY_SECONDS = "delay_seconds"
    }
}
