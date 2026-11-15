# `docusign/offline-signing-sdk`

**Target:** DocuSign, senior or lead Android / mobile SDK roles · **Base branch:** `01-minimal` · **Format:** an SDK-design machine coding exercise (2–3 hours) plus the system design discussion that follows.

## Why this problem
DocuSign ships an official **Android SDK** ([GitHub](https://github.com/docusign/mobile-android-sdk), [FAQ](https://www.docusign.com/blog/developers/docusign-android-sdk-faq)). It has separate modules for **offline signing**, **online (WebView) signing** and **captive signing**, and uses short-lived access tokens (about 1 hour) that the host app refreshes. The interview questions below are **my prediction** from the product, the job listings and recent reports, not a leaked prompt. The best evidence I found:

- **Recent report:** a July 2026 P3 offer with 3 technical rounds, mostly system design ([LeetCode](https://leetcode.com/discuss/post/8474565/docusign-software-engineer-july-2026-fin-up0c/)).
- **Android job listings:** Kotlin, Coroutines/Flow, Compose, MVVM/Clean Architecture, unit tests.

The problem mirrors the payment-SDK work on your resume (Nimbbl): a native/WebView checkout bridge, exactly-once result delivery, Maven Central distribution, backward compatibility, PII masking and merchant integration.

## Problem statement (as an interviewer would give it)
> Design and build an Android **signing SDK** that third-party apps embed.
> 1. **Online signing:** the host launches a hosted signing ceremony in a WebView and gets back **exactly one** result: completed, declined, cancelled or failed.
> 2. **Offline signing:** download an envelope, sign it with no network, survive app kills, and sync later. Never sign twice. If the sender voided the envelope while the user was offline, reject the signature.
> 3. **Auth:** the host's backend issues short-lived tokens. The SDK must never hold the integration secret, and it must refresh transparently.
> 4. **SDK hygiene:** minimal dependencies, a stable and explicit public API, testable by host apps, and no PII in telemetry.

## Layout
```
signing-sdk/                    Android library (the product)
  api/signing-sdk.api           Public API snapshot; ./gradlew apiCheck fails on unintended changes
  src/main/java/.../signing/    PUBLIC: SigningClient, SigningSdk.Builder, SignEnvelope contract, models, SigningException
  src/main/java/.../internal/   INTERNAL: TokenManager, OfflineStore, OfflineSyncer, SigningActivity, bridge, OriginPolicy, Redactor
  src/main/assets/docusign_sdk/ Demo ceremony page (served over https by WebViewAssetLoader)
app/                            Sample "merchant" app: Hilt provides the client, Compose UI, MVI ViewModel
```

## Design decisions to defend
| Decision | Why |
|---|---|
| `explicitApi()` + binary-compatibility validator | Nothing becomes public by accident. API changes show up in review as a diff of `signing-sdk.api`, which supports semantic versioning. |
| `SigningClient` is an **interface**, built by `SigningSdk.Builder` | Host apps can fake it in their own tests (the sample app does). No DI framework is forced on them. |
| **No Room, Retrofit or Hilt** inside the SDK | Avoids dependency conflicts and size bloat for merchants. Storage is a single JSON file with **atomic writes** (temp file + rename), written to disk before memory, so it's always durable. |
| `noBackupFilesDir` for offline documents | Signed documents must not end up in cloud backups. |
| `TokenProvider` callback | The host's backend holds the secret; the SDK only ever sees short-lived tokens. |
| `TokenManager` | Refreshes **proactively** (60 s before expiry) and **reactively** (on a 401, retries exactly once). Single-flight: 10 concurrent callers trigger 1 refresh. It only invalidates a token if it's still the one that failed. |
| Outbox with a `requestId` per signature | At-least-once delivery plus server de-duplication gives **effectively exactly-once** signing. A Mutex means concurrent `syncPending()` calls never double-submit. A failure on one item doesn't block the others. |
| Voided while offline | The server returns `VOIDED`, so the item is marked rejected, surfaced as `EnvelopeStatus.VOIDED`, and never retried. |
| `addWebMessageListener` with an origin allow-list | **Not** `addJavascriptInterface`, which exposes the bridge to every frame and origin. Only main-frame messages are accepted. |
| Hardened WebView | Navigation is limited to an allow-list, with no file or content access and no mixed content. Safe Browsing is on. Assets are served via `WebViewAssetLoader` on https (never `file://`). The WebView is destroyed in `onDestroy` to avoid leaks. |
| `SigningResultGate` | Duplicate "complete" posts, a back press after completing, and activity recreation (the delivered flag is saved in instance state) still produce **one** result. |
| `ActivityResultContract` (`SignEnvelope`) | The modern, testable launch API. The activity isn't exported. |
| `SigningException` sealed hierarchy | Hosts can handle errors exhaustively, and internal exception types never leak (`mapErrors`). |
| `Redactor` | Emails and long numbers are masked before reaching events or logs, and events never carry signer names. |

## Tests (29)
- **SDK (22):**
  - `TokenManager`: caching and skew, single-flight, retry after a 401, and no endless loop on repeated 401s.
  - `OfflineStore`: survives a restart, and a corrupted file doesn't crash the host.
  - `DefaultSigningClient`: the offline sign-then-sync flow, idempotent local signing, failure and retry, voided envelopes, de-duplication after a lost response, concurrent syncs, surviving an app restart, transparent token refresh, and error mapping.
  - Bridge parsing of untrusted input, the exactly-once gate, origin policy (including `user@host` and `?next=` tricks), and PII redaction.
- **Sample app (7):** the ViewModel tested against a **fake `SigningClient`**, which proves the SDK can be faked through its public interface.
- **Not covered:** `SigningActivity` needs an instrumented test (Espresso-Web), which isn't included.

```bash
./gradlew :signing-sdk:testDebugUnitTest :app:testDebugUnitTest
./gradlew apiCheck          # fails if the public API changed without ./gradlew apiDump
```

## Try it
Run the app, then:
- **Sign:** opens the ceremony. Tap "Finish signing", which posts "complete" twice on purpose; you get one result.
- **Save offline**, turn on airplane mode, then **Sign offline**. Turn the network back on and tap **Sync**.
- **Token refresh:** watch Logcat for `SigningSdk` events. The demo tokens expire after 2 minutes, so you'll see refreshes.

## Follow-up questions to prepare
- **Background sync:** the SDK exposes `syncPending()`, and the host schedules it. Why not bundle WorkManager? It's a dependency and policy decision that belongs to the host. When might you bundle it anyway?
- **Versioning:** semantic versioning, `@Deprecated` with a `ReplaceWith` cycle, and binary vs source compatibility (for example, why adding a parameter with a default value can break binary compatibility).
- **Multi-module distribution:** separate artifacts for online, offline and captive signing, as DocuSign does, plus a BOM. Talk about size budgets.
- **Signature capture:** a Compose `Canvas` producing a vector path, plus timestamps and audit trail fields. Discuss tamper evidence.
- **PDF rendering:** `PdfRenderer` tiles with a bitmap pool, and mapping signature-field coordinates correctly across zoom levels (your Hurix annotations experience).
- **Security:** certificate pinning trade-offs, Play Integrity, root detection, and encrypting offline documents with Keystore-backed AES-GCM.
- **Kotlin Multiplatform:** share `TokenManager`, the outbox and the store logic with iOS (your KMP Core API SDK), keeping the WebView and UI platform-native.

## Follow-up: LLD (low-level design)
- **Public surface:** `SigningClient` (interface), `SigningSdk.Builder`, `SignEnvelope` (contract), sealed `SigningResult`/`SigningException`, models. Everything else is `internal`. Explain explicit API mode and the binary-compatibility check.
- **Internal classes:** `DefaultSigningClient` (facade), `TokenManager` (single-flight cache), `OfflineStore` (atomic JSON file), `OfflineSyncer` (outbox drain), `SigningActivity` + `SigningResultGate` + `BridgeMessageParser` + `OriginPolicy`. Draw the dependencies; none point back to the public layer.
- **Patterns:** Facade (client), Builder (configuration), Strategy seam (`EnvelopeService` swaps the transport), State machine (result gate: pending → delivered), Observer (`envelopes: Flow`).
- **Idempotency in code:** the `requestId` is created once when signing, persisted with the outbox item, and reused on every retry.
- **Threading and lifecycle:** suspend functions are main-safe; the store writes on IO under a `Mutex`; the WebView is created and destroyed with the activity.

## Follow-up: HLD (high-level design)
- **End-to-end flow:** host app → host backend (holds the integration key, mints short-lived tokens) → eSignature API. Envelope created server-side, recipient view URL generated, completion reported by **Connect webhooks** to the host backend (the SDK only refreshes).
- **Offline signing server side:** submissions are idempotent by request id; the server validates the envelope is still active (not voided or expired) and records the device timestamp plus server receive time in the audit trail.
- **Document integrity:** documents hashed and tamper-sealed after signing; the certificate of completion lists every event with timestamps and IP/device data.
- **Storage and compliance:** documents encrypted at rest, regional data residency, retention policies; the SDK keeps offline copies in no-backup storage and deletes them after sync.
- **Observability:** per-SDK-version error rates, sync latency, WebView failures, and a kill switch to disable offline signing remotely.

## Follow-up: System design (mock round)
**Prompt:** "Design DocuSign's mobile signing: an SDK partners embed plus the backend behind it, including offline signing." About 45 minutes. This is the likely senior or lead round.
1. **Requirements:** embedded online signing, offline signing with later sync, envelope status, notifications, audit trail. Non-functional: legally valid audit trail, documents secure at rest and in transit, a small SDK, no secrets on devices.
2. **Client (SDK) architecture:** public facade + builder; online signing in a hardened WebView with an exactly-once result; offline outbox with idempotency keys; token provider callback; telemetry without PII.
3. **API and data model:** host backend → eSignature API for envelopes and recipient views; envelopes, documents, recipients, tabs and events; `POST /offline-signatures` idempotent by request id.
4. **Data flow:** online: host backend creates the envelope and signing URL → SDK opens the ceremony → completion goes to the host via webhooks. Offline: download → sign locally → outbox → sync → server validates the envelope state and seals the document.
5. **Scale and performance:** document storage in object storage with encryption and regional residency; webhooks with retries; rendering large PDFs in tiles on device.
6. **Trade-offs to state:** WebView ceremony vs native rendering, how long offline documents may stay on a device, device timestamps vs server time in the audit trail, SDK size vs features.
