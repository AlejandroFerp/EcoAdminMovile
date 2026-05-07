package com.ecoadminmovile.feature.auth

import com.ecoadminmovile.data.AuthRepository
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

/**
 * Tests del flujo de login: validación de campos, éxito y error de autenticación.
 * Usa mockk para simular AuthRepository sin necesitar Context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockAuthRepo: AuthRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockAuthRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = LoginViewModel(authRepository = mockAuthRepo)

    // --- Validation (no network needed) ---

    @Test
    fun `submit with empty email shows validation error`() = runTest {
        val vm = createViewModel()
        vm.updateEmail("")
        vm.updatePassword("password123")

        var successCalled = false
        vm.submit { successCalled = true }
        advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals("Introduce tu email de acceso.", vm.uiState.value.errorMessage)
    }

    @Test
    fun `submit with empty password shows validation error`() = runTest {
        val vm = createViewModel()
        vm.updateEmail("user@test.com")
        vm.updatePassword("")

        var successCalled = false
        vm.submit { successCalled = true }
        advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals("Introduce tu contraseña.", vm.uiState.value.errorMessage)
    }

    // --- Successful login ---

    @Test
    fun `submit with valid credentials calls onSuccess`() = runTest {
        coEvery { mockAuthRepo.login(any(), any()) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.updateEmail("admin@ecoadmin.com")
        vm.updatePassword("secret123")

        var successCalled = false
        vm.submit { successCalled = true }
        advanceUntilIdle()

        assertTrue("onSuccess should be called", successCalled)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `submit clears password on success`() = runTest {
        coEvery { mockAuthRepo.login(any(), any()) } returns Result.success(Unit)
        val vm = createViewModel()
        vm.updateEmail("admin@ecoadmin.com")
        vm.updatePassword("secret123")

        vm.submit {}
        advanceUntilIdle()

        assertEquals("", vm.uiState.value.password)
    }

    // --- Failed login ---

    @Test
    fun `submit shows error on auth failure`() = runTest {
        coEvery { mockAuthRepo.login(any(), any()) } returns
            Result.failure(RuntimeException("Credenciales incorrectas"))
        val vm = createViewModel()
        vm.updateEmail("user@test.com")
        vm.updatePassword("wrong")

        var successCalled = false
        vm.submit { successCalled = true }
        advanceUntilIdle()

        assertFalse(successCalled)
        assertFalse(vm.uiState.value.isLoading)
        assertEquals("Credenciales incorrectas", vm.uiState.value.errorMessage)
    }

    // --- State management ---

    @Test
    fun `updateEmail clears previous error`() = runTest {
        val vm = createViewModel()
        vm.updateEmail("")
        vm.updatePassword("pass")
        vm.submit {}
        advanceUntilIdle()

        vm.updateEmail("new@test.com")
        assertNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun `updatePassword clears previous error`() = runTest {
        val vm = createViewModel()
        vm.updateEmail("user@test.com")
        vm.updatePassword("")
        vm.submit {}
        advanceUntilIdle()

        vm.updatePassword("newpass")
        assertNull(vm.uiState.value.errorMessage)
    }
}
