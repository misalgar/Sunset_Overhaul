package com.markooverhaul.sunset.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.data.repository.AppSettings
import com.markooverhaul.sunset.core.data.repository.AuthRepository
import com.markooverhaul.sunset.core.data.repository.SettingsRepository
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ProviderError
import com.markooverhaul.sunset.core.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface StartupState {
    data object Loading : StartupState
    data object Ready : StartupState
    data object NeedsLogin : StartupState
    data object ServiceNotConfigured : StartupState
}

data class AppUiState(
    val startup: StartupState = StartupState.Loading,
    val settings: AppSettings = AppSettings(),
    val session: Session? = null,
)

/**
 * Application bootstrap and global preferences.
 *
 * Release builds bind the live provider, which is "not configured" until the viewer signs in;
 * that state now leads to the login screen. [StartupState.ServiceNotConfigured] is reserved for a
 * provider that cannot be used at all or reports it during session restore.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val provider: ContentProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(AppUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            authRepository.session.collect { session ->
                _state.update { it.copy(session = session) }
            }
        }
        bootstrap()
    }

    fun bootstrap() {
        viewModelScope.launch {
            _state.update { it.copy(startup = StartupState.Loading) }
            try {
                restoreOrStart()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: ProviderError.ServiceNotConfigured) {
                _state.update { it.copy(startup = StartupState.ServiceNotConfigured) }
            } catch (_: Throwable) {
                _state.update { it.copy(startup = StartupState.NeedsLogin) }
            }
        }
    }

    fun onSignedIn() {
        _state.update { it.copy(startup = StartupState.Ready) }
    }

    private suspend fun restoreOrStart() {
        val stored = authRepository.session.first()
        if (stored != null) {
            val restored = runCatching { provider.restoreSession(stored.token) }.getOrNull()
            if (restored != null) {
                _state.update { it.copy(startup = StartupState.Ready) }
                return
            }
            authRepository.signOut()
        }

        // No stored session: show the login screen. Debug builds keep the offline demo one tap
        // away ("Explore the demo"), but auto-entering it would hide the server field on every
        // launch and make a real panel impossible to reach after signing out.
        _state.update { it.copy(startup = StartupState.NeedsLogin) }
    }
}
