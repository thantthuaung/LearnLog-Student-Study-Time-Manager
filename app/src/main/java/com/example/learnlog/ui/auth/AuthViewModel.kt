package com.example.learnlog.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.R
import com.example.learnlog.auth.AuthManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthField { NAME, EMAIL, PASSWORD, CONFIRM, NONE }

sealed class AuthState {
    object Idle : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    /** [messageRes] wins over [message] when set. */
    data class Error(val field: AuthField, @StringRes val messageRes: Int = 0, val message: String? = null) : AuthState()
    object PasswordResetSent : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    fun signInWithEmail(email: String, password: String) {
        if (!validEmail(email)) return error(AuthField.EMAIL, R.string.auth_invalid_email)
        if (password.length < 6) return error(AuthField.PASSWORD, R.string.auth_short_password)
        run { authManager.signInWithEmail(email, password) }
    }

    fun signUpWithEmail(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank()) return error(AuthField.NAME, R.string.auth_name_required)
        if (!validEmail(email)) return error(AuthField.EMAIL, R.string.auth_invalid_email)
        if (password.length < 6) return error(AuthField.PASSWORD, R.string.auth_short_password)
        if (password != confirmPassword) return error(AuthField.CONFIRM, R.string.auth_passwords_mismatch)
        run { authManager.signUpWithEmail(email, password, name.trim()) }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) = run { authManager.signInWithGoogle(account) }

    fun sendPasswordResetEmail(email: String) {
        if (!validEmail(email)) return error(AuthField.EMAIL, R.string.auth_enter_email_first)
        _loading.value = true
        viewModelScope.launch {
            val result = authManager.sendPasswordResetEmail(email)
            _loading.value = false
            _authState.value = result.fold(
                onSuccess = { AuthState.PasswordResetSent },
                onFailure = { mapError(it) }
            )
        }
    }

    fun reportError(@StringRes message: Int) = error(AuthField.NONE, message)

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    private fun run(block: suspend () -> Result<FirebaseUser>) {
        _loading.value = true
        viewModelScope.launch {
            val result = block()
            _loading.value = false
            _authState.value = result.fold(
                onSuccess = { AuthState.Success(it) },
                onFailure = { mapError(it) }
            )
        }
    }

    private fun error(field: AuthField, @StringRes res: Int) {
        _authState.value = AuthState.Error(field, res)
    }

    private fun mapError(e: Throwable): AuthState.Error = when (e) {
        is FirebaseNetworkException -> AuthState.Error(AuthField.NONE, R.string.auth_offline)
        is FirebaseAuthWeakPasswordException -> AuthState.Error(AuthField.PASSWORD, R.string.auth_short_password)
        is FirebaseAuthUserCollisionException -> AuthState.Error(AuthField.EMAIL, message = "An account with this email already exists. Try signing in.")
        is FirebaseAuthInvalidUserException -> AuthState.Error(AuthField.EMAIL, message = "We couldn't find an account with that email.")
        is FirebaseAuthInvalidCredentialsException -> AuthState.Error(AuthField.PASSWORD, message = "Email or password is incorrect.")
        is FirebaseTooManyRequestsException -> AuthState.Error(AuthField.NONE, message = "Too many attempts. Please wait a moment and try again.")
        else -> AuthState.Error(AuthField.NONE, message = e.localizedMessage ?: "Something went wrong. Please try again.")
    }

    private fun validEmail(email: String) =
        email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
