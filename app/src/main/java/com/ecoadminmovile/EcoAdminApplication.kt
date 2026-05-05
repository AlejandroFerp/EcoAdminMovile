package com.ecoadminmovile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada de la aplicación Android.
 *
 * ## Conceptos Kotlin / Android:
 * - **Herencia**: `EcoAdminApplication` extiende `Application()` usando `:` (en Java sería `extends`).
 * - **Clase vacía**: en Kotlin una clase sin cuerpo no necesita llaves `{}`.
 *
 * ## Patrón de diseño — Inyección de Dependencias (Hilt / Dagger):
 * - `@HiltAndroidApp` genera en tiempo de compilación el componente raíz de Dagger.
 *   Esto permite que toda la app use `@Inject` para recibir dependencias automáticamente.
 * - Sin esta anotación, Hilt no puede inicializarse y las inyecciones fallarían.
 *
 * ## ¿Por qué existe esta clase?
 * Android necesita una clase Application personalizada cuando se usan frameworks
 * como Hilt que requieren código de inicialización al arrancar el proceso.
 */
@HiltAndroidApp
class EcoAdminApplication : Application()
