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

## Follow-up: LLD (low-level design)
- **`Paginator` as a state machine:** states are idle, loading, error and end; transitions are `loadNext`, `retry` and `reset`. Draw it. Why it's a separate class (single responsibility, unit-tested alone, reused by browse and search).
- **Concurrency guard:** one `Job` at a time; `loadNext` while loading is a no-op. Why not a `Mutex`? Dropping duplicate scroll triggers is the intended behaviour, not queueing them.
- **Ownership split:** the ViewModel owns page 1 (first load, refresh); the `Paginator` owns pages 2..n. `reset(nextPage, endReached)` is the hand-off. What happens if refresh races a load-more? `reset()` cancels it first.
- **The trigger:** `snapshotFlow` on `LazyListState`, emitting `(nearEnd, totalItems)` with `distinctUntilChanged`. Why include `totalItems` (a short page that still fits on screen keeps loading).
- **Extensibility:** swap page numbers for cursors by making `loadPage` take an opaque key; `PageStatus` stays the same.

## Follow-up: HLD (high-level design)
- **Offset vs cursor pagination:** offset breaks when items are inserted or deleted (duplicates or gaps); cursor (`?after=<id>` or an opaque token) is stable and lets the database use an index. Recommend cursor for feeds.
- **Page size:** trade-off between round trips and payload; allow the client to ask for a larger first page.
- **End detection:** prefer an explicit `hasMore`/`nextCursor` from the server over "short page", which breaks when the server filters items after paging.
- **Caching pages:** Room as the source of truth with a remote-keys table (see `feature/paging3`) so scrolling works offline and survives process death.
- **Backend:** keyset queries (`WHERE id < :cursor ORDER BY id DESC LIMIT n`) instead of `OFFSET`, which gets slower the deeper the user scrolls.

## Follow-up: System design (mock round)
**Prompt:** "Design an infinitely scrolling product or photo feed with millions of items." About 45 minutes.
1. **Requirements:** smooth endless scroll, pull to refresh, resume where the user left, offline browsing of what was seen. Non-functional: 60 fps scrolling, under 300 ms per page on good networks.
2. **Client architecture:** a `Paginator`-style state machine (or Paging 3) in the ViewModel; Room cache with remote keys; image pipeline with prefetch for the next screen of items.
3. **API and data model:** keyset pagination `GET /items?after=<cursor>&limit=30` with `nextCursor` and `hasMore`; stable sort order.
4. **Data flow:** scroll trigger near the end → load next page → append to Room → list updates. Refresh replaces page 1 and resets keys.
5. **Scale and performance:** CDN and edge caching for the first pages, database keyset queries (not `OFFSET`), thumbnail sizes per device.
6. **Trade-offs to state:** memory vs disk cache size, placeholders vs a loading footer, prefetch distance vs wasted data.

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
