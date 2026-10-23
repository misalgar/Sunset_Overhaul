package com.markooverhaul.sunset.v2

import android.content.res.Configuration
import android.os.Bundle
import android.app.UiModeManager
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Extends [AppCompatActivity] so per-app locales (English/Spanish) can be
 * applied through [androidx.appcompat.app.AppCompatDelegate.setApplicationLocales].
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val isTelevision = remember(context) {
                val manager = context.getSystemService(UiModeManager::class.java)
                manager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
            }
            SunsetApp(isTelevision = isTelevision)
        }
    }
}
