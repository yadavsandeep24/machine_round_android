# `docusign/organization-viewer`

**Company:** DocuSign · **Source:** DocuSign's own public repo [`docusign/Android-Candidate-Task`](https://github.com/docusign/Android-Candidate-Task) (published Aug 2020) · **Base branch:** `01-minimal`

This is the strongest evidence in this repo of what DocuSign asks Android candidates to build, because it's **DocuSign's own published task**, not a candidate's account of it. It's from 2020, so the current task may differ. The API it names still works (checked Nov 2026).

## The task (DocuSign's requirements, quoted from their README)
1. Users should be able to browse a list of Non-profit Organizations
2. Unit and functional tests should be written where appropriate
3. Code should be well architected and modular
4. Check the API Spec for necessary endpoint information
5. Feel free to use open source libraries as you see fit

API: `https://projects.propublica.org/nonprofits/api/v2/search.json`

## API quirks found against the live API (and handled)
| Behaviour | Handling |
|---|---|
| `page` is **0-based**, 25 results per page, `num_pages` in the response | `Paginator` starts at page 0 for the first load, then 1..n |
| A search with **no matches returns HTTP 404**, not an empty list | Mapped to an empty, finished page; the UI says "No organizations match …", not an error |
| A page **past the end returns HTTP 400** "Pagination out of range" | Mapped to end of list |
| Results are capped (`total_results` tops out at 10,000) | End of list = last page **or** an empty page |
| Extra and unexpected fields, nulls, empty strings | `ignoreUnknownKeys`, nullable DTO fields, blank-safe location formatting |

## "Modular": real Gradle modules
```
:app                     Thin shell: Application, MainActivity, theme, NavHost that only calls feature graphs
:core                    Shared infra: AppResult, safeCall, MviViewModel, UiText/UiState, Paginator,
                         common composables, OkHttp + Json + Retrofit.Builder (no base URL)
:feature-organizations   data (Retrofit DTOs, repository) · domain (models, repository interface)
                         · ui (MVI list, MVVM detail) · navigation (its own graph + routes) · di
```
- **Dependency direction:** features depend on `:core` only, never on each other, and `:app` depends on everything. A second feature would be a sibling module.
- **Each feature owns its API:** it builds its own Retrofit from `:core`'s builder with its own base URL, so features don't share or know about each other's endpoints.
- **Each feature owns its navigation:** `NavGraphBuilder.organizationsGraph()` lives in the feature, and `:app` doesn't know its screens or routes.
- **`api` vs `implementation`:** `:core` exposes only the types that appear in its public signatures (Retrofit, Compose, ViewModel, coroutines) as `api`.
- **Something the module split surfaced:** Kotlin can't smart-cast a property declared in another module (`PageStatus.loadMoreError` lives in `:core`), so the footer copies it into a local `val` first. Expect to be asked about this.

## What's implemented
- **List (MVI):** browsing and search share one pipeline: query (in `SavedStateHandle`) → debounce (instant when the field is cleared) → `distinctUntilChanged` → `flatMapLatest`, so a new query cancels the old request.
  - **Infinite scroll:** a `snapshotFlow` scroll trigger plus the reusable `Paginator` (one request at a time, stops at the end, no automatic retry after an error).
  - **States:** full-screen loading and error, empty, a thin progress bar while a new query loads over old results, a result count, footer states, and the query restored after process death.
- **Detail (MVVM):** the EIN is read by property name from `SavedStateHandle`, and `UiState` drives loading, error with retry, and data. It shows the IRS filings table (revenue, expenses, assets by year) with compact formatting: `$6.5B`, an em dash when a value wasn't reported, and EIN as `94-1340523`.

## Tests (32)
| Layer | Tests | How it runs |
|---|---|---|
| Functional, data layer | 7: page parsing with unknown fields, the exact query parameters sent (trimmed `q`, 0-based `page`, `q` omitted when browsing), last page, **404 → empty**, **400 → end**, 5xx and malformed JSON → failure, detail parsing and sorting | **MockWebServer** + real Retrofit and kotlinx.serialization, JVM |
| Unit, list ViewModel | 8: first page, paging to the end, debounced search restarting paging, empty result, error and retry, load-more error with explicit retry, navigation effect, process death | JVM, virtual time |
| Unit, detail and formatting | 4 | JVM |
| `:core` | 8: `Paginator` and `safeCall` | JVM |
| **Functional UI** | 5 Compose tests on the stateless screen: rows and count, a tap sends the EIN, typing sends the query, empty-state text, footer retry | **Device or emulator**, passed on an API 36 emulator |

```bash
./gradlew testDebugUnitTest                                   # 27 JVM tests
./gradlew :feature-organizations:connectedDebugAndroidTest    # 5 UI tests (emulator running)
```

## Follow-ups to prepare
- **Offline-first:** cache the browse pages in Room with a remote-keys table (see `feature/paging3`), and show cached results when offline.
- **Paging 3 instead of `Paginator`:** when you'd choose it, and why the hand-written version is easier to explain and test in an interview.
- **Filters:** filter by state or NTEE category (the API supports `state[id]` and `ntee[id]`), with the filter state in `SavedStateHandle`.
- **Scaling the modules:** split `:core` into `:core:network`, `:core:ui` and `:core:model`, use convention plugins for the repeated build config, and a `:feature-organizations:api` module if other features need to open the detail screen.

## Follow-up: LLD (low-level design)
- **Module and class map:** `:app` (shell, NavHost) → `:feature-organizations` (`NonprofitApi`, `OrganizationRepositoryImpl`, `OrganizationRepository`, list ViewModel, detail ViewModel, `organizationsGraph`) → `:core` (`Paginator`, `MviViewModel`, `safeCall`, `UiText`). Explain why features never depend on each other.
- **Error mapping as a policy:** the repository turns HTTP 404 (no matches) and 400 (past the end) into successful empty pages and everything else into `Failure`. Discuss putting this in a `ResponseClassifier` if more endpoints share the quirks.
- **One pipeline for browse and search:** a blank query means browse. Why that removes a whole code path and its bugs.
- **Formatting is pure and tested:** `formatUsdCompact`, `formatEin`. Where formatting belongs (UI layer, not the domain).
- **Adding a feature module:** what goes in `:feature-x:api` (routes, interfaces) vs `:feature-x:impl`, and how navigation between features works without a dependency cycle.

## Follow-up: HLD (high-level design)
- **If you owned the backend:** a search service over the IRS data (nightly ingest of the EO BMF extract and filings), indexed for name, location and NTEE code; cursor pagination instead of page numbers capped at 10,000 results.
- **Caching:** responses are effectively static per day, so cache at the CDN with long TTLs and `ETag`; cache list pages and details in Room for offline browsing.
- **API contract:** return `[]` with 200 for "no matches" and a `nextCursor` for paging, instead of 404 and 400. Say how you'd version that change without breaking old clients.
- **Detail payload:** the filings endpoint is large; add field selection or a summary endpoint for mobile.
- **Rate limits and resilience:** respect the public API's limits with client-side debounce, retries with backoff and jitter, and a circuit breaker that shows cached data when the API is down.

## Follow-up: System design (mock round)
**Prompt:** "Design a charity discovery app on top of public IRS non-profit data: search, browse by category and location, and detailed financials." About 45 minutes.
1. **Requirements:** search by name, filter by state and category, browse, view financial history, save favourites offline. Non-functional: fast search, works on poor networks, data refreshed daily.
2. **Client architecture:** feature modules (search, detail, favourites) on a shared core; MVI list with debounced search and pagination; Room cache for viewed organisations and favourites.
3. **API and data model:** your own backend ingests the IRS extracts nightly into a search index; `GET /orgs?q=&state=&ntee=&cursor=` and `GET /orgs/{ein}?fields=summary,filings`.
4. **Data flow:** search → page 1 → paginate; detail loads summary first then filings; favourites stored locally and synced when signed in.
5. **Scale and performance:** data changes daily, so cache aggressively at the CDN; small summary payloads for lists; filings fetched lazily.
6. **Trade-offs to state:** wrapping the public API vs your own index, data freshness vs cost, fixing the API quirks (404 for no results) in your own contract.
