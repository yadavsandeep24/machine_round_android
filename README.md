# Machine Coding Starter (Android): `feature/post-detail`

Starts from `02-posts-list` and adds a detail screen.

**What this feature shows:**
- **MVVM next to MVI:** `PostDetailViewModel` exposes a `StateFlow<UiState<Post>>` built with `stateIn(WhileSubscribed(5_000))`. Compare it with the list's MVI contract.
- **Navigation as an effect:** the list sends `PostClicked`, the ViewModel emits a one-off `NavigateToDetail` effect, and the route calls the `NavController`. The ViewModel never sees navigation APIs.
- **Type-safe argument:** `PostDetailDestination(val id: Int)` is read from `SavedStateHandle` by property name. That works in plain JVM tests, whereas `toRoute()` needs Robolectric.
- **Network fallback:** `observePost(id)` serves the Room row, or fetches `GET posts/{id}` with `transformLatest` when the post isn't cached (deep links, search results).
- **Tests:** a click → effect test, and 3 detail tests (cached, uncached, missing)

```bash
git diff 02-posts-list..feature/post-detail
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
