# Provider integration guide

Status: **BLOCKED — do not call production.**

The only permitted integration target is `ContentProvider` in `:core:model`. The release adapter
`EzServerContentProvider` currently throws `ProviderError.ServiceNotConfigured` for every call.

## 1. What is known

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
