package com.example.the_white_smile.testing

import android.app.Activity
import com.example.the_white_smile.data.AppointmentRepository
import com.example.the_white_smile.data.AuthRepository
import com.example.the_white_smile.data.DentistRepository
import com.example.the_white_smile.data.TreatmentRepository
import com.example.the_white_smile.data.UserRepository
import com.example.the_white_smile.domain.Appointment
import com.example.the_white_smile.domain.AuthResult
import com.example.the_white_smile.domain.ConnectivityChecker
import com.example.the_white_smile.domain.Dentist
import com.example.the_white_smile.domain.EmailValidator
import com.example.the_white_smile.domain.SessionUser
import com.example.the_white_smile.domain.Treatment
import com.example.the_white_smile.domain.UserProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

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

/**
 * Repositorio falso de citas: expone un flujo controlable y registra
 * las llamadas de creacion y cancelacion.
 */
class FakeAppointmentRepository : AppointmentRepository {

    val appointments = MutableStateFlow<List<Appointment>>(emptyList())

    val busySlots = MutableStateFlow<Set<Long>>(emptySet())

    var observeError: Throwable? = null
    var busyError: Throwable? = null

    var busyCalls = 0
    var lastBusyDentistId: String? = null
    var lastBusyDayStart: Long? = null
    var lastBusyDayEnd: Long? = null

    var createCalls = 0
    var lastService: String? = null
    var lastStartAt: Long? = null
    var lastTreatmentId: String? = null
    var lastDurationMinutes: Int? = null
    var lastDentistId: String? = null
    var lastDentistName: String? = null
    var createResult: Result<Unit> = Result.success(Unit)

    var cancelCalls = 0
    var lastCancelledId: String? = null
    var lastCancelledAppointment: Appointment? = null
    var cancelResult: Result<Unit> = Result.success(Unit)

    override fun observeAppointments(): Flow<List<Appointment>> {
        val error = observeError
        return if (error != null) flow { throw error } else appointments.asStateFlow()
    }

    override fun observeBusySlots(
        dentistId: String,
        dayStartAt: Long,
        dayEndAt: Long
    ): Flow<Set<Long>> {
        busyCalls++
        lastBusyDentistId = dentistId
        lastBusyDayStart = dayStartAt
        lastBusyDayEnd = dayEndAt
        val error = busyError
        return if (error != null) flow { throw error } else busySlots.asStateFlow()
    }

    override suspend fun createAppointment(
        service: String,
        startAt: Long,
        treatmentId: String,
        durationMinutes: Int,
        dentistId: String,
        dentistName: String
    ): Result<Unit> {
        createCalls++
        lastService = service
        lastStartAt = startAt
        lastTreatmentId = treatmentId
        lastDurationMinutes = durationMinutes
        lastDentistId = dentistId
        lastDentistName = dentistName
        return createResult
    }

    override suspend fun cancelAppointment(appointment: Appointment): Result<Unit> {
        cancelCalls++
        lastCancelledId = appointment.id
        lastCancelledAppointment = appointment
        return cancelResult
    }
}

/** Repositorio falso del catalogo de tratamientos. */
class FakeTreatmentRepository(
    private val treatments: List<Treatment> = emptyList()
) : TreatmentRepository {

    override fun getTreatments(): List<Treatment> = treatments

    override fun getTreatment(id: String): Treatment? =
        treatments.firstOrNull { it.id == id }
}

/** Repositorio falso de perfiles y roles de usuario. */
class FakeUserRepository : UserRepository {

    var profile: UserProfile? = null
    var error: Throwable? = null
    var calls = 0

    override suspend fun getProfile(uid: String): Result<UserProfile?> {
        calls++
        error?.let { return Result.failure(it) }
        return Result.success(profile)
    }
}

/**
 * Repositorio falso de dentistas: expone un flujo controlable y registra
 * las cuentas creadas y los cambios de estado.
 */
class FakeDentistRepository : DentistRepository {

    val dentists = MutableStateFlow<List<Dentist>>(emptyList())

    var observeError: Throwable? = null

    var createCalls = 0
    var lastName: String? = null
    var lastEmail: String? = null
    var lastPassword: String? = null
    var lastSpecialty: String? = null
    var lastWorkDays: Set<Int>? = null
    var lastStartMinute: Int? = null
    var lastEndMinute: Int? = null
    var createResult: Result<Dentist>? = null

    var setActiveCalls = 0
    var lastActiveId: String? = null
    var lastActiveValue: Boolean? = null
    var setActiveResult: Result<Unit> = Result.success(Unit)

    var updateScheduleCalls = 0
    var lastScheduleId: String? = null
    var lastScheduleWorkDays: Set<Int>? = null
    var lastScheduleStartMinute: Int? = null
    var lastScheduleEndMinute: Int? = null
    var updateScheduleResult: Result<Unit> = Result.success(Unit)

    override fun observeDentists(): Flow<List<Dentist>> {
        val error = observeError
        return if (error != null) flow { throw error } else dentists.asStateFlow()
    }

    override suspend fun createDentist(
        displayName: String,
        email: String,
        password: String,
        specialty: String,
        workDays: Set<Int>,
        startMinute: Int,
        endMinute: Int
    ): Result<Dentist> {
        createCalls++
        lastName = displayName
        lastEmail = email
        lastPassword = password
        lastSpecialty = specialty
        lastWorkDays = workDays
        lastStartMinute = startMinute
        lastEndMinute = endMinute

        createResult?.let { return it }

        val dentist = Dentist(
            id = "dentist-$createCalls",
            displayName = displayName,
            email = email,
            specialty = specialty,
            workDays = workDays,
            startMinute = startMinute,
            endMinute = endMinute
        )
        dentists.value = dentists.value + dentist
        return Result.success(dentist)
    }

    override suspend fun setActive(dentistId: String, active: Boolean): Result<Unit> {
        setActiveCalls++
        lastActiveId = dentistId
        lastActiveValue = active

        setActiveResult
            .onSuccess {
                dentists.value = dentists.value.map {
                    if (it.id == dentistId) it.copy(active = active) else it
                }
            }
        return setActiveResult
    }

    override suspend fun updateSchedule(
        dentistId: String,
        workDays: Set<Int>,
        startMinute: Int,
        endMinute: Int
    ): Result<Unit> {
        updateScheduleCalls++
        lastScheduleId = dentistId
        lastScheduleWorkDays = workDays
        lastScheduleStartMinute = startMinute
        lastScheduleEndMinute = endMinute

        updateScheduleResult
            .onSuccess {
                dentists.value = dentists.value.map {
                    if (it.id == dentistId) {
                        it.copy(
                            workDays = workDays,
                            startMinute = startMinute,
                            endMinute = endMinute
                        )
                    } else {
                        it
                    }
                }
            }
        return updateScheduleResult
    }
}
