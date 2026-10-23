package com.markooverhaul.sunset.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.markooverhaul.sunset.core.model.ProviderError
import com.markooverhaul.sunset.core.ui.R

/**
 * Maps typed provider failures to human copy. Raw provider messages, URLs and tokens are never
 * rendered. Every branch resolves through the string catalogue so English and Spanish stay in sync.
 */
@Composable
fun providerErrorText(error: Throwable?): String = when (error) {
    is ProviderError.InvalidCredentials -> stringResource(R.string.login_error_invalid)
    is ProviderError.SessionExpired -> stringResource(R.string.error_session_expired)
    is ProviderError.AccountExpired -> stringResource(R.string.error_account_expired)
    is ProviderError.DeviceNotActive -> stringResource(R.string.error_device_not_active)
    is ProviderError.ConnectionLimit -> stringResource(R.string.error_connection_limit)
    is ProviderError.Maintenance -> stringResource(R.string.error_maintenance)
    is ProviderError.Network -> stringResource(R.string.error_network_body)
    is ProviderError.Server -> stringResource(R.string.error_generic_body)
    is ProviderError.RateLimited -> stringResource(R.string.error_rate_limited)
    is ProviderError.NotFound -> stringResource(R.string.not_found_body)
    is ProviderError.PlaybackUnsupported -> stringResource(R.string.error_playback_unsupported)
    is ProviderError.PlaybackAuth -> stringResource(R.string.error_playback_auth)
    is ProviderError.ParentalLocked -> stringResource(R.string.guide_locked_programme)
    is ProviderError.ServiceNotConfigured -> stringResource(R.string.service_not_configured_body)
    is ProviderError.Unknown -> stringResource(R.string.error_unknown)
    null -> stringResource(R.string.error_unknown)
    else -> stringResource(R.string.error_unknown)
}
