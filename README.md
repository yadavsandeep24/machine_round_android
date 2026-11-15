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
