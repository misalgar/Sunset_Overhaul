# Sunset Overhaul — Agent Instructions (AGENTS.md)

# Agent Instructions & Project Rules

## Safe Shell Operations
You are permitted to use the terminal for read-only repository exploration and build inspection. You may use the following safe commands without asking for approval:
* **Directory mapping:** `ls`, `find`, `tree`
* **File reading:** `cat`, `head`, `tail`
* **Filtering & sorting:** `grep`, `awk`, `sort`
* **Build tasks:** `./gradlew` (for compiling or testing Sunset Overhaul)

## Command Formatting Rules
* Never use destructive or mutating shell commands (e.g., `rm`, `chmod`, `chown`, `mv`) without explicit prior discussion.

## File Access Boundaries
* Never access file directories outside the scope of the application, if needed ask for permission first and explain the reason behind it.

## 1. Ground-Truth Project State
- **Package Name:** `com.markooverhaul.sunset.v2`
- **Toolchain & SDK:** AGP `9.1.1`, Gradle `9.3.1`, Kotlin `2.4.10`, `compileSdk = 37`, `targetSdk = 37`, `minSdk = 23`.
  - Kotlin is applied via AGP 9 built-in support: NEVER add `org.jetbrains.kotlin.android` or `kotlinOptions`.
  - Keep `minSdk = 23` (Navigation Compose is pinned to 2.9.8 and DataStore to 1.2.1).
  - The project requires JDK 21 (Temurin), do not attempt to use JDK 17. 
  - JDK 21 is provisioned at `/home/misalgar/.local/share/sunset-toolchain/jdk-21`; the Android SDK is at `/home/misalgar/.local/share/sunset-toolchain/sdk`.
  - `~/.local/share/sunset-toolchain/env.sh` exports `JAVA_HOME`, `ANDROID_HOME` and the SDK tools; it is sourced from `~/.bashrc` for new shells.

## 2. Token-Saving & Documentation Rules (CRITICAL)
- **Do NOT read or update files in the `docs/` folder** (`IMPLEMENTATION_PLAN.md`, `PROVIDER_INTEGRATION.md`, `RELEASING.md`, `Sunset_Overhaul_Requirements_and_Design_Spec.md`) during normal tasks. This wastes tokens. All active architectural rules are already in this `AGENTS.md` file.
- **Changelog Logging:** If you need to record implementation notes or completed milestones, ONLY read/append a brief 3–5 bullet summary to `docs/CHANGELOG.md`. 
- **Do NOT scan or read** `*.ttf`, `*.mp4`, `*.png`, `*.apk`, `build/`, or `gradle/wrapper/`.
- **Targeted Verification:** During iteration, compile ONLY the modules you touched:
  - Fast compile: `./gradlew :feature:<name>:compileDebugKotlin`
  - Run `:app:assembleDebug` or `:app:lintDebug` ONLY when finalizing a task.

## 3. Architecture, Boundaries & Design Tokens
- **Dependency Flow:** `:feature:*` modules MUST NEVER depend on each other, `:core:network`, or `:core:database` directly. 
- **Design Tokens:** Always use `SunsetTheme.colors`, `SunsetTheme.typography`, and `SunsetSpacing`. Never hardcode raw hex colors or random padding values.
- **UI Testing:** We use Paparazzi for JVM-only visual regression testing. When creating or modifying a UI component or Screen in `:feature:*` or `:core:ui`, you MUST generate/update its Paparazzi test.
  - Setup: `@get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6_PRO.copy(screenWidth = 1920, screenHeight = 1080, orientation = ScreenOrientation.LANDSCAPE, density = Density.XHIGH), maxPercentDifference = 0.1)`
  - Generate: `./gradlew :<module_name>:recordPaparazziDebug`
  - Verify: `./gradlew :<module_name>:verifyPaparazziDebug`
- **Visual Testing Guardrail (Human Approval Required):** If `./gradlew :<module_name>:verifyPaparazziDebug` fails, you are STRICTLY FORBIDDEN from running `recordPaparazziDebug` to "fix" the test. You must output the file path to the failure diff (e.g., `[module]/build/paparazzi/failures/debug/...`) so the user can visually inspect it. Wait for the user's explicit permission before recording a new golden.
- **Navigation & D-Pad Testing:** Paparazzi only covers static visual layouts. D-pad focus traversal, keyboard/IME behavior, and back-stack navigation cannot be verified via Paparazzi. Any changes to focus logic (`FocusRequester`, `onFocusChanged`) MUST be flagged to the user for manual physical verification on their Fire TV via ADB.

## 4. Data, Provider & Playback Rules (The Xtream Integration)
- **Active Provider:** `XtreamContentProvider` (`:core:network`) is the primary production provider bound in `:app` release.
- **Xtream WAF Header:** All Xtream HTTP requests MUST send `User-Agent: SunsetTV/2.0 (Android)`. The default `okhttp` user-agent gets a 403 Forbidden.
- **Cleartext HTTP:** The Xtream panel API is HTTP and live streams redirect to dynamic raw IPs on port 8080. Debug permits cleartext.
- **Room Catalogue (DB v4):** Movies/Series use `CatalogueVariantIndex` (`titleKey`, `groupKey`, `isDefault = 1`) to group language/quality variants. Resyncs MUST preserve TMDB columns via `withTmdbState`.
- **Live Status & Event Times:** `channelStatus(...)` sets `startedAtEpochMillis` ONLY from `LiveChannel.scheduledStartUtcMs` (parsed by `LiveEventSchedule` with 10-min grace), NEVER from 24/7 EPG programme start times. 
- **OTA Updates:** Handled by `UpdateManager`. Release builds sign via untracked `local.properties` and publish to `misalgar/sunset_overhaul-releases` via `scripts/build-release.sh` (Linux) or `scripts/build-release.ps1` (Windows).

## 5. Android TV / Fire TV D-Pad & Focus Invariants (DO NOT REGRESS)
1. **`LocalContentEntryFocus` Binding:** Every top-level route in `LargeTopLevel`/`PhoneTopLevel` MUST read `LocalContentEntryFocus.current` and attach `Modifier.focusRequester(entryFocus)` to its primary entry control so pressing `Down` from `MainShell` top nav works deterministically.
2. **Focus Verification & Touch Mode:** Always use `requestFocusFromTouch()` when recovering view focus and verify focus gain across frames (hybrid remotes leave the window in touch mode).
3. **TV TextFields & Floating IME:** Keep TV text fields non-focusable (`canFocus = editing`) behind a D-pad launcher box so D-pad traversal does not auto-open the keyboard (Fire TV IME consumes `Back` silently). Any clear (`X`) button must be a separate focusable stop outside the `TextField`.
4. **Focus Styling:**
   - **Buttons:** Use fill color change + `sunsetFocusRing(gap = 2.dp)`. NEVER apply scale lift (`1.04f`) to buttons.
   - **Cards/Posters:** Use flush focus ring or border + `1.04f` scale lift on artwork only.
   - **Guide Grid:** Lay out timeline blocks sequentially (`Spacer` + block), NEVER with absolute `Modifier.offset`, so geometric D-pad left/right focus works.
   
   
