package com.markooverhaul.sunset.v2

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.markooverhaul.sunset.core.data.repository.ReduceMotionSetting
import com.markooverhaul.sunset.core.designsystem.theme.SunsetTheme
import com.markooverhaul.sunset.core.designsystem.tokens.SunsetColors
import com.markooverhaul.sunset.feature.account.LoginRoute
import com.markooverhaul.sunset.v2.ui.MainShell
import com.markooverhaul.sunset.v2.ui.ServiceNotConfiguredScreen
import com.markooverhaul.sunset.v2.ui.SplashScreen

@Composable
fun SunsetApp(
    isTelevision: Boolean,
    viewModel: AppViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val systemReduceMotion = rememberSystemReduceMotion(context)
    val reduceMotion = when (state.settings.reduceMotion) {
        ReduceMotionSetting.ON -> true
        ReduceMotionSetting.OFF -> false
        ReduceMotionSetting.SYSTEM -> systemReduceMotion
    }

    SunsetTheme(
        isTelevision = isTelevision,
        reduceMotion = reduceMotion,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = SunsetColors.BgBase,
        ) {
            when (state.startup) {
                StartupState.Loading -> SplashScreen(demoMode = BuildConfig.DEBUG)
                StartupState.ServiceNotConfigured -> ServiceNotConfiguredScreen()
                StartupState.NeedsLogin -> LoginRoute(
                    onSignedIn = viewModel::onSignedIn,
                    demoMode = BuildConfig.DEBUG,
                )

                StartupState.Ready -> MainShell(demoMode = BuildConfig.DEBUG)
            }
        }
    }
}

/** Reads the platform animation scale so "Follow system" still honours accessibility settings. */
@Composable
private fun rememberSystemReduceMotion(context: Context): Boolean = remember(context) {
    runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }.getOrDefault(false)
}
