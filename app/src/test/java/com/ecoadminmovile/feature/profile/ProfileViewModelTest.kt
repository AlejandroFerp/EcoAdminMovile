package com.ecoadminmovile.feature.profile

import com.ecoadminmovile.core.model.UsuarioPerfilDto
import com.ecoadminmovile.data.ProfileRepository
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
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockRepo: ProfileRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepo = mockk(relaxed = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = ProfileViewModel(repository = mockRepo)

    // --- Start/Cancel editing ---

    @Test
    fun `startEditing populates edit fields from profile`() {
        val profile = UsuarioPerfilDto(
            id = 1, nombre = "Juan García", email = "juan@test.com", rol = "ADMIN",
            telefono = "666111222", dni = "12345678A", cargo = "Supervisor"
        )

        val vm = createViewModel()
        vm.startEditing(profile)

        val state = vm.uiState.value
        assertTrue(state.isEditing)
        assertEquals("Juan García", state.editNombre)
        assertEquals("666111222", state.editTelefono)
        assertEquals("12345678A", state.editDni)
        assertEquals("Supervisor", state.editCargo)
    }

    @Test
    fun `startEditing with null profile does nothing`() {
        val vm = createViewModel()
        vm.startEditing(null)
        assertFalse(vm.uiState.value.isEditing)
    }

    @Test
    fun `startEditing handles null optional fields`() {
        val profile = UsuarioPerfilDto(
            id = 1, nombre = "Ana", email = "ana@test.com", rol = "USER",
            telefono = null, dni = null, cargo = null
        )

        val vm = createViewModel()
        vm.startEditing(profile)

        assertEquals("", vm.uiState.value.editTelefono)
        assertEquals("", vm.uiState.value.editDni)
        assertEquals("", vm.uiState.value.editCargo)
    }

    @Test
    fun `cancelEditing resets editing state`() {
        val vm = createViewModel()
        vm.startEditing(UsuarioPerfilDto(id = 1, nombre = "X", email = "x@t.com", rol = "R"))
        vm.cancelEditing()
        assertFalse(vm.uiState.value.isEditing)
    }

    // --- Field updates ---

    @Test
    fun `field update methods change state`() {
        val vm = createViewModel()
        vm.updateNombre("New Name")
        vm.updateTelefono("123456")
        vm.updateDni("DNI123")
        vm.updateCargo("Manager")

        val state = vm.uiState.value
        assertEquals("New Name", state.editNombre)
        assertEquals("123456", state.editTelefono)
        assertEquals("DNI123", state.editDni)
        assertEquals("Manager", state.editCargo)
    }

    // --- Save profile ---

    @Test
    fun `saveProfile validates nombre is not blank`() = runTest {
        val vm = createViewModel()
        vm.updateNombre("")

        var called = false
        vm.saveProfile { called = true }
        advanceUntilIdle()

        assertFalse(called)
        assertEquals("El nombre es obligatorio", vm.uiState.value.errorMessage)
    }

    @Test
    fun `saveProfile validates nombre is not only whitespace`() = runTest {
        val vm = createViewModel()
        vm.updateNombre("   ")

        vm.saveProfile {}
        advanceUntilIdle()

        assertEquals("El nombre es obligatorio", vm.uiState.value.errorMessage)
    }

    @Test
    fun `saveProfile calls repository and onProfileUpdated on success`() = runTest {
        val updatedProfile = UsuarioPerfilDto(
            id = 1, nombre = "Updated", email = "e@t.com", rol = "USER"
        )
        coEvery { mockRepo.updateProfile(any()) } returns Result.success(updatedProfile)

        val vm = createViewModel()
        vm.updateNombre("Updated")

        var receivedProfile: UsuarioPerfilDto? = null
        vm.saveProfile { receivedProfile = it }
        advanceUntilIdle()

        assertEquals("Updated", receivedProfile?.nombre)
        assertFalse(vm.uiState.value.isEditing)
        assertEquals("Perfil actualizado", vm.uiState.value.saveSuccessMessage)
    }

    @Test
    fun `saveProfile shows error on failure`() = runTest {
        coEvery { mockRepo.updateProfile(any()) } returns
            Result.failure(RuntimeException("Server error"))

        val vm = createViewModel()
        vm.updateNombre("Valid Name")
        vm.saveProfile {}
        advanceUntilIdle()

        assertEquals("Server error", vm.uiState.value.errorMessage)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `saveProfile trims fields and sends null for blank optionals`() = runTest {
        coEvery { mockRepo.updateProfile(any()) } returns Result.success(
            UsuarioPerfilDto(id = 1, nombre = "Clean", email = "e@t.com", rol = "R")
        )

        val vm = createViewModel()
        vm.updateNombre("  Clean  ")
        vm.updateTelefono("  ")
        vm.updateDni("")
        vm.updateCargo("  Manager  ")
        vm.saveProfile {}
        advanceUntilIdle()

        coVerify {
            mockRepo.updateProfile(match {
                it.nombre == "Clean" &&
                    it.telefono == null &&
                    it.dni == null &&
                    it.cargo == "Manager"
            })
        }
    }

    // --- Clear success message ---

    @Test
    fun `clearSuccessMessage nullifies message`() = runTest {
        coEvery { mockRepo.updateProfile(any()) } returns Result.success(
            UsuarioPerfilDto(id = 1, nombre = "X", email = "x@t.com", rol = "R")
        )
        val vm = createViewModel()
        vm.updateNombre("X")
        vm.saveProfile {}
        advanceUntilIdle()

        vm.clearSuccessMessage()
        assertNull(vm.uiState.value.saveSuccessMessage)
    }

    // --- Password change ---

    @Test
    fun `showPasswordDialog initializes dialog state`() {
        val vm = createViewModel()
        vm.showPasswordDialog()

        val state = vm.uiState.value
        assertTrue(state.showPasswordDialog)
        assertEquals("", state.currentPassword)
        assertEquals("", state.newPassword)
        assertEquals("", state.confirmPassword)
    }

    @Test
    fun `hidePasswordDialog closes dialog`() {
        val vm = createViewModel()
        vm.showPasswordDialog()
        vm.hidePasswordDialog()
        assertFalse(vm.uiState.value.showPasswordDialog)
    }

    @Test
    fun `changePassword validates current password not blank`() = runTest {
        val vm = createViewModel()
        vm.showPasswordDialog()
        vm.updateCurrentPassword("")
        vm.updateNewPassword("newpass123")
        vm.updateConfirmPassword("newpass123")

        vm.changePassword()
        advanceUntilIdle()

        assertEquals("Introduce tu contraseña actual", vm.uiState.value.passwordError)
    }

    @Test
    fun `changePassword validates new password min length`() = runTest {
        val vm = createViewModel()
        vm.showPasswordDialog()
        vm.updateCurrentPassword("current")
        vm.updateNewPassword("12345") // 5 chars, need 6
        vm.updateConfirmPassword("12345")

        vm.changePassword()
        advanceUntilIdle()

        assertEquals("La nueva contraseña debe tener al menos 6 caracteres", vm.uiState.value.passwordError)
    }

    @Test
    fun `changePassword validates passwords match`() = runTest {
        val vm = createViewModel()
        vm.showPasswordDialog()
        vm.updateCurrentPassword("current")
        vm.updateNewPassword("newpass123")
        vm.updateConfirmPassword("different")

        vm.changePassword()
        advanceUntilIdle()

        assertEquals("Las contraseñas no coinciden", vm.uiState.value.passwordError)
    }

    @Test
    fun `changePassword succeeds and closes dialog`() = runTest {
        coEvery { mockRepo.changePassword(any()) } returns Result.success(Unit)

        val vm = createViewModel()
        vm.showPasswordDialog()
        vm.updateCurrentPassword("oldpass")
        vm.updateNewPassword("newpass123")
        vm.updateConfirmPassword("newpass123")

        vm.changePassword()
        advanceUntilIdle()

        assertFalse(vm.uiState.value.showPasswordDialog)
        assertEquals("Contraseña actualizada", vm.uiState.value.saveSuccessMessage)
    }

    @Test
    fun `changePassword shows error on failure`() = runTest {
        coEvery { mockRepo.changePassword(any()) } returns
            Result.failure(RuntimeException("Contraseña actual incorrecta"))

        val vm = createViewModel()
        vm.showPasswordDialog()
        vm.updateCurrentPassword("wrong")
        vm.updateNewPassword("newpass123")
        vm.updateConfirmPassword("newpass123")

        vm.changePassword()
        advanceUntilIdle()

        assertEquals("Contraseña actual incorrecta", vm.uiState.value.passwordError)
        assertFalse(vm.uiState.value.isChangingPassword)
    }

    @Test
    fun `password field updates reflect in state`() {
        val vm = createViewModel()
        vm.updateCurrentPassword("c")
        vm.updateNewPassword("n")
        vm.updateConfirmPassword("x")

        assertEquals("c", vm.uiState.value.currentPassword)
        assertEquals("n", vm.uiState.value.newPassword)
        assertEquals("x", vm.uiState.value.confirmPassword)
    }
}
