package com.markooverhaul.sunset.v2.update

import kotlinx.serialization.Serializable

/** `latest.json` published alongside the release APKs. */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String = "",
    val apkUrl: String,
    val sha256: String = "",
    val notes: String = "",
    val mandatory: Boolean = false,
)

fun UpdateManifest.isNewerThan(installedVersionCode: Long): Boolean =
    versionCode > installedVersionCode

enum class UpdateError {
    CheckFailed,
    DownloadFailed,
    HashMismatch,
    SignatureMismatch,
    InvalidPackage,
    InstallFailed,
}

enum class UpdateMessage {
    UpToDate,
    DebugBuild,
}

data class UpdateState(
    val checking: Boolean = false,
    val available: UpdateManifest? = null,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val needsPermission: Boolean = false,
    val error: UpdateError? = null,
    val message: UpdateMessage? = null,
)
