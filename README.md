# `edtech/timed-quiz`

**Domain problem** (not from a company report), designed around EdTech apps like the ones in your StudioProjects (Quest, academy). **Base branch:** `01-minimal` · **Suggested time:** 90 minutes

## Problem statement (as an interviewer would give it)
> Build a timed quiz for a lesson.
> - Load a quiz (title, duration, N single-choice questions) from the API.
> - Show one question at a time with Previous/Next. A progress strip shows answered vs unanswered questions, and tapping it jumps to that question.
> - Show a countdown in the toolbar that turns red in the last 10 seconds. At zero, auto-submit whatever is answered and tell the user.
> - Manual submit asks for confirmation if any questions are unanswered.
> - After submitting, show the score and a per-question review (your answer vs the correct one). Answers are locked from then on.
> - **The attempt must survive rotation and process death:** the same answers, the same question, and the correct remaining time.
> - Retake starts a fresh attempt.

## What's implemented
- **Deadline, not counter:** the ViewModel stores `deadline = now + duration` in `SavedStateHandle` and computes the remaining time from an injected `Clock`. After process death the timer is exact, including the time the app was dead. A "seconds left" counter would reset or drift.
- **No drift:** the timer sleeps until the displayed second actually changes (`(deadline - now) % 1000`), rather than repeating `delay(1000)`.
- **Saved attempt:** answers (with `-1` meaning unanswered, since `SavedStateHandle` needs Bundle-friendly types), the current question index and the submitted flag are all saved.
- **Pure grading:** `Quiz.grade(answers, timedOut)` lives in the domain layer and is unit-tested.
- **One-off effect:** `TimeUp` is a `Channel` effect that shows a snackbar.
- **Tests (10):** 6 ViewModel tests, 1 grading test and 3 `safeCall` tests.
  - The ViewModel tests use virtual time as the wall clock, so they run instantly. They cover the countdown, auto-submit at zero, manual submit locking answers, **process death with exact remaining time**, retake, and error with retry.

## Follow-ups to prepare
- **Server-authoritative timing:** the server returns the deadline, so the client clock can't cheat. Handle clock skew.
- **Offline attempts:** queue the submission (see `edtech/attendance-offline-sync`).
- **Question types:** multi-select, ordering and free text, modelled as a sealed class with a `when` per type.
- **Proctoring signals:** log app backgrounding during the attempt with a lifecycle observer.
- **Accessibility:** announce the remaining time at intervals, not every second.

```bash
git diff 01-minimal..edtech/timed-quiz
```

## Follow-up: LLD (low-level design)
- **Classes:** `QuizViewModel` (timer + attempt state), `Clock` (injected), `Quiz`/`Question`/`QuizResult` (domain), `grade()` (pure), `QuizRepository`. Why the clock is an interface (deterministic tests, server-time later).
- **Question types:** model `Question` as a sealed hierarchy (`SingleChoice`, `MultiChoice`, `Ordering`, `FreeText`), each with its own answer type and a `grade(answer)` method. Adding a type = a new subclass; the compiler flags every `when` to update.
- **Timer as a state machine:** not started → running (deadline) → submitted (manual or timeout). Persist the deadline, not a counter; compute remaining time from the clock on every tick.
- **Attempt persistence:** what goes in `SavedStateHandle` (small, Bundle-friendly: deadline, answers, index) vs Room (a full attempt history you can resume after reboot).
- **Grading rules:** negative marking, partial credit and section weights as a `ScoringPolicy` strategy passed to grading, so exam types differ without changing the engine.

## Follow-up: HLD (high-level design)
- **Server-authoritative exams:** `POST /attempts` returns `attemptId` + server `deadline`; answers autosave with `PUT /attempts/{id}/answers` (idempotent, last write wins per question); the server rejects submissions after the deadline plus a grace window.
- **Clock skew:** the client computes `offset = serverTime - deviceTime` once per attempt and shows the countdown with it; the server deadline always wins.
- **Scale:** exam start is a thundering herd (everyone starts at 10:00), so pre-generate question sets, serve them from a CDN encrypted per attempt, and queue submissions for grading.
- **Integrity:** randomised question and option order per attempt, log app backgrounding and focus loss, and rate-limit answer changes. Be honest that a client can't prevent cheating, only signal it.
- **Results:** objective questions graded instantly server-side; subjective ones go to a review queue; results published when the exam window closes.

## Follow-up: System design (mock round)
**Prompt:** "Design an online exam platform's mobile app for 200,000 students starting at the same minute." About 45 minutes.
1. **Requirements:** start an exam, timed questions, autosave, resume after a crash or call, submit on timeout, results later. Non-functional: no lost answers, fair timing, survives the start-time spike.
2. **Client architecture:** exam module with a server-anchored timer (deadline + clock offset), local autosave in Room, background answer sync, lifecycle observer for focus-loss signals.
3. **API and data model:** `POST /attempts` (returns deadline and an encrypted question set), `PUT /attempts/{id}/answers` (idempotent per question), `POST /attempts/{id}/submit`.
4. **Data flow:** download questions before the start time → unlock at start → answers saved locally and synced every few seconds → timeout submits locally and syncs when online.
5. **Scale and performance:** pre-download encrypted papers to avoid the spike, CDN delivery, queued grading, horizontal answer-ingest service.
6. **Trade-offs to state:** server vs client timing authority, offline exams vs integrity, how much proctoring a phone can realistically do.
