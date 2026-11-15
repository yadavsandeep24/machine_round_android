# Android machine coding: company research (window: 15 May – 15 Nov 2026)

This note explains where the company-prefixed branches come from. Only reports from **the last 6 months** were used to build branches. Older reports are listed further down for context only.

## How sources were checked
1. **Candidate reports:** LeetCode Discuss, Medium, aggregators (FastPrep), Glassdoor, Taro. I recorded each report's date, role and what was asked.
2. **Company content:** for each company, I looked for official material confirming the round format or the Android stack: careers pages, job listings, engineering blogs, or Google's Android Developers Blog.
3. **No company publishes its machine coding questions.** So "verified" below means the round format or stack is consistent with company or official content. It never means the exact question is confirmed.

## Reports inside the window, which became branches

| Branch | Company / role | Date | What was asked (paraphrased) | Source | Official cross-check |
|---|---|---|---|---|---|
| `adobe/search-suggestions-infinite-scroll` | Adobe, MTS-2 Android | Aug 2026 | LLD round: a search bar with suggestions, and an infinite-scroll list. Earlier rounds covered thread-safe singletons, `AndroidViewModel` vs `ViewModel`, `Deferred` start, and Dagger/Hilt. | [FastPrep: Adobe experienced-hire collection](https://www.fastprep.io/experience/nov-2024-nov-2026-adobe-experienced-hire-first-technical-round-69-experiences-pa-36xptsr5o9) (aggregator) | Adobe hires native Android/Kotlin engineers (job listings). The question itself can't be verified. |
| `nykaa/product-listing-filters-cart` | Nykaa, SDE-II Android | 21 Aug 2026 | No separate machine coding round. MVVM screen design on screen share (where to call the API, ViewModel survival, StateFlow vs SharedFlow), then mobile system design for cart, filters and real-time features. | [LeetCode Discuss](https://leetcode.com/discuss/post/8473741/nykaa-sde-ii-android-interview-experienc-v8yv/) (first-hand) | No official Nykaa engineering content found. The branch is **my implementation** of the reported design topics. |
| `reddit/comment-thread-filter` | Reddit, Android track | Q4 2026 | Filter dog/cat-labelled comments, hiding each excluded comment with all its replies (BFS/DFS). Follow-up: keep comments above a score threshold. Plus a conceptual question on coroutines and `suspend`. | [FastPrep: 2026 Reddit SWE and Android](https://www.fastprep.io/experience/2026-reddit-experienced-swe-and-android-phone-screens-and-virtual-onsite-3-exper-32eqc7hd4x) (aggregator) | [Android Developers Blog, Dec 2024](https://android-developers.googleblog.com/2024/12/reddit-improved-app-startup-speed-using-baseline-profiles-r8.html): Reddit's app uses Compose and treats comments as a critical journey. |

Also inside the window, but **not enough detail for a branch**:
- **PhonePe Mobile Engineer, Jul 2026 ([Medium](https://medium.com/@singhmanjeetn/phonepe-mobile-engineer-interview-experience-2026-d9accfa32611)):** the rounds were DSA, React Native, machine coding, mobile system design and HM. The task itself is behind a paywall and the role is React Native.
- **Adobe Computer Scientist 1, May 2026:** a shape-packing algorithm question. It's not an Android build.

## Reports older than 6 months (context only, no branches)
These match the window's themes: build an API-driven list/detail app in 45–120 minutes, then defend the architecture.
- **Swiggy:**
  - Weather app with city search and a detail screen (Compose, MVVM/MVI, Hilt, tests), Sep 2024: [Medium](https://medium.com/@javainiyan/cracking-the-swiggy-machine-coding-round-dbc7aacc0dc1)
  - API data in a list with sorting, in 2 hours, Sep 2024: [Medium](https://medium.com/@himv1998/swiggy-sde-2-inteview-experience-android-7906956a924a)
  - Movie listing app, 2022: Taro
- **Flipkart SDE-3:** a 2-hour app that combines two APIs, then follow-ups on rotation and DI, 2021: [Medium](https://kanchan-pal.medium.com/flipkart-interview-android-sde-3-6f649c2292de)
- **PhonePe:**
  - Small e-commerce app with data synced across screens, in 90 minutes, Jul 2025: lets-code.co.in
  - App built from a given API contract, Mar 2025: [Medium](https://codeint.medium.com/phonepe-android-engineer-interview-experience-49b15ff37b05)
  - PhonePe's own blog confirms Compose with unidirectional data flow (2024): [tech.phonepe.com](https://tech.phonepe.com/building-the-pincode-android-app-on-jetpack-compose-a-journey/)
- **Meesho:** concurrent bitmap rendering by implementing 3 functions in an existing codebase, Apr 2025: [Medium](https://medium.com/@kumargaurav.xqf/my-android-developer-interview-experiences-at-zomato-rapido-and-meesho-1ffd93a8f524)

## Domain branches (`edtech/*`)
These aren't from company reports. I designed them around your own projects (Quest with ExoPlayer offline video, Learner Finder and academy, the ERP SDK):
- `edtech/timed-quiz`: a timer that survives process death, built on a deadline in `SavedStateHandle`.
- `edtech/offline-course-downloads`: WorkManager downloads with retry and cleanup, plus an offline-first outline.
- `edtech/attendance-offline-sync`: an outbox with idempotent sync, protection for in-flight edits, and conflict resolution.

## Caveats
- **Anonymous reports:** they're self-reported, and processes vary by team, level and month.
- **Aggregators:** pages like FastPrep re-publish candidate reports. I couldn't reach the original posts behind them.
- **Paywalls:** the paywalled parts of Medium posts weren't read.
