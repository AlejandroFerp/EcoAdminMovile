/**
 * Configuración tipográfica de Material 3 para EcoAdmin.
 *
 * Conceptos Kotlin demostrados:
 * - `val` top-level: constante a nivel de archivo, accesible sin instanciar ninguna clase.
 * - `Typography()`: invocación del constructor por defecto (sin argumentos).
 *   Material 3 provee estilos tipográficos predefinidos (headlineLarge, bodyMedium, etc.)
 *   que se pueden sobreescribir pasando parámetros nombrados al constructor.
 *
 * Patrón:
 * - Centralizar la tipografía en un solo lugar para inyectarla en `MaterialTheme`.
 */
package com.ecoadminmovile.ui.theme

import androidx.compose.material3.Typography

// Constructor sin argumentos → usa los estilos por defecto de Material 3
val Typography = Typography()
