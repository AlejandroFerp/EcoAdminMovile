package com.ecoadminmovile.feature.transfers

import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.data.TransfersRepository
import com.ecoadminmovile.feature.dashboard.ThrowingApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class TransfersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeTransfersRepo

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepo = FakeTransfersRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = TransfersViewModel(repository = fakeRepo)

    // --- Load ---

    @Test
    fun `init loads transfers and populates filteredTransfers`() = runTest {
        fakeRepo.loadResult = Result.success(sampleTransfers())

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.transfers.size)
        assertEquals(3, state.filteredTransfers.size)
        assertNull(state.errorMessage)
    }

    @Test
    fun `load shows error when repository fails`() = runTest {
        fakeRepo.loadResult = Result.failure(RuntimeException("Sin conexión"))

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Sin conexión", state.errorMessage)
    }

    // --- Search filter ---

    @Test
    fun `updateSearch filters by codigo`() = runTest {
        fakeRepo.loadResult = Result.success(sampleTransfers())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("003")
        val state = vm.uiState.value
        assertEquals(1, state.filteredTransfers.size)
        assertEquals("ECO-003", state.filteredTransfers[0].codigo)
    }

    @Test
    fun `updateSearch with empty string shows all`() = runTest {
        fakeRepo.loadResult = Result.success(sampleTransfers())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("003")
        vm.updateSearch("")
        assertEquals(3, vm.uiState.value.filteredTransfers.size)
    }

    // --- Status filter ---

    @Test
    fun `filterByStatus filters transfers by estado`() = runTest {
        fakeRepo.loadResult = Result.success(sampleTransfers())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.filterByStatus("COMPLETADO")
        val state = vm.uiState.value
        assertEquals(1, state.filteredTransfers.size)
        assertEquals("ECO-003", state.filteredTransfers[0].codigo)
    }

    @Test
    fun `filterByStatus with same status toggles off`() = runTest {
        fakeRepo.loadResult = Result.success(sampleTransfers())
        val vm = createViewModel()
        advanceUntilIdle()

        vm.filterByStatus("PENDIENTE")
        vm.filterByStatus("PENDIENTE") // Toggle off
        assertNull(vm.uiState.value.selectedStatus)
        assertEquals(3, vm.uiState.value.filteredTransfers.size)
    }

    // --- Delete ---

    @Test
    fun `deleteTransfer reloads on success`() = runTest {
        fakeRepo.loadResult = Result.success(sampleTransfers())
        fakeRepo.deleteResult = Result.success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        fakeRepo.loadResult = Result.success(sampleTransfers().drop(1))
        vm.deleteTransfer(1)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.transfers.size)
    }

    @Test
    fun `deleteTransfer shows error on failure`() = runTest {
        fakeRepo.loadResult = Result.success(sampleTransfers())
        fakeRepo.deleteResult = Result.failure(RuntimeException("Forbidden"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.deleteTransfer(1)
        advanceUntilIdle()

        assertEquals("Forbidden", vm.uiState.value.errorMessage)
    }

    // --- Helpers ---

    private fun sampleTransfers() = listOf(
        TrasladoDto(id = 1, codigo = "ECO-001", estado = "PENDIENTE"),
        TrasladoDto(id = 2, codigo = "ECO-002", estado = "EN_TRANSITO"),
        TrasladoDto(id = 3, codigo = "ECO-003", estado = "COMPLETADO")
    )
}

// Fake que hereda de TransfersRepository (open class, params default null)
class FakeTransfersRepo : TransfersRepository(
    api = ThrowingApi.instance
) {
    var loadResult: Result<List<TrasladoDto>> = Result.success(emptyList())
    var deleteResult: Result<Unit> = Result.success(Unit)
    var statusResult: Result<TrasladoDto> = Result.success(TrasladoDto(id = 0, codigo = "", estado = ""))

    override suspend fun loadTransfers(): Result<List<TrasladoDto>> = loadResult

    suspend fun deleteTransfer(id: Long): Result<Unit> = deleteResult
}
