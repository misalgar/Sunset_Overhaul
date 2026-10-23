# Sunset Overhaul — Product Requirements & Design Specification

| Field | Value |
|---|---|
| Document | Sunset Overhaul — PRD + Design Spec |
| Version | 0.1 (draft for review) |
| Date | 2026-10-23 |
| Status | Draft — decisions locked, API details still to confirm |
| Product | Sunset streaming client (Android phone, tablet, TV) |
| Rebuild basis | Clean rebuild from APK analysis; original source unavailable |
| Primary source | Existing EZServer/custom provider API |
| Design reference | tflix.box screenshots supplied by stakeholder |

---

## Contents

- [0. Decisions locked](#0-decisions-locked)
- [1. Executive summary](#1-executive-summary)
- [2. Current app analysis](#2-current-app-analysis)
- [3. Product definition](#3-product-definition)
- [4. Scope and release plan](#4-scope-and-release-plan)
- [5. Information architecture](#5-information-architecture)
- [6. Design principles](#6-design-principles)
- [7. Brand and visual identity](#7-brand-and-visual-identity)
- [8. App shell](#8-app-shell)
- [9. Screen specifications](#9-screen-specifications)
- [10. Component library](#10-component-library)
- [11. Content, metadata and editorial](#11-content-metadata-and-editorial)
- [12. Data and API architecture](#12-data-and-api-architecture)
- [13. Playback architecture](#13-playback-architecture)
- [14. Performance and quality](#14-performance-and-quality)
- [15. Security, privacy and compliance](#15-security-privacy-and-compliance)
- [16. Localisation](#16-localisation)
- [17. Testing and acceptance](#17-testing-and-acceptance)
- [18. Roadmap and estimates](#18-roadmap-and-estimates)
- [19. Risks and mitigations](#19-risks-and-mitigations)
- [20. Open questions](#20-open-questions)
- [Appendix A — Current feature disposition](#appendix-a--current-feature-disposition)
- [Appendix B — Endpoints observed in the APK](#appendix-b--endpoints-observed-in-the-apk)
- [Appendix C — Design tokens](#appendix-c--design-tokens)
- [Appendix D — Glossary](#appendix-d--glossary)

**How to use this document:** sections 0–4 define what we are building and why; 5–8 define navigation and visual language; 9–10 are the screen and component build specs; 11–13 cover content, API and playback; 14–20 cover quality, rollout and decisions still needed.

---

## 0. Decisions locked

These decisions came out of the discovery Q&A and are treated as requirements unless explicitly changed.

| Area | Decision |
|---|---|
| Build approach | New **Kotlin + Jetpack Compose** application, clean architecture. Do not patch the obfuscated APK. |
| Launch platforms | Android **phones, tablets, Android TV / Fire TV**. Web/desktop is **phase 2**. |
| Content source | Existing **EZServer/custom API**, same provider and content. Keep a provider abstraction so Xtream Codes/M3U can be added later without redesigning the UI. |
| v1 scope | Core viewing: login, home, live TV, movies, series, TV guide, catch-up, search, details, player, favourites, continue watching, settings. |
| Advanced features kept | **Parental controls** and **viewer profiles** only. Profiles are a later release. |
| Features dropped | DVR/recording, multi-screen, built-in VPN, cloud backup/restore, programme reminders, radio, Chromecast, offline downloads, speed test. |
| Brand | Keep **Sunset** name and the triangle/S motif, modernised with a **warm amber** accent on near-black. Remove the neon-cyan/magenta retro treatment. |
| Navigation | **Top navigation + cinematic hero on tablet/TV**; **bottom navigation on phones**. |
| Monetisation | **None.** No ads, no paywall, no subscription flow. Remove AdMob and ad-related SDKs. |
| Accounts | Single account in v1. Viewer profiles are a post-v1 release. |
| Metadata | Provider metadata plus **TMDB enrichment** (API key available). |
| Languages | **English + Spanish** at launch. |
| Player | **AndroidX Media3/ExoPlayer primary**, **LibVLC fallback** for unsupported streams/codecs. |
| App identity | **New package name** and direct APK / sideload distribution. |
| Crash reporting | **Privacy-first crash reporting only**; no behavioural analytics or advertising IDs. |

### Assumptions to validate

1. Login is username + password against the existing EZServer/custom API. If the provider requires device activation or a MAC-style registration, the flow will be added without changing the visual design.
2. The provider returns stream URLs (HLS/MPEG-TS/MP4) and artwork URLs. Exact JSON field names and URL formats still need discovery.
3. Catch-up/TV archive is part of the core Live/Guide experience and is retained in v1. If the provider does not expose catch-up for this account, the feature will be hidden rather than removed from the codebase.
4. The provider account exposes a single viewing identity. Profiles are local-only until the provider offers multi-profile support.
5. The existing content rights and provider terms permit a replacement client. Legal/compliance review is outside this document.
6. **There are currently no test servers, test credentials or API documentation.** Provider integration is therefore blocked. Development uses a fake provider and local fixtures only. The production service must not be called unless the owner supplies credentials and explicitly approves a read-only discovery session.

---

## 1. Executive summary

Sunset is currently a capable but dated white-label IPTV client. The APK contains live TV, movies, series, EPG, catch-up, favourites, continue watching, search, parental controls, recording, multi-screen, VPN, cloud backup, reminders, radio, cast and speed test. The UI is built from many separate Activities and four main fragments, uses obfuscated XML resources, and carries a neon-cyan/magenta retro identity plus AdMob and Firebase SDKs.

The overhaul replaces that application with a focused, premium streaming product:

- A cinematic dark interface with a warm amber accent, a hero billboard, and horizontal content rails.
- One consistent library across **Live TV, Movies, Series and Guide**.
- A modern adaptive experience: top navigation and large-screen composition on tablets/TV, bottom navigation on phones.
- A fast, reliable player with Media3 and automatic LibVLC fallback.
- Rich details, search, favourites, continue watching, parental controls and TMDB metadata.
- No ads, no paywall and no behavioural tracking.
- A measurable reduction in scope and technical debt: DVR, multi-screen, VPN, cloud backup, reminders, radio, cast, downloads and speed test are intentionally dropped.

The result should feel closer to Netflix, Prime Video or the supplied tflix.box reference than to a traditional IPTV utility: content-first, calm, quick to browse, and effortless to play.

### Success metrics

| Metric | Target |
|---|---|
| Time from successful login to first playback | ≤ 30 seconds for a returning user, ≤ 60 seconds for a new user |
| Playback start success rate | ≥ 98% of initiated plays |
| Crash-free sessions | ≥ 99.5% |
| Cold start to interactive | ≤ 2.5 seconds on a mid-range phone; ≤ 4 seconds on a low-end Fire TV device |
| Scroll performance | 60 fps target on supported devices; no sustained jank in poster rails or guide |
| Guide load (cached) | ≤ 1 second; uncached ≤ 3 seconds on a 10 Mbps connection |
| Search success | ≥ 90% of searches result in a play or detail view within 30 seconds |
| Support contacts about navigation/playback | Reduce by 30% versus current app (baseline to be measured) |
| App size | ≤ 60 MB per ABI; universal APK may be larger |
| Accessibility | WCAG 2.2 AA for contrast, focus and operable controls |

Metrics that require telemetry are collected only with explicit opt-in, and only as aggregated, non-identifying events.

---

## 2. Current app analysis

Findings below come from static analysis of the supplied APK: unzipping the package, parsing the binary manifest, extracting strings from both DEX files and `resources.arsc`, and inspecting activities, permissions and native libraries. The app was not executed as part of this analysis.

### 2.1 Technical baseline

| Item | Finding |
|---|---|
| Package | `com.nathnetwork.sunset` |
| App label | Sunset |
| Version | 5.0.1 (versionCode 724) |
| Min / target SDK | 21 / 31 |
| APK size | 106 MB |
| ABIs | `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` |
| UI technology | Java, XML Views, Material Components, Leanback components, AppCompat |
| Main structure | Multiple Activities plus four main fragments: Home, TV, VOD, Series |
| Player stack | ExoPlayer 2 + LibVLC + FFmpeg native libraries |
| Networking | OkHttp/Volley-era stack, Gson, custom token API |
| Local data | SQLite tables for channels, VOD, series, EPG, favourites, history, recordings, profiles |
| Images | Glide |
| Monetisation | AdMob |
| Analytics | Firebase Analytics, In-App Messaging, Installations |
| VPN | Embedded OpenVPN (`de.blinkt.openvpn`) with native libraries |
| Other native libs | Speedtest, FFmpeg, VLC, OpenVPN, crypto |
| Manifest | `usesCleartextTraffic="true"`, storage permissions, install-packages permission, boot receiver, foreground services |
| Resource protection | Obfuscated resource filenames and non-standard resource table encoding; source cannot be recovered cleanly |

The application appears to be based on an XCIPTV-style codebase, with strings and class paths indicating white-label panel support. It supports multiple backend panels (`PANEL 1`–`PANEL 5`) and both Xtream Codes and a custom EZServer API.

### 2.2 Feature inventory observed in the APK

| Area | Evidence in APK | Disposition |
|---|---|---|
| Login / account | `LoginActivity`, token endpoints, `query_user_days_left` | **Keep**, redesign |
| Device activation | `DeviceStatus`, activation strings, MAC/device messaging | **Keep if required** by provider |
| Home | `ORPlayerHomeFragment`, home rows | **Keep**, redesign around hero + rails |
| Live TV | `ORPlayerTVFragment`, `PlayStreamEPGActivity`, channel tables | **Keep**, redesign |
| Movies / VOD | `ORPlayerVODFragment`, `MovieInfoActivity`, VOD tables | **Keep**, redesign |
| Series | `ORPlayerSeriesFragment`, `SeriesActivity`, season/episode tables | **Keep**, redesign |
| EPG / TV guide | `EPGActivityXMLTV`, `epg_channel`, `epg_programme`, XMLTV endpoint | **Keep**, rebuild as timeline grid |
| Catch-up / TV archive | `CatchupActivity`, `CatchupPlayerActivity`, `tv_archive`, timeshift | **Keep** (pending API confirmation) |
| Favourites | `fav` table, favourites UI strings | **Keep** |
| Continue watching | `resume` table, continue-watching strings | **Keep**, improve |
| Search | `SearchActivity`, search tables/strings | **Keep**, rebuild as global search |
| Parental controls | `ParentalControlActivity`, parental tables, PIN strings | **Keep**, v1.0 release gate |
| Profiles | Profile strings, multi-profile tables | **Keep**, later release |
| Recording / DVR | `RecordingServices`, recordings table, schedule-recording strings | **Drop** |
| Multi-screen | `MultiScreenActivityEXO` | **Drop** |
| Built-in VPN | OpenVPN activities, native libs, VPN strings | **Drop** |
| Cloud backup / restore | `BackupActivity`, encrypted backup, `/backup.json` | **Drop** |
| Programme reminders | `ProgramRemindersActivity`, reminder strings | **Drop** |
| Radio | `RadioPlayerActivity`, radio streams | **Drop** |
| Cast | ExoPlayer Cast extension, Cast options provider | **Drop** |
| Offline downloads | Download-related strings in XCIPTV base | **Drop** |
| Speed test | `SpeedTestActivity`, OSS speedtest libs | **Drop** |
| Ads | AdMob SDK, ad unit strings, ad permissions | **Remove** |
| Firebase | Analytics, In-App Messaging, Installations | **Remove** except optional crash reporting |
| Auto-update | Update activities and APK URL strings | **Rebuild for sideload** |
| Multi-panel | `ORT_WHICH_PANEL`, PANEL 1–5 | **Keep one configured provider**; optional advanced server selector |
| Xtream / M3U | `player_api.php`, `xmltv.php`, M3U parsing | **Not in v1**; keep adapter boundary |

### 2.3 UX and brand observations

These are inferences from the codebase, resource inventory and supplied reference images:

- **Fragmented navigation.** The current app spreads core journeys across many Activities (login, categories, channel list, movie info, series, catch-up, settings, player). This makes consistent back behaviour, shared transitions and a single visual shell difficult.
- **Utility-first layout.** The app reads as an IPTV tool: dense lists, many icons, settings-heavy flows. Premium streaming apps put content first, with one obvious action per screen.
- **Dated visual identity.** Extracted assets show a neon cyan/magenta "SUNSET" wordmark with glowing inverted triangles and scanline effects. This is distinctive but clashes with a calm, cinematic library.
- **Inconsistent components.** The XCIPTV-derived base mixes Material, Leanback and custom views. Buttons, cards, focus states and spacing are not governed by one design system.
- **Thin content presentation.** Movie/series details, artwork and metadata are functional rather than editorial. There is little room for cast, similar titles, episodes or rich hero treatment.
- **TV support is bolted on.** Leanback is present, but the app is not designed remote-first. Focus order, overscan and 10-foot typography need to be first-class.
- **Ads interrupt the experience.** AdMob is integrated into a product that should feel premium. Removing ads is a major quality win.
- **Excessive permissions and native surface area.** VPN, recording, storage, boot, install-packages and speedtest libraries increase size, privacy risk and review friction. Dropping them simplifies the product substantially.

### 2.4 Constraints and risks discovered

| Risk | Detail | Mitigation |
|---|---|---|
| Undocumented custom API | The provider API is EZServer/custom, with token endpoints and unknown JSON schemas. **No test server, test account or API documentation exists.** | Build against `FakeContentProvider` and fixtures; keep the adapter behind an interface; do not call production. |
| Production credentials | Only a production login exists, and using it for discovery can affect the account or provider. | Do not call the live service by default. Require explicit owner approval for a read-only discovery session; never commit credentials. |
| Obfuscated resources | Resource names and parts of the resource table are non-standard; direct reuse is unreliable. | Rebuild UI from scratch; reuse only extracted brand motifs and verified assets. |
| Stream compatibility | IPTV providers often mix HLS, MPEG-TS, HEVC, AC-3 and inconsistent manifests. | Media3 first, LibVLC fallback, device test matrix, clear error recovery. |
| App size | LibVLC alone adds roughly 36–44 MB per ABI in the current APK. | ABI splits, R8, resource shrinking, optional VLC variant. |
| Sideload updates | No Play Store update channel. | Signed APK, in-app update check, signature verification, clear install guidance. |
| New package name | Existing users cannot update in place. | Communicate reinstall; optional settings export/import later. |
| EPG quality | Provider guide data may have timezone errors, missing end times or duplicate channels. | Normalize to UTC, validate intervals, dedupe, cache, show fallback "No guide data". |
| TMDB matching | Provider titles may not have IMDb/TMDB IDs. | Match by title + year + type, cache results, allow manual override later, fall back to provider art. |
| Content rights | The app is a client for provider content. | Confirm provider terms and privacy obligations before distribution. |

### 2.5 What can and cannot be reused

**Reusable**

- The **Sunset name and triangle/S brand motif**, refined into a new logo and design system.
- Knowledge of the provider's **EZServer endpoints, token model and content types**.
- The **feature requirements** captured above, and the existing account/content.
- Verified stream formats, device behaviour and edge cases discovered during API/player testing.

**Not reusable**

- Java source code, obfuscated layouts, resource IDs and most drawable assets.
- The current navigation structure, visual language and component implementations.
- The original signing identity (a new package name is required).
- AdMob/Firebase integrations and the VPN/recording/speedtest native stack, which are being removed.

### 2.6 Reference patterns to adopt

The supplied tflix.box screenshots are a useful benchmark, not a pixel-copy target.

| Reference pattern | Adopt | Adapt for Sunset |
|---|---|---|
| Top navigation with Home, Movies, TV Shows, Live TV | Yes | Home, Live TV, Movies, Series, Guide; amber active state; search and My List as actions |
| Full-bleed cinematic hero with eyebrow, title, metadata, synopsis and Play/More info | Yes | Add Resume CTA, page indicators and TMDB ratings; no autoplay/audio |
| Horizontal poster rails with quality/edition badges | Yes | Add progress, lock and favourite states; cap badges to avoid clutter |
| "Recently added" movie and TV rows | Yes | Add See all, category filters and clearer section hierarchy |
| "On Now" live cards with channel logo, current programme and progress | Yes | Add favourites-first ordering and a direct Guide link |
| Timeline guide with sticky channel column, time ruler and now line | Yes | Improve D-pad focus, add zoom/day filters, catch-up actions and a phone adaptation |
| Dense, information-heavy layout | Partly | Reduce density, enlarge type, increase whitespace and keep one primary action per screen |
| Search, profile and settings in the top-right | Yes | Add My List; keep the account menu simple |
| "Manage sources" / provider switching | No | One configured provider in v1; optional advanced server selector only if required |

---

## 3. Product definition

### 3.1 Vision

**Sunset is the calm, cinematic way to watch everything your provider offers.** It should feel like a premium streaming service, not a channel-list utility: open the app, see something worth watching, and press play.

### 3.2 Goals

1. **Make browsing enjoyable.** A strong hero, clear rows and rich artwork make the library feel curated.
2. **Make playback effortless.** One primary action, reliable resume, fast start and graceful fallback.
3. **Unify the experience.** Live TV, movies, series and guide share one navigation model, one design system and one player.
4. **Respect every screen.** Phone, tablet and TV each get an intentional layout, not a stretched phone UI.
5. **Reduce complexity.** Remove features that do not serve the core viewing loop.
6. **Earn trust.** No ads, no tracking, minimal permissions and secure credential storage.

### 3.3 Non-goals for v1

- Becoming a general-purpose media centre or file player.
- Live TV DVR, multi-view, VPN, radio, cast, downloads or cloud backup.
- Social features, watch parties, recommendations from a server-side profile, or user-generated content.
- Web/desktop clients (phase 2).
- In-app purchases, subscriptions or ads.
- Supporting every IPTV panel type. EZServer is the only required provider in v1; the architecture leaves room for others.

### 3.4 Personas

| Persona | Context | Needs | Design implications |
|---|---|---|---|
| **The lean-back viewer** | Android TV or Fire TV in the living room, remote in hand. | Find something quickly, read from 3 metres, never get lost. | Large type, strong focus states, predictable back, hero + rails, no tiny controls. |
| **The commuter / mobile viewer** | Phone, portrait, one hand, variable network. | Resume a movie, check what is live, search fast. | Bottom nav, thumb-reachable actions, offline-tolerant states, PiP. |
| **The tablet viewer** | Tablet in landscape, often on a stand. | Browse a rich library, watch in bed. | Two-pane details, larger rails, cinematic hero, landscape player. |
| **The parent** | Shared family account. | Hide adult categories, lock mature content, keep kids safe. | PIN-gated categories, clear lock indicators, session unlock rules. |
| **The provider administrator** | Configures the app for one service. | Stable login, predictable updates, fewer support calls. | Clear error messages, diagnostics, remote-config-ready build. |

### 3.5 Jobs to be done

- "When I open the app, show me something I want to watch without making me browse ten menus."
- "When I return to a movie or episode, put me back where I left off."
- "When I want live TV, show me what is on now and what is next, and let me tune in immediately."
- "When I want a specific channel or title, let me search across everything at once."
- "When I use a remote, I want to see where I am and reach everything with the D-pad."
- "When my children use the app, I want mature content hidden or locked."

---

## 4. Scope and release plan

### 4.1 v1.0 — Core viewing

| Capability | Included |
|---|---|
| Authentication | Username/password login, secure session, expiry display, device activation if required, sign out |
| Bootstrap | Splash, token restore, maintenance/announcement handling, update check |
| Home | Hero billboard, continue watching, live now, recently added movies, recently added series, my list, recommended |
| Live TV | On Now rail, category filters, channel list/grid, now/next, favourite channel, play |
| TV Guide | Timeline grid, day selector, channel filter, now line, programme details, catch-up entry point |
| Movies | Featured area, category filters, sort, poster grid, pagination |
| Movie details | Backdrop, poster, metadata, synopsis, cast, play/resume, favourite, similar titles |
| Series | Category filters, sort, poster grid, pagination |
| Series details | Seasons, episodes, progress, play/resume, next episode |
| Search | Global search across live, movies and series, recent searches, grouped results |
| Favourites / My List | Add/remove, filter by type, accessible from Home and More |
| Continue Watching | Local resume positions, progress, remove, completion rules |
| Parental controls | PIN setup, category/rating locks, hide or lock mature content, session unlock, lockout (**release gate** if adult categories exist) |
| Player | Media3 primary, VLC fallback, HLS/TS/MP4, resume, audio/subtitle tracks, aspect ratio, PiP, next episode, live/catch-up controls |
| Settings | Account, playback, appearance, language, about, privacy, sign out |
| Metadata | Provider metadata + TMDB enrichment, artwork fallback |
| Localisation | English and Spanish |
| Quality | Offline/error/empty states, accessibility, performance, crash reporting |

> **Release gate:** If the provider exposes adult categories, parental controls must be functional in the first public build. They are included in the v1.0 scope above and detailed in §9.14.

### 4.2 v1.1 — Polish and update

- In-app update check for sideloaded APKs.
- Diagnostics screen with redacted playback/network information and "share logs" action.
- Refined TMDB matching and manual artwork overrides if needed.
- Spanish copy review and accessibility audit fixes.
- Performance tuning based on field feedback.

### 4.3 v1.2 — Personal

- Local viewer profiles: name, avatar, maturity setting, separate favourites, history and continue watching.
- Profile picker on launch, profile switching, optional profile PIN.
- Import/export of local settings as a file (not cloud backup).

### 4.4 Phase 2 — Web/desktop

- Responsive web app with HLS playback and the same library model.
- Optional desktop app.
- Shared Kotlin Multiplatform domain/data layer where practical.
- Web playback likely uses a browser-native stack (HLS.js) rather than Compose video, because browser codec support is the constraint.

### 4.5 Explicitly out of scope

DVR/recording, multi-screen, built-in VPN, cloud backup/restore, programme reminders, radio, Chromecast, offline downloads, speed test, ads, subscriptions, paywall, social features, watch parties, user-generated content.

These can be reconsidered later, but they are not part of the v1 architecture or navigation. Removing them reduces permissions, native libraries, APK size and support surface.

---

## 5. Information architecture

### 5.1 Navigation model

**Large screens (tablet landscape, Android TV, Fire TV):**

Top navigation bar, transparent over the hero and solid after scroll:

`Home · Live TV · Movies · Series · Guide` — with `Search`, `My List`, and `Account/Settings` as icon actions on the right.

**Phones (portrait):**

Bottom navigation with four destinations plus More:

`Home · Live TV · Movies · Series · More`

`More` contains `Guide`, `My List`, `Search`, `Settings`, `About`, `Sign out`.

**Player:**

Full-screen modal. Navigation chrome is hidden. Back returns to the originating screen with state preserved.

**Details:**

A movie or series detail is a destination, not a modal, so back behaviour is predictable and deep links work. On tablets/TV, details can use a two-column composition (artwork left, metadata/actions right); on phones, a vertical scroll.

### 5.2 Screen map

| Route | Screen | Entry points | Notes |
|---|---|---|---|
| `/splash` | Bootstrap | App launch | Token restore, maintenance, update check |
| `/login` | Login | No session, expired session, sign out | Username/password; activation if required |
| `/home` | Home | App launch, nav | Hero + rails |
| `/live` | Live TV | Nav, Home "Live Now" | On Now + channel list |
| `/guide` | TV Guide | Nav (large), More (phone), Live TV | Timeline grid |
| `/movies` | Movies | Nav | Categories + poster grid |
| `/movie/{id}` | Movie details | Poster, search, deep link | Play/resume |
| `/series` | Series | Nav | Categories + poster grid |
| `/series/{id}` | Series details | Poster, search, deep link | Seasons + episodes |
| `/search` | Search | Top bar, More | Global search |
| `/my-list` | My List | Top bar/account menu, More | Favourites |
| `/player` | Player | Play actions | Full-screen; args: content type, id, resume position, live/catch-up |
| `/settings` | Settings | Account menu, More | Account, playback, appearance, parental, about |
| `/settings/parental` | Parental controls | Settings | PIN and locks (v1.0 release gate) |
| `/settings/about` | About | Settings | Version, legal, TMDB attribution |
| `/profiles` | Profile picker | Launch, account menu | v1.2 |
| `/diagnostics` | Diagnostics | Settings (hidden/advanced) | Redacted logs, playback info |

### 5.3 Deep links

- `sunset://home`
- `sunset://live`
- `sunset://guide`
- `sunset://movie/{providerId}`
- `sunset://series/{providerId}`
- `sunset://play?type=live&id={channelId}`
- `sunset://search?q={query}`

Use a custom scheme for sideload builds. If a web presence is added later, map `https://` links to the same routes through an app-links configuration.

---

## 6. Design principles

1. **Content is the interface.** Artwork, titles and the next action dominate. Chrome recedes.
2. **One clear next action.** Every screen has a primary action: Play, Resume, Watch live, or Choose an episode. Secondary actions are visually quieter.
3. **Calm, not cold.** Near-black surfaces, warm amber accents and soft scrims. No neon glow, no visual noise.
4. **Remote-first, touch-native.** The same product works with a D-pad and with a thumb. Focus is always visible; touch targets are always large enough.
5. **Fast by default.** Show cached content immediately, load the rest progressively, and never block the first screen on EPG or TMDB.
6. **Predictable navigation.** One shell, one back model, no dead ends. Returning from playback restores the exact scroll and focus position.
7. **Accessible to everyone.** Contrast, captions, font scaling, focus order and screen-reader labels are requirements, not polish.
8. **Privacy is a feature.** No ads, no tracking, minimal permissions, encrypted credentials.

---

## 7. Brand and visual identity

### 7.1 Brand direction

The current identity is a neon retro "SUNSET" wordmark with cyan/magenta glows and inverted triangles. It is memorable but loud. The new identity keeps the **triangle + S silhouette** as the brand memory, then strips it back:

- **Name:** Sunset
- **Tagline (optional):** *Everything you love, in one place.*
- **Personality:** cinematic, warm, calm, confident, uncluttered.
- **Visual metaphor:** the last light of the day — a warm amber horizon against a deep night sky.
- **What to avoid:** neon glow, scanlines, chrome bevels, purple/cyan gradients as primary colours, busy icon collages.

### 7.2 Logo and app icon

- **Symbol:** a simplified inverted triangle containing a negative-space "S" or a horizon cut. Keep it legible at 24 px.
- **Wordmark:** `SUNSET` in a geometric sans, uppercase, letter-spacing +0.08em. Use the symbol alone where space is tight.
- **Colour:** amber gradient (`#F5A524` → `#FF7A1A`) on near-black. A single-colour white or amber version must work on posters and light backdrops.
- **App icon:** adaptive icon with a near-black background, amber triangle/S foreground, 66% safe zone. Avoid text inside the icon.
- **TV banner:** 320×180, logo left or centred, near-black background, no critical content near edges.
- **Motion:** on splash, the triangle draws in over 500 ms, then the wordmark fades. Respect "reduce motion".

### 7.3 Colour system

Use a near-black neutral base with one warm accent. Do not use pure black for large surfaces; it crushes artwork and increases OLED smear.

| Token | Hex | Usage |
|---|---|---|
| `bg/base` | `#08090C` | App background, player background |
| `bg/surface` | `#101218` | Top bar after scroll, sheets |
| `bg/surface-2` | `#171A21` | Cards, rows, inputs |
| `bg/surface-3` | `#1F232C` | Hover, selected, skeleton base |
| `border/subtle` | `#262A33` | Dividers, card outlines |
| `accent/primary` | `#F5A524` | Primary buttons, active nav, progress, focus |
| `accent/hover` | `#FFC14D` | Hover/pressed highlight on pointer devices |
| `accent/pressed` | `#D98A12` | Pressed state |
| `accent/on` | `#0B0B0F` | Text/icons on amber |
| `text/primary` | `#F7F7F8` | Titles, body |
| `text/secondary` | `#A7ABB5` | Metadata, descriptions |
| `text/tertiary` | `#7C818D` | Disabled, timestamps (meets 4.5:1 on `bg/base`) |
| `semantic/live` | `#FF453A` | LIVE badge, on-air dot |
| `semantic/success` | `#32D583` | Available, completed |
| `semantic/warning` | `#F5A524` | Expiry warning |
| `semantic/error` | `#FF6B6B` | Errors, destructive actions |
| `focus/ring` | `#FFD166` | TV focus ring (2 dp) |
| `scrim/top` | `#00000000` → `#08090C` | Hero top gradient |
| `scrim/bottom` | `#00000000` → `#08090C` | Hero bottom gradient |
| `scrim/left` | `#08090CE6` → `#08090C00` | Text legibility over backdrops |

**Contrast rules**

- Body text on `bg/base` must meet 4.5:1. `text/primary` and `text/secondary` both pass.
- Amber `#F5A524` on `bg/base` passes for large text and UI; use `accent/on` for text on amber buttons.
- Never use colour alone to communicate state. Pair LIVE with a dot/label, locks with an icon, errors with text.
- Focus ring must be visible on both dark and light artwork. Add a 1 dp dark outer stroke around the amber ring when over bright posters.

### 7.4 Typography

Use **Inter** as the single UI family. It has excellent Spanish coverage, tabular numerals for time, and a neutral streaming feel. The wordmark may use a custom geometric face, but do not add a second body font.

| Style | Size / line height | Weight | Usage |
|---|---|---|---|
| `display/hero` | 56/64 sp (TV/tablet), 36/44 (phone) | 700 | Hero title |
| `display/section` | 32/40 sp | 700 | Screen titles |
| `headline` | 24/32 sp | 600 | Section headers, detail titles |
| `title` | 20/28 sp | 600 | Card titles, dialog titles |
| `body` | 16/24 sp | 400 | Descriptions, settings |
| `body/strong` | 16/24 sp | 600 | Emphasis |
| `label` | 14/20 sp | 600 | Buttons, chips, nav |
| `caption` | 13/18 sp | 400 | Metadata, timestamps |
| `micro` | 11/14 sp | 700 | Badges (LIVE, 4K, NEW) |

**Rules**

- TV text is 1.15–1.25× the phone size; minimum body size on TV is 18 sp.
- Hero titles max 2 lines on phone, 3 on TV; use ellipsis, never shrink below the style minimum.
- Sentence case for titles and buttons. Uppercase only for short badges and the wordmark.
- Use tabular numerals for clocks, durations, episode numbers and guide times.
- Keep body line length 45–75 characters. Descriptions clamp to 3 lines in cards, full text on details.
- Support font scaling up to 200% without clipping or overlap.

### 7.5 Spacing and layout

- Base unit: **4 dp**. Rhythm: 8 dp.
- Spacing scale: `4, 8, 12, 16, 24, 32, 48, 64`.
- Screen gutters: 16 dp phone, 24 dp tablet, 48 dp TV.
- Content max width: 1600 dp on very large screens; centre the rail area rather than stretching posters indefinitely.
- Row gap: 24 dp phone, 32 dp tablet/TV.
- Section gap: 32 dp phone, 48 dp tablet/TV.
- Poster grid: adaptive columns.
  - Phone portrait: 2 columns (3 on large phones).
  - Tablet: 4–6 columns.
  - TV: 6–8 columns.
- Horizontal rails show a **peek** of the next card (12–18% visible) to signal scrollability.
- Use safe-area insets on phones and 5% overscan on TV.

### 7.6 Shape, elevation and surfaces

- Corner radii: 8 dp (badges, inputs), 12 dp (posters/cards), 16 dp (sheets/dialogs), 24 dp (hero panels where needed).
- Avoid heavy drop shadows. Depth comes from surface tints, scrims and 1 dp borders.
- Cards sit on `bg/base`; on hover/focus they lift to `bg/surface-2` plus a focus ring, not a large shadow.
- Sheets/dialogs: `bg/surface`, 16 dp top radius, 32 dp scrim.
- Buttons: fully rounded pill for primary actions; 12 dp rounded rectangle for secondary.

### 7.7 Imagery

| Asset | Aspect | Notes |
|---|---|---|
| Poster | 2:3 | Primary card image. Provider poster first, TMDB fallback. |
| Backdrop | 16:9 | Hero and details. TMDB backdrop preferred when it matches. |
| Channel logo | 1:1 or 16:9 | Contain on transparent/dark; monogram fallback. |
| Episode still | 16:9 | Episode rows and next-up cards. |
| Profile avatar | 1:1 | v1.2; geometric/abstract avatars, no personal photos required. |

- Always reserve the aspect ratio before loading to prevent layout shift.
- Use a subtle gradient placeholder derived from the poster's dominant colour, or a neutral surface with a monogram.
- Never stretch artwork. Crop with `ContentScale.Crop` for backdrops and `Fit` for channel logos.
- Hero text must sit on a scrim, never directly on a busy image.
- Badges sit top-left on posters; progress sits bottom edge; favourite/lock icons top-right.

### 7.8 Motion

| Motion | Duration | Easing |
|---|---|---|
| Hover/press/focus | 120–150 ms | `cubic-bezier(0.2, 0, 0, 1)` |
| Screen transition | 250 ms | Standard ease-out |
| Hero crossfade | 500 ms | Ease-in-out |
| Shared poster → details | 300 ms | Container transform |
| Sheet enter/exit | 250 ms | Ease-out / ease-in |
| Skeleton shimmer | 1200 ms loop | Linear |

- Motion explains change; it never delays input.
- Respect system "reduce motion": disable hero auto-rotation, shared-element transforms, parallax and shimmer; use simple fades.
- Do not auto-play video/audio in the hero. If trailers are added later, default to muted and pause on focus loss.

### 7.9 Iconography

- Rounded outline icons, 1.8 dp stroke, 24 dp default; 20 dp in dense rows; 32 dp for TV actions.
- Use a single icon family (Material Symbols Rounded or a custom set) and a consistent optical weight.
- Icons always have a text label or content description unless the meaning is universally unambiguous (back, play, search).
- Active nav uses a filled variant plus the amber accent; inactive uses outline.
- Do not mix filled, outline and duotone styles arbitrarily.

### 7.10 TV focus model

- Every interactive element is reachable with the D-pad.
- Focus is shown with a 2 dp `focus/ring` plus 1.04 scale and, on cards, a subtle lift.
- Focus never causes layout shift. Reserve space for the ring and scale.
- Initial focus on Home: the primary hero action. On details: Play/Resume. On guide: the current programme for the first visible channel.
- D-pad behaviour:
  - Up/down moves between rows or guide channels.
  - Left/right moves within a row or across time.
  - Centre activates.
  - Back closes sheets/dialogs first, then returns to the previous screen.
  - Long centre on a poster opens a quick action menu (Play, My List, Details).
- Do not hide critical actions in hover-only states.
- Keep focus visible when content loads; if the focused item disappears, move focus to the nearest sensible item.

### 7.11 Accessibility

- Target **WCAG 2.2 AA**.
- Minimum touch target 48×48 dp; TV focus targets 56×56 dp where possible.
- Content descriptions for all images and icon buttons. Decorative images are marked as such.
- Captions/subtitles available wherever the stream provides them; honour system caption styling where possible.
- Audio description track surfaced when available.
- Support TalkBack and Switch Access; logical focus order; no keyboard traps.
- Font scale up to 200%; layouts reflow rather than clip.
- Do not rely on colour alone; pair with labels, icons and patterns.
- Provide a "Reduce motion" in-app preference in addition to the system setting.
- Player controls must be operable without gestures (visible buttons) and with gestures.

### 7.12 Design deliverables

- Figma (or equivalent) library with colour, type, spacing, radius, elevation, motion and icon tokens.
- Components with all states: default, hover, focus, pressed, selected, disabled, loading, error, empty.
- Key screens at three sizes: phone portrait, tablet landscape and 1080p TV (with overscan guides).
- Clickable prototype for the core journeys: login → home → movie details → player; live → guide → programme details; search → result; favourite/continue watching.
- Logo, wordmark, adaptive app icon, TV banner and splash assets (SVG + PNG exports).
- Motion specs for hero crossfade, poster-to-details, focus/press, sheet and player controls.
- Accessibility annotations: focus order, content descriptions, minimum targets, contrast notes.
- Redlines or a shared token source used directly by engineering. The token JSON in Appendix C is the source of truth until the Figma variables are final.

---

## 8. App shell

### 8.1 Responsive breakpoints

| Width | Layout |
|---|---|
| `< 600 dp` | Phone: bottom navigation, single column, compact hero |
| `600–839 dp` | Large phone / small tablet: bottom navigation or compact rail; 3–4 poster columns |
| `840–1279 dp` | Tablet landscape: top navigation, hero, 4–6 columns |
| `≥ 1280 dp` | TV / large tablet: top navigation, cinematic hero, 6–8 columns, 48 dp gutters |

Use `WindowSizeClass` and a TV-specific check (`UiDeviceMode.Television`) rather than hard-coded device types.

### 8.2 Top bar (tablet/TV)

```
┌──────────────────────────────────────────────────────────────────────┐
│  SUNSET     Home   Live TV   Movies   Series   Guide      🔍  ♥  ◉   │
└──────────────────────────────────────────────────────────────────────┘
```

- Height: 64 dp phone/tablet, 72 dp TV.
- Transparent over the hero; after 24 dp of scroll, animate to `bg/surface` with a bottom hairline.
- Active item: amber text plus a 2 dp underline or pill; inactive: `text/secondary`.
- `Search`, `My List` and `Account` are icon buttons with labels for accessibility.
- On TV, the top bar is focusable; focus moves into the hero or the first row with Down.
- Hide the top bar in the player and on the login screen.
- On narrow tablets, collapse secondary actions into an overflow menu.

### 8.3 Bottom bar (phone)

```
┌──────────────────────────────────────────────┐
│   Home      Live      Movies     Series  More │
│    ●                                           │
└──────────────────────────────────────────────┘
```

- Height: 64 dp + system inset.
- Five items maximum. Active item: amber icon and label; inactive: `text/tertiary`.
- `More` opens a bottom sheet with Guide, My List, Search, Settings, About, Sign out.
- Hide on player, login and full-screen guide (if the guide uses the full viewport).
- Preserve each tab's scroll state when switching.

### 8.4 Back behaviour

| Context | Back action |
|---|---|
| Player | Exit player, restore previous screen and position |
| Details | Return to originating screen, restore scroll/focus |
| Search | Clear query if non-empty; otherwise return |
| Guide programme sheet | Close sheet |
| Settings sub-page | Return to Settings |
| Login | Exit app (or return to profile picker in v1.2) |
| Root tab | Return to Home; second back exits app |

On Android TV, back must never trap the user in a sub-view. On phones, use predictive back where available.

### 8.5 Loading, empty and error states

- **Skeletons** for hero, rails and grids. Match final layout to avoid shift.
- **Empty states** explain why and offer one action: "No favourites yet — browse Movies", "No guide data for this day — try another day".
- **Errors** are human: "We couldn't reach Sunset. Check your connection and try again." Include Retry and, for playback, Switch player.
- **Offline banner** at the top of the shell when connectivity is lost; cached content remains browsable.
- Never show a raw stack trace, URL, token or JSON response to the user.

---

## 9. Screen specifications

Each screen below defines purpose, layout, components, behaviour, states and acceptance criteria. Wireframes are directional, not pixel-perfect.

### 9.1 Splash / bootstrap

**Purpose:** restore the session, check service status and enter the app as fast as possible.

```
┌──────────────────────────────────────────────┐
│                                              │
│                  ◢ SUNSET                    │
│                  Everything you love         │
│                                              │
│                  ● ● ●                       │
│                                              │
│  Checking your session…                      │
└──────────────────────────────────────────────┘
```

**Behaviour**

1. Show the logo immediately; do not wait for network.
2. Read the encrypted session from local storage.
3. If a token exists, validate it in the background and load cached catalog. Do not block on a network round trip.
4. If valid, go to Home. If expired, go to Login with a friendly message.
5. Check maintenance/announcement flags; if maintenance is active, show a blocking screen with status and retry.
6. Check for an app update in the background (sideload build); never block startup on update.
7. If no session and no cached data, go to Login.

**States**

| State | UI |
|---|---|
| Valid session | Home |
| No session | Login |
| Expired session | Login with "Your session expired. Please sign in again." |
| Offline + valid cached session | Home with offline banner |
| Offline + no cache | Login/offline screen with Retry |
| Maintenance | Maintenance screen with message, ETA if provided, Retry |
| Device not active | Activation screen with code/instructions |

**Acceptance:** cold start shows the logo within 300 ms; interactive Home within 2.5 s on a mid-range phone; no network call blocks the first frame.

---

### 9.2 Login

**Purpose:** authenticate against the existing provider with minimum friction.

```
┌──────────────────────────────────────────────┐
│                  ◢ SUNSET                    │
│                                              │
│              Welcome back                    │
│       Sign in to start watching              │
│                                              │
│   Username                                   │
│   ┌──────────────────────────────────────┐   │
│   │                                      │   │
│   └──────────────────────────────────────┘   │
│   Password                                   │
│   ┌──────────────────────────────────────┐   │
│   │ ••••••••••                       👁  │   │
│   └──────────────────────────────────────┘   │
│                                              │
│   [        Sign in        ]                  │
│                                              │
│   ☐ Remember me                 Need help?   │
│                                              │
│   ⚠ Invalid username or password             │
└──────────────────────────────────────────────┘
```

**Layout**

- Centred card, max width 420 dp, on a near-black background with a subtle amber horizon gradient.
- Logo at top; no hero imagery on login to keep it fast.
- Username and password fields with floating labels; password visibility toggle.
- Primary button full width, amber, dark text.
- "Remember me" remembers the username only, not the password; the session token is always stored securely.
- "Need help?" opens support information (provider contact or support URL from config).
- If the provider requires a server/panel selection, show an advanced "Server" expander with a preconfigured default. Do not expose it unless necessary.

**Behaviour**

- Validate non-empty fields before submitting.
- Show inline errors; do not clear the username on failure.
- Show a loading state on the button and disable duplicate submissions.
- On success, store the token encrypted, fetch the account summary, and go to Home.
- On failure, distinguish invalid credentials, expired account, device not active, too many connections, network error, maintenance.
- If device activation is required, navigate to an activation screen showing the code, instructions and a "Check again" action.
- Support hardware keyboard and TV remote; first field is focused on TV.
- Block screenshots on the login screen if the platform supports it (privacy hardening).

**Error taxonomy**

| Code | Message |
|---|---|
| Invalid credentials | "That username or password doesn't look right." |
| Account expired | "This account has expired. Contact your provider." |
| Device not active | "This device isn't activated yet. Follow the steps below." |
| Connection limit | "Too many devices are watching. Close a stream and try again." |
| Network | "We can't reach Sunset. Check your connection." |
| Maintenance | "Sunset is being updated. Please try again shortly." |
| Unknown | "Something went wrong. Please try again." |

**Acceptance:** a returning user can sign in with two fields and one tap; credentials are never logged; errors are specific and actionable.

---

### 9.3 Home

**Purpose:** the primary discovery surface. Show something worth watching immediately and make the next action obvious.

#### Large screen (tablet / TV)

```
┌────────────────────────────────────────────────────────────────────────────┐
│ SUNSET      Home   Live TV   Movies   Series   Guide        🔍   ♥    ◉    │
├────────────────────────────────────────────────────────────────────────────┤
│                                                                            │
│  MOVIE · RECENTLY ADDED                                                    │
│  The Hobbit: The Desolation of Smaug                                       │
│  2013 · PG-13 · 3h 6m · ★ 7.4 · Adventure                                  │
│  The dwarves, along with Bilbo Baggins and Gandalf, continue their quest…  │
│                                                                            │
│  [ ▶ Play ]   [ ⓘ More info ]                              ● ○ ○ ○ ○        │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  Continue Watching                                              See all →  │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                    │
│  │ poster │ │ poster │ │ poster │ │ poster │ │ poster │                    │
│  │ ▓▓▓░░░ │ │ ▓▓░░░░ │ │ ▓▓▓▓░░ │ │ ▓░░░░░ │ │ ▓▓▓░░░ │                    │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘                    │
│                                                                            │
│  Live Now                                                       See all →  │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐       │
│  │  ● LIVE      │ │  ● LIVE      │ │  ● LIVE      │ │  ● LIVE      │       │
│  │  Channel 1   │ │  Channel 2   │ │  Channel 3   │ │  Channel 4   │       │
│  │  Now: …      │ │  Now: …      │ │  Now: …      │ │  Now: …      │       │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘       │
│                                                                            │
│  Recently Added in Movies                                       See all →  │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐         │
│  │ poster │ │ poster │ │ poster │ │ poster │ │ poster │ │ poster │         │
│  │        │ │        │ │        │ │        │ │        │ │        │         │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘         │
│                                                                            │
│  Recently Added in Series                                       See all →  │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                    │
│  │ poster │ │ poster │ │ poster │ │ poster │ │ poster │                    │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘                    │
└────────────────────────────────────────────────────────────────────────────┘
```

#### Phone

```
┌──────────────────────────────┐
│ SUNSET                 🔍  ◉ │
│ ┌──────────────────────────┐ │
│ │        hero backdrop     │ │
│ │  MOVIE · RECENTLY ADDED  │ │
│ │  The Hobbit: The…        │ │
│ │  2013 · PG-13 · ★ 7.4    │ │
│ │  [ ▶ Play ]  [ ⓘ Info ]  │ │
│ └──────────────────────────┘ │
├──────────────────────────────┤
│ Continue Watching      →     │
│ ┌──────┐ ┌──────┐ ┌──────┐   │
│ │poster│ │poster│ │poster│   │
│ │▓▓▓░░ │ │▓▓░░░ │ │▓▓▓▓░ │   │
│ └──────┘ └──────┘ └──────┘   │
│                              │
│ Live Now               →     │
│ ┌────────────┐ ┌────────────┐│
│ │ ● LIVE     │ │ ● LIVE     ││
│ │ Channel 1  │ │ Channel 2  ││
│ │ Now: …     │ │ Now: …     ││
│ └────────────┘ └────────────┘│
│                              │
│ Recently Added Movies  →     │
│ ┌──────┐ ┌──────┐ ┌──────┐   │
│ │poster│ │poster│ │poster│   │
│ └──────┘ └──────┘ └──────┘   │
├──────────────────────────────┤
│ Home  Live  Movies  Series  More │
└──────────────────────────────┘
```

**Hero behaviour**

- Content selection priority:
  1. An in-progress movie or series with a strong backdrop (resume).
  2. A recently added movie/series with a TMDB backdrop.
  3. A manually featured item from provider config, if available.
  4. A popular title.
- Show `MOVIE · RECENTLY ADDED`, `SERIES · NEW EPISODE`, or `CONTINUE WATCHING` as the eyebrow.
- Title, year, certification, runtime/seasons, rating, first genre, and a 2–3 line synopsis.
- Primary CTA: `Play` or `Resume S2 E4` / `Resume 1:12:30`.
- Secondary CTA: `More info` (or `My List` on phone if space is tight).
- Page dots or a thin progress line for the hero carousel.
- Auto-rotate every 8 seconds, pause on focus/hover/touch, and disable with reduced motion.
- Do not auto-play trailers or audio.
- On TV, the hero is the initial focus target; Left/Right changes hero slide, Down enters the first row.

**Rows (in order, omit empty rows)**

1. **Continue Watching** — movies and episodes with 5–95% progress, most recent first, max 20. Show progress and remaining time; long-press/menu offers "Remove from Continue Watching".
2. **Live Now** — a horizontal sample of live channels, prioritized by favourites and watch history. Each card shows channel logo, name, current programme and progress.
3. **Recommended For You** — local heuristic based on genre affinity, recent watches, favourites and recency. If no history, label the row **New on Sunset** and show recent additions.
4. **Recently Added in Movies** — newest provider movies, 12–20 items.
5. **Recently Added in Series** — newest provider series, 12–20 items.
6. **My List** — favourites, if any.
7. **Browse by Genre** — optional category shortcuts, if the provider exposes useful genres.

Rows use a horizontal `LazyRow` with snap and a peek. Each row header has a `See all` action that opens the corresponding tab with the relevant filter applied.

**States**

| State | UI |
|---|---|
| Loading | Hero skeleton + two row skeletons |
| No history | Hide Continue Watching and Recommended; show Recently Added |
| No catalog | Empty state with Retry and "Check your connection" |
| Offline | Show cached rows + offline banner; disable Play for uncached streams |
| Expired account | Banner "Your account expires in N days" or blocking renewal message |
| Image failure | Monogram/gradient placeholder, title still readable |

**Acceptance:** Home shows a hero and at least two content rows after first load; all rows are keyboard/D-pad reachable; returning from playback restores scroll and focus; no empty row renders.

---

### 9.4 Live TV

**Purpose:** quickly find and watch a live channel, with enough context to choose confidently.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ SUNSET      Home   Live TV   Movies   Series   Guide        🔍   ♥    ◉    │
├────────────────────────────────────────────────────────────────────────────┤
│  Live TV                                                                   │
│                                                                            │
│  [ All channels ] [ Favourites ] [ News ] [ Sports ] [ Movies ]   [Search] │
│                                                                            │
│  On Now                                                                    │
│  ┌────────────────┐ ┌────────────────┐ ┌────────────────┐ ┌──────────────┐ │
│  │ ● LIVE         │ │ ● LIVE         │ │ ● LIVE         │ │ ● LIVE       │ │
│  │  [logo]        │ │  [logo]        │ │  [logo]        │ │  [logo]      │ │
│  │  BBC One  101  │ │  CNN      202  │ │  HBO      301  │ │  ESPN   401  │ │
│  │  Evening News  │ │  The Lead      │ │  Movie         │ │  Sports      │ │
│  │  18:00–19:00   │ │  18:00–19:00   │ │  18:00–20:00   │ │  18:00–20:00 │ │
│  └────────────────┘ └────────────────┘ └────────────────┘ └──────────────┘ │
│                                                                            │
│  Channels (128)                                      [Sort: Number v]      │
│  ┌──────┬──────────────────────┬──────────────────┬──────────────┬───────┐ │
│  │ 101  │ [logo] BBC One       │ Evening News     │ 18:00–19:00  │  ♥    │ │
│  │ 102  │ [logo] BBC Two       │ Garden Rescue    │ 18:00–18:30  │  ♥    │ │
│  │ 201  │ [logo] CNN           │ The Lead         │ 18:00–19:00  │  ♥    │ │
│  │ 301  │ [logo] HBO           │ Movie            │ 18:00–20:00  │  ♥    │ │
│  └──────┴──────────────────────┴──────────────────┴──────────────┴───────┘ │
└────────────────────────────────────────────────────────────────────────────┘
```

**Layout**

- Category chips: All, Favourites, then provider categories. Keep the chip row horizontally scrollable; selected chip uses amber fill or underline.
- `On Now` rail: 6–10 channels; each card is focusable and plays on activation. Card content: LIVE badge, logo, channel number/name, current programme, time range, progress bar.
- Channel list: virtualized rows with number, logo, name, current programme, now/next time, favourite toggle, and a catch-up indicator if available.
- On tablet/TV, the list can use a two-column layout: channel list on the left, a preview/now-next panel on the right for the focused channel. On phone, a single list.
- Sort options: channel number, name, category, recently watched. Default: channel number within category.
- Search opens global search filtered to Live TV.

**Behaviour**

- Tapping a channel starts playback immediately. On TV, centre plays; long centre opens channel details.
- Favourite toggles without leaving the screen; show a snackbar with Undo.
- Current programme progress updates every 30 seconds.
- If EPG is missing, show "Guide unavailable" but keep the channel playable.
- If a channel has catch-up and the current programme is nearly over, the detail sheet can offer "Watch from start" when supported.
- Empty category: "No channels in this category."
- Pull-to-refresh on touch; Refresh action on TV.

**States**

- Loading: chip skeletons + 6 card skeletons.
- No channels: empty state with Retry.
- No EPG: channel list without now/next metadata.
- Stream error: keep the list, show a player error with Retry/Switch player.
- Offline: show cached channel list; Play disabled with "Connect to watch live TV."

**Acceptance:** a user can go from Home to a live stream in ≤ 3 actions; the current programme and next programme are visible when guide data exists; favourite state persists.

---

### 9.5 TV Guide

**Purpose:** answer "what's on now and later" across channels, and provide a direct path to watch or catch up.

#### Large screen timeline grid

```
┌────────────────────────────────────────────────────────────────────────────┐
│ SUNSET      Home   Live TV   Movies   Series   Guide        🔍   ♥    ◉    │
├────────────────────────────────────────────────────────────────────────────┤
│  TV Guide                                                                  │
│  [ Today v ]  [ All channels v ]  [ Favourites ]        [ − ] [ Now ] [ + ]│
│                                                                            │
│  ┌────────────┬────────────┬────────────┬────────────┬────────────┬────────┐│
│  │ Channel    │ 18:00      │ 18:30      │ 19:00      │ 19:30      │ 20:00  ││
│  ├────────────┼────────────┼────────────┼────────────┼────────────┼────────┤│
│  │ [logo] 101 │ Evening    │ EastEnders │ EastEnders │ News       │ Movie  ││
│  │ BBC One    │ News       │            │            │            │        ││
│  ├────────────┼────────────┼────────────┼────────────┼────────────┼────────┤│
│  │ [logo] 201 │ The Lead   │ The Lead   │ Situation  │ Situation  │ ...    ││
│  │ CNN        │            │            │ Room       │ Room       │        ││
│  ├────────────┼────────────┼────────────┼────────────┼────────────┼────────┤│
│  │ [logo] 301 │ Movie: …   │ Movie: …   │ Movie: …   │ Movie: …   │ ...    ││
│  │ HBO        │            │            │            │            │        ││
│  └────────────┴────────────┴────────────┴────────────┴────────────┴────────┘│
│                              │ now │                                       │
└────────────────────────────────────────────────────────────────────────────┘
```

#### Phone

```
┌──────────────────────────────┐
│ TV Guide              🔍     │
│ [Today v] [All v] [♥]        │
│ ┌──────┬───────────────────┐ │
│ │ 101  │ Evening News      │ │
│ │ BBC  │ 18:00–19:00       │ │
│ ├──────┼───────────────────┤ │
│ │ 201  │ The Lead          │ │
│ │ CNN  │ 18:00–19:00       │ │
│ ├──────┼───────────────────┤ │
│ │ 301  │ Movie             │ │
│ │ HBO  │ 18:00–20:00       │ │
│ └──────┴───────────────────┘ │
│  ← scroll horizontally →     │
├──────────────────────────────┤
│ Home Live Movies Series More │
└──────────────────────────────┘
```

**Layout**

- Sticky channel column (160–200 dp on tablet/TV, 72 dp on phone) with logo, number and name.
- Time ruler at top; 30-minute slots by default. Zoom `−`/`+` switches between 1 hour, 2 hours and 4 hours of visible time.
- Programme blocks sized proportionally to duration; minimum width for readability; truncate title with ellipsis.
- Current time indicator: a 2 dp amber line with a "now" pill, updated every minute.
- Day selector: Today, Tomorrow, +2…+7 days (depending on provider). Use a compact date picker on TV.
- Channel filter: All, Favourites, category.
- Optional "Jump to now" action.
- Programme block content: title, start–end, catch-up icon, lock icon if parental-locked, progress for the current programme.
- Focused programme shows a preview card or a details sheet.

**Programme details sheet**

```
┌──────────────────────────────────────────────┐
│  [backdrop / still]                          │
│  EastEnders                                  │
│  19:30–20:00 · Drama · PG · BBC One          │
│                                              │
│  The residents of Albert Square face…        │
│                                              │
│  [ ▶ Watch live ]   [ ↺ Watch from start ]   │
│  [ ♥ Favourite channel ]                     │
└──────────────────────────────────────────────┘
```

- `Watch live` appears only for the current programme.
- `Watch from start` / `Catch up` appears only when the provider exposes a catch-up URL and the programme is within the archive window.
- If the programme is in the future, show `Remind me` only if reminders are reintroduced; otherwise omit.
- Show `Guide data unavailable` rather than an empty grid.

**Behaviour**

- Opening Guide defaults to the current time and the first favourite channel (or first channel).
- Horizontal scroll moves time; vertical scroll moves channels. The time ruler and channel column stay in sync.
- On TV, focus starts on the current programme of the first visible channel. Left/Right moves between programmes; Up/Down changes channels. Centre opens details; long centre plays the channel.
- On phone, tapping a programme opens the sheet; horizontal drag scrolls time; vertical drag scrolls channels.
- Pinch-to-zoom on touch changes the time scale; `−`/`+` on TV.
- Keep the now line accurate without re-laying out the whole grid every second.
- Cache the guide per day; show a subtle "Updated 10:32" label.

**States**

- Loading: skeleton grid with 5 channels × 4 slots.
- No data: "No guide data for this day." with a day selector and Retry.
- Partial data: render available programmes and gaps as neutral blocks.
- Offline: cached guide only; catch-up/play disabled with explanation.
- Parental lock: block title replaced with "Locked programme" and a lock icon; details require PIN.

**Acceptance:** the guide renders a full day for at least 50 channels without jank; now line is within 1 minute; catch-up action appears only when supported; D-pad focus never gets stuck.

---

### 9.6 Movies

**Purpose:** browse the movie library by category and find something to watch.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ SUNSET      Home   Live TV   Movies   Series   Guide        🔍   ♥    ◉    │
├────────────────────────────────────────────────────────────────────────────┤
│  Movies                                                                    │
│                                                                            │
│  [ All ] [ Action ] [ Comedy ] [ Drama ] [ Family ] [ Horror ]   [Sort v]  │
│                                                                            │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐         │
│  │ poster │ │ poster │ │ poster │ │ poster │ │ poster │ │ poster │         │
│  │  NEW   │ │        │ │ 4K     │ │        │ │        │ │  HD    │         │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘         │
│  Title      Title      Title      Title      Title      Title              │
│  2024       2023       2025       2022       2024       2021               │
│                                                                            │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐         │
│  │ poster │ │ poster │ │ poster │ │ poster │ │ poster │ │ poster │         │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘         │
└────────────────────────────────────────────────────────────────────────────┘
```

**Layout**

- Screen title + result count.
- Category chips: All, then provider categories. Keep "Favourites" as a chip if useful.
- Sort menu: Recently Added (default), A–Z, Rating, Year.
- Adaptive poster grid, 2:3 cards, 16 dp gap.
- Card content: poster, title (max 2 lines), year, badges (NEW, HD/4K, lock), progress if in progress.
- Infinite scroll with Paging 3; show a loading row, not a full-screen spinner.
- Optional featured strip at the top if the provider supports curated movies; otherwise start with the grid to keep the screen fast.

**Behaviour**

- Selecting a category resets the grid scroll to top.
- Selecting a card opens Movie details.
- Long-press/centre-hold opens quick actions: Play, My List, Details.
- Preserve filter and scroll position when returning from details.
- Pull-to-refresh on touch; Refresh action on TV.

**States**

- Loading: poster skeletons.
- Empty category: "No movies in this category."
- Offline: cached grid + banner; play disabled for uncached streams.
- Parental lock: poster/title visible but lock badge; details/play require PIN.

**Acceptance:** category switch responds in < 200 ms with cached data; grid holds 60 fps while scrolling; filters survive back navigation.

---

### 9.7 Movie details

**Purpose:** give enough information to decide, then make Play/Resume the obvious action.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [ full-bleed backdrop with top and bottom scrims ]                        │
│                                                                            │
│  ┌────────┐   The Hobbit: The Desolation of Smaug                         │
│  │ poster │   2013 · PG-13 · 3h 6m · ★ 7.4 · Adventure · Fantasy           │
│  │        │                                                                │
│  │        │   [ ▶ Resume 1:12:30 ]   [ ＋ My List ]   [ ⤴ Share ]          │
│  └────────┘                                                                │
│                                                                            │
│  Synopsis                                                                  │
│  The dwarves, along with Bilbo Baggins and Gandalf the Grey, continue…     │
│                                                                            │
│  Cast: Martin Freeman, Ian McKellen, Richard Armitage                      │
│  Director: Peter Jackson                                                   │
│                                                                            │
├────────────────────────────────────────────────────────────────────────────┤
│  More like this                                             See all →      │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                    │
│  │ poster │ │ poster │ │ poster │ │ poster │ │ poster │                    │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘                    │
└────────────────────────────────────────────────────────────────────────────┘
```

**Layout**

- **Phone:** backdrop at 16:9 (or 40% viewport), poster overlapping the bottom edge, title/meta below, primary button full width, secondary actions in a row, synopsis expandable, cast/director, similar titles.
- **Tablet/TV:** two-column top section: poster left (about 220–280 dp wide), metadata and actions right. Backdrop fills the top 60% behind a strong gradient.
- Show title, original title (if different), year, certification, runtime, rating, genres, audio/subtitle availability if known.
- Primary CTA: `Play`, `Resume 1:12:30`, or `Play again` with a restart option.
- Secondary: `My List`, `Share` (if a deep link exists), `Trailer` only if TMDB videos are enabled (not required for v1).
- Synopsis: 3 lines collapsed, "More" expands.
- Cast and director: horizontal chips or a compact line; do not overload.
- Similar titles: same genre, excluding current; max 20.
- Lock badge and rating reason if parental-locked.

**Behaviour**

- Back returns to the originating screen and restores scroll/focus.
- Play starts/resumes; if progress is 5–95%, show a small "Resume from 1:12:30" label on the button.
- If the stream fails, show an inline error with Retry and Switch player; keep the details visible.
- `My List` toggles immediately with a snackbar and Undo.
- On TV, initial focus is the primary CTA; Up moves to the top bar; Down moves to synopsis/similar titles.
- Deep link `/movie/{id}` opens the screen directly; if the id is unknown, show a graceful not-found state.

**Acceptance:** details load from cached metadata in < 300 ms; Play is reachable in one action from the screen; resume position is correct; similar titles never include the current title.

---

### 9.8 Series

**Purpose:** browse series and continue an in-progress show.

```
┌────────────────────────────────────────────────────────────────────────────┐
│ SUNSET      Home   Live TV   Movies   Series   Guide        🔍   ♥    ◉    │
├────────────────────────────────────────────────────────────────────────────┤
│  Series                                                                    │
│                                                                            │
│  [ All ] [ Drama ] [ Comedy ] [ Crime ] [ Documentary ]       [Sort v]     │
│                                                                            │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐         │
│  │ poster │ │ poster │ │ poster │ │ poster │ │ poster │ │ poster │         │
│  │  S3    │ │        │ │  NEW   │ │        │ │        │ │        │         │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘ └────────┘         │
│  Title      Title      Title      Title      Title      Title              │
│  3 seasons  2 seasons  1 season   4 seasons  2 seasons  3 seasons          │
└────────────────────────────────────────────────────────────────────────────┘
```

- Same grid/filter/sort/paging model as Movies.
- Card shows season count and, if in progress, "S2 E4" + progress.
- Selecting a card opens Series details.
- Optional "Continue Watching" rail at the top for series only.

**Acceptance:** series and movies share the same grid component and interaction model; progress labels are accurate.

---

### 9.9 Series details

**Purpose:** choose a season and episode, then play/resume with minimal friction.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  [ full-bleed backdrop ]                                                   │
│  ┌────────┐   Series Title                                                │
│  │ poster │   2024 · TV-MA · ★ 8.2 · Drama · 2 seasons                     │
│  │        │                                                                │
│  │        │   [ ▶ Resume S2 E4 ]   [ ＋ My List ]                          │
│  └────────┘                                                                │
│  Synopsis…                                                                 │
├────────────────────────────────────────────────────────────────────────────┤
│  [ Season 1 v ]   [ Season 2 v ]                     [ Episodes v ]         │
│                                                                            │
│  ┌──────────┐  E1  The Beginning                    42m   [ ▶ ]            │
│  │  still   │  Aired 12 Sep 2024 · Progress ▓▓▓░░░                       │
│  └──────────┘                                                              │
│  ┌──────────┐  E2  The Road                          45m   [ ▶ ]            │
│  │  still   │  Aired 19 Sep 2024                                           │
│  └──────────┘                                                              │
│  ┌──────────┐  E3  The Storm                         41m   [ ▶ ]            │
│  │  still   │  Aired 26 Sep 2024                                           │
│  └──────────┘                                                              │
└────────────────────────────────────────────────────────────────────────────┘
```

**Layout**

- Hero/backdrop + poster + title/meta + primary CTA.
- Season selector: horizontal chips or dropdown; default to the season containing the last-watched episode, otherwise season 1.
- Episode list: still, episode number/title, runtime, air date, description (2 lines), progress, play button. Current episode highlighted.
- `Resume S2 E4` uses the most recent incomplete episode; if none, `Play S1 E1`.
- On tablet/TV, episodes can be a two-column list or a horizontal row of episode cards; vertical list is preferred for readability.
- Show "Next episode" at the end of an episode via the player.

**Behaviour**

- Selecting an episode starts playback at the saved position or from the beginning.
- Changing season updates the episode list without losing the header.
- Favourite applies to the series, not the episode.
- Episode progress is per episode and displayed in the list.
- If a season has no episodes, show "Episodes coming soon" rather than an empty list.

**Acceptance:** resume chooses the correct episode; season switch is instant with cached data; episode progress is visible and accurate.

---

### 9.10 Search

**Purpose:** find any live channel, movie or series from one place.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Search                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ 🔍  Search live TV, movies and series…                              ✕ │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
│                                                                            │
│  Recent searches                                                           │
│  [ the hobbit ]  [ CNN ]  [ batman ]  [ comedy ]              Clear all    │
│                                                                            │
│  Results for “batman”                                                      │
│                                                                            │
│  Live TV (2)                                                               │
│  ┌──────────────┐ ┌──────────────┐                                         │
│  │ ● LIVE       │ │ ● LIVE       │                                         │
│  │ HBO          │ │ Cartoon Net  │                                         │
│  └──────────────┘ └──────────────┘                                         │
│                                                                            │
│  Movies (8)                                                                │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                               │
│  │ poster │ │ poster │ │ poster │ │ poster │                               │
│  └────────┘ └────────┘ └────────┘ └────────┘                               │
│                                                                            │
│  Series (3)                                                                │
│  ┌────────┐ ┌────────┐ ┌────────┐                                          │
│  │ poster │ │ poster │ │ poster │                                          │
│  └────────┘ └────────┘ └────────┘                                          │
└────────────────────────────────────────────────────────────────────────────┘
```

**Behaviour**

- Search is local-first against the cached catalog using SQLite FTS; debounce 250 ms, minimum 2 characters.
- Results grouped by type: Live TV, Movies, Series. Show counts. Limit each group to 6 with `See all`.
- Ranking: exact title match, prefix match, token match, then fuzzy; boost favourites and recently watched.
- Normalize accents and case (Spanish): "pelicula" matches "película".
- Recent searches: max 10, stored locally, clearable.
- If the local index is incomplete, fall back to a provider search endpoint if available; otherwise show "Results may be incomplete while the library updates."
- Selecting a result opens details or plays a channel.
- On TV, the search field is focused on entry; the keyboard is optional; recent searches and results are D-pad reachable.
- Empty query: show recent searches and suggestions (popular genres, favourites).
- No results: "No matches for '…'. Try a different title or browse Movies."

**Acceptance:** results appear within 300 ms for a warm index; Spanish accents are handled; search does not block on the network.

---

### 9.11 My List (Favourites)

**Purpose:** a single place for saved channels, movies and series.

```
┌────────────────────────────────────────────────────────────────────────────┐
│  My List                                                                   │
│  [ All ] [ Live TV ] [ Movies ] [ Series ]                                 │
│                                                                            │
│  Movies                                                                    │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                               │
│  │ poster │ │ poster │ │ poster │ │ poster │                               │
│  └────────┘ └────────┘ └────────┘ └────────┘                               │
│                                                                            │
│  Series                                                                    │
│  ┌────────┐ ┌────────┐                                                      │
│  │ poster │ │ poster │                                                      │
│  └────────┘ └────────┘                                                      │
└────────────────────────────────────────────────────────────────────────────┘
```

- Tabs/filters: All, Live TV, Movies, Series.
- Empty state: "Your list is empty. Add movies, series or channels with the ♥ button."
- Remove with a swipe/menu action and Undo.
- Sort: recently added (default), A–Z.
- Accessible from the top-bar heart, the phone `More` sheet, and the Home "My List" row.
- Favourites are local in v1; profiles will scope them per profile in v1.2.

**Acceptance:** adding/removing is instant and persists across restarts; the same item never appears twice.

---

### 9.12 Continue Watching

This is both a Home row and an optional full screen from "See all".

- Include movies and episodes with 5–95% progress.
- Sort by most recent; show progress bar and remaining time.
- Actions: Resume, Details, Remove from Continue Watching.
- Auto-remove at ≥ 95% or when the provider reports completion.
- For series, show the specific episode and "S2 E4" label; resuming opens that episode.
- If a title is no longer available from the provider, show a muted card with "No longer available" and allow removal.
- Store progress locally every 5 seconds during playback and on pause/stop/exit. Cap writes to avoid database churn.

**Acceptance:** resume returns to within ±2 seconds of the last position; completed items leave the row; removing an item does not delete the title from the catalog.

---

### 9.13 Player

**Purpose:** reliable, unobtrusive playback with clear controls on touch and remote.

#### Touch player (phone/tablet, landscape)

```
┌────────────────────────────────────────────────────────────────────────────┐
│  ←   The Hobbit: The Desolation of Smaug                         ⋯         │
│                                                                            │
│                                                                            │
│                              [ video ]                                     │
│                                                                            │
│                                                                            │
│                    ⏪ 10        ▶ / ⏸        ⏩ 10                         │
│       ──────────────────────●──────────────────────────────                │
│       12:34                                                    1:24:56     │
│                                                                            │
│   CC   Audio   Aspect   PiP   Next                                         │
└────────────────────────────────────────────────────────────────────────────┘
```

#### Live player

```
┌────────────────────────────────────────────────────────────────────────────┐
│  ←   ● LIVE   BBC One                                      [ Go to live ]  │
│                                                                            │
│                              [ video ]                                     │
│                                                                            │
│                    ⏪ 30        ▶ / ⏸        ⏩ 30                         │
│       ─────────────────────────────────────────────────────                │
│       18:42                              Evening News · 18:00–19:00         │
│                                                                            │
│   CC   Audio   Aspect   PiP                                                │
└────────────────────────────────────────────────────────────────────────────┘
```

**Controls**

- Top bar: back, title (truncated), LIVE badge for live, overflow menu (audio/subtitle, aspect, PiP, switch player, diagnostics).
- Centre: play/pause; double-tap left/right to seek ±10 s; tap to toggle controls.
- Bottom: seek bar with buffered range, elapsed/total or live time, next-episode action for series.
- Bottom action row: captions, audio track, aspect ratio, PiP, next episode (VOD/series only).
- Auto-hide controls after 4 seconds of inactivity during playback; keep visible when paused or when a sheet is open.
- Touch gestures: horizontal drag to scrub (with a time preview), vertical drag on the left to adjust brightness, on the right to adjust volume. Do not require gestures; buttons remain available.
- On TV: Centre play/pause; Left/Right seek ±10 s (VOD) or channel surf? For live, Left/Right opens the channel list rather than seeking; Up/Down shows controls; Back exits.
- Show a buffering spinner only after 500 ms to avoid flicker.
- Show a "Resume from 1:12:30" prompt for VOD if progress exists; options: Resume, Start over.
- Next episode: 10-second countdown sheet with `Play now`, `Cancel`, and an autoplay toggle.
- Live: `Go to live` appears when the user is behind the live edge. Catch-up shows a seek bar within the archive window and a `LIVE` badge that returns to live.
- PiP: enter on Home/gesture; playback continues; tapping PiP returns to the player. Respect system PiP settings.
- Background: pause video when the app leaves the foreground unless PiP is active; keep the media notification for active playback.
- Screen orientation: landscape on phones; allow sensor rotation if the user has rotation enabled; tablet/TV fixed landscape.
- Keep the screen awake during playback; restore the system setting on exit.
- Subtitles: render in-player; respect system caption size/style where possible; support sidecar and in-band tracks.
- Audio: remember the user's preferred language; fall back to the default track; show a toast/snackbar when the preferred track is unavailable.
- Aspect ratio: Fit (default), Fill, Zoom, 16:9, 4:3; persist per user.
- Quality: if the stream exposes variants, show Auto + options; HLS adaptive by default.
- Errors: classify network, auth, unsupported format, decoder, server. Offer Retry, Switch player, and Back. After two Media3 failures, recommend/auto-switch to VLC.
- Diagnostics: long-press the title or use the overflow to copy a redacted diagnostic report (stream type, engine, error code, device, app version). Never include credentials or full URLs.

**States**

| State | UI |
|---|---|
| Preparing | Poster/backdrop + spinner + title |
| Buffering | Subtle spinner over video, controls remain |
| Playing | Controls auto-hide |
| Paused | Controls visible, centre play |
| Ended (VOD) | Replay, Next episode, Back |
| Ended (live) | "This programme has ended" + Watch live / Guide |
| Error | Friendly message + Retry / Switch player / Back |
| Offline | "You're offline" + Retry when connection returns |

**Acceptance:** playback starts within 3 seconds on a 10 Mbps connection for a healthy stream; resume is within ±2 seconds; controls are reachable by touch and D-pad; switching engine preserves position; no credential or token appears in diagnostics.

---

### 9.14 Parental controls (v1.0 release gate)

**Purpose:** let a parent hide or lock mature content without making the app hard to use.

```
┌──────────────────────────────────────────────┐
│  Parental controls                           │
│                                              │
│  PIN protection                     [ OFF ]  │
│  Lock adult categories              [ ON  ]  │
│  Lock unrated content               [ OFF ]  │
│  Hide locked titles                 [ ON  ]  │
│                                              │
│  Locked categories                           │
│  [ Adult ] [ PPV ] [ Late night ]            │
│                                              │
│  Require PIN                                 │
│  ( ) Every session                           │
│  (•) Every 15 minutes                        │
│  ( ) Every play                              │
│                                              │
│  [ Change PIN ]        [ Forgot PIN? ]       │
└──────────────────────────────────────────────┘
```

**Setup flow**

1. Explain what parental controls do.
2. Create a 4–6 digit PIN; confirm it.
3. Optionally set a recovery hint (not the PIN itself).
4. Choose locked categories and whether to hide or blur locked titles.
5. Choose the unlock duration.

**Rules**

- PIN is hashed with a salted KDF (PBKDF2/Argon2) and stored in encrypted storage. Never store plaintext.
- After 5 failed attempts, lock for 30 seconds; escalate on repeated failures.
- "Forgot PIN?" requires signing out and resetting local preferences, or provider account verification if available. Do not reveal the PIN.
- Locked content is either hidden or shown with a lock badge, based on preference. Hidden mode must still allow search to explain that results are hidden.
- Unlocking applies to the session or a timed window (15 minutes by default).
- Adult category detection uses provider category names/flags plus optional manual selection; do not rely on title keywords alone.
- Parental state is local and applies to all profiles until profiles are introduced.
- The player blocks locked content even if launched via a deep link.

**Acceptance:** locked content cannot be played without a PIN; PIN is never logged or stored in plaintext; unlocking expires correctly; the app remains usable if the user forgets the PIN.

---

### 9.15 Settings

```
┌────────────────────────────────────────────────────────────────────────────┐
│  Settings                                                                  │
│                                                                            │
│  Account                                                                   │
│  Signed in as                                username@example.com       >  │
│  Plan status                                 Expires in 24 days            │
│  Sign out                                                                  │
│                                                                            │
│  Playback                                                                  │
│  Player engine                               Auto (Media3 → VLC)        >  │
│  Autoplay next episode                       [ ON ]                        │
│  Subtitles                                   [ OFF ]                       │
│  Subtitle appearance                         Default                    >  │
│  Audio language                              English                    >  │
│  Default aspect ratio                        Fit                        >  │
│  Picture-in-picture                          [ ON ]                        │
│                                                                            │
│  Appearance                                                                │
│  Language                                    English                    >  │
│  Reduce motion                               Follow system              >  │
│                                                                            │
│  Parental controls                                                         │
│  PIN protection                              [ OFF ]                    >  │
│                                                                            │
│  About                                                                     │
│  Version                                     1.0.0                        │
│  TMDB attribution                            This product uses…          >  │
│  Privacy policy                              Open                         >  │
│  Open-source licences                        Open                         >  │
│  Check for updates                           Last checked today          >  │
└────────────────────────────────────────────────────────────────────────────┘
```

**Sections**

- **Account:** username (masked if desired), plan status/expiry, device name, sign out (confirm).
- **Playback:** engine, autoplay, subtitles, subtitle appearance, audio language, aspect ratio, PiP, buffer mode (Auto/Data saver), maybe "Reset playback settings".
- **Appearance:** language (English/Español), reduce motion, maybe "Show mature content" (parental).
- **Parental controls:** entry point to PIN/locks.
- **About:** version/build, TMDB attribution, privacy policy, open-source licences, update check, support/contact.
- **Diagnostics** (hidden/advanced): app version, device, network type, player engine, last error, copy/share redacted logs.

**Behaviour**

- Changes apply immediately; no Save button.
- Destructive actions (sign out, clear history) require confirmation.
- Plan expiry warning appears in Settings and as a non-blocking banner on Home when ≤ 7 days remain.
- No ads, subscription or purchase options.
- On TV, settings use a two-column layout: section list left, detail right; on phone, a single scrolling list.

**Acceptance:** all settings persist across restarts; language change applies without a full restart (or with a clear relaunch prompt); sign out clears the session and protected local data.

---

### 9.16 Profiles (v1.2)

**Purpose:** let multiple viewers share one account with separate taste and history.

- Profile picker on launch if more than one profile exists.
- Create/edit/delete profile; name, avatar, maturity level, language, optional PIN.
- Each profile has its own favourites, continue watching, search history and parental settings.
- The provider account remains shared; profiles are local.
- "Kids" profile: hides locked categories, simplified Home, no adult search results.
- Switching profile is available from the account menu; playback stops and the shell reloads.
- Data model supports profiles from v1.0 even if the UI ships in v1.2.

**Acceptance:** switching profiles isolates history and favourites; deleting a profile removes its local data; kids profile cannot access locked content.

---

### 9.17 Cross-screen states

**Offline**

- Global banner: "You're offline. Showing downloaded information." (No downloads, so "Showing cached information.")
- Cached catalog remains browsable.
- Play actions are disabled with an explanation; live TV requires a connection.
- Retry automatically when connectivity returns; do not lose the current screen.

**Expired account**

- Non-blocking banner on Home/Settings: "Your account expires in N days."
- At expiry, block playback with a clear message and provider contact; keep browsing if cached data is available.

**Maintenance**

- Full-screen status with provider message, ETA and Retry.
- Do not show a broken shell.

**Not found**

- "We couldn't find that title. It may have been removed." with Back and Search.

**Parental lock**

- Locked poster shows a lock badge; details show a PIN prompt; the player refuses to start.
- Search results for locked items are hidden or labelled, depending on preference.

---

## 10. Component library

All components live in `:core:designsystem` (tokens, icons, primitives) or `:core:ui` (composed patterns). Feature modules must not re-implement them.

| Component | Purpose | Key specs |
|---|---|---|
| `SunsetTopBar` | Primary navigation on tablet/TV | 64/72 dp height; transparent → surface on scroll; 5 nav items + 3 actions; focusable |
| `SunsetBottomBar` | Primary navigation on phone | 64 dp + inset; 5 items; active amber; hides in player |
| `HeroBillboard` | Editorial focus on Home | 16:9 backdrop; dual scrims; eyebrow, title, meta, synopsis, 2 CTAs; 8 s rotation; reduced-motion aware |
| `ContentRow` | Horizontal rail | Header + See all; LazyRow; peek; snap; stable keys; TV focus scroll |
| `PosterCard` | Movie/series card | 2:3; 12 dp radius; badges; title/year; progress; lock; focus ring; long-press actions |
| `LandscapeCard` | Continue watching/next up | 16:9; progress; episode label; play overlay |
| `ChannelCard` | Live channel | Logo area; LIVE badge; number/name; now/next; progress; favourite |
| `ChannelRow` | Live list row | Number, logo, name, now, next, favourite, catch-up icon |
| `CategoryChip` | Filters | 32–40 dp height; selected amber; horizontal scroll; focus ring |
| `SectionHeader` | Row/screen titles | 24/32 sp; optional See all; count |
| `SearchField` | Search entry | 48 dp; leading search icon; clear; focus; TV keyboard support |
| `DetailHero` | Movie/series header | Backdrop, poster, meta, actions; responsive two-column |
| `EpisodeCard` | Episode list | Still 16:9; number/title; runtime/date; progress; play |
| `SeasonSelector` | Season switching | Chips/dropdown; scroll to selected; accessible labels |
| `GuideGrid` | EPG timeline | Sticky channel column; time ruler; programme cells; now line; zoom; D-pad |
| `ProgramCell` | Guide block | Title, time, catch-up/lock icons; proportional width; focus |
| `NowNextCard` | Programme preview | Now/next, progress, watch/catch-up actions |
| `LiveBadge` | On-air state | Red dot + "LIVE"; not colour-only |
| `ProgressBar` | Resume/completion | 3 dp track; amber fill; accessible value |
| `PrimaryButton` | Main action | Pill; amber; dark text; 48 dp min; loading state |
| `SecondaryButton` | Secondary action | Outline/tonal; 48 dp; icon optional |
| `IconButton` | Actions | 48 dp target; 24 dp icon; content description |
| `PlayerControls` | Playback UI | Auto-hide; touch/remote; seek bar; track/aspect/PiP actions |
| `TrackSheet` | Audio/subtitle/quality | Grouped list; selected check; close on selection |
| `PinPad` | Parental unlock | 4–6 digits; masked; error shake; lockout; accessible |
| `SettingsRow` | Settings item | Title, value, chevron/toggle; 56 dp min; TV focus |
| `EmptyState` | No content | Icon, title, body, one CTA |
| `ErrorState` | Failure | Human message, Retry, optional Switch player |
| `Skeleton` | Loading | Matches final layout; shimmer; reduced-motion static |
| `Snackbar` | Feedback | Undo for remove/favourite; 4 s; accessible |
| `ConfirmDialog` | Destructive actions | Clear title/body; cancel is default focus on TV |
| `LockBadge` | Parental state | Lock icon + label; not colour-only |

### 10.1 Component rules

- **States:** default, hover (pointer), focus (TV/keyboard), pressed, selected, disabled, loading, error. Document and test each.
- **Sizes:** use tokens, not ad-hoc dp. Cards scale with window size class.
- **Content:** every card supports missing artwork, long titles, missing year, 0/1/N badges.
- **TV:** focus ring and scale are part of the component, not added by screens.
- **Accessibility:** merge semantics for composite cards; expose a single meaningful label and action; do not announce decorative children.
- **Performance:** avoid nested scrollables; use stable keys; precompute image URLs; do not allocate in composition.

---

## 11. Content, metadata and editorial

### 11.1 Provider data mapping

| Domain object | Provider fields (to confirm) | Fallback |
|---|---|---|
| Live channel | id, name, number, logo, category, stream URL, EPG id, catch-up flag | Monogram logo, "Channel" |
| EPG programme | channel id, title, description, start, end, category, rating, catch-up URL | "No guide data" |
| Movie | id, title, year, plot, poster, backdrop, rating, duration, genre, cast, director, stream URL | TMDB or gradient placeholder |
| Series | id, title, year, plot, poster, backdrop, rating, genre, cast, seasons | TMDB or gradient placeholder |
| Season | number, name, episodes | "Season N" |
| Episode | id, season, episode, title, plot, duration, still, air date, stream URL | Episode number + gradient |
| Account | username, expiry, status, device/connection info | Unknown status |

The API adapter maps provider models to domain models. UI code never sees raw JSON or provider-specific field names.

### 11.2 TMDB enrichment

- Use TMDB only when provider metadata is missing or weak.
- Match order: provider TMDB id → provider IMDb id → title + year + type → title + type.
- Cache matches for 30 days; store confidence and source so a bad match can be corrected.
- Never overwrite a good provider poster with a poor TMDB image; choose by resolution and aspect.
- Prefer TMDB backdrops (16:9) for hero/details; prefer provider posters (2:3) unless missing.
- Fetch in the user's language (`en-US`/`es-ES`) with English fallback.
- Show the required TMDB attribution in About and on enriched detail screens: *"This product uses the TMDB API but is not endorsed or certified by TMDB."* Include the TMDB logo where their guidelines require it.
- Do not use TMDB for live channels or EPG.
- Rate-limit and cache image requests; use appropriate sizes (`w500` posters, `w1280` backdrops).
- The API key is supplied via a local Gradle property/BuildConfig, never committed. Rotate if leaked.

### 11.3 Artwork rules

- If no poster: gradient derived from the backdrop, or a neutral surface with the title's initials.
- If no backdrop: use the poster blurred/scaled behind a dark scrim; never upscale a tiny image.
- Channel logos: contain on a neutral tile; add a monogram fallback.
- Episode stills: 16:9; if missing, use the series backdrop.
- Preload the hero image and the first visible row; lazy-load the rest.
- Cache images on disk for 30 days with an LRU cap; respect HTTP cache headers when present.

### 11.4 "Recommended For You"

No server-side recommendation engine is assumed. Use a local, explainable heuristic:

1. Build genre affinity from the last 90 days of watch history and favourites.
2. Score unwatched movies/series by genre overlap, recency, rating and provider order.
3. Exclude completed titles and anything already in Continue Watching.
4. Diversify: at most 2 items per genre in the first 10.
5. If the profile has < 3 watched items, fall back to "New on Sunset" (recently added).
6. Never recommend locked content unless the profile is unlocked for it.

The row is optional; do not show it if it would be identical to "Recently Added".

### 11.5 EPG normalization

- Store all times in UTC; display in the device timezone.
- If the provider supplies local times without a timezone, use the provider timezone from config.
- Validate `end > start`; if missing, infer 30 minutes but mark as estimated.
- Dedupe programmes by channel + start + title.
- Cap absurd durations (> 12 hours) and flag them.
- Refresh guide data every 6–12 hours in the background; allow manual refresh.
- Keep at least yesterday/today/tomorrow cached for quick guide opens.
- If channel mapping fails, show the channel without guide data rather than hiding it.

### 11.6 Search index

- Build a local FTS index over channel names/numbers, movie titles/original titles, series titles, genre names and cast (if available).
- Normalize case, diacritics and punctuation.
- Rebuild incrementally after catalog sync; keep the last index usable while rebuilding.
- Rank favourites and recently watched higher.
- Store recent searches locally; never send them to a server.

---

## 12. Data and API architecture

### 12.1 Technology baseline

| Concern | Choice |
|---|---|
| Language | Kotlin (latest stable) |
| UI | Jetpack Compose + Material 3; Compose for TV (`androidx.tv`) |
| Architecture | MVVM + unidirectional data flow; repository/use-case boundaries |
| DI | Hilt |
| Async | Coroutines + Flow |
| Navigation | Navigation Compose (type-safe routes) |
| Local DB | Room (SQLite), FTS for search |
| Preferences | DataStore (Proto or Preferences) |
| Networking | Retrofit + OkHttp + kotlinx.serialization |
| Images | Coil 3 |
| Paging | Paging 3 |
| Background work | WorkManager (EPG/catalog refresh) |
| Media | AndroidX Media3 ExoPlayer + LibVLC fallback |
| Build | Gradle Kotlin DSL, version catalog, R8, baseline profiles |
| Min / target SDK | minSdk 23 recommended (confirm whether API 21/22 Fire TV devices must still be supported); compile/target latest stable (minimum 35) |
| Package | New package, e.g. `com.nathnetwork.sunset.v2` (final name TBD) |

### 12.2 Module structure

```
:app
:core:designsystem      tokens, typography, icons, primitives
:core:ui                shared composed components, state hosts
:core:model             domain models, IDs, result types
:core:network           Retrofit/OkHttp, auth, DTOs, mappers
:core:database          Room entities, DAOs, migrations, FTS
:core:data              repositories, sync, cache policy
:core:player            playback engine abstraction, Media3, VLC
:core:common            dispatchers, logging, analytics/crash wrapper
:feature:account        login, activation, session
:feature:home           hero, rails, recommendations
:feature:live           live TV list, now/next
:feature:guide          EPG grid, programme details
:feature:movies         movie grid
:feature:series         series grid
:feature:details        movie/series details
:feature:search         global search
:feature:mylist         favourites
:feature:player         player UI and controls
:feature:settings       settings, about, diagnostics
:feature:parental       PIN, locks (v1.0 release gate)
:feature:profiles       profile picker (v1.2)
```

Dependency direction: `feature → core:data/model/ui`; `core:data → core:network/database`; no feature depends on another feature. Shared navigation contracts live in `:core:ui` or a dedicated `:core:navigation`.

### 12.3 Provider abstraction

```kotlin
interface ContentProvider {
    suspend fun login(username: String, password: String): Session
    suspend fun restoreSession(token: String): Session
    suspend fun getAccountSummary(): AccountSummary

    suspend fun getLiveCategories(): List<Category>
    suspend fun getLiveChannels(categoryId: String?): List<LiveChannel>
    suspend fun getGuide(days: Int): List<EpgProgramme>

    suspend fun getMovieCategories(): List<Category>
    suspend fun getMovies(categoryId: String?, page: Int?): PagedResult<Movie>
    suspend fun getMovie(id: String): Movie

    suspend fun getSeriesCategories(): List<Category>
    suspend fun getSeries(categoryId: String?, page: Int?): PagedResult<SeriesSummary>
    suspend fun getSeriesDetail(id: String): SeriesDetail
    suspend fun getEpisodes(seriesId: String, season: Int): List<Episode>

    suspend fun resolveStream(request: StreamRequest): StreamSource
}
```

- v1 implementation: `EzServerContentProvider`.
- Future: `XtreamContentProvider`, `M3uContentProvider`. The UI must not change when the adapter changes.
- All provider calls return domain models or a typed `ProviderError`.
- Use tolerant JSON parsing: ignore unknown fields, provide defaults, and log schema drifts without crashing.

### 12.4 EZServer integration

Endpoints observed in the APK (base URL and exact parameters to confirm):

| Endpoint | Likely purpose | v1 use |
|---|---|---|
| `/token/createtoken?userid=…` | Create/obtain session token | Login |
| `/server/get_channel_category?token=…` | Live categories | Live |
| `/server/get_channel_list?token=…` | Live channels | Live |
| `/server/get_epg_info?token=…` | EPG programmes | Guide |
| `/server/get_movie_category?token=…` | Movie categories | Movies |
| `/server/get_movie_list?token=…` | Movie list | Movies |
| `/server/get_series_category?token=…` | Series categories | Series |
| `/server/get_series?token=…` | Series list | Series |
| `/server/get_series_profile?token=…` | Series detail/profile | Details |
| `/server/get_series_episode?token=…` | Episodes | Details |
| `/server/query_user_days_left?token=…` | Account expiry | Account |
| `/server/inquery_server_httpport?token=…` | Server/port info | Bootstrap |
| `/backup.json` | Legacy backup payload | Not used |

Also present but not required in v1: `player_api.php` and `xmltv.php` (Xtream), M3U parsing, `/server/get_*` variants for other panels.

**Authentication model (to confirm):**

1. User enters username/password.
2. App calls the token/login endpoint, with a stable app-scoped device identifier if required.
3. Provider returns a token and account metadata.
4. Token is stored encrypted and attached to `/server/*` calls as a query parameter or header (confirm which).
5. On 401/expired token, attempt one silent re-auth; otherwise return to Login.
6. Check days-left and account status after login and on app resume.

**Do not** send the IMEI, MAC address or Android ID. Use an app-generated UUID stored in encrypted preferences, and a user-visible device name. If the provider requires a MAC-like value, present it as a device registration code, not a hardware identifier.

### 12.5 API discovery plan (currently blocked)

**Current status:** there is no test server, no test account, no API documentation and no approved production access. Do not call the live provider. Do not ask for credentials in the initial build. Continue with a `FakeContentProvider`, local JSON fixtures and a stubbed `EzServerContentProvider` that is not wired to the network.

When access becomes available, proceed in this order:

1. **Obtain a test line or written API documentation** from the provider. This is the only low-risk path.
2. If only production access exists, obtain **explicit written approval** for a read-only discovery session before any call:
   - Capture only the login and catalog/EPG calls needed to define the contract.
   - Use a dedicated device/profile; do not run parallel streams.
   - Rate-limit requests; avoid repeated logins.
   - Do not share, publish or commit captured data.
3. Capture tooling: mitmproxy or Charles on a test device, with the owner's consent. If certificate pinning is present, document the obstacle and stop; do not bypass security controls.
4. Record request/response pairs as **redacted fixtures**: replace tokens, usernames, URLs and IDs with stable placeholders.
5. Document:
   - Base URL construction and panel selection.
   - Auth headers/query parameters and token lifetime.
   - Response envelopes, pagination, error codes and HTTP status usage.
   - Stream URL patterns for live, VOD, series, catch-up and timeshift.
   - Image URL patterns and supported sizes.
   - EPG format, timezone and update cadence.
6. Build contract tests that run against fixtures; add a manual "provider smoke test" that can be run only with an approved account.
7. Version the adapter (`EzServerV1`) and feature-flag breaking changes.

### 12.6 Domain models

| Entity | Key fields |
|---|---|
| `Account` | id, username, token (not in domain), expiry, status, deviceName, serverId |
| `Category` | id, type (LIVE/MOVIE/SERIES), name, parentId, isAdult, sortOrder |
| `LiveChannel` | id, name, number, logoUrl, categoryIds, streamRef, epgChannelId, hasCatchUp, catchUpDays |
| `EpgProgramme` | id, channelId, title, description, startUtc, endUtc, category, rating, iconUrl, catchUpRef |
| `Movie` | id, title, originalTitle, year, plot, posterUrl, backdropUrl, rating, durationMinutes, genres, cast, director, streamRef, tmdbId |
| `SeriesSummary` | id, title, year, posterUrl, backdropUrl, rating, seasonCount, streamRef, tmdbId |
| `SeriesDetail` | summary + plot, cast, genres, seasons |
| `Season` | number, name, episodeCount |
| `Episode` | id, seriesId, seasonNumber, episodeNumber, title, plot, durationMinutes, stillUrl, airDate, streamRef |
| `StreamRequest` | content type, id, episodeId?, startPositionMs?, live?, catchUp? |
| `StreamSource` | url, type (HLS/TS/MP4/OTHER), headers, drm?, expiry, engineHint |
| `WatchProgress` | contentId, type, episodeId?, positionMs, durationMs, updatedAt, completed |
| `Favourite` | contentId, type, addedAt |
| `ParentalRule` | categoryId?, rating?, action (HIDE/LOCK), profileId? |
| `SearchHistory` | query, lastUsedAt |
| `Profile` | id, name, avatar, maturity, language, isKids, pinHash? (v1.2) |

Use stable local IDs where provider IDs are not globally unique; keep a `providerId` + `providerType` pair for mapping.

### 12.7 Local storage and caching

**Room tables:** accounts, categories, live_channels, epg_programmes, movies, series, seasons, episodes, favourites, watch_progress, parental_rules, search_history, sync_state.

**Cache policy**

| Data | TTL | Strategy |
|---|---|---|
| Account summary | 5 min / on resume | Network-first, cached fallback |
| Categories | 24 h | Cache-first, refresh in background |
| Live channels | 12 h | Cache-first, refresh on pull |
| Movies/series lists | 6 h | Cache-first, refresh in background |
| Movie/series details | 24 h | Cache-first |
| EPG | 6–12 h | Cache per day; background refresh |
| TMDB metadata | 30 days | Cache-first, negative cache for misses |
| Images | 30 days | Disk LRU, HTTP cache when available |
| Search index | Rebuilt after catalog sync | FTS, incremental |

- Use stale-while-revalidate: show cached content immediately, then update.
- Never delete the last good catalog on a failed refresh.
- Clear cached catalog on sign out if the account changes; keep app settings.
- Use `allowBackup=false`; do not back up tokens or viewing history to Google.

### 12.8 Networking

- OkHttp with HTTP/2, gzip, connection pooling.
- Timeouts: connect 10 s, read 20 s, write 20 s; shorter for auth (10 s).
- Auth interceptor attaches the token; a 401 triggers one re-auth attempt, then emits `SessionExpired`.
- Retry only idempotent GETs; exponential backoff with jitter (max 3 attempts).
- Redact `Authorization`, `token`, `username`, `password` and full stream URLs from logs/crash reports.
- TLS: HTTPS required. If the provider only supports HTTP, allow it only for explicitly configured host(s) via network security config; never enable global cleartext. Show a one-time warning if HTTP is used.
- Certificate pinning: optional, only if the provider can guarantee certificate rotation. Do not pin by default; a broken pin bricks the app.
- User-Agent: stable app identifier; do not spoof another app/device.
- Handle captive portals and "connected but no internet" states distinctly.

### 12.9 Error taxonomy

| Error | Cause | UX |
|---|---|---|
| `InvalidCredentials` | Wrong username/password | Inline login error |
| `SessionExpired` | Token invalid/expired | Re-login prompt; preserve intended destination |
| `AccountExpired` | Plan ended | Blocking message + provider contact |
| `DeviceNotActive` | Activation required | Activation screen |
| `ConnectionLimit` | Too many streams | "Too many devices" + retry |
| `Maintenance` | Provider maintenance | Maintenance screen |
| `Network` | No connectivity/timeout | Retry, offline banner |
| `Server` | 5xx/unexpected payload | Retry + friendly message |
| `RateLimited` | 429 | Backoff + "Please wait" |
| `NotFound` | Content removed | Not-found state |
| `PlaybackUnsupported` | Codec/container | Switch player |
| `PlaybackAuth` | Stream token expired | Refresh stream URL, retry |
| `ParentalLocked` | PIN required | PIN prompt |
| `Unknown` | Anything else | Generic message + diagnostics |

---

## 13. Playback architecture

### 13.1 Engine abstraction

```kotlin
interface PlaybackEngine {
    val state: StateFlow<PlaybackState>
    suspend fun prepare(source: StreamSource, startPositionMs: Long)
    fun play(); fun pause(); fun seekTo(ms: Long)
    fun setSpeed(speed: Float)
    fun selectAudio(trackId: String?)
    fun selectSubtitle(trackId: String?)
    fun setAspect(mode: AspectMode)
    fun release()
}
```

Implementations: `Media3PlaybackEngine`, `VlcPlaybackEngine`. A `PlaybackController` owns the active engine, tracks, resume and error recovery. The UI observes a single `PlaybackUiState`.

### 13.2 Engine selection policy

| Situation | Engine |
|---|---|
| Default | Media3 |
| HLS/MP4/TS that Media3 supports | Media3 |
| Unsupported container/codec | VLC |
| Media3 decoder init failure twice | Offer/auto-switch to VLC |
| Media3 parsing error | VLC |
| Network/auth error | Do **not** switch; retry/refresh |
| User preference | Manual override in Settings |

- Remember the successful engine for the current stream/session; do not loop between engines.
- Show "Using compatible player" when VLC is active, with an option to switch back.
- Preserve position, audio/subtitle choice and aspect ratio across a switch.
- If both engines fail, show a diagnostic error and a Back action.
- **DRM:** not expected for this provider. If Widevine or another DRM scheme is introduced, Media3 is the only supported engine for those streams; disable VLC fallback for DRM content and add the appropriate licence acquisition flow.

### 13.3 Media3 configuration

- `ExoPlayer` with `DefaultMediaSourceFactory`; HLS, progressive, DASH if needed.
- `DefaultHttpDataSource.Factory` with the provider's required headers/User-Agent.
- `DefaultLoadControl`: target buffer 15–30 s VOD, 3–10 s live; tune per device/network.
- `DefaultLoadErrorHandlingPolicy` with limited retries; surface `PlaybackException.errorCode` to the error classifier.
- FFmpeg extension only if the provider uses audio codecs Media3 cannot decode (AC-3/E-AC-3/DTS). Otherwise rely on VLC fallback to keep APK size down.
- `MediaSessionService` for notifications/background/PiP; expose play/pause/seek/next.
- Hardware decoding preferred; allow fallback to software for problematic streams.
- Release the player when leaving the player screen; keep a single instance per playback session.

### 13.4 VLC configuration

- LibVLC with hardware decoding enabled; network caching 1500 ms (configurable 1000–3000).
- Subtitles: auto-detect, UTF-8 default, font size from user preference.
- Audio: passthrough only if the device supports it; otherwise decode.
- Handle VLC's own error/dialog callbacks and map them to the app's error taxonomy.
- Keep LibVLC initialization off the main thread; show the preparing state.
- Use ABI splits to control size; consider a "lite" build without VLC if the provider's streams are Media3-compatible (future option).

### 13.5 Resume and progress

- Save position every 5 seconds, on pause, on app background and on player release.
- Prompt to resume if position > 30 seconds and < 95% of duration.
- `Start over` sets position to 0 and clears the prompt for that session.
- Mark complete at ≥ 95% or when the player reports ended; remove from Continue Watching.
- For series, save per episode and derive the series' "next up" from the most recent incomplete episode.
- Clamp positions to duration; handle live streams (no resume).
- If the provider reports a different duration, prefer the player duration for progress calculations.

### 13.6 Live and catch-up

- Live streams: no seek bar; show `LIVE` and `Go to live` when behind the live edge.
- If the provider exposes timeshift, allow seeking within the window and show "Live" to jump back.
- Catch-up: resolve a separate stream URL/ref; show the programme's original time range and a seek bar within its duration.
- When a live programme ends, show "This programme has ended" with `Watch live` and `Guide` actions.
- Channel switching: Left/Right or a channel list overlay on TV; previous/next channel buttons on touch.
- Do not auto-play the next channel on live TV.

### 13.7 Tracks, aspect and PiP

- Audio: list languages/labels; selected track persisted per content type; fall back to default.
- Subtitles: Off, Auto, or a specific track; persist preference; show "CC" active state.
- Subtitle appearance: system default, size (small/medium/large), background (none/semi/opaque), colour (white/yellow).
- Aspect: Fit, Fill, Zoom, 16:9, 4:3; persist globally; reset per stream if unsupported.
- PiP: enable on Android 8+; auto-enter on Home gesture if playing; provide a PiP action; hide controls appropriately; TV PiP where supported.
- Audio focus: pause on loss unless transient; duck where appropriate; resume after transient loss.
- Screen wake: keep awake while playing; release on pause/exit.
- Headphone disconnect: pause.

### 13.8 Playback quality metrics

Collect only with opt-in, aggregated and non-identifying:

- Startup time (prepare → first frame).
- Rebuffering count and duration.
- Playback errors by class/engine.
- Dropped frames (device-level, optional).
- Engine used (Media3/VLC).
- Stream type (live/VOD/catch-up), not the URL or title.

Never collect credentials, tokens, full URLs, precise location or advertising IDs.

---

## 14. Performance and quality

### 14.1 Budgets

| Area | Budget |
|---|---|
| Cold start to first frame | ≤ 2.5 s mid-range phone; ≤ 4 s low-end Fire TV |
| Cold start to interactive Home | ≤ 3.5 s with cache |
| Warm start | ≤ 1 s |
| Screen transition | ≤ 250 ms; poster → details ≤ 300 ms |
| Scroll | 60 fps target; no frame > 32 ms in poster rails/guide under normal load |
| Image load | Placeholder immediately; hero ≤ 800 ms on 10 Mbps; posters lazy |
| Search results (warm index) | ≤ 300 ms |
| Guide cached open | ≤ 1 s; uncached ≤ 3 s |
| Player start | ≤ 3 s on 10 Mbps healthy stream; first frame feedback within 1 s |
| APK size | ≤ 60 MB per ABI; universal may be larger |
| Memory | ≤ 300 MB peak on mid-range; no leak across 20 screen navigations |
| Crash-free sessions | ≥ 99.5% |
| ANR rate | < 0.5% |

### 14.2 Startup strategy

1. Show a lightweight splash immediately.
2. Initialize only essential singletons (DI, DataStore, database handle).
3. Read session and cached Home data concurrently.
4. Compose Home from cache; start background refresh.
5. Defer EPG sync, TMDB enrichment, image prefetch and update checks.
6. Never block on network for the first frame.
7. Use AndroidX Startup or explicit initializers; avoid heavy work in `Application.onCreate`.

### 14.3 UI performance

- Use `LazyRow`/`LazyColumn` with stable keys and `contentType`.
- Avoid nested scrollables; the guide grid is a custom layout with a shared scroll state.
- Precompute image URLs and aspect ratios; do not allocate in composition.
- Use `derivedStateOf` for scroll-dependent chrome; avoid recomposing entire rows.
- Coil: memory + disk cache, downsampling, crossfade off for posters, prefetch hero/first row.
- Baseline Profiles for startup and key journeys.
- Macrobenchmark startup, scroll and navigation; track regressions in CI.
- Low-end TV mode: reduce simultaneous image requests, disable blur/parallax, cap animation duration.

### 14.4 Network performance

- Cache-first rendering; stale-while-revalidate.
- Parallelize independent catalog calls with a bounded dispatcher (e.g., 4).
- Paginate large lists; do not fetch the entire catalog on first launch.
- Compress images via provider/CDN sizes; never download a 4K backdrop for a phone card.
- Retry with exponential backoff; show cached data during retries.
- Prefer HLS adaptive streams; do not force the highest variant.
- Handle slow networks with lower buffer targets and clear buffering feedback.

### 14.5 Quality gates

- No P0/P1 crashes in the release candidate.
- All core journeys pass on the device matrix.
- Playback success ≥ 98% in the lab matrix.
- Accessibility audit: no critical issues; contrast and focus verified.
- Performance budgets met or explicitly waived with a plan.
- Privacy review: no ads/tracking SDKs, no sensitive data in logs.
- Security review: credentials encrypted, TLS enforced, no secrets in repo.
- Localization review: English and Spanish complete, no truncation at 200% font scale.

---

## 15. Security, privacy and compliance

### 15.1 Credentials and local data

- Store the session token and username in **EncryptedSharedPreferences/DataStore** backed by the Android Keystore.
- Store the password only if "Remember me" is explicitly enabled; otherwise keep it in memory for the login request only.
- Never log credentials, tokens, full stream URLs or provider responses containing personal data.
- `android:allowBackup="false"`; exclude sensitive files from backup/transfer.
- Block screenshots on the login and PIN screens (`FLAG_SECURE`) where appropriate; do not block the player globally.
- Clear the session and protected caches on sign out; confirm the action.
- Use a per-install UUID for device identity; do not collect IMEI, MAC, Android ID or advertising ID.

### 15.2 Network security

- HTTPS by default; no global `usesCleartextTraffic`.
- If the provider requires HTTP for stream hosts, allowlist only those hosts in `network_security_config.xml` and display a one-time security notice.
- Do not disable certificate validation. Do not bundle a custom trust store unless the provider uses a private CA and the certificate lifecycle is understood.
- Certificate pinning is optional and off by default; document the rotation plan if enabled.
- Validate redirects; do not forward auth tokens to third-party hosts.
- Redact query strings in logs; stream URLs often contain credentials.
- Use a stable, honest User-Agent; do not impersonate other apps.

### 15.3 Permissions (target)

| Permission | Why |
|---|---|
| `INTERNET` | API, images, playback |
| `ACCESS_NETWORK_STATE` | Connectivity/offline handling |
| `WAKE_LOCK` | Keep screen/CPU awake during playback |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Media session/playback notification |
| `POST_NOTIFICATIONS` (Android 13+) | Playback notification, optional |
| `REQUEST_INSTALL_PACKAGES` | In-app sideload update (optional; omit if updates are manual) |

No storage, camera, microphone, location, contacts, VPN, boot, or ad permissions. No `READ/WRITE_EXTERNAL_STORAGE`; use app-specific storage and the system file picker if needed.

### 15.4 Ads, analytics and crash reporting

- **Remove AdMob entirely**: SDK, ad units, `AD_ID`, `AD_MANAGER_APP`, ad activities/providers.
- **Remove Firebase Analytics/In-App Messaging/Installations**. Prefer a privacy-first crash SDK (e.g., Sentry/GlitchTip) with:
  - Opt-in on first run, or explicit "Share crash reports" in Settings.
  - PII scrubbing: no usernames, tokens, URLs, titles or device identifiers.
  - Aggregated, non-identifying events only.
- No behavioural analytics, session replay, heatmaps, ad IDs or cross-app tracking.
- Provide a privacy policy and a short in-app explanation of what is collected.
- Crash reports should include app version, build type, device model/OS, player engine, error class/code and a redacted stack trace.

### 15.5 Distribution and updates

- New package name; users install alongside or replace the old app manually.
- Sign release APKs with a dedicated keystore; store it securely and back it up.
- Direct APK distribution: publish per-ABI APKs plus a universal fallback; include SHA-256 checksums.
- In-app update check (v1.1): fetch a signed manifest with version, URL, SHA-256, minimum SDK and release notes. Verify the hash and the APK signature before prompting install.
- Use FileProvider and the system installer; never silently install.
- Provide a rollback path (keep the previous APK available).
- Display the version/build and a "Check for updates" action in About.

### 15.6 Compliance notes

- Confirm the provider's terms permit a replacement client and direct distribution.
- Confirm content rights for the regions where the app is distributed.
- Publish a privacy policy that matches the no-ads/no-analytics design.
- Provide open-source licence attribution (VLC, Media3, TMDB attribution, etc.).
- If the app is distributed to children, review applicable children's privacy rules; the Kids profile should not collect personal data.

---

## 16. Localisation

- Launch languages: **English (default)** and **Spanish**.
- Use `values/` and `values-es/`; no hardcoded user-facing strings.
- Support plurals, dates, times, numbers and durations through locale-aware formatters.
- Respect the device's 12/24-hour setting; the guide time ruler follows it.
- Spanish copy should be reviewed by a native speaker, especially provider/technical terms ("directo", "guía", "películas", "series", "favoritos", "continuar viendo").
- TMDB metadata: request `es-ES` with `en-US` fallback; show the original title on details when it differs.
- Do not translate provider content titles, channel names or category names; display them as supplied.
- Test long Spanish strings at 200% font scale and on narrow phones.
- RTL is not required for launch, but use start/end padding and avoid hard-coded left/right where practical.
- Language change applies immediately where possible; if a restart is required, show a clear prompt.

---

## 17. Testing and acceptance

### 17.1 Test layers

| Layer | Coverage |
|---|---|
| Unit | DTO mapping, error taxonomy, resume/completion rules, parental rules, recommendation heuristic, search normalization, EPG normalization |
| Integration | Repository + Room + mocked API; cache-first behavior; token refresh; migrations |
| UI (Compose) | Navigation, back behavior, focus order, form validation, empty/error states, settings persistence |
| Screenshot | Key screens at phone/tablet/TV sizes, light/dark? (dark only), English/Spanish, 100%/200% font scale |
| Playback lab | HLS live, VOD MP4, MPEG-TS, HEVC, AC-3, subtitles, audio tracks, catch-up, poor network, engine fallback |
| Device matrix | Small/large phone, tablet, Android TV 1080p/4K, Fire TV Stick, low-end device |
| Accessibility | TalkBack, Switch Access, focus order, contrast, captions, 200% font scale |
| Performance | Macrobenchmark startup/scroll/navigation; memory; APK size |
| Security | Secret scan, log redaction, TLS/cleartext review, exported components, backup settings |

### 17.2 Device matrix

- Phone: 360×800 dp (low-end), 412×915 dp (mid), 480×1080 dp (large).
- Tablet: 800×1280 dp, 1280×800 dp landscape.
- TV: 1080p and 4K, Android TV 9/11/14 where available, Fire OS equivalent.
- At least one device with 2 GB RAM and one with 4 GB+.
- Network: Wi-Fi, 4G, throttled 1.5 Mbps, high latency/loss, offline.

### 17.3 Acceptance criteria by journey

| Journey | Acceptance |
|---|---|
| First launch | Splash → login → Home in ≤ 60 s including typing; no crash; credentials stored securely |
| Returning launch | Splash → Home in ≤ 2.5 s; cached hero/rows visible; session restored |
| Browse movies | Filter/sort/paginate; open details; back restores scroll/filter |
| Play movie | Play from details; start ≤ 3 s; resume works; subtitle/audio/aspect controls work; back returns |
| Watch series | Select season/episode; resume correct episode; next episode works |
| Watch live | Home → Live → channel plays in ≤ 3 actions; now/next correct; favourite toggles |
| Use guide | Open guide; now line accurate; programme details; catch-up action only when supported; D-pad focus never stuck |
| Search | Query returns grouped results ≤ 300 ms warm; accent-insensitive; selecting a result opens/plays |
| Favourites | Add/remove from card/details/channel; persists; My List filters correctly |
| Continue watching | Progress saved; resume ±2 s; completed items removed; remove action works |
| Parental | PIN setup; locked content blocked; unlock expires; wrong PIN lockout; forgot PIN path |
| TV | Every action reachable by D-pad; focus visible; back predictable; 10-foot legibility |
| Accessibility | TalkBack labels; 200% font scale; contrast; captions; no focus traps |
| Offline | Cached browse works; clear banner; play disabled with explanation; recovery on reconnect |
| Expiry | Warning banner; at expiry playback blocked with provider contact |
| Error recovery | Media3 failure offers VLC; retry works; diagnostics redacted |

### 17.4 Definition of done (v1.0)

- All v1.0 scope items implemented and accepted.
- No P0/P1 defects; P2s documented with owners.
- Performance budgets met on the reference devices.
- Accessibility audit passed with no critical issues.
- English and Spanish strings complete; no truncation at 200%.
- Privacy/security checklist signed off.
- Release APK signed, checksummed, installable on a clean device, and upgrade-tested from the previous build.
- Support/help content and privacy policy published.
- Rollback APK and signing key backed up.

---

## 18. Roadmap and estimates

Estimates assume **one Android engineer, one product designer and part-time QA**, with API access available in Phase 0. They are indicative, not commitments.

| Phase | Duration | Deliverables |
|---|---|---|
| **0. Discovery & prototype** | 2 weeks | Mock fixtures and provider interface, data model, brand refresh, clickable prototype of Home/Live/Guide/Details/Player, device lab plan. API capture is blocked until access exists. |
| **1. Foundation** | 3 weeks | Project/modules, design system, app shell, navigation, login/session, secure storage, CI |
| **2. Catalog & browse** | 4 weeks | Home (hero + rails), movies, series, details, search, favourites, continue watching, TMDB enrichment |
| **3. Playback & live** | 3 weeks | Media3 player, VLC fallback, live TV, catch-up, resume, PiP, track/aspect controls |
| **4. Guide** | 3 weeks | EPG ingestion/normalization, timeline grid, now line, day/filter/zoom, programme details, catch-up entry |
| **5. Personalisation & settings** | 2 weeks | Recommendations heuristic, settings, language, about, diagnostics, offline/error states |
| **6. Parental & hardening** | 2 weeks | Parental controls, accessibility, performance, security, device QA, release candidate |
| **v1.0 total** | **~19 weeks** | Public build |
| **v1.1 Polish & update** | 1–2 weeks | Update check, diagnostics, TMDB refinement, Spanish review, accessibility fixes, performance tuning |
| **v1.2 Profiles** | 2 weeks | Profile picker, separate history/favourites, kids profile |
| **Phase 2 Web/desktop** | 8–12 weeks | Shared domain layer, responsive web/desktop client, browser playback (separate track) |

### 18.1 Critical path

1. API contract and stream URL formats.
2. Brand/logo and design tokens.
3. Player compatibility with the provider's real streams.
4. EPG data quality and timezone handling.
5. TV focus/performance on low-end hardware.
6. Signing/distribution and update path.

### 18.2 Milestones

- **M1:** API fixtures + clickable prototype approved.
- **M2:** Login + Home + Movies + Series + Details working against real data.
- **M3:** Live + player + catch-up working on phone/tablet/TV.
- **M4:** Guide + search + favourites + continue watching complete.
- **M5:** Parental + accessibility + performance + release candidate.
- **M6:** v1.0 public build.
- **M7:** Profiles (v1.2).
- **M8:** Web/desktop beta (phase 2).

---

## 19. Risks and mitigations

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| API is undocumented and changes | High | High | Provider abstraction, tolerant parsing, fixtures, contract tests, feature flags, approved test access when available |
| No test server, test credentials or API docs | High | High | Build against `FakeContentProvider` + fixtures; do not call production; require written approval for any read-only discovery; never commit credentials |
| Streams fail in Media3 | Medium | High | VLC fallback, device lab, per-stream engine memory, clear error recovery |
| EPG is incomplete or wrong timezone | High | Medium | UTC normalization, validation, fallback states, manual provider timezone config |
| TMDB matches wrong title | Medium | Medium | Match by IDs first, confidence threshold, cache, manual override, provider art priority |
| APK too large due to VLC | High | Medium | ABI splits, R8, optional VLC variant, WebP/artwork discipline |
| Low-end TV performance | Medium | High | Baseline profiles, image limits, no blur, lazy init, test on 2 GB device |
| Sideload update friction | Medium | Medium | Signed manifest, hash/signature verification, clear instructions, in-app check |
| New package loses existing users | High | Medium | Communicate reinstall; optional settings export/import; consider migration guide |
| Spanish copy quality | Medium | Low | Native review, screenshot tests, 200% font-scale checks |
| Parental controls bypassed | Low | High | Enforce at repository/player boundary, not just UI; hash PIN; test deep links |
| Credential leakage in logs/crashes | Medium | High | Redaction, no URL logging, secret scanning, crash SDK scrubbing |
| Provider terms/legal | Medium | High | Confirm rights and terms before distribution; privacy policy; legal review |
| Scope creep (DVR/VPN/multi-screen) | High | Medium | Explicit out-of-scope list; change control; revisit only after v1 metrics |
| Design drifts from reference | Medium | Low | Design tokens, component library, screenshot tests, design review per screen |

---

## 20. Open questions

1. **Exact API contract:** request/response JSON, pagination, error codes, token lifetime, panel/base URL selection.
2. **Authentication:** is device activation/MAC registration required for this account? What identifies a device?
3. **Stream formats:** which protocols/codecs does the provider actually serve for live, VOD, series and catch-up?
4. **Catch-up:** is it available, what is the archive window, and how are catch-up URLs constructed?
5. **EPG:** format, timezone, update cadence, channel mapping, days available.
6. **TMDB IDs:** does the provider expose TMDB/IMDb IDs? If not, approve title+year matching and manual overrides.
7. **Brand:** final logo/wordmark and app display name ("Sunset" vs "Sunset TV"); TV banner asset.
8. **Package name:** confirm `com.nathnetwork.sunset.v2` or another identifier.
9. **Update mechanism:** in-app update check vs manual APK distribution.
10. **Spanish review:** who approves the Spanish copy?
11. **Content ratings:** does the provider supply certifications/ratings, or should parental controls rely on category locks?
12. **Web/desktop:** confirm phase 2 scope and whether it is a browser app, desktop app, or both.
13. **Support:** provider contact details/help URL for login and account issues.
14. **Provider access:** there is currently no test server, test account or API documentation. When will access be available, and who approves a read-only discovery session if only production exists?
15. **Profiles:** should profiles ever sync to the provider, or remain local-only?
16. **Minimum Android version:** must Fire OS 5 / API 21–22 devices be supported, or can the app require API 23+?

---

## Appendix A — Current feature disposition

| Feature | Current app | New app |
|---|---|---|
| Login / account | Yes | Yes, redesigned |
| Device activation | Yes | Yes, if required |
| Live TV | Yes | Yes, redesigned |
| Movies (VOD) | Yes | Yes, redesigned |
| Series / episodes | Yes | Yes, redesigned |
| EPG / TV guide | Yes | Yes, timeline grid |
| Catch-up / archive | Yes | Yes (pending API) |
| Favourites | Yes | Yes |
| Continue watching | Yes | Yes, improved |
| Search | Yes | Yes, global |
| Parental controls | Yes | Yes, v1.0 release gate |
| Profiles | Yes | Yes, v1.2 |
| Recording / DVR | Yes | **Dropped** |
| Multi-screen | Yes | **Dropped** |
| Built-in VPN | Yes | **Dropped** |
| Cloud backup / restore | Yes | **Dropped** |
| Programme reminders | Yes | **Dropped** |
| Radio | Yes | **Dropped** |
| Chromecast | Yes | **Dropped** |
| Offline downloads | Partial/legacy | **Dropped** |
| Speed test | Yes | **Dropped** |
| Ads | AdMob | **Removed** |
| Firebase Analytics/In-App Messaging | Yes | **Removed** |
| Auto-update | Yes | Rebuilt for sideload (v1.1) |
| Multi-panel | Yes (PANEL 1–5) | One configured provider; optional advanced server selector |
| Xtream/M3U | Yes | Not in v1; adapter boundary retained |
| Web/desktop | No | Phase 2 |

---

## Appendix B — Endpoints observed in the APK

These are strings found during static analysis. They are **not yet validated** against the live provider and must be confirmed during API discovery.

```
/token/createtoken?userid=
/server/get_channel_category?token=
/server/get_channel_list?token=
/server/get_epg_info?token=
/server/get_movie_category?token=
/server/get_movie_list?token=
/server/get_series_category?token=
/server/get_series?token=
/server/get_series_profile?token=
/server/get_series_episode?token=
/server/query_user_days_left?token=
/server/inquery_server_httpport?token=
```

Legacy/compatibility strings also present:

```
/player_api.php?username=
/xmltv.php?username=
&action=get_live_streams
&action=get_vod_streams
&action=get_series
&action=get_series_info&series_id=
&action=get_vod_info&vod_id=
&action=get_simple_data_table&stream_id=
/timeshift/
```

Other observed configuration strings: `PANEL 1`–`PANEL 5`, `activation_url`, `activationXOR`, `xco_activation_cod`, `/backup.json`, `ottrun.com`.

Do not call production endpoints during automated tests or initial development. Use the fake provider and redacted fixtures. A live provider smoke test is permitted only with an approved test account.

---

## Appendix C — Design tokens

```json
{
  "color": {
    "bg": { "base": "#08090C", "surface": "#101218", "surface2": "#171A21", "surface3": "#1F232C" },
    "border": { "subtle": "#262A33" },
    "accent": { "primary": "#F5A524", "hover": "#FFC14D", "pressed": "#D98A12", "on": "#0B0B0F" },
    "text": { "primary": "#F7F7F8", "secondary": "#A7ABB5", "tertiary": "#7C818D" },
    "semantic": { "live": "#FF453A", "success": "#32D583", "warning": "#F5A524", "error": "#FF6B6B" },
    "focus": { "ring": "#FFD166" }
  },
  "typography": {
    "family": "Inter",
    "displayHero": { "size": 56, "lineHeight": 64, "weight": 700 },
    "displaySection": { "size": 32, "lineHeight": 40, "weight": 700 },
    "headline": { "size": 24, "lineHeight": 32, "weight": 600 },
    "title": { "size": 20, "lineHeight": 28, "weight": 600 },
    "body": { "size": 16, "lineHeight": 24, "weight": 400 },
    "label": { "size": 14, "lineHeight": 20, "weight": 600 },
    "caption": { "size": 13, "lineHeight": 18, "weight": 400 },
    "micro": { "size": 11, "lineHeight": 14, "weight": 700 }
  },
  "spacing": { "xs": 4, "sm": 8, "md": 12, "lg": 16, "xl": 24, "2xl": 32, "3xl": 48, "4xl": 64 },
  "radius": { "sm": 8, "md": 12, "lg": 16, "xl": 24, "pill": 999 },
  "motion": {
    "fast": 150,
    "standard": 250,
    "slow": 400,
    "hero": 500,
    "easing": "cubic-bezier(0.2, 0, 0, 1)"
  },
  "layout": {
    "gutterPhone": 16,
    "gutterTablet": 24,
    "gutterTv": 48,
    "topBarPhone": 64,
    "topBarTv": 72,
    "bottomBar": 64,
    "posterAspect": "2:3",
    "backdropAspect": "16:9"
  }
}
```

---

## Appendix D — Glossary

| Term | Meaning |
|---|---|
| **EZServer** | The provider's custom, token-based API used by the current app. |
| **Xtream Codes** | A common IPTV provider API using `player_api.php`; not required in v1. |
| **EPG** | Electronic Programme Guide; the TV schedule data. |
| **Catch-up / archive** | Replaying a programme that has already aired, within the provider's window. |
| **Timeshift** | Seeking within a live stream's buffer/archive window. |
| **VOD** | Video on demand; movies and series episodes. |
| **Media3** | AndroidX media playback library (successor to ExoPlayer 2). |
| **LibVLC** | VLC's native playback engine, used as a compatibility fallback. |
| **TMDB** | The Movie Database; metadata/artwork enrichment source. |
| **PiP** | Picture-in-picture. |
| **D-pad** | Directional pad on a TV remote. |
| **Overscan** | The unsafe area at the edges of a TV screen; keep critical UI inside 5%. |
| **Focus ring** | The visible outline showing which TV/keyboard element is selected. |
| **FTS** | Full-text search, used for the local search index. |

---

*End of document.*





