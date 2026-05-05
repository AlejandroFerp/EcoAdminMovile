/**
 * Tema principal de la aplicación EcoAdmin (Material 3).
 *
 * Conceptos Kotlin y Compose demostrados:
 * - `private val`: visibilidad privada al archivo; nadie fuera puede acceder directamente.
 * - `lightColorScheme()` / `darkColorScheme()`: funciones con parámetros nombrados (named params).
 *   Kotlin permite llamar funciones indicando el nombre del parámetro para mayor legibilidad.
 * - `isSystemInDarkTheme()`: función booleana del sistema que detecta el modo oscuro.
 * - `if` como expresión: en Kotlin `if` devuelve un valor, así que se puede asignar a un `val`.
 * - `content: @Composable () -> Unit`: parámetro de tipo función composable (trailing lambda).
 *   Esto implementa el **Slot API pattern**: el tema no dibuja UI, solo PROVEE contexto
 *   (colores, tipografía) a sus hijos a través de CompositionLocal.
 *
 * Patrón:
 * - **CompositionLocal / Theme Provider**: `MaterialTheme` inyecta valores que cualquier
 *   descendiente puede leer con `MaterialTheme.colorScheme`, sin pasarlos explícitamente.
 */
package com.ecoadminmovile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// private val → solo accesible dentro de este archivo
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

// @Composable: esta función participa del ciclo de recomposición de Compose.
// No retorna UI directamente; provee contexto (colores, tipografía) a sus hijos.
@Composable
fun EcoAdminTheme(content: @Composable () -> Unit) {
    // if como expresión: devuelve un valor que se asigna a colorScheme
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors

    // content es un trailing lambda: el bloque { } que se pasa al llamar EcoAdminTheme { ... }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
