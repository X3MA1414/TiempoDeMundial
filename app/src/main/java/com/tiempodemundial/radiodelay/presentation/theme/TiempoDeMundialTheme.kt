package com.tiempodemundial.radiodelay.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Paleta inspirada en la bandera de España.
 *
 * El rojo se reserva para navegación y acciones principales, mientras que el
 * amarillo identifica el dato central de la experiencia: el retraso elegido.
 * Los fondos neutros reducen la fatiga visual y mantienen un contraste accesible.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFFAA151B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF1BF00),
    onPrimaryContainer = Color(0xFF2B2100),
    inversePrimary = Color(0xFFFFB3B1),

    secondary = Color(0xFF8A1116),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD7),
    onSecondaryContainer = Color(0xFF5C0008),

    tertiary = Color(0xFF795900),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF261A00),

    background = Color(0xFFFFF9F2),
    onBackground = Color(0xFF211A18),
    surface = Color(0xFFFFF9F2),
    onSurface = Color(0xFF211A18),
    surfaceVariant = Color(0xFFF3E1DC),
    onSurfaceVariant = Color(0xFF534340),
    surfaceContainer = Color(0xFFFFF3EA),
    surfaceContainerHigh = Color(0xFFF8EAE1),

    outline = Color(0xFF85736F),
    outlineVariant = Color(0xFFD8C2BD),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC72C32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4B500),
    onPrimaryContainer = Color(0xFF211A00),
    inversePrimary = Color(0xFFAA151B),

    secondary = Color(0xFFF1BF00),
    onSecondary = Color(0xFF2B2100),
    secondaryContainer = Color(0xFF7C1116),
    onSecondaryContainer = Color(0xFFFFDAD7),

    tertiary = Color(0xFFFFD95B),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF5B4300),
    onTertiaryContainer = Color(0xFFFFE08A),

    background = Color(0xFF19110F),
    onBackground = Color(0xFFF2DEDA),
    surface = Color(0xFF19110F),
    onSurface = Color(0xFFF2DEDA),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BD),
    surfaceContainer = Color(0xFF261B18),
    surfaceContainerHigh = Color(0xFF312522),

    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF534340),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun TiempoDeMundialTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            window.navigationBarColor = colors.background.toArgb()

            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
