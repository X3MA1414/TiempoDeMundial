package com.lahoradelpartido.radiodelay.data.catalog

import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.EmissionKind
import java.time.ZonedDateTime
import org.json.JSONArray
import org.json.JSONObject

/**
 * Traduce la parrilla publicada por COPE a la lista de señales que suenan en un
 * instante concreto.
 *
 * El documento agrupa la semana en `d0`..`d6`, donde `d0` es el lunes, y cada
 * día contiene bloques con su minuto de inicio y de fin. El bloque apunta a la
 * señal que lo emite y su campo `se` guarda las emisiones simultáneas: ahí es
 * donde aparece el fútbol cuando la señal principal está dando otro programa.
 *
 * No hace red ni toca Android, de modo que la parte delicada (qué está sonando
 * ahora) se puede comprobar con pruebas normales.
 */
object CopeScheduleParser {

    fun parse(scheduleJson: String, at: ZonedDateTime): List<Emission> {
        val schedule = runCatching { JSONObject(scheduleJson) }.getOrNull() ?: return emptyList()
        val dayKey = "d${at.dayOfWeek.value - 1}"
        val minuteOfDay = at.hour * 60 + at.minute

        val blocks = schedule.optJSONObject("prg")
            ?.optJSONObject(dayKey)
            ?.optJSONArray("es")
            ?: return emptyList()

        val emissions = mutableListOf<Emission>()
        for (index in 0 until blocks.length()) {
            val block = blocks.optJSONObject(index) ?: continue
            appendIfLive(block, minuteOfDay, emissions)

            val simultaneous = block.optJSONArray("se") ?: JSONArray()
            for (position in 0 until simultaneous.length()) {
                val alternative = simultaneous.optJSONObject(position) ?: continue
                appendIfLive(alternative, minuteOfDay, emissions)
            }
        }
        return emissions.distinctBy(Emission::streamUrl)
    }

    private fun appendIfLive(
        block: JSONObject,
        minuteOfDay: Int,
        destination: MutableList<Emission>,
    ) {
        val from = block.optInt("from", -1)
        val to = block.optInt("to", -1)
        if (from < 0 || to < from || minuteOfDay < from || minuteOfDay > to) return

        val streamUrl = block.optJSONObject("track")?.optString("track").orEmpty()
        if (!streamUrl.startsWith("http")) return

        destination += Emission(
            id = signalIdOf(streamUrl),
            title = block.optString("title").ifBlank { "Emisión COPE" },
            subtitle = block.optString("lead").ifBlank { "En directo" },
            schedule = block.optString("horario").ifBlank { formatWindow(from, to) },
            streamUrl = streamUrl,
            kind = if (isSports(block)) EmissionKind.SPORTS else EmissionKind.GENERAL,
        )
    }

    /**
     * COPE etiqueta los bloques deportivos con `head = "deportes"`, pero no lo
     * hace en todos, así que el título sirve de red de seguridad.
     */
    private fun isSports(block: JSONObject): Boolean {
        if (block.optString("head").equals("deportes", ignoreCase = true)) return true
        val title = block.optString("title").lowercase()
        return SPORTS_KEYWORDS.any(title::contains)
    }

    private fun signalIdOf(streamUrl: String): String =
        SIGNAL_ID.find(streamUrl)?.groupValues?.get(1) ?: streamUrl

    private fun formatWindow(from: Int, to: Int): String =
        "%02d:%02d - %02d:%02d".format(from / 60 % 24, from % 60, to / 60 % 24, to % 60)

    private val SPORTS_KEYWORDS =
        listOf("tiempo de juego", "partidazo", "deportes", "carrusel")
    private val SIGNAL_ID = Regex("""/cope/(net\d+)""")
}
