/**
 * Definición de la base de datos Room. Room genera la implementación concreta de esta clase abstracta.
 *
 * Conceptos Kotlin demostrados:
 * - abstract class: clase que no puede instanciarse directamente. Room genera la subclase concreta.
 * - Anotación @Database: configura las entidades (tablas) y la versión del esquema.
 * - Array literal en anotaciones: `entities = [...]` (solo permitido dentro de anotaciones).
 * - KClass references: `TrasladoEntity::class` referencia la KClass (metadato del tipo en Kotlin).
 * - abstract fun: funciones sin implementación que Room implementa automáticamente.
 *
 * Patrón de diseño: Abstract Factory — Room genera los DAOs concretos a partir de las
 * declaraciones abstractas. La BD actúa como factoría de DAOs.
 */
package com.ecoadminmovile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ecoadminmovile.core.database.dao.CentroDao
import com.ecoadminmovile.core.database.dao.PendingOperationDao
import com.ecoadminmovile.core.database.dao.PerfilUsuarioDao
import com.ecoadminmovile.core.database.dao.TrasladoDao
import com.ecoadminmovile.core.database.entity.CentroEntity
import com.ecoadminmovile.core.database.entity.PendingOperationEntity
import com.ecoadminmovile.core.database.entity.PerfilUsuarioEntity
import com.ecoadminmovile.core.database.entity.TrasladoEntity

// @Database: lista de entidades (tablas) que Room debe crear/gestionar
// ::class es una referencia KClass (reflexión en Kotlin)
@Database(
    entities = [
        TrasladoEntity::class, // ::class = referencia KClass de la entidad
        CentroEntity::class,
        PerfilUsuarioEntity::class,
        PendingOperationEntity::class
    ],
    version = 2,
    exportSchema = false
)
// abstract class: Room genera la implementación concreta (EcoAdminDatabase_Impl)
abstract class EcoAdminDatabase : RoomDatabase() {
    // Funciones abstractas: Room las implementa para devolver el DAO correspondiente
    abstract fun trasladoDao(): TrasladoDao
    abstract fun centroDao(): CentroDao
    abstract fun perfilUsuarioDao(): PerfilUsuarioDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
