# `adobe/search-suggestions-infinite-scroll`

**Company:** Adobe · **Role reported:** MTS-2 (Android) · **Reported:** August 2026 · **Base branch:** `01-minimal`

## The problem (paraphrased from the report)
In the low-level design round, the candidate was asked to design two things: a **search bar that shows suggestions while you type**, and an **infinite-scroll results list**. Earlier rounds covered thread-safe singletons, `AndroidViewModel` vs `ViewModel`, when a `Deferred` starts running, and Dagger/Hilt.

This branch turns that design prompt into a working app, so you can practise building it in about 90 minutes and then defend the design.

## Sources and verification
| Claim | Source | Status |
|---|---|---|
| Round 3 covered LLD for a search bar with suggestions and an infinite-scroll list (MTS-2 Android, Aug 2026) | Aggregated candidate report on FastPrep, "Adobe Experienced-Hire First Technical Round" (Nov 2024 – Nov 2026 collection) | Second-hand: an aggregator page, not the original post |
| Adobe hires native Android engineers (Kotlin), e.g. SDE 3 Android in Noida | Adobe job listings (Anita B. jobs board, accel.com Frame.io posting) | Consistent with official listings |
| Adobe publishes its machine coding questions | Not found. No official Adobe page describes this round. | Not verifiable |

Treat the problem as **reported, not confirmed by Adobe**.

## What's implemented
- **Suggestions pipeline:** keystrokes → `debounce(200ms)` → trim → `distinctUntilChanged` → `flatMapLatest`, so stale requests are cancelled. Suggestions need at least 2 characters and are best-effort: on failure, none are shown.
- **Suggestion cache:** a hand-written `LruCache` (`LinkedHashMap` in access order). Retyping a prefix costs no network call, and failures are never cached.
- **Recent searches:** shown when the field is empty. Newest first, de-duplicated ignoring case, capped at 5.
- **Submit:** happens from the keyboard's search action or by tapping a suggestion. It hides the keyboard through a one-off effect, cancels any stale page load, and loads page 1.
- **Infinite scroll:** uses the reusable `Paginator` (one request at a time, stops at the end, no automatic retry after an error) with a `snapshotFlow` trigger and footer states.
- **Backend:** `FakeAssetServer` is an in-app stand-in with deterministic data, latency and failure injection. Replace it with a Retrofit API without touching the ViewModel.
- **Tests (19):** 6 ViewModel tests (debounce, cancellation, submit, infinite scroll, stale load-more, error and retry), 4 repository tests, 1 LRU test, 5 `Paginator` tests and 3 `safeCall` tests.

## Talking points for the follow-up discussion
- **Debounce vs throttle:** why debounce fits type-ahead.
- **Cache key normalisation** (trim + lowercase), cache size, and eviction policy.
- **Why suggestions and results are separate pipelines.**
- **Where pagination state lives:** `Paginator`, not the UI.
- **Production:** server-side ranking, request coalescing, and offline recent searches persisted with DataStore.

```bash
git diff 01-minimal..adobe/search-suggestions-infinite-scroll
```

## Follow-up: LLD (low-level design)
The reported round was LLD, so expect to draw this first.
- **Classes:** `SearchViewModel` (coordinates), `SearchRepository` (interface), `SuggestionCache` (`LruCache`), `Paginator` (results), `FakeAssetServer` (stand-in for the API). Name each one's single responsibility.
- **Extension point:** a `SuggestionProvider` interface (recent searches, popular terms, server suggestions) merged by a ranker. Strategy pattern: add a provider without touching the ViewModel.
- **Cache design:** key = normalised prefix; capacity; LRU eviction via `LinkedHashMap(accessOrder = true)`; never cache failures. Follow-up: prefix reuse (filter cached "mou" results for "moun" locally instead of a new request).
- **Concurrency:** `flatMapLatest` cancels stale suggestion requests; `Paginator` allows one page request at a time; a new submit resets the paginator before loading page 1.
- **State:** suggestions and results are separate pipelines with separate state, so typing never clears the results the user is reading.

## Follow-up: HLD (high-level design)
- **Typeahead service:** a prefix index (trie or an edge-n-gram search index) built offline from query logs plus asset tags; served by a stateless suggestion service with an in-memory top-K per prefix.
- **Latency budget:** under ~100 ms per keystroke, so cache hot prefixes at the edge/CDN (short TTL), keep payloads tiny (strings only), and debounce on the client.
- **Search results:** a separate search service with cursor pagination and ranking (relevance, popularity, licence type); image thumbnails from a CDN with size variants.
- **Personalisation:** per-user recent searches synced server-side; ranking can mix global and personal signals.
- **Quality loop:** log impressions and clicks per suggestion to retrain ranking; A/B test ranking changes without an app release.

## Follow-up: System design (mock round)
**Prompt:** "Design Adobe Stock's mobile search: typeahead plus an infinite grid of image results." About 45 minutes.
1. **Requirements:** suggestions while typing, results grid with filters (orientation, colour, licence), infinite scroll, recent searches, preview and licence. Non-functional: suggestions under 100 ms, smooth grid scrolling, low data use.
2. **Client architecture:** search module with two pipelines (suggestions, results); `Paginator` for the grid; image loader with size-aware thumbnails and memory/disk caches; filters in `SavedStateHandle`.
3. **API and data model:** `GET /suggest?q=` (cached prefixes), `GET /search?q=&filters=&cursor=` returning asset ids, thumbnail URLs per size, and licence info.
4. **Data flow:** debounce → suggest; submit → page 1 → paginate; tapping an asset opens a preview with a higher-resolution rendition.
5. **Scale and performance:** CDN for thumbnails (WebP/AVIF), edge caching for hot prefixes, prefetch the next page and next screen of thumbnails, bounded caches.
6. **Trade-offs to state:** thumbnail quality vs data, prefetch depth, client vs server filtering, personalised vs cacheable results.
