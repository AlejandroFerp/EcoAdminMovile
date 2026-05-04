package com.ecoadminmovile.core.network

import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.EstadisticasDto
import com.ecoadminmovile.core.model.HistorialEventoDto
import com.ecoadminmovile.core.model.ResiduoDto
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

interface EcoAdminApi {
    @GET("login")
    suspend fun getLoginPage(): Response<ResponseBody>

    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String,
        @Field("_csrf") csrfToken: String
    ): Response<ResponseBody>

    @GET("api/perfil")
    suspend fun getProfile(): Response<UsuarioPerfilDto>

    @GET("api/estadisticas")
    suspend fun getEstadisticas(
        @Query("desde") desde: String? = null
    ): Response<EstadisticasDto>

    // --- Traslados ---
    @GET("api/traslados")
    suspend fun getTraslados(): Response<List<TrasladoDto>>

    @GET("api/traslados/{id}")
    suspend fun getTraslado(@Path("id") id: Long): Response<TrasladoDto>

    @POST("api/traslados")
    suspend fun createTraslado(@Body body: TrasladoCreateDto): Response<TrasladoDto>

    @PUT("api/traslados/{id}")
    suspend fun updateTraslado(@Path("id") id: Long, @Body body: TrasladoCreateDto): Response<TrasladoDto>

    @DELETE("api/traslados/{id}")
    suspend fun deleteTraslado(@Path("id") id: Long): Response<Unit>

    @FormUrlEncoded
    @PUT("api/traslados/{id}/estado")
    suspend fun updateTransferStatus(
        @Path("id") id: Long,
        @Field("estado") estado: String,
        @Field("comentario") comentario: String? = null
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

    // --- Residuos ---
    @GET("api/residuos")
    suspend fun getResiduos(): Response<List<ResiduoDto>>

    // --- Usuarios ---
    @GET("api/usuarios")
    suspend fun getUsuarios(@Query("rol") rol: String? = null): Response<List<UsuarioResumenDto>>

    // --- Rutas ---
    @GET("api/rutas")
    suspend fun getRutas(@Query("transportistaId") transportistaId: Long? = null): Response<List<RutaDto>>
}
