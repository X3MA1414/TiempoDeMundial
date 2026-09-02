package com.lahoradelpartido.radiodelay.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Palette.GreenBright,
    onPrimary = Palette.GreenInk,
    primaryContainer = Palette.GreenSoftSurface,
    onPrimaryContainer = Palette.GreenSoftInk,
    inversePrimary = Palette.GreenDeep,

    secondary = Palette.Amber,
    onSecondary = Palette.AmberInk,
    secondaryContainer = Palette.AmberSoftSurface,
    onSecondaryContainer = Palette.AmberSoftInk,

    tertiary = Palette.LiveRed,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF43101A),
    onTertiaryContainer = Color(0xFFFFD4D9),

    background = Palette.NightBackground,
    onBackground = Palette.NightInk,
    surface = Palette.NightSurface,
    onSurface = Palette.NightInk,
    surfaceVariant = Palette.NightSurfaceHigh,
    onSurfaceVariant = Palette.NightInkMuted,
    surfaceContainerLowest = Palette.NightBackground,
    surfaceContainerLow = Palette.NightSurface,
    surfaceContainer = Palette.NightSurfaceHigh,
    surfaceContainerHigh = Palette.NightSurfaceHighest,
    surfaceContainerHighest = Palette.NightSurfaceHighest,

    outline = Palette.NightOutline,
    outlineVariant = Palette.NightOutlineSoft,
    error = Palette.LiveRed,
    onError = Color.White,
    errorContainer = Color(0xFF43101A),
    onErrorContainer = Color(0xFFFFD4D9),
)

private val LightColors = lightColorScheme(
    primary = Palette.GreenDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC7F5DA),
    onPrimaryContainer = Color(0xFF04301A),
    inversePrimary = Palette.GreenBright,

    secondary = Palette.AmberDeep,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7B8),
    onSecondaryContainer = Color(0xFF2A1B00),

    tertiary = Palette.LiveRedDeep,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDADD),
    onTertiaryContainer = Color(0xFF41000A),

    background = Palette.DayBackground,
    onBackground = Palette.DayInk,
    surface = Palette.DaySurface,
    onSurface = Palette.DayInk,
    surfaceVariant = Palette.DaySurfaceHigh,
    onSurfaceVariant = Palette.DayInkMuted,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Palette.DaySurface,
    surfaceContainer = Palette.DaySurfaceHigh,
    surfaceContainerHigh = Palette.DaySurfaceHighest,
    surfaceContainerHighest = Palette.DaySurfaceHighest,

    outline = Palette.DayOutline,
    outlineVariant = Palette.DayOutlineSoft,
    error = Palette.LiveRedDeep,
    onError = Color.White,
    errorContainer = Color(0xFFFFDADD),
    onErrorContainer = Color(0xFF41000A),
)

/**
 * Degradado de fondo de la pantalla principal.
 *
 * Vive en el tema y no en la pantalla para que cualquier otra vista futura
 * arranque con el mismo lienzo sin repetir los colores.
 */
@Composable
fun appBackgroundBrush(darkTheme: Boolean = isSystemInDarkTheme()): Brush = Brush.verticalGradient(
    colors = if (darkTheme) {
        listOf(Color(0xFF0B1A18), Palette.NightBackground, Color(0xFF06121C))
    } else {
        listOf(Color(0xFFE8F6EE), Palette.DayBackground, Color(0xFFEAEFF7))
    },
)

@Composable
fun LaHoraDelPartidoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            // La ventana es de borde a borde, así que solo hay que decidir si los
            // iconos de las barras del sistema se pintan claros u oscuros.
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
