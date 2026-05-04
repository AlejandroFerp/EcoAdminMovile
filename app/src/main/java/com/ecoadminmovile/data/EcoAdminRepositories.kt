package com.ecoadminmovile.data

import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.EstadisticasDto
import com.ecoadminmovile.core.model.HistorialEventoDto
import com.ecoadminmovile.core.model.ResiduoDto
import com.ecoadminmovile.core.model.RutaDto
import com.ecoadminmovile.core.model.TrasladoCreateDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.model.UsuarioPerfilDto
import com.ecoadminmovile.core.model.UsuarioResumenDto
import com.ecoadminmovile.core.network.EcoAdminApi
import com.ecoadminmovile.core.preferences.AppPreferences
import java.io.IOException
import okhttp3.ResponseBody
import retrofit2.Response

private class ApiException(message: String) : IllegalStateException(message)

class AuthRepository(
    private val api: EcoAdminApi,
    private val preferences: AppPreferences
) {
    fun hasActiveSession(): Boolean = preferences.hasSessionCookie()

    suspend fun login(email: String, password: String): Result<Unit> {
        preferences.clearSession()

        return try {
            val csrfToken = fetchCsrfToken()
                ?: return Result.failure(
                    ApiException("No se pudo obtener el token CSRF del servidor. Verifica que el backend está accesible.")
                )

            val response = api.login(
                email = email.trim(),
                password = password,
                csrfToken = csrfToken
            )
            val code = response.code()
            val location = response.headers()["Location"].orEmpty()
            val hasSession = preferences.hasSessionCookie()

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

                (code in 300..399 || response.isSuccessful) && hasSession -> {
                    Result.success(Unit)
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
            // Parse: <input type="hidden" name="_csrf" value="TOKEN_VALUE"/>
            val regex = """name="_csrf"\s+value="([^"]+)"""".toRegex()
            regex.find(html)?.groupValues?.get(1)
        } catch (_: Exception) {
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

class DashboardRepository(
    private val api: EcoAdminApi
) {
    suspend fun loadDashboard(desde: String? = null): Result<EstadisticasDto> =
        safeApiCall { api.getEstadisticas(desde) }
}

class TransfersRepository(
    private val api: EcoAdminApi
) {
    suspend fun loadTransfers(): Result<List<TrasladoDto>> = safeApiCall { api.getTraslados() }

    suspend fun loadTransfer(id: Long): Result<TrasladoDto> = safeApiCall { api.getTraslado(id) }

    suspend fun createTransfer(data: TrasladoCreateDto): Result<TrasladoDto> =
        safeApiCall { api.createTraslado(data) }

    suspend fun updateTransfer(id: Long, data: TrasladoCreateDto): Result<TrasladoDto> =
        safeApiCall { api.updateTraslado(id, data) }

    suspend fun deleteTransfer(id: Long): Result<Unit> =
        safeApiCall { api.deleteTraslado(id) }

    suspend fun updateStatus(id: Long, newStatus: String, comentario: String? = null): Result<TrasladoDto> =
        safeApiCall { api.updateTransferStatus(id, newStatus, comentario) }

    suspend fun loadHistory(id: Long): Result<List<HistorialEventoDto>> =
        safeApiCall { api.getTransferHistory(id) }

    suspend fun loadPdf(id: Long, tipo: String): Result<ResponseBody> =
        safeApiCall { api.getTransferPdf(id, tipo) }
}

class CatalogRepository(
    private val api: EcoAdminApi
) {
    suspend fun loadCentros(): Result<List<CentroDto>> = safeApiCall { api.getCentros() }
    suspend fun loadResiduos(): Result<List<ResiduoDto>> = safeApiCall { api.getResiduos() }
    suspend fun loadTransportistas(): Result<List<UsuarioResumenDto>> = safeApiCall { api.getUsuarios("TRANSPORTISTA") }
    suspend fun loadRutas(transportistaId: Long? = null): Result<List<RutaDto>> = safeApiCall { api.getRutas(transportistaId) }
}

class CentersRepository(
    private val api: EcoAdminApi
) {
    suspend fun loadCenters(): Result<List<CentroDto>> = safeApiCall { api.getCentros() }

    suspend fun loadCenter(id: Long): Result<CentroDto> = safeApiCall { api.getCentro(id) }
}

private suspend fun <T> safeApiCall(request: suspend () -> Response<T>): Result<T> {
    return try {
        val response = request()

        when {
            response.isSuccessful -> {
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

private fun Response<*>.isUnauthorizedRedirect(): Boolean {
    return code() in 300..399 && headers()["Location"]?.contains("/login") == true
}

private fun Response<*>.toHumanMessage(): String {
    val location = headers()["Location"].orEmpty()
    return when {
        location.contains("/login") -> "La sesion ya no es valida."
        message().isNotBlank() -> message()
        else -> "Respuesta inesperada del servidor (${code()})."
    }
}