# Implementation plan — foundation milestone

Source of truth: [Sunset_Overhaul_Requirements_and_Design_Spec.md](Sunset_Overhaul_Requirements_and_Design_Spec.md).
This document records what was built, how it is verified and what remains.

## 1. Goal

Deliver a runnable, fully offline foundation for the Sunset v2 client:

- adaptive Compose app shell for phone, tablet, Android TV and Fire TV;
- placeholder-quality but complete screens for the milestone journeys;
- one provider boundary with a fake implementation and a fail-closed production adapter;
- English/Spanish string catalogue and design tokens from the spec appendix;
- no production calls, credentials, ads or analytics.

## 2. Decisions taken

| Decision | Rationale |
| --- | --- |
| Package `com.markooverhaul.sunset.v2` | Instructed for this overhaul; spec appendix says `com.nathnetwork.sunset.v2`. Confirm before release. |
| 22 Gradle modules | Matches spec §12.2 and keeps provider, player and feature code isolated. |
| Version catalog + Gradle KTS | Single reviewable dependency source. |
| Strings centralised in `:core:ui` | One translation file pair shared by all features; avoids per-feature drift. |
| `ContentProvider` in `:core:model` | Domain contract has no networking or DTO types. |
| Fake provider in `:core:data` | Demo data and repositories share the same module. |
| Provider binding in `:app` source sets | Debug binds the fake; release binds the blocked adapter. No mock code is reachable in release. |
| Manual breakpoints + `LocalIsTelevision` | Avoids a window-size-class dependency for the milestone; TV detection uses `UiModeManager`. |
| Deterministic gradient/monogram artwork | Fixture artwork URLs are `null`, so the demo is 100% offline. |
| Bundled sample MP4 | Player is demonstrable without a public stream or network. |
| R8 disabled | Keeps the scaffold reviewable; must be enabled before production. |
| Settings session in DataStore | Synthetic demo session only. Real tokens must move to Keystore-backed storage before integration. |

## 3. Version matrix

| Component | Version |
| --- | --- |
| Android Gradle Plugin | 9.1.1 |
| Gradle wrapper | 9.3.1 |
| Kotlin | 2.4.10 (AGP built-in Kotlin) |
| KSP | 2.3.12 |
| Hilt | 2.60.1 |
| Compose BOM | 2026.09.00 |
| Compose for TV | 1.1.0 |
| Navigation Compose | 2.9.8 |
| Lifecycle | 2.11.0 |
| Activity Compose | 1.13.0 |
| Room | 2.8.5 |
| DataStore | 1.2.1 |
| Paging | 3.5.3 |
| Media3 | 1.11.1 |
| Coil | 3.6.3 |
| Retrofit / OkHttp | 3.0.0 / 4.12.0 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 23 |

`compileSdk 37` maps to the installed `platforms;android-37.0` package and requires AGP 9.1+.
Kotlin is applied by AGP 9's built-in Kotlin support; do not add `org.jetbrains.kotlin.android` or
`kotlinOptions`. Navigation is pinned to 2.9.8 and DataStore to 1.2.1 because their next releases
require `minSdk 24`, while this milestone must keep `minSdk 23`. The wrapper is committed so the
build is reproducible; only the JDK and SDK are host prerequisites.

## 4. Module build order

```text
core:common -> core:model -> core:designsystem -> core:ui
core:database -> core:data
core:network (blocked adapter, no runtime use)
core:player
feature:* -> app
```

`:core:ui` owns the shared string catalogue and shared composables. Feature modules depend on
design tokens and UI components, never on each other.

## 5. What is implemented

### 5.1 App shell and startup

- `SunsetApplication` with `@HiltAndroidApp`.
- `MainActivity : AppCompatActivity` (required for per-app locales) with TV detection.
- `AppViewModel` startup states: `Loading`, `Ready`, `NeedsLogin`, `ServiceNotConfigured`.
- Debug auto-starts a synthetic session; release fails closed before login.
- `SunsetTheme` applies TV typography and the persisted reduce-motion preference.
- Adaptive shell: phone bottom navigation; tablet/TV wordmark + top navigation.
- Navigation Compose graph with details and search deep links.
- `More` destination for Guide, Search, My List and Settings on phones.

### 5.2 Screens

| Screen | Notes |
| --- | --- |
| Splash | Wordmark, tagline, demo/checking copy |
| Home | Hero, Continue Watching, Live Now, recent/recommended rows, My List |
| Live TV | Category filters, favourites, now/next, guide entry |
| Guide | Day selection, channel column, programme cards, favourites |
| Movies / Series | Paging 3 grids with category filters and empty/error states |
| Search | Debounced query, recent searches, grouped results, favourite toggle |
| My List | Resolves local favourites into movies, series and channels |
| Details | Movie and series detail, seasons, episodes, similar titles |
| Player | Media3 surface, resume, 5 s progress saves, autoplay next, D-pad controls, live mode |
| Account | Demo login (debug), blocked-service copy in release |
| Settings | Account, playback toggles, language, reduce motion, parental/about/profile links |
| About | Version/build/provider status, TMDB attribution, privacy, licences |
| Parental | Honest placeholder; no lock state is claimed |
| Profiles | Honest placeholder for the v1.2 picker |

### 5.3 Data and playback

- `FakeCatalog`: fictional movies, series, episodes, channels and EPG; local-midnight guide data.
- `FakeContentProvider`: deterministic paging, search and `demo://local-sample` resolution.
- `SessionStore` / `AuthRepository`: DataStore-backed synthetic session.
- `SettingsRepository`, `FavouritesRepository`, `ProgressRepository`, `SearchHistoryRepository`.
- Room database for favourites and watch progress.
- `Media3PlaybackEngine`: maps demo references to `raw/sample_placeholder.mp4`; classifies errors.
- Per-player engine instance, released in `ViewModel.onCleared()`; final position persisted.

### 5.4 Design system and strings

- Appendix C colour, radius, spacing, motion and type tokens.
- Bundled Inter static fonts (offline, API 23+).
- Focus ring (flush on cards, 2 dp gap halo on buttons), card focus lift, pill buttons, chips,
  section cards, empty/error states, skeletons.
- Device-aware type scale (TV = 1.15× phone, body never below 18 sp).
- 285 English strings mirrored 1:1 in Spanish under `:core:ui`.

### 5.5 Live Xtream provider (first slice, 2026-10-23)

The panel was identified from the legacy app's own traffic (system proxy capture on the Fire TV,
hostnames only) and confirmed with one read-only auth request, then a categories request.

- `XtreamApi` / `XtreamContentProvider` in `:core:network`: `player_api.php` login, account summary,
  live categories, live channels, channel search, live stream URLs, JSON disk cache (6 h).
- Panel WAF rejects the default `okhttp` agent; requests send `SunsetTV/2.0 (Android)`.
- `RoutingContentProvider` (debug binding) serves the demo catalogue when the server field is empty
  and the live panel when it is set; the mode is re-derived from the stored token on every start.
- `EncryptedSessionStore`: AES-256-GCM key in the Android Keystore; the token carries the Xtream
  credentials because Xtream is stateless.
- Login screen gained a server field with focus chaining (IME Next moves between fields) and an
  **Explore the demo** action; debug no longer auto-starts a demo session.
- `network_security_config`: release permits cleartext only for the provider host; debug permits it
  because streams redirect to a raw-IP host.
- Player: the "placeholder sample" marker now shows only for the bundled demo clip, never over a
  real stream.

Verified on the Fire TV: server → username/password → Home live row → Live TV (1,130 channels,
real categories) → channel playback with live broadcast content; session survives reinstall.

### 5.6 VOD and series metadata (2026-10-24)

- `get_vod_categories` / `get_vod_streams` (52,499 items, 16.6 MB) and `get_series_categories` /
  `get_series` (12,966 items) with in-memory paging (60 per page) over a 6 h disk cache keyed by
  account. The cache survives restarts; it used to be wiped on every restore, which forced a full
  re-download per launch.
- `get_vod_info` enriches a movie with backdrop, plot, cast, director, genre and runtime.
- `get_series_info` maps the season → episodes object into `SeriesDetail`/`Season`/`Episode`, with
  duration, still, air date and per-episode stream extensions.
- Search now covers live, movies and series (40 results per group).
- Playback uses each item's `container_extension` instead of a hardcoded `.mp4`.
- Home loads live first and the large catalogues second, so it is usable while VOD downloads.
- TV grids use six 140 dp columns so the poster and its two-line caption fit the viewport; the Home
  hero grew to 320-380 dp so a two-line title no longer clips.

Verified on the Fire TV: movie grid and captions, movie detail (`get_vod_info`), movie playback,
series grid, series detail with 12 episodes and cast, episode playback, three-way search.

### 5.7 EPG via XMLTV (2026-10-24)

- `get_short_epg` and `get_simple_data_table` return zero listings on this panel; `xmltv.php`
  returns a 3.2 MB XMLTV document, so that is the source.
- `XmltvParser` walks the document once with a pull parser and keeps only the guide fields; no DOM,
  no 3 MB string kept around beyond the fetch.
- Programme channel ids are translated to stream ids while parsing, so Home/Live/Guide can match
  `EpgProgramme.channelId` to `LiveChannel.id` directly.
- The guide is cached in memory per process and on disk for six hours; `getGuide(days)` windows the
  result to the requested range.
- Home and Live group programmes by channel once instead of filtering per channel, and the Guide
  grid groups per day selection, because the document holds tens of thousands of programmes.
- Channels without guide data render "No guide data for this channel".

Verified on the Fire TV: On Now cards show programme title and time, the Guide lists a channel's
timeline with a red "Now" marker, and Home's Live Now rail shows now/next.

### 5.8 Broadcast-style TV guide (2026-10-24)

Replaced the stacked programme cards with a time grid closer to a set-top-box guide:

- Programme preview above the grid: channel, title, time range, remaining time, description,
  channel/programme artwork and a Watch action. It follows the focused block.
- Time ruler every 30 minutes with a red now-line; one row per channel; blocks sized by real
  duration (clipped at midnight) and a progress bar on the current programme.
- Ruler and every row share one `ScrollState`, so the ruler stays aligned with the grid while
  D-pad focus scrolls horizontally.
- Blocks are laid out sequentially (`Spacer` + block) instead of absolute `offset`. Absolute
  offsets made the focus engine treat every block as starting at the row origin, so left/right
  skipped the timeline. Sequential layout gives correct geometric focus traversal.
- The header date follows the selected day ("Tomorrow · Oct 25, 2026").
- Channel cells are focusable and play the channel on Select, so channels with no EPG data are
  still watchable; they show a short "No guide data" label.
- Grid text stays one line per block and only shows the time range when the block is wide enough.

Verified on the Fire TV: grid alignment, ruler sync, now marker, D-pad left/right through the
timeline, up/down across channels, preview updates, programme/channel playback, Tomorrow view and
the no-guide state.

### 5.9 Flat guide visual pass (2026-10-24)

Replaced the card grid with a continuous schedule surface:

- Programme cells are no longer enclosed: no per-block background, rounded corners or borders.
  Text sits directly on the channel row; only focused/current cells carry a fill.
- Focus is edge-to-edge: the whole programme cell fills with `AccentPrimary` and switches to
  `AccentOn` text. Channel cells use a translucent amber fill plus a 3 dp left bar. The regular
  `sunsetFocusRing` is bypassed via the new `drawFocusRing` flag on `SunsetFocusable`.
- Separation comes from 1 dp horizontal row dividers, alternating row shading and 1 dp programme
  boundaries. The old 30-minute vertical grid lines were removed.
- The preview is a borderless gradient band; Today/Tomorrow/Favourites are text tabs with a 2 dp
  underline on the selected tab and a borderless amber wash on focus. "Watch channel" is a text
  action with a play glyph and an underline on focus.
- Rows are 52 dp, the outer gutter 32 dp, and programme cells have no gaps. The initial scroll is
  snapped to a 30-minute boundary so the first ruler label is fully visible.
- The now line sits behind programme text; the ruler carries a red now line and triangle marker.
- TV focus is wired explicitly: grid → active day tab → preview action → grid, so the filters are
  reachable on a remote and Up from the grid returns to the active tab.

Verified on the Fire TV: flat rows and dividers, solid amber cell focus, channel focus wash, tab
focus loop, Tomorrow switch with the active tab retained, and channel playback.

### 5.10 Room catalogue, country filtering and TMDB (2026-10-24)

- The VOD/series catalogue now lives in Room (`catalogue_meta`, `catalogue_movies`,
  `catalogue_series`, `catalogue_episodes`, DB version 2) and is paged with indexed `LIMIT/OFFSET`
  queries. The raw JSON remains in the six-hour disk cache, so a failed insert does not refetch
  16.6 MB.
- First sync still parses the full payload; later launches query SQLite directly. Home was also
  decoupled: live rows render first, the Room hero fills in independently, and the XMLTV now/next
  map arrives last. Measured cold start to hero: ~6 s (previously ~30 s).
- `CountryResolver` parses leading tokens (`USA |`, `CAN|`, `CH |`, `NC /`) with a curated alias
  map and falls back to country-named categories (`Puerto Rico`, `España`, `TV Mexico`, …).
  Unrecognised tokens (`NCAA`, `TNT`, `CINE`) are left untouched. Channel names are stored cleaned
  and `LiveChannel.countryCode` is populated; 643 of 1,194 channels map to 23 countries.
- The guide has a right-aligned country text tab (`All countries` / `United States` / `Other`) that
  opens a D-pad dialog with per-country counts. The filter composes with day and favourites.
- TMDB enrichment is optional and machine-local: `tmdb.read.token` / `tmdb.api.key` in the
  gitignored `local.properties` become `BuildConfig` fields; blank values disable it. Provider
  identity and playback fields always win; TMDB supplies overview/cast/genres/runtime/certification
  and fills missing posters/backdrops. Matching strips quality suffixes and compares both localised
  and original titles with a ±1-year window. Movie/series details enrich on open; episode stills
  cover the first three seasons; a bounded backfill (60 movies + 40 series) runs after sync.
  Matches are cached for 30 days. The existing About attribution is used.

Verified on the Fire TV: cleaned channel names, country picker and US-only guide, instant Movies
grid from Room, cold start to hero, and a TMDB-enriched movie detail with backdrop, runtime, genre
and overview.

### 5.11 Home, PPV destination and TMDB rails (2026-10-24)

- Home billboard: TV height reduced to 280–320 dp and a 40 sp compact title style, so long titles
  fit two lines instead of clipping above the billboard. Continue Watching and Live Now cards were
  reduced from 300–360 dp to 220–240 dp and the landscape monogram is smaller.
- Titles stored by older catalogue versions are cleaned at mapping time, so `SUB`/`DUB`/`4K`
  suffixes disappear without a resync.
- PPV is a first-class destination (`PPV` in the top navigation, More on phones), separate from
  Live TV. Live categories/channels tagged `PAY PER VIEW` are filtered out of Live. The PPV screen
  shows the provider’s dated category (`PAY PER VIEW 24/10/2026`), categories, events and now/next.
- Live categories and channels now refresh every 15 minutes instead of six hours, and the PPV
  screen has a Refresh action that drops both the disk and in-memory caches. This is what updates
  the dated PPV label after the provider’s daily change.
- “More like this” now uses TMDB recommendations: `movie/{id}/recommendations` and
  `tv/{id}/recommendations`, resolved against the local Room catalogue by normalised
  localised/original title and year. Only playable local titles are shown; genre matching remains
  the fallback when TMDB has no recommendation. Title matching now ignores stop words and
  punctuation (`Deadpool & Wolverine` = `Deadpool and Wolverine`), and the match cache is versioned
  so old no-match rows are retried.

Verified on the Fire TV: compact Home billboard, PPV screen with the 24/10/2026 label and events,
PPV removed from Live, and a populated Deadpool “More like this” rail resolving to local titles.

### 5.12 Fast detail rendering (2026-10-24)

The series detail used to wait for all of `get_series_info`, TMDB series detail, up to three TMDB
season requests and recommendations before leaving the full-screen skeleton. On the Fire TV that
was a 10 s blue screen in the reported recording.

- `getMovieSummary` / `getSeriesSummary` read the Room row and paint the hero immediately
  (`isLoading = false`). The provider detail, episodes and enrichment then update the state.
- Recommendations were moved to a separate coroutine, so the “More like this” rail no longer blocks
  the hero.
- `getSeriesDetail` no longer awaits TMDB series metadata or season stills; both run in the
  provider's background scope. The first open shows provider data, and the next open shows the
  enriched result.
- While episodes are still loading, the series screen shows three compact skeletons under the hero
  instead of a full-screen skeleton or “episodes coming soon”.

Verified on the Fire TV: a warm open of “Fear the Walking Dead” paints the full hero in about 3 s
(previously the series skeleton alone exceeded 10 s). The movie “The Virtuoso” also renders its
hero before the recommendations rail.

Recommendations use a three-state model (`Loading`, `Empty`, `Loaded`). While loading, the “More
like this” header, a small spinner and “Finding similar titles…” sit above four layout-matched
poster skeletons, so the rail reserves its final height and does not pop in later. If both TMDB and
the genre fallback return nothing, the section is hidden instead of showing a dead spinner.

### 5.13 Home D-pad row traversal (2026-10-24)

Nested `LazyRow`s made Compose’s geometric focus search unreliable: Down inside Live Now could stay
in the rail, and Up from Recently added in Movies jumped to the hero.

- Home tracks the focused rail and intercepts Up/Down at the `LazyColumn` root.
- Moving vertically scrolls to the adjacent row and requests focus on its **first card**. This works
  even when the target row was outside the composed window.
- The hero’s initial focus request moved from the hero item to the screen, so recycling the hero
  while scrolling no longer steals focus back.
- “See all” and the hero keep their normal focus behaviour; the row handler only runs while a rail
  card is focused.

Verified on the Fire TV: Down from Live Now moves to the next rail, Up from Recently added returns
to the rail above, and vertical movement no longer gets stuck or jumps to Play.

### 5.14 Home artwork fallbacks (2026-10-24)

- The Home billboard now uses `backdropUrl ?: posterUrl`, so a movie without a TMDB/provider
  backdrop still gets artwork instead of a gradient monogram.
- Continue Watching resolves the catalogue backdrop (or poster) for each progress entry and carries
  it as a non-persisted `WatchProgress.artworkUrl`, so the landscape cards show real artwork.
- Verified on the Fire TV: the hero shows the recent movie's image and the South Park progress card
  shows the series artwork.

### 5.15 Small polish pass (2026-10-24)

- **TMDB recommendation cache.** Resolved recommendation IDs are stored in the existing disk cache
  for 24 hours, so reopening a detail does not re-query TMDB. If TMDB recommendations don’t resolve
  to local titles, the provider falls back to same-genre catalogue titles so the rail still contains
  playable cards.
- **Episode stills verified.** Pulling the Room database after opening *Fear the Walking Dead*
  showed TMDB still URLs for 37 episodes, confirming the background season enrichment.
- **Live 403 copy.** A 403 on a live stream now says “This channel isn’t available right now…” with
  guidance to try another channel, instead of incorrectly asking the viewer to sign in again. VOD
  and series 403s keep the authorization copy.
- **Search.** Added All / Live / Movies / Series filters, kept recent-query chips and clear-all, and
  replaced `contains` matching with a fuzzy scorer: accent/punctuation-insensitive, token order
  independent, compact word-boundary matching and one-character typo tolerance. Candidate retrieval
  uses title-ordered `LIKE` queries so fuzzy matches are not lost behind “recently added” limits.
  Verified with `deapool` → Deadpool, Deadpool 2 and Deadpool and Wolverine.
- **Home.** Added a “Because you watched …” rail anchored to the most recent progress entry. It uses
  cached TMDB recommendations and falls back to genre matches when needed.

Verified on the Fire TV: fuzzy search results and filters, the Because rail, the live 403 text and
the cached recommendation path.

### 5.16 Smooth Home scrolling and usable player errors (2026-10-24)

- Home no longer calls `scrollToItem` on every vertical move. It asks the target row’s first card
  for focus and lets focus-driven bring-into-view scroll only as far as needed, with an animated
  `animateScrollToItem` fallback when the row is outside the composed window. Verified: moving from
  Live Now to Because keeps the previous row visible instead of snapping the target to the top.
- The player error overlay is now root-driven. While an error is shown, the playback key handler
  keeps focus and Left/Right selects Retry or Exit; Center activates the selected action. The
  Retry/Exit buttons draw the selection ring manually and are non-focusable, avoiding the native
  `PlayerView` stealing D-pad input. The player surface is detached while the error is visible.
  Verified: Right selects Exit, Center leaves the player.

### 5.17 Search IME state after Back (2026-10-25)

The TV search field kept `editing = true` when the system keyboard was dismissed with Back. Focus
moved to the top navigation, and returning Down to the search box re-focused the still-editable
field and reopened the IME.

- A `BackHandler` while editing clears edit mode, hides the IME and returns focus to the search
  launcher.
- An IME visibility effect also clears edit mode when the keyboard transitions from visible to
  hidden, covering the case where the system IME consumes Back.
- The field also clears edit mode when it loses focus (the observed focus move to the top nav).
- The text field remains non-focusable unless `editing` is true; the launcher is the D-pad target,
  so only an explicit Select opens the keyboard again.

Next: catch-up playback, release binding, phone/tablet pass.

## 6. Verification

### 6.1 Automated checks

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:lintDebug
./gradlew test
```

Minimum automated gate for this milestone: both variants assemble, lint has no fatal errors, and the
JVM tests pass. The manual demo flows below are a separate device gate.

### 6.2 Manual demo checklist

- [ ] Debug launch reaches Home without network or credentials.
- [ ] Home hero → Details → Play starts the bundled sample.
- [ ] Continue Watching appears after leaving the player mid-clip.
- [ ] Live → channel → player shows LIVE mode and no seek bar.
- [ ] Live → Guide → day/channel selection works.
- [ ] Search → result → Details → Play works offline.
- [ ] My List add/remove survives process restart.
- [ ] Settings changes persist; language switches EN/ES; sign-out returns to login.
- [ ] Release build shows "Service not configured" and offers no sign-in.
- [ ] D-pad focus is visible on TV; back never traps the user.
- [ ] Player shows the placeholder marker at all times.

### 6.3 TV feedback from the first device run (2026-10-23)

The debug APK was installed on an Android TV and the overall app was accepted. Three TV issues were
reported and fixed in the same milestone:

| Report | Cause | Fix |
| --- | --- | --- |
| "Too big, can't see the titles or info below the boxes." | TV posters were a fixed 196 dp (only four fit a 960-1080 dp viewport) and the hero was 500 dp tall. The focus target covered only the artwork, so bring-into-view could leave the caption below the viewport. | Home now derives six poster columns from the actual viewport width (120-176 dp posters). The TV hero is 48% of the viewport height (280-340 dp) with compact type and no extra 80 dp. The whole card (artwork + title + metadata) is one focus target, so the caption scrolls into view with the poster. |
| "Going down in Search returns to the search box and the keyboard opens again; Next opens the keyboard again." | The IME action did not submit or dismiss the keyboard, focus stayed in the text field, and Down wrapped back to the field. | Search now submits on Search/Next/Done/Go, hides the IME and moves focus to the first result. With the keyboard dismissed, Down moves into the results; returning to Search restores focus to the results instead of reopening the keyboard. |
| "The Play/Resume button does not change colour when selected." | Material 3 buttons had no focus or hover colours. | Primary and secondary buttons now brighten on focus/hover (`accent/hover`, `bg/surface-3`) and icon buttons switch to `accent/primary`; all buttons draw the TV focus ring. |

A second device pass on the same TV (Fire TV AFTMM, 1920x1080 UI) reported two follow-ups, fixed and
verified on the device:

| Report | Cause | Fix |
| --- | --- | --- |
| "The hover of some buttons looks pixelated." | The 1.04 focus lift was applied to buttons, so the whole button layer (text, icon, ring) was rasterised once and then scaled up. | Focus lift removed from buttons; focus is now fill colour plus a 2 dp amber ring drawn outside the button with a 2 dp background gap and a 1 dp dark outer stroke (`sunsetFocusRing(gap = 2.dp)`). Cards keep the flush ring and the 1.04 lift on the artwork only. |
| "Going up and down over the search box opens the keyboard every time." | The TV search field was a focusable text field, so gaining focus showed the IME. | The TV search box is now a plain focus stop (`canFocus = editing`); only Select/Enter on it enters edit mode and shows the IME. D-pad traversal passes over it without the keyboard, Down moves into the results, and submitting from the IME (Search/Next/Done/Go) hides the keyboard and focuses the first result. |
| Result group header was half clipped once the first result took focus. | The redundant "Search" title pushed the results viewport below the height of one poster group, so bringing the focused card into view scrolled its section header off the top. | The "Search" title is hidden on TV (the nav pill and field placeholder already say it), which gives the results the full height and keeps the group header, poster and caption on screen together. |

### 6.4 Fire TV live-provider smoke pass (2026-10-23)

Current debug build tested on the same Fire TV AFTMM (1920x1080 UI):

- Session restored after reinstall; Home and Live TV loaded 1,130 channels and 61 categories.
- D-pad category/navigation and Back returned between More, Guide, Settings and top-level tabs.
- Guide channel rows fit on screen and correctly report that EPG data is not available yet.
- Search `Travel` returned two matching live channels; Select opened the keyboard, Next dismissed it
  and moved focus to the first result. Up/down traversal did not open the keyboard. Test history was
  cleared afterward.
- A real live stream played on the current build with the `LIVE` state, no seek bar and no demo
  watermark. Channel logos loaded after adding Coil's OkHttp network fetcher.
- My List's empty state and Settings account rows fit. Movies/Series now explain that the live
  provider catalogue is not connected in this version; demo copy remains separate.
- Debug/release assemble, lint and JVM tests pass (0 lint errors; 10 tests).

The device pass was TV-only; no phone/tablet was connected. Guide is intentionally empty until EPG
is implemented. Release cleartext remains limited to the panel hostname, so playback through its
rotating raw-IP redirect is not release-ready; debug allows cleartext for testing.

### 6.5 Toolchain status

Verified on 2026-10-23 on Windows with a portable toolchain:

| Component | Value |
| --- | --- |
| JDK | 17.0.20.1+1 (Temurin) |
| Android SDK | `platform-tools`, `platforms;android-37.0`, `build-tools;36.0.0` |
| Gradle | 9.3.1 (wrapper) |

Command results:

| Command | Result |
| --- | --- |
| `:app:assembleDebug` | **BUILD SUCCESSFUL**; `app/build/outputs/apk/debug/app-debug.apk` |
| `:app:assembleRelease` | **BUILD SUCCESSFUL**; `app/build/outputs/apk/release/app-release-unsigned.apk` |
| `:app:lintDebug` | **BUILD SUCCESSFUL**; 0 errors, 18 warnings (version notices and manifest attributes) |
| `test` | **BUILD SUCCESSFUL**; 10 JVM tests in `:core:data`, 0 failures; other modules have no test sources |

The release APK is unsigned by design; add a signing config outside the repository before installing
it on a device.

Lint note: AGP 9.1.1's `PropertyEscape` check reports a false positive for an absolute Windows
`sdk.dir` in `local.properties`, even when correctly escaped. `app/lint.xml` ignores that issue for
the machine-local, gitignored file only; all other lint checks remain enabled. The verified run used
`ANDROID_HOME`; a re-run with `local.properties` present also passed.

Host setup:

1. Install JDK 17 and set `JAVA_HOME`.
2. Install command-line tools, then:
   ```bash
   sdkmanager --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
   sdkmanager --licenses
   ```
3. Set `ANDROID_HOME`, or create `local.properties` with `sdk.dir=...` (never commit it).
4. Run the Gradle commands above.

A physical Android TV run has been completed for the Home and Search flows (§6.3). The remaining
§6.2 items still need a full device pass; this environment has no emulator image, TV emulator or
authorised device attached.

## 7. Sample media provenance

`core/player/src/main/res/raw/sample_placeholder.mp4` was generated locally for this repository:

```bash
ffmpeg -f lavfi -i color=c=0x08090C:s=640x360:d=12:r=30 \
  -vf "drawtext=fontfile=...:text='SUNSET DEMO — bundled sample — not provider content':fontcolor=0xF5A524:fontsize=18:x=(w-text_w)/2:y=(h-text_h)/2" \
  -c:v libx264 -pix_fmt yuv420p -c:a aac -shortest sample_placeholder.mp4
```

The baked-in text is media content, not UI copy. Replacing it with real provider content is out of
scope until provider access is approved.

## 8. Known limitations

- The live panel works in **debug** only; the release build still binds the fail-closed adapter.
- Catch-up/archive playback is not implemented against the live panel yet.
- The first catalogue sync still parses the 16.6 MB VOD payload; later launches read Room.
- Country aliases are panel-specific and intentionally conservative; unmapped channels fall under
  “Other”.
- TMDB enrichment requires a key in `local.properties`; without it the app runs with provider
  metadata only. Matching is conservative, so obscure or renamed titles may stay unenriched.
- Parental controls and profiles are placeholders.
- Autoplay-next works for series episodes; cross-content autoplay is not implemented.
- Track selection, PiP and aspect-ratio switching are settings placeholders.
- No cast, downloads, DVR, multi-screen, reminders, radio or speed test (out of scope).
- R8 is disabled; release shrinking and mapping upload are pending.
- No screenshot or UI tests yet. Fifteen JVM tests cover fake-provider login, paging, filtering,
  search, stream resolution, guide generation and country parsing. Paging 3 UI, Media3 raw-resource
  playback, deep links and source-set Hilt binding still need device or instrumentation verification.

## 9. Next three tasks

1. **Catch-up/archive playback.** Use the panel's `tv_archive` flags and programme timestamps to
   offer “watch from start” in the guide and player.
2. **Release hardening.** Add a signing config outside the repo, enable and test R8 with keep rules
   for serialization/Media3, and resolve the cleartext stream-host policy.
3. **Phone/tablet pass and quality gates.** Verify live and demo on a phone/tablet, fix the narrow
   guide column, and add screenshot/UI tests for login → live → details → playback.

Later milestones: parental PIN and category locks; profile picker and per-profile history; PiP and
track selection; R8 and baseline profiles; screenshot tests.
