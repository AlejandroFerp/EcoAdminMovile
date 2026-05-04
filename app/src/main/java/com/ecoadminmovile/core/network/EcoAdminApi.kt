package com.ecoadminmovile.core.network

import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.EstadisticasDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.model.UsuarioPerfilDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface EcoAdminApi {
    @FormUrlEncoded
    @POST("login")
    suspend fun login(
        @Field("username") email: String,
        @Field("password") password: String
    ): Response<ResponseBody>

    @GET("api/perfil")
    suspend fun getProfile(): Response<UsuarioPerfilDto>

    @GET("api/estadisticas")
    suspend fun getEstadisticas(): Response<EstadisticasDto>

    @GET("api/traslados")
    suspend fun getTraslados(): Response<List<TrasladoDto>>

    @GET("api/traslados/{id}")
    suspend fun getTraslado(
        @Path("id") id: Long
    ): Response<TrasladoDto>

    @GET("api/centros")
    suspend fun getCentros(): Response<List<CentroDto>>
}
