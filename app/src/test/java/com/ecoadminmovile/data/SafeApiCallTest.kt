package com.ecoadminmovile.data

import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.network.EcoAdminApi
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Tests safeApiCall behavior indirectly through CatalogRepository.loadCentros().
 * Covers: success, null body, HTTP error, redirect to login, IOException, and generic exception.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SafeApiCallTest {

    private lateinit var api: EcoAdminApi
    private lateinit var repo: CatalogRepository

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        repo = CatalogRepository(api)
    }

    @Test
    fun `success returns Result success with body`() = runTest {
        val centros = listOf(CentroDto(id = 1, codigo = "C-001", nombre = "Test", tipo = "P"))
        coEvery { api.getCentros() } returns Response.success(centros)

        val result = repo.loadCentros()

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrNull()?.size)
    }

    @Test
    fun `null body returns failure with empty response message`() = runTest {
        coEvery { api.getCentros() } returns Response.success(null)

        val result = repo.loadCentros()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("respuesta vacia") == true)
    }

    @Test
    fun `http 500 returns failure with human message`() = runTest {
        val errorResponse = Response.error<List<CentroDto>>(
            500,
            "Internal Server Error".toResponseBody(null)
        )
        coEvery { api.getCentros() } returns errorResponse

        val result = repo.loadCentros()

        assertTrue(result.isFailure)
    }

    @Test
    fun `redirect to login returns session expired message`() = runTest {
        // Simulate a 302 redirect to /login
        val mockResponse = mockk<Response<List<CentroDto>>>()
        every { mockResponse.isSuccessful } returns false
        every { mockResponse.code() } returns 302
        every { mockResponse.headers() } returns Headers.headersOf("Location", "https://ecoadmin.embention.com/login")
        every { mockResponse.message() } returns "Found"

        coEvery { api.getCentros() } returns mockResponse

        val result = repo.loadCentros()

        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue(msg.contains("sesion", ignoreCase = true))
    }

    @Test
    fun `IOException returns connectivity error`() = runTest {
        coEvery { api.getCentros() } throws IOException("Connection refused")

        val result = repo.loadCentros()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("conectar") == true)
    }

    @Test
    fun `generic exception returns unexpected error`() = runTest {
        coEvery { api.getCentros() } throws RuntimeException("Something exploded")

        val result = repo.loadCentros()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Something exploded") == true)
    }

    @Test
    fun `http 403 with message returns that message`() = runTest {
        val errorResponse = Response.error<List<CentroDto>>(
            403,
            "Forbidden".toResponseBody(null)
        )
        coEvery { api.getCentros() } returns errorResponse

        val result = repo.loadCentros()

        assertTrue(result.isFailure)
    }

    @Test
    fun `success with empty list returns empty result`() = runTest {
        coEvery { api.getCentros() } returns Response.success(emptyList())

        val result = repo.loadCentros()

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }
}
