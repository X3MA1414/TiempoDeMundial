package com.lahoradelpartido.radiodelay.data.catalog

import com.lahoradelpartido.radiodelay.domain.gateway.EmissionCatalog
import com.lahoradelpartido.radiodelay.domain.model.Emission
import java.io.IOException
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Lee la parrilla que alimenta al reproductor de cope.es y devuelve las señales
 * que están sonando ahora mismo.
 *
 * Si la consulta falla se devuelven las señales conocidas, de modo que el
 * selector nunca se queda vacío y el oyente siempre puede cambiar de emisión.
 */
class CopeEmissionCatalog(
    private val client: OkHttpClient = defaultClient(),
    private val now: () -> ZonedDateTime = { ZonedDateTime.now(BROADCAST_ZONE) },
) : EmissionCatalog {

    override suspend fun currentEmissions(): List<Emission> = withContext(Dispatchers.IO) {
        val live = runCatching { CopeScheduleParser.parse(downloadSchedule(), now()) }
            .getOrDefault(emptyList())

        val ordered = (live + KNOWN_EMISSIONS)
            .distinctBy(Emission::streamUrl)
            .sortedBy { emission -> emission.kind.ordinal }

        ordered + Emission.EXTRA_SIGNALS.filterNot { extra ->
            ordered.any { it.streamUrl == extra.streamUrl }
        }
    }

    private fun downloadSchedule(): String {
        val request = Request.Builder()
            .url(SCHEDULE_URL)
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "https://www.cope.es/programas/tiempo-de-juego")
            .header("User-Agent", USER_AGENT)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("La parrilla respondió con HTTP ${response.code}")
            }
            return response.body.string()
        }
    }

    private companion object {
        const val SCHEDULE_URL = "https://www.cope.es/ply/prg"
        const val USER_AGENT = "LaHoraDelPartido/2.0 Android"
        val BROADCAST_ZONE: ZoneId = ZoneId.of("Europe/Madrid")
        val KNOWN_EMISSIONS = listOf(Emission.SPORTS, Emission.NATIONAL)

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}
