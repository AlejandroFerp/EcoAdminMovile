package com.ecoadminmovile.feature.dashboard

import com.ecoadminmovile.core.model.EstadisticasDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.data.DashboardRepository
import com.ecoadminmovile.data.TransfersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitarios para DashboardViewModel.
 *
 * Conceptos demostrados:
 * - StandardTestDispatcher: dispatcher de test que permite controlar la ejecucion de corrutinas.
 * - Dispatchers.setMain(): reemplaza el Main dispatcher (que no existe en JVM puro) por uno de test.
 * - advanceUntilIdle(): avanza el reloj virtual hasta que todas las corrutinas pendientes terminen.
 * - Fakes con override: subclases que sobreescriben metodos para devolver datos controlados.
 *
 * Patron:
 * - AAA (Arrange-Act-Assert): cada test tiene 3 fases claras.
 * - Fakes > Mocks: mas simples, mas legibles, no requieren libreria de mocking.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    // StandardTestDispatcher: NO ejecuta corrutinas automaticamente.
    // Necesitas llamar advanceUntilIdle() para que avancen.
    // Esto te da control total sobre CUANDO se ejecutan.
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeDashboardRepo: FakeDashboardRepository
    private lateinit var fakeTransfersRepo: FakeTransfersRepository

    // @Before: se ejecuta ANTES de cada test. Prepara el entorno limpio.
    @Before
    fun setup() {
        // setMain: reemplaza Dispatchers.Main (que usa Android Looper) por nuestro dispatcher.
        // Sin esto, viewModelScope.launch crashea porque no hay Main looper en JVM.
        Dispatchers.setMain(testDispatcher)
        fakeDashboardRepo = FakeDashboardRepository()
        fakeTransfersRepo = FakeTransfersRepository()
    }

    // @After: se ejecuta DESPUES de cada test. Limpia efectos secundarios.
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = DashboardViewModel(
        repository = fakeDashboardRepo,
        transfersRepository = fakeTransfersRepo
    )

    @Test
    fun `init loads dashboard and transfers successfully`() = runTest {
        // Arrange
        fakeDashboardRepo.resultToReturn = Result.success(
            EstadisticasDto(totalCentros = 5, totalResiduos = 10,
                trasladosPendientes = 2, trasladosEnTransito = 1,
                trasladosEntregados = 0, trasladosCompletados = 7)
        )
        fakeTransfersRepo.resultToReturn = Result.success(listOf(
            TrasladoDto(id = 1, codigo = "ECO-001", estado = "PENDIENTE"),
            TrasladoDto(id = 2, codigo = "ECO-002", estado = "COMPLETADO")
        ))

        // Act
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertFalse("isLoading should be false after load", state.isLoading)
        assertEquals(5, state.data.totalCentros)
        assertEquals(2, state.recentTransfers.size)
        assertNull("errorMessage should be null on success", state.errorMessage)
    }

    @Test
    fun `load shows error when dashboard fails`() = runTest {
        fakeDashboardRepo.resultToReturn = Result.failure(RuntimeException("Network error"))
        fakeTransfersRepo.resultToReturn = Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Network error", state.errorMessage)
    }

    @Test
    fun `load shows error when transfers fail`() = runTest {
        fakeDashboardRepo.resultToReturn = Result.success(EstadisticasDto())
        fakeTransfersRepo.resultToReturn = Result.failure(RuntimeException("Timeout"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(
            "Error should mention Traslados",
            state.errorMessage?.contains("Traslados") == true
        )
    }

    @Test
    fun `load concatenates errors when both fail`() = runTest {
        fakeDashboardRepo.resultToReturn = Result.failure(RuntimeException("Dashboard down"))
        fakeTransfersRepo.resultToReturn = Result.failure(RuntimeException("Transfers down"))

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.errorMessage?.contains("Dashboard down") == true)
        assertTrue(state.errorMessage?.contains("Traslados") == true)
    }

    @Test
    fun `setPeriod changes period and reloads`() = runTest {
        fakeDashboardRepo.resultToReturn = Result.success(EstadisticasDto(totalCentros = 99))
        fakeTransfersRepo.resultToReturn = Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setPeriod(DashboardPeriod.WEEK)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DashboardPeriod.WEEK, state.selectedPeriod)
        assertEquals(99, state.data.totalCentros)
    }

    @Test
    fun `recent transfers are limited to 10`() = runTest {
        val manyTransfers = (1..15L).map {
            TrasladoDto(id = it, codigo = "T-$it", estado = "PENDIENTE")
        }
        fakeDashboardRepo.resultToReturn = Result.success(EstadisticasDto())
        fakeTransfersRepo.resultToReturn = Result.success(manyTransfers)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(10, viewModel.uiState.value.recentTransfers.size)
    }
}

// ============================================================================
// FAKES
// ============================================================================
// Un Fake es una implementacion funcional simplificada.
// Hereda de la clase real (ahora marcada como open) y sobreescribe los metodos
// para devolver datos controlados sin hacer llamadas de red.

class FakeDashboardRepository : DashboardRepository(
    api = ThrowingApi.instance
) {
    var resultToReturn: Result<EstadisticasDto> = Result.success(EstadisticasDto())
    var lastDesde: String? = null

    override suspend fun loadDashboard(desde: String?): Result<EstadisticasDto> {
        lastDesde = desde
        return resultToReturn
    }
}

class FakeTransfersRepository : TransfersRepository(
    api = ThrowingApi.instance
) {
    var resultToReturn: Result<List<TrasladoDto>> = Result.success(emptyList())

    override suspend fun loadTransfers(): Result<List<TrasladoDto>> {
        return resultToReturn
    }
}
