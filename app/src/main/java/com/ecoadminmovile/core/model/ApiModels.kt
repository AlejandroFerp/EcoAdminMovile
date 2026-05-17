/**
 * Data Transfer Objects (DTOs) que representan las respuestas JSON de la API REST.
 * Gson deserializa el JSON directamente en estas clases.
 *
 * Conceptos Kotlin demostrados:
 * - data class: genera automáticamente equals(), hashCode(), toString(), copy() y
 *   componentN() (destructuring). Ideal para objetos que solo transportan datos.
 * - Valores por defecto en parámetros: `val id: Long = 0` permite crear instancias
 *   sin especificar todos los campos (útil para deserialización parcial).
 * - Tipos nullables con `?`: `val telefono: String? = null` indica que el campo puede
 *   no venir en el JSON. Kotlin obliga a manejar el null explícitamente.
 * - Tipos genéricos: `Map<String, Int>` especifica los tipos de clave y valor.
 * - emptyMap(): función de la stdlib que crea un mapa inmutable vacío.
 *
 * Patrón de diseño: DTO (Data Transfer Object) — objetos sin lógica que transportan
 * datos entre capas (red → dominio → UI).
 */
package com.ecoadminmovile.core.model

// data class: el compilador genera equals, hashCode, toString, copy y destructuring
data class UsuarioPerfilDto(
    val id: Long = 0, // Valor por defecto: si el JSON no trae "id", se usa 0
    val nombre: String = "",
    val email: String = "",
    val rol: String = "",
    val fechaAlta: String? = null, // String? = tipo nullable, puede ser null
    val telefono: String? = null,
    val dni: String? = null,
    val cargo: String? = null,
    val fotoUrl: String? = null,
    val notificacionesEmail: Boolean? = null // Boolean nullable: true, false o ausente
)

data class EstadisticasDto(
    val totalCentros: Int = 0,
    val totalResiduos: Int = 0,
    val trasladosPendientes: Int = 0,
    val trasladosEnTransito: Int = 0,
    val trasladosEntregados: Int = 0,
    val trasladosCompletados: Int = 0,
    val residuosPorCentro: Map<String, Int> = emptyMap() // Tipo genérico con valor por defecto vacío
)

data class TrasladoDto(
    val id: Long = 0,
    val codigo: String? = null,
    val centroProductor: CentroResumenDto? = null,
    val centroGestor: CentroResumenDto? = null,
    val residuo: ResiduoResumenDto? = null,
    val transportista: UsuarioResumenDto? = null,
    val ruta: RutaResumenDto? = null,
    val estado: String? = null,
    val fechaCreacion: String? = null,
    val fechaInicioTransporte: String? = null,
    val fechaEntrega: String? = null,
    val fechaProgramadaInicio: String? = null,
    val fechaProgramadaFin: String? = null,
    val fechaUltimoCambioEstado: String? = null,
    val observaciones: String? = null
)

data class CentroDto(
    val id: Long = 0,
    val codigo: String? = null,
    val nombre: String? = null,
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
    val pais: String? = null,
    val latitud: Double? = null,
    val longitud: Double? = null
)

data class RutaResumenDto(
    val id: Long = 0,
    val nombre: String? = null,
    val distanciaKm: Double? = null,
    val origenLatitud: Double? = null,
    val origenLongitud: Double? = null,
    val destinoLatitud: Double? = null,
    val destinoLongitud: Double? = null
)

data class RutaDto(
    val id: Long = 0,
    val nombre: String = "",
    val distanciaKm: Double? = null,
    val transportista: UsuarioResumenDto? = null
)

data class ResiduoDto(
    val id: Long = 0,
    val codigoLER: String? = null,
    val descripcion: String? = null,
    val cantidad: Double? = null,
    val unidad: String? = null,
    val centro: CentroResumenDto? = null
)

data class HistorialEventoDto(
    val id: Long = 0,
    val estadoAnterior: String? = null,
    val estadoNuevo: String = "",
    val comentario: String? = null,
    val fecha: String? = null,
    val usuario: UsuarioResumenDto? = null
)

data class TrasladoCreateDto(
    val centroProductorId: Long,
    val centroGestorId: Long,
    val residuoId: Long,
    val transportistaId: Long? = null,
    val rutaId: Long? = null,
    val observaciones: String? = null,
    val fechaProgramadaInicio: String? = null,
    val fechaProgramadaFin: String? = null
)

data class CentroCreateDto(
    val nombre: String,
    val tipo: String,
    val nima: String? = null,
    val telefono: String? = null,
    val email: String? = null,
    val nombreContacto: String? = null,
    val direccionId: Long? = null
)

data class DireccionCreateDto(
    val calle: String? = null,
    val ciudad: String? = null,
    val provincia: String? = null,
    val codigoPostal: String? = null,
    val pais: String? = null
)

data class PerfilUpdateDto(
    val nombre: String,
    val telefono: String? = null,
    val dni: String? = null,
    val cargo: String? = null
)

data class PasswordChangeDto(
    val currentPassword: String,
    val newPassword: String
)

data class ResiduoCreateDto(
    val centroId: Long,
    val codigoLER: String,
    val descripcion: String? = null,
    val cantidad: Double? = null,
    val unidad: String? = null,
    val estado: String? = null,
    val fechaEntradaAlmacen: String? = null,
    val diasMaximoAlmacenamiento: Int? = null
)

data class DocumentoDto(
    val id: Long = 0,
    val tipo: String = "",
    val nombre: String = "",
    val url: String? = null,
    val trasladoId: Long? = null,
    val fechaCreacion: String? = null
)

data class RutaCreateDto(
    val nombre: String,
    val distanciaKm: Double? = null,
    val transportistaId: Long? = null
)
