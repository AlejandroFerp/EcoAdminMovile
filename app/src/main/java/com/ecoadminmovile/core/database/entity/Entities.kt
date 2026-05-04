package com.ecoadminmovile.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "traslados")
data class TrasladoEntity(
    @PrimaryKey val id: Long,
    val codigo: String,
    val centroProductorId: Long?,
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
