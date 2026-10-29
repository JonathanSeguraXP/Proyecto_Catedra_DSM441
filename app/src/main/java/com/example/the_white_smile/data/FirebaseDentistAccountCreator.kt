package com.example.the_white_smile.data

import android.content.Context
import com.example.the_white_smile.core.awaitResult
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException

/**
 * Crea cuentas con una instancia secundaria de [FirebaseApp].
 * La sesion del administrador vive en la instancia principal, por lo que
 * crear un dentista no la modifica ni la cierra.
 */
class FirebaseDentistAccountCreator(
    context: Context
) : DentistAccountCreator {

    private val appContext = context.applicationContext

    private val secondaryApp: FirebaseApp by lazy {
        FirebaseApp.getApps(appContext).firstOrNull { it.name == SECONDARY_APP }
            ?: FirebaseApp.initializeApp(
                appContext,
                FirebaseApp.getInstance().options,
                SECONDARY_APP
            )
    }

    override suspend fun createAccount(
        displayName: String,
        email: String,
        password: String
    ): Result<PendingDentistAccount> = try {
        val auth = FirebaseAuth.getInstance(secondaryApp)
        val user = auth.createUserWithEmailAndPassword(email, password).awaitResult().user
            ?: return Result.failure(IllegalStateException("Firebase no devolvio el usuario"))

        try {
            user.updateProfile(
                UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName)
                    .build()
            ).awaitResult()
        } catch (e: CancellationException) {
            auth.signOut()
            throw e
        } catch (e: Exception) {
            // No se deja una cuenta a medias si falla la actualizacion del perfil.
            try {
                user.delete().awaitResult()
            } catch (_: Exception) {
                // Si tampoco se puede eliminar, al menos se cierra la sesion secundaria.
            }
            auth.signOut()
            throw e
        }

        Result.success(FirebasePendingDentistAccount(auth, user))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    private class FirebasePendingDentistAccount(
        private val auth: FirebaseAuth,
        private val user: FirebaseUser
    ) : PendingDentistAccount {

        override val uid: String = user.uid

        override suspend fun confirm() {
            auth.signOut()
        }

        override suspend fun discard() {
            try {
                user.delete().awaitResult()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Si no se puede eliminar, al menos se cierra la sesion secundaria.
            } finally {
                auth.signOut()
            }
        }
    }

    private companion object {
        const val SECONDARY_APP = "admin-account-creator"
    }
}
