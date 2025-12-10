package com.smartcbwtf.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcbwtf.mobile.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _authEvents = kotlinx.coroutines.channels.Channel<AuthEvent>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val authEvents = _authEvents.receiveAsFlow()

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
                    _authEvents.send(AuthEvent.NavigateToHome)
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

sealed class AuthEvent {
    object NavigateToHome : AuthEvent()
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
