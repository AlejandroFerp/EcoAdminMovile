package com.ecoadminmovile.feature.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ecoadminmovile.core.model.EstadisticasDto
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysWelcomeSection() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardUiState(userName = "Juan"),
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("Juan", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardUiState(errorMessage = "Sin conexión"),
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("Sin conexión", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysStatisticsWhenLoaded() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardUiState(
                    stats = EstadisticasDto(
                        totalTraslados = 42,
                        trasladosEnTransporte = 5,
                        trasladosCompletados = 30,
                        trasladosBorrador = 7
                    )
                ),
                onRefresh = {}
            )
        }

        composeTestRule.onNodeWithText("42", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysEmptyStateWhenNoStats() {
        composeTestRule.setContent {
            DashboardScreen(
                state = DashboardUiState(stats = null, isLoading = false),
                onRefresh = {}
            )
        }

        // Should not crash with null stats
        composeTestRule.onNodeWithText("42").assertDoesNotExist()
    }
}
