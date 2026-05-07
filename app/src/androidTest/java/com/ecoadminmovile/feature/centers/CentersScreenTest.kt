package com.ecoadminmovile.feature.centers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ecoadminmovile.core.model.CentroDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CentersScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleCenters() = listOf(
        CentroDto(id = 1, codigo = "PROD-001", nombre = "Acme Factory", tipo = "PRODUCTOR"),
        CentroDto(id = 2, codigo = "GEST-001", nombre = "EcoGestor", tipo = "GESTOR"),
        CentroDto(id = 3, codigo = "PROD-002", nombre = "Beta Industries", tipo = "PRODUCTOR")
    )

    @Test
    fun displaysTitle() {
        composeTestRule.setContent {
            CentersScreen(
                state = CentersUiState(centers = sampleCenters(), filteredCenters = sampleCenters()),
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("Centros", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysSubtitle() {
        composeTestRule.setContent {
            CentersScreen(
                state = CentersUiState(centers = sampleCenters(), filteredCenters = sampleCenters()),
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("Gestión de productores y gestores", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun displaysCenterNames() {
        composeTestRule.setContent {
            CentersScreen(
                state = CentersUiState(centers = sampleCenters(), filteredCenters = sampleCenters()),
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("Acme Factory", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("EcoGestor", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyStateWhenNoCenters() {
        composeTestRule.setContent {
            CentersScreen(
                state = CentersUiState(centers = emptyList(), filteredCenters = emptyList()),
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("Acme Factory").assertDoesNotExist()
    }

    @Test
    fun clickingCenterTriggersCallback() {
        var selectedId: Long? = null
        composeTestRule.setContent {
            CentersScreen(
                state = CentersUiState(centers = sampleCenters(), filteredCenters = sampleCenters()),
                onRefresh = {},
                onCenterSelected = { selectedId = it }
            )
        }

        composeTestRule.onNodeWithText("Acme Factory", substring = true).performClick()
        assertEquals(1L, selectedId)
    }

    @Test
    fun clickingNewButtonTriggersCallback() {
        var createClicked = false
        composeTestRule.setContent {
            CentersScreen(
                state = CentersUiState(),
                onRefresh = {},
                onCreateNew = { createClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Nuevo", substring = true, ignoreCase = true).performClick()
        assertTrue(createClicked)
    }

    @Test
    fun typeFilterChipsAreDisplayed() {
        composeTestRule.setContent {
            CentersScreen(
                state = CentersUiState(centers = sampleCenters(), filteredCenters = sampleCenters()),
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("PRODUCTOR", substring = true, ignoreCase = true)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("GESTOR", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun typeFilterClickTriggersCallback() {
        var selectedType: String? = null
        composeTestRule.setContent {
            CentersScreen(
                state = CentersUiState(centers = sampleCenters(), filteredCenters = sampleCenters()),
                onRefresh = {},
                onTypeFilter = { selectedType = it }
            )
        }

        composeTestRule.onNodeWithText("PRODUCTOR", substring = true, ignoreCase = true)
            .performClick()
        assertEquals("PRODUCTOR", selectedType)
    }
}
