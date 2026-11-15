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
