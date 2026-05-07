package com.ecoadminmovile.feature.residuos

import com.ecoadminmovile.core.model.ResiduoCreateDto
import com.ecoadminmovile.core.model.ResiduoDto
import com.ecoadminmovile.data.ResiduosRepository
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
class ResiduosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: ResiduosRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel(): ResiduosViewModel {
        coEvery { mockRepo.loadAll() } returns Result.success(sampleResiduos())
        return ResiduosViewModel(repository = mockRepo)
    }

    // --- Load ---

    @Test
    fun `init loads residuos`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(3, state.residuos.size)
    }

    @Test
    fun `load shows error on failure`() = runTest {
        coEvery { mockRepo.loadAll() } returns Result.failure(RuntimeException("Network"))
        val vm = ResiduosViewModel(repository = mockRepo)
        advanceUntilIdle()

        assertEquals("Network", vm.uiState.value.errorMessage)
    }

    // --- Search ---

    @Test
    fun `updateSearch filters by codigoLER`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("20 01 01")
        assertEquals(1, vm.uiState.value.filteredResiduos.size)
    }

    @Test
    fun `updateSearch filters by descripcion`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("Plástico")
        assertEquals(1, vm.uiState.value.filteredResiduos.size)
    }

    @Test
    fun `empty search shows all residuos`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.updateSearch("xyz")
        assertEquals(0, vm.uiState.value.filteredResiduos.size)
        vm.updateSearch("")
        assertEquals(3, vm.uiState.value.filteredResiduos.size)
    }

    // --- Delete ---

    @Test
    fun `delete reloads list on success`() = runTest {
        coEvery { mockRepo.delete(1) } returns Result.success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        coEvery { mockRepo.loadAll() } returns Result.success(sampleResiduos().drop(1))
        vm.delete(1)
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.residuos.size)
    }

    @Test
    fun `delete shows error on failure`() = runTest {
        coEvery { mockRepo.delete(1) } returns Result.failure(RuntimeException("Forbidden"))
        val vm = createViewModel()
        advanceUntilIdle()

        vm.delete(1)
        advanceUntilIdle()

        assertEquals("Forbidden", vm.uiState.value.errorMessage)
    }

    // --- UiState computed ---

    @Test
    fun `filteredResiduos is case insensitive`() {
        val state = ResiduosUiState(
            residuos = sampleResiduos(),
            searchQuery = "PAPEL"
        )
        assertEquals(1, state.filteredResiduos.size)
    }

    private fun sampleResiduos() = listOf(
        ResiduoDto(id = 1, codigoLER = "20 01 01", descripcion = "Papel y cartón"),
        ResiduoDto(id = 2, codigoLER = "20 01 39", descripcion = "Plástico"),
        ResiduoDto(id = 3, codigoLER = "20 03 01", descripcion = "Mezcla residuos")
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class ResiduoFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: ResiduosRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = ResiduoFormViewModel(repository = mockRepo)

    // --- Init form ---

    @Test
    fun `initForm with null id does nothing`() = runTest {
        val vm = createViewModel()
        vm.initForm(null)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isEditing)
        assertEquals("", vm.uiState.value.codigoLER)
    }

    @Test
    fun `initForm with id loads existing residuo`() = runTest {
        coEvery { mockRepo.load(5) } returns Result.success(
            ResiduoDto(id = 5, codigoLER = "15 01 02", descripcion = "Envase plástico", cantidad = 12.5, unidad = "kg")
        )

        val vm = createViewModel()
        vm.initForm(5)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals("15 01 02", state.codigoLER)
        assertEquals("Envase plástico", state.descripcion)
        assertEquals("12.5", state.cantidad)
        assertEquals("kg", state.unidad)
    }

    @Test
    fun `initForm shows error on load failure`() = runTest {
        coEvery { mockRepo.load(99) } returns Result.failure(RuntimeException("Not found"))

        val vm = createViewModel()
        vm.initForm(99)
        advanceUntilIdle()

        assertEquals("Not found", vm.uiState.value.errorMessage)
    }

    // --- Field changes ---

    @Test
    fun `field change methods update state`() {
        val vm = createViewModel()
        vm.onCodigoLERChanged("20 01")
        vm.onDescripcionChanged("Desc")
        vm.onCantidadChanged("5.0")
        vm.onUnidadChanged("t")

        val state = vm.uiState.value
        assertEquals("20 01", state.codigoLER)
        assertEquals("Desc", state.descripcion)
        assertEquals("5.0", state.cantidad)
        assertEquals("t", state.unidad)
    }

    // --- Validation ---

    @Test
    fun `isFormValid requires codigoLER`() {
        assertFalse(ResiduoFormUiState(codigoLER = "").isFormValid)
        assertFalse(ResiduoFormUiState(codigoLER = "   ").isFormValid)
        assertTrue(ResiduoFormUiState(codigoLER = "20 01").isFormValid)
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
    fun `save creates new residuo on success`() = runTest {
        coEvery { mockRepo.create(any()) } returns Result.success(
            ResiduoDto(id = 10, codigoLER = "20 01 01")
        )

        val vm = createViewModel()
        vm.onCodigoLERChanged("20 01 01")
        vm.onDescripcionChanged("Papel")
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `save updates existing residuo when editing`() = runTest {
        coEvery { mockRepo.load(5) } returns Result.success(
            ResiduoDto(id = 5, codigoLER = "15 01")
        )
        coEvery { mockRepo.update(5, any()) } returns Result.success(
            ResiduoDto(id = 5, codigoLER = "15 01 02")
        )

        val vm = createViewModel()
        vm.initForm(5)
        advanceUntilIdle()

        vm.onCodigoLERChanged("15 01 02")
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
        coVerify { mockRepo.update(5, any()) }
    }

    @Test
    fun `save shows error on failure`() = runTest {
        coEvery { mockRepo.create(any()) } returns Result.failure(RuntimeException("Duplicate"))

        val vm = createViewModel()
        vm.onCodigoLERChanged("20 01 01")
        vm.save()
        advanceUntilIdle()

        assertEquals("Duplicate", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `save trims whitespace and converts blanks to null`() = runTest {
        coEvery { mockRepo.create(any()) } returns Result.success(
            ResiduoDto(id = 1, codigoLER = "20 01")
        )

        val vm = createViewModel()
        vm.onCodigoLERChanged("  20 01  ")
        vm.onDescripcionChanged("   ")
        vm.onCantidadChanged("abc") // invalid double
        vm.onUnidadChanged("")
        vm.save()
        advanceUntilIdle()

        coVerify {
            mockRepo.create(match {
                it.codigoLER == "20 01" &&
                    it.descripcion == null &&
                    it.cantidad == null &&
                    it.unidad == null
            })
        }
    }
}
