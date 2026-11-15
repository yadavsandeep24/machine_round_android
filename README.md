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

## Follow-up: LLD (low-level design)
- **The pipeline as composable operators:** `debounce` → `map(trim)` → `distinctUntilChanged` → `combine(retryTrigger)` → `flatMapLatest`. Be ready to explain each operator, and what breaks if you swap `flatMapLatest` for `flatMapMerge` (stale results overwrite fresh ones).
- **Two queries in state:** `query` (what's typed) vs `searchedQuery` (what the results belong to). The gap between them is the "pending" state. Why both are needed for correct empty and loading states.
- **Retry with `distinctUntilChanged`:** a retry of the same query would be filtered out, so a counter is combined in. Alternative: `MutableSharedFlow` trigger.
- **Process death:** the query lives in `SavedStateHandle.getStateFlow`, so the pipeline restarts itself on restore.
- **Testability:** virtual time (`advanceTimeBy`, `runCurrent`) proves the debounce and the cancellation without real delays.

## Follow-up: HLD (high-level design)
- **Typeahead backend:** a search index (Elasticsearch/OpenSearch or a trie-based suggestion service) separate from the primary database; the app calls `GET /search?q=&limit=`.
- **Load control:** client debounce plus a minimum query length; server-side rate limits per user and request coalescing for identical popular prefixes.
- **Caching:** popular prefixes cached at the CDN/edge with short TTLs; recent searches stored on the device.
- **Relevance and personalisation:** ranking signals (popularity, recency, the user's history) live server-side so they can change without an app release.
- **Offline:** search the local Room cache when offline, clearly labelled as "results from your saved items".

## Follow-up: System design (mock round)
**Prompt:** "Design search with typeahead for an app with 10M items." About 45 minutes.
1. **Requirements:** suggestions as the user types, full results on submit, recent searches, offline search of saved items. Non-functional: suggestions under 100 ms, results under 500 ms.
2. **Client architecture:** debounce → distinct → `flatMapLatest` pipeline in the ViewModel; separate state for suggestions and results; recent searches in DataStore.
3. **API and data model:** `GET /suggest?q=&limit=8` (strings only) and `GET /search?q=&cursor=` (results); the index lives in a search engine, not the primary database.
4. **Data flow:** keystrokes debounced on device; suggestion requests cancelled when stale; submit loads page 1, then paginates.
5. **Scale and performance:** edge cache for popular prefixes, rate limits per user, request coalescing, a minimum query length.
6. **Trade-offs to state:** client vs server ranking, freshness of the index, personalisation vs cacheability.

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
