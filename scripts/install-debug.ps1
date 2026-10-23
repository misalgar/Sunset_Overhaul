# Installs and launches the Sunset debug APK on a USB-connected Android device.
# Usage: powershell -ExecutionPolicy Bypass -File .\scripts\install-debug.ps1

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$apk = Join-Path $repoRoot "app\build\outputs\apk\debug\app-debug.apk"
$packageId = "com.markooverhaul.sunset.v2.debug"

if (-not (Test-Path $apk)) {
    Write-Host "Debug APK not found: $apk" -ForegroundColor Red
    Write-Host "Build it first:"
    Write-Host "  .\gradlew.bat :app:assembleDebug"
    exit 1
}

$candidates = @()
if ($env:ANDROID_HOME) { $candidates += (Join-Path $env:ANDROID_HOME "platform-tools\adb.exe") }
if ($env:ANDROID_SDK_ROOT) { $candidates += (Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe") }

$localProperties = Join-Path $repoRoot "local.properties"
if (Test-Path $localProperties) {
    $sdkLine = Get-Content $localProperties |
        Where-Object { $_ -match '^\s*sdk\.dir\s*=' } |
        Select-Object -First 1
    if ($sdkLine) {
        $sdkDir = $sdkLine.Substring($sdkLine.IndexOf('=') + 1).Trim()
        $sdkDir = $sdkDir.Replace('\\', '\').Replace('\:', ':')
        $candidates += (Join-Path $sdkDir "platform-tools\adb.exe")
    }
}

# Portable toolchain used during the foundation milestone.
$candidates += "C:\Users\mko_r\AppData\Local\Temp\opencode\sunset-toolchain\sdk\platform-tools\adb.exe"

$adb = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $adb) {
    $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCommand) { $adb = $adbCommand.Source }
}
if (-not $adb) {
    Write-Host "adb was not found." -ForegroundColor Red
    Write-Host "Set ANDROID_HOME, add platform-tools to PATH, or install Android platform-tools."
    exit 1
}

Write-Host "Using adb: $adb"
$deviceLines = & $adb devices |
    Select-Object -Skip 1 |
    Where-Object { $_ -match '\tdevice$' }

if (-not $deviceLines) {
    Write-Host ""
    Write-Host "No authorized device is connected." -ForegroundColor Yellow
    Write-Host "1. Enable Developer options and USB debugging on the phone."
    Write-Host "2. Connect the USB cable and accept the 'Allow USB debugging?' prompt."
    Write-Host "3. Run this script again."
    Write-Host ""
    & $adb devices -l
    exit 1
}

Write-Host "Installing $apk"
& $adb install -r $apk
if ($LASTEXITCODE -ne 0) {
    Write-Host "Install failed. See docs/RUNNING_ON_DEVICE.md for troubleshooting." -ForegroundColor Red
    exit $LASTEXITCODE
}

Write-Host "Launching Sunset"
& $adb shell monkey -p $packageId -c android.intent.category.LAUNCHER 1 | Out-Null
Write-Host "Done. The app should be open on the device." -ForegroundColor Green
