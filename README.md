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
