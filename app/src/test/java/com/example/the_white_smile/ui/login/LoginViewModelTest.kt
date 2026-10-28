package com.example.the_white_smile.ui.login

import android.app.Activity
import com.example.the_white_smile.R
import com.example.the_white_smile.core.MainDispatcherRule
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.domain.AuthResult
import com.example.the_white_smile.domain.ConnectivityChecker
import com.example.the_white_smile.domain.EmailValidator
import com.example.the_white_smile.domain.SessionUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Pruebas del [LoginViewModel] con dobles de prueba:
 * no se necesita red ni Firebase para ejecutarlas.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val connectivity = FakeConnectivityChecker()
    private val emailValidator = FakeEmailValidator()

    private fun createViewModel() = LoginViewModel(
        repository = repository,
        connectivity = connectivity,
        emailValidator = emailValidator
    )

    /** Escucha los eventos de una sola vez antes de disparar la accion. */
    private fun TestScope.collectEvents(viewModel: LoginViewModel): List<LoginEvent> {
        val events = mutableListOf<LoginEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    @Test
    fun `login con campos vacios avisa y no llama al repositorio`() = runTest {
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.login(email = "", password = "")
        advanceUntilIdle()

        assertEquals(listOf(LoginEvent.Message(R.string.login_error_empty)), events)
        assertEquals(0, repository.signInCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `login con correo invalido avisa y no llama al repositorio`() = runTest {
        emailValidator.valid = false
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.login(email = "correo-sin-arroba", password = "123456")
        advanceUntilIdle()

        assertEquals(listOf(LoginEvent.Message(R.string.login_error_invalid_email)), events)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun `login sin conexion avisa y no llama al repositorio`() = runTest {
        connectivity.online = false
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.login(email = "ana@correo.com", password = "123456")
        advanceUntilIdle()

        assertEquals(listOf(LoginEvent.Message(R.string.error_no_connection)), events)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun `login exitoso avisa el correo y apaga la carga`() = runTest {
        repository.signInResult =
            AuthResult.Success(SessionUser("uid-1", "ana@correo.com", "Ana"))
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.login(email = "  ana@correo.com  ", password = "secreto")
        advanceUntilIdle()

        assertEquals(listOf<LoginEvent>(LoginEvent.LoggedIn("ana@correo.com")), events)
        assertEquals("ana@correo.com", repository.lastEmail)
        assertEquals(1, repository.signInCalls)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `login con error de Firebase avisa la causa`() = runTest {
        val cause = IllegalStateException("fallo de prueba")
        repository.signInResult = AuthResult.Error(cause)
        val viewModel = createViewModel()
        val events = collectEvents(viewModel)

        viewModel.login(email = "ana@correo.com", password = "secreto")
        advanceUntilIdle()

        assertEquals(listOf<LoginEvent>(LoginEvent.Error(cause)), events)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `mientras el repositorio trabaja el estado queda en carga`() = runTest {
        repository.signInGate = CompletableDeferred()
        val viewModel = createViewModel()

        viewModel.login(email = "ana@correo.com", password = "secreto")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(R.string.loading_login, viewModel.uiState.value.loadingMessage)

        repository.signInGate?.complete(Unit)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `hasActiveSession delega en el repositorio`() {
        repository.loggedIn = true
        assertTrue(createViewModel().hasActiveSession())

        repository.loggedIn = false
        assertFalse(createViewModel().hasActiveSession())
    }

    // ---------- Dobles de prueba ----------

    private class FakeAuthRepository : AuthRepository {

        var loggedIn = false
        var signInCalls = 0
        var lastEmail: String? = null
        var signInResult: AuthResult<SessionUser> =
            AuthResult.Success(SessionUser("uid", "ana@correo.com", "Ana"))
        var signInGate: CompletableDeferred<Unit>? = null

        override val currentUser: SessionUser?
            get() = if (loggedIn) SessionUser("uid", "ana@correo.com", "Ana") else null

        override fun isLoggedIn(): Boolean = loggedIn

        override suspend fun signInWithEmail(
            email: String,
            password: String
        ): AuthResult<SessionUser> {
            signInCalls++
            lastEmail = email
            signInGate?.await()
            return signInResult
        }

        override suspend fun registerWithEmail(
            name: String,
            email: String,
            password: String
        ): AuthResult<SessionUser> = error("No se usa en estas pruebas")

        override suspend fun signInWithGoogleIdToken(idToken: String): AuthResult<SessionUser> =
            error("No se usa en estas pruebas")

        override suspend fun signInWithGithub(activity: Activity): AuthResult<SessionUser> =
            error("No se usa en estas pruebas")

        override suspend fun sendPasswordReset(email: String): AuthResult<Unit> =
            error("No se usa en estas pruebas")

        override fun signOut() {
            loggedIn = false
        }
    }

    private class FakeConnectivityChecker : ConnectivityChecker {

        var online = true

        override fun isOnline(): Boolean = online
    }

    private class FakeEmailValidator : EmailValidator {

        var valid = true

        override fun isValid(email: String): Boolean = valid
    }
}
