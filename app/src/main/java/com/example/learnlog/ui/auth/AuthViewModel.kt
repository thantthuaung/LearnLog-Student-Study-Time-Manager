package com.example.learnlog.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.learnlog.auth.AuthManager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    val currentUser: FirebaseUser?
        get() = authManager.currentUser

    val isUserLoggedIn: Boolean
        get() = authManager.isUserLoggedIn

    fun signInWithEmail(email: String, password: String) {
        if (!validateEmail(email)) {
            _authState.value = AuthState.Error("Please enter a valid email")
            return
        }
        if (!validatePassword(password)) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }

        _loading.value = true
        viewModelScope.launch {
            val result = authManager.signInWithEmail(email, password)
            _loading.value = false

            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                }
            )
        }
    }

    fun signUpWithEmail(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank()) {
            _authState.value = AuthState.Error("Please enter your name")
            return
        }
        if (!validateEmail(email)) {
            _authState.value = AuthState.Error("Please enter a valid email")
            return
        }
        if (!validatePassword(password)) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }

        _loading.value = true
        viewModelScope.launch {
            val result = authManager.signUpWithEmail(email, password, name)
            _loading.value = false

            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Sign up failed")
                }
            )
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        _loading.value = true
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(account)
            _loading.value = false

            result.fold(
                onSuccess = { user ->
                    _authState.value = AuthState.Success(user)
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Google sign in failed")
                }
            )
        }
    }

    fun sendPasswordResetEmail(email: String) {
        if (!validateEmail(email)) {
            _authState.value = AuthState.Error("Please enter a valid email")
            return
        }

        _loading.value = true
        viewModelScope.launch {
            val result = authManager.sendPasswordResetEmail(email)
            _loading.value = false

            result.fold(
                onSuccess = {
                    _authState.value = AuthState.PasswordResetSent
                },
                onFailure = { exception ->
                    _authState.value = AuthState.Error(exception.message ?: "Failed to send reset email")
                }
            )
        }
    }

    fun signOut() {
        authManager.signOut()
        _authState.value = AuthState.SignedOut
    }

    private fun validateEmail(email: String): Boolean {
        return email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun validatePassword(password: String): Boolean {
        return password.length >= 6
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    data class Success(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
    object PasswordResetSent : AuthState()
    object SignedOut : AuthState()
}

