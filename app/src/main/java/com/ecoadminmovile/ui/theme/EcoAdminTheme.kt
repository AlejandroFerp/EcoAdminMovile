package com.ecoadminmovile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = EcoBlue,
    secondary = EcoSlate,
    tertiary = EcoMint
)

private val DarkColors = darkColorScheme(
    primary = EcoBlue,
    secondary = EcoMint,
    tertiary = EcoGold
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
