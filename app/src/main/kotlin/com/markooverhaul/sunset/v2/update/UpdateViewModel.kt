package com.markooverhaul.sunset.v2.update

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Thin Compose-facing wrapper; [UpdateManager] is a singleton so every screen shares one state. */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val manager: UpdateManager,
) : ViewModel() {

    val state = manager.state

    fun checkIfDue() = manager.checkIfDue()

    fun checkNow() = manager.checkNow()

    fun dismiss() = manager.dismiss()

    fun install() = manager.install()

    fun openInstallSettings() = manager.openInstallSettings()
}
