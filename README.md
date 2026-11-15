# Machine Coding Starter (Android): `01-minimal`

Core infrastructure with no features. Start a round here when the interviewer lets you set up your own architecture.

**Added on top of `00-empty`:**
- `core/common`: `AppResult` (pure Kotlin, safe for the domain layer) and `@IoDispatcher`
- `core/network`: `NetworkModule` (OkHttp with Timber logging, Retrofit, kotlinx.serialization `Json`) and `safeCall`
- `core/mvi`: `MviViewModel` base (State + Intent + one-off Effects through a `Channel`)
- `core/ui`: `UiText` (strings without a `Context`), `UiState`, `toUserMessage()`, loader, error and list-footer composables
- `navigation/`: `NavHost` with type-safe `@Serializable` routes
- Tests: `MainDispatcherRule` and `SafeCallTest`

**Not here yet:** Room. `@Database` needs at least one entity, so `AppDatabase` arrives with the first feature in `02-posts-list`.

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
