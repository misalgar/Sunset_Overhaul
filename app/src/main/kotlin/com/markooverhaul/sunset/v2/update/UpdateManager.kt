package com.markooverhaul.sunset.v2.update

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.markooverhaul.sunset.v2.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Checks `latest.json`, downloads the APK, verifies its SHA-256 and signing certificate, then hands
 * it to the system installer. No update is ever installed silently: Fire OS always shows its own
 * confirmation prompt.
 */
@Singleton
class UpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: OkHttpClient,
    private val json: Json,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(UpdateState())
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private var checkJob: Job? = null

    /** Daily silent check, used when the signed-in shell becomes visible. */
    fun checkIfDue() {
        if (BuildConfig.DEBUG) return
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (System.currentTimeMillis() - last < CHECK_INTERVAL_MS) return
        check(manual = false)
    }

    /** Explicit "Check for updates" from Settings. */
    fun checkNow() {
        if (BuildConfig.DEBUG) {
            _state.update { it.copy(checking = false, error = null, message = UpdateMessage.DebugBuild) }
            return
        }
        check(manual = true)
    }

    fun dismiss() {
        _state.update {
            it.copy(
                available = null,
                needsPermission = false,
                error = null,
                message = null,
                downloading = false,
                progress = 0f,
            )
        }
    }

    /**
     * Downloads and installs the offered update. Reuses the manifest currently on screen so a Retry
     * after a failed download or the "Install unknown apps" settings trip starts over cleanly.
     */
    fun install() {
        val manifest = _state.value.available ?: return
        if (_state.value.downloading) return
        scope.launch {
            _state.update {
                it.copy(
                    downloading = true,
                    progress = 0f,
                    error = null,
                    needsPermission = false,
                )
            }
            try {
                val file = download(manifest) { progress ->
                    _state.update { it.copy(progress = progress) }
                }
                verify(file, manifest)
                _state.update { it.copy(downloading = false, progress = 1f) }
                launchInstaller(file)
                _state.update { it.copy(available = null, needsPermission = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: InstallPermissionRequired) {
                _state.update { it.copy(downloading = false, needsPermission = true) }
                openInstallSettings()
            } catch (error: UpdateException) {
                _state.update { it.copy(downloading = false, error = error.reason) }
            } catch (error: Throwable) {
                Log.w(TAG, "Update download failed", error)
                _state.update { it.copy(downloading = false, error = UpdateError.DownloadFailed) }
            }
        }
    }

    fun openInstallSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { Log.w(TAG, "Could not open unknown-app settings", it) }
    }

    private fun check(manual: Boolean) {
        if (checkJob?.isActive == true) return
        checkJob = scope.launch {
            _state.update { it.copy(checking = true, error = null, message = null) }
            try {
                val manifest = fetchManifest()
                prefs.edit { putLong(KEY_LAST_CHECK, System.currentTimeMillis()) }
                if (manifest.isNewerThan(installedVersionCode())) {
                    _state.update { it.copy(checking = false, available = manifest) }
                } else {
                    _state.update {
                        it.copy(
                            checking = false,
                            available = null,
                            message = if (manual) UpdateMessage.UpToDate else null,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Log.w(TAG, "Update check failed", error)
                if (manual) {
                    _state.update { it.copy(checking = false, error = UpdateError.CheckFailed) }
                } else {
                    _state.update { it.copy(checking = false) }
                }
            } finally {
                checkJob = null
            }
        }
    }

    private suspend fun fetchManifest(): UpdateManifest = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(BuildConfig.OTA_MANIFEST_URL)
            .cacheControl(CacheControl.Builder().noCache().build())
            .header("Cache-Control", "no-cache")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw UpdateException(UpdateError.CheckFailed)
            }
            val body = response.body?.string().orEmpty()
            json.decodeFromString<UpdateManifest>(body)
        }
    }

    private suspend fun download(
        manifest: UpdateManifest,
        onProgress: (Float) -> Unit,
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val file = File(dir, "sunset-${manifest.versionCode}.apk")
        if (file.exists()) file.delete()
        val request = Request.Builder().url(manifest.apkUrl).get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw UpdateException(UpdateError.DownloadFailed)
            val body = response.body ?: throw UpdateException(UpdateError.DownloadFailed)
            val total = body.contentLength()
            var written = 0L
            var lastReported = -1f
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0L) {
                            val progress = (written.toFloat() / total).coerceIn(0f, 1f)
                            if (progress - lastReported >= 0.01f) {
                                lastReported = progress
                                onProgress(progress)
                            }
                        }
                    }
                }
            }
        }
        file
    }

    private fun verify(file: File, manifest: UpdateManifest) {
        if (manifest.sha256.isNotBlank() &&
            !file.sha256().equals(manifest.sha256.trim(), ignoreCase = true)
        ) {
            file.delete()
            throw UpdateException(UpdateError.HashMismatch)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: run {
                file.delete()
                throw UpdateException(UpdateError.InvalidPackage)
            }
        if (archive.packageName != context.packageName ||
            PackageInfoCompat.getLongVersionCode(archive) != manifest.versionCode.toLong()
        ) {
            file.delete()
            throw UpdateException(UpdateError.InvalidPackage)
        }

        val archiveSigner = signerDigest(archive) ?: run {
            file.delete()
            throw UpdateException(UpdateError.SignatureMismatch)
        }
        val installedSigner = runCatching {
            signerDigest(context.packageManager.getPackageInfo(context.packageName, flags))
        }.getOrNull() ?: throw UpdateException(UpdateError.SignatureMismatch)
        if (!archiveSigner.contentEquals(installedSigner)) {
            file.delete()
            throw UpdateException(UpdateError.SignatureMismatch)
        }
    }

    private fun signerDigest(info: PackageInfo): ByteArray? =
        runCatching {
            val signature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()
            }
            signature?.toByteArray()
        }.getOrNull()

    private fun installedVersionCode(): Long = runCatching {
        PackageInfoCompat.getLongVersionCode(
            context.packageManager.getPackageInfo(context.packageName, 0),
        )
    }.getOrDefault(0L)

    private fun launchInstaller(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            throw InstallPermissionRequired()
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newRawUri("", uri)
        }
        context.startActivity(intent)
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    private class UpdateException(val reason: UpdateError) : Exception()

    private class InstallPermissionRequired : Exception()

    private companion object {
        const val TAG = "SunsetUpdate"
        const val PREFS = "sunset_update"
        const val KEY_LAST_CHECK = "last_check"
        const val CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
    }
}
