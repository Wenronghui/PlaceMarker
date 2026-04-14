package com.footprint.footprint.ui.theme

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

// Colors
val Primary = Color(0xFF2E7D32)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFA5D6A7)
val OnPrimaryContainer = Color(0xFF1B5E20)

val Secondary = Color(0xFF795548)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFD7CCC8)
val OnSecondaryContainer = Color(0xFF4E342E)

val Tertiary = Color(0xFF1565C0)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFF90CAF9)
val OnTertiaryContainer = Color(0xFF0D47A1)

val Background = Color(0xFFFAFAFA)
val OnBackground = Color(0xFF1C1B1F)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1C1B1F)

val Error = Color(0xFFB00020)
val OnError = Color(0xFFFFFFFF)

// Marker category colors
val MarkerCamp = Color(0xFFFF9800)
val MarkerPeak = Color(0xFFF44336)
val MarkerWater = Color(0xFF2196F3)
val MarkerView = Color(0xFF9C27B0)
val MarkerParking = Color(0xFF607D8B)
val MarkerOther = Color(0xFF4CAF50)

val TrackColor = Color(0xFF1976D2)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    error = Error,
    onError = OnError
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryContainer,
    onPrimary = OnPrimaryContainer,
    primaryContainer = Primary,
    onPrimaryContainer = OnPrimary,
    secondary = SecondaryContainer,
    onSecondary = OnSecondaryContainer,
    secondaryContainer = Secondary,
    onSecondaryContainer = OnSecondary,
    tertiary = TertiaryContainer,
    onTertiary = OnTertiaryContainer,
    tertiaryContainer = Tertiary,
    onTertiaryContainer = OnTertiary,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000)
)

@Composable
fun PlaceMarkerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
