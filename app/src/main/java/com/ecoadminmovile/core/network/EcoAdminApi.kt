/**
 * Interfaz de API REST usando Retrofit para comunicarse con el backend EcoAdmin.
 *
 * Conceptos Kotlin demostrados:
 * - interface: en Kotlin no necesita la palabra "abstract" para sus métodos.
 *   Retrofit genera la implementación en tiempo de ejecución.
 * - suspend fun: funciones suspendibles que solo pueden llamarse desde una corrutina.
 *   Permiten hacer llamadas de red sin bloquear el hilo principal.
 * - Valores por defecto en parámetros: `desde: String? = null` evita crear sobrecargas.
 * - Anotaciones (@GET, @POST, @Path, @Query, @Field, @Body): metadatos que Retrofit
 *   usa para construir las peticiones HTTP automáticamente.
 * - Response<T>: wrapper de Retrofit que da acceso al código HTTP, headers y body.
 *
 * Patrón de diseño: Repository Pattern (esta interfaz es la capa de acceso a datos remota).
 */
package com.ecoadminmovile.core.network

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
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// En Kotlin, una interfaz define un contrato que otras clases implementan
interface EcoAdminApi {
    // suspend = esta función es asíncrona y se ejecuta dentro de una corrutina
    @GET("login")
    suspend fun getLoginPage(): Response<ResponseBody>

    // @FormUrlEncoded indica que el body se envía como formulario (application/x-www-form-urlencoded)
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") email: String, // @Field mapea el parámetro a un campo del formulario
        @Field("password") password: String,
        @Field("_csrf") csrfToken: String
    ): Response<ResponseBody>

    @GET("api/perfil")
    suspend fun getProfile(): Response<UsuarioPerfilDto>

    @GET("api/estadisticas")
    suspend fun getEstadisticas(
        @Query("desde") desde: String? = null // Valor por defecto: evita sobrecarga de métodos
    ): Response<EstadisticasDto>

    // --- Traslados ---
    @GET("api/traslados")
    suspend fun getTraslados(): Response<List<TrasladoDto>> // List<T>: tipo genérico parametrizado

    @GET("api/traslados/{id}")
    suspend fun getTraslado(@Path("id") id: Long): Response<TrasladoDto> // @Path sustituye {id} en la URL

    @POST("api/traslados")
    suspend fun createTraslado(@Body body: TrasladoCreateDto): Response<TrasladoDto> // @Body serializa el objeto a JSON

    @PUT("api/traslados/{id}")
    suspend fun updateTraslado(@Path("id") id: Long, @Body body: TrasladoCreateDto): Response<TrasladoDto>

    @DELETE("api/traslados/{id}")
    suspend fun deleteTraslado(@Path("id") id: Long): Response<Unit> // Unit = void en Kotlin

    @FormUrlEncoded
    @PUT("api/traslados/{id}/estado")
    suspend fun updateTransferStatus(
        @Path("id") id: Long,
        @Field("estado") estado: String,
        @Field("comentario") comentario: String? = null // Parámetro opcional nullable con default null
    ): Response<TrasladoDto>

    @GET("api/traslados/{id}/historial")
    suspend fun getTransferHistory(@Path("id") id: Long): Response<List<HistorialEventoDto>>

    @GET("api/traslados/{id}/pdf/{tipo}")
    suspend fun getTransferPdf(
        @Path("id") id: Long,
        @Path("tipo") tipo: String
    ): Response<ResponseBody>

    // --- Centros ---
    @GET("api/centros")
    suspend fun getCentros(): Response<List<CentroDto>>

    @GET("api/centros/{id}")
    suspend fun getCentro(@Path("id") id: Long): Response<CentroDto>

    @POST("api/centros")
    suspend fun createCentro(@Body body: CentroCreateDto): Response<CentroDto>

    @PUT("api/centros/{id}")
    suspend fun updateCentro(@Path("id") id: Long, @Body body: CentroCreateDto): Response<CentroDto>

    @DELETE("api/centros/{id}")
    suspend fun deleteCentro(@Path("id") id: Long): Response<Unit>

    // --- Perfil ---
    @PUT("api/perfil")
    suspend fun updateProfile(@Body body: PerfilUpdateDto): Response<UsuarioPerfilDto>

    @PUT("api/perfil/password")
    suspend fun changePassword(@Body body: PasswordChangeDto): Response<Unit>

    // --- Residuos ---
    @GET("api/residuos")
    suspend fun getResiduos(): Response<List<ResiduoDto>>

    @GET("api/residuos/{id}")
    suspend fun getResiduo(@Path("id") id: Long): Response<ResiduoDto>

    @POST("api/residuos")
    suspend fun createResiduo(@Body body: ResiduoCreateDto): Response<ResiduoDto>

    @PUT("api/residuos/{id}")
    suspend fun updateResiduo(@Path("id") id: Long, @Body body: ResiduoCreateDto): Response<ResiduoDto>

    @DELETE("api/residuos/{id}")
    suspend fun deleteResiduo(@Path("id") id: Long): Response<Unit>

    // --- Documentos ---
    @GET("api/documentos")
    suspend fun getDocumentos(@Query("trasladoId") trasladoId: Long? = null): Response<List<DocumentoDto>>

    // --- Usuarios ---
    @GET("api/usuarios")
    suspend fun getUsuarios(@Query("rol") rol: String? = null): Response<List<UsuarioResumenDto>>

    // --- Rutas ---
    @GET("api/rutas")
    suspend fun getRutas(@Query("transportistaId") transportistaId: Long? = null): Response<List<RutaDto>>

    @GET("api/rutas/{id}")
    suspend fun getRuta(@Path("id") id: Long): Response<RutaDto>

    @POST("api/rutas")
    suspend fun createRuta(@Body body: RutaCreateDto): Response<RutaDto>

    @PUT("api/rutas/{id}")
    suspend fun updateRuta(@Path("id") id: Long, @Body body: RutaCreateDto): Response<RutaDto>

    @DELETE("api/rutas/{id}")
    suspend fun deleteRuta(@Path("id") id: Long): Response<Unit>
}
