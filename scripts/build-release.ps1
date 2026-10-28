param(
    [string]$Notes = "",
    [string]$ReleasesDir = "C:\Users\mko_r\Documents\sunset_overhaul-releases\sunset_overhaul-releases",
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

# Paparazzi 2.x lives on the root buildscript classpath and declares a JVM 21 runtime, so the
# whole Gradle build (not just the screenshot tasks) now needs JDK 21+. JDK 17 no longer works.
$env:JAVA_HOME = "C:\Users\mko_r\AppData\Local\Temp\opencode\sunset-toolchain\jdk\jdk-21.0.12.1+1"
if (-not (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
    throw "JDK 21 not found at $env:JAVA_HOME. The Paparazzi Gradle plugin requires a JDK 21+ runtime."
}
$env:ANDROID_HOME = "C:\Users\mko_r\AppData\Local\Temp\opencode\sunset-toolchain\sdk"

$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    if (-not $SkipBuild) {
        & .\gradlew.bat :app:assembleRelease --console=plain -q
        if ($LASTEXITCODE -ne 0) { throw "Gradle release build failed." }
    }

    $apk = Join-Path $root "app\build\outputs\apk\release\app-release.apk"
    if (-not (Test-Path $apk)) { throw "Release APK not found at $apk." }

    $buildTools = Get-ChildItem (Join-Path $env:ANDROID_HOME "build-tools") -Directory |
        Sort-Object Name -Descending |
        Select-Object -First 1
    if (-not $buildTools) { throw "Android build-tools not found." }

    $signatureOutput = & (Join-Path $buildTools.FullName "apksigner.bat") verify $apk 2>&1
    if ($LASTEXITCODE -ne 0) {
        $signatureOutput | Write-Host
        throw "APK signature verification failed."
    }

    $badging = & (Join-Path $buildTools.FullName "aapt.exe") dump badging $apk |
        Select-String -Pattern "^package:"
    $match = [regex]::Match($badging, "versionCode='(\d+)' versionName='([^']+)'")
    if (-not $match.Success) { throw "Could not read version from the APK." }
    $versionCode = [int]$match.Groups[1].Value
    $versionName = $match.Groups[2].Value

    $sha256 = (Get-FileHash $apk -Algorithm SHA256).Hash.ToLowerInvariant()

    $artifacts = Join-Path $root "build\release-artifacts"
    New-Item -ItemType Directory -Force -Path $artifacts | Out-Null
    $artifact = Join-Path $artifacts "sunset-$versionName.apk"
    Copy-Item $apk $artifact -Force

    $apkUrl = "https://github.com/misalgar/sunset_overhaul-releases/releases/download/v$versionName/sunset-$versionName.apk"
    $manifest = [ordered]@{
        versionCode = $versionCode
        versionName = $versionName
        apkUrl      = $apkUrl
        sha256      = $sha256
        notes       = $Notes
        mandatory   = $false
    }

    if (Test-Path $ReleasesDir) {
        $manifestPath = Join-Path $ReleasesDir "latest.json"
        $manifest | ConvertTo-Json | Set-Content -Path $manifestPath -Encoding UTF8
        Write-Host "Updated $manifestPath"
    } else {
        Write-Warning "Releases repo not found at $ReleasesDir; latest.json was not updated."
    }

    Write-Host ""
    Write-Host "Release artifact : $artifact"
    Write-Host "Version          : $versionName ($versionCode)"
    Write-Host "SHA-256          : $sha256"
    Write-Host "APK URL          : $apkUrl"
    Write-Host ""
    Write-Host "Next steps:"
    Write-Host " 1. Create a GitHub release tagged v$versionName in misalgar/sunset_overhaul-releases."
    Write-Host " 2. Upload '$artifact' as a release asset (do not commit it to git)."
    Write-Host " 3. Commit and push latest.json from the releases repo."
    Write-Host " 4. Installed clients pick it up on the next daily check or via Settings."
} finally {
    Pop-Location
}
