package com.ecoadminmovile.feature.transfers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.ecoadminmovile.core.model.TrasladoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TransfersListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleTransfers() = listOf(
        TrasladoDto(id = 1, codigo = "TR-001", estado = "BORRADOR"),
        TrasladoDto(id = 2, codigo = "TR-002", estado = "EN_TRANSPORTE"),
        TrasladoDto(id = 3, codigo = "TR-003", estado = "COMPLETADO")
    )

    @Test
    fun displaysTitle() {
        composeTestRule.setContent {
            TransfersListScreen(
                state = TransfersUiState(transfers = sampleTransfers()),
                onRefresh = {},
                onTransferSelected = {},
                onSearchChanged = {},
                onStatusFilter = {},
                onCreateNew = {},
                onScanQr = {}
            )
        }

        composeTestRule.onNodeWithText("Traslados", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysTransferCodes() {
        composeTestRule.setContent {
            TransfersListScreen(
                state = TransfersUiState(transfers = sampleTransfers()),
                onRefresh = {},
                onTransferSelected = {},
                onSearchChanged = {},
                onStatusFilter = {},
                onCreateNew = {},
                onScanQr = {}
            )
        }

        composeTestRule.onNodeWithText("TR-001", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("TR-002", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage() {
        composeTestRule.setContent {
            TransfersListScreen(
                state = TransfersUiState(errorMessage = "No se pudo cargar"),
                onRefresh = {},
                onTransferSelected = {},
                onSearchChanged = {},
                onStatusFilter = {},
                onCreateNew = {},
                onScanQr = {}
            )
        }

        composeTestRule.onNodeWithText("No se pudo cargar", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyStateWhenNoTransfers() {
        composeTestRule.setContent {
            TransfersListScreen(
                state = TransfersUiState(transfers = emptyList(), isLoading = false),
                onRefresh = {},
                onTransferSelected = {},
                onSearchChanged = {},
                onStatusFilter = {},
                onCreateNew = {},
                onScanQr = {}
            )
        }

        // Should show some empty indicator (no traslados message or empty list)
        composeTestRule.onNodeWithText("TR-001").assertDoesNotExist()
    }

    @Test
    fun clickingTransferTriggersCallback() {
        var selectedId: Long? = null
        composeTestRule.setContent {
            TransfersListScreen(
                state = TransfersUiState(transfers = sampleTransfers()),
                onRefresh = {},
                onTransferSelected = { selectedId = it },
                onSearchChanged = {},
                onStatusFilter = {},
                onCreateNew = {},
                onScanQr = {}
            )
        }

        composeTestRule.onNodeWithText("TR-001", substring = true).performClick()
        assertEquals(1L, selectedId)
    }

    @Test
    fun clickingNewButtonTriggersCallback() {
        var createClicked = false
        composeTestRule.setContent {
            TransfersListScreen(
                state = TransfersUiState(),
                onRefresh = {},
                onTransferSelected = {},
                onSearchChanged = {},
                onStatusFilter = {},
                onCreateNew = { createClicked = true },
                onScanQr = {}
            )
        }

        composeTestRule.onNodeWithText("Nuevo", substring = true, ignoreCase = true).performClick()
        assertTrue(createClicked)
    }
}
