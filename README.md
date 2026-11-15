# Machine Coding Starter (Android): `02-posts-list`

The base every feature branch starts from: one simple, complete feature with clean architecture and MVI.

**Added on top of `01-minimal`:**
- `core/model/Post`, `core/network/api` (`PostApi`, `PostDto`), `core/database` (`AppDatabase`, `DatabaseModule`)
- `feature/posts`, in Clean Architecture layers:
  - **data:** `PostEntity`, `PostDao` (atomic `replaceAll`), mappers, and an offline-first `PostRepositoryImpl` (the UI observes Room; the network writes into Room)
  - **domain:** the `PostRepository` interface, `ObservePostsUseCase`, `RefreshPostsUseCase`
  - **ui (MVI):** `PostListContract` (State / Intent / Effect), `PostListViewModel`, a stateful `PostListRoute` plus a stateless `PostListScreen` with a preview
- **States:** full-screen loading, full-screen error with retry, empty, data, and pull-to-refresh. When offline with cached data, the cache stays on screen and a snackbar explains why.
- **Tests:** `FakePostRepository` and 5 ViewModel tests

**Practise from here:** branch off and build a detail screen, pagination, search, Paging 3 or auth. Then compare with the matching `feature/*` branch.

## Follow-up: LLD (low-level design)
Expect to sketch classes on a whiteboard and justify each boundary.
- **Draw the layers:** `PostListScreen` → `PostListViewModel` → `ObservePostsUseCase` / `RefreshPostsUseCase` → `PostRepository` (interface) → `PostRepositoryImpl` → `PostApi` + `PostDao`. Say which arrows are interfaces and why (the domain owns `PostRepository`; data implements it).
- **Why a use case for a one-liner?** It's the seam where business rules land later (sorting, filtering, combining sources) without touching the ViewModel. Be honest that it's optional for trivial reads.
- **State modelling:** one immutable `PostListState` instead of separate `isLoading`/`error` flows, so impossible combinations can't be emitted out of order. One-off messages go through a `Channel` effect, not state.
- **Mappers:** DTO → Entity → domain keep the network, database and UI free to change independently. Where do you validate bad server data (nulls, empty titles)? In the mapper.
- **Testing seams:** the repository interface plus an injected dispatcher let you test the ViewModel with a fake and virtual time. Show `FakePostRepository`.

## Follow-up: HLD (high-level design)
- **"Design a feed for millions of users":** the client stays offline-first (Room as the source of truth); the server exposes a cursor-paginated `GET /posts?cursor=` (not offset, because new posts shift pages) with `ETag`/`If-None-Match` so unchanged pages cost a 304.
- **Freshness:** pull-to-refresh plus a background `WorkManager` sync; push (FCM) only to say "new posts", never the payload, so the client fetches with auth and caching.
- **Caching layers:** HTTP cache for images and static content, a CDN in front of read APIs, Room on the device. Define how long cached posts are trusted and when to show a "stale" banner.
- **Failure modes:** API down → cached list plus a snackbar (already implemented); partial failure → keep the last good page; schema change → `ignoreUnknownKeys` and versioned endpoints.
- **Metrics:** time to first content, refresh failure rate, cache hit rate.

## Follow-up: System design (mock round)
**Prompt:** "Design a news feed app (like a company blog or LinkedIn-style feed) used by 5M people a day." Spend about 45 minutes.
1. **Requirements:** read the feed, pull to refresh, open a post, read offline. Non-functional: first content within 1 s on 4G, works offline, low battery and data use.
2. **Client architecture:** UI (Compose, MVI) → ViewModel → use cases → repository → Room (source of truth) + API. Background sync with WorkManager. Image loading with memory and disk cache.
3. **API and data model:** `GET /feed?cursor=&limit=20` returns post summaries plus `nextCursor`; `GET /posts/{id}` for the full body. Post table keyed by id with `updatedAt`.
4. **Data flow:** the screen observes Room; refresh writes into Room; FCM sends "new posts available" and the app fetches. Offline shows the cache with a "last updated" time.
5. **Scale and performance:** CDN for images and feed pages, `ETag` for 304s, prefetch the next page, cap the cache size.
6. **Trade-offs to state:** cursor vs offset, push payload vs push-to-sync, how stale a cached feed may be.

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
