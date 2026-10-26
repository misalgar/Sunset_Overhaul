package com.markooverhaul.sunset.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.markooverhaul.sunset.core.data.repository.AppSettings
import com.markooverhaul.sunset.core.data.repository.AuthRepository
import com.markooverhaul.sunset.core.data.repository.ReduceMotionSetting
import com.markooverhaul.sunset.core.data.repository.SettingsRepository
import com.markooverhaul.sunset.core.model.AccountSummary
import com.markooverhaul.sunset.core.model.ContentProvider
import com.markooverhaul.sunset.core.model.ProviderError
import com.markooverhaul.sunset.core.model.Session
import com.markooverhaul.sunset.core.model.VersionPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val session: Session? = null,
    val account: AccountSummary? = null,
    val accountError: Throwable? = null,
    val signedOut: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val provider: ContentProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
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
                if (session != null) loadAccount()
            }
        }
    }

    fun setAutoplayNext(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoplayNext(enabled) }
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSubtitlesEnabled(enabled) }
    }

    fun setReduceMotion(setting: ReduceMotionSetting) {
        viewModelScope.launch { settingsRepository.setReduceMotion(setting) }
    }

    fun setVersionPreference(preference: VersionPreference) {
        viewModelScope.launch { settingsRepository.setVersionPreference(preference) }
    }

    fun setLanguage(tag: String?) {
        viewModelScope.launch {
            settingsRepository.setLanguageTag(tag)
            AppCompatDelegate.setApplicationLocales(
                if (tag.isNullOrBlank()) LocaleListCompat.getEmptyLocaleList()
                else LocaleListCompat.forLanguageTags(tag),
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _state.update { it.copy(signedOut = true) }
        }
    }

    fun retryAccount() {
        loadAccount()
    }

    private fun loadAccount() {
        viewModelScope.launch {
            try {
                val account = provider.getAccountSummary()
                _state.update { it.copy(account = account, accountError = null) }
            } catch (error: ProviderError) {
                _state.update { it.copy(account = null, accountError = error) }
            } catch (error: Throwable) {
                _state.update { it.copy(account = null, accountError = error) }
            }
        }
    }
}
