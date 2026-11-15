# Machine Coding Starter (Android)

A starter project for Android machine coding rounds: **Kotlin, Jetpack Compose, Clean Architecture, MVVM and MVI, manual pagination and Paging 3, Hilt, Retrofit with kotlinx.serialization, Room (offline-first), Coroutines/Flow, type-safe Navigation, and unit tests**.

The sample feature fetches posts from `jsonplaceholder.typicode.com`, caches them in Room, and supports pagination, debounced server-side search (also paginated), pull-to-refresh, favorites, loading/error/empty states, and a detail screen.

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

**Company problems** (reported between May and Nov 2026, built from `01-minimal`). Sources and verification are in [docs/INTERVIEW_RESEARCH.md](docs/INTERVIEW_RESEARCH.md):

| Branch | Problem |
|---|---|
| `adobe/search-suggestions-infinite-scroll` | Type-ahead suggestions (debounce, `flatMapLatest`, LRU cache) and infinite-scroll results |
| `nykaa/product-listing-filters-cart` | Product list with filters and sort, a shared cart, and live stock that corrects the cart |
| `reddit/comment-thread-filter` | Dog/cat comment-subtree filter with the score follow-up, in a threaded collapsible list |

**Domain problems** (EdTech and ERP, built from `01-minimal`):

| Branch | Problem |
|---|---|
| `edtech/timed-quiz` | Quiz timer that survives process death (deadline in `SavedStateHandle`), auto-submit, review |
| `edtech/offline-course-downloads` | Lesson downloads with WorkManager: progress, cancel, retry/backoff, offline view |
| `edtech/attendance-offline-sync` | Offline attendance with an outbox: idempotent sync, protection for in-flight edits, conflicts |

```bash
git checkout 02-posts-list && git checkout -b my-attempt   # practise a feature yourself
git diff 02-posts-list..feature/search-debounce            # then compare with the reference
```

## Architecture

```
presentation (ui/)          domain/                     data/
┌──────────────────┐   ┌────────────────────────┐   ┌──────────────────────────┐
│ Screen (Compose) │   │ Post (pure model)      │   │ PostRepositoryImpl       │
│   ▲ state   │intent  │ PostRepository (iface) │◄──│  ├─ PostApi  (Retrofit)  │
│ ViewModel ───────────►│ UseCases               │   │  ├─ PostDao  (Room)      │
└──────────────────┘   └────────────────────────┘   │  └─ Mappers DTO→Entity→Domain
                                                     └──────────────────────────┘
```

- **Dependency rule:** `ui → domain ← data`. Domain has no Android, Retrofit or Room imports.
- **Offline-first:** the UI only ever *observes Room*. The network only *writes into Room*.
- **Three models:** `PostDto` (network), `PostEntity` (DB), `Post` (domain). Mappers are in `PostMappers.kt`.

## MVVM vs MVI (both are in the project)

| | **MVVM**: `PostDetailViewModel` | **MVI**: `PostListViewModel` |
|---|---|---|
| UI → VM | Calls public functions (`toggleFavorite()`) | Sends one `onIntent(PostListIntent)` |
| State | `StateFlow<UiState<Post>>` | One immutable `PostListState` data class |
| One-off events | n/a | `PostListEffect` sent through a `Channel` (navigation, snackbar) |
| Use it when | Simple screens | Screens with lots of interactions; easy to explain and test |

The MVI base class is `core/mvi/MviViewModel.kt` (about 40 lines). Each screen gets a `*Contract.kt` file that holds its State, Intent and Effect.

## Pagination (both implementations are in the project)

Open the **Paging 3** button in the top bar of the posts list to compare them side by side.

| | **Manual**: `feature/posts` (MVI) | **Paging 3**: `feature/pagedposts` (MVVM) |
|---|---|---|
| Trigger | `snapshotFlow` on `LazyListState` sends `LoadNextPage` when within 5 items of the end | Reading `items[index]` makes Paging prefetch |
| Page tracking | `nextPage` in the ViewModel | `remote_keys` table, read by `PostRemoteMediator` |
| Cache | Room `posts` table, observed as `Flow<List>` | Room `paged_posts` table through the `PagingSource` that Room generates |
| State | `isLoadingMore`, `loadMoreError` and `endReached` in `PostListState` | `LazyPagingItems.loadState.refresh` / `.append` |
| Guards | No concurrent loads (`browseJob` / `searchMoreJob`), no automatic retry after an error, refresh cancels load-more | Built in |
| Tests | 6 ViewModel unit tests with a fake repository | None. A mediator test needs an in-memory Room database, which means an instrumented test |
| Pick it when | The interviewer wants to see your own logic (the usual case) | The interviewer says "use Paging 3", or for production code |

The two features use separate tables, so you can delete either folder (and its entities in `AppDatabase`) without breaking the other.

**End of data:** both detect it when a page comes back with fewer items than `pageSize`. If your API returns `total`, `hasMore` or `nextCursor`, use that instead (`PostRepositoryImpl.loadPage` and `PostRemoteMediator.load`).

## Package layout

```
core/                    Shared by every feature. Each folder maps to a future Gradle module.
  common/    AppResult (pure Kotlin), @IoDispatcher           -> :core:common  (domain may depend on it)
  model/     Post (shared domain model)                       -> :core:model
  network/   NetworkModule, safeCall, api/ (PostApi, PostDto), auth/ -> :core:network
  database/  AppDatabase + DatabaseModule                     -> :core:database
  paging/    Paginator + PageStatus (load-more state machine)
  mvi/       MviViewModel base, UiIntent / MviState / UiEffect markers
  ui/        UiText, UiState, toUserMessage(), shared composables -> :core:ui
  di/        CommonModule (dispatchers)
feature/posts/           Depends only on core/
  data/      local/ (Entity, Dao), mappers, RepositoryImpl
  domain/    PostRepository, PageResult/PostPage, usecase/
  di/        PostModule (@Binds repo)
  ui/list/   MVI: Contract, ViewModel, Screen (Route + stateless Screen + Preview)
  ui/detail/ MVVM: ViewModel, Screen
feature/pagedposts/      Depends only on core/ (never on feature/posts)
  data/      local/ (Entity, RemoteKey, DAOs), PostRemoteMediator, RepositoryImpl (Pager)
  domain/    PagedPostRepository, ObservePagedPostsUseCase
  ui/        MVVM + Paging 3: ViewModel (cachedIn), Screen (LazyPagingItems)
navigation/  AppNavHost with @Serializable routes
```

## Search (server-side, in `PostListViewModel`)

```
keystroke -> state.query (instant, so the text field never lags)
          -> queryFlow
               .debounce { if (blank) 0 else 300ms }   // clearing is instant
               .map { trim }
               .distinctUntilChanged()                 // "kotlin" -> "kotli" -> "kotlin" sends no new request
          combine(searchRetry)                         // Retry / pull-to-refresh re-runs the same query
          .flatMapLatest { query -> search(page 1) }   // a new query CANCELS the old request
```

- **Why `flatMapLatest`:** without it, a slow response for "ko" can arrive after the response for "kotlin" and overwrite it. That's a race condition interviewers like to ask about. It also cancels any in-flight load-more for the old query.
- **Results are paginated** with the same scroll trigger and footer as browsing, and they keep their own page counter (`searchNextPage`).
- **Results aren't cached.** Search goes straight to the network (`PostRepository.searchPosts`). Favorite state still updates live because the ViewModel combines results with `observeFavoriteIds()`. Favoriting an uncached result inserts it into Room, so it stays pinned to the top of the browse list.
- **Detail screen:** opening a result that isn't cached fetches it from `GET posts/{id}` (`PostRepositoryImpl.observePost`).
- **UI:** old results stay visible with a refresh indicator while a new query loads. Browse and search keep separate scroll positions, and a new query scrolls to the top.
- **Tests (7):** debounce, cancellation of a stale request, a retyped query not searching again, clearing being instant, search pagination, error and retry, and favoriting a search result.
- **Local filtering instead?** Replace `searchPosts` with a filter over `observePosts()` inside `combine`, and keep the same debounce chain.

## SOLID: how the project applies it

| Principle | Where you can point in the code |
|---|---|
| **S**ingle responsibility | `PostApi` (network), `PostDao` (DB), mappers, one action per use case, `AuthInterceptor` vs `TokenAuthenticator`. `PostListViewModel` only coordinates the screen; "load more" policy lives in `Paginator`, which has its own tests. |
| **O**pen/closed | New screens extend `MviViewModel`. Auth plugs into OkHttp without touching repositories. A new feature is a new folder plus a Hilt module, with core untouched. (Sealed intents with `when` are a deliberate trade for exhaustiveness checks.) |
| **L**iskov substitution | `FakePostRepository` replaces `PostRepositoryImpl` and honors the same contract, including the network fallback in `observePost`. `InMemoryTokenStore` replaces `DataStoreTokenStore`. |
| **I**nterface segregation | ViewModels depend on narrow use cases, not the whole repository. `PagedPostRepository`, `AuthApi` and `TokenStore` are small, focused interfaces. |
| **D**ependency inversion | The domain owns `PostRepository`, the data layer implements it, and Hilt binds them. **The domain imports only pure Kotlin:** `AppResult`, `Post`, coroutines, `javax.inject`. `safeCall` lives in the data layer and `toUserMessage()` in the UI layer. Features never import each other. |

**Known, defensible trade-offs:**
- ViewModels depend on concrete use case classes rather than interfaces. They're still testable through a fake repository, and interfaces there would only add ceremony.
- `PagedPostRepository` exposes `PagingData` (`paging-common` is pure Kotlin).
- `AppDatabase` and `NetworkModule` are shared registration points, which is normal in a single module.

## Adapting it to the problem in the round (about 10 minutes)

1. Change `BASE_URL` in `app/build.gradle.kts`.
2. Copy `feature/posts` to `feature/<thing>` and rename. Update the DTO fields to match the JSON.
3. Register the new `Entity` in `AppDatabase`, add a DAO provider in `DatabaseModule`, and add an API provider in `NetworkModule`.
4. Bind the repository in a `<Thing>Module`.
5. Add routes in `AppNavHost`.
6. No API? Delete Retrofit and Room, and back the repository with an in-memory `MutableStateFlow`, as `FakePostRepository` in the tests does.

## Things interviewers look for (already handled)

- Loading, error with retry, empty, and success states, plus cached data shown on network failure
- `collectAsStateWithLifecycle`, keys in `LazyColumn`, and a stateless screen split from the stateful route
- Server-side search: debounce, `distinctUntilChanged` and `flatMapLatest` (see below)
- Exceptions caught in the repository with `CancellationException` rethrown, and user-friendly error messages
- Dispatchers injected so they can be tested
- Configuration changes are safe (ViewModel + StateFlow), and effects are not replayed on rotation (`Channel`)
- Tests: `FakePostRepository`, `MainDispatcherRule`, Turbine for effects, Truth assertions, MockWebServer for auth, standalone `Paginator` tests (32 tests)

## Production basics (already wired)

| What | Where | Notes |
|---|---|---|
| **Process death** | `PostListViewModel` uses `SavedStateHandle.getStateFlow("query")` | The search text and its results come back after the system kills the app. Tested. |
| **Auth header** | `core/network/auth/AuthInterceptor` | Adds `Bearer <token>` when a token exists. Mark public endpoints with `@Headers("No-Auth: true")`. |
| **Token refresh** | `core/network/auth/TokenAuthenticator` | Runs on a 401. `synchronized` means 5 parallel 401s cause **1** refresh, and threads that waited reuse the new token. Retries are capped. The refresh call uses a separate `@AuthClient` OkHttp client so it can't recurse. Only a 400/401/403 from the refresh endpoint logs the user out; a 5xx or network error doesn't. 5 MockWebServer tests. |
| **Token storage** | `DataStoreTokenStore` (Preferences DataStore) | `isLoggedIn: Flow<Boolean>`: observe it at the app root to send the user to login. Production: encrypt the values (e.g. Tink). `AuthApi` is a placeholder for your refresh endpoint. |
| **Logging** | Timber, planted in `StarterApp` (debug only) | OkHttp logs go through Timber with `Authorization` redacted, and `safeCall` logs every failure. In release, plant a tree that forwards to Crashlytics or Sentry. |
| **Strings** | `res/values/strings.xml`, plus `core/common/UiText` | ViewModels emit `UiText` (resource ID or dynamic text) and never need a `Context`. The UI resolves it with `asString()`. |
| **Lint** | ktlint (`.editorconfig`) + detekt (`config/detekt/detekt.yml`, tuned for Compose) | Both pass with zero issues. Two deliberate suppressions, each with a comment: the catch-all in `safeCall` and the spread operator in `UiText`. |

## Run

Open the folder in Android Studio (JDK 17), or:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew ktlintCheck detekt        # ktlintFormat auto-fixes style
```
