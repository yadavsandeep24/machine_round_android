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

## Follow-up: LLD (low-level design)
- **Navigation contract:** the list emits `NavigateToDetail(id)` as an effect; the route calls the `NavController`. Why the ViewModel never touches navigation: testability and no leaked `Context`.
- **Type-safe argument:** `PostDetailDestination(val id: Int)` read from `SavedStateHandle`. What if the id is missing or the post was deleted? `checkNotNull` vs a `UiState.Error`; discuss which is right for deep links.
- **`observePost` with `transformLatest`:** cached row first, network fallback when missing. Why `transformLatest` (a new emission cancels the in-flight fetch) and not `flatMapConcat`.
- **MVVM vs MVI here:** a read-only screen with no intents doesn't need the MVI contract; `StateFlow` + `stateIn(WhileSubscribed(5_000))` is enough. Explain the 5-second window (survives rotation, stops when the user leaves).
- **Shared element / prefetch:** pass the title from the list for instant first paint, then load the full body.

## Follow-up: HLD (high-level design)
- **Deep links and app links:** `https://app/posts/{id}` opens the detail screen directly, so the screen must fetch by id without the list. Verified app links via `assetlinks.json`.
- **API design:** `GET /posts/{id}` returns the full body; the list endpoint returns a summary to keep list payloads small (field selection or a separate summary DTO).
- **Caching:** detail responses cached with `Cache-Control` and `ETag`; the client keeps detail rows in Room so a re-open is instant offline.
- **Consistency:** the post was edited or deleted server-side while cached. Use `updatedAt`/versions, and handle 404/410 by removing the row and telling the user.
- **Analytics:** a view event on open, deduplicated per session, sent in batches.

## Follow-up: System design (mock round)
**Prompt:** "Design the article detail experience for a news app, including deep links and offline reading." About 45 minutes.
1. **Requirements:** open from the list, a notification or a web link; render rich content; save for offline; share. Non-functional: open in under 500 ms when cached.
2. **Client architecture:** detail ViewModel (MVVM) reading `observeArticle(id)`: cache first, then network. A content renderer for rich blocks (text, images, embeds).
3. **API and data model:** `GET /articles/{id}` returning structured blocks (not raw HTML) so the app renders natively; `updatedAt` for change detection.
4. **Data flow:** list prefetches summaries; detail fills the body; "save for offline" downloads the body and images with WorkManager.
5. **Scale and performance:** CDN-cached article JSON with `ETag`; image variants by screen density; prefetch the top N articles on Wi-Fi.
6. **Trade-offs to state:** server-driven blocks vs HTML in a WebView, how long saved articles are kept, handling edits after save.

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
