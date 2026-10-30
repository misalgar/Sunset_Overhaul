package com.markooverhaul.sunset.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.data.repository.AuthRepository
import com.markooverhaul.sunset.core.model.ProviderError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val rememberMe: Boolean = true,
    val passwordVisible: Boolean = false,
    val serverUrl: String = "",
    val isSubmitting: Boolean = false,
    val error: Throwable? = null,
    val signedIn: Boolean = false,
) {
    val canSubmit: Boolean
        get() = serverUrl.isNotBlank() &&
            username.isNotBlank() &&
            password.isNotBlank() &&
            !isSubmitting

    val serviceNotConfigured: Boolean
        get() = error is ProviderError.ServiceNotConfigured
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val remembered = authRepository.rememberedUsername.firstOrNull()
            if (!remembered.isNullOrBlank()) {
                _state.update { it.copy(username = remembered, rememberMe = true) }
            }
        }
        viewModelScope.launch {
            val server = authRepository.lastServerUrl.firstOrNull()
            if (!server.isNullOrBlank()) {
                _state.update { it.copy(serverUrl = server) }
            }
        }
    }

    fun onServerChange(value: String) {
        _state.update { it.copy(serverUrl = value, error = null) }
    }

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun toggleRememberMe() {
        _state.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun togglePasswordVisible() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        if (!current.canSubmit) {
            _state.update { it.copy(error = ProviderError.InvalidCredentials) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                authRepository.login(
                    serverUrl = current.serverUrl,
                    username = current.username,
                    password = current.password,
                    rememberUsername = current.rememberMe,
                )
                _state.update { it.copy(isSubmitting = false, signedIn = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(isSubmitting = false, error = error) }
            }
        }
    }

    /** Debug-only shortcut into the bundled offline catalogue. */
    fun startDemo() {
        if (_state.value.isSubmitting) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            try {
                authRepository.startDemoSession()
                _state.update { it.copy(isSubmitting = false, signedIn = true) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(isSubmitting = false, error = error) }
            }
        }
    }
}
