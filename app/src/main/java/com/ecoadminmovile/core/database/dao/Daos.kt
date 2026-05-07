/**
 * DAOs (Data Access Objects) de Room para acceder a la base de datos local.
 * Room genera la implementación SQL automáticamente a partir de las anotaciones.
 *
 * Conceptos Kotlin demostrados:
 * - interface con @Dao: Room genera la implementación concreta en tiempo de compilación.
 * - suspend fun: funciones suspendibles que se ejecutan en corrutinas (no bloquean el hilo principal).
 * - Flow<List<T>>: flujo reactivo de Kotlin. Emite automáticamente cuando los datos cambian
 *   en la BD (patrón Observer). No necesita `suspend` porque Flow es frío (lazy).
 * - Tipo genérico List<T>: colección inmutable parametrizada.
 * - Retorno nullable: `suspend fun getById(id: Long): TrasladoEntity?` puede devolver null.
 *
 * Patrones de diseño:
 * - DAO (Data Access Object): separa la lógica de acceso a datos de la lógica de negocio.
 * - Observer/Reactive Streams: Flow emite nuevos valores cada vez que la tabla cambia.
 * - Upsert (Update or Insert): `OnConflictStrategy.REPLACE` inserta o actualiza si ya existe.
 */
package com.ecoadminmovile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ecoadminmovile.core.database.entity.CentroEntity
import com.ecoadminmovile.core.database.entity.PendingOperationEntity
import com.ecoadminmovile.core.database.entity.PerfilUsuarioEntity
import com.ecoadminmovile.core.database.entity.TrasladoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrasladoDao {
    // Flow<List<...>>: flujo reactivo, emite automáticamente cuando la tabla cambia (Observer pattern)
    // No es suspend porque Flow es "frío" — solo se activa cuando alguien lo colecta
    @Query("SELECT * FROM traslados ORDER BY id DESC")
    fun observeAll(): Flow<List<TrasladoEntity>>

    @Query("SELECT * FROM traslados ORDER BY id DESC")
    suspend fun getAll(): List<TrasladoEntity>

    // suspend + retorno nullable: puede devolver null si no existe el registro
    @Query("SELECT * FROM traslados WHERE id = :id")
    suspend fun getById(id: Long): TrasladoEntity?

    // OnConflictStrategy.REPLACE: si el PrimaryKey ya existe, actualiza en vez de fallar (upsert)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(traslados: List<TrasladoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(traslado: TrasladoEntity)

    @Query("DELETE FROM traslados")
    suspend fun deleteAll()
}

@Dao
interface CentroDao {
    @Query("SELECT * FROM centros ORDER BY nombre ASC")
    fun observeAll(): Flow<List<CentroEntity>>

    @Query("SELECT * FROM centros WHERE id = :id")
    suspend fun getById(id: Long): CentroEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(centros: List<CentroEntity>)

    @Query("DELETE FROM centros")
    suspend fun deleteAll()
}

@Dao
interface PerfilUsuarioDao {
    @Query("SELECT * FROM perfil_usuario LIMIT 1")
    suspend fun get(): PerfilUsuarioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(perfil: PerfilUsuarioEntity)

    @Query("DELETE FROM perfil_usuario")
    suspend fun delete()
}

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE retryCount < maxRetries ORDER BY createdAt ASC")
    suspend fun getPendingRetryable(): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_operations SET retryCount = retryCount + 1, lastAttemptAt = :now, errorMessage = :error WHERE id = :id")
    suspend fun markRetry(id: Long, now: Long = System.currentTimeMillis(), error: String?)

    @Query("DELETE FROM pending_operations WHERE retryCount >= maxRetries")
    suspend fun deleteFailed()
}
