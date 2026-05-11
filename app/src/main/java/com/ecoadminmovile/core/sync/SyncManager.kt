package com.ecoadminmovile.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ecoadminmovile.core.database.dao.CentroDao
import com.ecoadminmovile.core.database.dao.PendingOperationDao
import com.ecoadminmovile.core.database.dao.TrasladoDao
import com.ecoadminmovile.core.database.entity.CentroEntity
import com.ecoadminmovile.core.database.entity.TrasladoEntity
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.network.EcoAdminApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages offline caching and sync scheduling.
 * Strategy: API-first, cache results to Room, fallback to Room on failure.
 */
@Singleton
class OfflineSyncManager @Inject constructor(
    private val trasladoDao: TrasladoDao,
    private val centroDao: CentroDao,
    private val pendingOperationDao: PendingOperationDao
) {
    suspend fun cacheTransfers(transfers: List<TrasladoDto>) {
        val entities = transfers.map { it.toEntity() }
        trasladoDao.upsertAll(entities)
    }

    suspend fun cacheCenters(centers: List<CentroDto>) {
        val entities = centers.map { it.toEntity() }
        centroDao.upsertAll(entities)
    }

    suspend fun getCachedTransfers(): List<TrasladoDto> {
        val entities = trasladoDao.observeAll() // Flow, but we need snapshot
        // Use the DAO's direct method for one-shot read
        return emptyList() // We'll use Flow in the repository
    }

    suspend fun enqueueOperation(
        operationType: String,
        entityType: String,
        entityId: Long?,
        payload: String
    ) {
        pendingOperationDao.insert(
            com.ecoadminmovile.core.database.entity.PendingOperationEntity(
                operationType = operationType,
                entityType = entityType,
                entityId = entityId,
                payload = payload
            )
        )
    }

    suspend fun getPendingCount(): Int {
        return pendingOperationDao.getAll().size
    }

    fun observePendingCount() = pendingOperationDao.observeCount()

    companion object {
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            ).setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "ecoadmin_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun triggerImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }
}

/**
 * WorkManager Worker that processes pending operations and refreshes local cache.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: EcoAdminApi,
    private val trasladoDao: TrasladoDao,
    private val centroDao: CentroDao,
    private val pendingOperationDao: PendingOperationDao
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // 1. Process pending operations (retry queue)
            processPendingOperations()

            // 2. Refresh local cache from API
            refreshCache()

            Result.success()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun processPendingOperations() {
        val pending = pendingOperationDao.getPendingRetryable()
        for (operation in pending) {
            try {
                val success = executeOperation(operation)
                if (success) {
                    pendingOperationDao.deleteById(operation.id)
                }
            } catch (e: Exception) {
                pendingOperationDao.markRetry(operation.id, error = e.message)
            }
        }
        // Clean up operations that exceeded max retries
        pendingOperationDao.deleteFailed()
    }

    private suspend fun executeOperation(
        operation: com.ecoadminmovile.core.database.entity.PendingOperationEntity
    ): Boolean {
        val response = when (operation.operationType) {
            "STATUS_CHANGE" -> {
                val parts = operation.payload.split("|", limit = 2)
                val estado = parts[0]
                val comentario = parts.getOrNull(1)
                val id = operation.entityId ?: return false
                api.updateTransferStatus(id, estado, comentario)
            }
            "DELETE" -> {
                val id = operation.entityId ?: return false
                when (operation.entityType) {
                    "TRASLADO" -> api.deleteTraslado(id)
                    "CENTRO" -> api.deleteCentro(id)
                    "RESIDUO" -> api.deleteResiduo(id)
                    "RUTA" -> api.deleteRuta(id)
                    else -> return false
                }
            }
            else -> return false
        }
        return response.isSuccessful
    }

    private suspend fun refreshCache() {
        // Refresh transfers
        val transfersResponse = api.getTraslados()
        if (transfersResponse.isSuccessful) {
            transfersResponse.body()?.let { transfers ->
                trasladoDao.deleteAll()
                trasladoDao.upsertAll(transfers.map { it.toEntity() })
            }
        }

        // Refresh centers
        val centrosResponse = api.getCentros()
        if (centrosResponse.isSuccessful) {
            centrosResponse.body()?.let { centros ->
                centroDao.deleteAll()
                centroDao.upsertAll(centros.map { it.toEntity() })
            }
        }
    }
}

// Extension functions to map DTOs ↔ Entities
fun TrasladoDto.toEntity() = TrasladoEntity(
    id = id,
    codigo = codigo ?: "",
    centroProductorId = centroProductor?.id,
    centroProductorNombre = centroProductor?.nombre,
    centroGestorId = centroGestor?.id,
    centroGestorNombre = centroGestor?.nombre,
    residuoId = residuo?.id,
    residuoNombre = residuo?.descripcion ?: residuo?.codigoLER,
    transportistaNombre = transportista?.nombre,
    estado = estado ?: "",
    fechaCreacion = fechaCreacion,
    fechaInicioTransporte = fechaInicioTransporte,
    fechaEntrega = fechaEntrega,
    observaciones = observaciones
)

fun CentroDto.toEntity() = CentroEntity(
    id = id,
    codigo = codigo ?: "",
    nombre = nombre ?: "",
    tipo = tipo,
    nima = nima,
    telefono = telefono,
    email = email,
    nombreContacto = nombreContacto,
    direccionCalle = direccion?.calle,
    direccionCiudad = direccion?.ciudad,
    direccionProvincia = direccion?.provincia,
    direccionCodigoPostal = direccion?.codigoPostal
)

fun TrasladoEntity.toDto() = TrasladoDto(
    id = id,
    codigo = codigo,
    estado = estado,
    fechaCreacion = fechaCreacion,
    fechaInicioTransporte = fechaInicioTransporte,
    fechaEntrega = fechaEntrega,
    observaciones = observaciones
)
