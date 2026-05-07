package com.ecoadminmovile.feature.documentos

import com.ecoadminmovile.core.model.DocumentoDto
import com.ecoadminmovile.data.DocumentosRepository
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
class DocumentosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: DocumentosRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel(): DocumentosViewModel {
        coEvery { mockRepo.loadAll(null) } returns Result.success(sampleDocuments())
        return DocumentosViewModel(repository = mockRepo)
    }

    // --- Load ---

    @Test
    fun `init loads documentos`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(5, vm.uiState.value.documentos.size)
    }

    @Test
    fun `load shows error on failure`() = runTest {
        coEvery { mockRepo.loadAll(null) } returns Result.failure(RuntimeException("Error"))
        val vm = DocumentosViewModel(repository = mockRepo)
        advanceUntilIdle()

        assertEquals("Error", vm.uiState.value.errorMessage)
    }

    // --- Filter by type ---

    @Test
    fun `filterByTipo filters documentos`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.filterByTipo("contrato")
        assertEquals(1, vm.uiState.value.filteredDocumentos.size)
    }

    @Test
    fun `filterByTipo with null shows all`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.filterByTipo("contrato")
        vm.filterByTipo(null)
        assertEquals(5, vm.uiState.value.filteredDocumentos.size)
    }

    @Test
    fun `filterByTipo is case insensitive`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.filterByTipo("DI")
        val filtered = vm.uiState.value.filteredDocumentos
        assertEquals(1, filtered.size)
        assertEquals("di", filtered[0].tipo)
    }

    // --- Computed properties ---

    @Test
    fun `tiposDisponibles returns distinct sorted types`() {
        val state = DocumentosUiState(documentos = sampleDocuments())
        val tipos = state.tiposDisponibles
        assertEquals(listOf("carta-porte", "certificado", "contrato", "di", "notificacion"), tipos)
    }

    @Test
    fun `filteredDocumentos with null filter returns all`() {
        val state = DocumentosUiState(documentos = sampleDocuments(), tipoFilter = null)
        assertEquals(5, state.filteredDocumentos.size)
    }

    private fun sampleDocuments() = listOf(
        DocumentoDto(id = 1, tipo = "carta-porte", nombre = "CP-001"),
        DocumentoDto(id = 2, tipo = "notificacion", nombre = "NP-001"),
        DocumentoDto(id = 3, tipo = "certificado", nombre = "CERT-001"),
        DocumentoDto(id = 4, tipo = "di", nombre = "DI-001"),
        DocumentoDto(id = 5, tipo = "contrato", nombre = "CONT-001")
    )
}
