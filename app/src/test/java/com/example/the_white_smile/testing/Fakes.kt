package com.example.the_white_smile.testing

import android.app.Activity
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.domain.AuthResult
import com.example.the_white_smile.domain.ConnectivityChecker
import com.example.the_white_smile.domain.EmailValidator
import com.example.the_white_smile.domain.SessionUser
import kotlinx.coroutines.CompletableDeferred

/**
 * Repositorio falso para las pruebas de los ViewModel.
 * Registra las llamadas y permite configurar el resultado de cada operacion.
 */
class FakeAuthRepository : AuthRepository {

    var loggedIn = false
    var emailVerified = false

    var user = SessionUser(
        uid = "uid-1",
        email = "ana@correo.com",
        displayName = "Ana"
    )

    var signInCalls = 0
    var lastEmail: String? = null
    var signInResult: AuthResult<SessionUser> = AuthResult.Success(user)
    var signInGate: CompletableDeferred<Unit>? = null

    var updateNameCalls = 0
    var lastUpdatedName: String? = null
    var updateNameResult: AuthResult<SessionUser>? = null

    var verificationCalls = 0
    var verificationResult: AuthResult<Unit> = AuthResult.Success(Unit)

    var passwordResetCalls = 0
    var lastResetEmail: String? = null
    var passwordResetResult: AuthResult<Unit> = AuthResult.Success(Unit)

    var reloadCalls = 0

    override val currentUser: SessionUser?
        get() = if (loggedIn) user else null

    override fun isLoggedIn(): Boolean = loggedIn

    override fun isEmailVerified(): Boolean = emailVerified

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

    override suspend fun sendPasswordReset(email: String): AuthResult<Unit> {
        passwordResetCalls++
        lastResetEmail = email
        return passwordResetResult
    }

    override suspend fun updateDisplayName(name: String): AuthResult<SessionUser> {
        updateNameCalls++
        lastUpdatedName = name
        user = user.copy(displayName = name)
        return updateNameResult ?: AuthResult.Success(user)
    }

    override suspend fun sendEmailVerification(): AuthResult<Unit> {
        verificationCalls++
        return verificationResult
    }

    override suspend fun reloadUser(): AuthResult<Unit> {
        reloadCalls++
        return AuthResult.Success(Unit)
    }

    override fun signOut() {
        loggedIn = false
    }
}

class FakeConnectivityChecker : ConnectivityChecker {

    var online = true

    override fun isOnline(): Boolean = online
}

class FakeEmailValidator : EmailValidator {

    var valid = true

    override fun isValid(email: String): Boolean = valid
}
