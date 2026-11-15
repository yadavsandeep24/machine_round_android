# `reddit/comment-thread-filter`

**Company:** Reddit · **Track reported:** Android, full-time (experienced) · **Reported:** Q4 2026 (posted Nov 2026) · **Base branch:** `01-minimal`

## The problem (paraphrased from the report)
Each comment has an id, an optional parent and a **dog or cat** label.
- **Task:** for a *dog person*, hide every cat comment **and all of its replies**; for a *cat person*, the reverse. Return the ids to hide. The candidate's approach was to build a graph from the parent links and traverse it with BFS or DFS.
- **Follow-up:** comments whose score is above a threshold should still be shown. The candidate ran out of time on this part.
- **Also asked:** a conceptual question about coroutines and `suspend`.

The reported question was algorithmic, not a UI build. This branch solves it as a pure function, then wraps it in the Android screen you'd expect at Reddit: a threaded, collapsible comment list.

## Sources and verification
| Claim | Source | Status |
|---|---|---|
| Comment-filtering question with dog/cat labels, a subtree exclusion rule and a score-threshold follow-up (Q4 2026) | Aggregated candidate report on FastPrep, "2026 Reddit Experienced SWE and Android Phone Screens and Virtual Onsite" | Second-hand: an aggregator page, posted Nov 2026 |
| Reddit's Android app uses Jetpack Compose and treats post and comment screens as critical user journeys | Android Developers Blog (Google), "Reddit improved app startup speed using Baseline Profiles and R8", Dec 2024 | Official (Google, with Reddit) |
| Reddit publishes its interview questions | Not found | Not verifiable |

## What's implemented
- **`CommentFilter.hiddenIds()`:** a children map plus BFS, O(n). A hidden comment hides its whole subtree.
  - **Follow-up rule:** a comment scoring above the threshold is rescued, but a rescued comment under a hidden ancestor stays hidden. This rule is written down, and the tests pin it.
  - **Malformed data:** a visited set means a cycle can't loop forever.
- **`ThreadFlattener.flatten()`:** turns the tree into rows for a `LazyColumn`. It walks the tree in pre-order with siblings sorted by score, and records depth and reply count for each row.
  - It uses an iterative DFS, so a 20,000-deep reply chain doesn't overflow the stack (tested).
  - Collapsed comments keep their row but skip their replies, and replies to a deleted parent show as top-level.
- **ViewModel:** `combine(comments, preference, threshold, collapsed)` → filter → flatten, run with `flowOn(@DefaultDispatcher)`, because tree work is CPU-bound and shouldn't run on Main.
- **UI:** chips for the preference (Dog person, Cat person, Show all) and the score rule (off, >10, >50), indented threads with guide lines, tap to collapse or expand, and an "N comments hidden" count.
- **Tests (17):** 5 filter tests, 4 flattener tests, 5 ViewModel tests and 3 `safeCall` tests.

## Talking points
- **BFS vs DFS** for the subtree, and why recursion is risky on deep threads.
- **Clarifying the follow-up:** does a rescued comment rescue its replies, and does a hidden ancestor override a rescue? Ask the interviewer before coding.
- **Large threads at Reddit scale:** load collapsed "more replies" stubs from the server and page them, and diff rows with stable keys.

```bash
git diff 01-minimal..reddit/comment-thread-filter
```

## Follow-up: LLD (low-level design)
- **Classes:** `CommentFilter` (pure, BFS), `ThreadFlattener` (pure, iterative DFS), `CommentThreadViewModel` (combines comments, preference, threshold, collapsed set), `CommentRepository`.
- **Pluggable rules:** generalise "hide cat comments" into a `CommentRule` interface (`shouldHide(comment): Boolean`, plus whether it hides the subtree). Preference and score threshold become two rules composed with AND/OR. Strategy plus Composite.
- **Data structures:** children map built once (O(n)), BFS with a visited set (handles cycles), reply counts computed bottom-up in reverse pre-order (no recursion).
- **Threading:** filtering and flattening run on `Dispatchers.Default` via `flowOn`, never on Main; results are immutable lists, so Compose diffs by stable `key`.
- **Incremental updates:** when one comment is added, update the map and recount only its ancestors instead of reflattening the whole tree.

## Follow-up: HLD (high-level design)
- **Comment storage:** comments keyed by post with a parent pointer and a materialised path (or closure table) so a subtree is one indexed query.
- **Loading huge threads:** the server returns the top N comments per level by score plus "load more replies" stubs with cursors; the client pages each branch on demand.
- **Where filtering runs:** user preferences ("hide cat comments") applied server-side for consistency across devices, or client-side for instant toggles. Discuss the trade-off and caching per preference.
- **Real-time:** new comments and votes arrive over a WebSocket for the open thread only; scores update in batches to avoid list churn.
- **Ranking:** best/top/new sorts computed server-side (e.g. confidence-based "best"), cached per thread and invalidated on votes.

## Follow-up: System design (mock round)
**Prompt:** "Design the Reddit post and comments screen for threads with 50,000+ comments." About 45 minutes.
1. **Requirements:** threaded comments, collapse/expand, sort (best, top, new), vote, reply, live new comments, per-user content filters. Non-functional: first comments within 1 s, smooth scrolling, low memory.
2. **Client architecture:** comments ViewModel combining the thread, sort, filters and collapsed set; tree logic (filter, flatten) on a background dispatcher; `LazyColumn` with stable keys; optimistic votes and replies.
3. **API and data model:** `GET /posts/{id}/comments?sort=&depth=&limit=` returns a partial tree plus "more" stubs with cursors; `GET /comments/{id}/children?cursor=` expands a branch; comments carry parent ids and scores.
4. **Data flow:** load the top of the tree → expand on demand → WebSocket pushes new comments and score changes for the open thread, batched into the list.
5. **Scale and performance:** precomputed sort orders cached per thread, depth and breadth limits per request, incremental tree updates instead of reflattening.
6. **Trade-offs to state:** server vs client filtering, how much tree to send up front, real-time freshness vs list churn.
