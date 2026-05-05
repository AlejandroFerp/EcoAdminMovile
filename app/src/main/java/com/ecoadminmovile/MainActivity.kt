package com.ecoadminmovile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ecoadminmovile.ui.theme.EcoAdminTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity principal y única de la app (patrón Single-Activity).
 *
 * ## Conceptos Kotlin / Android:
 * - `override fun`: sobreescribe un método de la clase padre. `override` es OBLIGATORIO en Kotlin
 *   (a diferencia de Java donde `@Override` es opcional).
 * - `savedInstanceState: Bundle?`: el `?` indica que el parámetro puede ser **null** (nullable type).
 *   Kotlin distingue tipos nulables (`String?`) de no-nulables (`String`) en tiempo de compilación.
 * - `super.onCreate(...)`: llama al constructor padre, igual que en Java.
 *
 * ## Patrón de diseño — Single Activity + Jetpack Compose:
 * - En lugar de tener múltiples Activities, toda la navegación ocurre dentro de `EcoAdminApp()`.
 * - `setContent { }` es el punto de entrada de Compose: reemplaza `setContentView(R.layout.xxx)`.
 * - `EcoAdminTheme { }` envuelve toda la UI con Material Design 3 (Trailing Lambda Pattern).
 *
 * ## Patrón — Inyección de Dependencias:
 * - `@AndroidEntryPoint` habilita inyección Hilt en esta Activity.
 *   Sin esto, los `hiltViewModel()` dentro de los Composables fallarían.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent reemplaza al XML layout tradicional.
        // Todo lo que va dentro es una función @Composable (UI declarativa).
        setContent {
            // EcoAdminTheme aplica colores, tipografía y formas de Material 3
            // a todo el árbol de Composables hijos.
            EcoAdminTheme {
                EcoAdminApp()
            }
        }
    }
}
