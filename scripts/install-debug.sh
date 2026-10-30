#!/usr/bin/env bash
# Installs and launches the Sunset debug APK on a USB-connected Android device.
# Usage: scripts/install-debug.sh

set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_ID="com.markooverhaul.sunset.v2.debug"

if [[ ! -f "$APK" ]]; then
    echo "Debug APK not found: $APK" >&2
    echo "Build it first:" >&2
    echo "  ./gradlew :app:assembleDebug" >&2
    exit 1
fi

# Fall back to the local Linux toolchain when the shell has not been initialised.
if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/.local/share/sunset-toolchain/sdk" ]]; then
    export ANDROID_HOME="$HOME/.local/share/sunset-toolchain/sdk"
fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"

candidates=()
if [[ -n "$SDK" ]]; then
    candidates+=("$SDK/platform-tools/adb")
fi
if [[ -f "$ROOT/local.properties" ]]; then
    SDK_DIR="$(sed -nE 's/^[[:space:]]*sdk\.dir[[:space:]]*=[[:space:]]*(.*)$/\1/p' "$ROOT/local.properties" | head -1 | tr -d '\r')"
    if [[ -n "$SDK_DIR" ]]; then
        candidates+=("$SDK_DIR/platform-tools/adb")
    fi
fi
if command -v adb >/dev/null 2>&1; then
    candidates+=("$(command -v adb)")
fi

ADB=""
for candidate in "${candidates[@]}"; do
    if [[ -x "$candidate" ]]; then
        ADB="$candidate"
        break
    fi
done

if [[ -z "$ADB" ]]; then
    echo "adb was not found." >&2
    echo "Set ANDROID_HOME, add platform-tools to PATH, or source ~/.local/share/sunset-toolchain/env.sh." >&2
    exit 1
fi

echo "Using adb: $ADB"
DEVICE_LINES="$("$ADB" devices | awk 'NR > 1 && $2 == "device" { print }')"

if [[ -z "$DEVICE_LINES" ]]; then
    echo
    echo "No authorized device is connected."
    echo "1. Enable Developer options and USB debugging on the phone."
    echo "2. Connect the USB cable and accept the 'Allow USB debugging?' prompt."
    echo "3. Run this script again."
    echo
    "$ADB" devices -l
    exit 1
fi

DEVICE_COUNT="$(printf '%s\n' "$DEVICE_LINES" | wc -l)"
if [[ "$DEVICE_COUNT" -gt 1 ]]; then
    echo "Multiple devices are connected. Set ANDROID_SERIAL to choose one:" >&2
    "$ADB" devices -l >&2
    exit 1
fi

echo "Installing $APK"
"$ADB" install -r "$APK"

echo "Launching Sunset"
"$ADB" shell monkey -p "$PACKAGE_ID" -c android.intent.category.LAUNCHER 1 >/dev/null
echo "Done. The app should be open on the device."
