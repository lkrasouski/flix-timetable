package com.flix.timetable.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand green. White text on it fails WCAG contrast, so content on green is near-black in both themes.
private val FlixGreen = Color(0xFF73D700)
private val OnFlixGreen = Color(0xFF14240A)

private val LightColors: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF3B6E00),
        onPrimary = Color.White,
        primaryContainer = FlixGreen,
        onPrimaryContainer = OnFlixGreen,
        secondaryContainer = Color(0xFFE3F5CF),
        onSecondaryContainer = Color(0xFF1F3A05),
        background = Color.White,
        surface = Color.White,
        onSurface = Color(0xFF1A1C18),
        onSurfaceVariant = Color(0xFF5B6155),
        outlineVariant = Color(0xFFE1E4DB),
        error = Color(0xFFC4141B),
    )

private val DarkColors: ColorScheme =
    darkColorScheme(
        primary = FlixGreen,
        onPrimary = OnFlixGreen,
        primaryContainer = FlixGreen,
        onPrimaryContainer = OnFlixGreen,
        secondaryContainer = Color(0xFF2A3D17),
        onSecondaryContainer = Color(0xFFCDEFA5),
        background = Color(0xFF121412),
        surface = Color(0xFF121412),
        onSurface = Color(0xFFE3E3DC),
        onSurfaceVariant = Color(0xFFB4BAAC),
        outlineVariant = Color(0xFF2E332B),
        error = Color(0xFFFF8A80),
    )

@Composable
fun FlixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
