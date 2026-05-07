package com.ecoadminmovile

import android.app.Application
import com.ecoadminmovile.core.notifications.NotificationHelper
import com.ecoadminmovile.core.sync.OfflineSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Punto de entrada de la aplicación Android.
 *
 * ## Conceptos Kotlin / Android:
 * - **Herencia**: `EcoAdminApplication` extiende `Application()` usando `:` (en Java sería `extends`).
 *
 * ## Patrón de diseño — Inyección de Dependencias (Hilt / Dagger):
 * - `@HiltAndroidApp` genera en tiempo de compilación el componente raíz de Dagger.
 *   Esto permite que toda la app use `@Inject` para recibir dependencias automáticamente.
 */
@HiltAndroidApp
class EcoAdminApplication : Application() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannels()
        OfflineSyncManager.schedulePeriodic(this)
    }
}
