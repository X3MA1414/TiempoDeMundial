package com.lahoradelpartido.radiodelay.data.catalog

import com.lahoradelpartido.radiodelay.domain.model.Emission
import com.lahoradelpartido.radiodelay.domain.model.EmissionKind
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CopeScheduleParserTest {

    /**
     * Fragmento con la misma forma que publica COPE: un lunes por la tarde con
     * "La Tarde" en la señal principal y el carrusel deportivo en la segunda.
     */
    private val schedule = """
        {
          "prg": {
            "d0": {
              "es": [
                {
                  "from": 900, "to": 1139,
                  "title": "La Tarde", "lead": "Con Pilar Cisneros",
                  "horario": "16:00h a 19:00h", "head": null,
                  "track": {"track": "https://net1-cope-flucast.flumotion.com/cope/net1.mp3.m3u"},
                  "se": [
                    {
                      "from": 905, "to": 929,
                      "title": "Deportes COPE", "lead": "Con Corrochano",
                      "horario": "15:05h a 15:30h", "head": "deportes",
                      "track": {"track": "https://net2-cope-flucast.flumotion.com/cope/net2.mp3.m3u"}
                    }
                  ]
                },
                {
                  "from": 1410, "to": 1439,
                  "title": "El Partidazo de COPE", "lead": "Con Juanma Castano",
                  "horario": "23:30h a 01:30h", "head": null,
                  "track": {"track": "https://net2-cope-flucast.flumotion.com/cope/net2.mp3.m3u"},
                  "se": []
                }
              ]
            }
          }
        }
    """.trimIndent()

    private fun mondayAt(hour: Int, minute: Int): ZonedDateTime =
        ZonedDateTime.of(2026, 8, 31, hour, minute, 0, 0, ZoneId.of("Europe/Madrid"))

    @Test
    fun `devuelve las dos senales cuando coinciden en el tiempo`() {
        val emissions = CopeScheduleParser.parse(schedule, mondayAt(15, 10))

        assertEquals(listOf("La Tarde", "Deportes COPE"), emissions.map(Emission::title))
        assertEquals(listOf("net1", "net2"), emissions.map(Emission::id))
    }

    @Test
    fun `marca como deportiva la emision simultanea etiquetada por COPE`() {
        val emissions = CopeScheduleParser.parse(schedule, mondayAt(15, 10))

        val sports = emissions.single(Emission::isSports)
        assertEquals("Deportes COPE", sports.title)
        assertEquals(EmissionKind.GENERAL, emissions.first().kind)
    }

    @Test
    fun `descarta las emisiones simultaneas que ya han terminado`() {
        val emissions = CopeScheduleParser.parse(schedule, mondayAt(17, 0))

        assertEquals(listOf("La Tarde"), emissions.map(Emission::title))
    }

    @Test
    fun `reconoce el deporte por el titulo cuando no hay etiqueta`() {
        val emissions = CopeScheduleParser.parse(schedule, mondayAt(23, 40))

        assertEquals(listOf("El Partidazo de COPE"), emissions.map(Emission::title))
        assertTrue(emissions.single().isSports)
    }

    @Test
    fun `no devuelve nada cuando el dia no esta publicado`() {
        val tuesday = mondayAt(15, 10).plusDays(1)

        assertTrue(CopeScheduleParser.parse(schedule, tuesday).isEmpty())
    }

    @Test
    fun `tolera una respuesta que no es JSON`() {
        assertTrue(CopeScheduleParser.parse("<html>error</html>", mondayAt(15, 10)).isEmpty())
    }
}
