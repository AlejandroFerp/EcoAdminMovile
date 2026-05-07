package com.ecoadminmovile.feature.rutas

import com.ecoadminmovile.core.model.RutaDto
import com.ecoadminmovile.data.RutasRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class RutasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: RutasRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel(): RutasViewModel {
        coEvery { mockRepo.loadAll() } returns Result.success(sampleRutas())
        return RutasViewModel(repository = mockRepo)
    }

    // --- Load ---

    @Test
    fun `init loads rutas`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(3, vm.uiState.value.rutas.size)
    }

    @Test
    fun `load shows error on failure`() = runTest {
        coEvery { mockRepo.loadAll() } returns Result.failure(RuntimeException("Timeout"))
        val vm = RutasViewModel(repository = mockRepo)
        advanceUntilIdle()

        assertEquals("Timeout", vm.uiState.value.errorMessage)
    }

    // --- Search ---

    @Test
    fun `updateSearch filters by nombre`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("Valencia")
        assertEquals(1, vm.uiState.value.filteredRutas.size)
        assertEquals("Madrid-Valencia", vm.uiState.value.filteredRutas[0].nombre)
    }

    @Test
    fun `updateSearch is case insensitive`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("MADRID")
        assertEquals(2, vm.uiState.value.filteredRutas.size)
    }

    @Test
    fun `empty search shows all`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("nope")
        assertEquals(0, vm.uiState.value.filteredRutas.size)
        vm.updateSearch("")
        assertEquals(3, vm.uiState.value.filteredRutas.size)
    }

    // --- Delete ---

    @Test
    fun `delete reloads on success`() = runTest {
        coEvery { mockRepo.delete(1) } returns Result.success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        coEvery { mockRepo.loadAll() } returns Result.success(sampleRutas().drop(1))
        vm.delete(1)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.rutas.size)
    }

    @Test
    fun `delete shows error on failure`() = runTest {
        coEvery { mockRepo.delete(1) } returns Result.failure(RuntimeException("In use"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.delete(1)
        advanceUntilIdle()

        assertEquals("In use", vm.uiState.value.errorMessage)
    }

    // --- UiState computed ---

    @Test
    fun `filteredRutas with blank query returns all`() {
        val state = RutasUiState(rutas = sampleRutas(), searchQuery = "")
        assertEquals(3, state.filteredRutas.size)
    }

    private fun sampleRutas() = listOf(
        RutaDto(id = 1, nombre = "Madrid-Valencia", distanciaKm = 350.0),
        RutaDto(id = 2, nombre = "Madrid-Barcelona", distanciaKm = 620.0),
        RutaDto(id = 3, nombre = "Sevilla-Granada", distanciaKm = 250.0)
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class RutaFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: RutasRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = RutaFormViewModel(repository = mockRepo)

    // --- Init form ---

    @Test
    fun `initForm with null does nothing`() = runTest {
        val vm = createViewModel()
        vm.initForm(null)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isEditing)
    }

    @Test
    fun `initForm with id loads existing ruta`() = runTest {
        coEvery { mockRepo.load(7) } returns Result.success(
            RutaDto(id = 7, nombre = "Madrid-Sevilla", distanciaKm = 530.0)
        )

        val vm = createViewModel()
        vm.initForm(7)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals("Madrid-Sevilla", state.nombre)
        assertEquals("530.0", state.distanciaKm)
    }

    @Test
    fun `initForm shows error on failure`() = runTest {
        coEvery { mockRepo.load(99) } returns Result.failure(RuntimeException("Not found"))

        val vm = createViewModel()
        vm.initForm(99)
        advanceUntilIdle()

        assertEquals("Not found", vm.uiState.value.errorMessage)
    }

    // --- Field changes ---

    @Test
    fun `field updates reflect in state`() {
        val vm = createViewModel()
        vm.onNombreChanged("Test Route")
        vm.onDistanciaChanged("123.5")

        assertEquals("Test Route", vm.uiState.value.nombre)
        assertEquals("123.5", vm.uiState.value.distanciaKm)
    }

    // --- Validation ---

    @Test
    fun `isFormValid requires nombre`() {
        assertFalse(RutaFormUiState(nombre = "").isFormValid)
        assertFalse(RutaFormUiState(nombre = "   ").isFormValid)
        assertTrue(RutaFormUiState(nombre = "Ruta Norte").isFormValid)
    }

    // --- Save ---

    @Test
    fun `save does nothing when form invalid`() = runTest {
        val vm = createViewModel()
        vm.save()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.savedSuccessfully)
        coVerify(exactly = 0) { mockRepo.create(any()) }
    }

    @Test
    fun `save creates new ruta`() = runTest {
        coEvery { mockRepo.create(any()) } returns Result.success(
            RutaDto(id = 10, nombre = "Nueva Ruta")
        )

        val vm = createViewModel()
        vm.onNombreChanged("Nueva Ruta")
        vm.onDistanciaChanged("200")
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
    }

    @Test
    fun `save updates existing ruta when editing`() = runTest {
        coEvery { mockRepo.load(7) } returns Result.success(
            RutaDto(id = 7, nombre = "Old Name", distanciaKm = 100.0)
        )
        coEvery { mockRepo.update(7, any()) } returns Result.success(
            RutaDto(id = 7, nombre = "New Name", distanciaKm = 200.0)
        )

        val vm = createViewModel()
        vm.initForm(7)
        advanceUntilIdle()

        vm.onNombreChanged("New Name")
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
        coVerify { mockRepo.update(7, any()) }
    }

    @Test
    fun `save shows error on failure`() = runTest {
        coEvery { mockRepo.create(any()) } returns Result.failure(RuntimeException("Conflict"))

        val vm = createViewModel()
        vm.onNombreChanged("Ruta")
        vm.save()
        advanceUntilIdle()

        assertEquals("Conflict", vm.uiState.value.errorMessage)
    }

    @Test
    fun `save parses distanciaKm correctly and handles non-numeric`() = runTest {
        coEvery { mockRepo.create(any()) } returns Result.success(
            RutaDto(id = 1, nombre = "R")
        )

        val vm = createViewModel()
        vm.onNombreChanged("Ruta Test")
        vm.onDistanciaChanged("not-a-number")
        vm.save()
        advanceUntilIdle()

        coVerify { mockRepo.create(match { it.distanciaKm == null }) }
    }
}
