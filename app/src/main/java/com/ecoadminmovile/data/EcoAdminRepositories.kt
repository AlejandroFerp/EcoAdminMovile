/**
 * Capa de repositorios que encapsula las llamadas a la API y devuelve Result<T>.
 * Traduce errores HTTP a mensajes legibles para el usuario.
 *
 * Conceptos Kotlin demostrados:
 * - Result<T>: tipo de la stdlib para manejo funcional de errores (Railway-Oriented Programming).
 *   Envuelve un éxito (Result.success) o un fallo (Result.failure) sin lanzar excepciones.
 * - suspend fun: funciones de corrutinas para operaciones asíncronas sin callbacks.
 * - Funciones de extensión privadas: `private fun Response<*>.isUnauthorizedRedirect()`
 *   añade un método a Response sin modificar la clase original. `private` limita su visibilidad.
 * - fold(onSuccess, onFailure): descompone un Result en sus dos caminos posibles.
 * - Higher-order functions: `safeApiCall(request: suspend () -> Response<T>)` recibe una
 *   función como parámetro (lambda suspendible).
 * - when expression: reemplazo de if-else-if encadenados, más legible y exhaustivo.
 * - .orEmpty(): función de extensión que devuelve "" si el String? es null.
 * - Regex con .toRegex(): convierte un String a expresión regular.
 * - try-catch como expresión: en Kotlin, try devuelve un valor.
 * - private class: clase visible solo dentro de este archivo.
 * - `*` como tipo genérico (star projection): `Response<*>` acepta cualquier tipo.
 *
 * Patrones de diseño:
 * - Repository: abstrae la fuente de datos (API) del resto de la aplicación.
 * - Railway-Oriented Programming: Result<T> canaliza éxito/error sin excepciones.
 */
package com.ecoadminmovile.data

import com.ecoadminmovile.core.database.dao.CentroDao
import com.ecoadminmovile.core.database.dao.PendingOperationDao
import com.ecoadminmovile.core.database.dao.TrasladoDao
import com.ecoadminmovile.core.database.entity.PendingOperationEntity
import com.ecoadminmovile.core.model.CentroCreateDto
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.DocumentoDto
import com.ecoadminmovile.core.model.EstadisticasDto
import com.ecoadminmovile.core.model.HistorialEventoDto
import com.ecoadminmovile.core.model.PasswordChangeDto
import com.ecoadminmovile.core.model.PerfilUpdateDto
import com.ecoadminmovile.core.model.ResiduoCreateDto
import com.ecoadminmovile.core.model.ResiduoDto
import com.ecoadminmovile.core.model.RutaCreateDto
import com.ecoadminmovile.core.model.RutaDto
import com.ecoadminmovile.core.model.TrasladoCreateDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.model.UsuarioPerfilDto
import com.ecoadminmovile.core.model.UsuarioResumenDto
import com.ecoadminmovile.core.network.EcoAdminApi
import com.ecoadminmovile.core.preferences.AppPreferences
import com.ecoadminmovile.core.sync.toDto
import com.ecoadminmovile.core.sync.toEntity
import java.io.IOException
import okhttp3.ResponseBody
import retrofit2.Response

// private class: solo visible dentro de este archivo (encapsulación a nivel de fichero)
private class ApiException(message: String) : IllegalStateException(message)

open class AuthRepository(
    private val api: EcoAdminApi,
    private val preferences: AppPreferences
) {
    open fun hasActiveSession(): Boolean = preferences.hasSessionCookie()

    // suspend: función de corrutina, se ejecuta sin bloquear el hilo principal
    open suspend fun login(email: String, password: String): Result<Unit> {
        preferences.clearSession()

        // try-catch como EXPRESIÓN: todo el bloque devuelve un Result<Unit>
        return try {
            val csrfToken = fetchCsrfToken()
                // ?: return → operador Elvis con retorno temprano si es null
                ?: return Result.failure(
                    ApiException("No se pudo obtener el token CSRF del servidor. Verifica que el backend está accesible.")
                )

            val response = api.login(
                email = email.trim(), // .trim() es función de extensión de String
                password = password,
                csrfToken = csrfToken
            )
            val code = response.code()
            // .orEmpty(): si es null devuelve "" (función de extensión de String?)
            val location = response.headers()["Location"].orEmpty()
            val hasSession = preferences.hasSessionCookie()

            // when: expresión que reemplaza cadenas if-else, más legible y exhaustiva
            when {
                location.contains("/login?error") -> Result.failure(
                    ApiException("Credenciales incorrectas. Verifica tu email y contraseña.")
                )

                location.contains("/login") && !hasSession -> Result.failure(
                    ApiException(
                        "El servidor rechazó el login (HTTP $code → $location). " +
                        "Posible problema de CSRF o sesión."
                    )
                )

                // String templates: "$code" y "$location" interpolan variables en strings
                (code in 300..399 || response.isSuccessful) && hasSession -> {
                    Result.success(Unit) // Unit = equivalente a void, pero es un tipo real en Kotlin
                }

                !hasSession -> Result.failure(
                    ApiException(
                        "Login HTTP $code pero no se recibió cookie de sesión. " +
                        "Location: $location"
                    )
                )

                else -> Result.failure(
                    ApiException("Respuesta inesperada del servidor: HTTP $code. Location: $location")
                )
            }
        } catch (ioException: IOException) {
            Result.failure(ApiException("No se pudo conectar al servidor: ${ioException.message}"))
        } catch (exception: Exception) {
            Result.failure(ApiException("Error inesperado: ${exception.message}"))
        }
    }

    private suspend fun fetchCsrfToken(): String? {
        return try {
            val response = api.getLoginPage()
            val html = response.body()?.string().orEmpty()
            // .toRegex(): función de extensión que convierte String a Regex
            val regex = """name="_csrf"\s+value="([^"]+)"""".toRegex()
            // regex.find()?.groupValues?.get(1): encadenamiento seguro con ?.
            regex.find(html)?.groupValues?.get(1)
        } catch (_: Exception) { // _ = variable ignorada (no nos interesa la excepción)
            null
        }
    }

    suspend fun validateSession(): Result<UsuarioPerfilDto> {
        return safeApiCall { api.getProfile() }
    }

    fun logout() {
        preferences.clearSession()
    }
}

open class DashboardRepository(
    private val api: EcoAdminApi
) {
    open suspend fun loadDashboard(desde: String? = null): Result<EstadisticasDto> =
        safeApiCall { api.getEstadisticas(desde) }
}

open class TransfersRepository(
    private val api: EcoAdminApi,
    private val trasladoDao: TrasladoDao? = null,
    private val pendingOperationDao: PendingOperationDao? = null
) {
    open suspend fun loadTransfers(): Result<List<TrasladoDto>> {
        val apiResult = safeApiCall { api.getTraslados() }
        // Cache on success
        apiResult.onSuccess { transfers ->
            trasladoDao?.let { dao ->
                dao.deleteAll()
                dao.upsertAll(transfers.map { it.toEntity() })
            }
        }
        // Fallback to cache on failure
        if (apiResult.isFailure && trasladoDao != null) {
            val cached = trasladoDao.getAll()
            if (cached.isNotEmpty()) {
                return Result.success(cached.map { it.toDto() })
            }
        }
        return apiResult
    }

    suspend fun loadTransfer(id: Long): Result<TrasladoDto> = safeApiCall { api.getTraslado(id) }

    suspend fun createTransfer(data: TrasladoCreateDto): Result<TrasladoDto> =
        safeApiCall { api.createTraslado(data) }

    suspend fun updateTransfer(id: Long, data: TrasladoCreateDto): Result<TrasladoDto> =
        safeApiCall { api.updateTraslado(id, data) }

    open suspend fun deleteTransfer(id: Long): Result<Unit> {
        val result = safeApiCall { api.deleteTraslado(id) }
        if (result.isFailure) {
            pendingOperationDao?.insert(
                PendingOperationEntity(operationType = "DELETE", entityType = "TRASLADO", entityId = id, payload = "")
            )
        }
        return result
    }

    suspend fun updateStatus(id: Long, newStatus: String, comentario: String? = null): Result<TrasladoDto> {
        val result = safeApiCall { api.updateTransferStatus(id, newStatus, comentario) }
        if (result.isFailure) {
            pendingOperationDao?.insert(
                PendingOperationEntity(
                    operationType = "STATUS_CHANGE",
                    entityType = "TRASLADO",
                    entityId = id,
                    payload = "$newStatus|${comentario.orEmpty()}"
                )
            )
        }
        return result
    }

    suspend fun loadHistory(id: Long): Result<List<HistorialEventoDto>> =
        safeApiCall { api.getTransferHistory(id) }

    suspend fun loadPdf(id: Long, tipo: String): Result<ResponseBody> =
        safeApiCall { api.getTransferPdf(id, tipo) }
}

open class CatalogRepository(
    private val api: EcoAdminApi
) {
    open suspend fun loadCentros(): Result<List<CentroDto>> = safeApiCall { api.getCentros() }
    open suspend fun loadResiduos(): Result<List<ResiduoDto>> = safeApiCall { api.getResiduos() }
    open suspend fun loadTransportistas(): Result<List<UsuarioResumenDto>> = safeApiCall { api.getUsuarios("TRANSPORTISTA") }
    open suspend fun loadRutas(transportistaId: Long? = null): Result<List<RutaDto>> = safeApiCall { api.getRutas(transportistaId) }
}

open class CentersRepository(
    private val api: EcoAdminApi,
    private val centroDao: CentroDao? = null
) {
    suspend fun loadCenters(): Result<List<CentroDto>> {
        val apiResult = safeApiCall { api.getCentros() }
        apiResult.onSuccess { centers ->
            centroDao?.let { dao ->
                dao.deleteAll()
                dao.upsertAll(centers.map { it.toEntity() })
            }
        }
        return apiResult
    }

    suspend fun loadCenter(id: Long): Result<CentroDto> = safeApiCall { api.getCentro(id) }

    suspend fun createCenter(data: CentroCreateDto): Result<CentroDto> =
        safeApiCall { api.createCentro(data) }

    suspend fun updateCenter(id: Long, data: CentroCreateDto): Result<CentroDto> =
        safeApiCall { api.updateCentro(id, data) }

    suspend fun deleteCenter(id: Long): Result<Unit> =
        safeApiCall { api.deleteCentro(id) }
}

open class ProfileRepository(
    private val api: EcoAdminApi
) {
    open suspend fun updateProfile(data: PerfilUpdateDto): Result<UsuarioPerfilDto> =
        safeApiCall { api.updateProfile(data) }

    open suspend fun changePassword(data: PasswordChangeDto): Result<Unit> =
        safeApiCall { api.changePassword(data) }
}

open class ResiduosRepository(
    private val api: EcoAdminApi
) {
    open suspend fun loadAll(): Result<List<ResiduoDto>> = safeApiCall { api.getResiduos() }

    open suspend fun load(id: Long): Result<ResiduoDto> = safeApiCall { api.getResiduo(id) }

    open suspend fun create(data: ResiduoCreateDto): Result<ResiduoDto> =
        safeApiCall { api.createResiduo(data) }

    open suspend fun update(id: Long, data: ResiduoCreateDto): Result<ResiduoDto> =
        safeApiCall { api.updateResiduo(id, data) }

    open suspend fun delete(id: Long): Result<Unit> =
        safeApiCall { api.deleteResiduo(id) }
}

open class DocumentosRepository(
    private val api: EcoAdminApi
) {
    open suspend fun loadAll(trasladoId: Long? = null): Result<List<DocumentoDto>> =
        safeApiCall { api.getDocumentos(trasladoId) }
}

open class RutasRepository(
    private val api: EcoAdminApi
) {
    open suspend fun loadAll(): Result<List<RutaDto>> = safeApiCall { api.getRutas() }

    open suspend fun load(id: Long): Result<RutaDto> = safeApiCall { api.getRuta(id) }

    open suspend fun create(data: RutaCreateDto): Result<RutaDto> =
        safeApiCall { api.createRuta(data) }

    open suspend fun update(id: Long, data: RutaCreateDto): Result<RutaDto> =
        safeApiCall { api.updateRuta(id, data) }

    open suspend fun delete(id: Long): Result<Unit> =
        safeApiCall { api.deleteRuta(id) }
}

// Higher-order function: recibe una lambda `suspend () -> Response<T>` como parámetro
// Genérico <T>: funciona con cualquier tipo de respuesta
private suspend fun <T> safeApiCall(request: suspend () -> Response<T>): Result<T> {
    return try {
        val response = request() // Invoca la lambda pasada como parámetro

        when {
            response.isSuccessful -> {
                // ?.let { }: solo ejecuta el bloque si body() no es null
                response.body()?.let { body ->
                    Result.success(body)
                } ?: Result.failure(ApiException("El servidor devolvio una respuesta vacia."))
            }

            response.isUnauthorizedRedirect() -> {
                Result.failure(ApiException("Tu sesion ha caducado. Inicia sesion de nuevo."))
            }

            else -> {
                Result.failure(ApiException(response.toHumanMessage()))
            }
        }
    } catch (ioException: IOException) {
        Result.failure(ApiException("No se pudo conectar con el backend de EcoAdmin."))
    } catch (exception: Exception) {
        Result.failure(ApiException(exception.message ?: "Error inesperado al consultar la API."))
    }
}

// Función de extensión privada: añade isUnauthorizedRedirect() a Response<*>
// Response<*>: star projection, acepta Response de cualquier tipo genérico
private fun Response<*>.isUnauthorizedRedirect(): Boolean {
    return code() in 300..399 && headers()["Location"]?.contains("/login") == true
}

// Otra función de extensión privada sobre Response
private fun Response<*>.toHumanMessage(): String {
    val location = headers()["Location"].orEmpty()
    return when {
        location.contains("/login") -> "La sesion ya no es valida."
        message().isNotBlank() -> message()
        else -> "Respuesta inesperada del servidor (${code()})."
    }
}