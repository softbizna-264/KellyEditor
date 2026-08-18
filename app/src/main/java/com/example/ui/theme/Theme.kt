package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = KellyCyan,
    onPrimary = Color.Black,
    primaryContainer = StudioSurfaceElevated,
    onPrimaryContainer = KellyCyan,
    secondary = KellyTeal,
    onSecondary = Color.Black,
    secondaryContainer = StudioSurfaceCard,
    onSecondaryContainer = KellyTeal,
    tertiary = KellyViolet,
    background = StudioDarkBackground,
    onBackground = StudioTextPrimary,
    surface = StudioSurfaceDark,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceCard,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder,
    error = KellyCoral
)

private val LightColorScheme = darkColorScheme(
    primary = KellyCyan,
    onPrimary = Color.Black,
    primaryContainer = StudioSurfaceElevated,
    onPrimaryContainer = KellyCyan,
    secondary = KellyTeal,
    onSecondary = Color.Black,
    secondaryContainer = StudioSurfaceCard,
    onSecondaryContainer = KellyTeal,
    tertiary = KellyViolet,
    background = StudioDarkBackground,
    onBackground = StudioTextPrimary,
    surface = StudioSurfaceDark,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceCard,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder,
    error = KellyCoral
)

@Composable
fun KellyEditorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Creative photo editing studios benefit best from dedicated dark studio scheme
    // to allow accurate color perception without distracting white borders.
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
