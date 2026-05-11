package com.ecoadminmovile.feature.transfers

import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.HistorialEventoDto
import com.ecoadminmovile.core.model.ResiduoDto
import com.ecoadminmovile.core.model.RutaDto
import com.ecoadminmovile.core.model.TrasladoDto
import com.ecoadminmovile.core.model.UsuarioResumenDto
import com.ecoadminmovile.data.CatalogRepository
import com.ecoadminmovile.data.TransfersRepository
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
class TransferDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: TransfersRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = TransferDetailViewModel(repository = mockRepo)

    // --- Load ---

    @Test
    fun `load fetches transfer and history`() = runTest {
        val transfer = TrasladoDto(id = 5, codigo = "ECO-005", estado = "EN_TRANSITO")
        coEvery { mockRepo.loadTransfer(5) } returns Result.success(transfer)
        coEvery { mockRepo.loadHistory(5) } returns Result.success(
            listOf(HistorialEventoDto(estadoAnterior = null, estadoNuevo = "PENDIENTE", fecha = "2026-01-01"))
        )

        val vm = createViewModel()
        vm.load(5)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("ECO-005", state.transfer?.codigo)
        assertEquals(1, state.historial.size)
    }

    @Test
    fun `load shows error on failure`() = runTest {
        coEvery { mockRepo.loadTransfer(99) } returns Result.failure(RuntimeException("Not found"))

        val vm = createViewModel()
        vm.load(99)
        advanceUntilIdle()

        assertEquals("Not found", vm.uiState.value.errorMessage)
    }

    @Test
    fun `load does not reload if same id already loaded`() = runTest {
        val transfer = TrasladoDto(id = 5, codigo = "ECO-005", estado = "PENDIENTE")
        coEvery { mockRepo.loadTransfer(5) } returns Result.success(transfer)
        coEvery { mockRepo.loadHistory(5) } returns Result.success(emptyList())

        val vm = createViewModel()
        vm.load(5)
        advanceUntilIdle()
        vm.load(5) // Second call — should be no-op
        advanceUntilIdle()

        assertEquals("ECO-005", vm.uiState.value.transfer?.codigo)
    }

    // --- Status sheet ---

    @Test
    fun `showStatusSheet toggles state`() = runTest {
        val vm = createViewModel()
        assertFalse(vm.uiState.value.showStatusSheet)

        vm.showStatusSheet()
        assertTrue(vm.uiState.value.showStatusSheet)

        vm.hideStatusSheet()
        assertFalse(vm.uiState.value.showStatusSheet)
    }

    // --- Change status ---

    @Test
    fun `changeStatus updates transfer on success`() = runTest {
        val transfer = TrasladoDto(id = 10, codigo = "ECO-010", estado = "PENDIENTE")
        val updated = transfer.copy(estado = "EN_TRANSITO")
        coEvery { mockRepo.loadTransfer(10) } returns Result.success(transfer)
        coEvery { mockRepo.loadHistory(10) } returns Result.success(emptyList())
        coEvery { mockRepo.updateStatus(10, "EN_TRANSITO", null) } returns Result.success(updated)

        val vm = createViewModel()
        vm.load(10)
        advanceUntilIdle()

        vm.changeStatus("EN_TRANSITO")
        advanceUntilIdle()

        assertEquals("EN_TRANSITO", vm.uiState.value.transfer?.estado)
        assertFalse(vm.uiState.value.isUpdatingStatus)
    }

    @Test
    fun `changeStatus shows error on failure`() = runTest {
        val transfer = TrasladoDto(id = 10, codigo = "ECO-010", estado = "PENDIENTE")
        coEvery { mockRepo.loadTransfer(10) } returns Result.success(transfer)
        coEvery { mockRepo.loadHistory(10) } returns Result.success(emptyList())
        coEvery { mockRepo.updateStatus(10, "EN_TRANSITO", any()) } returns
            Result.failure(RuntimeException("Forbidden"))

        val vm = createViewModel()
        vm.load(10)
        advanceUntilIdle()

        vm.changeStatus("EN_TRANSITO", "comentario")
        advanceUntilIdle()

        assertEquals("Forbidden", vm.uiState.value.errorMessage)
    }

    // --- Delete ---

    @Test
    fun `deleteTransfer calls onDeleted on success`() = runTest {
        val transfer = TrasladoDto(id = 7, codigo = "ECO-007", estado = "PENDIENTE")
        coEvery { mockRepo.loadTransfer(7) } returns Result.success(transfer)
        coEvery { mockRepo.loadHistory(7) } returns Result.success(emptyList())
        coEvery { mockRepo.deleteTransfer(7) } returns Result.success(Unit)

        val vm = createViewModel()
        vm.load(7)
        advanceUntilIdle()

        var deleted = false
        vm.deleteTransfer { deleted = true }
        advanceUntilIdle()

        assertTrue(deleted)
    }

    @Test
    fun `deleteTransfer shows error on failure`() = runTest {
        val transfer = TrasladoDto(id = 7, codigo = "ECO-007", estado = "PENDIENTE")
        coEvery { mockRepo.loadTransfer(7) } returns Result.success(transfer)
        coEvery { mockRepo.loadHistory(7) } returns Result.success(emptyList())
        coEvery { mockRepo.deleteTransfer(7) } returns Result.failure(RuntimeException("Locked"))

        val vm = createViewModel()
        vm.load(7)
        advanceUntilIdle()

        vm.deleteTransfer {}
        advanceUntilIdle()

        assertEquals("Locked", vm.uiState.value.errorMessage)
    }

    // --- Companion: nextStates ---

    @Test
    fun `nextStates excludes current state`() {
        val states = TransferDetailViewModel.nextStates("PENDIENTE")
        assertFalse(states.contains("PENDIENTE"))
        assertTrue(states.contains("EN_TRANSITO"))
        assertTrue(states.contains("ENTREGADO"))
        assertTrue(states.contains("COMPLETADO"))
    }

    @Test
    fun `nextStates from COMPLETADO returns 3 options`() {
        val states = TransferDetailViewModel.nextStates("COMPLETADO")
        assertEquals(3, states.size)
        assertFalse(states.contains("COMPLETADO"))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransferFormViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: TransfersRepository
    private lateinit var mockCatalog: CatalogRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
        mockCatalog = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = TransferFormViewModel(
        repository = mockRepo,
        catalogRepository = mockCatalog
    )

    // --- Init form ---

    @Test
    fun `initForm loads catalogs`() = runTest {
        coEvery { mockCatalog.loadCentros() } returns Result.success(
            listOf(CentroDto(id = 1, codigo = "C1", nombre = "Centro 1", tipo = "PRODUCTOR"))
        )
        coEvery { mockCatalog.loadResiduos() } returns Result.success(
            listOf(ResiduoDto(id = 1, codigoLER = "20 01 01"))
        )
        coEvery { mockCatalog.loadTransportistas() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadRutas() } returns Result.success(emptyList())

        val vm = createViewModel()
        vm.initForm(null)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(1, state.centros.size)
        assertEquals(1, state.residuos.size)
    }

    @Test
    fun `initForm with id pre-fills from existing transfer`() = runTest {
        val existing = TrasladoDto(
            id = 3, codigo = "ECO-003", estado = "PENDIENTE",
            centroProductor = com.ecoadminmovile.core.model.CentroResumenDto(id = 10, codigo = "CP", nombre = "Prod"),
            centroGestor = com.ecoadminmovile.core.model.CentroResumenDto(id = 20, codigo = "CG", nombre = "Gest"),
            residuo = com.ecoadminmovile.core.model.ResiduoResumenDto(id = 5, codigoLER = "20 01"),
            observaciones = "Nota previa"
        )
        coEvery { mockCatalog.loadCentros() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadResiduos() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadTransportistas() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadRutas() } returns Result.success(emptyList())
        coEvery { mockRepo.loadTransfer(3) } returns Result.success(existing)

        val vm = createViewModel()
        vm.initForm(3)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(10L, state.selectedProductorId)
        assertEquals(20L, state.selectedGestorId)
        assertEquals(5L, state.selectedResiduoId)
        assertEquals("Nota previa", state.observaciones)
    }

    // --- Form validation ---

    @Test
    fun `isFormValid requires productor, gestor, and residuo`() {
        val invalid = TransferFormUiState()
        assertFalse(invalid.isFormValid)

        val valid = TransferFormUiState(
            selectedProductorId = 1, selectedGestorId = 2, selectedResiduoId = 3
        )
        assertTrue(valid.isFormValid)
    }

    // --- Field changes ---

    @Test
    fun `onFieldChanged updates corresponding field`() = runTest {
        val vm = createViewModel()

        vm.onFieldChanged(TransferFormField.Productor(42))
        assertEquals(42L, vm.uiState.value.selectedProductorId)

        vm.onFieldChanged(TransferFormField.Gestor(99))
        assertEquals(99L, vm.uiState.value.selectedGestorId)

        vm.onFieldChanged(TransferFormField.Observaciones("test note"))
        assertEquals("test note", vm.uiState.value.observaciones)
    }

    // --- Save ---

    @Test
    fun `save does nothing when form is invalid`() = runTest {
        val vm = createViewModel()
        vm.save()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.savedSuccessfully)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `save creates transfer on success`() = runTest {
        coEvery { mockCatalog.loadCentros() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadResiduos() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadTransportistas() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadRutas() } returns Result.success(emptyList())
        coEvery { mockRepo.createTransfer(any()) } returns Result.success(
            TrasladoDto(id = 99, codigo = "ECO-099", estado = "PENDIENTE")
        )

        val vm = createViewModel()
        vm.initForm(null)
        advanceUntilIdle()

        vm.onFieldChanged(TransferFormField.Productor(1))
        vm.onFieldChanged(TransferFormField.Gestor(2))
        vm.onFieldChanged(TransferFormField.Residuo(3))
        vm.save()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.savedSuccessfully)
    }

    @Test
    fun `save shows error on failure`() = runTest {
        coEvery { mockCatalog.loadCentros() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadResiduos() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadTransportistas() } returns Result.success(emptyList())
        coEvery { mockCatalog.loadRutas() } returns Result.success(emptyList())
        coEvery { mockRepo.createTransfer(any()) } returns Result.failure(RuntimeException("Server error"))

        val vm = createViewModel()
        vm.initForm(null)
        advanceUntilIdle()

        vm.onFieldChanged(TransferFormField.Productor(1))
        vm.onFieldChanged(TransferFormField.Gestor(2))
        vm.onFieldChanged(TransferFormField.Residuo(3))
        vm.save()
        advanceUntilIdle()

        assertEquals("Server error", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isSaving)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class QrScannerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: TransfersRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = QrScannerViewModel(repository = mockRepo)

    @Test
    fun `completeTransfer marks PENDIENTE as COMPLETADO`() = runTest {
        coEvery { mockRepo.loadTransfer(1) } returns Result.success(
            TrasladoDto(id = 1, codigo = "T-1", estado = "PENDIENTE")
        )
        coEvery { mockRepo.updateStatus(1, "COMPLETADO", any()) } returns Result.success(
            TrasladoDto(id = 1, codigo = "T-1", estado = "COMPLETADO")
        )

        val vm = createViewModel()
        var result: Pair<Boolean, String?> = false to null
        vm.completeTransfer(1) { success, error -> result = success to error }
        advanceUntilIdle()

        assertTrue(result.first)
        assertNull(result.second)
    }

    @Test
    fun `completeTransfer returns false when already COMPLETADO`() = runTest {
        coEvery { mockRepo.loadTransfer(2) } returns Result.success(
            TrasladoDto(id = 2, codigo = "T-2", estado = "COMPLETADO")
        )

        val vm = createViewModel()
        var result: Pair<Boolean, String?> = true to null
        vm.completeTransfer(2) { success, error -> result = success to error }
        advanceUntilIdle()

        assertFalse(result.first)
        assertTrue(result.second!!.contains("COMPLETADO"))
    }

    @Test
    fun `completeTransfer handles load error`() = runTest {
        coEvery { mockRepo.loadTransfer(3) } returns Result.failure(RuntimeException("Offline"))

        val vm = createViewModel()
        var result: Pair<Boolean, String?> = true to null
        vm.completeTransfer(3) { success, error -> result = success to error }
        advanceUntilIdle()

        assertFalse(result.first)
        assertEquals("Offline", result.second)
    }
}
