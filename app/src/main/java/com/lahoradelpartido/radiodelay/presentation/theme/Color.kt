package com.lahoradelpartido.radiodelay.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta "partido de noche": fondos muy oscuros de estadio, verde césped para
 * las acciones y ámbar para el dato que define la app, el retraso.
 *
 * El rojo queda reservado al indicador de directo y a los errores, de modo que
 * nunca compite con las acciones principales.
 */
internal object Palette {
    val GreenBright = Color(0xFF3CE07E)
    val GreenDeep = Color(0xFF0E8F4C)
    val GreenInk = Color(0xFF04240F)
    val GreenSoftSurface = Color(0xFF10301F)
    val GreenSoftInk = Color(0xFFB6F5CE)

    val Amber = Color(0xFFFFC24B)
    val AmberDeep = Color(0xFF8A5A00)
    val AmberInk = Color(0xFF2A1B00)
    val AmberSoftSurface = Color(0xFF3A2A05)
    val AmberSoftInk = Color(0xFFFFE3A6)

    val LiveRed = Color(0xFFFF4D5E)
    val LiveRedDeep = Color(0xFFB3121F)

    val NightBackground = Color(0xFF070B12)
    val NightSurface = Color(0xFF0D1420)
    val NightSurfaceHigh = Color(0xFF151F2E)
    val NightSurfaceHighest = Color(0xFF1D2A3B)
    val NightOutline = Color(0xFF2B3A4E)
    val NightOutlineSoft = Color(0xFF1B2634)
    val NightInk = Color(0xFFE9EFF7)
    val NightInkMuted = Color(0xFF9BABC0)

    val DayBackground = Color(0xFFF3F6FA)
    val DaySurface = Color(0xFFFFFFFF)
    val DaySurfaceHigh = Color(0xFFEDF1F7)
    val DaySurfaceHighest = Color(0xFFE3EAF3)
    val DayOutline = Color(0xFFC3CEDC)
    val DayOutlineSoft = Color(0xFFDDE4EE)
    val DayInk = Color(0xFF0B1220)
    val DayInkMuted = Color(0xFF56657A)
}
