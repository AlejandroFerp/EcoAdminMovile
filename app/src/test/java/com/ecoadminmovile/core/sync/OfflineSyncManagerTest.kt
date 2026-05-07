package com.ecoadminmovile.core.sync

import com.ecoadminmovile.core.database.dao.CentroDao
import com.ecoadminmovile.core.database.dao.PendingOperationDao
import com.ecoadminmovile.core.database.dao.TrasladoDao
import com.ecoadminmovile.core.database.entity.CentroEntity
import com.ecoadminmovile.core.database.entity.PendingOperationEntity
import com.ecoadminmovile.core.database.entity.TrasladoEntity
import com.ecoadminmovile.core.model.CentroDto
import com.ecoadminmovile.core.model.DireccionDto
import com.ecoadminmovile.core.model.TrasladoDto
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSyncManagerTest {

    private lateinit var trasladoDao: TrasladoDao
    private lateinit var centroDao: CentroDao
    private lateinit var pendingOperationDao: PendingOperationDao
    private lateinit var syncManager: OfflineSyncManager

    @Before
    fun setup() {
        trasladoDao = mockk(relaxed = true)
        centroDao = mockk(relaxed = true)
        pendingOperationDao = mockk(relaxed = true)
        syncManager = OfflineSyncManager(trasladoDao, centroDao, pendingOperationDao)
    }

    // --- cacheTransfers ---

    @Test
    fun `cacheTransfers maps DTOs to entities and upserts`() = runTest {
        val transfers = listOf(
            TrasladoDto(id = 1, codigo = "TR-001", estado = "BORRADOR"),
            TrasladoDto(id = 2, codigo = "TR-002", estado = "EN_TRANSPORTE")
        )

        syncManager.cacheTransfers(transfers)

        coVerify { trasladoDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `cacheTransfers with empty list upserts empty`() = runTest {
        syncManager.cacheTransfers(emptyList())
        coVerify { trasladoDao.upsertAll(emptyList()) }
    }

    // --- cacheCenters ---

    @Test
    fun `cacheCenters maps DTOs to entities and upserts`() = runTest {
        val centers = listOf(
            CentroDto(id = 1, codigo = "C-001", nombre = "Centro A", tipo = "PRODUCTOR"),
            CentroDto(id = 2, codigo = "C-002", nombre = "Centro B", tipo = "GESTOR")
        )

        syncManager.cacheCenters(centers)

        coVerify { centroDao.upsertAll(match { it.size == 2 }) }
    }

    @Test
    fun `cacheCenters maps direccion fields correctly`() = runTest {
        val centers = listOf(
            CentroDto(
                id = 1, codigo = "C-001", nombre = "Test", tipo = "PRODUCTOR",
                direccion = DireccionDto(
                    calle = "Calle Mayor 1",
                    ciudad = "Madrid",
                    provincia = "Madrid",
                    codigoPostal = "28001"
                )
            )
        )

        val slot = slot<List<CentroEntity>>()
        coEvery { centroDao.upsertAll(capture(slot)) } just Runs

        syncManager.cacheCenters(centers)

        val entity = slot.captured[0]
        assertEquals("Calle Mayor 1", entity.direccionCalle)
        assertEquals("Madrid", entity.direccionCiudad)
        assertEquals("Madrid", entity.direccionProvincia)
        assertEquals("28001", entity.direccionCodigoPostal)
    }

    // --- enqueueOperation ---

    @Test
    fun `enqueueOperation inserts PendingOperationEntity`() = runTest {
        val slot = slot<PendingOperationEntity>()
        coEvery { pendingOperationDao.insert(capture(slot)) } just Runs

        syncManager.enqueueOperation(
            operationType = "STATUS_CHANGE",
            entityType = "TRASLADO",
            entityId = 42L,
            payload = "EN_TRANSPORTE|Salida del centro"
        )

        val entity = slot.captured
        assertEquals("STATUS_CHANGE", entity.operationType)
        assertEquals("TRASLADO", entity.entityType)
        assertEquals(42L, entity.entityId)
        assertEquals("EN_TRANSPORTE|Salida del centro", entity.payload)
    }

    @Test
    fun `enqueueOperation with null entityId`() = runTest {
        val slot = slot<PendingOperationEntity>()
        coEvery { pendingOperationDao.insert(capture(slot)) } just Runs

        syncManager.enqueueOperation(
            operationType = "DELETE",
            entityType = "RESIDUO",
            entityId = null,
            payload = ""
        )

        assertEquals(null, slot.captured.entityId)
    }

    // --- getPendingCount ---

    @Test
    fun `getPendingCount returns count from DAO`() = runTest {
        val pending = listOf(
            PendingOperationEntity(id = 1, operationType = "DELETE", entityType = "TRASLADO", entityId = 1, payload = ""),
            PendingOperationEntity(id = 2, operationType = "DELETE", entityType = "CENTRO", entityId = 2, payload = "")
        )
        coEvery { pendingOperationDao.getAll() } returns pending

        val count = syncManager.getPendingCount()

        assertEquals(2, count)
    }

    @Test
    fun `getPendingCount returns 0 when no pending`() = runTest {
        coEvery { pendingOperationDao.getAll() } returns emptyList()

        val count = syncManager.getPendingCount()

        assertEquals(0, count)
    }

    // --- DTO to Entity mapping ---

    @Test
    fun `TrasladoDto toEntity maps all fields`() {
        val dto = TrasladoDto(
            id = 5,
            codigo = "TR-005",
            estado = "COMPLETADO",
            fechaCreacion = "2024-01-15",
            fechaInicioTransporte = "2024-01-16",
            fechaEntrega = "2024-01-17",
            observaciones = "Test obs"
        )

        val entity = dto.toEntity()

        assertEquals(5, entity.id)
        assertEquals("TR-005", entity.codigo)
        assertEquals("COMPLETADO", entity.estado)
        assertEquals("2024-01-15", entity.fechaCreacion)
        assertEquals("2024-01-16", entity.fechaInicioTransporte)
        assertEquals("2024-01-17", entity.fechaEntrega)
        assertEquals("Test obs", entity.observaciones)
    }

    @Test
    fun `CentroDto toEntity maps all fields including address`() {
        val dto = CentroDto(
            id = 3,
            codigo = "PROD-003",
            nombre = "Centro Test",
            tipo = "PRODUCTOR",
            nima = "NIMA-003",
            telefono = "912345678",
            email = "test@centro.es",
            nombreContacto = "Contact Name",
            direccion = DireccionDto(
                calle = "Av Libertad 10",
                ciudad = "Valencia",
                provincia = "Valencia",
                codigoPostal = "46001"
            )
        )

        val entity = dto.toEntity()

        assertEquals(3, entity.id)
        assertEquals("PROD-003", entity.codigo)
        assertEquals("Centro Test", entity.nombre)
        assertEquals("PRODUCTOR", entity.tipo)
        assertEquals("NIMA-003", entity.nima)
        assertEquals("912345678", entity.telefono)
        assertEquals("test@centro.es", entity.email)
        assertEquals("Contact Name", entity.nombreContacto)
        assertEquals("Av Libertad 10", entity.direccionCalle)
        assertEquals("Valencia", entity.direccionCiudad)
        assertEquals("Valencia", entity.direccionProvincia)
        assertEquals("46001", entity.direccionCodigoPostal)
    }

    @Test
    fun `CentroDto toEntity handles null address`() {
        val dto = CentroDto(
            id = 1,
            codigo = "G-001",
            nombre = "No Address",
            tipo = "GESTOR",
            direccion = null
        )

        val entity = dto.toEntity()

        assertEquals(null, entity.direccionCalle)
        assertEquals(null, entity.direccionCiudad)
        assertEquals(null, entity.direccionProvincia)
        assertEquals(null, entity.direccionCodigoPostal)
    }

    @Test
    fun `TrasladoEntity toDto maps back correctly`() {
        val entity = TrasladoEntity(
            id = 10,
            codigo = "TR-010",
            estado = "BORRADOR",
            fechaCreacion = "2024-02-01",
            fechaInicioTransporte = null,
            fechaEntrega = null,
            observaciones = null
        )

        val dto = entity.toDto()

        assertEquals(10, dto.id)
        assertEquals("TR-010", dto.codigo)
        assertEquals("BORRADOR", dto.estado)
        assertEquals("2024-02-01", dto.fechaCreacion)
        assertEquals(null, dto.fechaInicioTransporte)
        assertEquals(null, dto.fechaEntrega)
        assertEquals(null, dto.observaciones)
    }
}
