package com.example.the_white_smile.data

import android.app.Activity
import com.example.the_white_smile.core.awaitResult
import com.example.the_white_smile.domain.AuthResult
import com.example.the_white_smile.domain.SessionUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException

/** Implementacion de [AuthRepository] sobre Firebase Authentication. */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth
) : AuthRepository {

    override val currentUser: SessionUser?
        get() = auth.currentUser?.toSessionUser()

    override fun isLoggedIn(): Boolean = auth.currentUser != null

    override fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    override suspend fun signInWithEmail(
        email: String,
        password: String
    ): AuthResult<SessionUser> = runAuth {
        val user = auth.signInWithEmailAndPassword(email, password)
            .awaitResult()
            .user
            ?: throw IllegalStateException("No se pudo obtener el usuario")
        user.toSessionUser()
    }

    override suspend fun registerWithEmail(
        name: String,
        email: String,
        password: String
    ): AuthResult<SessionUser> = runAuth {
        val cleanName = name.trim()
        val user = auth.createUserWithEmailAndPassword(email, password)
            .awaitResult()
            .user
            ?: throw IllegalStateException("No se pudo crear el usuario")

        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName)
                .build()
        ).awaitResult()

        user.toSessionUser(cleanName)
    }

    override suspend fun signInWithGoogleIdToken(
        idToken: String
    ): AuthResult<SessionUser> = runAuth {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val user = auth.signInWithCredential(credential)
            .awaitResult()
            .user
            ?: throw IllegalStateException("No se pudo obtener el usuario")
        user.toSessionUser()
    }

    /**
     * El navegador del flujo OAuth lo abre la Activity, por eso se recibe como
     * parametro. La operacion termina cuando el usuario vuelve a la aplicacion.
     */
    override suspend fun signInWithGithub(
        activity: Activity
    ): AuthResult<SessionUser> = runAuth {
        val provider = OAuthProvider.newBuilder(GITHUB_PROVIDER)
            .setScopes(listOf("user:email"))
            .build()
        val user = auth.startActivityForSignInWithProvider(activity, provider)
            .awaitResult()
            .user
            ?: throw IllegalStateException("No se pudo obtener el usuario")
        user.toSessionUser()
    }

    override suspend fun sendPasswordReset(email: String): AuthResult<Unit> = runAuth {
        auth.sendPasswordResetEmail(email).awaitResult()
    }

    override suspend fun updateDisplayName(name: String): AuthResult<SessionUser> = runAuth {
        val cleanName = name.trim()
        val user = auth.currentUser
            ?: throw IllegalStateException("No hay una sesion activa")
        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(cleanName)
                .build()
        ).awaitResult()
        user.toSessionUser(cleanName)
    }

    override suspend fun sendEmailVerification(): AuthResult<Unit> = runAuth {
        val user = auth.currentUser
            ?: throw IllegalStateException("No hay una sesion activa")
        user.sendEmailVerification().awaitResult()
    }

    override suspend fun reloadUser(): AuthResult<Unit> = runAuth {
        val user = auth.currentUser
            ?: throw IllegalStateException("No hay una sesion activa")
        user.reload().awaitResult()
    }

    override fun signOut() {
        auth.signOut()
    }

    /** Ejecuta el bloque y traduce cualquier fallo a [AuthResult.Error]. */
    private inline fun <T> runAuth(block: () -> T): AuthResult<T> = try {
        AuthResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AuthResult.Error(e)
    }

    private fun FirebaseUser.toSessionUser(name: String? = displayName): SessionUser =
        SessionUser(
            uid = uid,
            email = email,
            displayName = name
        )

    private companion object {
        const val GITHUB_PROVIDER = "github.com"
    }
}
