package com.ecoadminmovile.data

import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.EstadisticasDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.model.UsuarioPerfilDto
import com.ecoadminmovile.core.network.EcoAdminApi
import com.ecoadminmovile.core.preferences.AppPreferences
import java.io.IOException
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
            val response = api.login(email = email.trim(), password = password)
            val location = response.headers()["Location"].orEmpty()
            val hasSession = preferences.hasSessionCookie()

            when {
                location.contains("/login?error") -> Result.failure(
                    ApiException("Email o contrasena incorrectos.")
                )

                (response.code() in 300..399 || response.isSuccessful) && hasSession -> {
                    validateSession().map { Unit }
                }

                else -> Result.failure(
                    ApiException("No se pudo iniciar sesion (${response.code()}).")
                )
            }
        } catch (ioException: IOException) {
            Result.failure(ApiException("No se pudo conectar con el servidor configurado."))
        } catch (exception: Exception) {
            Result.failure(ApiException(exception.message ?: "Error inesperado al iniciar sesion."))
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
    suspend fun loadDashboard(): Result<EstadisticasDto> = safeApiCall { api.getEstadisticas() }
}

class TransfersRepository(
    private val api: EcoAdminApi
) {
    suspend fun loadTransfers(): Result<List<TrasladoDto>> = safeApiCall { api.getTraslados() }

    suspend fun loadTransfer(id: Long): Result<TrasladoDto> = safeApiCall { api.getTraslado(id) }
}

class CentersRepository(
    private val api: EcoAdminApi
) {
    suspend fun loadCenters(): Result<List<CentroDto>> = safeApiCall { api.getCentros() }
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