# Machine Coding Starter (Android): `feature/search-debounce`

Starts from `02-posts-list` and adds debounced server-side search.

**What this feature shows:**
```
keystroke -> state.query (instant, so the text field never lags)
          -> SavedStateHandle["query"]                 // survives process death
               .debounce { if (blank) 0 else 300ms }   // clearing is instant
               .map { trim }
               .distinctUntilChanged()                 // kotlin -> kotli -> kotlin sends no new request
          combine(searchRetry)                         // Retry / pull-to-refresh re-runs the same query
          .flatMapLatest { query -> search(query) }    // a new query CANCELS the old request
```
- **Why `flatMapLatest`:** without it, a slow response for "ko" can arrive after the response for "kotlin" and overwrite it. That's a race condition interviewers like to ask about.
- **`searchedQuery` vs `query`:** the results belong to `searchedQuery`. The gap between the two is the "pending" state, during which old results stay visible with a refresh indicator.
- **UI:** a clear button, separate scroll positions for browse and search, and a new query scrolls to the top.
- **Not cached:** search results go straight to the network, and browsing still reads Room.
- **Tests (6):** debounce, cancelling a stale request, a retyped query not searching again, clearing being instant, error and retry, and process death. They use `runCurrent` and `advanceTimeBy` on virtual time.

```bash
git diff 02-posts-list..feature/search-debounce
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
