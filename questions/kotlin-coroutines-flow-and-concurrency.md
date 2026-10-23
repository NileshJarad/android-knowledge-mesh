# Kotlin Coroutines, Flow and Concurrency

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. Two coroutines update the same shared variable simultaneously. How would you prevent race conditions?

**What the interviewer is really asking**
Whether you understand that a **race condition is a logical bug, not a performance bug** — and whether you can pick the *right* synchronisation primitive for the *shape* of the invariant. A junior answer is "make it `@Volatile`". A staff answer classifies the shared state: single-writer counters want atomics, multi-step invariants want a `Mutex`, and anything that can be modelled as *state* should be a `StateFlow` that nobody mutates directly.

**Key concepts**
- Java Memory Model: no happens-before edge ⇒ no visibility guarantee (the classic `count++` lost update)
- `@Volatile` = **visibility only**, not atomicity
- `AtomicInteger` / `AtomicLong` / `AtomicReference` + `compareAndSet` / `updateAndGet` (CAS loop)
- `kotlinx.coroutines.sync.Mutex` — suspension-aware mutual exclusion, **not reentrant**
- Thread confinement / actor pattern: `Dispatchers.Default.limitedParallelism(1)` or `newSingleThreadContext`
- Immutable state + `MutableStateFlow` as the single writer (the "no shared mutable state" answer)
- `kotlinx.atomicfu` for `AtomicReference` in KMP

**Approach**

1. **Diagnose before synchronising.** Ask: what is the invariant? Three classes of problem need three different fixes.
   - *Lost update on a single field* (counters, flags, balances) → **atomics**.
   - *Compound invariant across two fields* ("balance ≥ 0 **and** balance + amount ≤ limit") → **`Mutex`**, because a CAS on one field cannot make the pair atomic.
   - *Multi-step business transition* (read → validate → write → log) → **single-writer confinement**: route all writes through one coroutine/actor so there is no interleaving at all.

2. **Prefer eliminating the shared mutable variable.** The strongest answer is "there is no race because there is only one writer". In UI code that means:
   ```kotlin
   private val _state = MutableStateFlow(UiState())
   val state: StateFlow<UiState> = _state.asStateFlow()
   ```
   Every mutation goes through `_state.update { it.copy(...) }`, and `update` is a CAS loop — it is atomic *and* it removes the read-modify-write race entirely.

3. **Use atomics for genuinely independent counters.** `AtomicLong.incrementAndGet()` is atomic; `count++` on a plain `Long` is not. For compound updates use `updateAndGet`/`update` rather than `get`-then-`set`:
   ```kotlin
   val total = AtomicLong(0)
   total.updateAndGet { it + amount }   // atomic read-modify-write
   ```
   For complex state use `AtomicReference<T>` with `compareAndSet` in a loop:
   ```kotlin
   fun updateBalance(delta: Long) {
       while (true) {
           val prev = balance.get()
           val next = prev + delta
           if (balance.compareAndSet(prev, next)) return
       }
   }
   ```

4. **Use a `Mutex` for compound invariants**, and remember: `Mutex.withLock` returns the block's value, and `Mutex` is **not reentrant** — calling `withLock` on the same mutex inside another `withLock` on it deadlocks (the second call suspends forever waiting for a lock the first will never release).
   ```kotlin
   private val mutex = Mutex()
   private var balance = 0L
   suspend fun withdraw(amount: Long): Boolean = mutex.withLock {
       if (balance < amount) return@withLock false
       balance -= amount
       true
   }
   ```

5. **Confine when the critical section is long.** If a mutation involves I/O (a DB write, a network call), holding a `Mutex` across it serialises everything and creates the contention of Q14. Move the I/O *out* of the lock, then re-validate inside it.

6. **Pick the right dispatcher.** `Dispatchers.IO` for blocking I/O, `Dispatchers.Default` for CPU-bound work, `Dispatchers.Main` for UI. `Dispatchers.IO` is elastic (64+ threads) so it does *not* serialise your code; `limitedParallelism(1)` is how you deliberately create a single-threaded actor.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| `@Volatile` only | Cheap, one-line | **No atomicity** — `count++` is still a lost update. A common wrong answer |
| `AtomicInteger` / `AtomicLong` | Lock-free, fast, no coroutine suspension | Only for single-field updates; a CAS loop is not a general transaction |
| `Mutex.withLock` | Correct for multi-field invariants; suspends instead of blocking a thread | Not reentrant; contention serialises callers; must not be held across I/O |
| `ReentrantLock` | Reentrant, has `tryLock`/`fairness` | **Blocks the thread** — bad on a limited dispatcher, and cannot be used from a suspending function without blocking |
| Single-writer confinement (`limitedParallelism(1)`) | No locks at all; impossible to interleave | Serialises all work behind one thread; the slowest task blocks everything |
| Immutable state + `StateFlow.update` | Removes the shared mutable variable; testable | Still a CAS internally; verbose for large state objects |

**Key detail to mention:** `kotlinx.coroutines.sync.Mutex` is **not** reentrant, and `Semaphore` with permits=1 is not either — the deadlock is a guaranteed hang, not an exception. `ReentrantLock` *is* reentrant, which is exactly why mixing the two mental models is dangerous.

**Code example**

```kotlin
class Account(private val initial: Long = 0) {

    // Compound invariant: balance must stay >= 0 and the two fields must move together.
    private val mutex = Mutex()
    private var _balance = initial
    private var _version = 0L

    // Independent counter: an atomic is enough — no lock needed.
    private val requestCount = AtomicLong(0)

    val balance: Long get() = mutex.withLock { _balance }   // suspends rather than blocks

    suspend fun debit(amount: Long): Boolean = mutex.withLock {
        if (_balance < amount) return@withLock false
        _balance -= amount
        _version++
        true
    }

    suspend fun credit(amount: Long) = mutex.withLock {
        _balance += amount
        _version++
    }

    fun onRequest() { requestCount.incrementAndGet() }
    val requests: Long get() = requestCount.get()
}

// Preferred alternative for UI state: one writer, CAS-based update, no explicit lock.
private val _uiState = MutableStateFlow(UiState(balance = 0L))
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

fun applyDelta(delta: Long) {
    _uiState.update { current ->            // atomic read-modify-write
        current.copy(balance = current.balance + delta)
    }
}
```

**Talking points / follow-ups**
- *"Is `synchronized` acceptable?"* → Yes for a pure-JVM, non-suspending critical section, but it **blocks the thread** and must never wrap a suspension point (you would hold the monitor across a suspension and deadlock other coroutines).
- *"What about `AtomicReference` with a copy-on-write data class?"* → Yes — `AtomicReference<UiState>` with `compareAndSet` is the manual version of `MutableStateFlow.update`.
- *"How would you find the race?"* → Thread-sanitizer / `kotlinx-atomicfu` instrumentation, or stress the code with many coroutines under `Dispatchers.Default` and assert the final invariant.
- *"Does `Dispatchers.Main.immediate` help?"* → It only changes *when* the code runs (immediately if already on the main thread) — it is not a synchronisation mechanism.

---

### 2. Multiple API requests need to run in parallel, but one request fails. How would you decide between coroutineScope and supervisorScope?

**What the interviewer is really asking**
This is the **structured concurrency** question. The interviewer wants to know if you understand that `coroutineScope` and `supervisorScope` differ *only* in failure propagation, and — more importantly — that neither one catches the exception for you. A weak answer says "use `supervisorScope` so the others keep going"; a strong answer explains the three outcomes you must model (all-success / partial-success-with-one-failure / total-failure) and picks the scope to match the *semantics of the screen*, not by preference.

**Key concepts**
- `coroutineScope` — a child failure **cancels siblings and rethrows** to the parent
- `supervisorScope` — a child failure does **not** cancel siblings; the scope only completes when all children finish
- `async` vs `launch`: `async` stores the exception in the `Deferred`; it is rethrown **only at `await()`**
- `awaitAll` — rethrows the first failure and cancels the rest (it is *not* a "collect all successes")
- `CoroutineExceptionHandler` — only honoured for **root** coroutines of a scope; **never** for `async` (the exception is captured in the `Deferred`)
- `SupervisorJob` — a scope-level equivalent of `supervisorScope`
- `coroutineScope`/`supervisorScope` are *suspend* functions ⇒ they can only be called from a coroutine; `SupervisorJob` is a `Job` you pass to a `CoroutineScope`
- `CancellationException` is rethrown by both scopes — neither one swallows it

**Approach**

1. **Decide by the semantics of the operation.**
   - *All-or-nothing* (a checkout needs the cart, the price, and the stock — all from the same snapshot) → **`coroutineScope`**. If any leg fails, the whole thing must abort; proceeding with half the data is a correctness bug.
   - *Independent parts of a screen* (profile, orders, promos) → **`supervisorScope`**, plus a per-child `runCatching` so each section can report its own error.

2. **Know precisely what `coroutineScope` does on failure.** The first child to fail **cancels the other children** (their suspensions resume with `CancellationException`) and then the exception propagates out of `coroutineScope`. This is a feature: it prevents wasted work on a doomed operation.

3. **Know precisely what `supervisorScope` does on failure.** Failing children do not cancel siblings, and the exception **does not propagate out of `supervisorScope`** — it is stored in the child's `Deferred`/`Job`. If you never `await()` the `async`, the failure is silently swallowed. This is the trap: `supervisorScope { async { boom() } }` completes "successfully".

4. **Therefore, under `supervisorScope` you must observe every child.** Either `await()` each one inside a `try/catch`, or wrap the child body itself:
   ```kotlin
   supervisorScope {
       val p = async { runCatching { api.profile() } }
       val o = async { runCatching { api.orders() } }
       val r = p.await()   // never throws now: runCatching captured it
       val s = o.await()
   }
   ```
   Note the alternative shape — `async { runCatching { ... } }` keeps the failure *inside* the result; `async { try { ... } catch { ... } }` is equivalent.

5. **Use `CoroutineExceptionHandler` correctly.** It only fires for a **root** coroutine launched in a scope with that handler installed:
   ```kotlin
   val handler = CoroutineExceptionHandler { _, t -> log.crash(t) }
   scope.launch(handler) { doWork() }        // handled ✔
   scope.launch { runCatching { doWork() } } // handled by runCatching ✔
   scope.async { doWork() }                   // NOT handled by the scope ✘ — throws at await()
   ```
   Under `supervisorScope`, a failing `launch` child also does **not** trigger the parent's handler — it goes to the scope's own handler, if any. So for supervised children you must catch explicitly.

6. **Combine with timeouts.** Both scopes support `withTimeout`; a timed-out child is a `TimeoutCancellationException` (a `CancellationException`), so it cancels siblings in `coroutineScope` but not in `supervisorScope`.

7. **Measure the outcome you want.** A good staff answer ends with: "I use `coroutineScope` for transactional-ish operations and `supervisorScope` for fan-out dashboards, and in the dashboard case every child is individually `runCatching`-wrapped and rendered as its own state — I never surface a raw exception to the UI."

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| `coroutineScope` | Fail-fast; no wasted server work; exception propagates naturally | One flaky optional call kills the whole screen |
| `supervisorScope` | Siblings survive; partial results usable | **Failures are silently swallowed** unless each child is awaited/caught; easier to get wrong |
| `supervisorScope` + per-child `runCatching` | Full per-part error modelling | Verbose; the caller must aggregate the `Result`s into a UI state |
| `awaitAll` | Concise parallel fetch | **Rethrows the first failure and cancels the rest** — it loses the successes; wrong for partial-failure screens |
| `awaitAll` inside `supervisorScope` | Waits for all children to settle | Still rethrows the first failure; wrap each child in `runCatching` first |
| `CoroutineExceptionHandler` | Central logging for root coroutines | **Ignored for `async`**; a common misconception — a missing `await()` hides the bug |
| `SupervisorJob` in a long-lived scope | Scope-level isolation | Applies to *all* children; too coarse for one call |

**Key detail:** `awaitAll` is the single most misapplied API here. Because each `async` inside a `supervisorScope` stores its exception rather than propagating it, `awaitAll` rethrows the first one — and since `awaitAll` uses `await` on each in turn, the remaining `Deferred`s are **not** cancelled (their exceptions remain captured and are simply dropped). Wrap each child in `runCatching` if you want to keep partial results.

**Code example**

```kotlin
// All-or-nothing: an order must have price, stock and shipping or nothing.
suspend fun placeOrder(cart: Cart): Order = coroutineScope {
    val price = async { pricing.priceFor(cart) }
    val stock = async { inventory.reserve(cart) }
    val ship  = async { shipping.quote(cart.address) }
    Order(price.await(), stock.await(), ship.await())   // any failure cancels the others
}

// Fan-out screen: each section is independent and reports its own outcome.
suspend fun loadHome(): HomeState = supervisorScope {
    val profile = async { runCatching { api.profile() } }      // failure captured, not propagated
    val orders  = async { runCatching { api.orders() } }
    val promos  = async { runCatching { api.promos() } }

    HomeState(
        profile = profile.await().fold(onSuccess = { Section.Ok(it) }, onFailure = { Section.Err(it) }),
        orders  = orders.await().fold(onSuccess = { Section.Ok(it) }, onFailure = { Section.Err(it) }),
        promos  = promos.await().fold(onSuccess = { Section.Ok(it) }, onFailure = { Section.Err(it) }),
    )
}

// The scope-level equivalent: a SupervisorJob makes every child in the scope supervised.
val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, t -> log.crash(t) })
```

**Talking points / follow-ups**
- *"What happens to siblings when one `async` fails under `coroutineScope`?"* → They are cancelled and resume with `CancellationException`; the original exception is rethrown out of the scope.
- *"Why does `async` behave differently from `launch` on failure?"* → `async` captures the exception into the `Deferred` for later `await()`; `launch` reports it to the parent scope's handler (and cancels the parent unless supervised).
- *"Can `supervisorScope` be used inside `launch`?"* → Yes — it is a suspend function and creates a nested scope; the failure isolation is scoped to that block.
- *"How do you test which children failed?"* → Fake the API to fail one endpoint; assert the other sections still render and the failed one shows an inline retry.
- *"Does `runCatching` break cancellation?"* → **Yes, it catches `CancellationException` too.** Inside a coroutine, always rethrow it explicitly.

---

### 3. A coroutine is cancelled while executing a long-running operation. How would you ensure proper cleanup?

**What the interviewer is really asking**
Whether you understand that **cancellation is cooperative and unwinds via `finally`**, that `CancellationException` must never be swallowed, and that cleanup which itself suspends needs `withContext(NonCancellable)`. The staff-level extension: how do you guarantee that the *durable* side-effect of an operation is recorded even though the operation was aborted half-way?

**Key concepts**
- Cooperative cancellation: `Job.cancel()` marks the job cancelled; suspension points (`delay`, `withContext`, `suspendCancellableCoroutine`) check for cancellation and throw `CancellationException`
- `ensureActive()` / `isActive` — explicit cancellation checkpoints for CPU loops with no suspension points
- `try/finally` — the unwinding mechanism; `finally` runs on both success and cancellation
- `withContext(NonCancellable)` — runs a block that is *not* cancelled by the surrounding scope
- `invokeOnCancellation` — for registering cleanup with a `Job`
- `CancellationException` extends `IllegalStateException` ⇒ `catch (e: Exception)` catches it
- Cleanup that must survive cancellation: DB writes, file finalisation, resource release
- `suspendCancellableCoroutine` + `invokeOnCancellation { call.cancel() }` for real cancellation of I/O

**Approach**

1. **Know the mechanics.** `cancel()` does not interrupt the thread. It sets the job's state to cancelling and **resumes suspended continuations with `CancellationException`**. If the coroutine is stuck in a non-suspending CPU loop, it keeps running until the next cancellation checkpoint — hence `ensureActive()` inside long loops:
   ```kotlin
   for (i in 0..1_000_000) {
       ensureActive()          // cooperative checkpoint
       process(i)
   }
   ```

2. **Always wrap the operation in `try/finally`.** `finally` runs on normal completion, on exception, **and on cancellation** — that is the guaranteed cleanup hook:
   ```kotlin
   try {
       doWork()
   } finally {
       releaseResources()
   }
   ```

3. **Make cleanup non-cancellable.** The classic bug: `finally` contains a suspending call (`dao.markAborted()`, `file.close()` via `withContext`), the scope is already cancelled, the suspending call throws `CancellationException` *immediately*, and your cleanup never happens — and it can mask the original exception. Fix:
   ```kotlin
   finally {
       withContext(NonCancellable) {
           dao.markOutcome(opId, Outcome.ABORTED)   // this WILL run
           stream.flush()
       }
   }
   ```
   `NonCancellable` is a `Job` element that makes the block immune to the parent's cancellation. Use it **only** for bounded, essential cleanup — never for the actual work, or you defeat cancellation.

4. **Never swallow `CancellationException`.**
   ```kotlin
   try { ... }
   catch (e: CancellationException) { throw e }   // always first
   catch (e: IOException) { retry() }
   ```
   Or use `runCatching` **only** where no coroutine cancellation can occur, and rethrow explicitly otherwise. This is the number-one bug in Android production code: a `catch (e: Exception)` turns a cancellation into a "handled error", and the coroutine keeps running in a zombie state.

5. **Propagate cancellation into the I/O layer.** Retrofit's suspend adapter does this; a hand-rolled `suspendCancellableCoroutine` must do it explicitly:
   ```kotlin
   suspend fun execute(): Response = suspendCancellableCoroutine { cont ->
       val call = client.newCall(request)
       cont.invokeOnCancellation { call.cancel() }   // closes the socket
       call.enqueue(object : Callback { /* resume(cont) */ })
   }
   ```

6. **Distinguish "the operation was cancelled" from "the operation failed".** Use a tri-state result and persist it in the `finally`:
   ```kotlin
   var outcome = Outcome.UNKNOWN
   try {
       outcome = Outcome.SUCCESS
   } catch (e: CancellationException) {
       outcome = Outcome.CANCELLED
       throw e
   } finally {
       withContext(NonCancellable) { dao.persist(opId, outcome) }
   }
   ```
   `UNKNOWN` is the honest value for "cancelled but may have had a server-side effect" — see Section 1 Q12.

7. **For idempotent long operations, prefer finishing over cancelling.** If the work is a payment or an upload that must complete, don't rely on `finally` to "undo" it — move the work to a scope that outlives the screen, and use cancellation only for the *observation*.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| `try/finally` | Guaranteed unwind on all exit paths | `finally` that suspends throws immediately if the scope is cancelled — must pair with `NonCancellable` |
| `withContext(NonCancellable)` cleanup | Cleanup actually completes | Can hang indefinitely if the cleanup blocks; must be bounded and fast |
| Rethrowing `CancellationException` | Preserves structured concurrency | Every `catch (e: Exception)` in the codebase must be audited |
| `ensureActive()` in CPU loops | Responsive cancellation | Adds a check per iteration; negligible cost |
| `invokeOnCancellation { call.cancel() }` | Real socket cancellation, saves battery | Only works for cancellable clients; the server-side work still continues |
| Catching cancellation and continuing | "Best effort" completion | **Breaks structured concurrency**; the coroutine keeps running in a zombie state; produces duplicate side effects |

**Key detail:** `CancellationException` in Kotlin is a typealias for `java.util.concurrent.CancellationException`, whose hierarchy is `CancellationException → IllegalStateException → RuntimeException → Exception`. Any `catch (e: Exception)` or `catch (e: Throwable)` will catch it. This is why `runCatching` is dangerous inside coroutines and why `catch (e: CancellationException) { throw e }` must be the *first* catch clause.

**Code example**

```kotlin
suspend fun syncRecord(id: String): Outcome {
    var outcome = Outcome.UNKNOWN
    val handle = openResource()
    try {
        val result = withContext(Dispatchers.IO) { handle.fetch(id) }
        outcome = Outcome.SUCCESS(result)
        return outcome
    } catch (e: CancellationException) {
        outcome = Outcome.CANCELLED            // record the truth before unwinding
        throw e                                // NEVER swallow cancellation
    } catch (e: IOException) {
        outcome = Outcome.FAILED(e)
        throw e
    } finally {
        // Cleanup must run even though the scope is cancelled → NonCancellable.
        withContext(NonCancellable) {
            runCatching { dao.persistOutcome(id, outcome) }
            runCatching { handle.close() }
        }
    }
}

// CPU-bound loop: add explicit cancellation checkpoints.
suspend fun hashAll(items: List<Item>): List<ByteArray> = buildList {
    for (item in items) {
        ensureActive()                        // cooperative cancellation checkpoint
        add(digest(item))
    }
}

// Real I/O cancellation for a hand-rolled HTTP wrapper.
suspend fun call(request: Request): Response = suspendCancellableCoroutine { cont ->
    val call = client.newCall(request)
    cont.invokeOnCancellation { call.cancel() }   // closes the socket
    call.enqueue(ResponseCallback(cont))
}
```

**Talking points / follow-ups**
- *"Does `cancel()` interrupt the thread?"* → No. It is cooperative; the coroutine continues until the next suspension point or `ensureActive()`.
- *"What runs if the coroutine is cancelled while `finally` is executing?"* → The suspending call inside `finally` throws `CancellationException` immediately unless wrapped in `NonCancellable`.
- *"How do you test cleanup?"* → Launch the job, cancel it mid-operation, and assert the cleanup ran (e.g. a fake resource registered as closed) — this catches the `finally`-without-`NonCancellable` bug immediately.
- *"What is `NonCancellable` for?"* → Cleanup and short critical sections that must complete; never for the primary work.
- *"How do you handle cancellation of a `Flow` collection?"* → The collector's `finally` block is where per-item resources are released; `flow { }` builders run `finally` on cancellation.

---

### 4. Two screens request the same API data simultaneously. How would you prevent duplicate network calls?

**What the interviewer is really asking**
Whether you know the difference between **caching** (a value with a TTL) and **request coalescing / single-flight** (one in-flight request shared by many callers), and whether you can push the sharing *down* into the data layer rather than into the UI. The bonus signal: knowing that `stateIn`/`shareIn` share the *upstream*, not the network call itself.

**Key concepts**
- Request coalescing / single-flight: many callers → one network request → one shared result
- Repository-level in-memory cache with TTL (`cache-control`, `max-age`)
- `MutableStateFlow` + `stateIn(scope, SharingStarted.WhileSubscribed(5000))` — shares the upstream across collectors
- `shareIn` vs `stateIn` — `shareIn` has no initial value and never replays to late subscribers unless `replay` is set
- `CompletableDeferred` / `Deferred` as the shared in-flight handle
- Cache-aside vs stale-while-revalidate
- `Mutex` + double-checked locking
- OkHttp `Cache` / HTTP caching at the transport layer

**Approach**

1. **Put the sharing in the repository, not in the ViewModel.** Two screens mean two ViewModels and two `viewModelScope`s — a ViewModel-level fix cannot deduplicate across screens. The repository (a singleton, app-scoped) is the right home.

2. **Use a hot `StateFlow` as the cache.** The repository exposes a `StateFlow<CacheEntry>`; screens *observe* it and the *upstream* (the network call) is only started when someone collects:
   ```kotlin
   val users: StateFlow<UsersState> = flow { api.fetchUsers() }
       .map { UsersState.Loading to it }          // or emit loading separately
       .stateIn(appScope, SharingStarted.WhileSubscribed(5_000), UsersState.Idle)
   ```
   `WhileSubscribed(5_000)` means the upstream is **shared** between concurrent collectors and kept alive for 5 s after the last one leaves (so a rotation does not re-fetch).

3. **If you need an explicit "refresh" semantic, separate cache from refresh.** `StateFlow` conflates equal values — re-emitting the same list is a no-op — so a refresh that returns identical data would not notify. This is why repositories usually expose *two* things: a `StateFlow` of cached data (for the UI) and a `suspend fun refresh()` (for pull-to-refresh).

4. **If two calls race before the first completes, add a single-flight guard.** `stateIn` handles the common case but only if both collectors attach to the *same* `StateFlow` instance. For a cold `suspend fun` API, use an in-flight `Deferred` map:
   ```kotlin
   private val inFlight = mutableMapOf<String, Deferred<Result<Data>>>()
   private val mapLock = Mutex()

   suspend fun fetch(key: String): Result<Data> {
       val existing = mapLock.withLock { inFlight[key] }
       if (existing != null) return existing.await()      // join the in-flight call

       val deferred = appScope.async { doFetch(key) }
       mapLock.withLock { inFlight[key] = deferred }
       try {
           return deferred.await()
       } finally {
           mapLock.withLock { if (inFlight[key] === deferred) inFlight.remove(key) }
       }
   }
   ```
   This is the "single-flight" pattern from Section 1 Q8 (token refresh) applied to data.

5. **Add a TTL cache so the second screen does not even hit the network after the first one completed.** Cache the last successful value with a timestamp; serve it while fresh, and serve-stale-while-revalidate if a background refresh is desirable.

6. **Use `SharingStarted.Eagerly` only if the data is genuinely needed at startup** — it keeps the upstream alive forever and burns battery. `WhileSubscribed` is the default choice for screen-scoped data.

7. **Do not forget the transport layer.** For HTTP GETs, an OkHttp `Cache` with a `Cache-Control` interceptor gives you conditional requests (`304 Not Modified`) — the server does the deduplication for identical resources.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Repository-level `StateFlow` cache | One upstream for all collectors; natural for Compose | `StateFlow` conflates equal values — a refresh returning identical data emits nothing |
| `stateIn(WhileSubscribed(5000))` | Shared upstream, survives rotation | Upstream stops after 5 s idle → next screen re-fetches; needs a TTL to avoid that |
| `shareIn` | No initial value; lighter than `stateIn` | Late subscribers get nothing unless `replay` is set |
| Single-flight `Deferred` map | Exact dedup of concurrent callers; one request | Manual bookkeeping; the `Deferred` must live in a scope that outlives the callers |
| TTL cache (no revalidation) | Zero extra requests in the TTL window | Stale data; the TTL length is a product decision |
| Stale-while-revalidate | UI is instant; data refreshes in the background | Two network calls per window; needs a background scope |
| OkHttp HTTP cache | Transparent; conditional requests | Requires server cache headers to be correct |

**Key detail:** `stateIn`/`shareIn` share the **upstream flow**, not the HTTP request. If the upstream is `flow { api.fetch() }` and the first collector triggers it, the second collector joins the *same* upstream execution — so yes, one network call. But if each screen creates its *own* repository instance (e.g. via a DI scope per destination), the sharing evaporates. The repository must be a **singleton in the app graph**.

**Code example**

```kotlin
class UserRepository(
    private val api: UserApi,
    private val appScope: CoroutineScope,     // app-scoped singleton → shared across screens
) {
    private val _users = MutableStateFlow<UsersState>(UsersState.Loading)
    val users: StateFlow<UsersState> = _users.asStateFlow()

    private val inFlight = mutableMapOf<String, Deferred<Result<List<User>>>>()
    private val lock = Mutex()

    // Shared upstream: N collectors → ONE network call.
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    init {
        appScope.launch {
            refreshTrigger
                .debounce(300)                       // coalesce rapid refreshes
                .mapLatest { fetch("users") }        // cancel the previous attempt
                .collect { result -> _users.value = result.toState() }
        }
    }

    fun refresh() { refreshTrigger.tryEmit(Unit) }

    // Exact single-flight for callers that want the value directly.
    suspend fun fetch(key: String): Result<List<User>> {
        lock.withLock { inFlight[key] }?.let { return it.await() }

        val deferred = appScope.async { runCatching { api.users() } }
        lock.withLock { inFlight[key] = deferred }
        return try {
            deferred.await()
        } finally {
            lock.withLock { if (inFlight[key] === deferred) inFlight.remove(key) }
        }
    }
}

// Screen A and Screen B both observe the same StateFlow → one request.
@Composable
fun UsersScreen(repo: UserRepository) {
    val users by repo.users.collectAsStateWithLifecycle()
}
```

**Talking points / follow-ups**
- *"Is `stateIn` enough on its own?"* → It is if every screen observes the *same* `StateFlow` instance and the upstream is a single cold flow — but it does not protect two callers of a plain `suspend fun fetch()`.
- *"What about `WhileSubscribed(5_000)` vs `Eagerly`?"* → `WhileSubscribed` ties network usage to actual screen presence; `Eagerly` starts the fetch at app start and keeps it alive — use it only for data needed immediately on launch.
- *"How would you test that only one call happens?"* → Count calls in a fake API, collect the flow from two collectors, advance the test scheduler, assert `callCount == 1`.
- *"What if the two screens want different parameters?"* → Key the in-flight map and the cache by the parameter set; that is the `key` argument above.

---

### 5. A shared cache is accessed by multiple coroutines. How would you make it thread-safe?

**What the interviewer is really asking**
Whether you can pick between a **`Mutex`**, a **`ReentrantLock`**, a **`Semaphore`**, a **concurrent collection**, or **copy-on-write with `AtomicReference`** — and whether you understand the cost model of each (suspension vs blocking, contention vs lock-free reads). A staff answer also asks: *is the cache actually shared across threads, or can it be confined to one?*

**Key concepts**
- `kotlinx.coroutines.sync.Mutex` — suspending, non-reentrant, no thread pinning, does not block a thread while waiting
- `ReentrantLock` — JVM blocking lock, reentrant, supports `tryLock`, `lockInterruptibly`
- `Semaphore(permits)` — bounds concurrency; permits=1 ≈ a mutex but with `acquire`/`release` and `tryAcquire`
- `ConcurrentHashMap` / `Collections.synchronizedMap` — for map-level safety
- `AtomicReference` + immutable snapshot — lock-free reads, copy-on-write writes
- `ReadWriteLock` — many readers, exclusive writer
- `withLock` returns the block's value; `tryLock` returns `Boolean`
- KMP: `kotlinx.atomicfu` instead of `java.util.concurrent.atomic`

**Approach**

1. **First ask whether the cache is shared across threads at all.** If every access happens on a single dispatcher, no lock is needed — confine it:
   ```kotlin
   private val cacheScope = Dispatchers.Default.limitedParallelism(1)
   private val cache = mutableMapOf<String, Entry>()
   suspend fun put(k: String, v: Entry) = withContext(cacheScope) { cache[k] = v }
   ```
   This is the cheapest correct answer and interviewers like to hear it. The risk: `limitedParallelism` is not a hard guarantee of thread affinity — it bounds parallelism, it does not pin a thread — so it is *not* sufficient for mutable non-thread-safe state across threads that can also touch the map directly. For strict confinement use `newSingleThreadContext` (or an actor) and never touch the map outside it.

2. **Classify the access pattern.**
   - *Read-heavy, write-rare* → `AtomicReference` to an immutable map snapshot, or `ConcurrentHashMap`. Reads become lock-free.
   - *Read-heavy, compound write* → copy-on-write under a `Mutex`:
     ```kotlin
     private val state = AtomicReference<Map<String, Entry>>(emptyMap())
     suspend fun put(k: String, e: Entry) = mutex.withLock {
         state.update { it + (k to e) }        // immutable map copy, atomic swap
     }
     fun get(k: String): Entry? = state.get()[k]   // lock-free read
     ```
   - *Many concurrent readers + occasional exclusive writer* → `ReadWriteLock`.
   - *Bounded concurrency* (e.g. max 4 simultaneous refreshes) → `Semaphore(4)`.
   - *General critical section* → `Mutex`.

3. **Prefer `Mutex` over `ReentrantLock` in coroutine code.** A `ReentrantLock` blocks the *thread* while waiting — inside a coroutine that pins an `IO`/`Default` thread for the duration. `Mutex.withLock` suspends instead, releasing the thread. The exception: when you need `tryLock` (try, and fall back to a cached value if the lock is busy) and cannot suspend, a `ReentrantLock` is acceptable — but you must call it from a blocking context (`Dispatchers.IO`), not from `Dispatchers.Main`.

4. **Remember `Mutex` is not reentrant.** If your cache's `put` internally calls `get`, and both take the same mutex → deadlock. Restructure so inner calls use a lock-free accessor.

5. **Make the lock granularity match the contention.** One global mutex for a 10k-entry cache serialises everything; per-key mutexes (a striped lock) or a `ConcurrentHashMap` with per-key atomic compute is better:
   ```kotlin
   private val locks = Array(16) { Mutex() }
   private fun lockFor(key: String) = locks[(key.hashCode() and 0xFFFF) % locks.size]
   ```

6. **Do not lock around I/O.** If the cache miss performs a network call, do **not** hold the mutex across it — otherwise one slow network call blocks every other reader of the cache. Use single-flight (Q4/Q13) to dedupe the fetch *without* holding a lock during the fetch.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| `Mutex.withLock` | Suspends instead of blocking a thread; idiomatic in coroutines | Not reentrant; contention serialises callers; must not be held across I/O |
| `ReentrantLock` | Reentrant; `tryLock`; familiar JVM semantics | **Blocks the thread**; wrong on the main dispatcher; cannot be held across a suspension point |
| `Semaphore(1)` | Bounds concurrency; `tryAcquire` for a fast fallback | Same blocking caveats; permits semantics can be misused |
| `ConcurrentHashMap` | Lock-free reads/writes; scales | Per-operation atomicity only; compound operations still need external synchronisation |
| `AtomicReference` copy-on-write | Lock-free reads; trivially safe | Writes copy the whole map — O(n) per write; bad for very large or frequently mutated caches |
| Thread confinement (`limitedParallelism(1)`) | No locks at all | Serialises all work; `limitedParallelism` bounds parallelism but does not pin a thread, so it is not a substitute for real confinement |
| `ReadWriteLock` | Many concurrent readers | Heavier; starvation risk without fairness; blocking |

**Key detail:** `Mutex.withLock` **returns the value of the block**, so `val entry = mutex.withLock { cache[key] }` works — but a `return` from inside the block (non-local return) is only allowed if `withLock` is inline (it is `inline suspend`), and it will still release the lock. Do not confuse `withLock` with `tryLock`, which returns `Boolean` and does **not** run the block if the lock is unavailable.

**Code example**

```kotlin
class ImageCache(maxEntries: Int = 64) {

    private val mutex = Mutex()
    private val entries = LinkedHashMap<String, ByteArray>(maxEntries, 0.75f, true)
    private val maxEntries = maxEntries

    // Lock-free read of an immutable snapshot.
    private val snapshot = AtomicReference<Map<String, ByteArray>>(emptyMap())

    suspend fun get(key: String): ByteArray? {
        snapshot.get()[key]?.let { return it }
        return mutex.withLock {                 // slow path under the lock
            entries[key]?.also { snapshot.set(entries.toMap()) }
        }
    }

    suspend fun put(key: String, value: ByteArray) {
        mutex.withLock {
            entries[key] = value
            if (entries.size > maxEntries) {
                val oldest = entries.keys.first()
                entries.remove(oldest)
            }
            snapshot.set(entries.toMap())        // atomic publish of the new state
        }
    }

    // Bounded concurrency for cache misses (no lock held during the fetch).
    private val refreshSlots = Semaphore(4)
    suspend fun getOrLoad(key: String, loader: suspend () -> ByteArray): ByteArray {
        get(key)?.let { return it }
        refreshSlots.acquire()
        try {
            get(key)?.let { return it }        // double-check after acquiring
            val value = loader()               // network/IO OUTSIDE any mutex
            put(key, value)
            return value
        } finally {
            refreshSlots.release()
        }
    }
}
```

**Talking points / follow-ups**
- *"Why not just use `synchronized`?"* → It blocks the thread and cannot be used across a suspension point; `Mutex` is the coroutine-native equivalent.
- *"Is `Mutex` fair?"* → No — the underlying implementation is a queue, but there is no strict fairness guarantee; do not build ordering assumptions on it.
- *"How would you detect lock contention?"* → Instrument the critical section with timing, or use a `Semaphore` with `availablePermits` to observe queue depth in production metrics.
- *"What about `kotlinx.atomicfu`?"* → In KMP, `java.util.concurrent.atomic` is JVM-only; `atomicfu` provides a common `AtomicRef`/`AtomicLong` API across platforms.
- *"Is `ConcurrentHashMap` enough for the whole cache?"* → Only if every operation is a single map call; `computeIfAbsent` is atomic but running a network call inside it blocks the bin.

---

### 6. An API should return cached data for 30 seconds and fetch fresh data afterward. How would you implement this?

**What the interviewer is really asking**
Whether you can implement a **TTL cache** correctly — and, more importantly, whether you understand the *semantics* you are choosing: does "cached data for 30 seconds" mean **serve stale and refresh in the background** (stale-while-revalidate), or **serve fresh if available, else block on the network** (cache-aside with a freshness window)? The second question is where the real design lives.

**Key concepts**
- TTL / freshness window vs. max-staleness (two different TTLs)
- Cache-aside vs stale-while-revalidate vs network-first
- `StateFlow` + `SharingStarted.WhileSubscribed` — natural TTL cache via upstream restart
- `Mutex` + double-checked locking; single-flight for concurrent misses
- `kotlinx.coroutines.sync.Mutex` vs `AtomicReference` snapshot
- HTTP `Cache-Control: max-age=30` handled by OkHttp's cache
- Clock injection for testability (`kotlinx-datetime` `Clock`)

**Approach**

1. **Name the policy explicitly.** Two different behaviours share the phrase "cache for 30 seconds":
   - **Freshness window (cache-aside)**: if the cached value is younger than 30 s, return it; otherwise call the API and wait. The user sees fresh data but pays the latency once per window.
   - **Stale-while-revalidate**: always return the cached value immediately; if it is older than 30 s, kick off a background refresh and let the UI update when it lands. The user *never* waits.

   For most Android screens, stale-while-revalidate is the better UX; for a screen where stale data is dangerous (balance, permissions), use the freshness window.

2. **Implement the freshness window with a timestamped entry.**
   ```kotlin
   data class Entry<T>(val value: T, val fetchedAtMillis: Long)

   suspend fun get(key: String): T {
       val cached = mutex.withLock { entries[key] }
       if (cached != null && clock.now() - cached.fetchedAtMillis < TTL_MILLIS) {
           return cached.value                     // still fresh
       }
       return refresh(key)                        // stale or missing
   }
   ```
   Use an injected `Clock` so tests can control time — do not read `System.currentTimeMillis()` inline.

3. **Avoid a thundering herd on expiry.** When 30 s elapses, every screen hitting the cache at once must not fire N requests. Combine the TTL check with the **single-flight** pattern (Q4/Q13): the first caller starts the fetch; the others await the same `Deferred`.

4. **Implement stale-while-revalidate on top of the same store.** The cached entry keeps a `fetchedAt`; the repository exposes a `StateFlow` that is updated when the background refresh completes:
   ```kotlin
   val users: StateFlow<UsersState> = flow {
       val cached = readCache()
       if (cached != null) {
           emit(UsersState.Data(cached.value, stale = isStale(cached)))
           if (isStale(cached)) emit(UsersState.Refreshing)   // and refresh
       }
       val fresh = api.fetchUsers()
       writeCache(fresh)
       emit(UsersState.Data(fresh, stale = false))
   }
   ```

5. **Use `StateFlow` + `WhileSubscribed` as the cache invalidation mechanism.** A very idiomatic trick: the upstream is re-run whenever the last subscriber leaves and a new one arrives after the stop timeout — that *is* a TTL, provided by the framework:
   ```kotlin
   val users = flow { api.fetchUsers() }
       .stateIn(scope, SharingStarted.WhileSubscribed(30_000), initial)
   ```
   Here the "30 seconds" is the *stop timeout*: the upstream stays alive for 30 s after the last collector leaves, so a quick navigation back does not re-fetch. Be precise in the interview about what this does and does not guarantee — it is a *subscription* timeout, not a data-freshness guarantee; if you need both, combine with an explicit TTL on the cached value.

6. **Push the TTL to the server where possible.** `Cache-Control: max-age=30` plus an OkHttp `Cache` gives you HTTP-correct caching, conditional requests, and `ETag`/`304` handling for free. Do this when the backend controls the data's freshness semantics.

7. **Handle the "no cached value" case.** A cold start has no cache: the first call must block (or the UI must show a skeleton). Decide explicitly: cold-start = block on network with a timeout; warm = instant from cache.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Freshness window (cache-aside) | Simple; always returns data newer than 30 s | The first request after expiry blocks the UI on the network |
| Stale-while-revalidate | Instant UI; never blocks on refresh | The user can see data up to (30 s + refresh time) old; two phases of UI state to model |
| `WhileSubscribed(30_000)` | Framework-managed upstream sharing; no manual TTL bookkeeping | A *subscription* timeout, not a freshness guarantee; the data can still be arbitrarily old |
| Explicit TTL + `Mutex` + single-flight | Exact semantics; testable with an injected clock | More code; the clock and TTL policy must be maintained in one place |
| OkHttp HTTP cache | HTTP-correct; conditional requests | Requires server headers; per-URL; no cross-endpoint sharing |
| Persisted (Room/DataStore) cache | Survives process death; instant cold start | Staleness across launches; invalidation policy needed |

**Key detail:** A `StateFlow` **conflates equal values** — if the refetched list is `equals()` to the cached one, `MutableStateFlow.value = ...` will not notify collectors. If the UI must show "updated just now" even for identical data, either include a timestamp in the state class or use a `SharedFlow` for the "refresh completed" event.

**Code example**

```kotlin
class ProfileRepository(
    private val api: ProfileApi,
    private val dao: ProfileDao,             // Room-backed cache (survives process death)
    private val clock: Clock,
    private val scope: CoroutineScope,
) {
    private val TTL = 30.seconds
    private val mutex = Mutex()
    private var inFlight: Deferred<Result<Profile>>? = null

    // Stale-while-revalidate: UI always gets an immediate value, freshness is refreshed behind it.
    val profile: StateFlow<ProfileState> = flow {
        dao.get()?.let { emit(ProfileState.Data(it, stale = isStale(it))) }
        emit(ProfileState.Loading)
        val fresh = fetchWithSingleFlight()
        fresh.onSuccess { dao.upsert(it); emit(ProfileState.Data(it, stale = false)) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ProfileState.Idle)

    private fun isStale(p: Profile): Boolean = clock.now() - p.fetchedAt > TTL

    // Freshness window + single-flight: concurrent callers share one request.
    suspend fun getFresh(): Result<Profile> {
        dao.get()?.takeIf { !isStale(it) }?.let { return Result.success(it) }
        return fetchWithSingleFlight()
    }

    private suspend fun fetchWithSingleFlight(): Result<Profile> {
        inFlight?.let { return it.await() }
        val deferred = scope.async { runCatching { api.fetchProfile() } }
        inFlight = deferred
        return try {
            deferred.await()
        } finally {
            if (inFlight === deferred) inFlight = null
        }
    }
}
```

**Talking points / follow-ups**
- *"Where do you store the timestamp?"* → Alongside the cached payload in Room (a `fetchedAt` column) so it survives process death; do not keep it only in memory.
- *"How do you test a 30-second TTL?"* → Inject a `Clock`; in `runTest` use `testScheduler.currentTime` as the clock so time is virtual — no `Thread.sleep`.
- *"What if the clock changes on the device?"* → Use a monotonic source for in-process TTLs (`SystemClock.elapsedRealtime`) and wall-clock only for cross-process persistence.
- *"Should the TTL be the same on Wi-Fi and cellular?"* → Consider a shorter TTL on metered networks to save data.
- *"Does `OkHttp`'s cache make this redundant?"* → Not entirely — OkHttp's cache is per-URL and requires server headers; an app-level cache can merge several endpoints into one model.

---

### 7. A StateFlow emits the same value repeatedly, causing unnecessary UI updates. How would you handle it?

**What the interviewer is really asking**
Whether you understand that **`StateFlow` is conflated and `distinctUntilChanged` by default** — so "the same value repeatedly" should already be suppressed — and therefore that the *real* problem is either (a) the values are *not* actually equal (a data class with a changing field such as `timestamp` or `isRefreshing`), or (b) the equality is wrong (an identity/`equals` that always differs, e.g. a lambda or a `List` built with a fresh instance). This is a "your mental model is broken" question as much as a "how do you fix it" question.

**Key concepts**
- `StateFlow` always has a value; `value = x` uses `equals()` to decide whether to emit (conflation)
- `MutableSharedFlow` has **no** such de-duplication — every `emit` reaches subscribers (subject to buffer overflow policy)
- `distinctUntilChanged()` / `distinctUntilChangedBy { }`
- Data-class equality vs identity: `equals` on `data class` compares fields; a class without `equals` uses identity
- `SharingStarted` and the *upstream* restart as a source of repeated emissions
- Compose: `collectAsStateWithLifecycle()` recomposes only when the value changes — but `StateFlow` collectors re-read on every emission
- `snapshotFlow` vs `collectAsState` — snapshot reads vs flow reads

**Approach**

1. **Verify that the values are truly equal.** `StateFlow` suppresses a write when `newValue == oldValue`. If the UI is still updating, then `equals` is returning `false`. Common causes:
   - The state contains a **changing field**: `data class UiState(val items: List<Item>, val loadedAt: Long = System.currentTimeMillis())` — the timestamp changes every time, so every write is a new value.
   - The state contains a **function or lambda** (not a stable `equals`).
   - The state contains a **`List`/`Set` built with a new but non-equal content**, e.g. a `List` of objects whose `equals` includes a `timestamp`.
   - The state is a **plain class** without `equals` → identity comparison → every instance is "new".

2. **Fix the model rather than the pipeline.** Make the UI state a `data class` of immutable fields with correct structural equality, and remove non-deterministic fields from it. Keep the timestamp in a *separate* `StateFlow` (or in a `SharedFlow` event) if the UI genuinely needs to show "updated at".

3. **Use `distinctUntilChanged` when you need a different notion of equality.**
   ```kotlin
   val uiState = rawState
       .distinctUntilChangedBy { it.itemIds }        // ignore e.g. a timestamp field
       .stateIn(scope, SharingStarted.WhileSubscribed(5_000), UiState.Idle)
   ```
   `distinctUntilChangedBy` is the right tool when the *semantic* identity is a projection of the state.

4. **Move the de-duplication upstream.** `distinctUntilChanged` after a `map` only helps if the expensive part is downstream of it. Put it as early as possible in the pipeline:
   ```kotlin
   val ids = dao.observeIds()
       .distinctUntilChanged()                      // cheap + early
       .flatMapLatest { ids -> fetchDetails(ids) }  // expensive work only when ids change
   ```

5. **Understand *why* the value is being re-emitted.** Usually the upstream is being **restarted** — e.g. `SharingStarted.WhileSubscribed(5_000)` restarts the upstream after the stop timeout, replaying the current value to the new subscriber. If that is unwanted, use `WhileSubscribed(0)` (stop immediately) or `Eagerly`. Also check for `retry { ... }` re-subscribing on every error.

6. **Do not use `MutableSharedFlow` for state.** If you find yourself adding `distinctUntilChanged` to a `SharedFlow` to suppress duplicate state, the abstraction is wrong: use `StateFlow` for state (it does the conflation for you) and `SharedFlow` for events.

7. **In Compose, verify the recomposition cause.** If the Composable recomposes but the *values* are unchanged, the cause is usually a different `State` read in the same scope, an unstable parameter (a lambda or a non-primitive), or a missing `key` in a list. Use the Compose compiler's stability report and the Layout Inspector.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Rely on `StateFlow`'s built-in conflation | Zero code; correct by construction | Only works if `equals` reflects the semantic identity — a timestamp in the state defeats it |
| Remove volatile fields from the UI state | Correct model; fixes the root cause | Requires discipline: "no timestamps in UI state" |
| `distinctUntilChangedBy { it.ids }` | Explicit semantic equality | Easy to get the projection wrong; a second definition of equality to maintain |
| `distinctUntilChanged()` in the pipeline | Cheap, early, reusable | Hides the underlying model problem if applied at the wrong point |
| Separate `StateFlow` for the timestamp | Each stream updates independently | More state objects to combine; risk of torn UI |
| `MutableSharedFlow` for UI state | Never conflates | Wrong semantics: no initial value, late subscribers get nothing, more boilerplate |

**Key detail:** `StateFlow` conflates based on `equals`, and the comparison is done with `Any.equals` — **not** referential. `MutableStateFlow.update { }` is a CAS loop, so two concurrent updates cannot lose one; a plain `value = ...` write is also atomic, but a read-modify-write (`state.value = state.value.copy(x = 1)`) is not — use `update`.

**Code example**

```kotlin
// BAD: the timestamp makes every instance unequal → every write emits.
data class BadState(val items: List<Item>, val loadedAt: Long = System.currentTimeMillis())

// GOOD: pure value semantics.
data class UiState(
    val items: List<Item>,
    val isLoading: Boolean,
    val error: UiText? = null,
)

private val _uiState = MutableStateFlow(UiState(items = emptyList(), isLoading = true))
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

fun onLoaded(items: List<Item>) {
    _uiState.update { current -> current.copy(items = items, isLoading = false) }
    // Identical list → equals() is true → StateFlow does NOT emit. No recomposition.
}

// When the semantic identity is a projection, express it explicitly.
val visibleItems: StateFlow<List<Item>> = uiState
    .distinctUntilChangedBy { state -> state.items.map { it.id } }   // ignore other fields
    .map { state -> state.items }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

// Keep "last updated" out of the state; deliver it as an event instead.
private val _refreshed = MutableSharedFlow<Long>(extraBufferCapacity = 1)
val refreshedAt: SharedFlow<Long> = _refreshed.asSharedFlow()
```

**Talking points / follow-ups**
- *"Does `StateFlow` really drop equal values?"* → Yes: `MutableStateFlow.value` setter compares with `equals` and ignores the write if they match. The *initial* value is always emitted to the first collector.
- *"What is `SharingStarted.WhileSubscribed(5_000)` actually doing?"* → It stops the upstream 5 s after the last collector leaves and restarts it when a new collector arrives — a common source of "the value was emitted again".
- *"How do I force an emission of identical data?"* → Put a monotonically increasing version/sequence number in the state, or use a `SharedFlow` for that signal.
- *"Is `distinctUntilChanged` on a `StateFlow` redundant?"* → Usually yes — but it is not redundant after a `map` that produces a *different* type with different equality semantics.

---

### 8. Events emitted through SharedFlow are missed when the screen is in the background. How would you design event delivery?

**What the interviewer is really asking**
Whether you understand the difference between **state** (a value that can be replayed) and **events** (a thing that happened once), and that a `SharedFlow` is *not* a queue — with `replay = 0` and no subscribers, emissions are **dropped entirely**. The staff-level answer is: model navigation/toast/error events as a **durable queue** (`Channel`) or as **state**, and treat `SharedFlow` as the transport, not the store.

**Key concepts**
- `SharedFlow` with `replay = 0` and no subscribers → **emissions are dropped**
- `replay = 1` → the last value is replayed to new subscribers, but only the *most recent* one (older ones are lost)
- `extraBufferCapacity` and `BufferOverflow.SUSPEND` vs `DROP_OLDEST` vs `DROP_LATEST`
- `Channel` — a single-consumer queue; `Channel.BUFFERED` (64), `UNLIMITED`, `CONFLATED`, `RENDEZVOUS`
- `receiveAsFlow()` — consume a `Channel` as a Flow
- `tryEmit` returns `Boolean` (false if the buffer is full and the policy is `SUSPEND`)
- `whileSubscribed(stopTimeout)` — the upstream is cancelled when the screen goes to the background, so events emitted during that window are lost
- `SavedStateHandle` / persistence for events that must survive process death
- The modern Compose answer: events are **state**, not events — a `StateFlow<Event?>` that the UI consumes and clears

**Approach**

1. **First diagnose *why* the event was missed.** Three different causes, three different fixes:
   - **The upstream was stopped** because the screen had no subscribers and `SharingStarted.WhileSubscribed` cancelled the upstream → the event was never produced. Fix: `SharingStarted.Eagerly` / `Lazily`, or `subscriptionCount` handling.
   - **The event was produced but not buffered** (`SharedFlow` with `replay = 0` and no active subscriber) → dropped. Fix: `replay`, `extraBufferCapacity`, or a `Channel`.
   - **The event was consumed by a different collector** (e.g. two screens collecting the same flow, or a screen that was recreated and re-collected). Fix: make the event flow single-consumer.

2. **Prefer modelling events as state.** The idiomatic modern answer (and what Google's own guidance now recommends) is: a one-shot event is a piece of **state that must be consumed**.
   ```kotlin
   private val _pendingNavigation = MutableStateFlow<Destination?>(null)
   val pendingNavigation: StateFlow<Destination?> = _pendingNavigation.asStateFlow()

   fun onPaymentSucceeded() { _pendingNavigation.value = Destination.Receipt }
   ```
   The Composable collects it and clears it in a `LaunchedEffect`. Because it is a `StateFlow`, a late collector **still sees it** — nothing is missed. This is the single biggest improvement over `SharedFlow` for one-shot UI events.

3. **When you must use a flow-of-events, use a `Channel`.** A `Channel` is a real queue with a buffer, so events emitted while nobody is collecting are retained:
   ```kotlin
   private val _events = Channel<UiEvent>(Channel.BUFFERED)   // 64 slots
   val events: Flow<UiEvent> = _events.receiveAsFlow()

   fun onEvent(e: UiEvent) { _events.trySend(e) }             // never suspends the caller
   ```
   `receiveAsFlow()` is single-consumer — exactly the semantics you want for navigation events. For multiple consumers, use a `SharedFlow` with `replay = N` (a small replay cache) instead.

4. **Understand `SharedFlow`'s buffering knobs.**
   - `MutableSharedFlow<T>()` — `replay = 0`, `extraBufferCapacity = 0`, `BufferOverflow.SUSPEND`: `tryEmit` returns `false` when there is no buffer and no subscriber; `emit` suspends.
   - `MutableSharedFlow<T>(replay = 1)` — the last value is kept and replayed to new subscribers. Good for "the latest screen state" — but it **replays a stale event** to a screen that opens later, which is why it is dangerous for navigation events.
   - `MutableSharedFlow<T>(replay = 0, extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)` — never suspends the emitter; the *oldest* event is discarded under pressure. Right for high-frequency telemetry; wrong for navigation (you would lose the oldest pending navigation).

5. **Keep the scope alive while the screen is in the background.** If the producer is tied to a `viewModelScope` and the ViewModel is cleared, no buffering helps. For events that must outlive the screen, use an **application-scoped** producer or a `Channel` owned by the application scope.

6. **For events that must survive process death** (e.g. a pending verification SMS, a deep link that arrived while the app was killed), persist them — `SavedStateHandle` or Room — and re-deliver on restore. A `Channel` does not survive process death.

7. **Test the exact scenario.** Use `runTest`, subscribe, then **unsubscribe** (cancel the collector job), emit, re-subscribe, and assert the event is delivered. This catches the `replay = 0` drop immediately.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Event modelled as `StateFlow<Event?>` | Late collectors still receive it; no drop; testable | The UI must explicitly clear it; a re-collection can re-trigger it if the clear is racing |
| `Channel` + `receiveAsFlow()` | Real queue; events emitted while unobserved are retained; single-consumer semantics | Single consumer only; not replayed to *new* collectors unless still buffered; lost on process death |
| `SharedFlow(replay = 1)` | Simple; a new subscriber gets the latest value | **Replays stale events** to a screen that opens later — a classic "navigate to the receipt twice" bug |
| `SharedFlow(replay = 0, extraBufferCapacity = N)` | Non-blocking emit with a buffer | `tryEmit` returns `false` when full; `DROP_OLDEST` can lose the oldest pending event |
| `SharingStarted.Eagerly` on the producer | The upstream keeps running while the screen is in the background | Wastes resources/battery; events still lost if nobody is subscribed and `replay = 0` |
| Persisted event queue (Room) | Survives process death; can be drained later | Heavier; needs ordering/dedup logic; overkill for most UI events |

**Key detail:** `MutableSharedFlow.tryEmit` returns `Boolean` — `false` means the value was **not** delivered (buffer full under `SUSPEND`, or no buffer and no subscribers). Treating `tryEmit`'s result as a success is a real bug: the event is silently dropped. `emit` is the suspending counterpart that guarantees delivery or suspends.

**Code example**

```kotlin
// Option A (preferred): a one-shot event is state. Nothing is missed.
class CheckoutViewModel : ViewModel() {
    private val _navigation = MutableStateFlow<Destination?>(null)
    val navigation: StateFlow<Destination?> = _navigation.asStateFlow()

    fun onPaymentSucceeded() { _navigation.value = Destination.Receipt }
    fun onNavigated() { _navigation.value = null }        // consumed by the UI
}

// Option B: a Channel is a durable queue for the duration of the process.
class EventBus(private val scope: CoroutineScope) {
    private val _events = Channel<UiEvent>(Channel.BUFFERED)     // 64 slots
    val events: Flow<UiEvent> = _events.receiveAsFlow()          // single-consumer

    fun send(e: UiEvent) {
        if (!_events.trySend(e).isSuccess) {
            // Buffer full → decide: drop oldest? block? surface a diagnostic.
            Log.w("EventBus", "event dropped: $e")
        }
    }
}

// Consuming it, including the background case:
@Composable
fun RootScreen(vm: CheckoutViewModel) {
    val nav by vm.navigation.collectAsStateWithLifecycle()
    LaunchedEffect(nav) {
        nav?.let { controller.navigate(it); vm.onNavigated() }
    }
}
```

**Talking points / follow-ups**
- *"What does `replay = 0` do when there are no subscribers?"* → The emission is **dropped**; it is not buffered for a future subscriber.
- *"What does `replay = 1` do to a screen that opens later?"* → It immediately receives the last event — which is why `replay = 1` is wrong for navigation and right for "latest state".
- *"Channel vs SharedFlow — when do you pick which?"* → Channel when events must be **queued** and consumed by exactly one consumer; SharedFlow when multiple consumers must each see the event.
- *"How do you prevent a navigation event from firing twice on rotation?"* → Clear the state in the same `LaunchedEffect` that consumes it, or use a `SavedStateHandle` flag.
- *"Does `collectAsStateWithLifecycle` help?"* → It stops collecting when the screen is STOPPED, which is exactly why the producer's upstream may be cancelled — pair it with `WhileSubscribed` semantics you control.

---

### 9. A coroutine launched in viewModelScope is cancelled when the ViewModel is cleared, but the business operation must survive. How would you handle it?

**What the interviewer is really asking**
Whether you understand **scope ownership**: the scope of a coroutine is a *contract about lifetime*, and business work outlives screens. The staff answer is not "use `GlobalScope`" (which is wrong) but "**give the work a scope that matches its real lifetime**" — an application scope, a `WorkManager` job, or a repository-owned scope — and separate *observation* (screen-scoped) from *execution* (app-scoped).

**Key concepts**
- `viewModelScope` — cancelled in `ViewModel.onCleared()`; survives configuration change
- Application-scoped `CoroutineScope(SupervisorJob() + Dispatchers.X)` — lives for the process lifetime
- `GlobalScope` — **should not be used**: no lifecycle, leaks, no cancellation, no testability
- Observation vs execution: `Deferred.asFlow()` / a `StateFlow` of results
- `WorkManager` `CoroutineWorker` — survives process death, can be constrained and retried
- `NonCancellable` — for cleanup, not for the work itself
- `repeatOnLifecycle` / `collectAsStateWithLifecycle` — the observation side
- Process death: an app scope does **not** survive it — use `WorkManager` or persisted state

**Approach**

1. **Name the real lifetime of the work.** "Must survive the ViewModel" and "must survive process death" are different requirements:
   - Survives screen exit / rotation → **application scope** (or a repository-scoped scope).
   - Survives process death / reboot → **WorkManager** (or `ForegroundService` for user-visible long work), with the *result* persisted.

2. **Move the work out of the ViewModel.** The ViewModel should *request* the operation and *observe* the outcome:
   ```kotlin
   class CheckoutViewModel(private val repo: CheckoutRepository) : ViewModel() {
       // Observe an app-scoped StateFlow; the ViewModel's own scope only collects.
       val state: StateFlow<CheckoutState> = repo.checkoutState
   }
   ```
   The repository owns the app-scoped coroutine that performs the work; the ViewModel simply reads a `StateFlow` that survives the ViewModel's death.

3. **If you must launch from the ViewModel, launch it in an injected scope.**
   ```kotlin
   class CheckoutViewModel(private val repo: CheckoutRepository) : ViewModel() {
       fun submit(order: Order) {
           repo.submitInBackground(order)      // repo uses its own appScope
       }
   }
   ```
   ```kotlin
   class CheckoutRepository(private val appScope: CoroutineScope) {
       private val _state = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
       val state: StateFlow<CheckoutState> = _state.asStateFlow()

       fun submitInBackground(order: Order) {
           appScope.launch {
               _state.value = CheckoutState.Submitting
               _state.value = runCatching { api.placeOrder(order) }
                   .fold(onSuccess = { CheckoutState.Done(it) }, onFailure = { CheckoutState.Error(it) })
           }
       }
   }
   ```
   The result is **observable after the ViewModel is gone** — a new ViewModel instance reads the same `StateFlow` and immediately sees `Done`.

4. **Do not use `GlobalScope`.** It has no `Job` to cancel, no dispatcher policy, no test control, and it keeps work alive after the app is backgrounded. If you find yourself reaching for it, the correct fix is an explicitly injected app scope.

5. **For work that must survive process death, use `WorkManager`.**
   ```kotlin
   class SubmitOrderWorker(...) : CoroutineWorker(...) {
       override suspend fun doWork(): Result = try {
           api.placeOrder(order)
           Result.success()
       } catch (e: IOException) {
           Result.retry()     // WorkManager applies backoff; the work is re-executed after process death
       }
   }
   ```
   `CoroutineWorker.doWork` runs in a scope that is cancelled when the work is stopped — and WorkManager re-runs the work after a process restart. The *result* must be delivered to the UI via persisted state (Room) or a `WorkInfo` observation, not via an in-memory flow.

6. **Handle the "user left" case deliberately.** If the operation is *side-effecting and expensive* (a payment), it must continue. If it is *purely a read* (a fetch), cancelling it is usually correct. Encode this in the API: `suspend fun load()` (cancellable) vs `fun submitAndPersist()` (fire-and-observe).

7. **Handle cancellation at the boundary.** If the ViewModel *does* cancel a job that is doing side-effecting work, persist the outcome as `UNKNOWN` and reconcile (Section 1 Q12) — cancellation does not undo the server-side effect.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Application-scoped `CoroutineScope` | Simple; survives screen exit; result observable later | **Does not survive process death**; keeps resources alive for the process lifetime; must be shut down deliberately in tests |
| `WorkManager` | Survives process death; constraints; built-in retry/backoff | Latency (not immediate); needs `ForegroundInfo` for long work; more plumbing to deliver results to the UI |
| Repository-owned scope + `StateFlow` | The operation and its *result* have a home that outlives the screen | The repository becomes a state holder; needs clear lifecycle ownership |
| `GlobalScope` | "Always runs" | **Never use it**: uncancellable, leaks, untestable, no dispatcher discipline |
| Foreground service | User-visible long work with a notification | Notification/permission complexity; Play policy constraints |
| `viewModelScope` + `NonCancellable` | Keeps the current shape | `NonCancellable` around the work means the work is **not cancellable at all** — a leak, not a solution |

**Key detail:** `viewModelScope` is cancelled in `onCleared()`, which happens when the ViewModel is cleared — on `finish()` for a `NavHost` destination, but **not** on rotation. So "the ViewModel was cleared" usually means the user genuinely navigated away. If the work must survive that, the operation's lifetime is longer than the screen's, and the scope must reflect that. And no in-memory scope survives process death — if that is a requirement, `WorkManager` is the answer.

**Code example**

```kotlin
// Application-scoped, injected, testable — NOT GlobalScope.
@Singleton
class AppScope @Inject constructor() {
    // SupervisorJob so one failed child never kills the scope.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Singleton
class CheckoutRepository @Inject constructor(
    private val api: CheckoutApi,
    private val appScope: CoroutineScope,     // provided by AppScope
) {
    private val _state = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val state: StateFlow<CheckoutState> = _state.asStateFlow()

    // Returns immediately; the work survives the caller's scope.
    fun submit(order: Order) {
        appScope.launch {
            _state.value = CheckoutState.Submitting
            _state.value = try {
                CheckoutState.Completed(api.placeOrder(order))
            } catch (e: CancellationException) {
                CheckoutState.Unknown     // the server may still have processed it
                throw e
            } catch (e: IOException) {
                CheckoutState.Failed(e)
            }
        }
    }
}

// The ViewModel only observes — its scope is irrelevant to the operation.
class CheckoutViewModel @Inject constructor(repo: CheckoutRepository) : ViewModel() {
    val state: StateFlow<CheckoutState> = repo.state
}
```

**Talking points / follow-ups**
- *"Why is `GlobalScope` bad?"* → It has no parent job, so it is never cancelled; it leaks the work past the app's lifetime, ignores the caller's cancellation, and makes tests non-deterministic.
- *"What if the process is killed?"* → An app scope dies with the process. Use `WorkManager` and persist the result in Room so the UI can pick it up on the next launch.
- *"How do you deliver the result if the ViewModel is gone?"* → Persist it (Room) and observe the DB, or expose it as a `StateFlow` on an app-scoped repository — the new ViewModel reads the current value on creation.
- *"Should I use a `Channel` instead of a `StateFlow` for the result?"* → A `StateFlow` — the result is *state*, and a new collector should see it immediately.
- *"How do you test this?"* → Inject a `TestScope` in tests and assert the state transitions; the app scope must be injectable for that to work.

---

### 10. Several coroutines need to update a shared database record atomically. How would you prevent inconsistent updates?

**What the interviewer is really asking**
Whether you distinguish **atomicity of the write** from **atomicity of the read-modify-write** (read → validate → write). The second is the real problem: two coroutines both read balance=100, both see "sufficient", both write 100-80=20 — and 80 is lost. The answer is a **transaction** (Room `@Transaction` / `withTransaction`) or **optimistic concurrency** (version column with a conditional update), plus an idempotency key for retries.

**Key concepts**
- Room `@Transaction` / `db.withTransaction { }` — atomic across multiple statements
- Room `@Query("UPDATE ... WHERE id = :id AND version = :version")` — optimistic concurrency control (compare-and-swap at the SQL level)
- `@Version` column → `UPDATE` returns rows affected = 0 on a conflict
- Room's suspend DAO methods run on Room's own executor; `withTransaction` is not a plain `BEGIN/COMMIT` on the calling thread
- `Mutex` as an in-process guard (does **not** help across processes — Android apps are usually single-process, but `android:process` and multi-module workers exist)
- Idempotency keys for retry safety (Section 1 Q1)
- `INSERT ... ON CONFLICT` / upserts vs read-then-write
- SQLite write serialisation: Room already serialises writers on one connection

**Approach**

1. **Name the invariant and the failure mode.** The classic lost update: two coroutines read the same row, compute, and write — the second write clobbers the first. SQL alone does not prevent this; only a transaction with an appropriate isolation level, or an explicit version check, does.

2. **Use a single atomic UPDATE statement.** If the update can be expressed as a delta, let the database do it:
   ```sql
   UPDATE accounts SET balance = balance - :amount, version = version + 1
   WHERE id = :id AND balance >= :amount
   ```
   Room returns the number of affected rows. `0` means the guard failed (insufficient balance or a concurrent modification) — you then re-read and decide. **This is the correct default**: no read-modify-write, no lost update.

3. **Use a version column for optimistic concurrency** when the update depends on the previous value:
   ```sql
   UPDATE accounts SET balance = :newBalance, version = version + 1
   WHERE id = :id AND version = :expectedVersion
   ```
   If the affected-row count is 0, another writer won the race → re-read and retry (bounded), or surface a conflict. This is the same pattern as an ETag/`If-Match`.

4. **Use a Room transaction for multi-statement invariants.**
   ```kotlin
   db.withTransaction {
       val acc = accountDao.getById(id) ?: return@withTransaction
       if (acc.balance < amount) return@withTransaction
       accountDao.update(acc.copy(balance = acc.balance - amount, version = acc.version + 1))
       ledgerDao.insert(LedgerEntry(id, -amount))
   }
   ```
   `withTransaction` gives atomicity across the two writes (balance + ledger entry) and serialises writers against each other. Note that Room's `withTransaction` **cannot** be nested and it dispatches to Room's transaction executor — it is not a lock you hold while doing network I/O.

5. **Add an in-process `Mutex` only where the invariant spans several statements *and* the database cannot express it.** Be honest in the interview: a `Mutex` protects only one process and only one instance of the repository. It is a supplement to the DB-level guarantee, never a replacement.

6. **Make retries idempotent.** If the write is retried (network failure after the server committed), a client-side `Mutex` gives you nothing. Persist an idempotency key with the write; the server deduplicates. This is the same principle as Section 1 Q1.

7. **Push the invariant into the DB where possible.** Triggers, `CHECK` constraints (`CHECK (balance >= 0)`) and unique indices are the last line of defence — they hold even if a developer forgets the guard in one code path.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Atomic single-statement UPDATE with a guard | Simplest; no lost update; no read-modify-write | Only works when the update is expressible as a delta/SQL expression |
| Optimistic concurrency with a version column | Explicit conflict detection; no locks; scales | Needs retry/rebase logic; conflicts must be surfaced if the retry budget is exhausted |
| `@Transaction` / `withTransaction` | Atomic multi-statement updates; the DB enforces it | Room serialises writers → contention; cannot span network calls |
| `Mutex` in the repository | Cheap; easy to reason about | **Process-local only**; serialises everything; a `Mutex` does not give durability |
| Pessimistic `SELECT ... FOR UPDATE` | Strongest isolation | SQLite's locking model + Room's single-writer executor make this awkward and slow |
| Serialise writes through a single actor | No lost updates; simple reasoning | One slow write blocks all others; the actor becomes a bottleneck |
| Server-side atomicity + idempotency key | Correct even across processes/devices | Requires backend support; the client still needs a conflict strategy |

**Key detail:** Room's suspend DAO functions and `withTransaction` run on **Room's transaction executor**, not on the caller's dispatcher — so `withTransaction` is safe to call from `Dispatchers.Main`, but you must not do long work inside it (it holds the database's write lock). And a `Mutex` in the repository protects against a lost update only within that process; the durable answer is a version column or a conditional UPDATE.

**Code example**

```kotlin
@Dao
interface AccountDao {
    // Atomic debit: the guard and the write are ONE statement — no lost update.
    @Query("""
        UPDATE accounts
           SET balance = balance - :amount,
               version = version + 1
         WHERE id = :id
           AND balance >= :amount
    """)
    suspend fun debit(id: String, amount: Long): Int      // rows affected

    // Optimistic concurrency: the write only lands if nobody else moved first.
    @Query("""
        UPDATE accounts
           SET balance = :newBalance,
               version = version + 1
         WHERE id = :id AND version = :expectedVersion
    """)
    suspend fun updateIfVersion(id: String, newBalance: Long, expectedVersion: Long): Int

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: String): Account?
}

class AccountRepository(
    private val db: AppDatabase,
    private val api: PaymentsApi,
) {
    private val accountDao = db.accountDao()
    private val ledgerDao = db.ledgerDao()

    // Single statement → atomic, no read-modify-write.
    suspend fun debit(id: String, amount: Long): Result<Unit> {
        val affected = accountDao.debit(id, amount)
        return if (affected == 1) Result.success(Unit)
        else Result.failure(InsufficientFunds)     // guard failed → re-read and decide
    }

    // Multi-statement invariant → one transaction.
    suspend fun transfer(from: String, to: String, amount: Long): Result<Unit> =
        db.withTransaction {
            val acc = accountDao.getById(from) ?: return@withTransaction Result.failure(NotFound)
            if (acc.balance < amount) return@withTransaction Result.failure(InsufficientFunds)
            accountDao.updateIfVersion(from, acc.balance - amount, acc.version)
            val target = accountDao.getById(to)!!
            accountDao.updateIfVersion(to, target.balance + amount, target.version)
            ledgerDao.insert(LedgerEntry(from, to, amount, idempotencyKey))
            Result.success(Unit)
        }

    // Write to the server with an idempotency key so a retry cannot double-apply.
    suspend fun syncToServer(id: String, key: String): Result<Unit> =
        runCatching { api.pushMutation(id, amount, idempotencyKey = key) }
}
```

**Talking points / follow-ups**
- *"Why not just use a `Mutex` around the read and the write?"* → It prevents the lost update inside one process, but it is not durable and does not survive a process restart. The DB-level conditional update is the real guarantee.
- *"How do you detect that a conflict happened?"* → The affected-row count is 0 for a conditional UPDATE — that is the signal, no exception is thrown.
- *"What if the transaction spans a network call?"* → Don't. Keep the DB transaction short; do the network call outside and use an outbox row (Section 1 Q10) for the remote effect.
- *"How does this interact with Room's single-writer executor?"* → Room serialises writers anyway, so an in-process `Mutex` mostly adds contention — but the *conditional update* is still needed, because the serialisation does not make read-then-write atomic.
- *"Should the check constraint live in the DB?"* → Yes — `CHECK (balance >= 0)` as the last line of defence against a code path that forgets the guard.

---

### 11. A Flow pipeline performs expensive transformations and causes performance problems. How would you optimize its execution?

**What the interviewer is really asking**
Whether you can profile a Flow pipeline before optimising it, and whether you know the four levers that actually matter: **where the work runs** (`flowOn`), **how often it runs** (share the upstream, `distinctUntilChanged`), **how much work is done** (`mapLatest`/`flatMapLatest` cancellation), and **whether the UI is even observing it** (`collectAsStateWithLifecycle`, `collectLatest`).

**Key concepts**
- `flowOn` — changes the context of the **upstream** of that operator; it does not affect the collector, and inside a `channelFlow` it does not affect the upstream of the `channelFlow` block
- `buffer()` / `conflate()` / `BufferOverflow` — decoupling the producer from the consumer
- `shareIn` / `stateIn` — run the upstream **once** for N collectors
- `distinctUntilChanged` — skip redundant work when the source re-emits equal values
- `mapLatest` / `flatMapLatest` — cancel the in-flight transform when a new value arrives
- `sample()` / `debounce()` — rate limiting of the downstream
- `collectLatest` — cancels the *collector body* for the previous item
- `Dispatchers.Default` for CPU work; `Dispatchers.IO` for blocking I/O
- `combine` vs nested `flatMapLatest`

**Approach**

1. **Measure before optimising.** Use `flowOn` + a timing operator, or simply instrument: log the time spent per operator. Common findings: the upstream is being **collected again per collector** (N screens → N network calls), a CPU-heavy `map` is running on `Dispatchers.Main`, or a downstream collector is doing work per item that should be batched.

2. **Share the upstream.** The single most common Flow performance bug: a cold flow collected by three collectors runs three times.
   ```kotlin
   val items: StateFlow<ItemsState> = repository.observeItems()
       .map { transform(it) }              // expensive — run it ONCE
       .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ItemsState.Idle)
   ```
   `stateIn`/`shareIn` run the upstream in the scope's context and share it across collectors.

3. **Move the work off the main thread — and get `flowOn` right.**
   ```kotlin
   val parsed = rawFlow
       .map { json.parse(it) }             // CPU-heavy
       .flowOn(Dispatchers.Default)        // affects everything ABOVE this line only
   ```
   `flowOn` applies to the **upstream** of the call. If you write `flow { ... }.flowOn(Dispatchers.IO)`, only the operators *above* `flowOn` move. Two facts people get wrong:
   - The **collector**'s context is never changed by `flowOn` — the collection still happens in the collector's context.
   - Inside a `channelFlow`/`callbackFlow`, `flowOn` does not move the `channelFlow` block itself; use `withContext` inside it.

4. **Avoid redundant recomputation with `distinctUntilChanged`, placed early.**
   ```kotlin
   dao.observeIds()
       .distinctUntilChanged()                 // cheap, early
       .flatMapLatest { ids -> fetchDetails(ids) }   // expensive work only when ids change
   ```
   If the expensive step is upstream of the `distinctUntilChanged`, you have already done the work.

5. **Cancel superseded work with `mapLatest` / `flatMapLatest`.** If a new value arrives while the transform is running, the previous one is usually obsolete:
   ```kotlin
   queryFlow
       .debounce(300)
       .mapLatest { q -> expensiveTransform(q) }   // cancels the previous transform
   ```
   (`mapLatest` cancels the transform; `flatMapLatest` cancels the previous inner *flow*.)

6. **Batch the downstream.** If the consumer does per-item work, collect a list instead:
   ```kotlin
   itemsFlow
       .chunked(50)                 // or `.buffer(50)` / custom batching
       .collect { batch -> processBatch(batch) }
   ```

7. **Decouple producer and consumer with `buffer`.** `buffer()` lets the producer run ahead on a separate coroutine; `conflate()` keeps only the latest value (good for "latest wins" UIs); `BufferOverflow.DROP_OLDEST` bounds memory for slow consumers.

8. **Do less work in the collector.** `collectLatest` cancels the previous item's processing — good for per-item navigation or writes. And prefer `collectAsStateWithLifecycle()` over `collectAsState()` so collection stops when the screen is backgrounded.

9. **Use `SharingStarted` deliberately.** `WhileSubscribed(5_000)` avoids re-running the upstream on every rotation; `Eagerly` keeps it running; `Lazily` starts on the first collector.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| `stateIn` / `shareIn` | One upstream for N collectors; the biggest single win | The upstream keeps running while subscribed; needs a sensible `SharingStarted`; the shared value can be stale for late subscribers |
| `flowOn(Dispatchers.Default)` | Keeps CPU work off the main thread | Only affects the **upstream**; a common misreading is that it moves the whole chain |
| `distinctUntilChanged` early | Skips redundant expensive work | Uses `equals` — must model equality correctly |
| `mapLatest` / `flatMapLatest` | Abandons superseded work | Cancels work that may have been nearly done; the cancelled work is wasted |
| `buffer()` / `conflate()` | Producer/consumer decoupling; bounds memory | `conflate` drops intermediate values — wrong for cumulative data; `buffer` increases memory |
| `chunked` / batching | Amortises per-item cost | Latency: the consumer waits for a full batch (unless the flow completes) |
| `collectLatest` in the consumer | Cancels obsolete per-item work | Requires the body to be cancellation-safe; re-reads of state must be cheap |

**Key detail:** `flowOn` does **not** change the context of the collector, and inside a `channelFlow` it does not move the `channelFlow` block — `flowOn` on a `channelFlow` only affects the operators above it. And `stateIn`/`shareIn` share the *upstream execution*, so a shared flow still performs its transform once per emission, not once per collector.

**Code example**

```kotlin
class FeedViewModel(repo: FeedRepository) : ViewModel() {

    // 1. The upstream runs ONCE, off the main thread; N collectors share it.
    val state: StateFlow<FeedState> = repo.observeFeedIds()          // Room Flow
        .distinctUntilChanged()                                       // cheap + early
        .flatMapLatest { ids -> repo.fetchDetails(ids) }              // cancels stale fetches
        .map { details -> FeedState.Ready(render(details)) }           // CPU work
        .flowOn(Dispatchers.Default)                                  // everything ABOVE moves
        .catch { e -> emit(FeedState.Error(e)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),          // survive rotation
            initialValue = FeedState.Loading,
        )

    // 2. Batch downstream work instead of per-item work.
    init {
        viewModelScope.launch {
            repo.observeFeedIds()
                .chunked(50)
                .collect { batch -> analytics.trackBatch(batch) }      // one call per 50 items
        }
    }

    // 3. Rate-limit a fast producer feeding a slow UI.
    val positions: StateFlow<PositionState> = repo.observePositions()
        .conflate()                                                   // keep only the latest
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PositionState.Idle)
}
```

**Talking points / follow-ups**
- *"How do you know the pipeline is the bottleneck?"* → Measure: add timing around the operator, or use Android Studio's coroutine debugger / `traceEvent` markers. Do not optimise blind.
- *"Why doesn't `flowOn` move my collector?"* → By design: `flowOn` only affects the upstream. To change the collector's context, wrap the collection site in `withContext`.
- *"What is the difference between `buffer()` and `conflate()`?"* → `buffer()` decouples with a bounded queue (no drops, but a slow collector grows memory up to the capacity); `conflate()` keeps only the newest value and drops the rest.
- *"Is `distinctUntilChanged` enough to stop duplicate work?"* → Only if placed **before** the expensive operator. After it, the work has already been done.
- *"How does this relate to Compose recomposition?"* → A Flow that emits too often causes recomposition of the collecting Composable; combine with `collectAsStateWithLifecycle` and stable state classes.

---

### 12. A coroutine exception crashes the application unexpectedly. How would you design structured concurrency and exception handling?

**What the interviewer is really asking**
Whether you understand the **exception propagation rules** of the coroutine hierarchy — where an exception goes, why `async` behaves differently from `launch`, why `CoroutineExceptionHandler` does not fire for `async`, and why `runCatching` is dangerous inside coroutines. And whether you have a **design** for the error vocabulary that keeps crashes out of the app.

**Key concepts**
- Structured concurrency: every coroutine belongs to a scope; a child's failure cancels its parent (except under a supervisor); a parent waits for all children
- `launch` vs `async`: `launch` reports the exception to the parent; `async` captures it in the `Deferred` and rethrows at `await()`
- `CoroutineExceptionHandler` — installed on the **root** scope only; ignored for `async` and for children of a `supervisorScope`
- `SupervisorJob` / `supervisorScope` — failure isolation
- `CancellationException` — always rethrown; it extends `IllegalStateException` ⇒ caught by `catch (e: Exception)`
- `runCatching` catches `Throwable` — including `CancellationException` — which breaks structured concurrency
- `SupervisorJob` vs `GlobalScope`
- Crash reporting: `Thread.setDefaultUncaughtExceptionHandler`, `Crashlytics`, `Application` scope handler
- `CoroutineStart.LAZY` / `UNDISPATCHED`; `CoroutineScope` in `ViewModel` vs `Application`

**Approach**

1. **Map the exception flow precisely.** A coroutine launched in a scope with no parent job is a **root** coroutine: its failure is handled by the scope's `CoroutineExceptionHandler` and, if there is none, by the default uncaught-exception handler → **the process crashes**. A child of a regular scope fails → the parent is cancelled → the parent's failure propagates up to the root → crash. A child of a `supervisorScope`/`SupervisorJob` fails → siblings survive, and the exception goes to the *supervisor's* handler (or the thread's default handler) — still a crash if unhandled!

   This is the key insight: **`supervisorScope` does not prevent the crash; it only prevents sibling cancellation.** You still need to catch the exception or install a handler.

2. **Decide the policy per layer.**
   - **At the UI/screen boundary** (`viewModelScope.launch`): the state should be captured into the UI state, not thrown. Use a typed `UiState.Error` — the screen must never crash the app.
   - **At the repository/API boundary**: translate transport exceptions into a domain error type (`AppError.Network`, `AppError.Http(code)`, `AppError.Timeout`) so the UI never sees `IOException`.
   - **At the scope boundary** (an app-level scope doing fire-and-forget work): install a `CoroutineExceptionHandler` that logs and reports, so an unexpected failure does not kill the process.

3. **Install the handler on the root scope.**
   ```kotlin
   val handler = CoroutineExceptionHandler { _, throwable ->
       crashReporter.log(throwable)
       Log.e("AppScope", "Unhandled coroutine error", throwable)
   }
   val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
   ```
   Remember: **the handler does not apply to `async`** — an `async` failure is stored in the `Deferred` and rethrown at `await()`. So an `async` whose result is never awaited swallows the error silently *and* — if the scope is a root scope — actually crashes via the default handler? No: for `async`, the exception is *not* reported to the parent at all; it is deferred until `await()`. An un-awaited `async` failure is simply lost. That is the "silent failure" trap.

4. **Never swallow `CancellationException`.**
   ```kotlin
   // WRONG — catches cancellation too and turns it into a "handled" error.
   runCatching { doWork() }
   // RIGHT
   try { doWork() }
   catch (e: CancellationException) { throw e }
   catch (e: IOException) { /* handle */ }
   ```
   `runCatching` returns `Result<T>` and catches `Throwable` — including `CancellationException` — so using it inside a coroutine silently breaks cancellation. Either avoid `runCatching` in coroutine code or always rethrow the cancellation explicitly.

5. **Never let a `try/catch` in a `finally` mask the original exception.** If `finally` throws (e.g. a suspending call that fails), the original exception is lost. Use `withContext(NonCancellable)` for essential cleanup and keep it minimal.

6. **Isolate background work with `supervisorScope`.** Independent background tasks (analytics, sync, prefetch) must not cancel each other or the app:
   ```kotlin
   appScope.launch {
       supervisorScope {
           launch { syncA() }
           launch { syncB() }     // a failure here does not cancel syncA
       }
   }
   ```
   And each child should still catch its own exceptions if a crash is unacceptable.

7. **Add a last-resort safety net.** A `Thread.setDefaultUncaughtExceptionHandler` in `Application.onCreate` that reports and then delegates to the previous handler. This does not fix the bug, but it gives you the crash data to prioritise it.

8. **Make failures observable in development.** Strict mode, `DebugProbes`, and a `CoroutineExceptionHandler` that logs with the full coroutine context (`CoroutineName`, thread name) so you can see *which* scope died.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Catch at the ViewModel/repository boundary | The app never crashes; the UI shows a state | Requires a typed error vocabulary; can hide genuine programming errors if over-used |
| `CoroutineExceptionHandler` on the root scope | Central logging/reporting; keeps the process alive | **Ignored for `async`** and for children of `supervisorScope`; easy to misplace |
| `supervisorScope` / `SupervisorJob` | Siblings survive; one task's failure does not kill the app | **Does not stop the crash** — an unhandled failure still reaches the default handler. Must also catch |
| `runCatching` everywhere | Concise | **Swallows `CancellationException`** → breaks structured concurrency and can leave zombie coroutines |
| Typed domain errors (`AppError`) | The UI never sees transport exceptions; testable | Boilerplate mapping; every new error type needs a UI treatment |
| Global uncaught-exception handler | Catches everything, including non-coroutine crashes | Reactive only; cannot prevent the crash; must delegate to the previous handler |
| Never letting exceptions cross the scope boundary | Deterministic behaviour | Requires discipline at every `launch` site |

**Key detail:** `CoroutineExceptionHandler` is only consulted for a coroutine whose parent has no other failure handler — in practice, the **root** coroutine of a scope. It is ignored for `async` (the exception lives in the `Deferred`) and for children of a `supervisorScope` (which report to the supervisor's own handler). So `scope.async { ... }` with no `await()` anywhere is a **silent failure**, not a crash — the worst kind.

**Code example**

```kotlin
// 1. Root scope: handler + supervisor → one failing background task cannot kill the app.
val appScope = CoroutineScope(
    SupervisorJob() +
    Dispatchers.Default +
    CoroutineExceptionHandler { _, t ->
        crashReporter.report(t)
        Log.e("AppScope", "unhandled", t)
    }
)

// 2. Typed domain errors at the data layer.
sealed interface AppError { data object Network : AppError; data object Timeout : AppError; data class Http(val code: Int) : AppError }

suspend fun <T> apiCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e                                   // NEVER swallow cancellation
    } catch (e: SocketTimeoutException) {
        Result.failure(AppError.Timeout)
    } catch (e: IOException) {
        Result.failure(AppError.Network)
    }

// 3. The ViewModel converts failures into UI state — it never lets them escape.
class FeedViewModel(private val repo: FeedRepository) : ViewModel() {
    private val _state = MutableStateFlow<FeedState>(FeedState.Loading)
    val state: StateFlow<FeedState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = repo.load()
                .fold(onSuccess = { FeedState.Ready(it) }, onFailure = { FeedState.Error(it.toUiText()) })
        }
    }
}

// 4. Background tasks that must not affect each other.
fun startBackgroundWork(scope: CoroutineScope) {
    scope.launch {
        supervisorScope {
            launch { runCatching { syncA() } }
            launch { runCatching { syncB() } }
        }
    }
}
```

**Talking points / follow-ups**
- *"Does `supervisorScope` stop the crash?"* → No. It stops sibling cancellation; the exception still propagates to the supervisor's handler and, absent one, to the default handler → crash. You must catch it.
- *"Why doesn't my `CoroutineExceptionHandler` fire for `async`?"* → `async` captures the exception in the `Deferred`; the handler only applies to root coroutines. The exception resurfaces at `await()`.
- *"What is the difference between `launch` and `async` for error handling?"* → `launch` reports to the parent (cancelling it); `async` defers to `await()`.
- *"What happens to a `runCatching` around a cancelled coroutine?"* → It catches the `CancellationException` and returns `Result.failure(...)`; the coroutine then continues running in a cancelled state, and the next suspension point throws again. The parent is not notified → silent breakage.
- *"How do you make sure an `async` is not silently dropped?"* → Always `await()` it, or use `launch` for fire-and-forget work with an explicit handler.

---

### 13. How would you implement request deduplication for concurrent callers using Kotlin Coroutines?

**What the interviewer is really asking**
Whether you know the **single-flight** pattern and can implement it *correctly* — which means: one in-flight `Deferred` per key, all concurrent callers await the same one, the entry is removed when the flight completes (success **or** failure), and the entry is removed with an **identity check** so a slow cleanup cannot delete a newer flight. This is the same pattern as token-refresh single-flight (Section 1 Q8), generalised to arbitrary keys.

**Key concepts**
- Single-flight / request coalescing
- `Deferred` as the shared handle; `CompletableDeferred` for manual completion
- `Mutex` guarding the map of in-flight requests (not the request itself)
- Identity comparison (`===`) when clearing the map
- Scope ownership: the `Deferred` must be launched in a scope that **outlives** the callers
- Cancellation semantics: if the *first* caller is cancelled, the shared `Deferred` must not be cancelled for the others
- TTL / stale-while-revalidate as an extension (cache + single-flight)
- `Mutex` is **not** reentrant; `tryLock` for a "don't wait" variant

**Approach**

1. **Distinguish the two problems.** *Deduplication* = "while a request is in flight, join it" (single-flight). *Caching* = "after it completes, reuse the answer for a while" (TTL cache). They compose: the cache is the store, the single-flight is the guard on the store.

2. **Keep the in-flight map guarded by a `Mutex`, and the request itself unguarded.** Holding the `Mutex` during the network call would serialise *different* keys as well — wrong. The `Mutex` protects only the map operations:
   ```kotlin
   private val lock = Mutex()
   private val inFlight = mutableMapOf<String, Deferred<Result<Data>>>()
   ```

3. **Implement the join-then-start protocol.**
   ```kotlin
   suspend fun load(key: String): Result<Data> {
       lock.withLock { inFlight[key] }?.let { return it.await() }   // 1. join
       val deferred = scope.async { runCatching { fetch(key) } }    // 2. start
       lock.withLock { inFlight[key] = deferred }                  // 3. publish
       return try {
           deferred.await()
       } finally {
           lock.withLock { if (inFlight[key] === deferred) inFlight.remove(key) }  // 4. clear
       }
   }
   ```
   The `===` identity check in step 4 is essential: if flight A is slow to clean up and flight B has already started for the same key, `remove` without identity would delete B's entry and allow a third flight.

4. **Get the cancellation semantics right — this is the subtle part.** If the `Deferred` is created in the *caller's* scope, then caller #1 cancelling cancels the shared request and caller #2 gets a `CancellationException` even though it never asked for cancellation. Therefore:
   - Launch the `Deferred` in a scope that **outlives** every caller (the application scope, or the repository's own scope).
   - The callers `await()` it; if a *caller* is cancelled, its `await()` throws `CancellationException` but the underlying request continues for the others.

5. **Do not hold the `Mutex` while awaiting.** `await()` inside `withLock` would hold the lock for the whole network call, serialising all keys.

6. **Add a cache layer** so callers arriving *after* completion still avoid the network (with a TTL). Store the completed value alongside its timestamp in the same `Mutex`-protected structure, or in a separate `AtomicReference` snapshot.

7. **Bound the map.** If keys are unbounded (e.g. a URL), the in-flight map leaks entries for keys whose flights never complete. Use a bounded map or remove entries eagerly as above; a `finally` guarantees removal even on failure.

8. **Distinguish "join" from "queue".** If you want the second caller to *wait and then issue its own* request (rare), use a `Semaphore`; for the common case, joining the in-flight flight is what you want.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Single-flight `Deferred` map | Exact dedup of concurrent callers; the failure is shared too | Manual bookkeeping; the map must be cleaned up correctly (identity check) |
| `Mutex` around the whole request | Simplest code | **Serialises all requests**, even for different keys — a throughput bug |
| `Mutex` + double-checked cache | Serves cached values too | Stale data; TTL policy needed |
| `Channel`-based request queue | Serialises work; gives ordering | Serialises everything; latency for unrelated keys |
| `Semaphore` per key | Bounds concurrency per key | Harder to reason about; still duplicates requests |
| `runBlocking` + a blocking cache (e.g. Guava) | Reuses a battle-tested implementation | Blocking; not coroutine-native; must not be called from the main thread |
| `stateIn`/`shareIn` on a hot flow | Framework-managed sharing for *observers* | Only dedups *collectors of the same flow*, not arbitrary `suspend` callers |

**Key detail:** the `Deferred` must be created in a scope that outlives the callers, otherwise the first caller's cancellation kills the shared request. A common variant that avoids this entirely is to make the repository expose a **hot `StateFlow`** and have all callers read that — the framework then owns the lifetime.

**Code example**

```kotlin
@Singleton
class RequestCoalescer<T>(
    private val scope: CoroutineScope,           // app-scoped: outlives every caller
) {
    private val lock = Mutex()
    private val inFlight = mutableMapOf<String, Deferred<Result<T>>>()
    private val cache = mutableMapOf<String, Pair<T, Long>>()   // value + timestamp
    private val ttlMillis = 30_000L

    suspend fun get(key: String, fetch: suspend () -> T): Result<T> {
        // 1. Fresh cache hit?
        lock.withLock {
            cache[key]?.takeIf { now() - it.second < ttlMillis }?.let { return Result.success(it.first) }
        }
        // 2. Join an existing flight (do NOT await inside the lock).
        lock.withLock { inFlight[key] }?.let { return it.await() }
        // 3. Start a new flight in the long-lived scope.
        val deferred = scope.async { runCatching { fetch() } }
        lock.withLock { inFlight[key] = deferred }
        return try {
            val result = deferred.await()
            result.onSuccess { value -> lock.withLock { cache[key] = value to now() } }
            result
        } finally {
            // Identity check: a late cleanup must not delete a newer flight.
            lock.withLock { if (inFlight[key] === deferred) inFlight.remove(key) }
        }
    }
}

// Usage: N concurrent screens → ONE network call.
class UserRepository(private val coalescer: RequestCoalescer<List<User>>) {
    suspend fun users(): Result<List<User>> = coalescer.get("users") { api.fetchUsers() }
}
```

**Talking points / follow-ups**
- *"Why not just use a `Mutex` and check a `isLoading` flag?"* → It serialises unrelated requests and can leave the flag stuck on failure; the `Deferred` map handles both cleanly.
- *"What if the request fails?"* → All joined callers receive the same failure (from the shared `Deferred`), and the entry is removed in the `finally` so the next caller retries.
- *"How does this interact with a TTL cache?"* → The cache serves completed values; the single-flight collapses concurrent misses. Together they give "one request per TTL window per key".
- *"How would you test it?"* → Launch 10 coroutines against a fake that counts calls and delays; assert `callCount == 1` and that all 10 received the same value.
- *"Does this work across processes?"* → No — the map is in memory. A multi-process app needs a shared store (a ContentProvider, or a server-side request cache).

---

### 14. A Mutex protects shared state but creates contention. How would you identify whether another synchronization strategy is more appropriate?

**What the interviewer is really asking**
Whether you can *diagnose* contention rather than guess: measure where the time goes, decide whether the lock is protecting the right thing, and know the menu of alternatives (atomics, confinement, finer granularity, lock-free structures, immutable state, `tryLock`, `Semaphore`). A strong answer also asks the design question: **why is there shared state at all?** — often the right fix is to remove it.

**Key concepts**
- Contention: waiting time, not just lock hold time; measure with instrumentation
- `Mutex.isLocked` / `tryLock` — inspect and probe without blocking
- Critical-section size vs. contention volume; the classic fix is to shrink the critical section
- Lock striping / per-key locks
- Thread confinement (`newSingleThreadContext`, actor) — no locks at all
- `AtomicReference` / `AtomicInteger` / `compareAndSet` — lock-free for single variables
- `ConcurrentHashMap` / `CopyOnWriteArrayList` — concurrent collections
- `Semaphore` — bounds concurrency rather than serialising it
- `ReentrantLock` with `tryLock` — fail fast with a fallback
- The `Mutex` non-reentrancy trap (deadlock, not contention)

**Approach**

1. **Measure first.** Instrument the critical section:
   ```kotlin
   private val mutex = Mutex()
   private val waitNanos = AtomicLong(0)

   suspend fun <T> guarded(block: suspend () -> T): T {
       val start = System.nanoTime()
       mutex.lock()
       val waited = System.nanoTime() - start
       waitNanos.addAndGet(waited)
       try { return block() } finally { mutex.unlock() }
   }
   ```
   Compare **wait time** with **hold time**. If wait time dominates, the problem is contention (too many callers); if hold time dominates, the problem is that the critical section is too big.

2. **Shrink the critical section — the highest-leverage fix.** Almost always, the lock is held across work that does not need to be atomic: a network call, a disk write, a logging call. Move those out:
   ```kotlin
   // BAD: the network call is inside the lock — everything else stalls for 300 ms.
   mutex.withLock { cache[key] = fetch(key) }
   // GOOD: fetch outside, publish inside (and single-flight so only one fetch happens).
   val value = fetch(key)
   mutex.withLock { cache[key] = value }
   ```

3. **Reduce the number of callers.** Contention is a function of arrival rate × hold time. If you hold the lock for 1 ms and 1000 coroutines/s arrive, the queue is saturated. Options:
   - **Batch** the updates (accumulate in an `AtomicReference` and flush periodically).
   - **Coalesce** redundant work (single-flight, `distinctUntilChanged`).
   - **Try-lock with a fallback**: if the lock is busy, compute the value optimistically and reconcile later.

4. **Choose the right primitive for the invariant.**
   - Single value, independent updates → `AtomicInteger` / `AtomicReference` (lock-free).
   - Multi-value invariant → `Mutex`, but only around the actual mutation.
   - Many independent keys → **lock striping**: a fixed array of locks indexed by key hash.
   - "At most N concurrent operations" → `Semaphore(N)`.
   - A whole subsystem's state → **confinement** (an actor / single-threaded dispatcher) — no locks, total ordering, easy to reason about.

5. **Use `tryLock` when waiting is not acceptable.**
   ```kotlin
   if (!mutex.tryLock()) {
       return cachedStaleValue            // fall back rather than queue
   } else {
       try { ... } finally { mutex.unlock() }
   }
   ```
   This converts contention into *staleness*, which is often the right trade for a UI cache.

6. **Remove the shared state.** The best answer to "should we keep the `Mutex`?" is often "no". If the state can be modelled as a `StateFlow` written by a single owner, there is no contention because there is only one writer. Confinement is strictly better than mutual exclusion when the work is naturally sequential.

7. **Check for the pathological case: a deadlock masquerading as contention.** If throughput goes to zero, it is not contention — it is a deadlock. The usual causes in coroutine code:
   - **`Mutex` is not reentrant**: `withLock` inside `withLock` on the same mutex → permanent hang.
   - Holding the lock while awaiting a `Deferred` that itself needs the lock.
   - `withLock` around a suspension point that waits for another coroutine that needs the same lock.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Shrink the critical section | Cheapest fix; keeps correctness | Requires care that the shrunk section is still atomic |
| `AtomicReference` / CAS | Lock-free; scales to high update rates | Only for single variables; CAS loops can spin |
| Lock striping / per-key locks | Contention divided across keys | More locks; ordering across keys is not guaranteed; stripe count must be tuned |
| Thread confinement / actor | No locks; total order; trivial reasoning | Everything is serialised; the slowest task stalls the rest |
| `tryLock` with a stale fallback | The UI never blocks on a lock | Returns stale or approximate data under contention |
| `Semaphore(N)` | Bounds concurrency; allows parallelism up to N | More complex reasoning; still a queue |
| `ReentrantLock` with `tryLock` | Familiar JVM semantics; reentrant | Blocks the thread; must not be held across suspension |
| Removing the shared state | Eliminates the problem entirely | Requires an architectural change |

**Key detail:** the diagnostic that distinguishes **contention** from **deadlock** is throughput: with contention, latency is high but progress continues; with a deadlock, progress stops entirely. And in coroutine code, the most common "deadlock" is not a lock-ordering bug at all — it is `Mutex` reentrancy, since `kotlinx.coroutines.sync.Mutex` explicitly does not support reentrant acquisition.

**Code example**

```kotlin
class MetricsRegistry {

    // 1. Lock-free counters — no mutex at all.
    private val requests = AtomicLong(0)
    private val errors = AtomicLong(0)

    // 2. A snapshot of aggregates, published atomically (copy-on-write).
    private val snapshot = AtomicReference<Map<String, Long>>(emptyMap())
    private val publishMutex = Mutex()

    fun recordRequest(path: String) { requests.incrementAndGet() }

    suspend fun record(path: String, durationMillis: Long) {
        // The expensive/IO part is OUTSIDE the lock.
        val normalised = normalise(path)
        publishMutex.withLock {                       // tiny critical section
            val current = snapshot.get()
            snapshot.set(current + (normalised to (current[normalised] ?: 0L) + durationMillis))
        }
    }

    // 3. Lock striping: one mutex per key-bucket, so unrelated keys do not contend.
    private val stripes = Array(32) { Mutex() }
    private fun stripeFor(key: String) = stripes[(key.hashCode() and 0x7fffffff) % stripes.size]

    suspend fun updateUser(key: String, block: suspend () -> Unit) {
        stripeFor(key).withLock { block() }            // only same-stripe keys serialise
    }

    // 4. tryLock with a graceful fallback instead of queueing.
    suspend fun computeOrFallback(key: String, compute: suspend () -> Long): Long {
        if (!stripeFor(key).tryLock()) {
            return snapshot.get()[key] ?: 0L            // stale fallback: no waiting
        }
        return try {
            compute()
        } finally {
            stripeFor(key).unlock()
        }
    }
}
```

**Talking points / follow-ups**
- *"How do you detect a deadlock versus contention?"* → Throughput drops to zero and `Mutex.isLocked` stays `true` forever; contention shows high wait time with steady progress.
- *"How do you measure lock wait time?"* → Instrument with `System.nanoTime()` around `lock()`/`unlock()` (as above) and export the histogram — or use a library like `Ticker`/JFR in debug builds.
- *"Is `Mutex` faster than `ReentrantLock`?"* → Not inherently; `Mutex` is fair-ish and suspending, `ReentrantLock` is a JVM monitor. On Android, a `ReentrantLock` is cheaper but blocks the thread — the deciding factor is whether you ever suspend inside the critical section.
- *"What is the `isLocked` property for?"* → Diagnostics and for `tryLock`-style fast paths; it is a snapshot, not a guarantee (the lock may be released right after you check).
- *"When would you use a `Semaphore` instead of a `Mutex`?"* → When you want to allow N concurrent operations rather than serialising to one (e.g. at most 4 parallel image decodes).

---

### 15. How would you implement cancellation-safe retries with exponential backoff and jitter?

**What the interviewer is really asking**
Whether you know that **`delay()` is itself cancellable** (so backoff respects cancellation), that **`CancellationException` must be rethrown** rather than retried, and that a retry policy needs a *budget* (attempts **and** total time) plus a *classification* of what is retryable. The staff-level extension: the `kotlinx.coroutines.retry` API, `Retry-After`, and the interaction with idempotency (see Section 1 Q7 and Q11).

**Key concepts**
- `kotlinx.coroutines.retry` — `retry(times) { cause, attempt -> delayMillis }` and `retryOnExceptionIf`
- Exponential backoff with **full jitter** (`randomInRange(0, ceiling)`) — the AWS "Exponential Backoff and Jitter" result
- `delay()` is cancellable and throws `CancellationException` when the scope is cancelled
- `CancellationException` extends `IllegalStateException` ⇒ a `catch (e: Exception)` will catch it — it must be rethrown first
- Retry *classification*: transport errors, 408/429/5xx retryable; 4xx (except 408/429) not
- `Retry-After` header (seconds or HTTP-date) is authoritative
- Budgets: max attempts **and** a total elapsed-time ceiling
- `withTimeout` wrapping the whole retry loop
- Idempotency requirement (Section 1 Q1) — retrying a non-idempotent POST without a key duplicates it

**Approach**

1. **Write the loop by hand first — it is 15 lines and shows you understand the mechanics.**
   ```kotlin
   suspend fun <T> retrying(
       maxAttempts: Int = 3,
       baseDelayMs: Long = 500,
       maxDelayMs: Long = 30_000,
       shouldRetry: (Throwable) -> Boolean = { it is IOException },
       block: suspend () -> T,
   ): T {
       var delayMs = baseDelayMs
       repeat(maxAttempts) { attempt ->
           try {
               return block()
           } catch (e: CancellationException) {
               throw e                                          // 1. cancellation is never retried
           } catch (t: Throwable) {
               if (attempt == maxAttempts - 1 || !shouldRetry(t)) throw t
               // 2. full jitter: random in [0, ceiling]
               val ceiling = min(maxDelayMs, baseDelayMs * (1L shl attempt))
               delay(Random.nextLong(ceiling / 2, ceiling))      // 3. delay() is cancellable
               delayMs = (ceiling * 2).coerceAtMost(maxDelayMs)
           }
       }
       error("unreachable")
   }
   ```
   Point out the three load-bearing lines: the `CancellationException` rethrow, the cancellable `delay`, and the jittered ceiling.

2. **Use the built-in `kotlinx.coroutines.retry` for the standard case.**
   ```kotlin
   import kotlinx.coroutines.retry

   val result = api.fetch() .retryOnExceptionIf { cause, attempt ->
       cause is IOException && attempt < 3
   } { cause, attempt ->
       val ceiling = min(30_000L, 500L * (1L shl attempt))
       Random.nextLong(ceiling / 2, ceiling)
   }
   ```
   `retry` returns the value on success, rethrows the last cause on failure, and the `delayMillis` lambda is evaluated *before* the next attempt. `retryOnExceptionIf` lets you classify — and you can check `cause is CancellationException` inside the predicate and return `false` so cancellation is not retried (the library already does not retry on cancellation for `retry`, but be explicit).

3. **Classify what is retryable.** Never retry everything:
   ```kotlin
   fun isRetryable(t: Throwable): Boolean = when (t) {
       is SocketTimeoutException, is ConnectException, is UnknownHostException -> true
       is HttpException -> t.code() in setOf(408, 429) || t.code() in 500..599
       else -> false
   }
   ```
   A 401, a 400, or a validation error will never succeed on a retry.

4. **Honour `Retry-After`.** When the server sends it, it wins over your own backoff — but never retry *sooner* than it says:
   ```kotlin
   val serverDelay = response.headers()["Retry-After"]?.toLongOrNull()?.times(1000)
   val wait = maxOf(serverDelay ?: 0L, jittered(attempt))
   ```

5. **Bound the total time, not just the attempts.** Wrap the whole loop in `withTimeoutOrNull` so a retry storm cannot outlive the user's patience:
   ```kotlin
   withTimeoutOrNull(15_000) { retrying(maxAttempts = 5) { api.fetch() } }
   ```

6. **Make retries idempotency-safe.** Retrying a `POST /payments` without an idempotency key duplicates the charge. Retrying is only safe for idempotent operations or when a key is present (Section 1 Q1/Q3).

7. **Propagate cancellation through the retry loop.** Because `delay()` throws `CancellationException` when cancelled, a cancelled scope aborts the loop immediately — *provided* you do not swallow the exception in the `catch`. This is the whole "cancellation-safe" requirement.

8. **Do not retry forever.** Every retry policy needs a maximum attempt count and a total-time budget; otherwise a permanently failing dependency becomes a battery drain and a server load amplifier.

**Trade-offs**
| Decision | Pros | Cons |
|---|---|---|
| Hand-written loop | Full control over classification, budget and jitter | Boilerplate; easy to get the `CancellationException` handling wrong |
| `kotlinx.coroutines.retry` | Small, well-tested, cancellable | Classification is via a predicate; less control over the delay source (e.g. `Retry-After`) |
| Exponential backoff **with jitter** | Avoids the thundering herd; measurably better than no jitter | Random ⇒ harder to test deterministically (inject a `Random`/delay source) |
| Exponential backoff **without jitter** | Deterministic, easy to test | Clients retry in lockstep — the classic retry storm |
| Retry only idempotent operations | Correctness | Requires knowing the API's semantics; non-idempotent calls get no auto-retry |
| Honour `Retry-After` | Polite; matches server load | A hostile/large `Retry-After` can stall the UX — cap it |
| Total-time budget (`withTimeout`) | Bounds the user-visible wait | Kills retries that would have succeeded later |

**Key detail:** `delay()` is cancellable — it throws `CancellationException` immediately when the scope is cancelled, so backoff respects cancellation for free. The failure mode is never `delay` itself but a `catch (e: Exception)` in the retry block that catches the `CancellationException` and starts another attempt; the loop then keeps running in a cancelled scope until it exhausts its attempts. Always rethrow cancellation **before** classifying anything.

**Code example**

```kotlin
import kotlinx.coroutines.retry

// 1. Built-in API with jitter.
suspend fun <T> fetchWithRetry(block: suspend () -> T): T =
    block.retryOnExceptionIf { cause, attempt ->
        cause !is CancellationException && cause is IOException && attempt < 3
    } { _, attempt ->
        val ceiling = min(30_000L, 500L * (1L shl attempt))
        Random.nextLong(ceiling / 2, ceiling)         // full jitter
    }

// 2. Hand-written version with classification, Retry-After and a total budget.
suspend fun <T> retrying(
    maxAttempts: Int = 3,
    baseDelayMs: Long = 500,
    maxDelayMs: Long = 30_000,
    totalBudgetMs: Long = 15_000,
    retryAfterMillis: (Throwable) -> Long? = { null },
    shouldRetry: (Throwable) -> Boolean = { it is IOException },
    block: suspend () -> T,
): T = withTimeoutOrNull(totalBudgetMs) {
    var attempt = 0
    while (true) {
        try {
            return@withTimeoutOrNull block()
        } catch (e: CancellationException) {
            throw e                                   // never retried, never swallowed
        } catch (t: Throwable) {
            if (attempt >= maxAttempts - 1 || !shouldRetry(t)) throw t
            val ceiling = min(maxDelayMs, baseDelayMs * (1L shl attempt))
            val jittered = Random.nextLong(ceiling / 2, ceiling)
            val serverHint = retryAfterMillis(t) ?: 0L
            delay(maxOf(serverHint, jittered))        // cancellable backoff
            attempt++
        }
    }
} ?: throw TimeoutCancellationException("retry budget exhausted")
```

**Talking points / follow-ups**
- *"Why jitter?"* → Without it, every client that failed at the same time retries at the same time — the "thundering herd". Full jitter (uniform in [0, ceiling]) is measurably better than equal jitter.
- *"How do you test the backoff schedule?"* → `runTest` uses virtual time: `delay()` does not actually wait, so the test finishes instantly and you can assert `currentTime` equals the expected cumulative delay.
- *"What if the server returns `Retry-After: 120`?"* → Honour it but cap it (e.g. 60 s) and consider failing the request with a "try again later" state rather than holding the connection.
- *"Should a 401 be retried?"* → No — refresh the token once (single-flight, Section 1 Q8) and retry the original request at most once.
- *"Does `retry` retry a `CancellationException`?"* → `retry` rethrows cancellation; `retryOnExceptionIf` lets you exclude it explicitly. Be explicit in either case.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Structured concurrency & scope ownership** | 2, 3, 9, 12 — every coroutine has a parent; every scope has a lifetime; `GlobalScope` is never the answer |
| **Cancellation correctness** | 3, 12, 15 — `CancellationException` must always be rethrown; `NonCancellable` for cleanup; `delay()` is cancellable |
| **Choosing the right synchronisation primitive** | 1, 5, 10, 14 — atomics vs `Mutex` vs confinement vs a DB transaction; the `Mutex` is not reentrant |
| **Sharing and deduplication** | 4, 6, 13 — `stateIn` shares the upstream; single-flight collapses concurrent callers; TTL adds caching |
| **State vs events** | 7, 8 — `StateFlow` conflates by `equals`; `SharedFlow` with `replay = 0` drops; a `Channel` is a queue |
| **Performance of Flow pipelines** | 11 — share the upstream, move work off the main thread, dedup early, cancel superseded work |
| **Consistency boundaries** | 10, 13 — the database is the only durable boundary; `Mutex` is process-local and not a guarantee |

**The one sentence to memorise for this section:**
> *"Coroutines do not make concurrency safe — they make it structured. Every shared mutable thing needs an explicitly chosen primitive (an atomic, a mutex, a confinement boundary, or a database transaction), every scope needs a lifetime that matches the work it owns, and every failure needs a typed destination: the UI state, a domain error, or a crash report — never a silently swallowed exception."*
