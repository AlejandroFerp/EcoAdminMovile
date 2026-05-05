/**
 * Entidades de Room que representan las tablas de la base de datos local SQLite.
 * Cada data class se mapea directamente a una tabla.
 *
 * Conceptos Kotlin demostrados:
 * - data class con anotaciones Room: combina la potencia de data class (equals, hashCode,
 *   copy) con el mapeo ORM de Room.
 * - @Entity(tableName = "..."): anotación que indica a Room el nombre de la tabla.
 * - @PrimaryKey: marca el campo como clave primaria de la tabla.
 * - Valores por defecto evaluados en runtime: `System.currentTimeMillis()` se ejecuta
 *   cada vez que se crea una instancia (no es una constante de compilación).
 * - Tipos nullables: los campos opcionales se declaran con `?` para reflejar que la
 *   columna puede contener NULL en SQLite.
 *
 * Patrón de diseño: Active Record simplificado — cada entidad se corresponde 1:1 con una tabla.
 */
package com.ecoadminmovile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity: Room crea la tabla "traslados" basándose en las propiedades de esta data class
@Entity(tableName = "traslados")
data class TrasladoEntity(
    @PrimaryKey val id: Long, // @PrimaryKey: clave primaria de la tabla
    val codigo: String,
    val centroProductorId: Long?, // Long? = la columna permite NULL
    val centroProductorNombre: String?,
    val centroGestorId: Long?,
    val centroGestorNombre: String?,
    val residuoId: Long?,
    val residuoNombre: String?,
    val transportistaNombre: String?,
    val estado: String,
    val fechaCreacion: String?,
    val fechaInicioTransporte: String?,
    val fechaEntrega: String?,
    val observaciones: String?,
    // System.currentTimeMillis(): valor por defecto evaluado en RUNTIME (no compilación)
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "centros")
data class CentroEntity(
    @PrimaryKey val id: Long,
    val codigo: String,
    val nombre: String,
    val tipo: String?,
    val nima: String?,
    val telefono: String?,
    val email: String?,
    val nombreContacto: String?,
    val direccionCalle: String?,
    val direccionCiudad: String?,
    val direccionProvincia: String?,
    val direccionCodigoPostal: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "perfil_usuario")
data class PerfilUsuarioEntity(
    @PrimaryKey val id: Long,
    val nombre: String,
    val email: String,
    val rol: String,
    val fechaAlta: String?,
    val telefono: String?,
    val dni: String?,
    val cargo: String?,
    val fotoUrl: String?,
    val lastSyncedAt: Long = System.currentTimeMillis()
)
