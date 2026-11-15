# Machine Coding Starter (Android): `feature/auth-token-refresh`

Starts from `02-posts-list` and adds the networking side of authentication. JSONPlaceholder has no login, so with no token saved the app behaves exactly as before.

**What this feature shows** (`core/network/auth/`):
- **`AuthInterceptor`:** adds `Authorization: Bearer <token>` when a token exists. Public endpoints opt out with `@Headers("No-Auth: true")`.
- **`TokenAuthenticator`:** OkHttp calls it on every 401.
  - `synchronized`: 5 parallel 401s cause **one** refresh. Threads that waited notice that the token already changed and reuse it.
  - The number of retries is capped, so a rejected new token can't loop forever.
  - The refresh call uses a separate `@AuthClient` OkHttp client without the authenticator, so it can't recurse. The two clients share a connection pool through `newBuilder()`.
  - Only a 400/401/403 from the refresh endpoint logs the user out. A 5xx, a timeout or bad JSON keeps the session.
- **`DataStoreTokenStore`:** Preferences DataStore behind a `TokenStore` interface. `isLoggedIn: Flow<Boolean>` is meant to be observed at the app root to send the user to login. In production, encrypt the values (e.g. with Tink).
- **`AuthApi`:** a placeholder `POST auth/refresh`. Rename it to match your backend.
- **Logging:** the `Authorization` header is redacted in OkHttp logs.
- **Tests (5, with MockWebServer):** refresh and retry, parallel 401s causing a single refresh, a rejected refresh token logging out, a server error keeping the session, and no token meaning no header and no refresh.

```bash
git diff 02-posts-list..feature/auth-token-refresh
```

## Follow-up: LLD (low-level design)
- **Responsibilities:** `AuthInterceptor` (adds the header), `TokenAuthenticator` (reacts to 401), `TokenStore` (persistence behind an interface), `AuthApi` (refresh call on a separate client). Why each is separate and testable on its own.
- **Concurrency:** `synchronized` plus the "already refreshed?" check means N parallel 401s cause one refresh. Why not a coroutine `Mutex`? `Authenticator` is a blocking OkHttp API, so `runBlocking` inside is acceptable there and nowhere else.
- **Loop protection:** count prior responses and give up after a limit; the refresh client has no authenticator, so it can't recurse.
- **Logout policy as a decision table:** 400/401/403 from refresh → clear tokens; 5xx, timeout or bad JSON → keep the session. Tested with MockWebServer.
- **Opt-out:** a `No-Auth` header for public endpoints instead of a URL allow-list in the interceptor.

## Follow-up: HLD (high-level design)
- **Token model:** short-lived access token (minutes) plus a long-lived, rotating refresh token; refresh-token reuse detection on the server revokes the whole session family.
- **Where secrets live:** the client never holds client secrets; OAuth 2.0 with PKCE for login. Tokens encrypted at rest (Keystore-backed).
- **Logout everywhere:** server-side session list; revoking a session makes its refresh token fail with 401, which the client turns into a logout.
- **Observability:** refresh rate, refresh failures by status code, forced logouts per app version.
- **Multiple processes or apps:** a single source of truth for tokens (one process, or a ContentProvider), otherwise two processes rotate the refresh token and one gets logged out.

## Follow-up: System design (mock round)
**Prompt:** "Design authentication and session management for a banking or payments app." About 45 minutes.
1. **Requirements:** login (password + OTP or biometrics), stay signed in safely, log out on all devices, step-up auth for sensitive actions. Non-functional: secure storage, no secrets in the app, resilient to network blips.
2. **Client architecture:** auth module with `AuthInterceptor`, `TokenAuthenticator`, encrypted `TokenStore`, and a session state (`LoggedIn`/`LoggedOut`) observed at the app root.
3. **API and data model:** OAuth 2.0 with PKCE; short-lived access tokens, rotating refresh tokens, a server-side session list per user and device.
4. **Data flow:** every request carries the access token; a 401 triggers one refresh shared by all requests; a rejected refresh logs out; biometrics unlock a Keystore key for step-up.
5. **Scale and performance:** stateless JWT validation at the gateway, refresh-token reuse detection, rate limits on login and OTP.
6. **Trade-offs to state:** token lifetimes, certificate pinning risk, how much to trust device biometrics, offline access to cached data.

## Branches

Each branch is a step you can check out and practise from. Feature branches all start from `02-posts-list`, so a diff against it shows exactly one feature.

| Branch | What it adds |
|---|---|
| `00-empty` | Compose + Hilt app, all dependencies, theme, Timber, ktlint/detekt, a blank screen. This is what many interviewers hand you. |
| `01-minimal` | Core infrastructure with no features: Retrofit/OkHttp/Json, `safeCall`, `AppResult`, MVI base, `UiText`/`UiState`, error messages, shared composables, navigation shell |
| `02-posts-list` | A basic feature: fetch posts, cache in Room (offline-first), loading/error/empty states, pull-to-refresh, MVI |
| `feature/post-detail` | Detail screen (MVVM), type-safe navigation argument, network fallback for uncached posts |
| `feature/pagination-manual` | Hand-written load-more with a reusable `Paginator`, scroll trigger, footer states |
| `feature/search-debounce` | Server-side search: debounce, `distinctUntilChanged`, `flatMapLatest` cancellation, `SavedStateHandle` |
| `feature/paging3` | Paging 3 with `RemoteMediator`, a Room `PagingSource` and remote keys |
| `feature/auth-token-refresh` | Auth interceptor, a synchronized refresh `Authenticator`, DataStore token store, MockWebServer tests |
| `main` | Everything combined, plus favorites and search with pagination |

```bash
git checkout 02-posts-list && git checkout -b my-attempt   # practise a feature yourself
git diff 02-posts-list..feature/search-debounce            # then compare with the reference
```

## Run

Open in Android Studio (JDK 17), or:

```bash
./gradlew assembleDebug testDebugUnitTest
./gradlew ktlintCheck detekt        # ktlintFormat auto-fixes style
```

The full write-up (architecture, MVVM vs MVI, pagination, search, SOLID, production basics) is in the README on `main`.
