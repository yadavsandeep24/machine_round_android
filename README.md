# `edtech/attendance-offline-sync`

**Domain problem** (not from a company report), designed around your ERP work (`erp_sdk`, "zdl erp"). **Base branch:** `01-minimal` · **Suggested time:** 2 hours (senior)

## Problem statement (as an interviewer would give it)
> Teachers take attendance in classrooms with patchy Wi-Fi.
> - Show today's roster for a class, cached so it opens offline.
> - Mark each student Present or Absent, plus "mark the rest present". **Every tap must work instantly, online or offline.**
> - Marks sync to the ERP server in the background, survive app kills, wait for network and retry with backoff. Show the sync state for each student (pending, synced, failed) and a count of pending marks.
> - Retries must never double-apply a mark. If the teacher changes a mark while it's uploading, the newer choice must win.
> - If the school office corrected a mark on the web portal after the teacher marked it, the newer correction wins and the app shows that it was updated.

## What's implemented
- **Outbox pattern:** each `(class, date, student)` row carries a `version` and a `syncedVersion`. A row is in the outbox while `version > syncedVersion`. The UI reads only Room, so marking never waits for the network.
- **Idempotency:** `requestId = key + version`. If the response to an upload is lost and the upload is retried, the server recognises the request and doesn't write again (tested).
- **Edits during an upload:** an acknowledgement marks only the version that was sent. An edit made while the batch was in flight stays pending and goes out on the next pass (tested).
- **Conflicts:** last-writer-wins by `updatedAt`. When the server's value is newer, the app adopts it and sets `overriddenByServer`, which shows a "Updated from portal" chip (tested).
- **No-op edits:** setting the same value again creates no outbox work.
- **WorkManager:** unique work with `APPEND_OR_REPLACE`, so marks made during a running sync get a follow-up run (with `KEEP` they would be dropped). Constraints: network connected, with exponential backoff.
  - The worker is a thin `@HiltWorker` over `AttendanceSyncer`, which drains the outbox in batches of 50 and can be unit-tested.
- **Room without destructive migration:** unlike the other branches, the outbox holds data that hasn't synced yet, so wiping the database would lose user data.
- **`NetworkMonitor`:** shows an offline banner. "Sync now" while offline explains that the sync will happen later.
- **Tests (14):** 6 syncer tests, 5 ViewModel tests and 3 `safeCall` tests.
  - The syncer tests cover draining, a failure followed by a retry, an idempotent replay, no-op edits, a portal conflict, and an edit made while its upload is in flight.

## Follow-ups to prepare
- **Server-assigned versions** or vector clocks instead of client timestamps, and handling clock skew.
- **Bulk "mark all":** a single transaction, with uploads batched by the server's limit.
- **Retention and audit:** keep a history of who changed what and when.
- **Multiple devices:** two teachers marking the same class.
- **Reporting sync health:** how long marks stay pending, alerting when they're stuck, and telemetry on attempts.

```bash
git diff 01-minimal..edtech/attendance-offline-sync
```

## Follow-up: LLD (low-level design)
- **Classes:** `AttendanceViewModel`, `AttendanceRepository` (local-first writes), `AttendanceStore` (outbox, Room), `AttendanceSyncer` (drains the outbox), `SyncScheduler` (WorkManager), `FakeErpServer` (stand-in API), `Clock`.
- **The outbox record:** `version` vs `syncedVersion`, `attempts`, `lastError`, `overriddenByServer`. Derived `syncState` instead of a stored status, so it can never disagree with the versions.
- **Conflict resolution as a strategy:** today last-writer-wins by timestamp; define a `ConflictResolver` interface so a school can pick "server wins", "teacher wins" or "flag for review" without touching the syncer.
- **Ack scoped to the sent version:** an acknowledgement marks only the version that was uploaded, so an edit made mid-upload stays pending. Show the test.
- **WorkManager policy:** `APPEND_OR_REPLACE` so marks made during a running sync get a follow-up run; with `KEEP` they'd wait for the next trigger.

## Follow-up: HLD (high-level design)
- **ERP sync API:** `POST /attendance/batch` with per-record idempotency keys and versions; the response returns per-record outcomes (accepted, conflict with the server value, rejected) so one bad record doesn't fail the batch.
- **Source of truth:** the server; the device is a cache plus an outbox. Use server-assigned versions, or a per-record sequence, instead of trusting device clocks.
- **Multi-device and multi-role:** two teachers or a teacher plus the office editing the same class; record who changed what, show conflicts, and keep a full audit log for compliance.
- **Scale:** every school marks attendance between 9:00 and 9:30, so the sync endpoint must absorb a burst; queue writes and process them asynchronously, with the client retrying with backoff and jitter.
- **Downstream:** parent notifications for absences sent only after the mark syncs and a grace period passes, so a corrected mark doesn't send a false alert.

## Follow-up: System design (mock round)
**Prompt:** "Design a school ERP mobile app where teachers take attendance and marks offline, synced for 10,000 schools." About 45 minutes.
1. **Requirements:** take attendance and enter marks offline, sync automatically, see sync status, parents notified of absences, office can correct records. Non-functional: no lost or duplicated data, correct under poor connectivity, auditable.
2. **Client architecture:** local-first writes to Room with a versioned outbox; WorkManager sync with backoff; conflict resolver strategy; roster and timetable cached per teacher.
3. **API and data model:** `POST /sync/batch` with idempotency keys and versions, per-record outcomes; records keyed by (class, date, student) with an audit log of changes.
4. **Data flow:** tap → saved locally (instant) → outbox → batch upload → per-record result → conflicts shown to the teacher → notifications sent after sync plus a grace period.
5. **Scale and performance:** burst traffic every morning, so queue writes and process asynchronously; multi-tenant data partitioned by school; client retries with jitter.
6. **Trade-offs to state:** last-writer-wins vs manual review, server-assigned versions vs device clocks, how long data may stay unsynced before warning the teacher.
