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
