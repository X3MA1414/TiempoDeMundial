package com.lahoradelpartido.radiodelay.data.preferences

import android.content.Context
import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.EmissionKind

interface EmissionPreferences {
    /** Señal elegida por el oyente, o `null` si todavía no ha elegido ninguna. */
    fun read(): Emission?
    fun write(emission: Emission)
}

class SharedPreferencesEmissionPreferences(
    context: Context,
) : EmissionPreferences {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun read(): Emission? {
        val streamUrl = preferences.getString(KEY_STREAM_URL, null) ?: return null
        return Emission(
            id = preferences.getString(KEY_ID, streamUrl).orEmpty(),
            title = preferences.getString(KEY_TITLE, "COPE").orEmpty(),
            subtitle = preferences.getString(KEY_SUBTITLE, "").orEmpty(),
            schedule = preferences.getString(KEY_SCHEDULE, "").orEmpty(),
            streamUrl = streamUrl,
            kind = runCatching {
                EmissionKind.valueOf(preferences.getString(KEY_KIND, null).orEmpty())
            }.getOrDefault(EmissionKind.GENERAL),
        )
    }

    override fun write(emission: Emission) {
        preferences.edit()
            .putString(KEY_ID, emission.id)
            .putString(KEY_TITLE, emission.title)
            .putString(KEY_SUBTITLE, emission.subtitle)
            .putString(KEY_SCHEDULE, emission.schedule)
            .putString(KEY_STREAM_URL, emission.streamUrl)
            .putString(KEY_KIND, emission.kind.name)
            .apply()
    }

    private companion object {
        const val FILE_NAME = "radio_emission_preferences"
        const val KEY_ID = "emission_id"
        const val KEY_TITLE = "emission_title"
        const val KEY_SUBTITLE = "emission_subtitle"
        const val KEY_SCHEDULE = "emission_schedule"
        const val KEY_STREAM_URL = "emission_stream_url"
        const val KEY_KIND = "emission_kind"
    }
}
