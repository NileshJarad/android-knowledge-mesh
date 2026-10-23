# Network, API and Distributed Systems

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. A payment request times out, but the server may have processed the payment. How would you prevent duplicate charges?

**What the interviewer is really asking**
Can you reason about the *ambiguous state* of a distributed system — where the client cannot distinguish "the server never got it" from "the server processed it but the response was lost" — and do you know that **idempotency is a server-side contract, not a client-side trick**? A junior answer is "add a loading spinner and disable the button". A staff answer is "the client cannot prevent duplicates; the server can, and only by accepting an idempotency key."

**Key concepts**
- Idempotency keys (client-generated, per logical operation)
- At-most-once vs at-least-once delivery
- Transactional outbox / state machine on the server
- Optimistic UI with pending states
- Reconciliation (poll for final state)

**Approach**

1. **Recognise the ambiguity explicitly.** A timeout means the outcome is *unknown*, not *failed*. Treating it as failure and blindly retrying is exactly what causes the double charge. The first line of the answer should be: *"The client cannot solve this alone — it must be solved with a server-side idempotency contract."*

2. **Client generates an idempotency key per logical user intent** (not per HTTP attempt). A `UUID` minted when the user taps "Pay" and stored with the request:
   ```http
   POST /v1/payments
   Idempotency-Key: 7f2c...-9a1b
   ```
   The key must be **persisted** (Room/DataStore) *before* the request is fired, otherwise a process death between "tap" and "request sent" loses it.

3. **Server stores the key with the outcome.** On the first request the server begins a transaction: insert `(idempotency_key, request_fingerprint, status=PENDING)` and process the charge. Any subsequent request with the same key returns the *stored outcome* instead of executing a new charge:
   - key + same request body → replay the original response (`200` with the original transaction id)
   - key + **different** request body → `409 Conflict` (key reuse with a mutated payload)
   - key + original still `PENDING` → `409`/`425 Too Early`, or block briefly and then return the terminal result

4. **Never reuse a key across different payments.** Keys are scoped to (account, endpoint) and expire after a retention window (e.g. 24h–7d). After expiry, a *replayed* key would be treated as new — so the retention window must exceed the maximum client retry horizon.

5. **Client-side UX reflects the tri-state outcome**, not a binary success/failure:
   - `SUCCESS` → show receipt
   - `PENDING` → show "Payment processing", and **poll** `GET /v1/payments/{idempotencyKey}` until it reaches a terminal state
   - `FAILED` → allow retry, but with a **new** idempotency key only if the server confirms the previous attempt terminally failed; otherwise reuse the original key

6. **Reconciliation layer.** A periodic (WorkManager) or on-launch job queries payments in `PENDING` beyond a threshold and resolves them — this catches the case where the app was killed and the polling loop never ran.

7. **Card-network idempotency as a second line of defence.** Many PSPs (Stripe, Adyen, Braintree) accept their own idempotency key. Pass yours through as their key so a duplicate at *their* layer is also collapsed. Defence in depth: your key → PSP key.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Client-generated idempotency key | Standard, portable across gateways, works offline-queued | Client must persist it; a client that mints a fresh key per retry breaks the guarantee entirely |
| Server-side key + replay | Correct, idempotent under any client retry behaviour | Requires server storage and a "result cache" with TTL; adds a DB write to the hot path |
| Blocking/deduplicating on the client only | Zero server change | **Wrong under process death, multi-device, or concurrent app instances.** Client state is not authoritative |
| Reconciling via polling | Handles the "unknown" state correctly | Polling is battery-costly; needs backoff and a server-side "list pending" endpoint |
| Optimistic UI showing "Paid" | Best perceived latency | Dangerous — if it turns out to fail, trust is lost and the app now shows a false ledger entry. Reserve "Paid" for a confirmed server response |

**Key failure mode to call out in the interview:** *retrying with a new idempotency key after a timeout*. This is the single most common real-world double-charge bug, because "retry" and "new attempt" feel the same to a developer. The rule is: **one logical intent ⇒ one key ⇒ many attempts.**

```kotlin
// Client side: the key is minted once per user intent and persisted before the call.
class PaymentRepository(
    private val api: PaymentApi,
    private val dao: PaymentDao,       // Room
    private val clock: Clock,
) {
    suspend fun pay(request: PaymentRequest): PaymentOutcome {
        // 1. Reuse any key we already minted for this intent.
        var record = dao.findByIntentId(request.intentId)

        if (record == null) {
            record = PaymentRecord(
                intentId = request.intentId,           // stable across retries
                idempotencyKey = UUID.randomUUID().toString(),
                state = PaymentState.PENDING,
                createdAt = clock.nowMillis(),
            )
            dao.insert(record)                        // persist BEFORE the network call
        }

        // 2. Always send the SAME key on every attempt.
        return try {
            val resp = api.pay(
                amount = request.amount,
                idempotencyKey = record.idempotencyKey,
            )
            dao.markTerminal(record.idempotencyKey, resp.transactionId, PaymentState.SUCCEEDED)
            PaymentOutcome.Success(resp.transactionId)
        } catch (e: IOException) {
            // 3. Timeout / no connectivity ⇒ UNKNOWN, not failed. Keep PENDING, do not retry
            //    with a new key. The reconciliation worker will resolve it.
            dao.markUncertain(record.idempotencyKey)
            PaymentOutcome.Pending(record.idempotencyKey)
        }
    }
}
```

**Talking points / follow-ups**
- *"What if the user taps Pay on two devices simultaneously?"* → The key must be derived from a **server-issued intent id**, not a purely local UUID, or the two devices mint different keys and the server cannot collapse them. Real answer: the server creates a *payment intent* (idempotent, keyed on a client-side correlation id) and the charge references the intent.
- *"How long do you keep idempotency keys?"* → Long enough to cover the worst-case retry window + offline queues; measure from telemetry, not guesswork.
- *"Does a distributed transaction help?"* → No — you do not want a 2PC across the ledger service and the PSP. Idempotency + reconciliation is the standard saga/outbox pattern.
- *"How would you test this?"* → Fault injection at the transport layer: server processes, response dropped. Assert exactly one charge row.
- *"What about a partially-successful payment — charged but the ledger write failed?"* → The server's own internal idempotency must cover *its* downstream calls; the PSP key + a ledger outbox do that.

---

### 2. The user kills the app during a payment and reopens it later. How would you recover the payment state?

**What the interviewer is really asking**
Do you understand that **in-memory state is unreliable** and that recovery must be driven from *durable* client state plus a *server-authoritative* status? And can you articulate the difference between "recover the UI" and "recover the truth"?

**Key concepts**
- Durable local state (Room, DataStore) as the write-ahead log of intent
- `SavedStateHandle` vs persistent storage (process death vs task eviction)
- `WorkManager` for deferred reconciliation
- Server-side status endpoint (GET-by-key)
- Cold-start state restoration via `onRestoreInstanceState` / `SavedStateHandle`
- App startup "recovery sweep"

**Approach**

1. **Persist intent before acting.** The moment the user initiates a payment, write a `PaymentRecord` to Room in `PENDING` state containing the idempotency key, intent id, amount, and a reference to the screen/destination to return to. This is the single most important line — *if you only hold the key in memory, a kill loses it and you can never reconcile*.

2. **On app cold start, run a recovery sweep** *before* showing the payment screen (or immediately after the graph is built, off the main thread):
   ```kotlin
   // Application-level, run once per cold start
   class RecoveryWorker(...) : CoroutineWorker(...) {
       override suspend fun doWork(): Result {
           val unresolved = dao.findByState(PaymentState.PENDING)
           for (p in unresolved) {
               val remote = api.getPayment(p.idempotencyKey)   // idempotent GET
               dao.updateFromRemote(p.idempotencyKey, remote)
           }
           return Result.success()
       }
   }
   ```
   Trigger it via `WorkManager` with a `NetworkType.CONNECTED` constraint so it survives the process being killed again, and run it eagerly on launch when connectivity is available.

3. **Restore navigation to the right screen.** Persist a `resumeDestination` (e.g. `"payment/receipt/{intentId}"`) alongside the record. On cold start, the `NavController` reads the pending record and routes to the payment-status screen, which then shows the resolved state. If a `SavedStateHandle`/saved-instance-state bundle survives, use it; if the *process* was killed, only persistent storage survives, so the Room record is the source of truth.

4. **Use `SavedStateHandle` for the "cheap" case.** If only the *Activity* was recreated (rotation) but the process lived, `SavedStateHandle` in the ViewModel restores the `intentId` and you skip the server round-trip. If the *process* died, `SavedStateHandle` is empty and you fall back to the Room record. Design the code to handle both:
   ```kotlin
   class PaymentViewModel(
       private val savedStateHandle: SavedStateHandle,
       private val repo: PaymentRepository,
   ) : ViewModel() {
       private val intentId: String? =
           savedStateHandle.get<String>(KEY_INTENT) ?: repo.peekLatestPending()?.intentId
   }
   ```

5. **Show an honest state on return.** Because the outcome is unknown until reconciled, the UI shows a *pending/uncertain* screen, not a failure. Only after the server confirms do you show success or failure. Optionally show the last known local state (e.g. "Payment submitted, checking status…").

6. **Idempotent resume.** Resuming is a *read*, so it is naturally safe. Never re-fire the charge on resume — only re-read the status. If the server says `UNKNOWN`/not found for the key, that means it was never processed; then it is safe to re-submit **with the same key**.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Room record as source of truth | Survives process death, uninstall-safe-ish, queryable | Adds a write before the network call; must be kept idempotent itself |
| `SavedStateHandle` only | Simple, survives rotation | **Does not survive process death** — a common misconception worth stating explicitly |
| Eager recovery sweep at cold start | Immediate correct UI | Extra network calls at startup; must be lazy/non-blocking to not delay TTID |
| `WorkManager` reconciliation | Survives repeated kills, network-aware | Minimum latency ~15 min by default; use expedited work for user-visible flows |
| Poll-until-terminal | Simple, converges | Battery cost; must stop after N attempts and fall back to "check later" |

**Key detail to mention:** `WorkManager`'s default `PeriodicWorkRequest` has a 15-minute minimum period, so it is *not* suitable for user-visible recovery latency. Use `OneTimeWorkRequest` with expedited/`setExpedited` for "user just reopened the app" cases.

**Code example**

```kotlin
@Entity(tableName = "payment_records")
data class PaymentRecord(
    @PrimaryKey val idempotencyKey: String,
    val intentId: String,
    val amountCents: Long,
    val state: String,            // PENDING | SUCCEEDED | FAILED | UNCERTAIN
    val transactionId: String?,
    val resumeRoute: String?,     // where to send the user on recovery
    val updatedAt: Long,
)

class PaymentRecoveryManager(
    private val dao: PaymentDao,
    private val api: PaymentApi,
) {
    suspend fun recoverOnColdStart(): List<PaymentRecord> {
        val pending = dao.findByStateIn(listOf("PENDING", "UNCERTAIN"))
        pending.forEach { local ->
            val remote = runCatching { api.getPayment(local.idempotencyKey) }.getOrNull()
            if (remote != null) dao.updateFromRemote(local.idempotencyKey, remote)
        }
        return dao.findByStateIn(listOf("PENDING", "UNCERTAIN"))
    }
}
```

**Talking points / follow-ups**
- *"What is the difference between process death and task removal?"* → Process death = process killed, state lost except saved instance state; task removal = Activity destroyed but process may live.
- *"How long before you declare the payment failed?"* → Never unilaterally; the server owns the terminal state. The client can time out *waiting*, but the *charge* outcome is server-determined.
- *"How would you test this?"* → Kill the app (`adb shell am force-stop`) mid-payment, relaunch, assert the user lands on the pending screen and the final state resolves correctly.
- *"What if the record was written but the request was never sent?"* → Server returns 404 for the key → safe to re-submit with the same key. The server's "key not found" is the signal.

---

### 3. An API request is triggered twice because the user double-taps a button. How would you prevent duplicate operations?

**What the interviewer is really asking**
Layered defence: the interviewer wants to see that you distinguish **UI-level** double-submission from **semantic** duplicate operations, and that you know the UI guard is a UX mitigation while the **server-side idempotency key is the real fix**.

**Key concepts**
- Debouncing vs disabling vs "in-flight" guards
- Mutex / atomic state to guard the critical section
- Idempotency keys (server contract)
- `collectLatest` / `flatMapLatest` for search-style inputs
- Distinguish *create* (non-idempotent) from *update* (idempotent, PUT)

**Approach**

1. **Understand which kind of operation it is.**
   - *Non-idempotent create* (e.g. `POST /orders`) — dangerous. Needs a real guard + idempotency key.
   - *Idempotent operation* (e.g. `PUT /profile`, `POST /like` with a natural key, `PATCH`) — a double-fire is merely wasteful; it is safe but still should be suppressed.

2. **Layer 1 — UI-level immediate feedback (fast, imperfect).**
   - Disable the button and show a spinner *immediately* on the first tap (not after a network round-trip).
   - In Compose: hoist an `isSubmitting` flag and guard the click handler.
   - A short `debounce(300)` on the click flow is a cheap extra layer but **not sufficient** — a slow network means the user genuinely taps twice with intent.

3. **Layer 2 — in-flight guard in the ViewModel/repository.** Even with the button disabled, other entry points (deep link, re-delivery of a Fragment result, a fast double-tap landing between recompositions) can fire twice. Use an atomic in-flight guard:
   ```kotlin
   private val submitMutex = Mutex()
   private var inFlight = false

   suspend fun submit(order: Order): Result<Order> =
       submitMutex.withLock {
           if (inFlight) return Result.failure(AlreadySubmitting)   // note: return inside withLock
           inFlight = true
           try { api.createOrder(order) } finally { inFlight = false }
       }
   ```
   `Mutex.withLock` makes the check-and-set atomic; a plain `var` check would not be.

4. **Layer 3 — semantic deduplication via idempotency key.** This is what survives process death and multi-device. The key is minted **once per logical intent** (see Q1). So even if layers 1 and 2 both fail, the server collapses the duplicate into one operation and returns the original result.

5. **Layer 4 — server-side natural-key dedupe.** For entities with a natural key (e.g. `POST /payments` with `(accountId, clientRequestId)`), enforce a unique index server-side. This protects against a buggy client too.

6. **Layer 5 — observability.** Log the idempotency key and a client-generated `correlationId` so that duplicate submissions are *visible* in analytics. A "duplicate suppressed" counter is a useful early-warning metric.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Disable button only | Trivial, familiar UX | Fails on slow networks, multi-device, and race between UI state and the network call |
| `Mutex` in-flight guard | Correct within one process | Lost on process death; doesn't protect other devices; must not be held across a long network call if it blocks other work (it does block concurrent `submit` calls — which is intended here) |
| Debounce | Filters accidental double-taps | Also filters *intentional* rapid repeats; timing-based, not state-based |
| Idempotency key | Correct in all cases | Requires backend work; needs a stable key across retries |
| Server natural-key unique index | Protects against all clients | Requires schema change; can reject legitimate near-duplicates (business rules needed) |

**Key nuance:** "Prevent" has two meanings — prevent the *request* (client hygiene) and prevent the *effect* (server idempotency). Say both explicitly in the interview; most candidates only address the first.

**Code example (Compose + ViewModel)**

```kotlin
// ViewModel — the guard lives here, not in the Composable.
class CheckoutViewModel(private val repo: OrderRepository) : ViewModel() {
    private val mutex = Mutex()

    var isSubmitting by mutableStateOf(false)
        private set

    fun submit() = viewModelScope.launch {
        mutex.withLock {
            if (isSubmitting) return@withLock       // second tap is a no-op
            isSubmitting = true
        }
        try {
            repo.createOrder()                      // carries a stable idempotency key
        } finally {
            isSubmitting = false
        }
    }
}

// Composable — purely reflects state; the guard is not duplicated here.
Button(
    onClick = viewModel::submit,
    enabled = !viewModel.isSubmitting,
) { Text("Place order") }
```

**Talking points / follow-ups**
- *"Why not just `debounce(500)` on the click?"* → Because it does not survive process death and it breaks legitimate rapid navigation; state-based guards are deterministic.
- *"Does a `Mutex` around a network call cause problems?"* → It serialises concurrent submits, which is what we want here. But do not use a global mutex for *all* network calls — that would serialise unrelated work.
- *"What about a `Mutex` in a `viewModelScope` — is it safe across configuration change?"* → Yes; the ViewModel survives configuration change so the mutex and the in-flight flag survive it. It does **not** survive process death.
- *"What if the user double-taps after a validation error?"* → Reset the guard in `finally` so the user can retry, and mint a *new* idempotency key only if the previous attempt was rejected as a validation error (server never created anything). Be careful: a validation error before processing is safe to re-key; a 5xx is not.

---

### 4. Two search API requests return responses out of order. How would you ensure that stale data doesn't overwrite the latest results?

**What the interviewer is really asking**
Classic **race condition / last-writer-wins** problem. They want to see if you know the difference between *cancelling* the in-flight request and *ignoring* its result, and whether you can explain why cancellation alone is not sufficient (the server may still respond; the response may already be in flight).

**Key concepts**
- `flatMapLatest` (cancel previous) vs `mapLatest` vs `flatMapMerge` vs `flatMapConcat`
- Request sequence numbers / generation counters
- `Mutex` + monotonic sequence check
- `collectLatest` on the collector side
- Race window: cancellation is *cooperative* — the response can arrive between cancellation request and actual cancellation

**Approach**

1. **Model the trigger as a Flow of query strings** and let the framework handle the latest-wins semantics:
   ```kotlin
   queryFlow
       .debounce(300)
       .distinctUntilChanged()
       .flatMapLatest { q -> repo.search(q).flowOn(Dispatchers.IO) }
       .collect { result -> _uiState.value = UiState.Success(result) }
   ```
   `flatMapLatest` cancels the previous inner flow when a new query arrives, so only the newest result is ever delivered downstream.

2. **Why cancellation alone is not bulletproof.** `flatMapLatest` cancels the *collection* of the previous inner flow. If the network layer has already parsed the response and is about to emit, or if the cancellation is not honoured (a blocking call in `runBlocking`, a non-cancellable callback), the stale result can still land. Also, cancellation of a `Deferred`/HTTP call does not guarantee the server did not process it. Hence, layer 2.

3. **Add a sequence/generation guard at the point of state mutation.** This is the robust, framework-independent fix:
   ```kotlin
   private val requestSeq = AtomicLong(0)

   fun onQueryChanged(query: String) {
       viewModelScope.launch {
           val seq = requestSeq.incrementAndGet()
           val result = repo.search(query)
           if (seq == requestSeq.get()) {          // only the newest wins
               _uiState.value = UiState.Success(result)
           }
       }
   }
   ```
   Equivalently, keep the token *inside* the flow so the whole pipeline is protected:
   ```kotlin
   queryFlow
       .flatMapLatest { q -> repo.search(q).map { Requested(q, it) } }
       // ^ but this still lets an inner emission through if cancellation is late
   ```
   The `flatMapLatest` + sequence combination is belt-and-braces.

4. **Alternatively, isolate the network layer from UI state entirely** — push results into a **single-writer** (Room). With an offline-first architecture the "latest wins" problem is solved differently: the DB is the single source of truth, and writes are scoped to the current query (`@Query("SELECT * FROM results WHERE query = :q")`), so a stale response writes to a different row and never corrupts the visible one. The UI observes only the current query's row. This is the most robust design at scale.

5. **Also address the other half of the problem**: a *newer* response could arrive before an *older* one and the older one must not overwrite. The sequence guard handles that; `flatMapLatest` handles it in the common case.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `flatMapLatest` | Idiomatic, minimal, automatic cancellation | Depends on cooperative cancellation; a small race window remains; abandons in-flight work (wasted server resources) |
| Sequence/generation counter | Fully deterministic; works regardless of cancellation behaviour | Extra state to manage; must be reset carefully on re-subscription (e.g. new query type) |
| Room as single writer | Eliminates the class of bug entirely; offline-first, survives process death | Heavier; needs a write strategy and DB churn; results are not "live" until written |
| `Mutex` around the request | Serialises access | Does **not** fix ordering — it just serialises, and would make requests sequential (bad for search latency). A common misconception to correct |

**Key detail:** `Mutex` is the wrong tool here. People reach for it because it is "thread-safe". The bug is not a data race on a shared variable — it is a **logical ordering** problem. The correct tools are cancellation semantics or an explicit version/epoch.

**Code example**

```kotlin
class SearchViewModel(private val repo: SearchRepository) : ViewModel() {

    private val query = MutableStateFlow("")

    // Epoch guard: only the latest request may publish to UI state.
    private val epoch = AtomicLong(0)

    val state: StateFlow<UiState> = query
        .debounce(300)
        .distinctUntilChanged()
        .flatMapLatest { q ->
            flow {
                val current = epoch.incrementAndGet()
                emit(repo.search(q).map { SearchResult(current, q, it) })
            }
        }
        .map { res -> UiState.Success(res.items) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Idle)

    fun onQueryChanged(q: String) { query.value = q }
}
```

*(A simpler and very common variant: keep the epoch check inside the repository's `search` suspend function and only apply the result if the epoch still matches.)*

**Talking points / follow-ups**
- *"What is the difference between `mapLatest` and `flatMapLatest`?"* → `mapLatest` transforms values in a suspending `map` (cancels the previous transform); `flatMapLatest` works with *flows* of results and cancels the previous inner flow. For search, `flatMapLatest` is right because the result is itself a flow.
- *"Does cancellation cancel the HTTP request?"* → Only if the underlying client supports it (OkHttp's `Call.cancel()`); Retrofit's coroutine support does cancel the call. But the server may have already done work.
- *"How do you test this?"* → Use a fake server that delays response A by 2s and response B by 100ms; assert only B's data is shown.
- *"What about `collectLatest`?"* → `collectLatest` cancels the *body* of the previous collector when a new value arrives — useful for expensive per-item work (e.g. writing each result to DB), but it does not stop an old network response from arriving; combine with `flatMapLatest` upstream.
- *"Should we cancel the old request at all?"* → Trade-off: cancelling saves server load but may waste work already done; for search, cancelling is right. For non-cancellable/expensive server work, consider letting it finish and discarding the result.

---

### 5. A screen depends on five APIs, but only three succeed. How would you handle partial API failures?

**What the interviewer is really asking**
Can you design a **composite state** for a multi-source screen, and do you understand **per-section error handling vs. screen-level failure**? A good answer separates "this section failed" from "the whole screen failed" and degrades gracefully.

**Key concepts**
- Sealed UI state / per-widget state
- `awaitAll` vs sequential vs `async` with individual error capture
- `Result<T>` vs exceptions
- Skeleton/placeholder vs error-with-retry per section
- Cache fallback (stale-while-revalidate)
- `supervisorScope` for partial failure tolerance
- Observability: per-endpoint success/failure metrics

**Approach**

1. **Never let one failure nuke the whole screen.** Fetch each section independently with its own error capture:
   ```kotlin
   data class DashboardState(
       val profile: Section<User>,
       val orders: Section<List<Order>>,
       val alerts: Section<List<Alert>>,
       val recommendations: Section<List<Product>>,
       val promos: Section<Promo>,
   )

   sealed interface Section<out T> {
       data object Loading : Section<Nothing>
       data class Success<T>(val data: T, val fromCache: Boolean = false) : Section<T>
       data class Error(val message: UiText, val cause: Throwable) : Section<Nothing>
   }
   ```
   The screen renders each section with its own state — spinner, content, or an inline error card with a retry button scoped to that section.

2. **Fetch concurrently, capture failures individually.** `awaitAll` cancels siblings on the first failure — wrong here. Use `supervisorScope` + `runCatching` per section, or a helper:
   ```kotlin
   suspend fun <T> SectionResult<T> = runCatching { block() }

   suspend fun loadDashboard(): DashboardState = supervisorScope {
       val p = async { SectionResult { api.profile() } }
       val o = async { SectionResult { api.orders() } }
       val a = async { SectionResult { api.alerts() } }
       val r = async { SectionResult { api.recommendations() } }
       val m = async { SectionResult { api.promo() } }
       DashboardState(
           profile   = p.await().toSection(),
           orders    = o.await().toSection(),
           alerts    = a.await().toSection(),
           recommendations = r.await().toSection(),
           promos    = m.await().toSection(),
       )
   }
   ```
   With `supervisorScope`, a failure in one `async` does not cancel the others.

3. **Classify errors and decide the fallback per section.** Not all failures are equal:
   - `401` → global: trigger auth refresh; the whole screen is blocked.
   - `5xx` / `IOException` → **use cached data if available**, mark it stale (`fromCache = true`) and show a subtle "showing saved data" banner.
   - `404` → this section genuinely has no data; render an empty state, not an error.
   - `422` / validation → user-fixable; surface inline.

4. **Bound the total latency.** Five sequential calls is unacceptable. Fan out concurrently, but impose a **per-section timeout** and a **global deadline**:
   ```kotlin
   withTimeoutOrNull(3_000) { api.orders() } ?: Section.Error(UiText.Timeout)
   ```
   A section that exceeds its budget degrades to its cached/empty state rather than blocking the screen.

5. **Retry only what is retryable.** Automatic retry for idempotent GETs with backoff (429/5xx/network); manual retry button for 4xx. Retrying all five on a single failure wastes battery and hammers the backend.

6. **Make it observable.** Emit a per-endpoint metric (`dashboard_section_load{endpoint="orders", result="error", code="503"}`) and a dashboard-level "partial load rate". A screen that "looks fine" but silently drops the alerts section is a silent correctness bug that nobody reports.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Per-section state | Best UX, isolates failures, enables per-section retry | More UI code and more complex state modelling; many small states to reconcile |
| All-or-nothing screen state | Simple | Bad UX — one flaky endpoint blanks an otherwise useful screen |
| `awaitAll` | Concise | **Cancels sibling requests on first failure** — loses data that already succeeded. Wrong primitive here |
| `supervisorScope` | Sibling failures isolated | Requires per-child `runCatching` anyway; exceptions still propagate if uncaught in a child |
| Cache fallback | Always shows something | Users may act on stale data; must be visibly labelled |
| Global timeout | Bounds worst case | A slow-but-useful section is dropped; tune per-section |
| Compose all five in one ViewModel method | Single source of truth | Long method, harder to test; consider per-section ViewModels |

**Key detail:** In Compose, a per-section state model also means a **stable key per section** in the layout so that one section's state change does not recompose the others. This connects Q5 to Section 6 (Compose) nicely.

**Code example**

```kotlin
// Generic helper: never throws, always produces a Section.
suspend fun <T> loadSection(
    timeoutMs: Long = 3_000,
    block: suspend () -> T,
): Section<T> = withTimeoutOrNull(timeoutMs) {
    try {
        Section.Success(block())
    } catch (c: CancellationException) {
        throw c                                     // never swallow cancellation
    } catch (t: Throwable) {
        Section.Error(t.toUiText(), t)
    }
} ?: Section.Error(UiText.Timeout, TimeoutException())
```

**Talking points / follow-ups**
- *"What if the alerts endpoint is down for everyone?"* → Ship a kill switch (remote config) that disables the section and renders a placeholder; treat it as a product decision, not an engineering one.
- *"How do you decide the timeout?"* → From the backend's own latency distribution (p95/p99), not from a guess. If the backend p99 is 2s, a 3s client timeout is right.
- *"Should we prefetch the next screen?"* → Only if it does not compete for the same bandwidth; a "critical" vs "nice-to-have" priority queue per section is a good answer.
- *"How do you test partial failure?"* → Fake API where one endpoint throws; assert the other four render and the failing one shows an inline retry.
- *"How is this different from just using `runCatching` everywhere?"* → `runCatching` catches `CancellationException` too, which breaks structured concurrency. Always rethrow it.

---

### 6. The network switches from Wi-Fi to mobile data while an API request is in progress. How would you handle the request?

**What the interviewer is really asking**
Network handover semantics: does the request **fail** (connection reset), **stall** (TCP timeout), or **survive**? And what is the right UX and retry policy in each case?

**Key concepts**
- TCP connections are bound to a network; handover usually kills the socket
- `ConnectivityManager` `NetworkCallback` / `registerDefaultNetworkCallback`
- `NetworkCapabilities` — `NET_CAPABILITY_VALIDATED`, `NET_CAPABILITY_INTERNET`, `NOT_METERED`
- OkHttp connection retry-on-failure, connection pool lifetime
- DNS caching and stale-while-revalidate
- Metered vs unmetered (Wi-Fi → mobile is a *downgrade* in cost, not just capability)

**Approach**

1. **Know what actually happens.** When the default network changes, Android tears down the old network's sockets. Existing in-flight requests typically fail with `SocketException: Connection reset` / `java.net.SocketException` or `UnknownHostException` (if DNS changed), rather than hanging. However:
   - On some devices/OS versions, the socket is retained briefly and the request can complete.
   - If the request is stuck in a connection-pool/keepalive window, it may fail fast.
   - If the app was using a **bound** network (`bindProcessToNetwork` or `ConnectivityManager.requestNetwork`), behaviour differs.

2. **Classify the error as retryable.** Treat `IOException` subclasses as retryable transport errors, and retry on the *new* network. This is exactly where a retry policy with backoff helps — the immediate retry after a handover has a good chance of succeeding on the new network.

3. **Do not kill in-flight work proactively by default.** Cancelling everything on every connectivity change causes unnecessary churn — the network may flap. Instead:
   - Let the current request fail naturally.
   - The retry layer re-issues it.
   - Only *cancel* if the request has been failing for a long time or the user navigated away.

4. **Use `ConnectivityManager` to know the *quality* of the new network** and adapt:
   ```kotlin
   val caps = cm.getNetworkCapabilities(cm.activeNetwork)
   val metered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false
   val validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
   ```
   - `NOT_METERED` lost → defer large downloads/uploads (media sync) until Wi-Fi returns. Do **not** defer small JSON calls — blocking them would break the core experience.
   - `VALIDATED` false → captive portal / no actual internet; show "check your connection" rather than a raw error.

5. **Differentiate requests by sensitivity to network class.** A small API call can go over cellular; a 200 MB video upload should be paused and resumed on Wi-Fi (see Section 9). Put a policy layer in the repository:
   ```kotlin
   when {
       request.requiresUnmetered && metered -> Result.Deferred(untilUnmetered)
       else -> proceed()
   }
   ```

6. **Handle DNS.** DNS caching means a request right after handover can fail with `UnknownHostException`. Retrying after a short delay (a few hundred ms) resolves it as the new network's DNS is configured. Prefer a short randomised delay for this specific class of error.

7. **Re-resolve the server.** If you use a CDN or geo-DNS, the endpoint host may resolve differently. Avoid hardcoding IPs; use hostnames so DNS re-resolution happens automatically.

8. **Expose connectivity state to the UI** via a `StateFlow<NetworkStatus>` derived from `NetworkCallback`, so the UI can show a subtle banner ("You're offline — we'll sync when you're back") instead of hard errors. But drive the UI from *request outcomes* primarily, since connectivity state can say "connected" while the server is unreachable.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Let requests fail and retry | Simple, correct, survives flapping | Slight latency cost; some requests fail unnecessarily during handover |
| Cancel on network change | Avoids doomed requests | Over-reacts to flapping; cancels work that would have completed; breaks long uploads |
| Defer all work until Wi-Fi | Saves data | Unacceptable for the core app experience |
| Defer only bulk media | Balanced | Needs a per-request "network class" policy; more code |
| Proactive connectivity state in UI | Good UX | Connectivity ≠ reachability; can show "online" while requests fail. Must combine with actual error state |

**Key detail:** `NET_CAPABILITY_VALIDATED` means the system has verified the network can reach the internet. On cellular it usually appears quickly; on a captive-portal Wi-Fi it stays absent. `NET_CAPABILITY_NOT_METERED` distinguishes Wi-Fi (usually) from cellular.

**Code example**

```kotlin
class ConnectivityObserver(context: Context) {
    private val cm = context.getSystemService<ConnectivityManager>()!!

    val status: StateFlow<NetworkStatus> = callbackFlow {
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val caps = cm.getNetworkCapabilities(network)
                trySend(
                    NetworkStatus(
                        connected = true,
                        metered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false,
                        validated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true,
                    )
                )
            }
            override fun onLost(network: Network) { trySend(NetworkStatus.OFFLINE) }
        }
        cm.registerDefaultNetworkCallback(cb)
        awaitClose { cm.unregisterNetworkCallback(cb) }
    }.stateIn(scope, SharingStarted.Eagerly, NetworkStatus.OFFLINE)
}
```

**Talking points / follow-ups**
- *"Does OkHttp automatically retry on network change?"* → OkHttp retries on certain connection failures (route selection) and has `retryOnConnectionFailure(true)` by default, but it does not re-issue a request whose socket was already reset mid-body.
- *"What about `Network.bindProcessToNetwork`?"* → Binding the process to a specific network prevents handover surprises but disables fallback to cellular — usually wrong for consumer apps.
- *"How would you test this?"* → `adb shell svc wifi disable` / `svc data enable` while a slow request is in flight; assert the retry succeeds and the UI shows the right state.
- *"Should a request that failed mid-download resume?"* → Yes, with `Range` headers — for large downloads this is essential.

---

### 7. An API takes 30 seconds to respond. How would you design the loading, timeout, cancellation, and retry behavior?

**What the interviewer is really asking**
End-to-end request lifecycle design: **timeouts** (per-stage), **cancellation propagation** (who cancels whom), **retry policy** (what is retryable, backoff, jitter, budget), and **user experience** (progress, cancel button, backgrounding). This is a staple staff question because it probes whether you treat the network as an unreliable, adversarial system.

**Key concepts**
- Connect / read / write / call timeouts (OkHttp) vs overall deadline (`withTimeout`)
- `kotlinx.coroutines.withTimeout` vs `withTimeoutOrNull`
- `CancellationException` propagation; `suspendCancellableCoroutine`
- Retry with exponential backoff + full jitter
- Retry budget / max attempts; idempotency requirement for retry
- Progress reporting and partial results
- `Dispatchers.IO` for blocking IO; never block the main thread

**Approach**

1. **Separate the timeouts by stage.** A single 30s read timeout is wrong for a small JSON call and right for a report generation.
   ```kotlin
   val client = OkHttpClient.Builder()
       .connectTimeout(10, TimeUnit.SECONDS)   // TCP+TLS
       .readTimeout(45, TimeUnit.SECONDS)     // between bytes; long for streaming
       .writeTimeout(15, TimeUnit.SECONDS)    // upload
       .callTimeout(60, TimeUnit.SECONDS)     // whole call incl. redirects/retries
       .build()
   ```
   For a JSON API, the *product* deadline should be much shorter than the server's — if the server takes 30s, the user has already left. Design the UX around ~10–15s for interactive calls, and use a background job for anything longer.

2. **Impose an application-level deadline** so the whole operation (including retries) is bounded:
   ```kotlin
   withTimeoutOrNull(15_000) {
       repo.fetchWithRetry()
   } ?: uiState.emit(UiState.Timeout)
   ```
   Note: `withTimeout` cancels the inner block; `withTimeoutOrNull` returns null instead of throwing.

3. **Make cancellation real and hierarchical.**
   - The UI scope owns the request. When the user leaves the screen, the ViewModel scope (or an explicit `Job`) is cancelled → the request is cancelled → OkHttp's `Call.cancel()` closes the socket.
   - Cancellation must propagate all the way down. Retrofit's suspend support does this natively; a custom `suspendCancellableCoroutine` wrapper must call `continuation.invokeOnCancellation { call.cancel() }`.
   - **Never swallow `CancellationException`** — this is the #1 bug in retry/timeout code:
     ```kotlin
     try { ... } catch (e: Exception) { retry() }   // WRONG: catches CancellationException too
     ```
     Correct:
     ```kotlin
     try { ... } catch (e: CancellationException) { throw e } catch (e: IOException) { retry() }
     ```

4. **Design retry policy precisely.**
   - **Retryable:** connection failures, timeouts, DNS failures, HTTP 408, 429, 500, 502, 503, 504.
   - **Not retryable:** 4xx (except 408/429), malformed responses, auth failures (401/403).
   - **Only retry idempotent operations** unless an idempotency key exists (see Q1).
   - **Exponential backoff with full jitter** (avoid thundering herd):
     ```kotlin
     val delay = min(maxDelayMs, baseDelayMs * (1 shl attempt)) * random.nextDouble()
     ```
   - **Cap total time**: max attempts (e.g. 3) *and* a total budget (e.g. 60s). Never retry indefinitely.

5. **Use `kotlinx.coroutines.retry` for the happy path**, and write a manual loop when you need custom classification:
   ```kotlin
   suspend fun <T> retrying(
       maxAttempts: Int = 3,
       baseDelayMs: Long = 500,
       shouldRetry: (Throwable) -> Boolean = { it is IOException },
       block: suspend () -> T,
   ): T {
       var delay = baseDelayMs
       repeat(maxAttempts - 1) {
           try { return block() }
           catch (e: CancellationException) { throw e }
           catch (t: Throwable) {
               if (!shouldRetry(t)) throw t
               delay(delay + Random.nextLong(0, delay))
               delay = (delay * 2).coerceAtMost(30_000)
           }
       }
       return block()
   }
   ```
   Note: `delay()` is itself cancellable — good, cancellation during backoff works.

6. **Give the user a way out and a way forward.**
   - Show a determinate/indeterminate progress indicator.
   - Offer a **Cancel** action that cancels the coroutine (and the socket).
   - If the request is slow, offer "continue in background" (move it to a WorkManager job) rather than forcing the user to wait.
   - Stream partial results if the API supports it (SSE/chunked) so a long operation can show progress.

7. **Separate "fast path" and "slow path".** Interactive calls get a short deadline; long operations become background work with their own lifecycle and notification. This is the architectural answer that scales.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Long client timeout matching server | Fewer false failures | Terrible UX — the user stares at a spinner; wasted resources |
| Short timeout + user retry | Responsive, honest | Extra taps; must make retry safe (idempotency key) |
| Auto-retry with backoff | Seamless | Hidden latency spikes; can hammer the backend; must respect 429/Retry-After |
| Streaming partial results | Great UX for long ops | Server + client complexity, harder to test, partial-state modelling |
| Move to background (WorkManager) | User can leave | Notification/permission complexity; result delivery to UI needs plumbing |

**Key detail:** `retryOnConnectionFailure(true)` in OkHttp handles connection-level retries, but **not** read timeouts after the request was sent — those need your own layer. Also, OkHttp's built-in retry is not counted against your own attempt budget, which can make your total time exceed expectations.

**Code example**

```kotlin
class ProfileRepository(
    private val api: ProfileApi,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun fetchProfile(): Profile = withContext(io) {
        retrying(maxAttempts = 3, baseDelayMs = 400) {
            val response = api.getProfile()
            when (response.code()) {
                in 200..299 -> response.body()!!
                408, 429 -> throw HttpRetryableException(response.code())
                in 500..599 -> throw HttpRetryableException(response.code())
                else -> throw HttpFatalException(response.code())
            }
        }
    }
}

// Cancellation-safe manual retry
suspend fun <T> retrying(
    maxAttempts: Int,
    baseDelayMs: Long,
    block: suspend () -> T,
): T {
    var delayMs = baseDelayMs
    var last: Throwable? = null
    for (attempt in 1..maxAttempts) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e                                    // critical
        } catch (t: Throwable) {
            last = t
            if (attempt == maxAttempts) break
            // full jitter
            val sleep = Random.nextLong(delayMs / 2, delayMs * 2)
            delay(sleep)
            delayMs = (delayMs * 2).coerceAtMost(30_000)
        }
    }
    throw last ?: IllegalStateException("retry failed")
}
```

**Talking points / follow-ups**
- *"What is the difference between `withTimeout` and `withTimeoutOrNull`?"* → The former throws `TimeoutCancellationException`; the latter returns null. Note that a `TimeoutCancellationException` is a `CancellationException`, so a `catch (e: Exception)` upstream can mask a timeout as a cancellation.
- *"Why is `delay()` cancellable?"* → Because it is a suspend function on a `CoroutineScope` that registers with the job; on cancellation it throws immediately.
- *"How do you test timeout behaviour?"* → Inject a fake that delays longer than the timeout and assert the `TimeoutCancellationException`/`null`; use `runTest` with virtual time so the test does not actually wait.
- *"How do you test retry?"* → Fake API that fails N times then succeeds; assert attempt count and that the total elapsed (virtual) time matches the backoff schedule.
- *"Should retries be automatic for writes?"* → Only with an idempotency key; otherwise no.

---

### 8. The API returns HTTP 401 while multiple requests are running concurrently. How would you refresh the access token without triggering multiple refresh requests?

**What the interviewer is really asking**
This is the classic **"single-flight" / thundering herd on token refresh** problem. The interviewer wants to see whether you know how to **share a single in-flight refresh** across concurrent callers and **retry the original requests** afterwards — and that you understand token storage, expiry, and the refresh-token rotation problem.

**Key concepts**
- Mutex / single-flight pattern
- `Deferred` sharing; `CompletableDeferred`
- Token expiry with proactive refresh (clock skew buffer)
- Refresh token rotation + reuse detection
- Authenticator/interceptor pattern (OkHttp `Authenticator`)
- Token storage (`EncryptedSharedPreferences`, `DataStore`)
- `StateFlow` of auth state

**Approach**

1. **Refresh proactively, not reactively.** Store the token's expiry (`expiresAt`) alongside the token. Before each request, if `now > expiresAt - skew` (e.g. 60s skew for clock drift), refresh *first*. This avoids the storm entirely in the common case.

2. **When a 401 does happen, do a single-flight refresh.** All concurrent requests that get a 401 must share one refresh:
   ```kotlin
   class TokenAuthenticator(
       private val tokenStore: TokenStore,
       private val refreshApi: AuthApi,
   ) {
       // One mutex guarding the refresh critical section.
       private val mutex = Mutex()

       // The in-flight refresh, if any. Others await this same Deferred.
       @Volatile private var inFlight: Deferred<Token>? = null

       suspend fun refreshToken(): Token {
           val existing = inFlight
           if (existing != null) return existing.await()   // join the existing refresh

           val deferred = scope.async { doRefresh() }
           inFlight = deferred
           try {
               return deferred.await()
           } finally {
               mutex.withLock { if (inFlight === deferred) inFlight = null }
           }
       }
   }
   ```
   The critical correctness points:
   - The `Deferred` is created **once** and shared; concurrent callers `await()` the same one.
   - `inFlight` is cleared in a `finally` so a failed refresh does not poison later calls.
   - Clearing uses identity comparison (`===`) so a newer refresh is not nulled out by an older one finishing late.

3. **Alternative: use a Mutex around the whole refresh, and re-check the token inside the lock.** This is simpler and also correct (the classic "double-checked" pattern):
   ```kotlin
   suspend fun getValidToken(): String {
       val cached = tokenStore.token
       if (cached != null && !cached.isExpired()) return cached.value
       return mutex.withLock {
           val again = tokenStore.token          // re-check inside the lock
           if (again != null && !again.isExpired()) return@withLock again.value
           val fresh = refreshApi.refresh(tokenStore.refreshToken).body()!!
           tokenStore.save(fresh)
           fresh.value
       }
   }
   ```
   Here N concurrent callers all block on the mutex, but only the first performs the refresh; the rest re-read and return the new token. Cost: they are serialised (fine — it's a fast call), and it is far easier to reason about. **For a staff answer, present this as the simple correct baseline and the `Deferred` approach as the lower-latency variant.**

4. **Retry the original request after refresh.** A 401 must not surface to the caller if it was caused by an expired token. The OkHttp `Authenticator` is the natural place:
   ```kotlin
   class TokenAuthenticator(...) : Authenticator {
       override fun authenticate(route: Route?, response: Response): Request? {
           if (responseCount(response) > 2) return null      // avoid infinite loop
           val newToken = runBlocking { tokenManager.refreshToken() }
           return response.request.newBuilder()
               .header("Authorization", "Bearer $newToken")
               .build()
       }
   }
   ```
   Note the guard against retry loops. Also: for Retrofit suspend functions, a custom `CallAdapter` or an interceptor that calls `runBlocking` is common; be aware `runBlocking` on the OkHttp dispatcher thread is acceptable but must not deadlock.

5. **Handle the case where the refresh token itself is expired/invalid** (401 on refresh): clear the session, emit an "auth expired" event, and navigate to login. The original in-flight requests should fail with a typed `AuthExpiredException` so the UI can react coherently.

6. **Handle refresh token rotation.** Many backends rotate the refresh token on each use. If two refreshes happen concurrently, one uses a stale refresh token and the server may revoke the whole session (reuse detection). The single-flight pattern prevents this — it is not just about efficiency, it is **security-relevant**. This is a strong point to make.

7. **Store the token safely.** `EncryptedSharedPreferences` or `DataStore` + Tink (for Keystore-backed encryption). Keep refresh tokens in the Keystore-backed store; keep access tokens in memory where possible.

8. **Expose auth state as a `StateFlow`** so the UI reacts to session expiry reactively instead of only on the next failed request.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Mutex + double-check | Simple, obviously correct, no duplicate refresh | Serialises waiters; each waiter wakes sequentially (fine for a fast call) |
| Shared `Deferred` (single-flight) | Lowest latency, no queuing | More moving parts; must handle identity-correct cleanup; `Deferred` scope must be a scope that outlives the caller |
| OkHttp `Authenticator` | Automatic retry of the failed request | Runs on OkHttp's thread; `runBlocking` there is risky; only handles HTTP-level auth, not "token missing" |
| Proactive refresh by expiry | Avoids the 401 path entirely | Requires accurate expiry and clock-skew handling; refresh token may expire before access token |
| Application interceptor pre-refresh | Full control, easy to test | Adds latency to every request if done naively; must be fast-path cached |
| Refresh-token rotation without single-flight | — | **Security bug**: parallel refresh can trigger server-side reuse detection and revoke the session |

**Key detail:** The `responseCount(response) > 2` guard prevents an infinite loop when the refreshed token is also rejected.

**Code example (interceptor + single-flight authenticator)**

```kotlin
class AuthInterceptor(
    private val tokenManager: TokenManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenManager.currentAccessToken()   // may trigger a proactive refresh
        val newRequest = if (token != null && !request.header("Authorization").isNullOrBlank()) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else request
        return chain.proceed(newRequest)
    }
}

class TokenManager(
    private val store: TokenStore,
    private val api: AuthApi,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    @Volatile private var inFlight: Deferred<Token>? = null

    suspend fun refreshToken(): Token {
        inFlight?.let { return it.await() }             // join existing
        val deferred = scope.async { performRefresh() }
        inFlight = deferred
        try {
            return deferred.await()
        } finally {
            mutex.withLock { if (inFlight === deferred) inFlight = null }
        }
    }

    private suspend fun performRefresh(): Token {
        val current = store.token ?: throw AuthExpiredException()
        val resp = api.refresh(current.refreshToken)
        if (resp.code() == 401) { store.clear(); throw AuthExpiredException() }
        val fresh = resp.body()!!
        store.save(fresh)
        return fresh
    }
}
```

**Talking points / follow-ups**
- *"Why not just call refresh in each 401 handler?"* → N concurrent 401s cause N refreshes; with rotation, that invalidates the session.
- *"How do you test this?"* → Fire 10 concurrent requests against a fake that always 401s once; assert the refresh endpoint was called exactly once and all 10 succeeded.
- *"What about `runBlocking` in the OkHttp `Authenticator`?"* → It blocks an OkHttp dispatcher thread. Acceptable in practice but consider `await` in a suspending call adapter instead to avoid thread starvation under load.
- *"Where do you store the refresh token?"* → Keystore-backed encrypted storage; never plain `SharedPreferences`. Consider `Android Keystore` + Tink, or a backend-for-frontend (BFF) pattern where the refresh token is an HttpOnly cookie the app never sees — the strongest option on Android.
- *"What if the device clock is wrong?"* → Use `skew` buffers and, ideally, derive expiry from a server-provided delta rather than local `System.currentTimeMillis()`.

---

### 9. The access token expires while a file upload is in progress. How would you recover without uploading the file twice?

**What the interviewer is really asking**
This combines **streaming upload resumption** with **auth renewal**. The interviewer wants to see that you know **multipart/resumable upload with byte-range resumption**, and that you understand **the server must support it** — you cannot solve this purely client-side.

**Key concepts**
- Resumable upload protocol (Google resumable upload / S3 multipart / tus)
- `Content-Range` / `offset` semantics; upload session id
- Chunked uploads with checkpointing
- OkHttp `RequestBody` that can report bytes written; `Authenticator` interaction with streaming bodies
- Content digest / checksum per chunk and for the whole file
- Idempotency of chunk PUTs (same chunk index/offset is idempotent)

**Approach**

1. **Recognise that resumption requires server support.** A plain `PUT` of the whole file cannot be resumed — if the connection dies at 60%, the server has to restart from 0 (or the client re-sends from 0). You need a **resumable protocol**: the server hands out an *upload session id*, and the client uploads in chunks with byte offsets, resuming from the last acknowledged offset.

2. **Server-side flow (canonical):**
   - `POST /uploads` → `{ uploadId, chunkSize }`
   - `PUT /uploads/{uploadId}/chunks/{index}` with `Content-Range: bytes start-end/total` → `200 { receivedBytes }`
   - `POST /uploads/{uploadId}/complete` → assembles, verifies digest, returns the resource.

3. **Client-side flow:**
   - Chunk the file (e.g. 5–10 MB), **hash each chunk**, and persist an upload checkpoint in Room: `(uploadId, chunkIndex, bytesUploaded, etag, fileUri, totalSize)`.
   - Before each chunk, check the checkpoint; **skip already-uploaded chunks**. Chunk PUTs are idempotent by `(uploadId, index)` — re-sending the same chunk is safe.
   - After each acknowledged chunk, update the checkpoint. On app restart, resume from the checkpoint and reconcile with the server: `GET /uploads/{uploadId}` → `{"receivedBytes": N}` → resume from `N`.

4. **Handle token expiry mid-upload.** Two layers:
   - **Don't wait for expiry.** Refresh the token *proactively* when it is near expiry, before starting the next chunk. Since chunks are small and independently authenticated, refreshing between chunks is trivial. This is the main answer: **chunking is what makes token renewal easy.**
   - **If a 401 occurs anyway**, the OkHttp `Authenticator` refreshes and retries the chunk. For a *streaming* body this is safe **only if** the body is replayable. OkHttp's `RequestBody.writeTo` can be called again, but a one-shot stream cannot. Therefore, **read each chunk fully into memory (or a temp file) before sending**, so it can be replayed:
     ```kotlin
     val bytes = file.slice(offset, offset + chunkSize).readBytes()  // replayable
     ```
     For very large chunks, spool to a temp file and use a `RequestBody` that reads from the file (also replayable).
   - Note: `Authenticator` **does not** retry if the request body is not replayable — OkHttp gives up with an error. So the design must guarantee replayability.

5. **Alternative if the backend does not support resumable uploads:** use **multipart chunk upload with a server-side list of received part numbers** (S3 multipart upload). Or, as a last resort, restart the upload with an **idempotency key** so the server de-duplicates the *logical* upload (though it may still re-transfer bytes). Be honest in the interview that client-only restart does not avoid re-transferring bytes.

6. **Move the upload to a `WorkManager` job** (or a foreground service for long uploads) so it survives process death and screen rotation. The `Worker` reads the checkpoint and resumes.

7. **Integrity:** compute a whole-file SHA-256 and send it on `complete`; the server verifies. Also verify each chunk's CRC/ETag. This catches silent corruption from interrupted writes.

8. **Progress and UX:** report bytes uploaded (not just percent) and surface `WorkInfo` progress in a notification. If the user cancels, keep the checkpoint so they can resume later.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Resumable/chunked upload protocol | True resumption, bounded re-transfer, natural token-refresh boundary | Requires backend support; more endpoints and state |
| Single multipart POST | Simple | Cannot resume without re-uploading everything; a 401 mid-body is hard to recover from |
| Restart from scratch on failure | Simplest | Wastes bandwidth and battery; unacceptable on cellular |
| Proactive refresh between chunks | Almost never hits 401 | Requires accurate expiry knowledge |
| Spooling chunks to memory/temp file | Replayable body → `Authenticator` works | Extra I/O and disk usage; temp file must be cleaned up |

**Key detail:** The `Authenticator` cannot save a non-replayable request body. This is the crux of "recover without uploading twice" — you need chunk-level replayability *and* server-side chunk idempotency.

**Code example**

```kotlin
class ResumableUploader(
    private val api: UploadApi,
    private val dao: UploadDao,
    private val chunkSize: Long = 8L * 1024 * 1024,
) {
    suspend fun upload(fileUri: Uri): String {
        var session = dao.findByUri(fileUri.toString())
        if (session == null) {
            val created = api.createUpload(fileUri.fileName, fileUri.size())
            session = UploadSession(
                uploadId = created.uploadId,
                uri = fileUri.toString(),
                nextChunk = 0,
                totalSize = fileUri.size(),
            )
            dao.insert(session)
        }

        // Reconcile with the server in case the client checkpoint is behind.
        val remoteOffset = api.getUploadStatus(session.uploadId).receivedBytes
        var offset = maxOf(session.nextChunk * chunkSize, remoteOffset)

        val fd = contentResolver.openFileDescriptor(fileUri, "r")!!
        FileInputStream(fd.fileDescriptor).use { input ->
            input.skip(offset)
            while (offset < session.totalSize) {
                val len = minOf(chunkSize, session.totalSize - offset).toInt()
                val bytes = ByteArray(len)                     // replayable chunk
                input.readFully(bytes)

                // Token is refreshed by the interceptor before the chunk is sent.
                val ack = api.putChunk(
                    uploadId = session.uploadId,
                    index = offset / chunkSize,
                    body = bytes.toRequestBody("application/octet-stream".toMediaType()),
                    contentRange = "bytes $offset-${offset + len - 1}/${session.totalSize}",
                )
                offset = ack.receivedBytes
                dao.updateProgress(session.uploadId, offset)   // checkpoint
            }
        }
        return api.completeUpload(session.uploadId).resourceId
    }
}
```

**Talking points / follow-ups**
- *"How does the `Authenticator` interact with a streaming body?"* → If the body is not replayable, OkHttp cannot retry it; you get an IOException and the upload fails.
- *"What if the server rejects a chunk?"* → Record the error, retry with backoff up to a budget; on `complete`, the server reports which chunks are missing.
- *"How do you avoid uploading twice if the app is killed?"* → Checkpoint per chunk in Room + reconcile with the server's `receivedBytes`. Chunk PUTs are idempotent, so re-sending an unacknowledged chunk is safe.
- *"Where should this run?"* → `WorkManager` with a foreground notification for long uploads, or a foreground service — a plain `viewModelScope` coroutine dies with the process.
- *"What about encrypted files?"* → Decrypt to a temp file in app-private storage and upload from there; never hold the plaintext key in memory longer than needed.

---

### 10. The app must work offline and synchronize data when connectivity returns. How would you design the architecture?

**What the interviewer is really asking**
A classic **offline-first architecture** question that also spans Sections 5 and 11. They want to see the *data flow* (single source of truth = local DB), the *sync engine* (WorkManager + conflict rules), and *honesty about trade-offs*.

**Key concepts**
- Local database as the single source of truth (SSOT); UI observes DB, never the network directly
- Unidirectional data flow: Network → DB → Flow → UI
- Write-behind / outbox pattern (local write + queued mutation)
- `WorkManager` with `NetworkType.CONNECTED` constraint
- Sync metadata: `lastSyncedAt`, per-record `version`, `updatedAt`, tombstones
- Conflict resolution: last-write-wins, version vectors, or CRDTs
- Delta sync vs full sync

**Approach**

1. **Make the local DB the single source of truth.** The UI never awaits the network for display data; it observes Room via `Flow`. This is the foundation — it makes the app work identically online and offline.
   ```kotlin
   interface TransactionRepository {
       fun observeTransactions(query: Query): Flow<List<Transaction>>
       suspend fun refresh(): SyncResult
   }
   ```

2. **Data flow:**
   ```
   UI (Compose) ← Flow ← Room (SSOT)
                      ↑
                 SyncWorker (WorkManager)
                      ↑ ↓
              Retrofit ↔ Backend
   ```
   Reads always come from Room. Writes go to Room **first** (optimistically) and are pushed to the server by the sync engine.

3. **Separate read-sync from write-sync.**
   - **Read sync** (pull): `GET /sync?since=<cursor>` returns changes. Upsert into Room. Write into Room *and* enqueue an outbox row in the same transaction.
   - **Write sync** (push): drain the outbox to the server in order; each entry carries an idempotency key and a base version.

4. **Outbox pattern for writes.** In a single Room transaction:
   ```kotlin
   @Transaction
   suspend fun applyLocalEdit(tx: Transaction): Long {
       val updated = txDao.update(tx)                       // 1. local write
       outboxDao.insert(OutboxRow(                         // 2. queue the push
           id = UUID.randomUUID().toString(),
           entityType = "transaction",
           entityId = tx.id,
           operation = Operation.UPDATE,
           payload = tx.toJson(),
           baseVersion = tx.version,
           idempotencyKey = UUID.randomUUID().toString(),
           createdAt = now(),
       ))
       return updated
   }
   ```
   The outbox is durable → survives process death → no write is lost.

5. **Sync engine with `WorkManager`.**
   ```kotlin
   val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
       .setConstraints(Constraints.Builder()
           .setRequiredNetworkType(NetworkType.CONNECTED)
           .build())
       .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
       .build()
   ```
   - Use `enqueueUniqueWork` with `ExistingWorkPolicy.KEEP` (or `REPLACE`) so only one sync runs at a time — no duplicate syncs (see Section 9, Q3).
   - `SyncWorker` returns `Result.retry()` on transient failure so WorkManager applies backoff.
   - Trigger a sync on: app foreground, connectivity regained (`NetworkCallback`), user pull-to-refresh, and after a local write (if online).

6. **Conflict resolution — be explicit about the strategy.** This is the part interviewers probe.
   - **Last-write-wins** with server timestamps: simplest, works when fields are independent and users rarely edit the same field. Fails for simultaneous edits to the same field (one user's change is silently lost).
   - **Version/optimistic-concurrency:** every record has a `version`. The client sends `baseVersion`; the server rejects with `409` if it has moved on, and the client then re-reads and re-applies. This **detects** conflicts rather than silently losing data — usually the right default.
   - **Field-level merge / three-way merge:** keep the base value the client started from (`baseJson`), and merge field-by-field. Good for forms where different users edit different fields.
   - **CRDT / OT:** for true collaborative editing (text documents). Heavy; only worth it for the "same document, many users" case.
   - **Tombstones:** record deletions so a delete is not resurrected by a stale pull.

7. **Sync cursor / delta sync.** Store `lastSyncCursor` (an opaque server token, or `updatedAt + id` pair) and request only changes. Full sync only on first run or after a schema change. Use a **monotonic, server-assigned** cursor to avoid missing concurrent writes.

8. **Freshness UI.** Expose `lastSyncedAt` so the UI can show "Last updated 5 min ago" and a manual sync affordance. This builds trust and makes sync bugs visible.

9. **Storage hygiene.** TTL/cleanup of stale records, cache size limits, and a "clear cache" that does not delete user-created offline data.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Local DB as SSOT | Offline works, one code path, testable | Extra storage and write amplification; a sync bug shows stale data everywhere |
| Direct network→UI reads | Simpler, always fresh | No offline; race conditions; the classic anti-pattern |
| Last-write-wins | Simple, no conflicts surfaced | Silent data loss on concurrent edits |
| Optimistic concurrency (409) | Detects conflicts, no silent loss | Needs conflict-resolution UX; can produce conflict storms |
| Three-way merge | Preserves independent edits | Needs base state storage; complex |
| CRDT | No conflicts ever, offline-first | Big implementation cost; not for a 4-month feature |
| Full sync each time | Simple, always consistent | O(n) bandwidth; unusable at scale |
| Delta sync with cursor | Efficient | Cursor bugs cause missing/duplicated data |

**Key detail:** The sync engine must be **idempotent**. If the same sync runs twice (WorkManager retry, connectivity flap, user pull-to-refresh), applying the same delta must not duplicate rows. Use `upsert` keyed on the server id, and a monotonic cursor.

**Code example**

```kotlin
class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val db: AppDatabase,
    private val api: SyncApi,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        // 1. PUSH: drain the durable outbox (idempotent per row).
        while (true) {
            val pending = db.outboxDao().nextBatch(50) ?: break
            if (pending.isEmpty()) break
            for (row in pending) {
                val resp = api.push(
                    entityId = row.entityId,
                    payload = row.payload,
                    baseVersion = row.baseVersion,
                    idempotencyKey = row.idempotencyKey,   // safe to retry
                )
                when (resp) {
                    is PushResult.Accepted -> {
                        db.outboxDao().markDone(row.id)
                        db.txDao().applyServerVersion(row.entityId, resp.version)
                    }
                    is PushResult.Conflict -> {
                        // 2. Conflict: keep local edit, rebase on server, ask user if needed.
                        db.outboxDao().markConflict(row.id, resp.serverPayload)
                    }
                }
            }
        }

        // 3. PULL: apply the delta using an idempotent upsert.
        val cursor = db.metaDao().syncCursor()
        val delta = api.pull(since = cursor)
        db.transaction {
            delta.changes.forEach { db.txDao().upsert(it.toEntity()) }
            delta.tombstones.forEach { db.txDao().softDelete(it) }
            db.metaDao().setSyncCursor(delta.nextCursor)
        }
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Result.retry()          // WorkManager applies exponential backoff
    } catch (e: HttpException) {
        if (e.code() >= 500) Result.retry() else Result.failure()
    }
}
```

**Talking points / follow-ups**
- *"Why not use `SharedPreferences` as a cache?"* → No queries, no relations, no atomicity, no reactive observation. Room is the right tool.
- *"How do you handle the first launch offline?"* → Seed the DB with bundled/pre-cached data, and clearly show "content may be out of date".
- *"What if the sync cursor expires server-side?"* → The server must signal "cursor invalid" (e.g. 410 Gone) → client performs a full re-sync. This is a real production failure mode; plan for it.
- *"How do you test offline sync?"* → Fake the API to fail, assert the local write is visible and the outbox has a pending row; then bring the API up and assert the push drains it.
- *"How do you avoid syncing on cellular?"* → Constraint-based: `NetworkType.UNMETERED` for large syncs, `CONNECTED` for small ones. Optionally user-configurable.

---

### 11. An API returns HTTP 429 because of rate limiting. How would you handle retries?

**What the interviewer is really asking**
Do you know the **semantics of 429** (`Retry-After` header, shared vs per-client quotas) and can you implement **cooperative backoff** that doesn't punish the whole fleet? Also: does the client need to self-limit (token bucket) or just react?

**Key concepts**
- `Retry-After` header (seconds or HTTP-date)
- Exponential backoff with **full jitter**
- Token bucket / leaky bucket client-side rate limiting
- Server-side quotas vs client-side politeness
- Circuit breaker
- Distinguishing 429 (too many requests) from 503 (overloaded) and 503 with `Retry-After`

**Approach**

1. **Read `Retry-After` first.** If the server sends it, it is authoritative — do not ignore it in favour of your own backoff.
   ```kotlin
   val retryAfter = response.headers["Retry-After"]?.toLongOrNull()?.let { it * 1000 }
   ```

2. **Classify 429 as retryable but with respect.** It is retryable — but retrying immediately is wrong. The delay must be at least `Retry-After`, and the server may be enforcing a shared quota across all clients of a tenant.

3. **Exponential backoff with full jitter, capped:**
   ```kotlin
   val base = 1_000L
   val max = 60_000L
   val ceiling = min(max, base * (1L shl attempt))
   val delay = Random.nextLong(ceiling / 2, ceiling)   // full jitter
   ```

4. **Add a global "cooldown" gate, not just a per-request delay.** When the server says "slow down", the *client* should reduce its request rate for all requests, not just the one that got 429. A small token-bucket / rate limiter in the OkHttp interceptor:
   ```kotlin
   class RateLimitInterceptor(private val limiter: TokenBucket) : Interceptor {
       override fun intercept(chain: Interceptor.Chain): Response {
           limiter.acquire()                       // blocks until a token is available
           val res = chain.proceed(chain.request())
           if (res.code == 429) {
               val ra = res.headers["Retry-After"]?.toLongOrNull() ?: 1
               limiter.penalize(ra * 1000)         // pause the whole client
           }
           return res
       }
   }
   ```
   This turns a "one request waits" problem into "the client backs off globally" — much friendlier to the backend.

5. **Propagate the pause to the UI.** A 429 should surface as a distinguishable state ("Too many requests — retrying in Ns") rather than a generic error. The UI can also proactively stop auto-refresh (e.g. a polling loop) while in cooldown.

6. **Do not retry non-idempotent requests without an idempotency key** (see Q1/Q3). A 429 on a `POST /payments` must not be blindly re-fired.

7. **Respect the server's quota in your own polling.** If the app polls every 5 seconds and the quota is 60/min, the app will get 429s. Use `ETag`/`If-None-Match` so a 304 does not consume quota on many backends, and slow the poll on 429 (exponential increase of the poll interval, with jitter).

8. **Circuit breaker for sustained 429s.** After N consecutive 429s, open the circuit: stop calling the endpoint for a cooldown window, serve cached data, and surface "temporarily unavailable". This protects the battery and the backend.

9. **Respect Retry-After in `Retry-After` HTTP-date form** — parse with `DateTimeFormatter.RFC_1123_DATE_TIME` (or `DateUtils.parseHttpDate`).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Per-request backoff only | Simple | Each request independently hammers the server; the fleet gets hammered harder |
| Global client cooldown | Polite, protects backend, better UX | Adds a local state machine; can stall unrelated endpoints |
| Client-side token bucket | Prevents 429s before they happen | Needs accurate quota knowledge; can throttle unnecessarily |
| Aggressive retry | Fast recovery | Escalates the rate-limit event; may get the client/IP banned |
| Circuit breaker | Protects device and backend | Needs a good half-open probe policy |

**Key detail:** Full jitter (random between 0 and the ceiling) is measurably better than equal jitter or no jitter for avoiding retry storms — this is the AWS Architecture Blog "Exponential Backoff and Jitter" result, and citing it is a strong staff-level touch.

**Code example**

```kotlin
suspend fun <T> executeWithRetry(
    request: suspend () -> Response<T>,
    maxAttempts: Int = 4,
): Response<T> {
    var attempt = 0
    while (true) {
        try {
            val res = request()
            if (res.code() == 429) {
                val serverDelay = res.headers()["Retry-After"]
                    ?.toLongOrNull()?.times(1000)
                    ?: jitteredDelay(attempt)
                if (attempt >= maxAttempts) return res
                delay(maxOf(serverDelay, jitteredDelay(attempt)))
                attempt++
                continue
            }
            return res
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            if (attempt++ >= maxAttempts) throw e
            delay(jitteredDelay(attempt))
        }
    }
}

fun jitteredDelay(attempt: Int): Long {
    val ceiling = min(60_000L, 1_000L * (1L shl attempt))
    return Random.nextLong(ceiling / 2, ceiling)
}
```

**Talking points / follow-ups**
- *"What is the difference between 429 and 503?"* → 429 means *you* are sending too much (client-side quota); 503 means the server is overloaded/unavailable (server-side). They have different retry policies: 429 respects `Retry-After` and a client-side throttle; 503 uses standard backoff.
- *"Should the app retry a 429 immediately on the next user action?"* → A user-initiated retry should still respect the cooldown for a short window, but a manual, deliberate action can be allowed after a shorter delay with an explanation.
- *"How do you test rate limiting?"* → Fake the server to return 429 with `Retry-After: 1` and assert the number of attempts and the elapsed (virtual) time via `runTest`.
- *"Is 429 a client or server problem?"* → Both — the client caused it, so the client must be polite; but a server that 429s without `Retry-After` or with unreasonable quotas is poorly designed. Both sides have work.

---

### 12. The server processes a request, but the client cancels the coroutine before receiving the response. What happens, and how would you handle it?

**What the interviewer is really asking**
The **unknown-outcome** problem again, but focused on cancellation semantics. The interviewer wants precision about what `cancel()` actually does to an in-flight HTTP call, and the realisation that **cancellation of the client does not cancel the server**.

**Key concepts**
- Cooperative cancellation; `CancellationException`
- OkHttp `Call.cancel()` → socket closed → `IOException("Canceled")`
- Retrofit suspend adapter cancellation
- `ensureActive()` / cancellation checkpoints
- Server-side idempotency; compensation (saga) / reconciliation
- `NonCancellable` for cleanup
- Distinguish "cancel the UI observation" from "cancel the work"

**Approach**

1. **What actually happens, precisely.**
   - `coroutine.cancel()` marks the job cancelled and resumes suspended continuations with `CancellationException`.
   - The HTTP call is cancelled only if the suspending adapter propagates it to OkHttp's `Call.cancel()`. Retrofit's suspend support does this — the socket is closed and the pending read fails.
   - The **server is completely unaffected**: it has already received the request, executed the handler, and committed the change. From the server's point of view the client just disconnected.
   - The HTTP transaction may still be **committed** server-side. The response is written to a socket that no longer exists → the server sees a broken pipe, logs an error, and — importantly — many servers still commit the transaction.

2. **The client is now in an unknown state.** It cannot distinguish "server never received it" from "server committed it". This is the crux, and it is exactly the situation idempotency keys exist to solve.

3. **Handle it in three layers:**
   - **Client-side idempotency key** (see Q1): re-issuing with the same key returns the original result rather than duplicating the effect.
   - **Server-side reconciliation:** the client records the key locally and later polls `GET /v1/ops/{key}` for the terminal status. The server keeps the key→outcome mapping for the retention window.
   - **Compensation** (for long-running business operations that cannot be queried): a saga — the client issues a compensating action (e.g. `POST /v1/payments/{id}/void`) once it learns the original succeeded but the user abandoned it.

4. **Do not cancel blindly.** Not every cancellation should abort the request. Distinguish:
   - **User navigated away / screen closed** → the *observation* should stop (the Flow collection is cancelled), but the *operation* may need to continue (e.g. the payment must still complete). This is a **scope separation** problem:
     ```kotlin
     // Observation cancelled with the screen; the operation lives in the app/repository scope.
     val op = applicationScope.async { repo.performPayment(idempotencyKey) }
     // The ViewModel collects its result only while subscribed:
     op.asFlow().collect { _state.value = it }
     ```
     When the collector is cancelled, the `async` job in the app scope continues (it is not a child of the ViewModel scope).
   - **User explicitly cancelled the operation** (there is a Cancel button) → cancel the work, and reconcile afterwards if the operation may have side effects.

5. **Cleanup must still run.** Use `try/finally` and `withContext(NonCancellable)` for cleanup that must complete even when the coroutine is cancelled:
   ```kotlin
   try {
       // work
   } finally {
       withContext(NonCancellable) {
           dao.markOutcomeUnknown(opId)   // persist what we know
           closeConnection()
       }
   }
   ```
   This is exactly where a lot of production bugs live: cancellation throws, `finally` throws too, and the original exception is lost.

6. **Never assume cancel == failure.** Model the outcome as a **three-state** result: `Success`, `Failure`, `Unknown`. Only `Unknown` triggers reconciliation. This is a much better model than a boolean.

7. **Server side.** The server should also be defensive: idempotency-key dedupe, and it should not depend on the client having received the response. If the server commits a transaction and then fails to write the response, the outcome is still recorded against the key.

8. **Server-side cancellation of the work.** For expensive operations, the server can support explicit cancellation (`DELETE /ops/{key}`), but the client should only send that if the user *intended* to abort, not because the coroutine was cancelled incidentally.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Cancel the request on navigate-away | Saves bandwidth; user gets a fast response | Leaves an unknown outcome; if the op is a payment, it may still succeed → UI says "cancelled" but money moved. **Worse** for side-effecting operations |
| Keep the operation running in an app-level scope | The operation completes and its true outcome is known | Must design the scope carefully; the result must be delivered back to the UI later (StateFlow/Event) |
| Idempotency key + reconcile | Correct under all conditions | Requires backend support |
| Compensation (saga) | Reverts business effect | Compensations can fail; not all effects are reversible |
| Fire-and-forget after cancel | Simple | Loses the result entirely; user cannot know |

**Key detail:** `kotlinx.coroutines.CancellationException` is a typealias for `java.util.concurrent.CancellationException`, whose hierarchy is `CancellationException → IllegalStateException → RuntimeException → Exception`. So a `catch (e: Exception)` block *will* catch a cancellation — which is precisely why the "always rethrow `CancellationException`" rule matters everywhere in retry and error-handling code.

**Code example**

```kotlin
sealed interface OpOutcome {
    data class Succeeded(val id: String) : OpOutcome
    data class Failed(val reason: String) : OpOutcome
    data object Unknown : OpOutcome          // cancelled or timed out — must reconcile
}

class PaymentRepository(
    private val api: PaymentApi,
    private val dao: PaymentDao,
    private val appScope: CoroutineScope,     // outlives any screen
) {
    fun submit(request: PaymentRequest): Flow<OpOutcome> = flow {
        val key = dao.mintKey(request.intentId)
        var outcome: OpOutcome = OpOutcome.Unknown
        try {
            val resp = api.pay(request.amount, key)
            outcome = OpOutcome.Succeeded(resp.transactionId)
        } catch (e: CancellationException) {
            dao.markUnknown(key)
            throw e                                  // respect cancellation, but record state
        } catch (e: IOException) {
            outcome = OpOutcome.Unknown
            dao.markUnknown(key)
        } finally {
            withContext(NonCancellable) { dao.markOutcome(key, outcome) }
        }
        emit(outcome)
    }.flowOn(Dispatchers.IO)

    // Reconciliation sweep — resolves every UNKNOWN against the server.
    suspend fun reconcile(): Int {
        val unknowns = dao.findByState("UNKNOWN")
        unknowns.forEach { local ->
            val remote = runCatching { api.getPayment(local.idempotencyKey) }.getOrNull()
            if (remote != null) dao.updateFromRemote(local.idempotencyKey, remote)
        }
        return unknowns.size
    }
}
```

**Talking points / follow-ups**
- *"Does cancelling actually stop the server work?"* → No. Only if the server has its own cancellation endpoint or request-propagation mechanism.
- *"What's the difference between cancelling the Flow collection and cancelling the work?"* → The former stops the UI from receiving updates; the latter aborts the operation. Conflating them is the classic bug.
- *"How do you test this?"* → Use a fake server that sleeps; cancel the coroutine; assert the local record is `UNKNOWN`, then run `reconcile()` and assert it resolves to the server's truth.
- *"Where should the `async` live for a payment that must survive screen exit?"* → An application-scoped `CoroutineScope` (or a WorkManager job), not `viewModelScope`.

---

### 13. The backend occasionally returns inconsistent data across multiple endpoints. How would you maintain a consistent UI?

**What the interviewer is really asking**
This is the **read-consistency / "torn read"** problem, plus the question of where the fix belongs (client vs backend). A strong answer recognises that some inconsistencies are inherent to a distributed backend and must be *modelled*, not papered over.

**Key concepts**
- Snapshot/consistent-read APIs (one endpoint returns a whole aggregate)
- Read-your-writes / monotonic reads
- `ETag` / `If-None-Match`, versioned responses
- Cache coherency, cache keys including version
- BFF (backend for frontend) aggregation
- Eventual consistency and how the UI communicates it
- Local cache as the consistency point

**Approach**

1. **First diagnose: what kind of inconsistency is it?**
   - *Torn read within one response* → a backend bug (missing transaction/isolation).
   - *Across endpoints* → the endpoints read different replicas/snapshots; classic eventual consistency.
   - *Client-side* → the client made two calls at different times and stitched them together.
   These need different fixes; the answer should start by naming which one it is.

2. **Best fix: reduce the number of endpoints.** If a screen needs a consistent view of 3 related entities, ask the backend for **one endpoint returning a consistent aggregate** (or a BFF that assembles it server-side under one transaction/snapshot). This is the highest-leverage change and the one interviewers want to hear first. Trade-off: the backend team must do work; you may need a versioned contract.

3. **If you must call multiple endpoints, make the reads consistent from the client's perspective:**
   - **Read from a single source** — e.g. always read related data from the local cache rather than mixing a fresh network response with a cached one.
   - **Pin to a version/token**: if the backend exposes a `snapshotToken` or ETag per aggregate, fetch it once and pass it to subsequent reads so they read the same snapshot.
   - **Fetch in one logical pass** and render atomically — never render half-updated data. Buffer the responses and publish them together:
     ```kotlin
     // Publish atomically: the UI never sees a half-updated aggregate.
     val combined = combine(profileFlow, ordersFlow) { p, o -> HomeState(p, o) }
     ```
   - **Use a single ViewModel state object** rather than many independent `StateFlow`s that update at different times. Independent flows = torn UI. This is a Compose-relevant point that connects to Section 6 (modelling UI state as one immutable object).

4. **Design for eventual consistency explicitly, and communicate it.**
   - Mark data with its freshness: `updatedAt` per section; the UI shows "updated 2 min ago".
   - Do not show contradictory states without qualification. If the balance says ₹5,000 but the transaction list does not yet include the ₹500 transfer, show a "syncing" indicator rather than silently inconsistent numbers.
   - In banking-like UIs, consistency of *balances* matters most — treat those as a separate, higher-consistency path (often a dedicated, strongly-consistent endpoint).

5. **Use the local DB as the consistency boundary.** With an offline-first design, the client writes a batch of related entities in a **single Room transaction** so the UI never observes a partial write:
   ```kotlin
   db.transaction {
       balanceDao.update(newBalance)
       txDao.insert(newTransaction)
       auditDao.insert(entry)
   }
   ```
   Room's transaction guarantees atomicity and invalidates the observing `Flow`s together — the UI receives one consistent emission.

6. **Validate and reconcile on the client where cheap.** E.g. if `sum(transactions) != balance`, flag it (a "reconciliation check") and re-fetch. In banking, a reconciliation mismatch is a bug worth surfacing rather than hiding.

7. **Use conditional requests to avoid mixing versions.** Send `If-None-Match` with the ETag you hold; the server returns 304 if unchanged, so your cache never mixes an old entity with a new one. Include the version in the cache key.

8. **Add observability.** Log the aggregate `snapshotId`/versions returned by each endpoint for one screen load. When inconsistency is reported, you can see whether the endpoints returned different snapshots. This is a concrete, high-value diagnostic that most candidates miss.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| One aggregate endpoint | Guarantees consistency, fewer round trips, fewer client states | Backend work; the endpoint can become a god endpoint; less flexible for partial loading |
| BFF layer | Client stays simple; server owns aggregation | Extra component to deploy/operate; another hop |
| Client-side atomic publish | No backend change | Still shows whatever the server returned; if the server itself is torn, the client is consistent but wrong |
| Local DB transaction boundary | Atomic, reactive, works offline | Client can only be as consistent as the data it received |
| Showing "as of X" freshness | Honest, sets user expectations | Does not fix the inconsistency |
| Eventual consistency + refresh | Keeps the system scalable | Requires careful UX design; users may perceive bugs |

**Key detail:** "Inconsistent across endpoints" is often a *reporting/observability* problem before it is a correctness problem. Adding a `snapshotId` to responses and logging it per screen load frequently reveals that the endpoints are hitting different read replicas — and that the backend can fix it with a single read-only transaction without any client change.

**Code example**

```kotlin
// One atomic state object → one consistent emission → no torn UI.
data class HomeState(
    val profile: Profile,
    val balance: Balance,
    val recentTransactions: List<Transaction>,
    val asOf: Instant,
)

class HomeViewModel(
    private val dao: HomeDao,
    private val repo: HomeRepository,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(
        dao.observeProfile(),
        dao.observeBalance(),
        dao.observeTransactions(limit = 20),
    ) { profile, balance, txs ->
        HomeState(profile, balance, txs, asOf = Instant.now())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState.Loading)

    fun refresh() = viewModelScope.launch {
        repo.refreshAtomically()   // one backend aggregate + one Room transaction
    }
}

// The repository writes all related entities in ONE Room transaction.
suspend fun applySnapshot(snapshot: HomeSnapshot) {
    db.withTransaction {
        profileDao.upsert(snapshot.profile)
        balanceDao.upsert(snapshot.balance)
        txDao.upsertAll(snapshot.transactions)
    }
}
```

**Talking points / follow-ups**
- *"How do you prove it is a backend problem and not yours?"* → Log a per-screen-load correlation id plus the `snapshotId`/ETag of each entity; compare across devices and endpoints.
- *"What about read-your-writes after a mutation?"* → After a write, prefer the local DB (which you just wrote) over the server for reads, until the push is confirmed; or ask the backend for a `read-your-writes` guarantee via a version token.
- *"Should we cache aggressively to reduce cross-endpoint skew?"* → Caching reduces load but can *increase* skew if one entity is cached and another is fresh. Align cache TTLs and use a shared snapshot token.
- *"How do you test for consistency?"* → Property-based tests that assert invariants (e.g. `balance == sum(transactions)`) against the seeded DB, plus fake-server tests that return deliberately mismatched snapshots and assert the UI shows the "as of" qualifier.

---

### 14. A user starts an operation on one device and continues on another. How would you handle synchronization and conflicting updates?

**What the interviewer is really asking**
Cross-device continuity. This probes whether you can reason about **identity, session handoff, and distributed conflict resolution** — and whether you understand that "continue on another device" usually means **server-side state**, with the client being a stateless view of it.

**Key concepts**
- Server-side operation state (the operation lives on the server, not the device)
- Session/account-scoped resources, not device-scoped
- Idempotency keys that are *server-issued* (so both devices share the same logical operation)
- Optimistic concurrency with version numbers / ETags
- Conflict resolution policies (LWW, three-way merge, manual)
- Push/Firebase for cross-device invalidation
- Device-scoped vs account-scoped state (e.g. "selected bank account" may be device-scoped; "transaction draft" should be account-scoped)

**Approach**

1. **Make the operation server-side.** The correct architecture for "start on device A, continue on device B" is that the **operation is a server resource**:
   ```
   POST /v1/transactions  → { operationId, status: DRAFT }   // created on device A
   GET  /v1/transactions/{operationId} → device B reads the same operation
   PATCH /v1/transactions/{operationId} → device B continues editing
   ```
   The device only holds a **pointer** (`operationId`) plus a local cache. This eliminates the hardest version of the conflict problem: two devices editing *different* copies of a draft.

2. **Derive the idempotency key from the operation, not the device.** If device A creates operation `op-123`, and device B resumes it, both must use the same key when submitting. Therefore the key should be `operationId` (server-issued) or a client-side `clientRequestId` generated at operation-creation time and stored server-side.

3. **Handle session handoff.** The user must be authenticated on device B. Options:
   - **Same account, separate sessions**: simplest; device B logs in and can access the account-scoped operation.
   - **Account switching / multi-account**: keep operations tagged by `accountId`; ensure the token for account X never exposes account Y's operations.
   - **Handoff without re-login**: a "continue on another device" QR/OTP flow that transfers a short-lived, scoped handoff token.

4. **Detect conflicts with optimistic concurrency.** Every server resource has a `version` (or ETag). The client sends the version it read; the server compares:
   ```http
   PATCH /v1/transactions/op-123
   If-Match: "v7"
   ```
   - If the server's current version is still 7 → apply, bump to 8, return 200.
   - If it is 8 → **412 Precondition Failed**; return the current server state so the client can merge or ask.

5. **Choose a conflict policy per field type.** This is where staff-level answers differentiate:
   - **Monotonic/immutable fields** (amount, createdAt, account number): never auto-merge — surface a conflict.
   - **Independently-editable fields** (notes, category): field-level three-way merge using the base version the client started from.
   - **Text documents**: CRDT or OT (e.g. Yjs) for true collaborative editing.
   - **Last-write-wins** is acceptable only for low-value fields (e.g. a "last viewed" timestamp) and must be based on **server** time, not device clock (device clocks are unreliable).

6. **Invalidate the other device's cache.** Device B continues on device A's stale cache otherwise. Use:
   - **Push notification** (Firebase Cloud Messaging data message) → device A receives an invalidation for `op-123` → it re-fetches or applies the delta.
   - **Polling with ETags** as a fallback when FCM is unavailable.
   - **Delta feed** (`GET /sync?since=cursor`) as the general mechanism.

7. **Show a human-readable conflict UI.** If a true conflict occurs (two people set different amounts), the app should show both values, label which device changed what, and let the user choose — do not silently pick one.

8. **Offline on device A while device B continues.** The general rule: **the server arbitrates**. Device A's offline edits carry a `baseVersion`; when it reconnects, the server either accepts them (if the version is unchanged) or returns a conflict. The client then re-syncs and resolves.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Operation lives on the server | Cross-device continuity works naturally; one source of truth | Requires server-side draft support; a draft is now network-dependent (mitigate with a local cache) |
| Draft lives locally, sync on switch | Works offline; simpler | Cross-device continuation is manual and fragile; conflicts are client-side |
| Optimistic concurrency (`If-Match`) | No silent data loss; precise | Needs a conflict UX; can cause frequent conflicts with many users |
| Last-write-wins | Simple, always converges | Silently loses one user's edit — unacceptable for money/amount fields |
| Field-level merge | Preserves independent edits | Requires keeping base state; complex merge rules |
| CRDT/OT | No conflicts; true real-time collaboration | Significant complexity; not justified for most apps |
| Push invalidation | Instant cross-device consistency | FCM may be delayed/unavailable; needs a polling fallback |

**Key detail:** Device clocks are not trustworthy. Any conflict resolution that compares timestamps **must** use server-assigned timestamps (or a monotonic server version), never `System.currentTimeMillis()` from two devices.

**Code example**

```kotlin
// Client: send the version we based our edit on; let the server arbitrate.
suspend fun updateOperation(op: Operation): UpdateResult {
    val res = api.patchOperation(
        id = op.id,
        ifMatch = op.version,                 // "v7"
        changes = op.changes,
    )
    return when (res.code()) {
        200, 204 -> UpdateResult.Applied(res.newVersion)
        412 -> UpdateResult.Conflict(serverState = res.body())   // surface to the user
        else -> UpdateResult.Failed
    }
}

// Conflict resolution helper: merge by field policy.
sealed interface MergePolicy {
    object LastWriteWins : MergePolicy                 // only for low-value fields
    object Reject : MergePolicy                        // for amount, account, dates
    object FieldLevel : MergePolicy                    // for notes, category, tags
}

fun merge(base: Op, local: Op, remote: Op, policy: MergePolicy): Op = when (policy) {
    MergePolicy.Reject -> local
    MergePolicy.LastWriteWins -> if (remote.version > local.version) remote else local
    MergePolicy.FieldLevel -> local.copy(
        notes = base.notes.merged(local.notes, remote.notes),
        category = base.category.merged(local.category, remote.category),
        amount = remote.amount,       // sensitive field: server wins, flagged as changed
    )
}
```

**Talking points / follow-ups**
- *"How do you know it is the same user on device B?"* → Authentication (see Section 8). Operations are scoped to the account, and cross-account leakage must be impossible.
- *"What if the operation is not in the account yet?"* → The server creates a "pending handoff" record when device A explicitly shares, and device B claims it with a one-time code.
- *"Does the server hold a full lock on the operation?"* → No — a soft, version-based optimistic lock. Pessimistic locks are hostile to mobile: the device may vanish mid-edit.
- *"How do you test this?"* → Simulate two clients against a fake server: A creates v1, B patches to v2, A patches with `If-Match: v1` → assert 412 and that the client surfaces the conflict.

---

### 15. How would you design a centralized network error-handling mechanism for the entire application?

**What the interviewer is really asking**
This is a **staff-level architecture** question. They want a coherent, layered error-handling design — not a `try/catch` in every repository. The markers are: typed errors, a single mapping to UI state, centralised logging/telemetry, and per-layer responsibility (transport / repository / UI).

**Key concepts**
- Typed error hierarchy (`sealed interface AppError`)
- Result type vs exceptions (`kotlin.Result` is fine for expected errors, exceptions for programmer errors)
- Global error handler (`CoroutineExceptionHandler` + `Thread.setDefaultUncaughtExceptionHandler`)
- Error → UI state mapping (`UiText` with string resources)
- Interceptor chain: logging, auth, retry, cache, error mapping
- Network availability abstraction
- Analytics/crash reporting integration
- `IOException` vs `HttpException` vs domain errors

**Approach**

1. **Layered responsibilities:**
   ```
   ┌──────────────────────────────────────────────────────┐
   │ UI (Compose/View)   → renders UiState, triggers retry  │
   ├──────────────────────────────────────────────────────┤
   │ ViewModel           → maps errors to UiState          │
   ├──────────────────────────────────────────────────────┤
   │ Repository          → domain errors, no HTTP types    │
   ├──────────────────────────────────────────────────────┤
   │ Data source / Retrofit → maps HTTP/IO to typed errors │
   └──────────────────────────────────────────────────────┘
   ```
   The rule that makes this work: **HTTP and IO types do not escape the data layer.** Repositories speak in domain terms; the ViewModel maps domain errors to UI state.

2. **Define a typed error hierarchy** in one place:
   ```kotlin
   sealed interface AppError {
       data class NetworkUnavailable(val cause: Throwable? = null) : AppError
       data class Timeout(val timeoutMs: Long) : AppError
       data class Http(val code: Int, val body: String?, val cause: Throwable? = null) : AppError
       data class Unauthorized(val cause: Throwable? = null) : AppError
       data class Conflict(val serverState: String) : AppError
       data class Server(val code: Int) : AppError
       data class Parsing(val cause: Throwable) : AppError
       data class Unknown(val cause: Throwable) : AppError
   }
   ```
   Each error carries what the UI needs: whether to show a retry button, whether it is fatal, whether to prompt re-login, and a user-facing message key.

3. **Map at the data layer, once.** A Retrofit `CallAdapter` or a repository-level helper converts `IOException`/`HttpException`/parse failures into `AppError`:
   ```kotlin
   suspend fun <T> safeApiCall(block: suspend () -> Response<T>): AppResult<T> = try {
       val res = block()
       if (res.isSuccessful) AppResult.Success(res.body()!!)
       else AppResult.Failure(res.toHttpError())
   } catch (e: CancellationException) {
       throw e
   } catch (e: SocketTimeoutException) {
       AppResult.Failure(AppError.Timeout(timeoutMs = 30_000))
   } catch (e: IOException) {
       AppResult.Failure(AppError.NetworkUnavailable(e))
   } catch (e: Exception) {
       AppResult.Failure(AppError.Unknown(e))
   }
   ```

4. **Centralise at the OkHttp interceptor chain** for cross-cutting concerns:
   ```kotlin
   OkHttpClient.Builder()
       .addInterceptor(LoggingInterceptor(...))        // redacts PII, honours BuildConfig.DEBUG
       .addInterceptor(AuthInterceptor(tokenManager))
       .addInterceptor(RateLimitInterceptor(tokenBucket))
       .addNetworkInterceptor(ConnectivityInterceptor())  // no network → fail fast
   ```
   Do **not** put business error mapping in an interceptor — interceptors cannot construct domain errors, and they see the wire representation only.

5. **Map errors to UI state in the ViewModel**, not in the UI. The UI renders a state; it does not interpret exceptions:
   ```kotlin
   sealed interface UiState<out T> {
       data object Loading : UiState<Nothing>
       data class Success<T>(val data: T) : UiState<T>
       data class Empty : UiState<Nothing>
       data class Error(val error: AppError) : UiState<Nothing>
   }
   ```
   The UI then decides the *presentation* by type: `NetworkUnavailable` → "You're offline" + retry; `Unauthorized` → redirect to login; `Timeout` → "Taking too long" + retry; `Server` → "Something went wrong" + retry.

6. **A global safety net for uncaught exceptions.** For coroutines launched outside a ViewModel scope (application scope, `launch` on a `GlobalScope`-like scope), install a `CoroutineExceptionHandler`; for truly uncaught throwables, a `Thread.setDefaultUncaughtExceptionHandler` that reports to the crash reporter and then delegates to the previous handler. Never swallow — report and rethrow.

7. **Telemetry.** Every error should carry a `correlationId` that also appears in server logs, so a client error can be traced to a backend trace. Report to Crashlytics/Bugsnag with breadcrumbs (endpoint, status code, attempt count) — but **scrub PII** (see Section 8, Q5).

8. **A "no network" pre-check** so that offline is reported as such instead of a generic timeout. Use the `ConnectivityObserver` from Q6, but treat it as a *hint*, not ground truth — the request outcome is authoritative.

9. **Configuration over hardcoding.** Base URLs, timeouts, retry counts, feature flags → `BuildConfig` / remote config, so behaviour can be tuned in production without a release.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Typed error hierarchy | Exhaustive `when`, self-documenting, testable | More types to maintain; verbose `when` branches |
| Exceptions instead of `Result` | Ergonomic happy path | Easy to forget to catch; runtime cost; `Result` is discouraged in public APIs by Kotlin convention (inline class) but widely used internally |
| Global uncaught handler | Nothing escapes | Hard to test; must delegate to the default handler or the app "swallows" crashes |
| Logging interceptor | Central, consistent | Must redact; heavy if not sampled |
| Remote-configurable timeouts/retries | Tunable in production | More moving parts; a bad remote value can break the app (needs validation and rollback) |
| Mapping errors in the UI | Quick to implement | Duplicated logic across screens; inconsistent UX |

**Key detail:** "Centralized" does not mean "one giant try/catch". It means **one place that defines the error vocabulary and one place that maps it to presentation**, with the layers in between doing their own job.

**Code example**

```kotlin
// 1) Data layer: wire → domain
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

suspend fun <T> apiCall(block: suspend () -> Response<T>): AppResult<T> = try {
    val res = block()
    if (res.isSuccessful) AppResult.Success(res.body()!!)
    else AppResult.Failure(
        when (res.code()) {
            401, 403 -> AppError.Unauthorized()
            409 -> AppError.Conflict(res.errorBody()?.string().orEmpty())
            429 -> AppError.Http(429, retryAfter = res.headers()["Retry-After"]?.toIntOrNull())
            in 500..599 -> AppError.Server(res.code())
            else -> AppError.Http(res.code(), res.errorBody()?.string())
        }
    )
} catch (e: CancellationException) {
    throw e
} catch (e: SocketTimeoutException) {
    AppResult.Failure(AppError.Timeout(30_000))
} catch (e: IOException) {
    AppResult.Failure(AppError.NetworkUnavailable(e))
}

// 2) ViewModel: domain → UI state
class TransactionsViewModel(private val repo: TransactionRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<List<Transaction>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Transaction>>> = _state.asStateFlow()

    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        when (val r = repo.getTransactions()) {
            is AppResult.Success -> _state.value =
                if (r.data.isEmpty()) UiState.Empty else UiState.Success(r.data)
            is AppResult.Failure -> _state.value = UiState.Error(r.error)
        }
    }
}

// 3) UI: state → presentation (no error interpretation logic here)
@Composable
fun TransactionsScreen(vm: TransactionsViewModel) {
    when (val s = vm.state.collectAsStateWithLifecycle().value) {
        UiState.Loading -> CircularProgressIndicator()
        UiState.Empty -> EmptyState()
        is UiState.Success -> TransactionList(s.data)
        is UiState.Error -> ErrorState(error = s.error, onRetry = vm::load)
    }
}
```

**Talking points / follow-ups**
- *"Why not just throw `IOException` and catch it in the UI?"* → It leaks transport concerns into the UI, forces the UI to know about HTTP codes, and makes testing brittle.
- *"Where does the 'no network' state come from?"* → Ideally from the repository, using both the connectivity hint and the actual failure. Never trust connectivity state alone.
- *"How do you make this testable?"* → The repository returns `AppResult`; the ViewModel test asserts the `UiState` sequence. No mocking of OkHttp needed.
- *"How do you keep messages localised?"* → Errors carry a `UiText` (a sealed `StringRes` / `DynamicString`) rather than a raw `String`, so the ViewModel does not reference `Context`.
- *"What about analytics on errors?"* → Report a sanitised error *type* and endpoint, never the body or the message.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Idempotency & the unknown outcome** | 1, 2, 3, 9, 12 — the single most important thread in this section |
| **Cancellation semantics** | 3, 7, 9, 12 — cooperative cancellation, `CancellationException`, scope separation |
| **Retry, backoff and politeness** | 7, 8, 11, 12 — jitter, `Retry-After`, budgets, idempotency requirement |
| **Single source of truth & consistency** | 4, 10, 13 — local DB as the consistency boundary; atomic publish |
| **Cross-cutting infrastructure** | 6, 8, 10, 15 — interceptors, connectivity, sync engine, error vocabulary |
| **Staff framing** | 10, 13, 14, 15 — architecture, trade-offs, and where the fix belongs |

**The one sentence to memorise for this section:**
> *"On the client, every outcome is one of three — success, failure, or unknown — and only a server-side idempotency key plus reconciliation can convert an unknown into a truth. Everything else — loading states, retry buttons, optimistic UI — is presentation on top of that contract."*
