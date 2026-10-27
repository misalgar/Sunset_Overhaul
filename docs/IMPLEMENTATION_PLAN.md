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
- Field focus loss during an edit session (excluding the one at edit start) schedules a refocus of
  the launcher. The navigation also restores focus after Back, so the refocus keeps asking until
  the box has held focus for two consecutive checks instead of stopping at the first success.
- Up/Down/Submit clear the edit session before moving, so leaving the field never triggers the
  refocus. Down only leaves the field when it has a result/recent group to enter.
- The text field remains non-focusable unless `editing` is true; the launcher is the D-pad target,
  so only an explicit Select opens the keyboard again.

Verified on the Fire TV, with an empty and a typed query: Select opens the IME, Back closes it and
leaves the amber focus ring on the search box, Up still reaches the top navigation, Down returns to
the box without reopening the IME, and Select reopens it. Submitting from the keyboard still moves
focus into the first result group.

Next: catch-up playback, release binding, phone/tablet pass.

### 5.18 Live TV master-detail overhaul (2026-10-25)

The old single-column Live screen had 45+ horizontal category chips, a duplicated On Now row and a
1,194-row list that took dozens of D-pad presses to cross.

- **Layout:** TV/tablet use a 27/73 master-detail split. The left sidebar pins All channels and
  Favourites, then groups provider categories as Genres / Live Events / Countries & Regions with
  muted, non-focusable headers. The right pane is a 2-column `LazyVerticalGrid` (8 tiles visible).
  Phones keep the chip row and dense list.
- **D-pad:** Right or Select enters the grid at row 0; Left from the first column scrolls and
  focuses the selected category; focusing a category previews it after a 250 ms debounce. The grid
  and sidebar retry focus while the new content composes.
- **Cards:** channel numbers are gone. Logos sit in a 64 dp framed `White 6%` surface; the text
  column shows name, current EPG title (or "Now Playing"), time + LIVE badge, a 2 dp progress bar
  and a far-right favourite star. Holding Select toggles the favourite without a second focus stop.
- **Cleanup:** provider instructions (`(presiona …)`) are stripped from live category and channel
  names; `uso interno` / `banned` categories and their channels are dropped. Adult channels are
  hidden app-wide (Home, Search, Guide) and only appear under the explicit Adultos row, which
  aggregates the provider's adult category when one exists.
- **Guide button / surfaces:** the guide button is a dark elevated surface that turns amber on
  focus; the divider is replaced by a rounded `White 3%` sidebar container.
- **Known limitation:** the provider's XMLTV payload currently contains channels but **zero
  programmes**, so every tile shows "Now Playing" with no time range or progress bar. The EPG
  fields are wired; they populate as soon as the panel returns a schedule.

Verification: Fire TV screenshots for the grouped sidebar, focus preview, Left/Right restoration,
long-press favourite (star fills, Favourites count increments, playback does not start), the
Adultos row (54 channels) and the guide button focus state.

Next: channel availability/offline states, catch-up playback, release binding, phone/tablet pass.

### 5.19 Advisory event schedules and full titles (2026-10-25)

The panel's XMLTV payload has 973 channels but zero programmes, so every live tile used to claim
LIVE — including event feeds such as `21;10 Tijuana vs Santos` that are not active yet.

- `parseLiveEventSchedule` reads the leading panel time from the channel name (`:` or the panel's
  occasional `;`), the date from the category (`NFL 25/10/26`) or a matching weekday (`Domingo`),
  and converts it with the panel clock/timezone from `server_info`. It ignores names without a
  time and weekday categories on the wrong day, and strips the time from the display name.
- Cards show **Upcoming event** + `Starts 9:10 PM` (viewer's local time) ten minutes before the
  start; otherwise the existing LIVE badge. There is deliberately no Ended state and playback is
  never blocked — the stream remains the final authority (no probing, no extra requests).
- The player failure overlay adds the scheduled time via a memory-only `cachedLiveChannels()`
  lookup (no network call), so an early attempt explains itself.
- Channel/event names in the tiles and category labels in the sidebar wrap to two lines, so
  `Steelers vs. Saints`, `Los Angeles FC vs LA Galaxy` and `Live Soccer Domingo` are readable
  without ellipses.

Verified on the Fire TV: Tijuana vs Santos shows `Upcoming event / Starts 9:10 PM`, an earlier
kick-off shows LIVE, and opening the upcoming feed reports `This event is scheduled for 9:10 PM.`

Next: catch-up playback, release binding, phone/tablet pass.

### 5.20 Guide EPG outage banner (2026-10-25)

The panel's `xmltv.php` feed shrank from the ~3.2 MB document recorded in `198ed58` (channels and
`<programme>` entries) to a 152 KB channels-only file. A fresh fetch after deleting the cache
returned 976 `<channel>` entries and **0 `<programme>` entries**, so the Guide correctly has no
schedule cells. The same account shows no listings in another IPTV app, confirming a provider-side
change; `get_short_epg` / `get_simple_data_table` were already known to return no listings on this
panel.

- The Guide now shows one “Guide data isn't available from the provider. Channels are still
  playable.” banner above the ruler when the loaded guide has no programmes.
- Per-row “No guide data” labels are suppressed in that state; channel names use the freed space.
- Rows, logos, day tabs, country filter and the time ruler remain functional.

Next: catch-up playback, release binding, phone/tablet pass.

### 5.21 Signed release and in-app OTA (2026-10-25)

The family/friends distribution is now a signed release with an in-app updater; full procedure in
`docs/RELEASING.md`.

- Release binds the production `XtreamContentProvider` (no demo router), allows the panel's HTTP
  hosts, and is signed from a keystore outside the repo (`sunset.signing.*` in untracked
  `local.properties`). Version `0.1.0` (`versionCode` 1), R8 off.
- `UpdateManager` checks a public `latest.json` once per day (plus Settings → Check for updates),
  downloads the APK, verifies SHA-256 and the signing certificate against the installed app, then
  hands it to the Fire TV installer. The update UX is a D-pad dialog with notes, Install and Later.
- Manifest: `https://raw.githubusercontent.com/misalgar/sunset_overhaul-releases/main/latest.json`;
  APKs live as assets on `misalgar/sunset_overhaul-releases`.
- `scripts/build-release.ps1` builds, verifies, copies the artifact, computes the hash and rewrites
  the releases repo manifest in one step.
- Verified locally: signed `app-release.apk` with the Sunset certificate, 41 unit tests, lint
  0 errors. No device test was performed because the TV is in use.

Pending publish: upload `build/release-artifacts/sunset-0.1.0.apk` to the GitHub release tag
`v0.1.0`, then commit/push `latest.json` from the releases repo.

### 5.22 Release startup fix (2026-10-26)

The first signed release (0.1.0) showed **Service not configured** on a fresh TV instead of the
login screen. `AppViewModel.bootstrap()` still applied the old fail-closed rule: a
`ProviderStatus.NotConfigured` provider meant a blocked build. The release now binds the live
`XtreamContentProvider`, which reports NotConfigured until credentials exist — so the gate ran
before login and never fell through to it.

- Removed the startup gate; no stored session now always reaches the login screen. The
  `ServiceNotConfigured` state remains only for a provider that fails during session restore.
- The daily OTA check now starts as soon as bootstrap resolves, not only after login, so a future
  pre-login regression can still receive a fix.
- Built **0.1.1** (`versionCode` 2), signed with the same keystore; manifest updated to 0.1.1.
- 0.1.0 cannot self-update because it never reaches the update check: install 0.1.1 manually once
  on the affected TV. Subsequent releases use OTA normally.

Next: publish 0.1.1, catch-up playback, phone/tablet pass.

### 5.23 Live ordering, nav entry focus and Home skeletons (2026-10-26)

Four navigation/loading rough edges reported after the first releases:

- **Live Events first:** `LiveCategoryGroup` now orders LiveEvents, Genres, Countries, so the dated
  sports feeds sit directly under All/Favourites instead of below every genre.
- **No sidebar jump:** Left from the grid only scrolls the sidebar when the selected category is
  off-screen. Previously `animateScrollToItem` always snapped it to the top. All/Favourites are
  also pinned above the scrolling category list, so they stay visible.
- **Down from the top nav lands on All:** MainShell provides a content-entry `FocusRequester`; the
  nav's Down handler requests it. `SunsetFilterRow` (Movies/Series), the Live All row, and the
  PPV/MyList All chips attach it, so entering those screens no longer follows the nearest
  geometric chip (`Estrenos Septiembre`).
- **Home loading rails:** `HomeUiState` exposes `loadingLive`, `loadingCatalogue` and
  `becauseLoading`; Home renders titled skeleton rails (and a hero skeleton) until each part of the
  first catalogue sync completes, instead of leaving sections invisible.

Checks: debug build, 41 unit tests, lint 0 errors. Fire TV verified: Live Events first, Down from
the nav focuses All, Left from the grid leaves the sidebar position unchanged, and Movies Down
lands on All. The Home skeletons were not captured because the cached catalogue loaded too fast;
they appear during the first-login catalogue sync.

Next: verify the Home skeletons on a fresh login, then publish 0.1.2.

### 5.24 Home row focus recovery (2026-10-26)

After scrolling Home and moving Right/Left inside *Recently added in Series*, Up stopped working.
The row focus key was cleared when the old card reported focus loss after the new card had already
reported focus gain; Up then fell through to geometric search and found nothing above.

- Home tracks the **focused item identity** (`rowKey` + item key). Only the item that still owns
  focus can clear the row state, so moving within a LazyRow no longer loses the row.
- `SunsetContentRow` attaches the row focus requester to the **first visible card** rather than
  index 0, so a horizontally scrolled row can still be entered with Up/Down.
- The Home hero Play button reuses the shared nav entry requester, making Down from the top bar
  deterministic (the previous missing target left focus on the nav).

Fire TV verified: series → Right ×2 → Left → Up focuses *Recently added in Movies*; a second Up
focuses *Recommended for you*. Home → Down from the nav focuses the hero Play button.

Checks: debug build, 41 unit tests, lint 0 errors.

### 5.25 Live card and sidebar polish (2026-10-26)

- **Compact live cue:** the wide LIVE pill on grid tiles is replaced by a 6 dp red dot beside
  the programme title (or `Live` when there is no EPG). Show titles no longer lose ~43 dp to the
  pill.
- **Card surface:** #161B24 slate with a subtle white border, #202836 when focused, and a solid
  white 50 dp logo badge so JPEG logos with white backgrounds look intentional.
- **Progress:** the bottom bar is inset 12 dp / 4 dp and uses translucent white rather than yellow,
  so it cannot be mistaken for the focus ring.
- **Sidebar:** Live Events now precedes Genres again; `Live Soccer <Day>` becomes
  `Soccer • <Day>`; labels are single-line and rows, headers and pinned filters share a 12 dp left
  margin.
- **Hint:** “Press ◀ for categories” only appears while focus is inside the channel grid.
- **Logo:** the symbol is now a clean inverted triangle with a negative-space sun; the tiny `S` and
  horizon cut-line are gone.
- **Update dialog:** the primary action retries focus over the first frames after the modal opens,
  so the first Select lands on Install.

Fire TV verified: new card surface, dot + full show titles, white logo badges, Live-Events-first
sidebar, and the hint appearing only from the grid.

Checks: `assembleDebug`, `testDebugUnitTest` (41 tests), lint 0 errors.

Next: publish 0.1.2, catch-up playback, phone/tablet pass.

### 5.26 Provider language/quality labels (2026-10-26)

The Xtream list endpoints have no language/quality field; the panel encodes them as trailing tokens
in `name` (`Mayday (2026) LAT`, `The Runner (2026) SUB`, `… DUAL`, `… 4K`). Duplicate cards were
distinct stream ids in the same category with the same poster; the movie cleaner removed the only
differentiator.

- `parseProviderVariant` extracts trailing language (`LAT`, `LATINO`, `SUB`, `DUB`, `DUAL`, `ESP`,
  `ING`, `CAST`, `VOSE`) and quality (`HD`, `FHD`, `UHD`, `4K`, `SD`, `1080P`, `720P`) tags,
  supports bracketed/combined tags, and returns the clean title.
- `Movie` and `SeriesSummary` now carry `language`/`quality`; the catalogue schema adds the same
  columns (DB v3, non-destructive migration, catalogue meta reset to force a one-time re-sync).
- Card badges show the variant (`LAT`, `SUB`, `ESP`, `4K`, `LAT · 4K`) on Movies, Series, Home
  rails, Search, My List and the details hero, so duplicates are distinguishable. Series titles are
  cleaned as well.
- Unit tests cover the parser and label formatting.

Fire TV verified after the automatic catalogue re-sync: The Senior Class (`LAT`/`SUB`), The American
Dream (`ESP`/`SUB`) and the other variants now carry clear badges.

Next: group variants under one card with a details version picker.

### 5.27 Release 0.1.2 (2026-10-26)

Release contents since 0.1.1:

- Live TV card and sidebar polish: compact live dot with programme titles, deep slate cards,
  white logo badges, calm inset progress bar, Live Events first, `Soccer • Day`, single-line
  aligned sidebar labels, grid-only categories hint.
- Home: titled loading rails, Home row focus recovery, deterministic Down from the top nav.
- Guide: single EPG-unavailable banner; the provider's guide data has since returned.
- Search: Back returns focus to the search box.
- Provider language/quality badges (`LAT`, `SUB`, `ESP`, `4K`) with a DB v3 migration and one-time
  catalogue resync.
- OTA startup check before login.

Built as version `0.1.2` (`versionCode` 3), signed with the Sunset release key. Artifact:
`build/release-artifacts/sunset-0.1.2.apk`, SHA-256
`990b77a70dd14bd3ad6db5c5bcd87830e418365ef033f2313c311e02ec3d6d47`.

Publish: create the `v0.1.2` GitHub release with the APK, then push the updated `latest.json`.
0.1.1 clients will prompt; 0.1.0 clients still need one manual install.

Checks: `testDebugUnitTest` (45 tests), `assembleDebug`, lint 0 errors.

### 5.28 Movie versions: one card, version picker, preferred language (2026-10-26)

Movies that exist in several provider versions (`LAT`, `SUB`, `ESP`, `DUAL`, `4K`) now show one card.
The details screen lists the versions and Play uses the one selected.

- Grouping (`assignVariantGroups`, pure, unit-tested): rows share a normalised title key
  (title + year). A bucket with one TMDB id is one group, including rows TMDB has not matched yet.
  Buckets with conflicting TMDB ids split by id, so remakes never merge.
- One default per group, chosen by the viewer's preference (`LATINO`: LAT, ESP, SUB, DUAL;
  `SPANISH`: ESP, LAT, SUB, DUAL; `SUBTITLED`: SUB, LAT, ESP, DUAL), then quality (4K > FHD > HD),
  then newest. Grids, search and genre rails read only `isDefault = 1` rows.
- DB v4 (`Migration3To4`): adds `titleKey`, `groupKey`, `isDefault` and indices, and fills `titleKey`
  in place. No resync. `CatalogueVariantIndex` assigns groups and defaults in the background
  (debounced); it writes only rows that changed and bumps a revision only when a visible default
  flips, so open grids don't reload on every TMDB match.
- Resyncs keep TMDB enrichment (`withTmdbState`), so the six-hourly refresh no longer re-matches
  the catalogue.
- Details: a Versions row appears for multi-version titles. Selecting a version reloads that
  provider row; Play, progress and favourites follow the selected id.
- Settings → Playback → Preferred version (persisted in DataStore). Changing it re-derives defaults
  and the open Movies grid reloads when it is next shown.
- Series still show one card per provider row, with badges. Grouping series is the next step.

Known limits: favourites and watch progress are stored per provider id, so they stay with the
version used. Home rails refresh on the same revision signal and may briefly reload.
Verified on Fire TV: v3→v4 upgrade in place, duplicate cards collapse, versions row switches the
hero's language, the preference changes the default after a relaunch.

Follow-up: movie cards no longer show the language/quality badge (the version is chosen on
details). The details hero shows it only when a title has more than one version. Series keep their
badge until they are grouped the same way.

### 5.29 Live playback: zapping, Back focus, PPV tiles (2026-10-26)

- Up/Down in the live player changes channel through the list the viewer came from
  (`LivePlaybackQueue`, set by Live/Pay Per View with the visible order and category). It wraps at
  the ends, works while the error overlay is up, and adds no provider requests: it only resolves
  cached channels.
- The video surface is no longer focusable, and the player root re-requests focus whenever
  playback state changes, so D-pad keys are never swallowed after a reattach.
- Back from playback returns to Live with the watched channel scrolled into view and focused, in
  the same category. `LivePlaybackQueue.lastPlayedChannelId` survives navigation.
- Pay Per View uses the same 2-column `SunsetLiveChannelTile` grid as Live TV: event start times
  ("Starts …"), live status, EPG progress and the event name. `rememberChannelStatus` is shared so
  the two screens cannot drift. PPV zapping stays inside the event list.
- The error overlay names the failed channel and shows the D-pad zapping hint on live.

### 5.30 Live hero preview (2026-10-26)

- A guide-style hero occupies ~32% of the content area above the master-detail layout: focused
  channel name, current programme title, time range and up to three description lines.
- The right side is a 16:9 panel with a bottom gradient and a 2 dp live-progress line. It shows the
  programme/EPG art immediately and switches to a muted video preview once the viewer rests on the
  channel for 1.5 s (`LiveViewModel.focusedChannel` / `previewChannel`).
- `LivePreviewViewModel` owns a separate muted `PlaybackEngine` (new `setMuted` on the engine) and
  pauses on `ON_STOP`, so no decoder runs behind the full-screen player or another destination.
- Focus wiring: the tile's own `onFocusChanged` reports the channel; leaving the grid clears both
  focus states and stops the preview. Focus is also cleared on category changes.

Verified on Fire TV: zapping across Trev→MegaTV→Telemundo (including dead feeds), Back restoring
Telemundo San Juan with its 6–8 PM guide entry, the PPV tile grid, and the preview starting after
the rest delay and resuming after Back.

### 5.31 Live TV grid: full-height sidebar and compact hero (2026-10-26)

The hero used to span the full width above the 2-pane row, which squeezed the sidebar to a handful
of categories and the channel grid to 4–6 tiles.

- The wide layout is now a single `Row`: a 240 dp full-height sidebar, then the right pane with the
  hero, a 12 dp spacer and the grid. The sidebar shows 9+ rows without scrolling.
- The separate "All channels / 976 channels" header row is gone. The category name and channel
  count are the hero breadcrumb (`ALL CHANNELS • 976 channels`), with a compact `◀ Categories`
  hint on the right when the grid has focus.
- The hero is a 176 dp card (`#141923`, 14 dp radius, 1 dp white 8% border) with no outer padding.
  The metadata column uses a 44 dp white logo badge, a 22 sp channel name, programme title plus
  time range, and up to two description lines (or the "Press OK" hint). The 16:9 preview fills the
  card height (~313 dp wide), bleeds to the right edge and blends into the card with an 80 dp
  horizontal gradient. The bottom gradient keeps the live progress line and "X min left".
- Channel tiles are 64 dp tall with a 44 dp logo badge and a single-line title; the grid uses
  10 dp horizontal and 8 dp vertical gaps and full-height slate surfaces (`#161C26` /
  `#202938`). Four full rows (eight channels) are visible on a 1080p Fire TV.
- `SunsetChannelLogoBadge` is shared by the tile and the hero so the white badge never drifts.

Verified on Fire TV: full-height category list, hero tracking focus with the programme description,
preview, progress and "13 min left", and four complete channel rows above the fold.

### 5.32 Fix dead D-pad on Home after Live TV (2026-10-27)

Symptom: after visiting Live TV and going Back, Home could ignore the D-pad. Three causes stacked
up, and each one alone was enough to fake a working fix:

1. **The entry requester was never bound to the rails.** The `Recently added in Movies/Series`
   LazyColumn items used the keys `recent-movies` / `recent-series`, while the focus logic
   (`visibleRowKeys`, `sectionKeys`, `entryRowKey`, `fullyVisibleRowKey()`) used
   `recentMovies` / `recentSeries`. With the hero scrolled away, nothing matched, so the shell's
   requester was attached to no card. The two rails are now keyed consistently.
2. **`requestFocus()` success was trusted.** Compose reports success for a request aimed at a
   stale or not-yet-placed node, so the early-exit loops stopped while focus still sat on the nav
   chip. Focus moves are now verified (`focusedRowKey`) after two frames and retried.
3. **Touch mode left the window without a focused Android view.** After `adb input tap` (or any
   touch on a hybrid remote) the window stayed in touch mode, `root.findFocus()` was `null`, and
   Compose silently ignored focus requests until a key press. `requestFocusFromTouch()` restores
   view focus so the requests land.

- Home attaches the shell's requester directly to the first card of the first visible rail (and to
  the hero Play button while the hero is on screen). The old 1 dp anchor and its hand-off are gone:
  a node cannot move focus while it holds focus, so the hand-off was unreliable by construction.
- `SunsetContentRow` takes an extra `entryFocusRequester`, so one card can serve both row
  navigation and the nav bar's Down key.
- `MainShell` resets its focus flag, re-focuses the Compose view, and re-requests the entry target
  on every top-level route change until the content really holds focus (12 attempts).
- Home's load-time restore effect retries with its own row requesters rather than the shell's
  shared one, which can still point at a node from the screen just left.

Verification on Fire TV (1920x1080, density 320): remote-only Back from Live restores the hero
Play ring and Down walks the rails; tap-driven (touch mode) Back restores a visible card ring and
Down moves to the next rail. `:app:assembleDebug testDebugUnitTest :app:lintDebug` passes with
lint errors = 0.

### 5.33 Every top-level screen must bind `LocalContentEntryFocus` (2026-10-27)

Symptom: More and Search behaved like the earlier Home bug — the nav bar's Down key did nothing, so
the remote felt dead on those two screens.

Cause: `MainShell` owns one shared `FocusRequester` (`contentEntryFocus`) and publishes it through
`LocalContentEntryFocus`. Its Down handler on the top navigation bar calls `requestFocus()` on that
requester. When a screen never attaches the requester to a node, the request does not throw in this
Compose version — it simply does nothing — so the key is consumed with nowhere to go. Home, Live,
Movies, Series, PPV and My List bound it; More and Search were the only top-level routes that did
not.

- `MoreScreen` attaches the requester to the first row (Guide) via a new `modifier` parameter on
  `MoreRow`.
- `SearchScreen` attaches it to the search launcher box, or to the text field while the viewer is
  editing. Both are already focus stops on TV, so no extra node was added.
- Rule for new screens: any route in `LargeTopLevel` / `PhoneTopLevel` must read
  `LocalContentEntryFocus.current` and put `Modifier.focusRequester(entryFocus)` on the control the
  viewer should land on first. An unbound requester is invisible in code review and only shows up as
  a dead Down key on the device.

Verification on Fire TV (1920x1080, density 320): More lands on Guide with a ring, Down moves to
PPV, Back from Guide returns with Guide focused. Search lands in the search box with a ring, Up to
the nav then Down returns to the box, and a `verity` search focuses the first result; Up from the
results lands on the filter row and Down returns to the first result.
`:app:assembleDebug testDebugUnitTest :app:lintDebug` passes with lint errors = 0.

### 5.34 The Search clear button must be a focus stop on TV (2026-10-27)

Symptom: with a query in the Search box, the remote could not reach or activate the X, so the only
way to clear a search was to reopen the keyboard and erase it.

Cause: the X is `SunsetSearchField`'s `trailingIcon`, which lives inside the text field. A text field
consumes Left/Right for the cursor, so nothing inside it can take focus from the D-pad — and while
the box is not in edit mode the field itself is not even focusable. The X was therefore reachable
with a mouse or finger, but invisible to the remote.

- `SunsetSearchField` takes `showClearAction` (default true). When false it hides its own icon and
  reserves the same 48 dp so the text cannot run under the caller's control.
- `SearchScreen` passes `showClearAction = !isTv` and, on TV, overlays a `SunsetFocusable` circle in
  the same place: Right from the box focuses it, Select clears the query and returns focus to the
  box (the X disappears with the query), Left goes back to the box, Down goes to the results, Up
  leaves for the nav. The box's key handler ignores keys while the X holds focus so Select cannot
  re-open the IME instead of clearing.

Verification on Fire TV (1920x1080, density 320): Right from a filled box rings the X; Left returns
to the box; Select on the X empties the field, hides the X and leaves the box focused with the recent
searches showing; Down from the X focuses the first result. The phone path still uses the built-in
trailing icon.

### 5.35 The login keyboard must stay closed after Back (2026-10-27)

Symptom: on the login page, Select opened the keyboard on a field, Back closed it, but navigating
back down to that field re-opened the keyboard without another Select — so the only way out was the
IME's Next key.

Cause: the Fire TV IME consumes Back itself. The app's `BackHandler` never fires, and the floating
IME window reports no IME insets (`WindowInsets.isImeVisible` never becomes true), so the screen
never learned the keyboard had closed and kept `editingField` set. The field therefore stayed
editable, and an editable text field asks for the IME as soon as it regains focus.

- `LoginScreen` now tracks `focusedField` through `onFocusChanged` on each input.
- When an edited field loses focus and has not regained it after 400 ms, the keyboard was dismissed:
  `stopEditing()` runs, the effect waits two frames so the field recomposes as read-only (focusing a
  still-editable field is itself a keyboard-show request), and then focus is restored to that field
  with the same `requestFocusFromTouch()` + retry pattern the shell uses. The 400 ms wait also lets
  a legitimate IME Next/Previous move between fields settle first.
- The form's focus order now skips stops that cannot take focus (the Sign in button while the form
  is incomplete, the demo link while submitting). Before, Up from the demo link dead-ended on the
  disabled Sign in button and the D-pad looked broken.

Verification on Fire TV (1920x1080, density 320): Select opens the IME on Server; Back closes it and
the ring stays on Server; Down and Up return to Server without the IME; Select re-opens it; IME Next
moves to Username with the keyboard still open; Back there also leaves the ring on Username. With the
form incomplete, Down from Need help reaches the demo link and Up from it returns to Need help, both
skipping the disabled Sign in button.

### 5.36 Live events and PPV must show when they started (2026-10-27)

Event feeds carry their start time in the channel name. `XtreamContentProvider` already parses it
once at load time with `parseLiveEventSchedule(...)`, stores `LiveChannel.scheduledStartUtcMs`, and
replaces the name with the clean matchup. The Live tile only kept the parsed time while the event
was upcoming, so once it began the status dropped to `Live` and the start time was lost.

- `core/model` now owns the grace rule: `isUpcomingEvent(startUtcMs, nowUtcMs)` and
  `LiveEventSchedule.isUpcoming(nowUtcMs)` both compare against `LIVE_EVENT_START_GRACE_MS`
  (10 minutes). The parser and the UI share one definition instead of repeating the arithmetic.
- `ChannelStatus` is a sealed interface. `Upcoming(startsAtEpochMillis)` is returned while the
  grace has not run out; `Live(programmeTitle, startedAtEpochMillis, progress)` keeps the event
  start after kick-off, falling back to the current EPG programme's start when the channel has no
  parsed event. `channelStatus(...)` is a pure function so it is testable without Compose.
- `SunsetLiveChannelTile` receives `startsAtEpochMillis` / `startedAtEpochMillis` rather than a
  pre-formatted label. The tile formats the clock with `SunsetFormat.time(context, ...)` itself,
  which keeps `core/ui` independent of `feature/live`.
- Status text uses the new `live_started_at` (`Live • Started %1$s` / `En vivo • Inició %1$s`) and
  `live_programme_started_at` (`%1$s • Started %2$s` / `%1$s • Inició %2$s`) strings. A 6 dp red
  `SemanticLive` dot stays inline before the text, with one line and an ellipsis.
- PPV uses the same tile and `channelStatus(...)`, so its events show the clean matchup plus
  `Live • Started …` or `Starts …` without a second regex or any extra request.
- `LiveHeroPreview` wraps the title to two lines and drops from 22 sp to 18 sp above 20 characters,
  so matchups such as `Fleetwood Town vs Arsenal` no longer truncate. When the EPG has no title it
  shows the event's own `Starts …` / `Live • Started …`.
- The TV grid clips its top edge and adds 4 dp of top content padding, so a partially scrolled card
  no longer draws over the hero's bottom border.

Verification on Fire TV (1920x1080, density 320): Soccer • Martes tiles read `Live • Started
6:15 a.m.`; a future event reads `Starts 4:00 p.m.`; the hero shows the full `Beijing Guoan vs
Jeonbuk Motros` on two lines with `Live • Started 6:15 a.m.`; PPV shows both `Live • Started 3:00
a.m.` and `Starts 8:00 p.m.`; a normal channel reads `Family Guy • Started 3:30 p.m.`. Unit tests
cover a past event, the grace boundary, a 24/7 channel's EPG fallback and a programme that has not
started. Full gate: `:app:assembleDebug testDebugUnitTest :app:lintDebug` passes with 0 lint errors
and 19 warnings.

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
