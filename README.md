# `nykaa/product-listing-filters-cart`

**Company:** Nykaa · **Role reported:** SDE-II Android (4 YOE, Gurugram) · **Reported:** 21 Aug 2026 · **Base branch:** `01-minimal`

## The problem (based on the report)
The August 2026 report describes **no separate machine coding round**. Instead:
- **Round 2** had the candidate design an MVVM screen on screen share, asking where the API call belongs in a ViewModel, how a ViewModel survives configuration changes, and `StateFlow` vs `SharedFlow`.
- **Round 3** was mobile system design covering a **cart, filters and real-time features**.

This branch implements that combination as a 90-minute exercise, so you can practise both building it and defending it:
- A product list with brand and price filters and sorting
- A cart shared across screens
- A live stock stream that marks items sold out and corrects the cart when stock drops

## Sources and verification
| Claim | Source | Status |
|---|---|---|
| 3 rounds: fundamentals + AI, deep Android + MVVM design on screen share, mobile system design (cart, filters, real-time) + HM | LeetCode Discuss, "Nykaa SDE-II Android Interview Experience and Compensation", posted 21 Aug 2026 (first-hand, anonymous) | First-hand, within the last 6 months |
| Nykaa's mobile team runs at large scale with sale-day traffic spikes | Dataford interview guide (third-party) | Secondary |
| Nykaa publishes its interview questions or Android architecture | Not found. No official Nykaa engineering post on this. | Not verifiable |

The exact screens are **my interpretation** of the reported design topics. They aren't a confirmed Nykaa machine coding prompt.

## What's implemented
- **Where the API is called:** in `init`, once per ViewModel, so rotation never re-fetches. This is the question asked in round 2.
- **One state from four sources:** `combine(products, filter, liveStock, cart)` produces one immutable `ProductListState`. Filtering is a pure `List<Product>.applyFilter()` function with its own tests.
- **StateFlow vs SharedFlow:** the cart is a `StateFlow`, because it's state with a current value that new screens need immediately. Snackbars are one-off effects sent through a `Channel`.
- **Real-time stock:** `FakeCatalogServer.stockStream()` simulates other shoppers buying. Each push updates the "Only N left" and "Sold out" labels, and `CartRepository.reconcile()` clamps the cart or drops lines that have sold out.
- **Cart rules:** you can never add more than the live stock, and an over-limit attempt shows a message. The cart screen (MVVM) reads the same singleton.
- **Tests (14):** 3 filter tests, 3 cart tests (stock cap, totals, reconcile), 5 ViewModel tests (load, filters, over-stock message, live stock reconcile, error and retry) and 3 `safeCall` tests. In the app the stock stream never ends, so tests use a finite stream (`maxTicks = 1`) and advance virtual time explicitly.

## System design follow-ups to prepare
- **Live stock in production:** a WebSocket or SSE connection with backpressure (`conflate()`), and reconnecting with backoff.
- **Cart:** persisting it locally (Room) and merging it with the server cart after login.
- **Filters:** moving filtering to the server when the catalogue is large (facets, counts), with filter state in `SavedStateHandle` for process death.
- **Sale-day spikes:** caching, request collapsing, and showing stale data with a banner.

```bash
git diff 01-minimal..nykaa/product-listing-filters-cart
```

## Follow-up: LLD (low-level design)
- **Classes:** `ProductListViewModel` (MVI, combines four flows), `CatalogRepository`, `CartRepository` (singleton `StateFlow<Cart>`), `ProductFilter` + `applyFilter()` (pure), `CartViewModel` (MVVM). Draw who owns which state.
- **Filters as data, not code:** `ProductFilter(brands, maxPrice, sort)` is an immutable value; sorting is a `when` over `SortOption`. To add a filter type, add a field and one predicate. Discuss when to move to a list of `FilterRule` objects (Specification pattern).
- **Cart invariants in one place:** `add()` never exceeds stock; `reconcile()` clamps quantities when stock drops. The rules live in the repository, not the UI, so every screen gets them.
- **Money:** `Int` rupees in the exercise; in production use minor units (paise) or `BigDecimal`, and never floating point.
- **Observer vs event:** stock is a stream (`Flow`), the cart is state (`StateFlow`), "can't add more" is a one-off effect (`Channel`).

## Follow-up: HLD (high-level design)
- **Services:** catalogue and search (faceted filters with counts), inventory (stock per SKU and warehouse), cart (server-side, per user), pricing and offers. The app talks to a backend-for-frontend that aggregates them.
- **Live stock:** inventory publishes changes to a stream (Kafka); a WebSocket/SSE gateway fans out only the SKUs a client is viewing. The client conflates updates and reconnects with backoff.
- **Cart consistency:** the server cart is the source of truth after login; the guest cart merges in on login. Stock is reserved at checkout, not at "add to cart", to avoid locking inventory.
- **Sale-day spikes:** CDN for catalogue reads, pre-warmed caches, request collapsing, a queue in front of checkout, and graceful degradation (hide live stock before failing checkout).
- **Filters at scale:** facets computed by the search engine, not the app; the app sends filter state and renders server-provided facet counts.

## Follow-up: System design (mock round)
**Prompt:** "Design the Nykaa shopping app for a flash sale: catalogue, filters, cart and checkout with live stock." About 45 minutes. This matches round 3 of the reported interview.
1. **Requirements:** browse and filter products, add to cart, live stock and price, checkout. Non-functional: survive 10x traffic at sale start, never oversell, cart consistent across devices.
2. **Client architecture:** catalogue, cart and checkout feature modules; MVI screens; cart as a shared `StateFlow` backed by Room and synced with the server cart; a WebSocket client for live updates of the SKUs on screen.
3. **API and data model:** BFF endpoints: `GET /plp?filters=&cursor=` (products + facet counts), `GET/PUT /cart`, `POST /checkout` (idempotency key); stock and price events per SKU.
4. **Data flow:** filters change → server query with facets; add to cart → optimistic local update → server confirms or corrects; stock event → UI label + cart reconcile; checkout reserves stock server-side.
5. **Scale and performance:** CDN for listing pages and images, pre-warmed caches before the sale, a queue (virtual waiting room) in front of checkout, graceful degradation of live stock.
6. **Trade-offs to state:** optimistic vs pessimistic cart updates, reserve stock at add-to-cart vs checkout, WebSocket vs polling, consistency vs availability during the spike.
