package com.ecoadminmovile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ecoadminmovile.core.database.entity.CentroEntity
import com.ecoadminmovile.core.database.entity.PerfilUsuarioEntity
import com.ecoadminmovile.core.database.entity.TrasladoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TrasladoDao {
    @Query("SELECT * FROM traslados ORDER BY id DESC")
    fun observeAll(): Flow<List<TrasladoEntity>>

    @Query("SELECT * FROM traslados WHERE id = :id")
    suspend fun getById(id: Long): TrasladoEntity?

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
