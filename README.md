# `edtech/offline-course-downloads`

**Domain problem** (not from a company report), designed around your Quest app (ExoPlayer, offline video). **Base branch:** `01-minimal` · **Suggested time:** 2 hours (senior)

## Problem statement (as an interviewer would give it)
> A course has N video lessons. Learners must be able to study on a train with no network.
> - Show the course outline. It must be available offline after the first load.
> - **Download** any lesson. Show Queued and Downloading (with progress) states, then Downloaded. Allow **cancel** and **delete**.
> - Downloads must continue if the user leaves the screen or the app is killed, wait for network, and **retry** transient failures with backoff. After 3 failures, show the error with a retry action.
> - Tapping Download twice must not start two downloads, and a cancelled or failed download must not leave a half-written file.
> - Add a "Downloaded only" filter, show storage used, and when the device goes offline, automatically show only downloaded lessons with an offline banner.

## What's implemented
- **Architecture:** the ViewModel only observes Room (course outline plus download state) and the network. WorkManager, files and transfers sit behind interfaces:
  - `DownloadStore`: a Room table
  - `DownloadScheduler`: WorkManager
  - `LessonFileStore`: `filesDir`
  - `LessonDownloader`: the transfer itself
- **WorkManager:**
  - Each lesson gets one unique work item, enqueued with `KEEP` (double taps are idempotent).
  - Constraints: `NetworkType.CONNECTED` and storage not low. Retries use `BackoffPolicy.EXPONENTIAL`.
  - The worker is a `@HiltWorker`. `StarterApp` supplies `HiltWorkerFactory`, and the manifest removes the default initializer.
- **`LessonDownloadProcessor`:** the worker's logic as a plain class, so it can be unit-tested without WorkManager.
  - Progress is throttled to one DB write per percent.
  - The partial file is deleted on cancel or error.
  - Before the last attempt it returns `RETRY` and goes back to Queued. On the final failure it returns `FAILED` with the reason.
- **Fake transfer:** `FakeLessonDownloader` writes real files chunk by chunk and checks for cancellation between chunks with `ensureActive()`. Lesson 5 fails its first attempt so you can watch retry with backoff happen. Swap it for an OkHttp streaming implementation.
- **`NetworkMonitor`:** wraps the `ConnectivityManager` callback in `callbackFlow`, with `awaitClose` to unregister, plus `conflate` and `distinctUntilChanged`.
- **Offline-first outline:** `CourseRepositoryImpl` caches the course in Room. A refresh failure with a cached outline shows a snackbar, and with no cache it shows a full-screen error.
- **Tests (14):** 3 processor tests (on real temp files), 3 repository tests, 5 ViewModel tests and 3 `safeCall` tests.

## Follow-ups to prepare
- **Long downloads:** `setForeground()` with a notification (Android 14 needs the `dataSync` foreground service type), plus pause and resume.
- **Resuming partial files:** HTTP `Range` requests.
- **Storage quota:** checking it and evicting lessons on an LRU basis.
- **DRM / encryption:** ExoPlayer `DownloadService` with Widevine, and encrypted files.
- **Playback:** a `CacheDataSource` keyed by lesson id, and reporting progress back to the server when online again.

```bash
git diff 01-minimal..edtech/offline-course-downloads
```

## Follow-up: LLD (low-level design)
- **Classes:** `CourseViewModel`, `DownloadRepository`, `DownloadStore` (Room), `DownloadScheduler` (WorkManager), `LessonDownloadWorker` (thin adapter), `LessonDownloadProcessor` (logic), `LessonDownloader` (transfer), `LessonFileStore` (files), `NetworkMonitor`. Every Android-specific piece sits behind an interface.
- **Download state machine:** `NotDownloaded → Queued → Downloading(%) → Downloaded`, with `Failed` and cancel transitions. Draw it; who is allowed to move each transition (repository vs worker).
- **Why a processor separate from the worker:** WorkManager is hard to unit test; the processor is plain Kotlin with fakes, and the worker only maps outcomes to `Result.success/retry/failure`.
- **Idempotency and cleanup:** unique work per lesson with `KEEP`; partial files deleted on cancel or error; progress written to the database once per percent, not per chunk.
- **Pause/resume design:** add a `Paused` state, store bytes downloaded, and resume with an HTTP `Range` header.

## Follow-up: HLD (high-level design)
- **Content delivery:** videos transcoded into adaptive renditions (HLS/DASH) and served from a CDN; downloads pick one rendition by device storage and network (e.g. 480p on cellular).
- **Licences and DRM:** offline playback with Widevine persistent licences that expire (e.g. 30 days) and renew when online; revoke licences when a subscription ends.
- **Entitlements:** the server signs short-lived download URLs per user and lesson; the app never gets a permanent public link.
- **Storage policy:** device quota per course, LRU eviction of watched lessons, and a "download whole module" bulk action that schedules work in batches.
- **Progress sync:** watch progress recorded locally while offline, then synced idempotently (per lesson, max position wins) so streaks and analytics stay correct.

## Follow-up: System design (mock round)
**Prompt:** "Design offline video learning for an EdTech app like Quest: download courses, watch on a train, sync progress." About 45 minutes.
1. **Requirements:** download lessons or whole modules, play offline, resume progress, expire downloads with the subscription. Non-functional: survive app kills, respect storage limits, protect paid content.
2. **Client architecture:** download manager (WorkManager + state machine) or ExoPlayer `DownloadService`; Room for course outline, downloads and progress; DRM licence manager; network and storage monitors.
3. **API and data model:** `GET /courses/{id}` (outline), signed short-lived download URLs per rendition, licence endpoint, `PUT /progress` (per lesson, max position wins).
4. **Data flow:** user picks quality → work scheduled with constraints → chunks downloaded with resume → licence fetched → offline playback → progress synced later.
5. **Scale and performance:** HLS/DASH renditions on a CDN, Wi-Fi-only by default, bulk downloads batched, LRU eviction when storage is low.
6. **Trade-offs to state:** DRM cost and complexity vs piracy risk, rendition size vs quality, licence lifetime vs offline convenience.
