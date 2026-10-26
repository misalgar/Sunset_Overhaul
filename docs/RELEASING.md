# Releasing Sunset and publishing OTA updates

The app is sideloaded. A release APK is signed locally and a public GitHub repository carries the
update manifest and APKs. Installed copies check that manifest daily and can update themselves
through the Fire TV package installer.

## One-time setup (already done)

| Item | Value |
| --- | --- |
| Release keystore | `C:\Users\mko_r\.sunset\sunset-release.jks` |
| Key alias | `sunset` |
| Signing properties | untracked `local.properties` (`sunset.signing.*`) |
| Update manifest URL | `https://raw.githubusercontent.com/misalgar/sunset_overhaul-releases/main/latest.json` |
| Releases repo | `https://github.com/misalgar/sunset_overhaul-releases` (public, artifacts only) |
| App id | `com.markooverhaul.sunset.v2` (debug app is `.debug`) |

**Back up the keystore and the passwords in `local.properties`.** Every future update must be signed
with this key; without it, existing installs cannot be updated.

## Build a release

1. Bump both values in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 2
   versionName = "0.2.0"
   ```
2. Run:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\build-release.ps1 -Notes "What changed"
   ```
   The script:
   - builds `:app:assembleRelease`,
   - verifies the APK signature,
   - copies it to `build\release-artifacts\sunset-<version>.apk`,
   - computes SHA-256,
   - rewrites `latest.json` in the releases repo checkout.

The APK is **not** committed to git. It is uploaded as a GitHub release asset.

## Publish an update

1. Open the releases repo on GitHub → **Releases** → **Draft a new release**.
2. Tag: `v<versionName>` (for example `v0.1.0`).
3. Upload `build\release-artifacts\sunset-<version>.apk`.
4. Publish the release. The asset URL must match `latest.json`:
   `https://github.com/misalgar/sunset_overhaul-releases/releases/download/v<version>/sunset-<version>.apk`
5. In the releases repo (GitHub Desktop), commit and push `latest.json`.

GitHub's raw endpoint caches for a few minutes; allow up to five minutes before an immediate
re-check sees the new manifest.

## How the app updates

- After login, the release build checks `latest.json` at most once every 24 hours.
- Settings → **Check for updates** checks immediately.
- If `versionCode` is newer, a dialog shows the version and notes:
  - **Install update** downloads the APK, verifies SHA-256, verifies that the signing certificate
    matches the installed app, then opens the system installer.
  - **Later** dismisses it for this session.
- Fire OS always shows its own install confirmation. If prompted, allow “Install unknown apps” for
  Sunset; the dialog can open that settings page.
- Updates are never silent and never installed without the system prompt.

## Manifest format

```json
{
  "versionCode": 2,
  "versionName": "0.2.0",
  "apkUrl": "https://github.com/misalgar/sunset_overhaul-releases/releases/download/v0.2.0/sunset-0.2.0.apk",
  "sha256": "lowercase hex",
  "notes": "Live TV fixes",
  "mandatory": false
}
```

`mandatory: true` hides **Later** and prevents dismissing the dialog.

## Security notes

- The manifest and APK are public by design; the APK contains the TMDB key and the update URL, but
  no provider credentials (those are entered at login and stored encrypted).
- Even over HTTP, a tampered APK fails the SHA-256 check and the signature comparison against the
  installed app's certificate. Android would also refuse the update.
- `REQUEST_INSTALL_PACKAGES` is declared for the system installer; the user still confirms every
  install.

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| `latest.json` still has an old hash | Push the releases repo; GitHub raw caches ~5 min. |
| `apkUrl` returns 404 | Tag or file name mismatch. Use `v<version>` and `sunset-<version>.apk`. |
| “not signed by Sunset” | A different keystore signed the APK. Rebuild with the original keystore. |
| No update prompt | Installed `versionCode` is already >= the manifest, or the last check was under 24 h ago. Use Settings → Check for updates. |
| Fire TV refuses to install | Allow installs from Sunset in Fire TV settings, then tap Install again. |
| Release build is unsigned | `local.properties` is missing `sunset.signing.*` or the keystore path is wrong. |
