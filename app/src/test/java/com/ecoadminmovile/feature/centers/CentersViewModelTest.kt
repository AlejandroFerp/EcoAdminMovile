package com.ecoadminmovile.feature.centers

import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.data.CentersRepository
import io.mockk.coEvery
import io.mockk.mockk
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
class CentersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: CentersRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel(): CentersViewModel {
        coEvery { mockRepo.loadCenters() } returns Result.success(sampleCenters())
        return CentersViewModel(repository = mockRepo)
    }

    // --- Load ---

    @Test
    fun `init loads centers and applies filters`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(4, state.centers.size)
        assertEquals(4, state.filteredCenters.size)
    }

    @Test
    fun `load shows error on failure`() = runTest {
        coEvery { mockRepo.loadCenters() } returns Result.failure(RuntimeException("401"))
        val vm = CentersViewModel(repository = mockRepo)
        advanceUntilIdle()

        assertEquals("401", vm.uiState.value.errorMessage)
    }

    // --- Search ---

    @Test
    fun `updateSearch filters by nombre`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("Acme")
        assertEquals(1, vm.uiState.value.filteredCenters.size)
    }

    @Test
    fun `updateSearch filters by codigo`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("PROD-002")
        assertEquals(1, vm.uiState.value.filteredCenters.size)
    }

    @Test
    fun `updateSearch filters by nima`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("NIMA-999")
        assertEquals(1, vm.uiState.value.filteredCenters.size)
    }

    @Test
    fun `updateSearch is case insensitive`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("acme")
        assertEquals(1, vm.uiState.value.filteredCenters.size)
    }

    @Test
    fun `empty search shows all`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("xyz")
        assertEquals(0, vm.uiState.value.filteredCenters.size)
        vm.updateSearch("")
        assertEquals(4, vm.uiState.value.filteredCenters.size)
    }

    // --- Type filter ---

    @Test
    fun `filterByType filters by PRODUCTOR`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.filterByType("PRODUCTOR")
        assertEquals(2, vm.uiState.value.filteredCenters.size)
        assertEquals("PRODUCTOR", vm.uiState.value.selectedType)
    }

    @Test
    fun `filterByType filters by GESTOR`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.filterByType("GESTOR")
        assertEquals(2, vm.uiState.value.filteredCenters.size)
    }

    @Test
    fun `filterByType with same type toggles off`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.filterByType("PRODUCTOR")
        vm.filterByType("PRODUCTOR") // Toggle off
        assertNull(vm.uiState.value.selectedType)
        assertEquals(4, vm.uiState.value.filteredCenters.size)
    }

    // --- Combined filters ---

    @Test
    fun `search and type filter combined`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("Eco")
        vm.filterByType("GESTOR")

        val filtered = vm.uiState.value.filteredCenters
        assertEquals(1, filtered.size)
        assertEquals("EcoGestor", filtered[0].nombre)
    }

    private fun sampleCenters() = listOf(
        CentroDto(id = 1, codigo = "PROD-001", nombre = "Acme Factory", tipo = "PRODUCTOR", nima = "NIMA-001"),
        CentroDto(id = 2, codigo = "PROD-002", nombre = "Beta Industries", tipo = "PRODUCTOR", nima = "NIMA-002"),
        CentroDto(id = 3, codigo = "GEST-001", nombre = "EcoGestor", tipo = "GESTOR", nima = "NIMA-999"),
        CentroDto(id = 4, codigo = "GEST-002", nombre = "GreenRecycling", tipo = "GESTOR", nima = "NIMA-004")
    )
}
