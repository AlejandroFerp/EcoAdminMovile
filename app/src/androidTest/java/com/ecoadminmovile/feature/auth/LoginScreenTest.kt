package com.ecoadminmovile.feature.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displaysEmailAndPasswordFields() {
        composeTestRule.setContent {
            LoginScreen(
                state = LoginUiState(),
                onEmailChanged = {},
                onPasswordChanged = {},
                onLoginClick = {}
            )
        }

        composeTestRule.onNodeWithText("Email", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Contraseña", substring = true).assertIsDisplayed()
    }

    @Test
    fun loginScreen_displaysErrorMessage() {
        composeTestRule.setContent {
            LoginScreen(
                state = LoginUiState(errorMessage = "Credenciales incorrectas"),
                onEmailChanged = {},
                onPasswordChanged = {},
                onLoginClick = {}
            )
        }

        composeTestRule.onNodeWithText("Credenciales incorrectas").assertIsDisplayed()
    }

    @Test
    fun loginScreen_loginButtonTriggersCallback() {
        var clicked = false
        composeTestRule.setContent {
            LoginScreen(
                state = LoginUiState(email = "user@test.com", password = "pass"),
                onEmailChanged = {},
                onPasswordChanged = {},
                onLoginClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("Iniciar sesión", substring = true, ignoreCase = true)
            .performClick()

        assertTrue(clicked)
    }
}
