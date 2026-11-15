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
