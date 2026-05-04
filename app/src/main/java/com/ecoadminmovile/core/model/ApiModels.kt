package com.ecoadminmovile.core.model

data class UsuarioPerfilDto(
    val id: Long = 0,
    val nombre: String = "",
    val email: String = "",
    val rol: String = "",
    val fechaAlta: String? = null,
    val telefono: String? = null,
    val dni: String? = null,
    val cargo: String? = null,
    val fotoUrl: String? = null,
    val notificacionesEmail: Boolean? = null
)

data class EstadisticasDto(
    val totalCentros: Int = 0,
    val totalResiduos: Int = 0,
    val trasladosPendientes: Int = 0,
    val trasladosEnTransito: Int = 0,
    val trasladosEntregados: Int = 0,
    val trasladosCompletados: Int = 0,
    val residuosPorCentro: Map<String, Int> = emptyMap()
)

data class TrasladoDto(
    val id: Long = 0,
    val codigo: String = "",
    val centroProductor: CentroResumenDto? = null,
    val centroGestor: CentroResumenDto? = null,
    val residuo: ResiduoResumenDto? = null,
    val transportista: UsuarioResumenDto? = null,
    val estado: String = "",
    val fechaCreacion: String? = null,
    val fechaInicioTransporte: String? = null,
    val fechaEntrega: String? = null,
    val observaciones: String? = null
)

data class CentroDto(
    val id: Long = 0,
    val codigo: String = "",
    val nombre: String = "",
    val tipo: String? = null,
    val usuario: UsuarioResumenDto? = null,
    val direccion: DireccionDto? = null,
    val nima: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val nombreContacto: String? = null
)

data class CentroResumenDto(
    val id: Long = 0,
    val codigo: String = "",
    val nombre: String = ""
)

data class ResiduoResumenDto(
    val id: Long = 0,
    val codigo: String = "",
    val cantidad: Double? = null,
    val unidad: String? = null,
    val codigoLER: String? = null,
    val descripcion: String? = null
)

data class UsuarioResumenDto(
    val id: Long = 0,
    val nombre: String = "",
    val email: String? = null,
    val rol: String? = null
)

data class DireccionDto(
    val id: Long = 0,
    val nombre: String? = null,
    val calle: String? = null,
    val ciudad: String? = null,
    val provincia: String? = null,
    val codigoPostal: String? = null,
    val pais: String? = null
)
