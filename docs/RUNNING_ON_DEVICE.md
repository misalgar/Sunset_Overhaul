# Running Sunset on a real Android device

The **debug APK** is the fastest way to run the app. It is already signed with the local debug
keystore, so it installs directly. It uses the offline demo provider and needs no account, server or
network connection.

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Package id: `com.markooverhaul.sunset.v2.debug`

The commands below assume Linux/macOS. On Windows use `gradlew.bat` and the `.ps1` scripts.

## Option A — USB with adb (recommended)

This is best while developing because you can see crash logs.

### 1. Enable USB debugging on the phone

1. Open **Settings → About phone**.
2. Tap **Build number** seven times. Enter your PIN if asked.
3. Go back to **Settings → System → Developer options**.
4. Turn on **USB debugging**.
5. On some phones (Xiaomi/Redmi/Oppo/Vivo), also turn on **Install via USB** and
   **USB debugging (Security settings)**.

### 2. Connect the phone

1. Connect the phone to the PC with a data-capable USB cable.
2. On the phone, choose **File transfer / Android Auto** if a USB mode prompt appears.
3. Accept the **Allow USB debugging?** prompt and tick **Always allow from this computer**.

### 3. Confirm the PC can see it

From the repository root:

```bash
adb devices -l
```

If `adb` is not on `PATH`, source the toolchain environment or use the full path:

```bash
source ~/.local/share/sunset-toolchain/env.sh
adb devices -l

# or
~/.local/share/sunset-toolchain/sdk/platform-tools/adb devices -l
```

Expected:

```text
List of devices attached
R58M1234567    device usb:1-1 product:... model:... device:...
```

- `unauthorized` → accept the prompt on the phone.
- `offline` → unplug, replug, or run `adb kill-server` then `adb devices`.
- Nothing listed → try another cable/port, or install the phone manufacturer's USB driver.

### 4. Install and launch

Use the helper script:

```bash
./scripts/install-debug.sh
```

Or run the commands manually:

```bash
adb install -r ./app/build/outputs/apk/debug/app-debug.apk
adb shell monkey -p com.markooverhaul.sunset.v2.debug -c android.intent.category.LAUNCHER 1
```

You can also tap the **Sunset** icon on the phone after installation.

## Option B — copy the APK to the phone

Use this when you do not have a USB cable or adb.

1. Copy `app-debug.apk` to the phone with a USB file transfer, Google Drive, or email.
2. Open the APK with the phone's file manager.
3. Allow **Install unknown apps** for that file manager/browser when prompted.
4. Install and open **Sunset**.

## What you should see

1. A brief Sunset splash.
2. Home with the offline demo banner and fictional catalogue.
3. **Home → hero → Details → Play** opens the bundled 12-second sample video.
4. **Live TV → channel** opens live-style playback.
5. **More → Settings** switches English/Spanish and playback preferences.

The debug build starts a synthetic demo session automatically. It does not contact a provider.

## Troubleshooting

### `INSTALL_FAILED_UPDATE_INCOMPATIBLE`

An older build with a different signature is installed. Uninstall the old app first:

```bash
adb uninstall com.markooverhaul.sunset.v2.debug
adb install ./app/build/outputs/apk/debug/app-debug.apk
```

### `INSTALL_FAILED_USER_RESTRICTED`

Enable **Install via USB** in Developer options, or confirm the install prompt on the phone.

### App opens and immediately closes

Capture the crash:

```bash
adb logcat -c
adb shell monkey -p com.markooverhaul.sunset.v2.debug -c android.intent.category.LAUNCHER 1
adb logcat '*:E'
```

Send the `AndroidRuntime` / `FATAL EXCEPTION` block.

### `adb: command not found`

Source the toolchain environment, or add platform-tools to the current shell:

```bash
source ~/.local/share/sunset-toolchain/env.sh

# or
export PATH="$ANDROID_HOME/platform-tools:$PATH"
```

## After the debug run: a proper signed release

The debug APK is for testing. For a release APK that can be shared or installed like a normal app:

1. Create a private upload keystore **outside the repository**.
2. Add `sunset.signing.*` values to the untracked `local.properties`, never by committing the
   keystore or passwords.
3. Build `:app:assembleRelease` (or run `scripts/build-release.sh`).
4. Install and test the signed `app-release.apk` on the same device.
5. Only then consider R8, baseline profiles and store packaging.

Do not reuse the debug keystore for a published release.
