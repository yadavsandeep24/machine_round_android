# Machine Coding Starter (Android): `feature/pagination-manual`

Starts from `02-posts-list` and adds hand-written load-more pagination. This is what most interviewers want to see, rather than Paging 3.

**What this feature shows:**
- **`core/paging/Paginator`:** a reusable state machine with its own tests. It allows one request at a time, never requests past the end, never retries automatically after an error, and `reset()` cancels a load that's still running.
- **Ownership:** the ViewModel loads page 1 (first load, pull-to-refresh) and then tells the `Paginator` where to continue. A refresh cancels any load-more in flight.
- **Scroll trigger:** `snapshotFlow` over `LazyListState` sends `LoadNextPage` when the user is within 5 items of the end. Because it re-emits when the item count changes, a short page keeps loading until the screen is full.
- **Footer states:** a spinner while loading, an error with Retry, and "You've reached the end".
- **End detection:** a page with fewer items than `pageSize`. Use `total`, `hasMore` or `nextCursor` instead if your API returns one.
- **Offline:** if refresh fails with cached rows, paging continues after what's cached.
- **Tests:** 5 `Paginator` tests and 5 new ViewModel pagination tests

```bash
git diff 02-posts-list..feature/pagination-manual
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
