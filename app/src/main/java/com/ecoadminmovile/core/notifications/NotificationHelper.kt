package com.ecoadminmovile.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local notifications for the app.
 * Push notifications via FCM require google-services.json and Firebase setup.
 * This class handles local notifications (sync complete, pending ops, etc.)
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_SYNC = "ecoadmin_sync"
        const val CHANNEL_TRANSFERS = "ecoadmin_transfers"
        private const val NOTIFICATION_ID_SYNC = 1001
        private const val NOTIFICATION_ID_TRANSFER = 1002
    }

    fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val syncChannel = NotificationChannel(
                CHANNEL_SYNC,
                "Sincronización",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones de sincronización offline"
            }

            val transferChannel = NotificationChannel(
                CHANNEL_TRANSFERS,
                "Traslados",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Actualizaciones de estado de traslados"
            }

            manager.createNotificationChannel(syncChannel)
            manager.createNotificationChannel(transferChannel)
        }
    }

    fun notifySyncComplete(pendingCount: Int) {
        if (pendingCount > 0) return // Only notify when all synced
        val notification = NotificationCompat.Builder(context, CHANNEL_SYNC)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Sincronización completa")
            .setContentText("Todos los datos están actualizados")
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_SYNC, notification)
    }

    fun notifyTransferStatusChange(transferCode: String, newStatus: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSFERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Traslado actualizado")
            .setContentText("$transferCode → $newStatus")
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_TRANSFER, notification)
    }
}
