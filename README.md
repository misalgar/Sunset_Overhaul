# Sunset Overhaul — foundation milestone

A new Kotlin + Jetpack Compose streaming client for Android phones, tablets, Android TV and Fire TV.
This repository is a **from-scratch scaffold**; it does not patch, decompile or reuse the legacy APK.

The app builds and demos entirely offline against a local fake provider, and debug builds can also
sign in to a live Xtream Codes panel entered on the login screen. No endpoint, credential, token or
stream URL is stored in the repository; release builds still bind the fail-closed adapter.

## Status

| Area | State |
| --- | --- |
| Multi-module Gradle scaffold | Complete |
| Adaptive shell (phone bottom bar / tablet + TV top bar) | Complete |
| Home, Live, Guide, Movies, Series, Search, My List, Details, Settings, About, Player | Complete for the milestone |
| Parental controls, Profiles | Honest placeholders (v1.0 / v1.2 scope) |
| Offline fake provider + fixture catalogue | Complete |
| JVM tests for fake-provider login, paging, search and streams | 10 passing |
| Bundled sample video for the player | Complete (`core/player/src/main/res/raw/sample_placeholder.mp4`) |
| English + Spanish UI | Complete; all strings live in `:core:ui` |
| Release fail-closed provider binding | Complete |
| Live Xtream Codes login, account, live categories/channels, live playback (debug) | Complete |
| VOD and series catalogues, details, episodes and three-way search from the live panel (debug) | Complete |
| EPG from the live panel | Next slice |

Builds, lint and JVM tests were verified on 2026-10-23; see [Verification](#verification) and
[docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) for exact commands and results. Manual
phone and TV demo flows still need a device or emulator.

## Requirements

- JDK 17
- Android SDK platform 37, build-tools 36.0.0 and platform-tools
- `ANDROID_HOME` or `local.properties` with `sdk.dir=...`
- Gradle is provided by the wrapper (9.3.1)

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug   # requires a connected device/emulator
```

On Windows use `gradlew.bat`.

## Signing in to a live panel (debug)

The debug build keeps the offline demo and adds a real Xtream Codes provider behind the same
`ContentProvider` contract.

1. Launch the debug build; the login screen appears (no session is created automatically).
2. Enter the **server address** (`http://host:8080`), username and password, then **Sign in**.
   The server form remembers the last address; the session token is encrypted with the Android
   Keystore.
3. **Explore the demo** keeps the offline catalogue available with no server or account.

Cleartext HTTP is permitted in debug because the panel redirects streams to a raw-IP host. The
release policy is generated at build time from `sunset.provider.host` in the gitignored
`local.properties`, so no provider hostname is stored in the repository.

## Demo flow (offline)

1. Launch the debug build and tap **Explore the demo** on the login screen.
2. **Home → hero → Details → Play** opens the bundled sample video.
3. **Live TV → any channel** opens live-style playback; **Open guide** shows the EPG grid.
4. **Search** (from More on a phone, or the top bar on a tablet/TV) → result → Details → Play.
5. **My List** persists locally through Room; **Continue Watching** resumes VOD position.
6. **More → Settings** changes language, subtitles, autoplay and reduce-motion preferences.

The player always shows a persistent "placeholder sample" marker. The bundled clip is 12 seconds of
locally generated H.264/AAC video — it is never labelled as provider content.

Deep links:

```text
sunset://open/details/MOVIE/<id>
sunset://open/details/SERIES/<id>
sunset://open/search?q=<query>
```

## Module map

| Module | Responsibility |
| --- | --- |
| `:app` | Application, single activity, startup state, adaptive shell, navigation, debug/release provider binding |
| `:core:common` | Coroutine dispatchers and redacting logger |
| `:core:model` | Domain models and the `ContentProvider` contract |
| `:core:designsystem` | Appendix C tokens, theme, typography, buttons, chips, focus ring, states |
| `:core:ui` | Shared cards, hero, rows, formatting and the canonical EN/ES string catalogue |
| `:core:database` | Room entities, DAOs and database |
| `:core:data` | Fake catalogue/provider, sessions, settings, favourites, progress, paging |
| `:core:network` | Known endpoint constants, empty config, fail-closed EZServer adapter |
| `:core:player` | Media3 engine, playback surface, bundled sample |
| `:feature:*` | One module per screen area (13 feature modules) |

## Debug vs release

- **Debug** (`app/src/debug`): binds `FakeContentProvider`, auto-starts a synthetic demo session, and
  uses the `demo://local-sample` stream reference. No socket is opened.
- **Release** (`app/src/release`): binds `EzServerContentProvider`, which throws
  `ProviderError.ServiceNotConfigured` for every call. `AppViewModel` detects the unconfigured
  provider before login and renders "Service not configured". There is no mock auth path in release.

The debug application id is `com.markooverhaul.sunset.v2.debug`; release is
`com.markooverhaul.sunset.v2`.

## Product guardrails

- No ads, AdMob, Firebase Analytics, In-App Messaging or behavioural analytics.
- No credentials, tokens, stream URLs or production hostnames in the repository.
- No DVR, multi-screen, VPN, cloud backup, reminders, radio, cast, downloads or speed test.
- English and Spanish only; no hardcoded user-facing copy outside string resources.
- TV is first-class: D-pad focus rings, 48 dp gutters, 10-foot typography, predictable back.
- R8/minification is **disabled** in this scaffold. It must be enabled and tested before a
  production release.

## Verification

Verified on 2026-10-23 with JDK 17, Android SDK 37 and Gradle 9.3.1:

```bash
./gradlew :app:assembleDebug    # BUILD SUCCESSFUL
./gradlew :app:assembleRelease  # BUILD SUCCESSFUL (unsigned)
./gradlew :app:lintDebug        # BUILD SUCCESSFUL, 0 errors
./gradlew test                  # BUILD SUCCESSFUL, 10 JVM tests
```

Artifacts:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release-unsigned.apk`

Manual phone/TV checks are still outstanding because this environment has no emulator or device.
See [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) §6 for the checklist and exact
toolchain notes. If `java`, the Android SDK or the build tools are unavailable, treat the build as
unverified and follow the setup steps there.

## Package naming decision

The implementation uses `com.markooverhaul.sunset.v2` as instructed for this overhaul. The design
spec appendix refers to `com.nathnetwork.sunset.v2`. Confirm the final application id before any
store or provider release; changing it later also changes the deep-link authority and signing
identity.

## Documentation

- [Implementation plan and status](docs/IMPLEMENTATION_PLAN.md)
- [Running on an Android device](docs/RUNNING_ON_DEVICE.md)
- [Provider integration guide and blockers](docs/PROVIDER_INTEGRATION.md)
- [Requirements and design spec](docs/Sunset_Overhaul_Requirements_and_Design_Spec.md)

## Third-party notices

Kotlin, Jetpack Compose, AndroidX, Media3, Hilt, Room, Retrofit, OkHttp, Coil, Paging and
kotlinx.serialization are used under the Apache License 2.0 or their respective licences. The
bundled Inter font is used under the SIL Open Font License 1.1. The sample video was generated
locally for this repository and contains no third-party footage.
