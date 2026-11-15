# Machine Coding Starter (Android): `feature/paging3`

Starts from `02-posts-list` and adds a separate screen built with Paging 3 and `RemoteMediator`. Open it with the **Paging 3** button in the top bar.

**What this feature shows:**
- **`PostRemoteMediator`** (network → Room):
  - `REFRESH` loads page 1 and clears the table plus the remote key in one transaction.
  - `APPEND` reads `nextPage` from the `remote_keys` table.
  - `PREPEND` is a no-op.
  - It catches `IOException`, `HttpException` and `SerializationException`, and returns them as `MediatorResult.Error`.
- **Room `PagingSource`:** the DAO returns `PagingSource<Int, PagedPostEntity>`. Room invalidates it whenever the table changes, so the UI only ever reads the database.
- **`Pager`** in `PagedPostRepositoryImpl` maps entities to the shared domain `Post`. The ViewModel is one line: `observePagedPosts().cachedIn(viewModelScope)`.
- **UI:** `collectAsLazyPagingItems()`, `itemKey` and `itemContentType`. States come from `loadState.refresh` (full-screen loading/error/empty, plus a snackbar when a refresh fails over cached rows) and `loadState.append` (spinner, retry or end at the bottom of the list).
- **Independent tables** (`paged_posts`, `remote_keys`), so the feature can be deleted without touching `feature/posts`. The two features share only `core/`.
- **Trade-off:** `PagingData` appears in the domain interface. That's accepted because `paging-common` is pure Kotlin.
- **No unit tests for the mediator:** testing it needs an in-memory Room database, which means an instrumented test. Compare with `feature/pagination-manual`, where everything is unit-tested.

```bash
git diff 02-posts-list..feature/paging3
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
