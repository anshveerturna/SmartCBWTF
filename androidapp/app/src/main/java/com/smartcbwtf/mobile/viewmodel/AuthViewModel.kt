package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeAuth()
    }

    private fun observeAuth() {
        viewModelScope.launch {
            authRepository.getAuthStateFlow().collect { token ->
                if (!token.isNullOrBlank()) {
                    _authState.value = AuthState.Authenticated
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun login(username: String, password: String) {
        // 1. Input Validation
        if (username.isBlank() && password.isBlank()) {
            _loginState.value = LoginState.Error(LoginError.EmptyUsername) // Prioritize one or emit a combined state if needed. 
            // For simplicity and per requirements, let's emit EmptyUsername first or handle both in UI if we had a list.
            // The requirement says: "If both are empty → prioritize showing both inline errors or a combined message."
            // To show both, I might need a list of errors or a specific state.
            // Let's change LoginState.Error to hold a list or specific flags.
            // Or simpler: emit a specific error that implies both, or just emit one and let the user fix it.
            // Re-reading: "If both are empty → prioritize showing both inline errors or a combined message."
            // I will define a specific error for both or use a data class for validation errors.
            // Let's stick to the sealed class structure requested but maybe add a Combined one or just handle it sequentially.
            // Actually, the cleanest way is to have the Error state hold a set of errors.
            // But the prompt asked for: sealed class LoginError { object EmptyUsername ... }
            // So I will use a list of LoginErrors in the state, or just emit one.
            // Let's try to emit a list of errors in the state.
            _loginState.value = LoginState.ValidationFailed(setOf(LoginError.EmptyUsername, LoginError.EmptyPassword))
            return
        }
        if (username.isBlank()) {
            _loginState.value = LoginState.ValidationFailed(setOf(LoginError.EmptyUsername))
            return
        }
        if (password.isBlank()) {
            _loginState.value = LoginState.ValidationFailed(setOf(LoginError.EmptyPassword))
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val success = authRepository.login(username, password)
                if (success) {
                    _loginState.value = LoginState.Success
                    _authState.value = AuthState.Authenticated
                } else {
                    // This branch might not be reached if repository throws on failure, 
                    // but if it returns false, it's likely invalid credentials.
                    _loginState.value = LoginState.Error(LoginError.InvalidCredentials)
                }
            } catch (e: Exception) {
                val error = when {
                    e.message?.contains("No internet") == true -> LoginError.NetworkError
                    e is java.io.IOException -> LoginError.NetworkError
                    e is retrofit2.HttpException && (e.code() == 401 || e.code() == 403) -> LoginError.InvalidCredentials
                    else -> LoginError.UnknownError
                }
                _loginState.value = LoginState.Error(error)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _authState.value = AuthState.Unauthenticated
        }
    }
    
    fun clearError(error: LoginError) {
        val currentState = _loginState.value
        if (currentState is LoginState.ValidationFailed) {
            val newErrors = currentState.errors - error
            if (newErrors.isEmpty()) {
                _loginState.value = LoginState.Idle
            } else {
                _loginState.value = LoginState.ValidationFailed(newErrors)
            }
        } else if (currentState is LoginState.Error && currentState.error == error) {
             _loginState.value = LoginState.Idle
        }
    }
}

sealed class LoginError {
    object EmptyUsername : LoginError()
    object EmptyPassword : LoginError()
    object InvalidCredentials : LoginError()
    object NetworkError : LoginError()
    object UnknownError : LoginError()
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class ValidationFailed(val errors: Set<LoginError>) : LoginState()
    data class Error(val error: LoginError) : LoginState()
}

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}
