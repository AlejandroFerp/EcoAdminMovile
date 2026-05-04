package com.ecoadminmovile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ecoadminmovile.core.database.dao.CentroDao
import com.ecoadminmovile.core.database.dao.PerfilUsuarioDao
import com.ecoadminmovile.core.database.dao.TrasladoDao
import com.ecoadminmovile.core.database.entity.CentroEntity
import com.ecoadminmovile.core.database.entity.PerfilUsuarioEntity
import com.ecoadminmovile.core.database.entity.TrasladoEntity

@Database(
    entities = [
        TrasladoEntity::class,
        CentroEntity::class,
        PerfilUsuarioEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EcoAdminDatabase : RoomDatabase() {
    abstract fun trasladoDao(): TrasladoDao
    abstract fun centroDao(): CentroDao
    abstract fun perfilUsuarioDao(): PerfilUsuarioDao
}
