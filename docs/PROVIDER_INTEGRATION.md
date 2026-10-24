# Provider integration guide

Status: **live in debug; release still fails closed.** The panel in use is a standard
**Xtream Codes** server, not the EZServer paths listed below, so the Xtream adapter is the one that
ships. The release adapter `EzServerContentProvider` still throws
`ProviderError.ServiceNotConfigured` until release access is approved.

## 0. Confirmed integration (2026-10-23)

The panel is a stock **Xtream Codes** server, not the EZServer paths in §1. The real host, port and
streaming IPs are kept out of the repository (see §0.6); the adapter takes them from the login form.

### 0.1 Endpoint

| Item | Value |
| --- | --- |
| Base URL | `http://<provider-host>:8080` (plain HTTP, nginx) |
| API path | `/player_api.php` |
| Auth | `GET /player_api.php?username=<user>&password=<pass>` |
| Actions | `&action=get_live_categories`, `get_live_streams`, `get_vod_categories`, `get_vod_streams`, `get_series_categories`, `get_series`, `get_series_info`, `get_vod_info`, `get_short_epg` |
| Live stream | `GET /live/<user>/<pass>/<stream_id>.ts` (302 to the streaming host) |
| Movie stream | `/movie/<user>/<pass>/<stream_id>.<container_extension>` |
| Episode stream | `/series/<user>/<pass>/<episode_id>.<container_extension>` |
| No pagination | Lists are returned whole; page client-side |

### 0.2 Auth response (observed)

`user_info`: `username`, `auth` (`1` = ok), `status` (`Active`), `exp_date` (epoch **seconds**),
`max_connections` (string), `allowed_output_formats` (`["m3u8","ts"]`), `is_trial`.
`server_info`: `time_now`, `timezone` (`America/Mexico_City`), `url`, `port`.
Failure is `auth: 0` with a status such as `Disabled`/`Expired`, not an HTTP error. Types are
inconsistent between panels (`1` vs `"1"`, ids as number or string), hence the flexible serializers
in `XtreamJson.kt`.

### 0.3 Catalogue (observed)

| Call | Items | Shape |
| --- | --- | --- |
| `get_live_categories` | 61 | `category_id`, `category_name`, `parent_id` |
| `get_live_streams` | 1,130 | `num`, `name`, `stream_id`, `stream_icon`, `epg_channel_id`, `category_id`, `tv_archive`, `tv_archive_duration` |
| `get_vod_categories` | 40 | same category shape |
| `get_vod_streams` | 52,499 | `num`, `name`, `stream_id`, `stream_icon`, `rating`, `added`, `category_id`, `container_extension` |
| `get_series_categories` | 36 | same category shape |
| `get_series` | 12,966 | `num`, `name`, `series_id`, `cover`, `plot`, `cast`, `genre`, `releaseDate`, `rating`, `category_id` |

`get_vod_streams` is **16.6 MB** and takes ~10 s from a desktop: it must be cached (Room or disk),
never fetched on every launch. Live lists are small (~400 KB) and cached for 6 h.

### 0.4 WAF and transport rules

- The default `okhttp` user agent gets **403 Forbidden** (nginx). Every request sends
  `SunsetTV/2.0 (Android)`; verified 200 with that agent.
- `Range` requests are rejected (404/403), so payload size can only be measured by reading the body.
- The API is HTTP. Streams redirect to a **raw IP on port 8080** that changes between sessions, so a
  host allowlist cannot cover playback: release keeps the scoped policy, debug permits cleartext.
- The account allows 3 simultaneous connections; the player uses one.

### 0.5 How the host was recovered (repeatable)

The legacy app (`com.nathnetwork.sunset`, XCIPTV 5.0.1) has no server field: the panel is baked in
("SELECT PANEL -> SUNSET MEDIA"), the account screen shows only user/expiry, and the app is not
debuggable, so `run-as`, `/proc` and `logcat` do not expose the URL.

1. Confirm the API host IP from the device: `adb shell cat /proc/net/tcp`, filter the app UID.
2. TLS certificate on that IP: `CN=pixxxels.cc` (`s1..s33` SANs) -> shared hosting, hostname still
   needed.
3. Capture the hostname with the Android system proxy: run a listener that logs only the request
   line, then `adb shell settings put global http_proxy <pc-ip>:8888`, relaunch the app, read the
   plain-HTTP `GET /player_api.php?...` line, then reset the proxy to `:0`.
4. `adb reverse` is broken on this adb build, and the Fire TV cannot reach the PC without a temporary
   inbound firewall rule for the listener port (remove it afterwards).
5. Delete the capture log: the plain-HTTP request line contains the credentials.

### 0.6 What is stored where

| Data | Location |
| --- | --- |
| API shape, WAF rules, stream patterns, discovery steps | this file (committed) |
| Host, port, streaming IPs, account username | `provider.local.md` (gitignored, never committed) |
| Password | not stored in the repo or in `provider.local.md`; enter it on the login screen |
| Runtime session token (credentials, encrypted) | Android Keystore via `EncryptedSessionStore` |
| Legacy app on the Fire TV | left signed in; used only for discovery |

No provider hostname is tracked. A variant-specific network security config is generated at build
time (`GenerateNetworkSecurityConfig`) from `sunset.provider.host` in the gitignored
`local.properties`; without that key the release config permits no cleartext host. Debug permits
cleartext because the live stream redirects to a raw-IP host that varies by session. Release
playback remains blocked until there is a stable approved stream-host policy or HTTPS.

### 0.7 Status

Implemented: login, account summary, live categories/channels and playback, the VOD catalogue with
detail (`get_vod_info`), the series catalogue with detail and episodes (`get_series_info`),
three-way search, Keystore session storage, a runtime server field and a debug provider router
(demo vs live).

**Catalogue handling.** VOD and series lists are fetched once, cached on disk for six hours and
paged in memory (60 per page): the panel has no pagination and the VOD payload is 16.6 MB. The cache
is keyed by account, survives restarts and is dropped only when a different account signs in. Title
years are split out of names like `Name (1999) SUB`, and playback uses the panel's
`container_extension` instead of assuming MP4.

**Known limits.** Some VOD files are HEVC/MKV and fail on older devices with "stream not
supported" — a device codec limit, not a provider error. EPG and catch-up are still pending.

Verified on the Fire TV (2026-10-24): live login and playback; movie grid with posters and captions;
movie detail with backdrop, runtime, genre, plot and cast from `get_vod_info`; movie playback;
series grid; series detail with seasons/episodes and episode playback; search across live, movies
and series.

## 1. What is known (legacy EZServer paths)

The legacy APK exposed the endpoint names below (spec Appendix B). These are the only known API
facts. There is no approved base URL, request/response schema, authentication header format,
pagination contract or stream format guarantee in this repository.

| Endpoint constant | Path (names only) | Intended `ContentProvider` method |
| --- | --- | --- |
| `CREATE_TOKEN` | `/token/createtoken?userid=` | `login`, `restoreSession` |
| `LIVE_CATEGORIES` | `/server/get_channel_category?token=` | `getLiveCategories` |
| `LIVE_CHANNELS` | `/server/get_channel_list?token=` | `getLiveChannels` |
| `EPG` | `/server/get_epg_info?token=` | `getGuide` |
| `MOVIE_CATEGORIES` | `/server/get_movie_category?token=` | `getMovieCategories` |
| `MOVIES` | `/server/get_movie_list?token=` | `getMovies`, `getMovie` |
| `SERIES_CATEGORIES` | `/server/get_series_category?token=` | `getSeriesCategories` |
| `SERIES` | `/server/get_series?token=` | `getSeries` |
| `SERIES_PROFILE` | `/server/get_series_profile?token=` | `getSeriesDetail` |
| `SERIES_EPISODES` | `/server/get_series_episode?token=` | `getEpisodes` |
| `USER_DAYS_LEFT` | `/server/query_user_days_left?token=` | `getAccountSummary` |
| `SERVER_HTTP_PORT` | `/server/inquery_server_httpport?token=` | future stream host discovery |

`EzServerEndpoints` in `core/network/.../EzServerConfig.kt` is the single reviewable list. Do not add
guessed paths, DTOs or URLs outside an approved integration change.

## 2. What is missing

To implement the adapter we need, in writing from the provider:

1. An approved **test-line** host and port, or explicit permission to use a named sandbox.
2. Authentication flow: how the username/password are exchanged, token lifetime, refresh and
   revocation. The `createtoken?userid=` query suggests a user id, not a password, but this is
   unconfirmed.
3. Exact JSON (or other) response shapes for each endpoint, including error envelopes and status
   codes.
4. Pagination semantics for movie/series lists.
5. EPG time zone, duration and catch-up fields.
6. Stream resolution: direct URL vs. redirect, container/DRM expectations, per-device limits, and
   whether URLs are short-lived.
7. Rate limits, maintenance behaviour and connection-limit error codes.
8. Whether cleartext HTTP is ever required. The current manifest sets
   `android:usesCleartextTraffic="false"`; any exception needs a scoped network security config and
   security review.

Without these, writing an adapter would mean inventing a schema, which is explicitly out of scope.

## 3. Guardrails

- **No production call** from this scaffold. The release adapter is fail-closed.
- **No credentials or hostnames in git.** Use `local.properties` or environment variables supplied at
  build time; never commit them. `gradle.properties` intentionally contains no secrets.
- **No mock auth in release.** Debug binds `FakeContentProvider`; release binds the blocked adapter.
  Do not add a debug-only bypass to release code.
- **No guessed DTOs.** Keep provider DTOs inside `:core:network`, mapped to domain models at the
  boundary.
- **TLS only** unless a security review approves a scoped cleartext exception.
- **Redact logs.** Never log query strings, tokens, credentials or full stream URLs.
- **Token storage.** Replace the DataStore session with Keystore-backed storage before real auth.
  The current store is suitable only for the synthetic demo token.
- **Feature flag** the rollout and keep the "Service not configured" state for builds without an
  approved configuration.
- **Respect connection limits** and surface typed errors (`ConnectionLimit`, `SessionExpired`,
  `DeviceNotActive`, `Maintenance`).

## 4. Implementation sequence

1. **Approve access and freeze fixtures.** Obtain the test line and redacted sample responses.
2. **Add configuration.** Extend `EzServerConfig` with a base URL and approved cleartext hosts only;
   supply values from `local.properties`/environment. Keep the default empty.
3. **Define DTOs and a Retrofit API** in `:core:network`, one interface per endpoint group. Add
   serialization tests with redacted fixtures.
4. **Implement the adapter** in `EzServerContentProvider`:
   - map DTOs to `Movie`, `SeriesSummary`, `SeriesDetail`, `Episode`, `LiveChannel`, `EpgProgramme`;
   - map HTTP/auth failures to `ProviderError`;
   - implement paging using the confirmed contract, not an invented page size;
   - resolve streams into `StreamSource` with format and headers.
5. **Wire token storage.** Add a Keystore-backed `SessionStore`; keep the interface unchanged.
6. **Contract tests.** Run against the sandbox/test line only, with credentials supplied by the
   environment. Never run them against production by default.
7. **Manual validation.** Verify login, expiry, connection limit, guide time zones, movie/series
   paging, episode playback and live playback on phone and TV.
8. **Rollout.** Keep the blocked state as the default for builds without config. Enable by feature
   flag for approved testers.

## 5. Suggested `ContentProvider` mapping

| Method | Expected behaviour |
| --- | --- |
| `login` | Exchange credentials for a token; return `Session` with expiry; never persist the password |
| `restoreSession` | Validate a stored token; throw `SessionExpired`/`AccountExpired` on failure |
| `getAccountSummary` | Days left/status/device name/connection limit |
| `getLiveCategories` / `getLiveChannels` | Categories and channels, stable ids |
| `getGuide` | Normalize to UTC epoch millis; mark estimated ends |
| `getMovieCategories` / `getMovies` | Category filter + confirmed pagination |
| `getMovie` | Single movie details |
| `getSeriesCategories` / `getSeries` | Category filter + confirmed pagination |
| `getSeriesDetail` | Summary, seasons, episodes (or merge with `getEpisodes`) |
| `getEpisodes` | Season episodes with stable ids |
| `search` | Map provider search results into `SearchResults`; keep `androidx.paging` in data layer |
| `resolveStream` | Return `StreamSource`; set `StreamFormat`, headers and expiry |

## 6. Error mapping

| Provider condition | `ProviderError` |
| --- | --- |
| Bad username/password | `InvalidCredentials` |
| Expired token | `SessionExpired` |
| Expired account | `AccountExpired` |
| Device not activated | `DeviceNotActive` |
| Too many streams | `ConnectionLimit` |
| Maintenance page/response | `Maintenance` |
| Timeout / no route | `Network` |
| 5xx or malformed payload | `Server` |
| 429 | `RateLimited` |
| Unknown content id | `NotFound` |
| Unsupported codec/container | `PlaybackUnsupported` |
| Stream URL unauthorized | `PlaybackAuth` |
| Parental rejection | `ParentalLocked` |
| Missing config | `ServiceNotConfigured` |

## 7. Open questions

- Is `createtoken?userid=` the only login step, and how is the password verified?
- Are tokens bearer headers, query parameters, or both?
- Are stream URLs short-lived, and do they require per-request headers or device binding?
- What are the real page-size limits and total-count semantics?
- Which time zone does EPG use, and how are overnight programmes represented?
- Are there separate endpoints for catch-up and live, or a single resolver?
- What are the exact error bodies for expiry, connection limit and maintenance?
- Is HTTP (cleartext) required on any approved test host?

## 8. Definition of done for integration

- [ ] Written approval and test-line credentials received.
- [ ] Redacted fixtures and schema documentation checked into the repo (no secrets).
- [ ] Adapter implemented behind `ContentProvider`; no provider types leak into features.
- [ ] Contract tests pass against the approved test line.
- [ ] Keystore-backed token storage replaces the demo DataStore token.
- [ ] Release build without configuration still shows "Service not configured".
- [ ] Manual phone/TV checklist passes.
- [ ] Security review of logging, TLS, token handling and stream URLs.
