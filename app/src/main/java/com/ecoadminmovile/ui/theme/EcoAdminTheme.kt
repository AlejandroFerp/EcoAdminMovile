package com.ecoadminmovile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = EcoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBF2FF), // Very light blue for containers
    onPrimaryContainer = EcoPrimaryDark,
    secondary = EcoTextStrong,
    onSecondary = Color.White,
    tertiary = EcoCompletedDot,
    onTertiary = Color.White,
    background = EcoBg,
    onBackground = EcoText,
    surface = EcoSurface,
    onSurface = EcoText,
    surfaceVariant = Color.White,
    onSurfaceVariant = EcoTextMuted,
    outline = EcoBorder,
    error = EcoDanger,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = EcoPrimary,
    onPrimary = Color.White,
    secondary = EcoCompletedDot,
    onSecondary = Color.Black,
    tertiary = EcoPendingDot,
    onTertiary = Color.Black,
    background = Color(0xFF0F172A), // Dark slate
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    outline = Color(0xFF334155),
    error = EcoDanger
)

@Composable
fun EcoAdminTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
