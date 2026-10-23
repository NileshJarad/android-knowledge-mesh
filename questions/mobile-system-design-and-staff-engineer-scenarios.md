# Mobile System Design and Staff Engineer Scenarios

> Answers written from the perspective of a **Staff Android Engineer**. This section is **design- and judgement-heavy**, so the answers are structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach / architecture sketch* → *Trade-offs* → *How it is validated in production* → *Talking points*.
> Code is deliberately omitted here — what is being assessed is architectural reasoning, sequencing, and communication, not API recall.

---

### 1. Design a banking transaction-history screen that supports millions of records, offline access, search, and filtering.

**What the interviewer is really asking**
Can you design a screen where **the data volume is the problem**, and show that you understand *where* the data lives, *when* the network is involved, and *how* you avoid loading a million rows into memory or into a `RecyclerView`.

**Key concepts**
- **Local-first**: Room is the single source of truth for the UI; the network only ever writes into Room.
- **Cursor-based (keyset) pagination**, never `OFFSET`
- **Paging 3** — `PagingSource`, `PagingData`, `RemoteMediator`
- **FTS5** for search (SQLite's full-text search) vs `LIKE '%x%'`
- **Incremental / delta sync** with a cursor, not full re-fetch
- Local **filtering** with SQL indices; **sorting** in SQL with a matching index
- **Selective sync** — sync only what the user is likely to look at
- Retention policy / pruning (a banking app cannot keep a million rows forever on device)

**Approach / architecture sketch**

```
UI (Compose)
  └── observes  Room Flow<Transaction>          ← never touches the network
        ▲                        ▲
        │ writes                 │ writes
   SyncWorker (WorkManager)   Repository (single writer)
        │
        └── API: GET /v1/transactions?after=<cursor>&accountId=…
```

1. **Separate the "display" concern from the "sync" concern.** The screen reads from a Room-backed `Flow`. The network layer is never on the UI thread's critical path. If the DB has data, the screen renders immediately — offline or not.
2. **Never load a million rows into the client.** Use **keyset pagination**: `WHERE (createdAt, id) < (:lastCreatedAt, :lastId) ORDER BY createdAt DESC, id DESC LIMIT 50`. `OFFSET 500000` forces the database to walk and discard 500k rows on every page request — it is O(offset) per page and gets worse with every page.
3. **Use Paging 3** with a `RemoteMediator` so that paging works offline *and* online:
   - `PagingSource` reads from Room (so paging is fully available offline),
   - `RemoteMediator` refreshes the DB when the user is near the end of the list.
   This means "scroll to the bottom while offline" still works, because the data is already local.
4. **Sync incrementally with a cursor.** The server exposes `GET /transactions?after=<seq>`; the client stores `lastSeq`. On each sync, fetch only what is new. The initial backfill is chunked with a progress indicator.
5. **Do not backfill a million rows to the device.** The strategy: sync **recent + relevant** history eagerly (e.g. the last 12 months, or the account's activity within a window), and let the user **request more history explicitly** ("Load older transactions" → a paged fetch of the next chunk). Search against older history is the exception — see the FTS point below.
6. **Search with FTS5, not `LIKE`.** `LIKE '%acme%'` cannot use an index and scans the whole table. Create an FTS5 virtual table over the searchable columns, kept in sync via triggers or by writing to it in the same transaction. FTS5 gives prefix matching (`acme*`), which is what users expect from a search box.
7. **Filter in SQL with indices**, not in memory. `WHERE accountId = ? AND createdAt >= ? AND type = ?` needs a composite index in that column order: `(accountId, createdAt DESC)` plus a covering index for the type filter where justified. Verify with `EXPLAIN QUERY PLAN` — look for `USING INDEX` and the absence of `USE TEMP B-TREE FOR ORDER BY`.
8. **Prune.** A device cannot hold unbounded history. Keep a rolling window (e.g. 24 months) locally, prune older rows on a schedule, and make older history an explicit server-side request ("view in web / download statement").
9. **Budget the screen.** Only the visible rows (+ a small prefetch) are ever in memory. The list must be virtualised, keyed by a stable `id`, and must render a bounded number of rows.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Room as the single source of truth | Instant render, full offline, one code path for UI | More code; the DB can drift from the server |
| Cursor/keyset pagination | O(page size) per page; stable under concurrent inserts | Requires a compound sort key; not usable with arbitrary sort orders |
| `OFFSET` pagination | Trivial to implement | O(offset) per page; degrades badly; rows can shift under concurrent writes |
| Eager backfill of full history | Everything is instantly searchable offline | Huge download; storage cost; slow first sync; no realistic path to millions of rows |
| Selective + on-demand older history | Fast first sync; bounded storage | Older data needs a network round trip; a worse search experience for old records |
| FTS5 for search | Indexed, fast, supports prefix match | Extra table to keep in sync; tokenizer language needs care |
| `LIKE '%x%'` | Trivial | Full table scan — unusable at a million rows |
| Filtering in SQL | Uses indices; scales | Every new filter needs an index (write cost) |
| Filtering in memory | No index maintenance | Requires loading everything — not viable at this scale |
| Paging 3 with a Room-backed `PagingSource` | Paging works offline; one source of truth | `RemoteMediator` adds complexity |
| Paging straight from the network | No local storage pressure | No offline support; slow; broken when the network is bad |

**How it is validated in production**
- Query plans checked in tests with `EXPLAIN QUERY PLAN` asserted to use the index.
- A migration/perf test that seeds 1M synthetic rows and asserts p95 page latency.
- Field metrics: sync duration, rows synced, DB size, and the p95 time-to-first-row on the history screen by device class.

**Talking points / follow-ups**
- *"Why not `OFFSET`?"* → It is O(offset); at 500k rows the database must scan and discard half a million rows for the next page.
- *"How does search work offline?"* → FTS5 over the local table, restricted to the locally synced window.
- *"How much data do you store?"* → A bounded window, e.g. 24 months; older history is server-side only.
- *"How do you handle a new transaction arriving while the user scrolls?"* → The sync writes into Room; the `Flow` re-emits; the list uses stable keys so only the affected rows recompose.
- *"What if the user wants all million rows in a CSV?"* → A server-side export job delivered as a download, not a client-side query.

---

### 2. Design a payment architecture that prevents duplicate charges despite timeouts, retries, process death, and concurrent requests.

**What the interviewer is really asking**
Do you understand that **the client cannot know whether a charge happened**, and can you design an architecture where the *server* enforces exactly-once while the client behaves correctly under every failure mode?

**Key concepts**
- **Idempotency keys** — a client-generated key minted once per user intent, sent with every attempt
- Server-side idempotency store: key → the original response, with a TTL
- The **tri-state outcome**: success / failure / **unknown**
- **Reconciliation** — a status endpoint that resolves an unknown outcome
- Atomicity of the "record the intent" step — the key and the local record must be written together
- `Mutex` / single-flight for **in-process** concurrency
- A DB constraint for **cross-process / cross-device** concurrency
- Optimistic UI with a **pending** state and an explicit resolution

**Approach / architecture sketch**

```
UI  ──intent──▶ PaymentRepository
                    │
                    ├──▶ [transaction] write PENDING + idempotencyKey (local DB)
                    │
                    └──▶ POST /payments  (Idempotency-Key: <uuid>)
                              │
                              ├── server: dedupe(key) → charge once, store response
                              │
                    ◀── 200 (replayed original response) | 409 | 5xx | timeout
```

1. **Mint the idempotency key once, per user intent, and persist it before the network call.** The key must be written to the local DB **in the same transaction** as the `PENDING` record. If the process dies between "user tapped Pay" and "the request was sent", the key survives and the retry reuses it. If the key were generated in memory at call time, a retry after process death would mint a *new* key and the server would charge twice.
2. **The server enforces exactly-once.** The server stores `(idempotency_key → original response)` atomically with the charge. A second request with the same key returns the *stored original response*, not a new charge. The client can therefore retry as freely as it likes — the server is the arbiter. This is the only place the guarantee can live, because the client cannot observe the server's internal state.
3. **Treat a timeout as UNKNOWN, not FAILED.** A timeout means "I did not hear back". The client keeps the record in `UNCERTAIN`, keeps the same idempotency key, and schedules a **reconciliation** query against `GET /payments/by-key/{key}`. The server returns the authoritative status. Only the server can convert UNKNOWN into SUCCESS or FAILURE.
4. **Handle concurrency in three layers.**
   - **UI layer**: disable the button and show a pending state — prevents the common double-tap.
   - **In-process layer**: a `Mutex` (or single-flight `Deferred`) around the submit call so two coroutines in the same process share one request.
   - **Cross-process / cross-device layer**: the server's idempotency store plus a **DB unique constraint** on the local key, so a second device cannot create a second local intent for the same order.
5. **Bound retries.** Retry with exponential backoff and full jitter, and **only** for transient failures (connection error, 5xx, 429). Never retry a 4xx that is a validation error. And never retry indefinitely — cap the attempts and fall back to reconciliation.
6. **Do not lose the intent.** If the process dies mid-flight, the `PENDING`/`UNCERTAIN` record is still in the local DB. On the next launch, the app scans for unresolved records and reconciles them. This is the "user kills the app during payment and reopens later" scenario.
7. **Make the UX honest.** A `PENDING` state that shows "Confirming your payment…" and a "Check status" action. Never show "Payment failed" for an UNKNOWN outcome — that would push the user to retry and risk a second charge.
8. **Audit everything.** Every attempt (with its key, timestamp, and outcome) is logged — locally and server-side. This is what makes a duplicate-charge dispute tractable.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Server-side idempotency store | The only place exactly-once can be enforced; retries become free | Server state to store and expire; a key TTL policy is needed |
| Client-generated idempotency key | Client controls retries; no coordination | Must be persisted before the first attempt or a process death breaks it |
| Deduping on (user, orderId) instead | Simpler for the client | Wrong for a legitimate double-charge of the same order; couples the key to the business entity |
| UNKNOWN state + reconciliation | Honest; eventually resolves | More states; needs a resolution UI and a background job |
| Reporting timeout as FAILED | Simple | Causes duplicate charges on retry — the exact bug to avoid |
| Optimistic UI (show success immediately) | Fast; good UX | Wrong for payments — a false success is worse than a delay |
| Idempotent UI only (disable the button) | Cheap | Does not survive a process death or a second device |
| Retrying the same key | Safe with server dedupe | Consumes the idempotency store; needs a TTL |
| Auto-retry on any error | Simple | Hides permanent errors; hammers the server |

**How it is validated in production**
- A test suite that replays the same idempotency key 10× concurrently against a staging server and asserts exactly one charge.
- A synthetic monitor that reports duplicate-charge attempts per key.
- A reconciliation-health metric: unresolved `PENDING`/`UNCERTAIN` records older than N minutes (should be ~0).

**Talking points / follow-ups**
- *"Why can't the client guarantee exactly-once?"* → Because the failure window is between the server committing the charge and the client receiving the response. The client cannot observe that window.
- *"How long do you keep the idempotency key?"* → Longer than the maximum client retry window (e.g. 24–72 hours) — and the business must define how long a duplicate submission is meaningful.
- *"What if the user genuinely wants to pay twice?"* → A new intent mints a new key. The key is per *intent*, not per user or per order.
- *"How do you handle the case where the server charges but the key expires?"* → The key TTL must exceed the client's retry horizon; otherwise the client must reconcile via a different mechanism (an order reference, not a key).

---

### 3. Design a common networking layer for 20 Android feature modules with authentication, retries, caching, logging, and error handling.

**What the interviewer is really asking**
Can you design a **platform** rather than a library — one that 20 teams can adopt without blocking delivery, and that you can change centrally without touching 20 modules.

**Key concepts**
- An **OkHttp `Interceptor` chain** — the natural seam for cross-cutting concerns
- `Authenticator` vs `Interceptor` — the correct place for token refresh
- A **single-flight token refresh** (`Deferred` + `Mutex`) so 20 concurrent 401s cause one refresh
- A **typed error model** (`AppError` sealed hierarchy) — the single biggest lever on module-level code quality
- `kotlinx.serialization` / Moshi with a shared `Json` instance
- **Timeouts**: `connectTimeout`, `readTimeout`, `writeTimeout`, `callTimeout`
- **Retry** with exponential backoff + **full jitter**, and a **retry budget** to avoid retry storms
- **Correlation IDs** (`X-Request-Id`) propagated end-to-end — the bridge to backend traces
- **Redaction** of sensitive headers/body before logging
- **Network availability** gating and offline degradation
- Gradle module structure: `:core:network` as an `api` dependency

**Approach / architecture sketch**

```
:core:network
  ├── HttpClientFactory      → OkHttpClient + interceptors
  ├── AuthInterceptor        → adds the access token
  ├── TokenAuthenticator     → refresh (single-flight) + retry once
  ├── RetryInterceptor        → backoff + jitter, retry budget
  ├── LoggingInterceptor     → redacting, sampled, body only in debug
  ├── RequestIdInterceptor   → X-Request-Id (UUID or server-provided)
  ├── ErrorMapper            → Throwable → AppError
  └── Result<T>              → typed success/failure
```

1. **Model the outcome explicitly, not as an exception.** Every call returns a sealed result:
   `Success(data)` | `Failure(AppError.Network)` | `Failure(AppError.Timeout)` | `Failure(AppError.Unauthorized)` | `Failure(AppError.Http(code, body))` | `Failure(AppError.Serialization)` | `Failure(AppError.Unknown)`.
   This means a feature module never has to interpret an `IOException` vs a `SocketTimeoutException` vs a `JsonSyntaxException`. It is the highest-leverage decision in the whole layer — it converts 20 modules' ad-hoc error handling into one switch.
2. **Put authentication in an `Authenticator`, not in an interceptor.** The `Authenticator` is only invoked on a 401, so the token refresh does not run on every request. It must also **return null** for anything other than a 401 so OkHttp does not loop, and it must limit itself to **one retry** of the original request.
3. **Make token refresh single-flight.** If 20 requests get a 401 simultaneously, there must be exactly one refresh call. Implement it with an atomic `Deferred`:
   - A `@Volatile var inFlight: Deferred<Token>?`.
   - On a 401, take the current `inFlight` or create one under a `Mutex`.
   - Await it, and clear it **only if it is still the same instance** (identity check) — otherwise a second request could clear a newer refresh.
   The `Mutex` is **not reentrant**; never call `refresh()` from inside a block that already holds it — use a `withLock` only to publish the `Deferred`, and do the network call outside the lock.
4. **Add a retry interceptor with a budget.** Retry only idempotent methods (`GET`, `HEAD`, `PUT`, `DELETE` — and `POST` only when it carries an idempotency key). Retry on connection failures, 408, 429 (honouring `Retry-After`), and 5xx. Use **full jitter** (`random(0, base * 2^attempt)`) to avoid a thundering herd. Add a **retry budget** (a token bucket per host) so a mass outage does not turn 20 modules into a load generator against your own backend.
5. **Centralise caching, but keep it honest.** An OkHttp disk cache with a `Cache-Control`-aware policy, plus an **ETag/`If-None-Match`** path so a revalidation is a 304 rather than a body. But the *authoritative* cache for offline-first features is the local database, not the HTTP cache — the HTTP cache is a network-layer optimisation, not a data layer.
6. **Redact at the logging boundary.** Never log `Authorization`, cookies, card numbers, or tokens. A `redactingLogger` replaces the values before they leave the process; the crash-reporting pipeline also scrubs, but the client-side redaction is the control that actually works.
7. **Propagate a request ID.** Generate a `UUID` per request (or accept one from the caller), send it as `X-Request-Id`, and log it with the response. This is the **join key** to the backend's traces — it is what makes a client-side failure diagnosable (see Q9).
8. **Make timeouts a policy, not a per-call guess.** Defaults for `connectTimeout` (short — fail fast on a dead network), `readTimeout` (a multiple of the p99 latency, not the p99), and `callTimeout` (the outer bound). Let a feature override within limits.
9. **Adopt without blocking.** Ship the layer as a `:core:network` Gradle module exposing a small, stable API. Migrate one module as the reference implementation, publish the migration guide and a codemod-ish set of templates, and let teams migrate on their own schedule. Track adoption, not mandate it.
10. **Add a lint rule / CI check** that forbids direct `OkHttpClient()` construction outside `:core:network` — otherwise the standard decays within a quarter.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Central `core:network` module | One place to fix auth/retries/logging; enforceable | A shared module can become a bottleneck for feature teams; requires governance |
| Sealed `Result`/`AppError` | Removes per-module error interpretation; exhaustive `when` | More verbose at call sites; a migration cost |
| Exceptions with a base class | Familiar; less churn | Easy to forget to catch; inconsistent handling across modules |
| `Authenticator` for refresh | Only runs on 401; retries the original request | Must return `null` correctly or OkHttp loops; only handles 401 |
| Interceptor for refresh | Simple | Refreshes on every request unless carefully gated |
| Client-side single-flight refresh | One refresh under concurrency | Requires careful `Deferred` identity handling |
| Automatic retry in the layer | Transparent; fixes flaky networks for everyone | Can mask real errors; must be idempotency-aware; needs a budget |
| Per-module retry policy | Fits the endpoint's semantics | Configuration sprawl; harder to reason about |
| HTTP cache | Free offline-ish reads; 304 support | Wrong as the data layer; stale-data bugs |
| DB as the data cache | Correct offline-first; one source of truth | Requires per-feature work |
| Central logging with redaction | One place to get redaction right | Bodies are sensitive — sample and limit them |

**How it is validated in production**
- Contract tests against the layer: "20 concurrent 401s → exactly one refresh call", "a 429 is retried with `Retry-After`", "no `Authorization` value ever reaches the log sink".
- A synthetic error-injection job that fails a percentage of requests and measures the client's retry amplification factor.
- Per-module adoption dashboard and a CI check for direct client construction.

**Talking points / follow-ups**
- *"Why not just let each module use Retrofit?"* → 20 modules each with their own interceptors means 20 different auth, retry and error behaviours — the thing the shared layer exists to eliminate.
- *"How do you avoid a retry storm?"* → A retry budget (token bucket) per host, plus full jitter.
- *"How do you prevent the standard decaying?"* → A CI lint check plus an architecture decision record plus one team owning the module.
- *"What happens to in-flight requests on logout?"* → A session epoch fence — see Q9/Section 8, Q4: requests from an old epoch are rejected rather than silently returning another user's data.

---

### 4. Design a reusable offline-first synchronization framework for multiple features.

**What the interviewer is really asking**
Can you design a **framework with an extension model** — a generic sync engine plus per-feature policy — rather than a copy-pasted sync implementation in each feature.

**Key concepts**
- A generic **sync engine** with per-feature **`SyncStrategy`** / `SyncSpec` implementations
- **Delta sync** with a per-feature cursor stored in the DB
- **Push** via an **outbox** pattern (the local write and the pending push are committed together)
- **Idempotency keys** on push, applied server-side
- **Conflict resolution** policy per feature: server-wins, client-wins, last-write-wins, three-way merge, or manual
- **Watermarks** / `lastAppliedSeq` for exactly-once-ish application of server events
- **Atomicity**: local data + outbox row + cursor advance in one transaction
- **Scheduling**: `WorkManager`, unique work names per feature, `ExistingWorkPolicy.KEEP`
- **Observability**: per-feature sync status, last-synced-at, error count

**Approach / architecture sketch**

```
:core:sync
  ├── SyncEngine             ← generic: scheduling, network gate, retry, cursor
  ├── SyncStrategy (iface)   ← per-feature: pull(since), push(outbox), merge()
  ├── Outbox                 ← durable queue, one table, feature-tagged
  └── SyncStatus             ← StateFlow<SyncState> per feature
```

1. **Split the engine from the policy.** The generic engine handles: scheduling, constraints, retry/backoff, cursor management, transaction boundaries, telemetry, and the push queue. The per-feature strategy handles: which endpoints to call, how to parse the delta, how to merge a conflict, and how to serialise a local change for push. This is the difference between a framework and a copy-paste.
2. **Use the outbox pattern for writes.** The user writes locally; in the **same transaction** you insert a row into the outbox holding a **serialised snapshot of the payload** (not a reference — the referenced row may change before the push happens). The sync worker drains the outbox. This gives: no lost writes, no "I updated locally but it never reached the server" bug, and retries that are naturally idempotent.
3. **Pull with a cursor and apply with a watermark.** The server sends a monotonically increasing sequence per feature. The client stores `lastAppliedSeq`. Applying an event is idempotent because a replayed event has `seq <= lastAppliedSeq` and is skipped. Combine with an **upsert** keyed on the server id so a partially applied batch converges.
4. **Make the merge policy explicit and per feature.** Do not hard-code one policy in the framework:
   - **Server-wins** — simple, appropriate for read-mostly data (a price list, a catalogue).
   - **Client-wins** — appropriate for a user-authored draft.
   - **Last-write-wins per field, with a server timestamp** — appropriate for a settings-like record.
   - **Three-way merge** (base / local / remote) — appropriate for structured documents.
   - **Manual** — the server returns a conflict and the UI asks.
   The framework supplies the hook; the feature decides.
5. **Conflict detection needs a version.** The server returns a `version` (or an `ETag`) per record. The client sends `If-Match` with the version it based its edit on. A `409`/`412` means a genuine conflict → invoke the feature's merge policy. Without a version, "last write wins" is silent data loss.
6. **Atomicity at the boundaries.** Everything that must move together moves in one transaction: apply the delta rows, advance the cursor, insert the outbox row, write the local edit. If the process dies mid-transaction, SQLite rolls back and the next run re-applies — safe because the apply is idempotent.
7. **Schedule per feature, uniquely.** `enqueueUniqueWork("sync:<feature>", KEEP, request)` so a feature never has two concurrent syncs, and a `PeriodicWorkRequest` as the backstop. Also allow an **explicit user-triggered** sync that uses the same unique name with `KEEP`.
8. **Expose sync status as state.** `SyncStatus` per feature: `Idle | Syncing | Offline | Failed(reason) | Pending(n)`. The UI shows a subtle indicator; it also gives the framework a testable contract.
9. **Make the network optional.** The engine must handle "no network" as a normal state — defer the work, keep the outbox, and retry when the constraint is satisfied. `WorkManager` with a network constraint handles the scheduling; the engine handles the policy.
10. **Budget the sync.** Cap the delta page size, cap the batch size, and process in chunks so a huge initial backfill does not blow the memory or the battery. Persist the cursor per chunk so an interrupted backfill resumes.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Generic engine + per-feature strategy | One implementation of the hard parts; each feature only writes policy | The interface must be designed carefully; a bad abstraction is worse than none |
| Copy-pasted sync per feature | Total local flexibility | Divergent behaviour; every bug fixed 20 times; no consistent observability |
| Outbox pattern | No lost writes; retries are safe; the queue is inspectable | Extra table and code; payload snapshots can be large |
| Direct push from the UI layer | Simple; immediate | A failure leaves the local state inconsistent; no retry; the UI must handle the network |
| Cursor/delta sync | Small payloads; no full refetch | Requires server support; cursor invalidation needs handling |
| Full refetch | Simple; server needs no cursor support | Expensive; unusable at scale; loses local-only state |
| Per-feature conflict policy | Correct semantics per data type | More design work; each policy needs tests |
| One global conflict policy | Simple | Wrong for most features |
| `WorkManager`-based scheduling | Doze-aware; survives process death | Deferred; not exact |
| FCM push-triggered sync | Immediate when the server changes | Requires notifications; not guaranteed; adds a server dependency |

**How it is validated in production**
- A **sync simulator** in tests: a fake server that applies the delta out of order, duplicates events, and returns conflicts — assert the client converges.
- Per-feature telemetry: sync duration, pages, conflicts, outbox depth, last-synced-at, and the ratio of failures by cause.
- A "stuck outbox" alarm: pending items older than a threshold.

**Talking points / follow-ups**
- *"How do you guarantee no lost writes?"* → The outbox row is committed in the same transaction as the local change; the payload is a snapshot.
- *"What if the server changes the delta format?"* → Version the sync endpoint; the client sends its sync protocol version and the server adapts (see Q7).
- *"How do you handle a huge initial backfill?"* → Chunk it, persist the cursor per chunk, show progress, and cap the work per sync window.
- *"Why does the client need to know the server's version?"* → Without a version, the client cannot distinguish "no conflict" from "silent overwrite".

---

### 5. An application has 100 feature modules and slow build times. How would you restructure the architecture and improve build performance?

**What the interviewer is really asking**
Do you know that **build performance is an architecture problem**, and can you connect module boundaries to build-graph shape, incremental correctness, and team parallelism?

**Key concepts**
- **Gradle configuration cache** and the **build cache**
- **Incremental compilation** (Kotlin), **incremental annotation processing** (KSP/kapt)
- **Classpath snapshots**; `kapt.use.worker.api`
- **The "configuration" phase** — the cost that configuration cache removes
- **Dynamic versioning** → remote dependency lookups on every build (network = slow and flaky)
- **Module count itself** — every module adds configuration and classpath overhead
- **Module boundaries aligned to features** (vertical) vs to layers (horizontal)
- `api` vs `implementation` — leaking `api` widens the classpath of every consumer
- **API/binary compatibility validation** (binary-compatibility-validator, Metalava) — enables safe refactors
- **Build cache** with a remote cache (Gradle Enterprise / Develocity)
- **`org.gradle.parallel`, `org.gradle.caching`, `org.gradle.configuration-cache`**
- The **monorepo vs polyrepo** question
- `ksp` over `kapt` (KSP is dramatically faster)
- **Version catalogs** for dependency resolution

**Approach / architecture sketch**

```
app/
  ├── core/           (network, database, designsystem, analytics)  ← stable, rarely changes
  └── feature/        (feature-login, feature-checkout, …)           ← changes constantly
```

1. **Measure before restructuring.** Get the numbers: `buildScan`/Develocity, or the Gradle build scan, or `--profile`. You need to know the split between:
   - **Configuration time** (evaluating build scripts) — the biggest and most fixable chunk in large projects,
   - **Execution time** (compilation, annotation processing, packaging),
   - **Dependency resolution / network** (dynamic versions, snapshot lookups),
   - **Test execution**.
   A common profile in a 100-module project: 60% configuration, 25% compile, 15% everything else. Guessing at this point wastes weeks.
2. **Turn on the configuration cache and the build cache first.** These are usually the highest ROI for the least risk:
   - `org.gradle.configuration-cache=true` — removes configuration-time work from subsequent builds.
   - `org.gradle.caching=true` + a remote build cache — reuse compiled outputs across machines and CI.
   These two often halve the build time in a large project with no architectural change at all.
3. **Eliminate dynamic versions and snapshot dependencies.** `1.+` and `latest.release` force a **network round trip on every build** and make the build non-reproducible. Pin everything in a version catalog and enable dependency locking. This is a large chunk of the "it is slow and sometimes fails" complaints.
4. **Move from `kapt` to `KSP`.** Annotation processing via `kapt` is slow and breaks incremental builds. KSP is typically 2–10× faster and is incremental-aware. Migrate Room, Hilt (via KSP support), and Moshi/kotlinx-serialization to KSP.
5. **Restructure the module graph around change frequency and ownership.** The classic heuristic:
   - **`core/` modules** — stable, widely depended on (`core-network`, `core-database`, `core-designsystem`). Change rarely; keep them small and dependency-light.
   - **`feature/` modules** — one per feature, containing *everything* for that feature (UI + ViewModel + its own data slice). These change constantly and should be **leaves** in the dependency graph.
   - The app module depends on features; features depend on core; core depends on nothing app-specific. **No cycles.**
   The key insight: a module that changes often should have **few** dependents, so its recompilation does not cascade. A module that changes rarely can have many dependents.
6. **Use `implementation`, not `api`, everywhere possible.** Every `api` dependency leaks onto the consumer's compile classpath, widening it and slowing every consumer's compilation. `api` should be reserved for types that genuinely appear in a module's public API.
7. **Reduce the module count where it is not buying isolation.** 100 modules for a mid-size app is often over-modularised — each module has configuration and classpath overhead. Group closely-related features into a single module until the split is needed. The right number of modules is the number of **independently-owned, independently-changing** units, not the number of screens.
8. **Enable incremental compilation and check that it is not silently disabled.** Any non-incremental annotation processor, or a Kotlin plugin version issue, turns the whole compile incremental. Watch the build log for "Incremental compilation disabled".
9. **Add a **binary compatibility check** to CI (Metalava / binary-compatibility-validator). This is what makes aggressive refactoring safe: the check fails the build if a public API changed without a deliberate decision. It also makes aggressive internal restructuring safe, because the public contract is protected.
10. **CI hygiene.** Cache `~/.gradle` and the build cache keyed on the lockfile hash and the wrapper version; use a **larger, faster runner**; run the **full build only on `main`**, and run a **compile-only** check per PR. Split the pipeline into fast (compile + unit tests) and slow (instrumentation, lint, release build) stages.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Configuration cache | Removes most configuration-time cost | Some plugins are not compatible; rare "configuration cache problems" |
| Local + remote build cache | Huge reuse across machines and CI | Needs a cache key that is correct; a mis-keyed cache serves stale output |
| KSP over kapt | 2–10× faster processing; incremental-aware | Some libraries do not support it yet |
| Vertical (feature) modules | Parallel team ownership; blast radius contained | More modules = more overhead; cross-feature UI duplication risk |
| Horizontal (layer) modules | Shared code in one place | Every feature change touches shared modules → recompiles everything |
| Fewer, coarser modules | Lower per-module overhead | Larger blast radius; weaker isolation |
| `implementation` over `api` | Narrower classpath; faster compilation | Requires deliberate exposure decisions |
| Pinning all versions | Reproducible; no network lookups | Version bumps need an intentional process |
| Binary compatibility checks | Safe refactoring; enforces the public contract | Some churn when APIs legitimately change |

**How it is validated in production**
- Track **CI wall-clock time** and **local build time** as a first-class metric with a budget, and report it per PR.
- A **build performance dashboard** over time; a regression in CI time triggers a review.
- A weekly "build health" check: configuration time, cache hit rate, and the slowest 10 tasks.

**Talking points / follow-ups**
- *"What is the single biggest win?"* → Usually the **configuration cache** plus removing dynamic versions — before touching a single module boundary.
- *"How do you know a module split is worth it?"* → It is worth it if the module has a **separate owner and a different change cadence**; splitting by screen produces overhead with no isolation.
- *"How do you avoid a big-bang migration?"* → Migrate incrementally: introduce the new boundaries, move one feature at a time, keep the old modules as thin facades until nothing depends on them.
- *"How does modularisation help CI?"* → It lets CI build only the affected modules and reuse cached outputs for the rest; with a correct remote build cache, a one-module change becomes a small build.

---

### 6. Two teams implement different networking and dependency-injection patterns. How would you standardize the architecture without blocking delivery?

**What the interviewer is really asking**
This is a **people and sequencing** question disguised as a technical one. Can you propose an approach that makes the standard the *easy* path rather than the mandated one, and that keeps both teams shipping throughout?

**Key concepts**
- **Adoption over mandate** — make the standard cheaper to use than the alternative
- **Strangler-fig migration** — new code uses the standard, old code is migrated gradually
- **A thin façade / adapter** so old modules still compile against their existing pattern
- **A "golden path"** template project that is copy-paste-ready
- **Lint rules and CI gates** as the enforcement mechanism (not code review)
- **Design Decision Records (ADRs)** and an architecture review group
- **A shared "platform" team** owning the standard and the migrations
- **Profiling before standardising** — maybe the two patterns are not equally bad
- **Versioning the standard** — a published artifact, semver, changelog
- **Migration tooling**: codemods, lint autocorrect, templates in the IDE
- **Ownership and incentives** — the standard must reduce the other teams' work

**Approach / architecture sketch**

```
Phase 1  Platform team ships :core:network + :core:di (published, versioned)
Phase 2  One reference feature migrated → documented as the golden path
Phase 3  CI lint rules make the standard the path of least resistance
Phase 4  Old modules get a facade adapter → migrated one at a time
Phase 5  The old modules are deleted; the standard is the only option
```

1. **Do not start with the rule; start with the diagnosis.** First, understand *why* the two teams diverged. Often one pattern was chosen under a constraint that no longer holds (a different release cadence, a different API gateway, a team that owns its own service). A standard that ignores a real constraint will be rejected. Profile the actual problems: how many duplicated clients, how many inconsistent error paths, how much time lost to bugs that the standard would have prevented.
2. **Ship the standard as a product, not a mandate.** The platform team's job is to make adoption *cheaper* than not adopting:
   - A published Gradle module with a **semver version and a changelog**.
   - **IDE templates** so a new module is created correct by default.
   - **A codemod or lint autocorrect** that rewrites the old pattern to the new one.
   - **A reference implementation** in one real feature — a working example beats a 40-page document.
   - **Migration guides with before/after code**, per pattern.
3. **Make the old path keep working.** While the teams migrate, both patterns must work:
   - Keep the legacy module alive.
   - Provide an **adapter** so a legacy module can be consumed by a new one, and vice versa. The adapter is a thin shim — no new behaviour — and it is explicitly marked temporary.
   - Do not let the two worlds meet in the domain layer: keep the boundary at the network/DI edge.
4. **Use CI gates, not code review, to enforce.** Enforcement that depends on a human reviewing a PR is unreliable and creates friction. Instead:
   - A **lint rule** (custom `lint-check` or Detekt rule) that flags a direct `OkHttpClient()` construction or a manual `@Inject` in a feature module.
   - The rule should start as a **warning** and become an **error** once adoption passes a threshold (e.g. 80%).
   - A **"no new legacy"** rule: new code cannot use the old pattern; existing code is grandfathered. This stops the divergence from growing without forcing a big-bang migration.
5. **Migrate in dependency order, not by team.** Migrate the **leaf** modules first — the ones with the fewest dependents — because migrating them is cheap and each success builds confidence. Migrate the shared modules last, once the features are already on the new pattern. Sequencing by "who is easiest to move" beats "who is most important".
6. **Give the standard a real owner.** A platform team with a dedicated roadmap, a public changelog, and a **Slack channel** for support. A standard without an owner decays into three standards within two quarters.
7. **Make the ADRs the record.** Every architectural decision (why OkHttp + an interceptor chain, why Hilt, why a `Result` type) is written down, with the alternatives considered. New teams read the ADRs and do not relitigate the decision.
8. **Measure adoption as a health metric.** Track: number of modules on the standard, number of direct client constructions, duplicated error-handling paths, and the bugs attributable to inconsistency. Publish it. A standard nobody measures is a standard nobody adopts.
9. **Handle the political dimension explicitly.** The most common failure is that standardisation is perceived as one team asserting technical superiority. Frame it as: *"this removes the burden of maintaining auth, retries and error mapping from every team"* — the standard is a **service to the feature teams**, and they are its customers. Involve a representative from each team in the design and review of the standard; teams adopt standards they helped build far more readily than ones imposed on them.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Strangler-fig, gradual migration | Neither team stops shipping; risk is bounded | Prolonged dual state; temporary adapters accumulate |
| Big-bang cutover | Clean end state; no legacy | High risk; long period where nobody can ship; a bad week is unrecoverable |
| Mandate by policy | Fast | Resisted; ignored; creates workarounds; the standard rots |
| Make it the easy path (templates, codemods) | Adoption happens because it is cheaper | Up-front investment by the platform team |
| CI lint enforcement | Consistent and automatic | False positives erode trust; needs a grandfathering mechanism |
| Enforce by code review | No tooling work | Inconsistent; a human bottleneck |
| Adapter layer | Old and new interoperate | Complexity; risk of the adapter becoming permanent |
| One reference implementation | Concrete and convincing | Only proves it for one kind of feature |
| Platform team owns the standard | Someone is accountable | A platform team can become a ticket queue and a bottleneck |

**How it is validated in production**
- Adoption metrics: % of modules on the standard, number of violations (trending to zero), time-to-adopt for a newly created module.
- Defect metrics: bugs that the standard would have prevented (auth bugs, retry storms, inconsistent error handling) — this is the business case.
- A quarterly review of the ADR set and the standard's changelog.

**Talking points / follow-ups**
- *"How do you get the teams to agree?"* → Do not ask for agreement on the pattern; deliver a working standard and show the concrete cost it removes. Involve them in the design so it is theirs too.
- *"What if a team refuses?"* → Make the standard better than the alternative, provide a codemod, and let the CI gate be the final word. Do not create a special exemption — that is how a standard dies.
- *"How do you avoid the standard becoming a legacy platform of its own?"* → Version it, publish a changelog, keep it small, and delete features from it as aggressively as you add them.
- *"What is the biggest risk in a migration like this?"* → The temporary adapter layer never being deleted, so the "old" pattern stays alive indefinitely and the standardisation never completes.

---

### 7. Your team wants to migrate a large XML-based application to Compose. How would you plan a safe, incremental migration?

**What the interviewer is really asking**
Can you sequence a **multi-quarter migration** so that value ships continuously, and do you know the specific technical hazards of a hybrid XML/Compose app?

**Key concepts**
- **Incremental adoption** — `ComposeView` inside XML, and XML inside Compose via `AndroidView`
- **`ViewCompositionStrategy`** — `DisposeOnViewTreeLifecycleDestroyed` is the correct choice inside a Fragment/RecyclerView item
- `createComposeView` in a Fragment's `onCreateView`
- **View interoperability**: `AndroidView` / `AndroidViewBinding`; the cost of hosting a View inside Compose
- **`RecyclerView` hosting Compose**: `ComposeView` in a ViewHolder — works but **destroys the recycling benefit** if the composition is not disposed properly; Compose in a recycled ViewHolder has real jank/memory issues
- **Theming**: Material 2 → Material 3; `ComposeTheme` mapping to the existing XML theme; colour/typography parity
- **Navigation**: `Navigation Compose` alongside the existing `Navigation Component`; deep-link parity
- **State**: `ViewModel` works in both worlds — the migration seam that avoids rewriting logic
- **View interop performance**: `AndroidView` is not free; measure with Macrobenchmark
- **Testing**: Compose tests vs. Espresso for the hybrid screens
- **Accessibility** parity
- **Bundle/method count** growth (Compose adds methods; `dex` limits)

**Approach / architecture sketch**

```
Phase 0  Foundations: Compose BOM, theme bridge, shared design system, CI/benchmark baseline
Phase 1  Pilot: one low-traffic, self-contained screen
Phase 2  New screens in Compose; old screens untouched
Phase 3  Migrate high-traffic screens one at a time, screen by screen
Phase 4  Compose-only navigation; delete XML infrastructure
```

1. **Phase 0 — foundations, before a single screen.** This is where a migration is won or lost:
   - Pin the **Compose BOM** and decide the **minimum SDK** (Compose requires API 21+, but the real constraint is your user base and the desugaring setup).
   - Build a **theme bridge**: map the existing colours, typography, and shapes to a `ComposeTheme` so that the migrated screens look identical to the old ones. Visual regression tests (Roborazzi/Paparazzi) on the migrated screens keep parity honest.
   - Extract the **design system** into a Compose module that both worlds can use — including a `View` → Compose wrapper for the shared components (a Compose-rendered button hosted in XML, and an `AndroidView` wrapper for XML-only components).
   - Establish **Macrobenchmark** baselines for the screens you are about to migrate, so you can prove the migration did not regress startup, frame timing, or memory.
2. **Phase 1 — pilot one screen.** Pick a screen that is **self-contained, low-traffic, and not business-critical** (a settings sub-screen, a static "About" page). Not the home screen — you want the first failure to be cheap. Its purpose is to prove the **tooling**, the **interoperability**, and the **team's fluency**.
3. **Phase 2 — new screens are Compose-only.** From this point on, every *new* screen is written in Compose. This is the phase that stops the migration from being a permanent "two worlds" tax, and it is where most of the real progress happens — new work is often more than half the codebase in a living app.
4. **Phase 3 — migrate high-traffic screens last.** Migrate the most important screens **after** the pilot, the tooling, and the team are ready. Migrate them one screen at a time, with the ViewModel **unchanged** — the ViewModel is the seam that lets you swap the view without touching the business logic. This is the key architectural insight: **if your XML screens already use ViewModels, the Compose migration is a view-layer change, not an architecture change.**
5. **Handle the two interop hazards explicitly.**
   - **Compose inside XML** (`Fragment` + `ComposeView`): always set `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed`. The default (`DisposeOnDetachedFromWindow`) disposes too eagerly and breaks state restoration across configuration changes; the "always dispose" default can leak. And **never** use `ComposeView` inside a `RecyclerView` item without thinking — the composition must be disposed when the item is recycled, and the standard `ComposeView` in a ViewHolder pattern is a known source of scroll jank and retained compositions. Migrate the whole list to `LazyColumn` rather than incrementally.
   - **XML inside Compose** (`AndroidView`): fine for a leaf (a map view, a video player, a legacy chart). **Avoid `AndroidView` for anything that participates in a scrolling Compose list** — the View is not recycled with the Compose item, which defeats virtualisation.
6. **Keep the ViewModels, migrate the views.** Reuse the existing `ViewModel` classes and their `StateFlow`/`LiveData` state, consumed via `collectAsStateWithLifecycle()`. Do not rewrite the state layer during the view migration — you would be running two risky changes at once.
7. **Migrate navigation carefully.** `Navigation Compose` and the XML `Navigation Component` can coexist, but **deep links, back stack behaviour, and analytics** must be kept identical. A deep link that broke in the migration is a production incident. Keep a **deep-link parity test** as part of the migration checklist for each screen.
8. **Do not migrate screens whose ViewModels are the actual problem.** If a screen has spaghetti state, migrate it *with* a ViewModel rewrite, as a separate piece of work, and say so. Bundling the two makes the regression impossible to attribute.
9. **Watch the bundle and method count.** Compose adds a large number of methods and classes. On a large app, watch the **dex method count limit** (65,536 methods, 64K) and the multidex threshold. Enable R8 for release, and use `minSdk 21+` with desugaring if your API floor is lower.
10. **Measure and report.** Track: screens migrated, screens remaining, crash-free rate per migrated screen, Macrobenchmark deltas on startup and frame timing, bundle size, and the **average time to migrate a screen**. That last number is the metric that tells you whether the migration is a one-off project or a sustainable way of working.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Big-bang rewrite | Clean end state; no hybrid complexity | Extremely high risk; a long freeze on new features; unrecoverable if it goes wrong |
| Incremental, screen by screen | Continuous value; bounded risk; learnings applied | A long hybrid period; two skill sets on the team; duplicated component work |
| New screens in Compose, old ones in XML | Stops the migration from expanding; fastest progress | Two UI stacks coexist indefinitely for a while |
| Reuse the ViewModel, swap the view | The migration is a view-layer change; low risk | Constrains what Compose screens can do until the state layer is also modernised |
| Rewrite the state layer too | Modern result | Two changes at once; regressions cannot be attributed |
| `ComposeView` in XML | Lets you migrate one screen | Requires the correct `ViewCompositionStrategy`; the interop tax |
| `AndroidView` for legacy components | Reuses what exists | Not free performance; cannot be virtualised in a Compose list |
| Compose in `RecyclerView` items | Incremental | Defeats recycling; jank and retained compositions; a trap |
| Migrate the home screen first | Maximum visibility; proves the approach | Maximum risk; a failure is very visible |
| Migrate the home screen last | Cheap early wins; the team is ready | The hardest screen is deferred to the end when attention may have moved on |

**How it is validated in production**
- **Visual regression** tests (Roborazzi/Paparazzi) on every migrated screen, comparing against the XML version.
- **Macrobenchmark** before/after for startup, frame timing, and memory on each migrated screen.
- **Deep-link parity** tests in CI.
- Crash-free rate and ANR rate segmented by screen (Compose vs XML) so a regression is attributable to the migration.
- A dashboard of migration progress and the per-screen time cost.

**Talking points / follow-ups**
- *"What is the hardest part of a Compose migration?"* → Not the Compose code; it is the **interoperability and the theming parity**, and keeping deep links, back stack, and analytics identical.
- *"Can you use Compose in a RecyclerView?"* → Technically yes, but you must manage the composition lifecycle on recycle, and you lose the virtualisation benefit. Migrate the whole list to `LazyColumn`.
- *"When is a big-bang rewrite right?"* → Rarely on a live app. It is defensible only when the app is small, is about to be replaced anyway, or the team can pause feature work for the duration.
- *"How do you keep the app's look consistent?"* → A shared design system with a theme bridge and visual regression tests; the design system is the deliverable that matters most.

---

### 8. How would you design a feature-flag system that supports staged rollouts, emergency disabling, and offline behavior?

**What the interviewer is really asking**
Can you build a flag system where the **emergency disable is genuinely fast and reliable**, the rollout is **statistically sound**, and the app still works with **no network at all**.

**Key concepts**
- Flag taxonomy: **release** flags, **experiment** flags, **operational** (kill) flags, **configuration** flags
- **Server-side vs client-side** flags — and the latency difference in the emergency path
- **Deterministic bucketing** — a hash of `(userId, flagKey)`, never `Random`
- **Local evaluation** — the SDK evaluates locally from a downloaded config; no per-call network round trip
- **Config delivery**: Firebase Remote Config, LaunchDarkly, or a custom config service via CDN + ETag
- **Cached config with a local default** — the app must work offline
- **Fetch on foreground** to reduce the kill-switch latency; the default periodic fetch can be hours
- **Exposure events** for correct cohort analysis
- **Flag types**: boolean, percentage, variant (string), numeric, JSON
- **Expiry / cleanup** — flag debt
- **Audit trail** — who changed the flag, when, and why

**Approach / architecture sketch**

```
Server (control plane)
  └── flag store + rules + audit log
        │  CDN / ETag delivery
        ▼
App (data plane)
  ├── cached config (local)  ← evaluation happens HERE, offline
  ├── local default (safe = OFF)
  ├── kill-switch check (server-side, authoritative, if needed)
  └── exposure events (batched, queued offline)
```

1. **Separate the flag types, because they have different lifecycles and risks.**
   - **Release flags** — a boolean that gates a feature. Removed once at 100%.
   - **Experiment flags** — variants with a percentage split and a stable assignment.
   - **Operational flags** — the kill switch; a separate, higher-privilege namespace that can disable a feature **immediately, regardless of the rollout percentage**.
   - **Configuration flags** — numeric/string values (a timeout, a max file size) with safe defaults.
   Mixing them is how you end up unable to turn a feature off at 2am because the flag is also carrying an experiment's variant assignment.
2. **Evaluate locally, deliver a config.** The app downloads a **config document** (all flags and their rules) and evaluates the rules **on device**. This means: (a) no per-flag network call, (b) the evaluation works **offline** from the cached config, (c) the kill switch takes effect the moment the new config is fetched.
3. **Make the kill switch fast.** Two mechanisms, in order of speed:
   - **Server-side authority** (fastest): the server checks the kill switch on every request and returns a "feature disabled" response, or omits the feature payload. This takes effect on the **next request** — seconds — with no client involvement. Use it wherever the behaviour is server-influenced.
   - **Client-side config** (next): fetch the config **on every foreground transition**, and optionally poll periodically while a risky feature is active. This bounds the latency to seconds-to-minutes for the running app.
   - Never rely on the default periodic fetch (often 12 hours) as your emergency path.
4. **Default to OFF, and cache for offline.** The config's `defaultValue` for a risky feature is `false`. The cached config is stored locally (a file or `DataStore`) so evaluation works with no network. The app must never block waiting for a config fetch.
5. **Deterministic bucketing.** Assign a variant by hashing `(flagKey + ":" + userId)` — stable across sessions, reinstalls and devices, and independent per flag. Never use `Random`, and never use "the Nth request". Store the assignment so the user sees a consistent experience.
6. **Exposure events for correct analysis.** Emit an event when the user actually *sees* the treatment, with the flag key, variant, and the user id. Without exposure data, cohort comparisons are wrong — a common and subtle mistake.
7. **Design the offline behaviour of each flag explicitly.** For each flag, document: "with no network and no cached config, what does the app do?" The answer must be safe. A feature that requires a flag value must have a sane default path, not a disabled app.
8. **Make the flag auditable.** Every change records who, when, the previous value, and a reason. For a kill switch used in an incident, the audit trail is how you learn what happened. Add a **"who turned this off and when"** lookup to the incident runbook.
9. **Lifecycle management.** Each flag has an **owner** and an **expiry date**. The system flags flags that have been at 100% for more than N days ("ready to delete") and flags that have been evaluated by less than X% of users (dead code). Flag count and flag age are reported metrics — flag debt is a leading indicator of future incidents.
10. **Validate before you trust.** A flag system that can turn on a feature that crashes is worse than no flag system. Before enabling a flag for a rollout, the code path should be covered by tests that run with the flag on — **CI runs the suite twice**, once with the flag on and once with it off.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Server-side flags | Fastest kill switch; no client release | Only helps server-driven behaviour; server work required |
| Client-side config evaluated locally | Works offline; no per-flag round trip; fast evaluation | The kill switch needs a config fetch (seconds-to-minutes) |
| Per-flag network calls | Always fresh | Slow; breaks offline; hammers the server |
| Firebase Remote Config | Turnkey; built-in; reasonable latency | The fetch cadence is the weak point; vendor lock-in |
| A custom config service | Full control over delivery and latency | You build and operate it |
| Percentage rollout with stable hashing | Consistent user experience; measurable | Needs exposure telemetry to analyse correctly |
| Random assignment | Trivial | The user sees different variants across sessions — unacceptable |
| Default ON | The feature is available immediately | Dangerous: a broken config enables a risky feature |
| Default OFF | Fails safe | The feature is unavailable if the config service is down |
| Killing a flag by removing it | Simple | Removes the ability to re-enable; no audit; risky mid-incident |

**How it is validated in production**
- Flag evaluation is **unit-tested** with a table of (flag state, user id, network availability) → expected result.
- An **end-to-end test** that flips a kill switch on the server and asserts the client disables the feature within the target window.
- CI runs the suite with the risky flags **on**, so the flag-on path is covered.
- Metrics: flag evaluation failures, config fetch latency and failure rate, per-flag adoption and exposure, and the age/count of live flags.

**Talking points / follow-ups**
- *"What is the kill-switch latency target?"* → Seconds for a server-side flag; under a minute for a client-side flag with a foreground fetch. State a target and measure it.
- *"How do you handle the app being offline when you disable a feature?"* → The client keeps the last known config. If the feature must be disabled *now*, it has to be a server-side decision — that is the argument for server authority.
- *"How do you avoid flags becoming permanent?"* → Owner + expiry date + a report of flags at 100% and of dead code; delete the flag and the branch together.
- *"What if the config service is down?"* → The app uses the cached config and the safe local default (OFF). It must never block or crash.

---

### 9. How would you design observability for a mobile application to connect client-side failures with backend request traces?

**What the interviewer is really asking**
Can you build the **join** between a mobile crash or ANR and the backend's request trace — and do you understand that the client is an unreliable, lossy reporter that must be designed for?

**Key concepts**
- **Correlation IDs** — a `traceId`/`requestId` generated or propagated per request, echoed in the response and in server logs
- **W3C `traceparent`** / OpenTelemetry context propagation
- **Client-side telemetry**: crash reporting (Crashlytics/Bugsnag/Sentry), **ANR** detection, **custom breadcrumbs**, **network logging**
- **Crash reports** carry: app version, build, device, OS, **the last N breadcrumbs**, and the **network log of the failing request**
- **Symbolication** — uploading the R8 `mapping.txt`; without it, stack traces are obfuscated and useless
- **Offline buffering** — telemetry must survive offline and be uploaded later
- **Sampling** — never sample 100% of network logs in production
- **PII scrubbing** — redaction *before* the data leaves the device (Section 8, Q5)
- **Crash-free users / sessions** as the SLO
- **Backend trace IDs in the response header** (`X-Trace-Id`) so the client can quote it in a bug report
- **OpenTelemetry for Android** / a vendor SDK that supports OTLP
- **Custom key-value breadcrumbs** and `setCustomKey`
- **Symbol / build ID** per release, so a crash is attributable to a build

**Approach / architecture sketch**

```
App request ──► OkHttp interceptor
                   ├── adds X-Request-Id (UUID) / traceparent
                   ├── records the request in a bounded in-memory ring buffer
                   └── on response: stores the response's X-Trace-Id

Crash/ANR ──► Crash reporter
                 ├── symbolicated stack trace (via uploaded mapping.txt)
                 ├── last 20 breadcrumbs (including the failed request + its trace id)
                 └── build id, device, OS, session id
                       │
                       ▼  (batched, offline-buffered, PII-scrubbed)
             Telemetry pipeline ──► dashboards / alerting / trace store
```

1. **Generate a correlation ID on the client, and have the server honour it.** Every request carries an `X-Request-Id` (a UUID) or a W3C `traceparent`. The server **echoes it back** in the response header and includes it in its own logs and traces. This is the join key: given a crash report, you have the request ID, and given a request ID, you have the server's full trace — request, downstream calls, and timings.
2. **Capture the network layer's activity as breadcrumbs.** An OkHttp interceptor records, into a **bounded ring buffer** (say the last 50 requests): the URL (path only, no query secrets), the method, the status code, the duration, the request ID, and the outcome. The crash reporter attaches the last N breadcrumbs to the crash. This turns "the app crashed on the checkout screen" into "the app crashed right after `POST /v1/payments` returned 500 with request id abc-123".
3. **Make the crash report carry the trace ID explicitly.** On a crash in a network call, the interceptor's breadcrumb holds the ID; the crash reporter's `setCustomKey("last_request_id", id)` puts it at the top level of the report so a support agent can quote it without digging.
4. **Symbolicate.** Upload the R8 `mapping.txt` for every release to the crash backend. Without it, the stack trace is `a.b.c` and unusable. Make this a **CI step that fails the build** if the mapping file is not uploaded — a silent failure here quietly destroys your entire crash-investigation capability.
5. **Attach the build identity.** Set a unique `build id` per release (a UUID in `BuildConfig`, plus the version code and version name) and put it in every crash report and every telemetry event. Without it you cannot answer "is this crash new in this release?" — and you cannot stage a rollout safely (see Section 10, Q9).
6. **Instrument the critical journeys, not just crashes.** Crashes are the loudest signal; the quiet ones matter more. Add **custom events and traces** for the business-critical funnel steps (payment initiated, sync started, upload checkpointed), with the trace ID attached. When a metric moves, you have the trace.
7. **Design for an unreliable reporter.** The client may be offline, may be killed, may have no battery. Therefore:
   - **Buffer** events on disk and upload them in batches.
   - **Bound** the buffer (size and age) so a device that has been offline for a week does not upload 50MB.
   - **Sample**: keep 100% of crashes and ANRs, but sample successful network logs (e.g. 1%). Keep 100% of *failures*.
   - **Never block the app** on telemetry upload — it is not on the user's critical path.
8. **Scrub PII before it leaves the device.** Redact tokens, cookies, card numbers, names, and free-text fields **in the client**, before the payload is queued. Server-side scrubbing is a second layer, but the client-side control is the one that actually works, because by the time data reaches the server it has already left the device.
9. **Instrument ANRs and non-fatal exceptions.** ANRs are invisible to crash reporters by default. Use `ApplicationExitInfo` (API 30+) to detect ANRs and low-memory kills after the fact, and add a watchdog for main-thread stalls. Report them with the same breadcrumbs.
10. **Close the loop with the backend.** Agree with the backend team on:
    - A shared header contract (`X-Request-Id` in, `X-Trace-Id` out).
    - A **trace store** the mobile team can query (Jaeger/Tempo/Honeycomb) — with the mobile `traceId` as a searchable field.
    - A **runbook**: "Given a crash ID, how do I get the server trace?" — ideally a single query.
11. **Define the SLO and alert on it.** Crash-free users and crash-free sessions, per version and per device cohort, are the headline metrics. Alert on a **version-over-version delta**, not an absolute threshold — a crash rate that is stable but high should not page you every night.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Client-generated request ID | The client controls the ID; no server setup needed | Only correlates requests the client made; no parent/child span structure |
| W3C `traceparent` / OpenTelemetry | Standard; full trace topology; works with the existing tracing backend | Requires OTel instrumentation; a bigger change |
| Server-generated ID | The server owns the identity | The client cannot know it until the response — useless if the request fails |
| Breadcrumbs on crash | Rich, immediately actionable context | Bounded memory; can leak sensitive data if not scrubbed |
| Full network logging | Complete | Costly; privacy risk; must be sampled and redacted |
| Sampling successful requests | Cost and privacy controlled | You lose the context of a *successful* call that preceded the failure — keep 100% of failures |
| Uploading in batches | Efficient; works offline | Reports arrive late; an immediate crash may be reported only after the next launch |
| On-device scrubbing | The control that actually works | More code in the client |
| Server-side scrubbing only | Centralised | Data already left the device; also a compliance problem |
| ANR detection via `ApplicationExitInfo` | Post-mortem, no overhead | Only visible after the app restarts |

**How it is validated in production**
- A **contract test** against the backend: the client sends `X-Request-Id`, the server echoes it and the trace is findable by that ID.
- A **chaos drill**: deliberately fail a request, trigger a crash, and verify the crash report contains the request ID and the server trace is findable.
- A CI gate that fails the build if the `mapping.txt` for the release was not uploaded.
- Monitoring of the telemetry pipeline itself: upload success rate, event drop rate, and the "crash report contains a request ID" percentage.

**Talking points / follow-ups**
- *"How do you correlate a crash with a backend trace?"* → The client generates a request ID, the server echoes it, and the crash report's breadcrumbs carry it. The ID is the join key on both sides.
- *"What if the app crashes before the request completes?"* → The request breadcrumb is written **before** the call and updated with the outcome afterwards, so the in-flight request is still visible with its ID.
- *"How do you keep crash reports free of PII?"* → Redact on the device before the payload is queued, and audit the redaction rules.
- *"How do you know the telemetry itself is working?"* → Monitor the pipeline: upload success rate, drop rate, and the fraction of crash reports that contain a request ID.
- *"What about offline users?"* → Buffer and batch; the reports arrive later. Bound the buffer so a long-offline device does not flood the backend.

---

### 10. A critical production bug affects only 2% of users. How would you prioritize investigation, mitigate impact, and coordinate with backend and QA teams?

**What the interviewer is really asking**
Do you can triage a **partial-impact incident** — where the signal is diluted — and can you run a **cross-team incident response** without organisational friction?

**Key concepts**
- **Incident severity vs. affected-user count** — 2% of a large base can still be a large number of users
- **Segmentation** — crash-free rate by version, device model, OS version, region, carrier, account cohort, A/B variant
- **The "2%" is a clue** — 2% suggests a **specific cohort**, not randomness. A specific OS version, a specific device, a specific data value, a specific locale, a specific payment method
- **Kill switch / feature flag** as the primary mitigation (Section 10, Q9)
- **The unknown third state** — Section 1's central insight, applied to a buggy write path
- **Cross-functional roles**: incident commander, communications, operations, scribe — and the **"who is the IC"** question when the bug spans client and server
- **Blameless postmortem**
- **Fix-forward vs rollback**: rollback may be blocked by a non-backward-compatible data change
- **Regression test from the production case**

**Approach / architecture sketch**

```
T+0     Detect (alert on version-over-version crash delta)
T+5     Triage: segment by version/device/OS/data → find the 2% cohort
T+15    Mitigate: kill switch / server-side disable  (or hotfix)
T+60    Fix forward: ship the hotfix to the affected cohort first
T+24h   Verify the metric recovers; close the incident
T+1w    Blameless postmortem; regression test; fix the detection gap
```

1. **Quantify the blast radius honestly.** "2% of users" is only alarming if you know the denominator. 2% of 10 million MAU is 200,000 users. Lead with the absolute number, not the percentage. Then check whether it is 2% of *users*, 2% of *sessions*, or 2% of *crashes*.
2. **Segment immediately — the 2% is a clue, not a statistic.** A crash affecting a clean 2% is almost always a **cohort**, and the cohort is the diagnosis. Slice by: **app version and build**, **OS version**, **device model/OEM**, **screen/feature**, **region and locale**, **carrier**, **network type**, **account/plan tier**, and **experiment variant**. The intersection that reproduces 2% is usually found within the first hour.
   - If it correlates with a **device or OS version** → likely a platform behaviour change (see Section 7, Q3).
   - If it correlates with a **data value** (a long name, a specific currency, a malformed record) → a parsing/encoding bug.
   - If it correlates with a **variant or flag** → the new feature is the culprit; kill switch.
   - If it correlates with **version** only → a regression in the new release.
3. **Mitigate before you diagnose fully.** Do not wait for the root cause to stop the bleeding. The mitigation ladder, fastest first:
   - **Server-side disable** of the offending behaviour (fastest — seconds).
   - **Kill switch** on a client feature (seconds-to-minutes).
   - **Targeted rollout exclusion**: ship a build that skips the affected path for the affected cohort.
   - **Remote config** to relax the failing input (e.g. disable the new format, raise a limit).
   - **Hotfix release** (hours to a day).
   - **Rollback** — usually blocked, and often the wrong instinct: it removes the fix for the *other* 98% of users too, and it does not remove the bad version from devices anyway.
4. **Decide fix-forward vs rollback with the data in mind.** If the buggy version wrote data that the previous version cannot read, a rollback is impossible by definition — you must fix forward. If the bug is purely in the new code and no bad data exists, a rollback is defensible. Say this explicitly rather than defaulting to "roll back".
5. **Run the incident formally, even though it is 2%.** Assign an **incident commander** (not the person who is also debugging — that is the classic mistake), a scribe, and a communications owner. Set a cadence. Even a small incident deserves the structure, because the *process* is what scales when it is 20% and a customer is watching.
6. **Coordinate across teams with a clear contract.** With the backend team: does the crash correlate with a specific server response, a specific API version, or a specific field the server started sending? Ask for the server's error rate for the same window and the same cohort — a client crash often has a server-side correlate (e.g. the server started returning a null where it used to return a value). With QA: give them the **exact reproduction steps, the device/OS matrix, and the build id**, and turn the production case into a regression test. With support: give them a script and the request ID format.
7. **Communicate outward only when you have something to say.** If users are affected, a status note and support guidance. If it is a 2% edge case with no data loss, do not over-communicate — but do record it.
8. **Close the loop and prevent recurrence.**
   - A **regression test** built from the exact production data shape.
   - A **detection fix**: the alert did not catch this — add the metric/cohort segmentation that would have.
   - A **postmortem** with concrete action items and owners; the blameless rule keeps the reporting honest.
   - If the bug slipped through a staged rollout, review the **rollout gate metrics** (Section 10, Q9) — the guardrail metric was probably not measuring the right thing.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Kill switch / server-side disable | Fastest; targeted; no release | Only works if the failure is behind a switchable path |
| Hotfix | Actually fixes it | Hours to a day; needs review and testing |
| Rollback | Instant fix for the code | Does not remove the bad build from devices; may lose unrelated fixes; often impossible with a data change |
| Fix forward | Preserves the other 98% of improvements | Requires understanding the bug well enough to write a safe fix |
| Ship to the affected cohort first | Limits exposure | More complex release management |
| Pause the rollout | Stops new exposure | Does not help already-affected users |
| Formal incident process | Scales; clear ownership | Overhead for a small incident — keep it lightweight but present |
| Blameless postmortem | Honest; finds systemic causes | Requires a culture that tolerates it |

**How it is validated in production**
- The regression test reproduces the exact cohort in CI.
- The cohort's crash-free rate returns to baseline within the expected time of the mitigation, and the fix is verified by **staged rollout** of the hotfix.
- A postmortem action-item tracker with owners and dates; a review of the "why did we not detect this sooner" question.

**Talking points / follow-ups**
- *"Why does the 2% matter?"* → Because it points at a **cohort**, and finding the cohort is most of the diagnosis. 2% is a signature, not noise.
- *"Who is the incident commander when the bug is half client and half server?"* → The person who owns the **customer impact** — usually the client lead, with the backend lead as a technical partner — and it is stated explicitly at the start so there is no ambiguity.
- *"Roll back or fix forward?"* → Ask whether the buggy build wrote data the previous build cannot read. If yes, fix forward is the only option. And remember a Play rollback does not remove the bad build from devices.
- *"What would you have done differently?"* → Ideally the release would have been behind a kill switch with staged rollout and an automated crash gate — which is a design decision, not a testing decision.

---

### 11. Your team must reduce crash rate and improve app startup performance within one quarter. How would you establish measurable goals and prioritize work?

**What the interviewer is really asking**
Can you run a **quarter-long technical-debt-reduction programme** — with baselines, targets, prioritisation by impact, and a delivery cadence — rather than a vague "let's fix the crashes" effort?

**Key concepts**
- **Baseline first** — crash-free users, crash-free sessions, ANR rate, cold-start TTID, by version and device cohort
- **Segment everything** — a 0.5% overall crash rate can be 5% on one OEM
- **The crash Pareto** — the top 10 crash signatures are usually 70–80% of crashes
- **Targets that are measurable and dated**: "crash-free users 99.2% → 99.7% within 12 weeks", "cold-start TTID p90 < 900ms on the low-end device class"
- **Impact × effort** prioritisation; the **Pareto of crashes** beats the Pareto of code
- **Startup**: `ContentProvider` initialisation before `Application.onCreate`, main-thread I/O, class loading, baseline profiles, R8
- **Weekly cadence** and visible progress (a dashboard in the team channel)
- **Non-negotiables** that protect the metrics: crash gates on the release pipeline, static analysis, a `StrictMode` gate in CI
- **Sustaining the gain** — the problem recurs without a gate
- **Trade-offs**: fixing crashes vs. new features

**Approach / architecture sketch**

```
Week 1-2   Baseline + segmentation + Pareto
Week 3-8   Fix the Pareto: top crash signatures, then the startup hot path
Week 9-12  Gates, guards, and the dashboard; final measurement
```

1. **Week 1 — establish the baseline and the segmentation.** Without a baseline, "we improved" is unfalsifiable. Measure and publish:
   - Crash-free users and crash-free sessions, overall and **per version**.
   - ANR rate.
   - **Crash signatures ranked by frequency** — the Pareto.
   - Cold-start **TTID and TTFD**, p50 and p90, **per device class**.
   - All of it **segmented by device model, OEM, and OS version**. This is where the value is: an overall 0.4% crash rate is usually one OEM at 4%.
2. **Set measurable, dated targets.** A good target is specific, segmented, and time-bound:
   - "Crash-free users ≥ 99.7% (from 99.1%) by end of quarter, on the top-5 device cohorts."
   - "Cold-start TTID p90 ≤ 900 ms on the low-end device class (from 1.6 s)."
   - "ANR rate ≤ 0.15% (from 0.4%)."
   Avoid a target of "reduce crashes" — it cannot be verified, and it cannot be traded against a feature.
3. **Prioritise by the Pareto, not by the codebase.** The top 5–10 crash signatures typically account for the majority of crashes. Fixing them takes days and moves the metric dramatically; a broad "clean up the codebase" effort takes the quarter and moves it a little. For startup, the equivalent Pareto is the **startup trace** — the top few main-thread blocks that dominate the cold start.
4. **Segment first, then fix.** Build a matrix of crash signature × device cohort × version. The wins are almost always concentrated: one OEM's WebView, one OS version's permission flow, one Android 14 behaviour change (Section 7, Q3), one screen with a fragile third-party SDK. Each of those is a **targeted** fix — sometimes a one-line guard, sometimes a targeted rollout exclusion.
5. **Plan the work in weekly slices with a visible dashboard.** A quarter is 13 weeks; a quarter-long "fix everything" project delivers nothing until the end. Instead, ship a fix every week, and publish the metric movement in the team channel each Friday. Visibility creates momentum and makes a missed week obvious immediately.
6. **Attack startup with the known causes, in order:**
   - **Third-party `ContentProvider` initialisation** — runs before `Application.onCreate`, so it is invisible in your code. Remove it (`tools:node="remove"`), or make the SDK lazy.
   - **Main-thread I/O and `StrictMode` violations** — enable `StrictMode` in debug builds and fix every violation.
   - **Class loading and DEX verification** — ship a **baseline profile**; enable **R8** shrinking.
   - **Heavy `Application.onCreate`** — initialise only what the first frame needs; defer the rest to after the first frame.
   - **Layout inflation** — flatten the hierarchy; move to Compose for the hot screens.
   - **Verify with Macrobenchmark** on a pinned low-end device, in CI, as a gate.
7. **Do not let the release pipeline erode the gains.** A quarter of fixes decays if the next release reintroduces the same patterns. Add **durable guards**:
   - A **crash-rate gate** on the release pipeline (a build cannot be promoted if the crash-free rate regresses).
   - A **startup budget check** in Macrobenchmark, failing the build on a regression.
   - **Static analysis / lint rules** for the patterns that caused the crashes (e.g. a rule against main-thread disk access).
   - A **"new crash must be triaged within 48 hours"** rule.
8. **Protect the delivery capacity.** Reserve roughly 20–30% of the team's capacity for this work and make the trade-off **explicit** with the PM: "this quarter we are spending 25% of capacity on stability, which means these three features move to next quarter." A quality programme without an explicit capacity trade-off gets silently deprioritised.
9. **Report honestly, including what you did not fix.** Publish the Pareto of what remains and why. Staff-level credibility comes from an accurate picture, not from a green dashboard.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Fix the crash Pareto | Maximum metric movement per unit of effort | Leaves a long tail of low-frequency crashes |
| Broad stabilisation | Reduces future crashes across the board | Diffuse; slow to show results; risks not shipping anything |
| Target one OEM/cohort | Huge win on that cohort | May not move the overall number much if the cohort is small |
| Exclude the affected cohort via rollout | Immediate mitigation | Not a fix; the bug remains in the codebase |
| Startup: baseline profile + R8 | Largest startup win for the least code | Requires a device to collect the profile; must be regenerated |
| Startup: rewrite the startup path | Durable improvement | Takes weeks; risk of regressing startup |
| Per-week shipping | Visible progress; momentum | Requires a stable pipeline and small, safe changes |
| Big-bang stabilisation branch | One merge, one test cycle | Long unreviewed branch; merge conflicts; no incremental value |
| No capacity reservation | Feature delivery uninterrupted | The work gets squeezed and never finishes |

**How it is validated in production**
- The baseline dashboard is the validation instrument: the same segmentation, re-measured weekly.
- Targets are checked against the **same metric definitions and cohorts** as the baseline — a redefinition mid-quarter invalidates the comparison.
- Post-quarter: the crash signatures that were fixed are confirmed absent in production, and the new gates are verified by deliberately introducing a regression in a test build.

**Talking points / follow-ups**
- *"How do you prioritise between a 2% crash affecting payments and a 10% crash on a settings screen?"* → Segment the **user impact**: payments affect a critical, money-touching flow and carry a trust cost; a settings screen crash may be self-recovering. Fix by impact and by effort, not by frequency alone.
- *"How do you stop this regressing?"* → Gates: a crash-rate gate on the pipeline, a startup budget in Macrobenchmark, and lint rules for the patterns that caused the crashes.
- *"How do you handle the PM?"* → Make the trade-off explicit and quantified: capacity reserved, features deferred, named.
- *"What if the top crash cannot be fixed in the quarter?"* → Say so in week 2, not week 12. Then either ship a mitigation (kill switch, cohort exclusion) or renegotiate the target.

---

### 12. A product manager requests a major feature halfway through a sprint, while the team is already committed to a critical release. How would you handle the trade-off?

**What the interviewer is really asking**
Can you make a **trade-off decision and communicate it** — with the technical reasoning the PM does not have — rather than either blindly accepting or reflexively refusing?

**Key concepts**
- **Sprint commitment vs. new request** — the definition of "committed" in Scrum
- **Cost of context switching** — the cost of an unfinished task (the "sunk" work and the re-orientation cost)
- **The critical release is not movable** — a regulatory deadline, a partner integration, a security fix, a marketing commitment
- **Options: accept, defer, partially deliver, spike, negotiate scope**
- **A time-boxed spike** to reduce the uncertainty before committing
- **De-risk the commitment**: reduce the scope of the critical release, or cut a lower-priority item from it
- **Technical feasibility and hidden cost** — the estimate the PM does not see
- **Communicating in business terms** — customer/revenue/risk impact, not engineering hours
- **Escalation** — when the decision is above your level, escalate with options, not a complaint
- **Written decision record** — so the outcome is not re-litigated

**Approach / architecture sketch**

```
1. Understand what the PM actually needs      → is it the feature, or the outcome?
2. Quantify the true cost of the request       → engineering + risk + release risk
3. Enumerate the options with their costs      → accept / defer / partial / spike / cut scope
4. Recommend one, with the trade-off made explicit
5. Get a decision, in writing, from the decision-maker
```

1. **Understand what the PM actually needs.** A PM request is a *problem statement* wrapped as a solution. "Add a referral programme" might really be "we need more installs in Q4". Ask: *what outcome are you trying to achieve, and by when?* Often a smaller change (a landing page, a deep link, an email) achieves the same outcome at a fraction of the cost. This is the highest-leverage question and it is often skipped.
2. **Make the trade-off explicit and quantified.** Present three numbers: the cost of the new feature, the cost of the delay to the critical release, and the risk of doing both. The technical reality the PM usually does not see:
   - The **cost of context-switching**: an interrupted task leaves partially-done work, and the re-orientation cost is real (often 20–50% of the original estimate).
   - **Hidden cost**: a "major feature" is rarely one feature — it is UI, API, analytics, error handling, offline behaviour, edge cases, testing, and rollout. The estimate that matters is the *full* one.
   - **Release risk**: adding a major feature to a release that is already committed multiplies the risk of *both*.
3. **Enumerate the options with costs — do not present a binary.** Real options:
   - **Accept and swap**: drop an item from the critical release to make room. The best answer when the critical release has a movable item.
   - **Defer to the next sprint**: state the new feature's target date explicitly.
   - **Deliver a thin slice**: ship the smallest version that produces the outcome (e.g. the referral *link* and tracking, without the reward logic), and schedule the rest.
   - **Spike**: a time-boxed (2–5 day) investigation to reduce uncertainty, then re-estimate — for requests whose feasibility is genuinely unknown.
   - **Push to the server**: if the feature can be delivered largely backend-side, the client cost may be small enough to fit.
   - **Say no with a reason and an alternative**: sometimes the right answer. "No" is a legitimate outcome when the release risk or the technical cost is unacceptable.
4. **Protect the critical release.** If the release date is truly immovable (regulatory, contractual, security), say so early and frame the request in those terms — a deadline is not a matter of engineering preference. Also check whether the *release* is really immovable or only the *date* is: the scope of a release is always negotiable.
5. **Give a recommendation, not just options.** A staff engineer presents an analysis and a recommendation: *"My recommendation is to swap item 3 (the low-priority reporting screen) for the referral spike; here is why, here is the cost, here is the risk. I need a decision by Thursday so we can still make the release."* Escalate **with options** if the decision is not yours.
6. **Do the arithmetic on the deadline.** If the feature genuinely must be in this release, work backwards: what has to be true? This often surfaces that the feature needs a backend that is not ready, or a design that is not final — neither of which is an engineering-scheduling problem. Naming the true blocker is more useful than refusing.
7. **Write down the decision.** A short decision note — what was requested, what was decided, the trade-off, the risks accepted, and the date — prevents the request from being re-raised and creates a record for the postmortem. Send it to the PM and the team.
8. **Protect the team from thrash.** Whatever is decided, the team needs a stable plan. Once the decision is made, execute it without re-litigating, and communicate the new plan clearly.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Accept the new request now | Delivers on the PM's ask immediately | Endangers the critical release; context switching; both may fail |
| Swap: cut something from the release | Makes room without moving the date | Something valuable still gets cut |
| Defer to the next sprint | Protects the committed release | The PM's need is unmet; the risk is they escalate |
| Thin slice now | Partial value; keeps the date | The thin slice may not produce the outcome; technical debt |
| Time-boxed spike | Cheap uncertainty reduction | Delays a decision by a few days; may not be needed |
| Push to the server | Possibly the cheapest path | May not be possible; still needs client UI |
| Decline with an alternative | Protects quality and trust | Requires influence; the request may resurface |
| Escalate | Surfaces it to the decision-maker | Can look like passing responsibility upward if done without options |

**How it is validated in production**
- The release lands on time with the agreed scope, and the release's crash-free rate and startup metrics are unchanged — i.e. the risk was actually managed, not just the date met.
- The follow-up feature ships on the agreed date (or a new date is agreed and recorded).

**Talking points / follow-ups**
- *"Why not just say yes?"* → Because a half-finished feature plus an unstable release is the worst outcome for everyone, and the cost of context switching is real.
- *"What if the PM insists?"* → Escalate with options and a recommendation; make the risk explicit in writing; let the decision-maker decide with full information.
- *"How do you say no to a PM?"* → With an alternative and a date: *"I cannot get this into the 15th release without putting the release at risk. Here is a thin slice we can ship on the 15th, and here is the full feature on the 30th."*
- *"What if the deadline is genuinely immovable?"* → Then the conversation is about **scope**, not timing — and the technical feasibility of the new feature becomes the critical question, which is where the spike comes in.

---

### 13. How would you introduce code reviews, automated testing, and architecture standards when you do not have direct authority over every developer?

**What the interviewer is really asking**
Can you create **engineering standards without positional authority** — by making the right thing the easy thing, and by leading with credibility rather than instruction?

**Key concepts**
- **Influence without authority** — the classic staff-engineer problem
- **Make it the path of least resistance**: templates, codemods, lint autocorrect, a good starter project
- **Automation over exhortation** — a CI gate beats a wiki page
- **Start with "yes, and"**: contribute to their code, fix their bugs, build trust first
- **The "practise what you preach" effect** — your own PRs are the reference implementation
- **Buy-in via shared pain** — find the bug class that costs everyone (a flaky CI, a 3am rollback) and let the standard be the answer
- **Small, incremental, opt-in first** — prove the value, then make it default
- **A "champion" in each team** rather than a central authority
- **Ratcheting** — enforce only for new code, grandfather existing code
- **Documentation that lives next to the code** (ADRs in the repo, module-level READMEs) — not in a wiki nobody opens
- **Automation of the boring parts**: a template repository, a lint rule set, a CI workflow that runs on every PR
- **Social proof** — publish the metrics that the standard improved (defect rate, review turnaround)

**Approach / architecture sketch**

```
Phase 1  Listen + fix: solve people's real problems, build trust
Phase 2  Opt-in: templates, codemods, examples; no enforcement
Phase 3  Default: new modules/projects get the standard automatically
Phase 4  Ratchet: CI gates apply to new/changed code only
Phase 5  Systematise: champions per team, ADRs, metrics
```

1. **Phase 1 — build credibility by giving before asking.** Before asking anyone to change how they work, solve problems for them: fix their flaky test, unblock their stuck CI, write the docs they keep asking for, review their PRs quickly and usefully. Influence is accumulated goodwill. A standard introduced by someone who has only ever demanded things gets ignored; the same standard from someone who has been fixing their build for a month gets adopted.
2. **Find the shared pain and let the standard be the answer.** Standards that address a felt problem spread; standards that exist because a style guide says so do not. The most fertile ground: flaky tests (a standard of hermetic tests + a fixed test harness), flaky releases (a standard of staged rollout + a crash gate), or slow builds (a standard of module boundaries + the configuration cache). Pick the one where your teams already complain, and make the standard the fix.
3. **Phase 2 — make the standard opt-in and easy.** Ship:
   - A **project template** / IDE template so a new module is correct by default.
   - **Codemods and lint autocorrect** that rewrite the old pattern to the new one in one command.
   - **A reference implementation** — a real screen, a real module, in the codebase, written the standard way.
   - **A short, practical guide** (one page per rule, with the *why*) in the repository, not in a wiki.
   Adopt it yourself first. Your PRs become the reference, and reviewers can point at them.
4. **Phase 3 — make it the default.** Standards spread by being the path of least resistance, not by being mandatory. When starting a new module or screen, the template produces the standard-compliant code. Nobody has to remember; nobody has to be convinced. This is where most real adoption happens.
5. **Phase 4 — ratchet, do not revolution.** Enforcement should **apply to new and changed code only**, with the existing code grandfathered. This is the crucial mechanic: it removes the "you are asking me to rewrite everything" objection, it produces a steady ratchet instead of a stalled migration, and it never blocks a release. Announce it clearly: *"from Monday, changed files must pass these checks; existing files are grandfathered."*
6. **Make the gates automated and non-negotiable but never surprising.** A lint rule that fails a build for something you did not warn about is corrosive. Every rule needs: a **clear message explaining the rule and the fix**, an **autocorrect** where possible, and an **escape hatch** (a `// lint-disable` with a justification, reviewed like code). A rule that is frequently wrong gets disabled wholesale — so tune it and delete the rules that misfire.
7. **Make code review work as a mechanism, not a gate.** If every PR needs a senior reviewer, the senior reviewer becomes the bottleneck and the queue becomes the reason people bypass it. Instead:
   - Define **ownership by area** so reviews go to the right person, not the most senior person available.
   - Use **risk-based review**: a small PR gets one review; a change to a critical module (auth, payments, the sync engine) gets two and an owner.
   - Automate what can be automated (formatting, lint, tests, static analysis) so human review is spent on **design and correctness**.
   - Set a **response-time expectation** (e.g. one working day) and honour it — the fastest way to make people dislike reviews is slow reviews.
   - Use **pairing and a "design review" ritual** for the big things *before* the code is written, which is where a staff engineer's leverage actually is.
8. **Phase 5 — distribute ownership.** A central standards body does not scale and creates a queue. Instead:
   - A **champion in each team** who keeps the standard alive locally and is consulted on changes.
   - **ADRs** for decisions, so the reasoning is captured once and referenced forever.
   - A **regular forum** (a short, recurring architecture review) where proposals are discussed — the forum is where consensus is built, and it is far cheaper than mandates.
9. **Measure and publish.** Adoption and outcome metrics, not compliance theatre: % of new modules using the template, CI failure rate by rule (a rule that fires on 30% of PRs is wrong), review turnaround time, defect rate by area, and the **postmortem metric** the standard was meant to improve. If the numbers do not move, either the standard is wrong or the problem was never the standard — and you should be honest about which.
10. **Accept that it will never be 100%.** Standards erode as the codebase and the team change. The goal is not compliance; it is that the *default* is right and the *escape hatches* are visible. A codebase where 80% of new code follows the standard and the rest is a documented exception is a success, not a failure.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Mandate the standard | Fast, uniform | Resisted; bypassed; erodes trust; people comply superficially |
| Automate the gate in CI | Consistent; no human bottleneck | Rules misfire and annoy; needs tuning |
| Ratchet (new code only) | No big-bang; steady progress | Old violations persist; some files never get touched |
| Big-bang cleanup | Clean end state | Long freeze; huge PR; high risk; blocks everyone |
| Opt-in templates/codemods | Adoption by convenience | Slow to reach full coverage |
| Education and documentation | Builds understanding | Does not change behaviour on its own |
| Pairing and design reviews | Spreads judgement, not just rules | Time-consuming; does not scale linearly |
| Champions per team | Local ownership; scales | Needs champions who have time and influence |
| A central standards team | Consistency; authority | A queue; a bottleneck; creates an us-vs-them dynamic |
| Publishing metrics | Honest; shows value | Can turn into compliance theatre if the wrong metric is chosen |

**How it is validated in production**
- Adoption metrics (template usage, % new code on the standard, lint failure rate by rule).
- **Outcome** metrics: escaped defects by area, mean time to detect, review turnaround, and the postmortem categories the standard was meant to prevent.
- A **developer-experience pulse**: "do you feel this standard helps you?" — the leading indicator of sustained adoption.

**Talking points / follow-ups**
- *"Why not just make it a rule?"* → Because without trust, rules are bypassed; and rules without a path of least resistance make people work around the system. Automation plus convenience beats instruction.
- *"How do you handle a team that resists?"* → Start by fixing something for them, find a champion, and make the standard optional but attractive. Escalate only for the few genuine blockers (a CI gate that must not be bypassed).
- *"What if a rule causes constant false positives?"* → Delete or fix it. A rule that misfires gets disabled and takes the credibility of every other rule with it.
- *"How do you make code review not a bottleneck?"* → Automate the mechanical checks, review by area ownership rather than by seniority, and set a response-time expectation you keep.

---

### 14. A monolithic Android application is becoming difficult to maintain as multiple teams contribute. How would you plan modularization?

**What the interviewer is really asking**
Can you decompose a monolith for **organisational scalability** — with an incremental, always-buildable migration path — rather than producing a beautiful diagram that lands in a repository nobody uses.

**Key concepts**
- **Vertical slicing by feature**, not horizontal slicing by layer
- **The dependency rule**: modules must form a **DAG**; no cycles; the app module depends on features; features depend on core
- **`api` vs `implementation`** — the enforced boundary
- **Module-level encapsulation** — hide internals with `internal` visibility
- **Feature modules own their whole slice** — UI, ViewModel, data, navigation, and their tests
- **Core modules** — stable, shared, slow-changing (`core-network`, `core-database`, `core-designsystem`, `core-analytics`)
- **Do not over-modularise** — a module per screen is overhead without isolation
- **Gradle `includeBuild` / composite builds** — for genuinely independent teams
- **Build performance** as a first-class constraint (see Q5)
- **Incremental migration with an always-green main branch**
- **Ownership**: one team per module; a CODEOWNERS file
- **Binary compatibility** — a module boundary means other teams depend on your API

**Approach / architecture sketch**

```
:app
  ├── :feature:home        :feature:checkout     :feature:settings
  ├── :feature:onboarding  :feature:profile      ...
  └── :core
        ├── :core:network   :core:database   :core:designsystem
        ├── :core:analytics :core:common    :core:testing
```

1. **Slice by feature (vertically), not by layer.** The mistake is splitting into `:ui`, `:network`, `:data` and having every feature module pull from all three — a change to `:network` recompiles everything and every team is blocked. The vertical slice means `:feature:checkout` contains its screens, its ViewModels, its data access, and its navigation, and depends only on `:core:*`. The dependency graph becomes shallow and wide, and a change to one feature affects one module.
2. **Design the module boundary around *change*, not around *file type*.** A good module boundary is one where the things inside change together and are owned by one team. If two teams always change the same files, that is one module, not two.
3. **Make the DAG explicit and enforced.** `:app` → `:feature:*` → `:core:*`, and `:core:*` never depends on `:feature:*`. Enforce it with a **Gradle dependency-analysis plugin** or an architecture test that asserts the module graph matches the intended shape. A boundary that is not machine-enforced is a boundary that erodes.
4. **Control the API surface of every module.** Use `implementation` for everything that is not part of the module's public contract, and mark the public surface explicitly. A module that leaks its internals is not a boundary, and its consumers keep coupling to it.
5. **Define the core modules narrowly.** `core-network`, `core-database`, `core-designsystem`, `core-analytics`, `core-testing` — each with a clear, small, stable API. `core-testing` (fakes, test rules, in-memory Room, MockWebServer helpers) is the one teams forget, and it is the one that most improves test adoption across teams.
6. **Migrate incrementally, always green.** The sequence that works:
   - **Extract, don't rewrite.** Move an existing feature into a module *unchanged*, then change its dependencies. The first step of a modularisation is a pure code move, so a review can verify "nothing changed but the location".
   - **Move one feature per week**, with the CI build green after each.
   - Introduce the new structure alongside the old; the app module assembles both.
   - Delete the old code as each slice lands.
   The rule: **every commit builds, tests pass, and the app runs**. No long-lived branches, no "big bang" merge week.
7. **Fix the build performance as you go.** Each new module adds configuration overhead. Measure the build time after every few extractions and compensate with the configuration cache, the build cache, and `implementation` hygiene. If the build gets slower with every module, the migration will be abandoned.
8. **Establish ownership explicitly.** A `CODEOWNERS` file plus a team-to-module mapping, and a rule that a module is only changed by its owning team (or with that team's review). Cross-module changes become a negotiation — which is the point.
9. **Protect the boundary with a compatibility check.** Use **Metalava / binary-compatibility-validator** per module: changing a public API without an explicit decision fails the build. This is what lets you refactor confidently and tells consumers what changed.
10. **Keep the boundaries from becoming a maze.** Define a small number of stable `core` modules and resist the "just one more module" pressure. A deep module graph with 100 modules is worse than 15 well-chosen ones. Re-evaluate the boundaries as the organisation changes.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Vertical (feature) modules | Parallel ownership; shallow graph; small blast radius | Duplicated setup per feature; shared UI must live in `core` |
| Horizontal (layer) modules | Shared code in one place | Every change ripples through everything; teams blocked on each other |
| Big-bang modularisation | Clean end state; one migration | Long freeze; huge PR; high risk; the app is broken for weeks |
| Incremental, extract-and-move | Always green; reviewable; reversible | Slower to finish; temporary duplication |
| Rewrite during modularisation | Modern result | Two changes at once; regressions unattributable |
| Enforced DAG via tooling | Boundaries hold | Requires plugin setup and upkeep |
| Documented boundaries only | Cheap | Erode within a quarter |
| 100 modules | Max isolation | Build overhead; over-engineering; navigation between features gets complex |
| ~15 well-chosen modules | Right-sized | Requires judgement about where the seams are |
| Composite builds for independent teams | True team autonomy | Complex; a separate build pipeline; shared code needs publishing |

**How it is validated in production**
- **Build time** tracked as the primary guard — a module boundary that doubles the CI time will not survive.
- **Cycle detection** in CI.
- **Dependency-rule tests** asserting the module graph matches the intended shape.
- CODEOWNERS enforcement plus the review-time metric per module.

**Talking points / follow-ups**
- *"How do you decide the module boundaries?"* → By **team ownership and change coupling**: things that change together and are owned by one team belong in one module. Team boundaries are the primary signal; technical layers are not.
- *"How do you avoid slowing the build?"* → Measure after every few extractions; use the configuration and build caches; keep `api` usage minimal; do not create a module per screen.
- *"What is the hardest part?"* → Not the Gradle files — it is the **shared code**. Teams have to agree on what goes into `core` versus what each feature owns, and that is an organisational negotiation, not a technical one.
- *"How do you migrate without a big-bang?"* → Extract-and-move, one feature at a time, keeping main green. Every commit ships.

---

### 15. How would you decide whether a performance issue requires a local code optimization, an architectural change, or a backend redesign?

**What the interviewer is really asking**
Can you make a **rational, evidence-based decision about where to fix a problem** — and resist both the "just add a cache" reflex and the "rewrite the backend" reflex.

**Key concepts**
- **Measure first, always** — and measure the *right* layer: is it the network, the server, the DB, the CPU, the render?
- **The attribution question**: where is the time actually spent?
- **Latency budget**: decompose the end-to-end time into network, server compute, DB, serialisation, transfer, and render
- **Amdahl's law** — optimising 5% of the time cannot move the total much
- **The cost of each option**: local optimisation (hours, low risk, may not hold), architectural change (weeks, medium risk), backend redesign (months, high risk, and you do not control it)
- **Client-side vs server-side** — you can change one immediately and the other requires other teams
- **The "does it scale" question** — is the problem a constant-factor problem or a complexity/scaling problem?
- **P75/p95 vs p50** — optimise the tail
- **Measuring the server from the client** — request timing breakdown, `Server-Timing` header, distributed tracing
- **Cost of the fix vs. the user impact** — a 200 ms improvement on the most common path, versus a redesign
- **Technical debt vs. user pain**

**Approach / architecture sketch**

```
User complains: "the feed is slow"
  │
  ├─ Measure the end-to-end budget on a real device
  │     total 3.2s  =  network 0.4  +  server 2.4  +  db 0.3  +  client 0.1
  │
  ├─ 75% of the time is in the server → not a client optimisation
  │
  └─ Options, in order of cost:
        1. Local: cache, prefetch, paginate, render tuning   (hours)
        2. Architectural: move work off the request path     (weeks)
        3. Backend redesign: new API, caching, sharding      (months)
```

1. **Measure before deciding — and decompose the budget.** "The screen is slow" has almost never a single cause. Build the end-to-end budget for the slowest journey:
   - **Client**: request construction, serialisation, waiting, response parsing, DB write, UI render, image decode.
   - **Network**: DNS, TLS handshake, connection reuse, time-to-first-byte, transfer time, RTT.
   - **Server**: compute time, database time, downstream calls, queueing.
   Use **distributed tracing** with the client's `X-Request-Id` propagated (see Q9) so the server's contribution is visible. The `Server-Timing` header is a lightweight way for the server to report its own breakdown to the client.
   Without this decomposition, teams optimise the wrong layer — the most common version of this story is a client team rewriting a screen that spends 90% of its time waiting for the server.
2. **Ask the scaling question: constant factor or complexity?**
   - **Constant factor** (a slow query missing an index, a missing HTTP cache, an unoptimised JSON parse) → a **local fix** suffices. It is a specific, bounded change.
   - **Complexity** (an O(n²) algorithm, an N+1 query pattern, the server doing per-request work it should do once) → the fix is **architectural**: move the work, cache it, or restructure the flow.
   - **Capacity/scaling** (the endpoint does not scale with traffic) → a **backend redesign** or a scale-out is needed, and it is a conversation with the backend team, not a client change.
3. **Choose the option by cost-to-benefit, in this order:**
   - **Local optimisation** — hours to days, low risk, reversible. Justified when the client is genuinely the bottleneck, or when it buys a meaningful user-perceived win (e.g. rendering the first screen with cached data instead of a spinner). Note that it usually does **not** fix the underlying problem and can hide it.
   - **Architectural change** — weeks. Justified when the *shape* is wrong: the client does work the server should do, the request is synchronous when it could be deferred, or a `Flow`-driven cache would eliminate the request entirely. Example: replacing "fetch a summary on every screen open" with "the server pushes an update and the client renders local data" changes the shape and removes the latency.
   - **Backend redesign** — months, cross-team, and you do not own it. Justified when the server is the bottleneck *and* the fix is structural (a new aggregate endpoint, server-side caching, denormalisation, sharding). **Only do this when the measurement shows the backend is the bottleneck** — otherwise it is the most expensive way to fix a client bug.
4. **Apply Amdahl explicitly.** If the client is 10% of the budget, halving the client time improves the total by 5%. Say this out loud — it is the most effective way to redirect effort away from a layer that cannot help. This is also the strongest staff-level move in the conversation: **it is a fact, not an opinion.**
5. **Consider the "what does the user perceive" question.** Users feel time-to-first-content, not total time. A change that moves 3.2 s to 1.8 s may matter less than one that shows content in 400 ms and fills in later. Sometimes the right answer is **progressive rendering**, which is architectural but cheap.
6. **Weigh the user impact against the effort.** A 400 ms improvement on 99% of sessions for one engineer's week is a better trade than a 2 s improvement for a quarter of a team's effort. Rank by **(impact × frequency) ÷ effort**. And check: is this on the critical path for a business metric (checkout completion), or a rarely used screen?
7. **Prefer the reversible, local fix first — but say out loud that it is a mitigation.** If the client is masking a backend problem with a cache, that is a legitimate short-term move, and it should be labelled as one, with a ticket for the real fix. Undocumented masking is how a technical debt becomes permanent.
8. **Look at the tail, not the average.** A p50 of 400 ms with a p95 of 4 s is a different problem from a uniform 2 s. Tail latency usually comes from **cold starts, cache misses, lock contention, connection setup, or a noisy neighbour** — a different set of causes, and often fixable with a targeted change (prewarm, a longer TTL, connection reuse).
9. **Consider the device population.** A change that helps a flagship device may do nothing for the low-end 20% of your users. Segment the measurement by device class before choosing a fix — the answer often differs per segment (e.g. a smaller image decode budget for low-memory devices).
10. **Make the decision explicit and written.** A short decision note — the measurement, the attribution, the options with costs, the decision, and the follow-up — prevents the debate from restarting and creates the accountability for the deferred part.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Local optimisation | Days; low risk; reversible | Often treats a symptom; does not scale; can hide a backend problem |
| Architectural change | Removes the latency structurally; scales | Weeks; touches many layers; risk of regression |
| Backend redesign | Fixes the true bottleneck for all clients | Months; cross-team; you do not own it; highest risk |
| Caching / prefetch on the client | Fast, visible win | Staleness; cache invalidation complexity; memory cost |
| Server-side caching / precomputation | Benefits all clients; removes client work | Invalidation complexity; a backend project |
| Progressive rendering | Improves perceived latency hugely | More complex UI states; more requests in some designs |
| New aggregate endpoint | One round trip instead of five | A backend endpoint to design, version, and own |
| Do nothing | Zero cost | The problem persists and grows |
| Move the work to the client | Offloads the server | Slow devices; more data transferred; privacy considerations |

**How it is validated in production**
- The **budget decomposition** is re-measured after the change, on the same journey and the same device class, and the delta is published.
- Field metrics (from the client's own telemetry) confirm the improvement in production, not just in a lab.
- Guardrails: the change must not regress a different metric (battery, data usage, memory, crash rate) — a latency win that costs 30% more battery is not a win.

**Talking points / follow-ups**
- *"How do you decide where the problem is?"* → Decompose the latency budget with distributed tracing; never guess. The most common mistake is optimising the client when 90% of the time is server-side.
- *"When is a backend redesign the answer?"* → When the measurement shows the backend dominates the budget **and** the fix is structural — and it is a jointly-owned decision with the backend team, not a client team's unilateral call.
- *"When is a local optimisation the right answer?"* → When the client is genuinely a meaningful share of the budget, or when a cheap local change (caching, prefetch, progressive rendering) removes the user's wait — while the structural fix is tracked separately.
- *"How do you avoid optimising the wrong thing?"* → Amdahl: if the layer is a small share of the budget, no amount of optimisation there will move the number. Do the arithmetic before you write the code.
- *"What if the backend team disagrees about the attribution?"* → Use the trace data, not opinion — that is exactly why you built the request-ID correlation in the first place.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Measure before you decide** | 1, 3, 9, 11, 15 — latency budgets, trace correlation, crash Pareto, baselines |
| **Make the guarantee server-side** | 2, 4, 8 — idempotency keys, outbox, kill switches; the client cannot be the arbiter |
| **Bound the data, bound the work** | 1, 4 — keyset pagination, delta sync, bounded storage and bounded batches |
| **Adoption over mandate** | 3, 6, 7, 13, 14 — platforms, strangler migrations, ratchets, champions, module boundaries |
| **Design for failure and for the unknown** | 2, 8, 10 — UNKNOWN outcomes, staged rollout, a cohort as the diagnosis |
| **Talk to humans, not just to code** | 10, 11, 12, 13 — incident command, capacity trade-offs, PM negotiation, influence without authority |

**The one sentence to memorise for this section:**
> *"A staff engineer's job on Android is less about writing the screen than about choosing where the work happens — on the device, in the database, or on the server — and then making that choice measurable, incremental, and safe to reverse."*
