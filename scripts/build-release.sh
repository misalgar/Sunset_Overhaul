#!/usr/bin/env bash
# Builds, verifies, copies and hashes the Sunset release APK, then updates latest.json
# in the releases repo checkout.
#
# Usage: scripts/build-release.sh [--notes "What changed"] [--releases-dir <path>] [--skip-build]

set -euo pipefail

NOTES=""
RELEASES_DIR="${HOME}/Repositories/sunset_overhaul-releases"
SKIP_BUILD=0

usage() {
    cat <<'EOF'
Usage: scripts/build-release.sh [options]

Options:
  --notes <text>         Release notes written to latest.json
  --releases-dir <path>  Local checkout of misalgar/sunset_overhaul-releases
                         (default: ~/Repositories/sunset_overhaul-releases)
  --skip-build           Reuse the existing release APK
  -h, --help             Show this help
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --notes)
            NOTES="${2:-}"
            shift 2
            ;;
        --releases-dir)
            RELEASES_DIR="${2:-}"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd -- "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT"

# Fall back to the local Linux toolchain when the shell has not been initialised.
if [[ -z "${JAVA_HOME:-}" && -x "$HOME/.local/share/sunset-toolchain/jdk-21/bin/java" ]]; then
    export JAVA_HOME="$HOME/.local/share/sunset-toolchain/jdk-21"
fi
if [[ -z "${ANDROID_HOME:-}" && -d "$HOME/.local/share/sunset-toolchain/sdk" ]]; then
    export ANDROID_HOME="$HOME/.local/share/sunset-toolchain/sdk"
fi

if [[ -z "${JAVA_HOME:-}" || ! -x "$JAVA_HOME/bin/java" ]]; then
    echo "JDK 21 not found. Set JAVA_HOME or install it at $HOME/.local/share/sunset-toolchain/jdk-21." >&2
    exit 1
fi
if [[ -z "${ANDROID_HOME:-}" && -z "${ANDROID_SDK_ROOT:-}" ]]; then
    echo "Android SDK not found. Set ANDROID_HOME or ANDROID_SDK_ROOT." >&2
    exit 1
fi
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"

if [[ "$SKIP_BUILD" -eq 0 ]]; then
    ./gradlew :app:assembleRelease --console=plain -q
fi

APK="$ROOT/app/build/outputs/apk/release/app-release.apk"
if [[ ! -f "$APK" ]]; then
    echo "Release APK not found at $APK." >&2
    echo "If the build is unsigned, add sunset.signing.* to local.properties." >&2
    exit 1
fi

BUILD_TOOLS="$(find "$SDK/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
if [[ -z "$BUILD_TOOLS" ]]; then
    echo "Android build-tools not found under $SDK/build-tools." >&2
    exit 1
fi
APKSIGNER="$BUILD_TOOLS/apksigner"
AAPT="$BUILD_TOOLS/aapt"

if [[ ! -x "$APKSIGNER" || ! -x "$AAPT" ]]; then
    echo "apksigner or aapt not found in $BUILD_TOOLS." >&2
    exit 1
fi

if ! SIGNATURE_OUTPUT="$("$APKSIGNER" verify "$APK" 2>&1)"; then
    printf '%s\n' "$SIGNATURE_OUTPUT" >&2
    echo "APK signature verification failed." >&2
    exit 1
fi

BADGING="$("$AAPT" dump badging "$APK" | grep -m1 '^package:')"
VERSION_CODE="$(printf '%s\n' "$BADGING" | sed -nE "s/.*versionCode='([0-9]+)'.*/\1/p")"
VERSION_NAME="$(printf '%s\n' "$BADGING" | sed -nE "s/.*versionName='([^']+)'.*/\1/p")"
if [[ -z "$VERSION_CODE" || -z "$VERSION_NAME" ]]; then
    echo "Could not read version from the APK." >&2
    exit 1
fi

SHA256="$(sha256sum "$APK" | awk '{print $1}')"
ARTIFACTS="$ROOT/build/release-artifacts"
mkdir -p "$ARTIFACTS"
ARTIFACT="$ARTIFACTS/sunset-$VERSION_NAME.apk"
cp -f "$APK" "$ARTIFACT"

APK_URL="https://github.com/misalgar/sunset_overhaul-releases/releases/download/v$VERSION_NAME/sunset-$VERSION_NAME.apk"

if [[ -d "$RELEASES_DIR" ]]; then
    if ! command -v python3 >/dev/null 2>&1; then
        echo "python3 is required to write latest.json." >&2
        exit 1
    fi
    MANIFEST="$RELEASES_DIR/latest.json"
    python3 - "$MANIFEST" "$VERSION_CODE" "$VERSION_NAME" "$APK_URL" "$SHA256" "$NOTES" <<'PY'
import json
import sys

path, version_code, version_name, apk_url, sha256, notes = sys.argv[1:7]
with open(path, "w", encoding="utf-8") as manifest:
    json.dump(
        {
            "versionCode": int(version_code),
            "versionName": version_name,
            "apkUrl": apk_url,
            "sha256": sha256,
            "notes": notes,
            "mandatory": False,
        },
        manifest,
        indent=2,
    )
    manifest.write("\n")
PY
    echo "Updated $MANIFEST"
else
    echo "Warning: releases repo not found at $RELEASES_DIR; latest.json was not updated." >&2
    echo "Pass --releases-dir <path> to point at the checkout." >&2
fi

echo
echo "Release artifact : $ARTIFACT"
echo "Version          : $VERSION_NAME ($VERSION_CODE)"
echo "SHA-256          : $SHA256"
echo "APK URL          : $APK_URL"
echo
echo "Next steps:"
echo " 1. Create a GitHub release tagged v$VERSION_NAME in misalgar/sunset_overhaul-releases."
echo " 2. Upload '$ARTIFACT' as a release asset (do not commit it to git)."
echo " 3. Commit and push latest.json from the releases repo."
echo " 4. Installed clients pick it up on the next daily check or via Settings."
