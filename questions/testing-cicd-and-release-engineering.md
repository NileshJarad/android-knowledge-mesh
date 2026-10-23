# Testing, CI/CD and Release Engineering

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. A payment feature works manually but fails intermittently in automated tests. How would you make the tests deterministic?

**What the interviewer is really asking**
Can you distinguish **test flakiness** from a **real bug**, find the *source of nondeterminism* (time, concurrency, network, randomness, ordering, shared state), and fix the test harness rather than adding retries?

**Key concepts**
- Sources of nondeterminism: wall-clock time, `System.currentTimeMillis()`, coroutine dispatchers, `Thread.sleep`, network I/O, random ids, iteration order of `HashMap`/`HashSet`, static/global state, locale, timezone, and test ordering
- `kotlinx-coroutines-test`: `runTest`, `StandardTestDispatcher` vs `UnconfinedTestDispatcher`, `Dispatchers.setMain`, `advanceUntilIdle`, `advanceTimeBy`, `runCurrent`
- Virtual time — `runTest` skips `delay()` instantly, so tests are fast *and* deterministic
- `Instant`/`Clock` injection instead of reading the system clock directly
- Randomness — inject a seeded `Random` / `FakeRandom`
- Test isolation — no shared static state; `@Before`/`@After` resetting singletons
- Robolectric vs instrumentation tests vs JUnit4 vs JUnit5
- Flake detection: a test that fails >X% of runs; quarantine, not retry
- `kotlinx-coroutines-test` `TestScope` and the `Dispatchers.setMain` requirement
- Turbine for Flow testing (`app.cash.turbine:turbine`)
- Idempotency and retries masking a bug (retrying is an anti-pattern for a deterministic bug)

**Approach**

1. **First establish that it is really flaky, and measure the flake rate.** A test that fails 2% of the time is a *flaky test*; a test that fails 30% of the time is usually a **real bug** that the manual path happens not to trigger. Run the test in a loop (100–1000 iterations) in CI and record the pass rate. This distinction changes the entire approach: a real bug goes to the product owner, not to the test harness.

2. **Make time deterministic.** Replace every direct read of the system clock with an injected `Clock`/`TimeProvider`.
   ```kotlin
   // Bad
   val expiresAt = System.currentTimeMillis() + 60_000

   // Good
   interface TimeProvider { fun nowMillis(): Long }
   class SystemTimeProvider : TimeProvider { override fun nowMillis() = System.currentTimeMillis() }
   class FakeTimeProvider(var now: Long = 0L) : TimeProvider { override fun nowMillis() = now }
   ```
   Then the test controls time exactly. In the app, use `SystemClock.elapsedRealtime()` for *durations* (monotonic, unaffected by clock changes) and `System.currentTimeMillis()` only for wall-clock timestamps.

3. **Make coroutine timing deterministic.** Never `Thread.sleep` in a coroutine test. Use `runTest` and let virtual time do the work.
   ```kotlin
   @Test
   fun `token refresh happens before expiry`() = runTest {
       val clock = FakeTimeProvider(now = 1_000_000)
       val manager = TokenManager(api, clock)

       clock.now += 61_000                        // advance the *injected* clock
       val token = manager.getValidToken()

       assertTrue(api.refreshCallCount == 1)
       assertEquals("new-token", token)
   }
   ```
   Note that `runTest` uses a `TestScope` with a virtual scheduler: `delay(5_000)` completes instantly but in the correct *order*. Use `advanceUntilIdle()` to run all pending virtual-time work, and `advanceTimeBy(ms)` to move to a specific point.

4. **Replace the main dispatcher in tests.** Any class that uses `Dispatchers.Main` (ViewModels, `viewModelScope`) will throw in a unit test unless you install a test dispatcher:
   ```kotlin
   @OptIn(ExperimentalCoroutinesApi::class)
   class MainDispatcherRule(
       private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
   ) : TestWatcher() {
       override fun starting(description: Description) { Dispatchers.setMain(dispatcher) }
       override fun finished(description: Description) { Dispatchers.resetMain() }
   }
   ```
   Using `UnconfinedTestDispatcher` makes the code under test run eagerly, which removes most timing assertions from the test. Using `StandardTestDispatcher` (the `runTest` default) requires explicit `advanceUntilIdle()`.

5. **Eliminate real network I/O.** Use `MockWebServer` (OkHttp) to serve **canned, deterministic responses**, not a live server. MockWebServer is deterministic because it replays a fixed `Dispatcher` — this is much better than mocking the HTTP client interface, because it exercises the real OkHttp stack (interceptors, serialisers, error mapping).
   ```kotlin
   val server = MockWebServer()
   server.enqueue(MockResponse().setResponseCode(200).setBody("""{"status":"ok"}"""))
   ```
   For determinism, use a `Dispatcher` that returns responses by path rather than by call order, so a reordering of requests does not break the test.

6. **Inject randomness.** Never call `Random.nextInt()` or `UUID.randomUUID()` in production code that a test touches. Inject a seeded `Random` or a `IdGenerator`:
   ```kotlin
   interface IdGenerator { fun next(): String }
   class SeededIdGenerator(seed: Long) : IdGenerator { /* deterministic sequence */ }
   ```

7. **Remove global/static state.** The classic flake source is a `companion object { var INSTANCE }` singleton, a `object UserSession`, or a DI graph that survives between tests. Options:
   - Prefer constructor injection of an interface over a global singleton.
   - If a singleton is unavoidable, expose a `reset()` and call it in `@After`.
   - Better: use a DI container that is recreated per test (Hilt's `@TestInstallIn` / a plain `TestComponent`), so the graph is fresh.

8. **Make timeouts and ordering explicit.** `withTimeout` in production code + virtual time in tests = deterministic. `Thread.sleep(100)` in a test = flake. Never rely on the relative completion order of two parallel coroutines — assert on the *final state*, not on the interleaving. If ordering genuinely matters, make it part of the design (a `Mutex`, a `Channel`, a `StateFlow` with `distinctUntilChanged`).

9. **Isolate the filesystem and the clock.** Use `TemporaryFolder`/a temp directory for file-backed tests, and never depend on the machine's timezone or locale (set `TimeZone.setDefault` in setup if needed).

10. **Do not paper over flakes with retries.** An automatic retry hides the problem and inflates CI time. Instead:
    - **Quarantine** the flaky test (mark it `@Ignore`/`@Flaky` and track it) so it does not block the pipeline.
    - **File a ticket** with the flake rate and the reproduction command.
    - Fix the root cause. Flakes erode trust in the whole suite, which is worse than a failing test.

11. **Instrument the flake detection in CI.** Run the suspicious test N times in a dedicated nightly job (`--rerun` style loops) and report the pass rate. A dashboard of flake rate per test makes prioritisation obvious.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Injecting a `TimeProvider` | Total control over time; fast; no flakes | A bit of production-code ceremony |
| `kotlinx-coroutines-test` virtual time | Deterministic ordering, instant `delay()` | Requires all time to go through coroutines; a `Thread.sleep` still flakes |
| `UnconfinedTestDispatcher` | Eager execution; fewer explicit advances | Can hide ordering bugs; deep call stacks |
| `StandardTestDispatcher` | Explicit, predictable execution order | Requires `advanceUntilIdle()` in every test |
| MockWebServer vs a mocked client | Exercises the real HTTP stack | Slightly slower; must manage the server lifecycle |
| Mocked repository interface | Fast, simple | Does not exercise serialisers/interceptors — hides real bugs |
| Retrying a flaky test in CI | Keeps the pipeline green | Hides the bug; inflates CI time; erodes trust |
| Quarantining | Unblocks delivery while the fix is scheduled | The test is not protecting the feature in the meantime |
| Seeding randomness | Deterministic | Requires an injection point |

**Key detail:** The fastest way to find the flake source is to **make time, randomness, and I/O controllable, then run the test in a loop**. If it passes 1000/1000 after that, the remaining flake is shared state — and shared state is found by running the test in different orders or in isolation.

**Code example**

```kotlin
// Deterministic time + deterministic dispatcher + deterministic ids.
interface TimeProvider { fun nowMillis(): Long }
interface IdGenerator { fun next(): String }

class FakeTimeProvider(var now: Long = 1_700_000_000_000L) : TimeProvider {
    override fun nowMillis(): Long = now
    fun advance(ms: Long) { now += ms }
}

class TokenManager(
    private val api: AuthApi,
    private val clock: TimeProvider,
    private val ids: IdGenerator,
) {
    private var token: String? = null
    private var expiresAt = 0L

    suspend fun getValidToken(): String {
        val current = token
        if (current != null && clock.nowMillis() < expiresAt) return current

        val response = api.refresh(ids.next())          // deterministic id
        token = response.token
        expiresAt = clock.nowMillis() + response.ttlMillis
        return response.token
    }
}

@Test
fun `refreshes the token exactly once when it expires`() = runTest {
    val server = MockWebServer().apply { start() }
    val api = AuthApi(server.url("/"))
    val clock = FakeTimeProvider()
    val manager = TokenManager(api, clock, SeededIdGenerator(seed = 42L))

    assertEquals("t1", manager.getValidToken())
    clock.advance(61_000)                 // move the injected clock past the TTL
    assertEquals("t2", manager.getValidToken())

    assertEquals(2, server.requestCount)  // deterministic: no flakiness
    server.shutdown()
}
```

**Talking points / follow-ups**
- *"Why not just retry the flaky test?"* → Retries hide real bugs, inflate CI time, and erode trust. Quarantine and fix the root cause.
- *"How do you find shared state between tests?"* → Run the test in isolation vs. in the suite; run the suite with randomised order (`junit-random`/`--random`); look for `object` singletons, static caches, and a DI graph that is not recreated.
- *"What is the difference between `StandardTestDispatcher` and `UnconfinedTestDispatcher`?"* → `Standard` queues tasks until the scheduler is advanced (`advanceUntilIdle`); `Unconfined` runs them eagerly at the point of dispatch. `runTest` defaults to `Standard`.
- *"How do you test something that depends on the current time of day?"* → Inject the clock; never read `Calendar.getInstance()` in production code.
- *"How do you detect flakiness in CI?"* → A nightly job that runs the suite repeatedly and reports per-test pass rates.

---

### 2. How would you test a ViewModel that combines several Flows and concurrent API requests?

**What the interviewer is really asking**
Do you know the **testing vocabulary** for coroutines and flows — `runTest`, `StandardTestDispatcher`, `advanceUntilIdle`, `UnconfinedTestDispatcher`, `Turbine`, `backgroundScope` — and can you assert on *intermediate* states without making the test timing-dependent?

**Key concepts**
- `runTest` / `TestScope` / `TestDispatcher`
- `UnconfinedTestDispatcher` vs `StandardTestDispatcher` — when to use each
- `advanceUntilIdle()` vs `advanceTimeBy(ms)` vs `runCurrent()`
- `backgroundScope` — for coroutines launched inside the `runTest` block that should be cancelled at the end of the test (this is what `viewModelScope` work usually needs)
- `kotlinx-coroutines-test` `TestScope` + `UnconfinedTestDispatcher` as the "eager" mode
- Turbine: `flow.test { assertEquals(Loading, awaitItem()); ... }`
- `stateIn`/`SharingStarted.WhileSubscribed` — the upstream is only alive while subscribed, so the test must subscribe
- `MockK`/`Mockito` + `coEvery`/`whenever`
- `kotlinx-coroutines-test` `runCurrent()` — run tasks scheduled at the *current* virtual time
- `Dispatchers.setMain` — required because `viewModelScope` uses `Dispatchers.Main.immediate`
- `TestScope.testScheduler` shared across `runTest` — virtual time is per-scheduler, so nested `runTest` calls share it

**Approach**

1. **Install a test dispatcher for `Dispatchers.Main`.** `viewModelScope` uses `Dispatchers.Main.immediate`; in a unit test you must replace it.
   ```kotlin
   @get:Rule val mainDispatcherRule = MainDispatcherRule()   // setMain(UnconfinedTestDispatcher())
   ```

2. **Use `runTest` as the test body.** It gives you a `TestScope` whose `delay()` calls are skipped instantly but ordered correctly.

3. **Choose the dispatcher mode deliberately.**
   - **`UnconfinedTestDispatcher`** — for the majority of ViewModel tests. Work starts eagerly, so by the time you `awaitItem()` the state has usually settled. Fewer `advanceUntilIdle()` calls, less boilerplate.
   - **`StandardTestDispatcher`** (the `runTest` default) — when you need to control *exactly* when things happen (e.g. "at t=0 the state is Loading, at t=1000 it is Success"). Requires explicit `advanceUntilIdle()`/`advanceTimeBy()`.

4. **Know `backgroundScope`.** This is the single most useful idiom for ViewModel tests. `viewModelScope` launches its coroutines on `Dispatchers.Main.immediate` — but `viewModelScope`'s job is **not** a child of the `runTest` scope, so it is *not* automatically cancelled when the test ends. The fix: run the ViewModel's work in `backgroundScope` by making the dispatcher injected, OR by cancelling the ViewModel at the end of the test:
   ```kotlin
   @Test
   fun `loads and exposes data`() = runTest {
       val viewModel = MyViewModel(repo, dispatcher = UnconfinedTestDispatcher(testScheduler))
       viewModel.load()

       assertEquals(UiState.Loading, viewModel.state.value)
       ...
       viewModel.onClearedForTest()   // or use `backgroundScope`
   }
   ```
   The idiomatic modern approach is to **inject the dispatcher** into the ViewModel and pass `backgroundScope` in tests, so the test framework cancels the work automatically:
   ```kotlin
   @Test
   fun `emits success then failure`() = runTest {
       val vm = MyViewModel(repo, scope = backgroundScope, dispatcher = UnconfinedTestDispatcher(testScheduler))
       vm.load()
       advanceUntilIdle()
       assertEquals(UiState.Success(data), vm.state.value)
   }
   ```
   `backgroundScope` coroutines are cancelled when the test body completes, so you never leak a running coroutine into the next test.

5. **Assert on intermediate states, not just the final one.** Use Turbine to observe the sequence of emissions:
   ```kotlin
   @Test
   fun `emits loading then success`() = runTest {
       repo.emitSuccess(listOf(item))
       viewModel.state.test {
           assertEquals(UiState.Loading, awaitItem())
           assertEquals(UiState.Success(listOf(item)), awaitItem())
           cancelAndIgnoreRemainingEvents()
       }
   }
   ```
   Turbine's `awaitItem()` suspends until an item arrives — with virtual time this is deterministic and fast, but it will **fail with a timeout** if the emission never happens, which is exactly the behaviour you want. Use `expectNoEvents()` to assert nothing was emitted.

6. **Test concurrent requests deterministically.** If the ViewModel fires two API calls in parallel, do not assert on which completes first. Instead:
   - Use a `CompletableDeferred` per call in the fake repository so you control **when** each completes:
     ```kotlin
     val call1 = CompletableDeferred<Result<Data>>()
     val call2 = CompletableDeferred<Result<Data>>()
     ```
   - Complete them in a defined order and assert the final state.
   - Assert on the *number* of calls and the final merged state, not on interleaving.

7. **Test cancellation.** Use `job.cancel()` on the ViewModel's job and assert that the repository call was cancelled:
   ```kotlin
   val job = launch { vm.load() }
   advanceUntilIdle()
   job.cancel()
   assertTrue(fakeApi.wasCancelled)
   ```
   Also verify that a `CancellationException` is **not** swallowed into an error state (a classic bug — see Section 3).

8. **Test the `stateIn` sharing behaviour.** If the ViewModel exposes `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)`, note that:
   - The upstream only runs **while there are subscribers**. A test that reads `state.value` without subscribing may not trigger the load at all. Either collect the flow in the test, or use `SharingStarted.Eagerly` in the ViewModel when the load should start on init.
   - After the last subscriber leaves, the upstream is stopped after the timeout — assert this if it matters.

9. **Prefer `UnconfinedTestDispatcher` for "did it ever reach this state" assertions**, and `StandardTestDispatcher` for ordering/timing assertions. Do not mix them casually inside one test — mixing makes the interleaving hard to reason about.

10. **Test the ViewModel, not the Compose UI, in unit tests.** Use `compose-ui-test` only for the behaviour that genuinely needs the UI (semantics, gestures). State and logic belong in the ViewModel where they are cheap to test.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `UnconfinedTestDispatcher` | Eager; minimal boilerplate; most tests just assert the final state | Hides ordering; deep stacks; can mask a race |
| `StandardTestDispatcher` | Explicit control over virtual time | Every test needs `advanceUntilIdle()`; more verbose |
| Turbine | Clean, readable assertions on the emission sequence | An extra dependency; `awaitItem()` needs a timeout for the failure case |
| Manual collection into a list | No dependency | Racy to write correctly; easy to miss an emission |
| `backgroundScope` | Automatic cancellation; no leaked coroutines | Requires injecting the scope into the ViewModel |
| Cancelling the ViewModel manually | No production-code change | Easy to forget → leaked coroutines → flaky suite |
| MockK for the repository | Concise `coEvery`/`coVerify` | Mocking too much means you are not testing behaviour |
| A fake repository with `CompletableDeferred` | Precise control over completion order | More code per test |
| Testing the ViewModel only (not Compose) | Fast, no instrumentation | Cannot assert on UI behaviour |

**Key detail:** `viewModelScope` is **not** a child of the `runTest` scope, so its coroutines are not cancelled when the test ends. Either inject the scope (`backgroundScope`) or cancel the ViewModel explicitly. Leaked coroutines from one test bleed into the next — this is one of the most common sources of "passes alone, fails in the suite" (see Q4).

**Code example**

```kotlin
@get:Rule val mainRule = MainDispatcherRule()   // Dispatchers.setMain(UnconfinedTestDispatcher())

class MyViewModel(
    private val repo: Repository,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load(id: String) {
        scope.launch(dispatcher) {
            _state.value = UiState.Loading
            val result = runCatching { repo.load(id) }
            _state.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message) },
            )
        }
    }
}

// --- test ---
@Test
fun `emits loading then success`() = runTest {
    val repo = FakeRepository()
    val vm = MyViewModel(repo, backgroundScope, UnconfinedTestDispatcher(testScheduler))

    vm.state.test {
        assertEquals(UiState.Loading, awaitItem())       // initial
        vm.load("42")
        assertEquals(UiState.Loading, awaitItem())       // reload
        repo.completeSuccess(listOf(Item("a")))
        assertEquals(UiState.Success(listOf(Item("a"))), awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
    // backgroundScope is cancelled automatically when the test ends.
}

@Test
fun `does not show an error when the coroutine is cancelled`() = runTest {
    val repo = FakeRepository()
    val vm = MyViewModel(repo, backgroundScope, UnconfinedTestDispatcher(testScheduler))
    val job = launch { vm.load("42") }
    advanceUntilIdle()
    job.cancel()
    advanceUntilIdle()

    assertTrue(vm.state.value is UiState.Loading)   // not an Error state
}
```

**Talking points / follow-ups**
- *"What is `advanceUntilIdle()` vs `advanceTimeBy(1000)`?"* → `advanceUntilIdle` runs everything scheduled at or before the current virtual time and beyond, until nothing is pending; `advanceTimeBy` moves the virtual clock forward by exactly that much, running tasks scheduled within that window.
- *"Why does my test hang?"* → Usually a coroutine on a real dispatcher, or a `CompletableDeferred` never completed, or the ViewModel's scope was never cancelled. `runTest` has a 60-second (real-time) timeout and reports the stuck coroutines.
- *"How do you test two parallel API calls?"* → Complete them explicitly with `CompletableDeferred` in a defined order; assert the final state and the call count, never the interleaving.
- *"What does `SharingStarted.WhileSubscribed` mean for tests?"* → The upstream only runs while collected; a test that only reads `.value` may not trigger the load.
- *"Do you need Robolectric?"* → Only if the code under test touches Android framework classes. Keep ViewModel logic framework-free so it does not.

---

### 3. How would you test a timeout scenario where the server processes a request but the response never reaches the app?

**What the interviewer is really asking**
Do you understand that a client-side timeout creates an **"unknown" outcome** — not a failure — and can you test that the app handles it correctly (does not double-charge, does not silently swallow, does not leave the UI in a lying state)?

**Key concepts**
- `withTimeout` / `withTimeoutOrNull` and `TimeoutCancellationException`
- OkHttp `callTimeout`, `connectTimeout`, `readTimeout`, `writeTimeout` — four distinct timeouts
- The tri-state outcome: **success / failure / unknown** (Section 1, Q1)
- `CancellationException` vs `SocketTimeoutException` vs `InterruptedIOException`
- Idempotency key + server-side reconciliation
- MockWebServer `SocketPolicy.NO_RESPONSE` / `DISCONNECT_DURING_RESPONSE_BODY`
- `Dispatcher` with `setBodyDelay` to simulate a slow response
- Fake clocks for the client-side timeout
- `MockWebServer` + `Dispatcher` for deterministic simulation

**Approach**

1. **Recognise the semantic problem first.** A client timeout means: *"I sent the request; I did not hear back."* It does **not** mean the server did not process it. The request may have:
   - Never arrived (network down).
   - Arrived and been processed successfully.
   - Arrived and been rejected.
   - Be in-flight and complete later.
   Therefore the client outcome is **UNKNOWN**, and the correct handling is *reconcile*, not *retry blindly*.

2. **Test the three layers separately.**

   **Layer A — the transport timeout.** Simulate a server that never responds:
   ```kotlin
   @Test
   fun `times out when the server never responds`() = runTest {
       val server = MockWebServer()
       server.dispatcher = object : Dispatcher() {
           override fun dispatch(request: RecordedRequest): MockResponse =
               MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)
       }
       val api = PaymentApi(baseUrl = server.url("/").toString(), client = client)

       val result = api.charge(amount = 1000, idempotencyKey = "key-1")

       assertTrue(result is ApiResult.Timeout)   // not a hard failure
   }
   ```
   `SocketPolicy.NO_RESPONSE` keeps the connection open without replying — exactly the "server processed it but the response is lost" simulation at the socket level.

   **Layer B — the client-side timeout.** Use `withTimeout` around the call so the test does not depend on real socket timeouts:
   ```kotlin
   @Test
   fun `client timeout yields Unknown, not Failed`() = runTest {
       val result = withTimeoutOrNull(5_000) { api.charge(1000, "key-1") }
       assertNull(result)                                   // the timeout fired
       assertEquals(PaymentState.UNCERTAIN, repo.stateFor("key-1"))
   }
   ```
   With `runTest`'s virtual time, a `withTimeout(5_000)` inside a `delay`-based fake completes instantly. Note `withTimeoutOrNull` returns `null` on timeout; `withTimeout` throws `TimeoutCancellationException` (a `CancellationException`).

   **Layer C — the recovery/reconciliation.** After the unknown outcome, the app must be able to determine the truth:
   ```kotlin
   @Test
   fun `reconciles an uncertain payment and finds it succeeded`() = runTest {
       // The first attempt timed out.
       server.dispatcher = neverResponds()
       assertEquals(PaymentState.UNCERTAIN, payments.charge(1000, "key-1"))

       // The server actually processed it.
       server.dispatcher = chargeSucceededDispatcher()
       val reconciled = payments.reconcile("key-1")

       assertEquals(PaymentState.SUCCEEDED, reconciled)
       assertEquals(1, server.requestCount)   // the same idempotency key → no double charge
   }
   ```

3. **Assert the invariants that matter.**
   - The idempotency key is **the same** across the original attempt and the retry/reconcile (so the server deduplicates).
   - The local record transitions `PENDING → UNCERTAIN`, not `PENDING → FAILED`.
   - The user is shown an honest message ("We're confirming your payment") rather than "Payment failed".
   - No duplicate charge: with the same idempotency key, the server returns the original result.

4. **Distinguish the timeout types in code, and handle them correctly.**
   ```kotlin
   suspend fun charge(amount: Long, key: String): PaymentState = try {
       withTimeout(30_000) { api.charge(amount, key).toState() }
   } catch (e: TimeoutCancellationException) {
       PaymentState.UNCERTAIN        // the request may have been processed
   } catch (e: SocketTimeoutException) {
       PaymentState.UNCERTAIN        // same reasoning — the server may have received it
   } catch (e: CancellationException) {
       throw e                        // our own cancellation → propagate, do not convert
   } catch (e: IOException) {
       PaymentState.FAILED            // a connection that never opened → safe to fail
   }
   ```
   The distinction: an `IOException` on **connect** (the connection was never established) is safe to call a failure; a timeout **after the request was sent** is unknown. OkHttp surfaces both as `IOException` subtypes, so you need to inspect the cause (`e is SocketTimeoutException`).

5. **Use OkHttp's distinct timeouts deliberately.** `callTimeout` caps the whole call; `connectTimeout` caps the connection; `readTimeout` caps the wait for data. For a payment you typically want a generous `callTimeout` and a `connectTimeout` short enough to fail fast on a dead network. Setting them all to the same value hides which phase failed.

6. **Use a fake server that responds *later*, not never.** To test the "the server processed it and the response arrived late" case, use `MockResponse().setBodyDelay(...)` — the response arrives after the client has already timed out, proving the client-side timeout is not a server-side failure.

7. **Test the retry path is idempotent.** Run the same test twice against a stateful fake server that records every request body, then assert that both requests carried the same idempotency key and that the fake charged once.

8. **Assert on the user-visible state too.** The UI must show "Confirming…" (a pending/uncertain state) with an appropriate timeout for the UI, and offer a manual "Check status" action after the reconciliation window. Test that the pending state is not rendered as an error.

9. **Add a fault-injection test in the shared test suite** so every network call site is exercised against "no response", "slow response", "garbage body", and "500" — a reusable `MockWebServer` rule.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Client timeout → `UNCERTAIN` | Honest; enables reconciliation | More states in the UI; needs a resolution path |
| Client timeout → `FAILED` | Simple | Can double-charge if the server did process it |
| `withTimeout` around the call | Testable with virtual time; precise | Cancels the coroutine; the socket call is abandoned |
| OkHttp `callTimeout` | Realistic; covers all phases | Harder to test; needs real elapsed time in tests |
| `MockWebServer` `NO_RESPONSE` | Deterministic socket-level simulation | Does not model a server that processes then fails to reply |
| Fake in-memory server | Full control of state and ordering | More code; does not exercise the real HTTP stack |
| Retry on timeout | Might eventually succeed | Only safe with an idempotency key + server-side dedupe |
| Poll a status endpoint | Definitive answer | Extra endpoint; extra latency; extra load |
| Show "confirming" UI | Honest and calm | Requires a reconciliation flow and a fallback |

**Key detail:** A client timeout is **not** a failure — it is an *unknown*. The whole test exists to prove that the app treats it that way: it keeps the same idempotency key, marks the record `UNCERTAIN`, and reconciles rather than blindly retrying. Asserting `FAILED` is the bug this test catches.

**Code example**

```kotlin
@Test
fun `server processed the charge but the response never arrives`() = runTest {
    val server = MockWebServer()
    server.dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest) =
            MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE)   // never reply
    }
    server.start()

    val api = PaymentApi(baseUrl = server.url("/").toString())
    val payments = PaymentRepository(api, PaymentDao.inMemory())

    // 1. The client times out — the outcome is UNKNOWN, not failed.
    val outcome = payments.charge(amount = 1000, idempotencyKey = "idem-1")
    assertEquals(PaymentState.UNCERTAIN, outcome)
    assertEquals(PaymentState.UNCERTAIN, payments.stateFor("idem-1"))

    // 2. The server actually did process it. Reconciliation finds the truth.
    server.dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val charge = request.path!!.substringAfter("charge/")
            return MockResponse().setResponseCode(200)
                .setBody("""{"id":"$charge","status":"SUCCEEDED"}""")
        }
    }
    val reconciled = payments.reconcile("idem-1")
    assertEquals(PaymentState.SUCCEEDED, reconciled)

    // 3. The same idempotency key was reused → the server charged only once.
    assertEquals("idem-1", server.takeRequest().headers["Idempotency-Key"])
}
```

**Talking points / follow-ups**
- *"How do you simulate a server that never responds?"* → `MockWebServer` with `SocketPolicy.NO_RESPONSE`, or a `Dispatcher` returning a response with a very long `bodyDelay`.
- *"What is the difference between a connect timeout and a read timeout?"* → Connect timeout: the TCP/TLS connection was never established — safe to treat as a failure. Read timeout: the connection was established and the request was sent — the outcome is unknown.
- *"Why not just retry?"* → Because the first attempt may have succeeded. Retry only with the same idempotency key, so the server deduplicates.
- *"How do you test this against a real server?"* → A staging environment with a fault-injection proxy that can drop the response after processing (e.g. Toxiproxy).
- *"What does the user see?"* → A "confirming" state, not an error. And after the reconciliation deadline, a manual "check status" action.

---

### 4. A test passes individually but fails when the full test suite runs. How would you investigate shared state, dispatchers, and test isolation?

**What the interviewer is really asking**
Do you know how to **systematically debug order-dependent test failures** — the discipline of bisecting the suite, hunting global state, and recognising that parallelism is the amplifier, not the cause?

**Key concepts**
- Order dependence and test isolation
- Global/static state: `object` singletons, `companion object` mutable fields, `static` Java fields, a shared `Context`, a shared in-memory DB
- Leaked coroutines from a previous test still running and mutating shared state
- `Dispatchers.setMain` not reset → the second test uses a dispatcher whose scheduler is torn down
- `runTest` with the default 60s timeout reporting "unfinished coroutines"
- Test parallelism: Gradle `maxParallelForks`, `forkEvery`, JUnit5 parallel execution
- Randomised test order (`junit-pioneer` `@SetSystemProperty`, `--random`); Gradle `--tests` bisection
- Time: one test sets a default timezone/locale and doesn't restore it
- The database / file system / SharedPreferences shared between tests
- LeakCanary / heap dumps in tests; `@After` teardown
- `kotlinx-coroutines-test`'s "leaked coroutine" detection and `UncompletedCoroutinesError`

**Approach**

1. **Confirm it is order dependence and find the minimal failing pair.** Run the suite and capture the order. Then bisect:
   ```bash
   ./gradlew test --tests "com.example.FooTest" --tests "com.example.BarTest"
   ```
   Find the smallest set of test classes that reproduces the failure. This is the fastest way to shrink a 4-hour problem to a 5-minute one.

2. **Check the obvious shared state, in this order:**
   - **DI container / singletons** — a Hilt or Koin graph created once for the whole run, or a `companion object { lateinit var INSTANCE }`. Tests that inject the singleton see the previous test's state.
   - **In-memory database** — a `Room.inMemoryDatabaseBuilder` created in a `companion object` is shared; if it is not cleared between tests, rows leak across.
   - **`Dispatchers.setMain` / `resetMain`** — a `TestWatcher` rule that sets Main but the reset is skipped (an exception in the test body, or the rule not applied).
   - **The test clock / timezone / locale** — `TimeZone.setDefault`, `Locale.setDefault`, `System.setProperty("user.timezone", ...)` without a reset.
   - **File system** — tests writing to the same path in the app's cache directory.
   - **Static caches** — an image cache, a "has the user seen the onboarding" flag, a rate-limiter's timestamp map.

3. **Look for leaked coroutines.** This is the most common and least obvious cause. A coroutine launched in test A that is never cancelled keeps running into test B and mutates shared state (or the database). `runTest` reports this at the end of the test:
   ```
   Unfinished coroutines during teardown. This is likely a leak.
   ```
   But that only fires for coroutines in the `runTest` scope — a `viewModelScope` coroutine is **not** a child of the `runTest` scope and will not be reported. Hence: inject `backgroundScope` or cancel the ViewModel in `@After`.

4. **Distinguish "leaked coroutine" from "shared state" empirically.** Add a `Thread.sleep(50)` (or better, a barrier) at the start of the failing test. If the failure disappears with a delay, it is a **timing/leak** problem. If it persists, it is **shared state**.

5. **Check whether the failure is a real assertion or a timeout.** Many "suite failures" are actually `UncompletedCoroutinesError` (a `runTest` timeout) caused by a leaked dispatcher or a never-completed `CompletableDeferred` — not a logic assertion. Read the actual stack trace; do not assume.

6. **Audit the parallelism settings.** Gradle's `maxParallelForks` runs test *classes* in separate JVMs (isolated) but tests *within* a class share a JVM. JUnit5's parallel execution runs tests in the same JVM concurrently — which exposes shared-state bugs that serial execution hides. So:
   - If a test fails only in parallel, it is **almost certainly** shared state or a leaked coroutine. Fix the test, do not disable parallelism.
   - `forkEvery` can isolate further but slows CI.

7. **Adopt a discipline that prevents recurrence:**
   - **No mutable global state in production code.** Everything shared is injected.
   - **A fresh DI graph per test** — Hilt's `@TestInstallIn` replaces production modules for the whole test run; for a plain container, construct it in `@Before`.
   - **A `TestDispatcherRule` that always resets** — implement `TestWatcher` so the reset happens in `finished()` even if the test throws.
   - **Clear the database in `@After`** or use a fresh in-memory DB per test.
   - **Restore global state** in `@After` (timezone, locale, `Dispatchers.resetMain()`).
   - **Never use `GlobalScope`** in production code — it makes leaks untestable.

8. **Add tooling.**
   - Run the suite with a **randomised order** in CI (a nightly job) to catch order dependence early.
   - Use **JUnit5's `@ResourceLock`** to serialise tests that genuinely share a resource — a targeted fix rather than turning off parallelism globally.
   - Run LeakCanary in instrumentation tests to catch retained contexts.

9. **Introduce a "flaky test" quarantine list** in CI so a known-bad test does not block, but is tracked.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Bisecting the suite | Shrinks the problem fast | Manual effort; needs a deterministic reproduction command |
| Randomised order in CI | Finds order dependence automatically | Flaky CI by nature; run nightly, not on every PR |
| Fresh DI graph per test | True isolation | Slower tests; more setup boilerplate |
| Injecting state instead of using singletons | Removes the class of bug | Requires production-code refactoring |
| `backgroundScope` in tests | Automatic coroutine cleanup | Requires injecting the scope |
| Disabling parallelism | Makes CI green fast | Hides real bugs; the app has the same race in production |
| `@ResourceLock` | Targeted serialisation | Reduces concurrency only for the locked tests |
| Quarantining flaky tests | Unblocks the pipeline | The feature is unprotected meanwhile |

**Key detail:** The signal that distinguishes the two main causes is a **short delay at the start of the failing test**. If adding a delay makes it pass, it is a leak or a race (timing). If it still fails, it is **shared state** (deterministic contamination).

**Code example**

```kotlin
// A rule that ALWAYS restores the main dispatcher, even if the test throws.
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

// A fresh graph and a fresh database per test — no state survives.
class TestGraph @AfterEach
fun resetGraph() {
    container.reset()                    // clears singletons
    db.clearAllTables()                  // Room in-memory DB
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))   // restored
    Locale.setDefault(Locale.US)
}

@get:Rule val mainRule = MainDispatcherRule()

@Test
fun `reads cached value`() = runTest {
    val repo = Repository(cache = FakeCache())   // injected, not a global
    repo.put("k", "v")
    assertEquals("v", repo.get("k"))
}

@Test
fun `does not see the previous test state`() = runTest {
    val repo = Repository(cache = FakeCache())   // a NEW fake, not the global one
    assertNull(repo.get("k"))
}
```

**Talking points / follow-ups**
- *"How do you find the culprit test quickly?"* → Bisect with `--tests` filters, then randomise the order to confirm.
- *"What is the difference between a leak and shared state?"* → A leak is *timing*-dependent (a coroutine still running); shared state is *deterministic* contamination that a delay will not fix.
- *"Why not disable test parallelism?"* → It hides genuine concurrency bugs that will bite in production.
- *"How do you prevent this in the long run?"* → No mutable global state in production code; a fresh DI graph and database per test; a `TestWatcher` that always restores global state.
- *"What does `UncompletedCoroutinesError` mean?"* → `runTest` timed out because a coroutine in its scope never completed — often a `CompletableDeferred` nobody completed, or a `withTimeout` that was expected.

---

### 5. A production crash occurs after a release, but rollback is not immediately available. How would you mitigate the impact?

**What the interviewer is really asking**
Do you understand the difference between **mitigation** (reduce impact *now*) and **remediation** (fix and ship), and can you use the platform's kill switches — feature flags, remote config, forced-stop of the crashing path — to stop the bleeding while a proper fix is built?

**Key concepts**
- Feature flags / remote config: **kill switch**, staged rollout, percentage rollout, **dynamic** vs build-time flags
- The Google Play "halt rollout" / "revert to previous version" mechanism in the Play Console
- Firebase Remote Config (real-time) — but note it is not truly real-time; it is fetched on app start and periodically (a `min_fetch_interval`), so a kill switch has a latency
- `Play Integrity` / `Firebase App Distribution` — not relevant for kill switches
- Crash-free users / crash-free sessions, ANR rate
- Crashlytics/Crash reporting: **symbolicated** stack traces, `mapping.txt`, ProGuard/R8
- `rollout` / `rolloutPercentage` in Google Play
- Emergency hotfix via the Play "hotfix" (Play Instant / dynamic code loading) — generally not allowed for arbitrary code
- **Server-side kill switch** — the most reliable: the server stops returning the offending feature
- `Targeted rollout` by device model / OS version / app version
- ANR vs crash; the crash-rate alarm

**Approach**

1. **Classify the crash before acting.** Crash vs ANR vs OOM vs native (NDK) crash vs `TransactionTooLargeException`. The mitigation differs: a crash in a new feature → kill switch; a crash on a specific OEM → targeted rollout exclusion; a startup crash on all users → **hotfix via Play patch if allowed**, otherwise roll back.

2. **Stop the bleeding with the fastest available lever, in this order:**
   - **Server-side kill switch** — the server stops sending the offending payload or returns a feature-disabled response. This takes effect on the next request, and requires **no app release**. This is the most valuable kill switch to design for in advance. Example: a `featureFlags` block in the API response that the client honours.
   - **Remote config kill switch** — `Firebase Remote Config` with a boolean `new_checkout_enabled = false`. The client reads it at start and periodically. **Latency:** the config is fetched at app start and on a timer (default `min_fetch_interval` 12h, but you can force a fetch on the app's foreground transition). To make it responsive, call `remoteConfig.fetchAndActivate()` when the app comes to the foreground. Even so, it is not instantaneous for users with the app open.
   - **Play Console "halt rollout"** — stops new users getting the bad version. Does **not** help users who already have it.
   - **Play Console "revert to previous version"** — a *rollout* revert, not an app rollback: users who already installed the bad version keep it. This is a common misconception — the Play Console's revert only affects the rollout of the version, not already-installed apps. To actually remove a bad version from devices you need a **version code higher than the bad one** that is published, and users update.
   - **A remote "kill" of the offending code path** in the running app via the server (see above) — the fastest and most surgical.

3. **Design kill switches to be server-driven and fail-safe.** The critical detail: the client must honour the kill switch **without needing to know about it in advance**. That means the *server* decides, and the client reads a generic "feature enabled" flag as part of its normal data flow — not as new app logic. A kill switch that requires a new app version to honour is not a kill switch.

4. **Scope the rollout instead of stopping it.** If the crash only affects a subset (an OEM, an OS version, a screen size), use **targeted rollout** or a client-side guard:
   ```kotlin
   if (!isAffectedDevice(Build.MODEL, Build.VERSION.SDK_INT)) { /* new path */ } else { /* old path */ }
   ```
   Target by OS version, OEM, device model, app version, or server-provided cohort.

5. **Understand why rollback is unavailable and work within it.** Common reasons: Play requires a **higher version code** than the bad one; the bad version has already been installed by users; the release was **staged over days**; a data migration is **not backward-compatible** (the bad version wrote data the old version cannot read — this is the real reason a rollback is often blocked). If it is a data-compatibility issue, the fix is a **forward-compatible hotfix**, not a rollback. Design migrations to be additive so forward fixes are always possible.

6. **Ship a hotfix.** A new build with a higher version code containing only the fix. To make it fast:
   - **Internal app distribution / Firebase App Distribution** for a quick test on affected devices.
   - **Play staged rollout** to 1% → 5% → 20% → 100%, watching the crash-free rate at each step.
   - Keep the hotfix branch **minimal** — cherry-pick only the fix, no unrelated changes.

7. **Instrument to detect and to verify the fix.** The metrics that matter:
   - **Crash-free users** and **crash-free sessions** (per version, per device cohort).
   - Crash **version** and **build** attached to every report — you cannot act on a crash you cannot attribute to a version.
   - **Symbolication**: upload the R8 `mapping.txt` to the crash backend, or the stack traces are useless.
   - An **automated alert** on a crash-rate spike, ideally per version, so the response starts before a human notices.

8. **Communicate.** Publish a status note; if the crash affects a user-visible flow, tell support. Transparency buys credibility and speeds up the postmortem.

9. **Run the blameless postmortem and prevent recurrence.** Capture: the timeline, the root cause, why the tests did not catch it, and the **action items with owners**. Then harden the release process: expand the staged rollout, add a crash-rate gate that blocks promotion, add a **canary** step.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Server-side kill switch | Fastest; no app release; surgical | Must be designed in advance; adds a round trip |
| Remote config kill switch | No release needed | Fetch latency (not truly instant); needs a foreground fetch |
| Play "halt rollout" | Stops new installs | Does not help existing users |
| Play "revert" | Stops the rollout | **Not** an app rollback — installed users keep the bad build |
| Hotfix release | Actually fixes it | Takes hours to a day; needs review and testing |
| Targeted rollout by cohort | Limits the blast radius | Requires device-cohort detection; more rollout permutations |
| Forward-compatible data design | Always allows a hotfix | More migration work up front |

**Key detail:** **Play Console "revert to previous version" does not roll back already-installed apps** — it only stops/rolls back the *rollout* of the version. Users who already have the bad version keep it until a newer version (with a higher version code) is published. This is exactly why "rollback is not immediately available", and it is the moment to explain that the real mitigation levers are a **server-side kill switch** and a **hotfix**.

**Code example**

```kotlin
// The client reads a generic feature flag delivered with normal data —
// so a kill switch works without shipping a new app version.
data class FeatureFlags(
    val newCheckoutEnabled: Boolean = true,
    val maxUploadMb: Int = 50,
)

class RemoteConfigSource(private val config: FirebaseRemoteConfig) {
    fun flags(): FeatureFlags {
        // Fetch on foreground so the kill switch takes effect within seconds.
        runCatching { config.fetchAndActivate() }
        return FeatureFlags(
            newCheckoutEnabled = config.getBoolean("new_checkout_enabled"),
            maxUploadMb = config.getInt("max_upload_mb"),
        )
    }
}

class CheckoutViewModel(
    private val flags: FeatureFlags,
    private val repo: CheckoutRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UiState>(UiState.Idle)

    fun submit(amount: Long) {
        viewModelScope.launch {
            _state.value = UiState.Loading
            // The guard is data-driven: flipping the flag on the server disables the path.
            if (!flags.newCheckoutEnabled) {
                _state.value = UiState.Error("Checkout is temporarily unavailable.")
                return@launch
            }
            _state.value = repo.submit(amount)
        }
    }
}
```

**Talking points / follow-ups**
- *"How fast can you kill a bad feature?"* → Seconds, if the kill switch is server-driven and the app reads it on every foreground/navigation. Minutes if it is remote config with a foreground fetch. Days if it requires a release.
- *"What if the crash is in app startup?"* → The app cannot read a remote config; you need a server-side rollout gate (the server returns a "minimum supported version" and the app shows a blocking update screen) or a Play hotfix.
- *"How do you know the crash rate is bad?"* → Crash-free users per version, with an alert threshold; compare against the previous version's baseline.
- *"Why not just roll back?"* → Because Play cannot uninstall an app from a device; a "revert" only affects the rollout. And often the bad version wrote data that the previous version cannot read, which blocks a downgrade — the reason rollback is unavailable in the first place.
- *"How do you prevent this happening again?"* → Staged rollout with automated crash-rate gates, a symbolicated crash pipeline with version attribution, a rehearsed kill-switch path, and forward-compatible migrations.

---

### 6. A build works locally but fails in CI. How would you investigate dependency resolution, build configuration, and environment differences?

**What the interviewer is really asking**
Do you know how to make a build **hermetic and reproducible**, and can you systematically diff the local environment against CI?

**Key concepts**
- Gradle dependency resolution, `repositories`, `dependencyResolutionManagement`, `repositoriesMode`
- Version catalogs (`gradle/libs.versions.toml`) — single source of truth for versions
- Dependency locking (`dependencyLocking` / `--write-locks`) — pins transitive versions
- Dynamic versions (`1.+`, `latest.release`) — **non-reproducible**; a CI build may resolve a different version than a local one
- `SNAPSHOT` dependencies — change without a commit
- Repositories order and `content` filtering
- Gradle caches: `~/.gradle/caches`, `--refresh-dependencies`, `GRADLE_USER_HOME`
- `gradle.properties`, `gradle.properties` in CI, `ORG_GRADLE_PROJECT_*`
- The **Gradle daemon** and its JVM args (`org.gradle.jvmargs`), and **different JDK versions** (toolchains)
- JDK toolchain resolution — `jvmToolchain(17)`; the local machine may have 17, CI 21
- `local.properties` — `sdk.dir` is machine-specific and must be gitignored
- `ANDROID_HOME` / `ANDROID_SDK_ROOT` / the SDK components installed in CI
- The **Android Gradle Plugin** requiring a matching Gradle version and JDK
- Network/proxy restrictions in CI (no access to `dl.google.com` → missing artifacts)
- `lint`, `detekt`, `ktlint` running in CI but not locally
- `R8`/`minifyEnabled` differences between the build types
- `BuildConfig` fields injected per environment (flavours)
- `--offline`, `--no-daemon`, `--stacktrace`, `--info`, `--scan`
- `dependencyInsight` — why a specific version was selected
- Reproducibility: the Gradle **dependency verification** (`gradle/verification-metadata.xml`)

**Approach**

1. **Reproduce the failure locally first.** If you cannot reproduce it locally, you have a *difference*, not a bug in your code. So the first step is to make CI's environment reproducible locally:
   ```bash
   ./gradlew build --stacktrace
   ./gradlew build --no-daemon -Dorg.gradle.java.home=/path/to/ci/jdk
   ```
   Or run the CI job locally with the same container image.

2. **Get the real error.** "CI fails" is not a diagnosis. Read the actual failure. Common categories:
   - **Dependency resolution failure** — "Could not find X" / "Could not resolve X".
   - **Compilation failure** — a Kotlin/Java compile error that does not happen locally.
   - **Test failure** — a test that fails only in CI.
   - **Lint/detekt failure** — a static-analysis gate.
   - **Resource/config failure** — a missing `google-services.json`, a missing signing config.
   - **OOM / timeout** — the CI machine is smaller.

3. **For dependency resolution specifically:**
   ```bash
   ./gradlew :app:dependencies --configuration releaseRuntimeClasspath
   ./gradlew :app:dependencyInsight --dependency okhttp --configuration releaseRuntimeClasspath
   ```
   `dependencyInsight` shows **why** a particular version was selected — usually a conflict resolution to the highest version. If CI picks a different version, look for:
   - **Dynamic versions** (`1.2.+`, `latest.release`) — the single biggest cause of "works locally, fails in CI".
   - **`SNAPSHOT` dependencies** — a moving target.
   - **Different repository order** — `google()` before `mavenCentral()` vs after, and which artifact wins.
   - **A transitive dependency published after your local build** — your local cache holds the old version.
   - **Dependency constraints / `strictly`** in a version catalog that resolves differently.

   The fix is to **eliminate dynamic versions** and **enable dependency locking**:
   ```kotlin
   // gradle/libs.versions.toml
   [versions]
   okhttp = "4.12.0"          # never "4.+"
   ```
   ```kotlin
   // settings.gradle.kts
   dependencyResolutionManagement {
       repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
       repositories { google(); mavenCentral() }
   }
   dependencyLocking { lockAllConfigurations() }
   ```
   Then `./gradlew dependencies --write-locks` commits `gradle.lockfile`.

4. **Diff the environments systematically.** Compare:
   - **JDK version** — `java -version` locally vs `java -version` in CI. AGP 8.x requires JDK 17+; a JDK 21 vs 17 difference changes bytecode targets and can break some plugins.
   - **Gradle version** — the wrapper (`gradle/wrapper/gradle-wrapper.properties`) should pin it. If a developer has a system Gradle, they may not use the wrapper.
   - **Android SDK components** — CI may lack `build-tools;34.0.0` or an NDK version. Use `sdkmanager` in the CI image, and check `ANDROID_HOME`.
   - **Environment variables** — `~/.gradle/gradle.properties` locally (proxies, `ORG_GRADLE_PROJECT_*`) vs CI secrets. **`gradle.properties` is a classic source of difference**: a local `org.gradle.jvmargs=-Xmx4g` vs CI's `-Xmx2g` explains a CI-only OOM.
   - **Build variants/flavours** — CI may build `release` (minified) while you built `debug`. R8 can expose issues (missing ProGuard rules, unresolved classes) that never appear in debug.
   - **Signing / config files** — `google-services.json`, `keystore.properties` — present locally, supplied as CI secrets.

5. **Clean and rebuild deliberately.** Distinguish a stale-cache problem from a real one:
   ```bash
   ./gradlew clean
   ./gradlew --refresh-dependencies build     # re-resolve everything
   rm -rf ~/.gradle/caches/modules-2         // nuke the dependency cache
   ```
   Do this on the CI machine, or in a throwaway container, to confirm.

6. **Use `--stacktrace` and `--info` and capture the CI logs.** Turn on verbose output in the CI job:
   ```yaml
   - run: ./gradlew build --stacktrace --info
   ```
   Upload the Gradle daemon logs and the build scan. A **Gradle build scan** (develocity) is the fastest way to compare "the same task failed locally but succeeded here" because it diffs the inputs.

7. **Eliminate environment drift at the source.** The durable fixes:
   - **Pin the JDK** via `java.toolchain` so every machine and CI uses the same one:
     ```kotlin
     kotlin { jvmToolchain(17) }
     ```
   - **Pin Gradle via the wrapper** and always use `./gradlew`.
   - **Pin dependency versions** in the version catalog; no dynamic versions.
   - **Enable dependency verification** (`gradle/verification-metadata.xml`) so a tampered or unexpectedly published artifact fails the build.
   - **Pin the Android SDK components** in CI (`sdkmanager "platforms;android-35" "build-tools;35.0.0"`) rather than installing "latest".
   - **Make the CI image explicit** — a versioned Dockerfile or a pinned runner image.

8. **Verify the CI is actually running what you think.** Check the checkout ref, the branch, the merge base, the submodule state, the Gradle cache key, and whether a stale artifact or a cached build output is being reused (`--build-cache` with a wrong cache key can serve a bad build).

9. **Add a "build works from clean" check** to CI: a job that clones fresh and builds with `--no-build-cache` on a schedule. It catches the class of failures where incremental state masks a problem.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Version catalog | One source of truth; easy to bump | Does not pin transitive versions by itself |
| Dependency locking | Fully reproducible resolution | Requires maintenance when a dependency must be updated |
| Dependency verification | Detects tampered/unexpected artifacts | Extra setup; needs updating for every intentional bump |
| `jvmToolchain` | Same JDK everywhere | Requires the JDK to be downloadable/installable |
| Dockerised CI image | Reproducible environment | Maintenance burden; slower cold starts |
| Caching in CI | Much faster builds | A mis-keyed cache can serve a stale/bad build |
| Cleaning everything | Rules out stale state | Slow — use as a diagnostic, not a habit |
| Pinning SDK components | Reproducible toolchain | Requires updating the CI image per Android version |

**Key detail:** The most common root cause of "works locally, fails in CI" is a **dynamic dependency version** (`1.+`) resolving to a different version on the CI machine, often only in the `release` variant. The fix — pin versions in the catalog and enable dependency locking — eliminates the entire class.

**Code example**

```kotlin
// settings.gradle.kts — a deterministic resolution environment.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}
dependencyLocking { lockAllConfigurations() }

// build.gradle.kts — pin the JDK so local and CI are identical.
kotlin { jvmToolchain(17) }
```

```yaml
# .github/workflows/ci.yml — pin the toolchain explicitly.
- uses: actions/setup-java@v4
  with: { distribution: temurin, java-version: '17' }
- uses: android-actions/setup-android@v3
- run: sdkmanager "platforms;android-35" "build-tools;35.0.0"
- run: ./gradlew build --stacktrace --info
- run: ./gradlew dependencies --write-locks   # refresh the lockfile when deps change
```

**Talking points / follow-ups**
- *"How do you find why a dependency version was chosen?"* → `./gradlew :app:dependencyInsight --dependency <name>` — it shows the requested and selected versions and the conflict resolution.
- *"Why is CI-only failure so common with `release` builds?"* → R8 minification, resource shrinking, and `BuildConfig` differences only apply to release. Debug builds hide them.
- *"What is dependency verification?"* → A Gradle feature that records checksums of all artifacts and fails the build if an artifact's checksum differs — it protects against tampered or unexpectedly re-published dependencies.
- *"How do you make CI reproducible?"* → Pin the JDK (toolchain), the Gradle version (wrapper), the SDK components, and the dependency versions (catalog + locking + verification). Use a versioned CI image.
- *"What do you do about a stale Gradle cache?"* → `--refresh-dependencies` or delete `~/.gradle/caches/modules-2`; verify the CI cache key includes the wrapper version, JDK, and lockfile hash.

---

### 7. How would you introduce contract tests between an Android app and a backend team?

**What the interviewer is really asking**
Can you create a **shared, enforceable artifact** between two teams — a consumer-driven contract — so that a backend change that breaks the app is caught *before* release, not in production?

**Key concepts**
- **Consumer-driven contracts**: the consumer defines the expectation; the provider verifies it
- **PACT**: the consumer writes a pact (a JSON contract) describing the requests it makes and the responses it expects; the provider runs a **pact verification** task against a stubbed provider to prove it satisfies the contract
- **Pact-JVM** (`au.com.dius.pact.consumer` / `provider`) for JVM/Android; `pact-jvm` supports Kotlin
- **Schemathesis** (Python/pytest) and **Dredd** (OpenAPI → HTTP tests) — provider-side contract testing driven by the **OpenAPI spec**
- **OpenAPI/AsyncAPI** as a shared schema
- **Consumer-driven vs provider-driven** contracts
- **Schema registry** (Avro/Protobuf) with **backward compatibility rules** (`BACKWARD`, `FULL`) — `Confluent Schema Registry` compatibility checks
- **Protobuf** field numbers and the "never reuse a field number" rule
- **Nullability / optionality** rules: adding a field is usually backward compatible; making an optional field required is not
- Where it runs: **PR gates** on both sides
- JSON Schema / OpenAPI request/response validation in the app's own integration tests
- Mock servers generated from the contract (`pact-stub-server`, WireMock) so the app can develop against a stub

**Approach**

1. **Start with the contract artefact.** Agree on **OpenAPI** (for REST) or **Protobuf/AsyncAPI** as the source of truth, versioned in a shared repository. If the backend already publishes an OpenAPI spec, use it; the app's DTOs can be **generated** from it (OpenAPI Generator), which eliminates a whole class of drift.

2. **Layer the two complementary techniques:**
   - **Provider-side contract testing driven by the schema**: run the provider's implementation against every example and edge case derived from the OpenAPI spec (Schemathesis, Dredd, or a `spring-cloud-contract` test). This catches "the response does not match the spec".
   - **Consumer-driven contracts (PACT)** for the **critical** flows: the app declares the exact request it will make and the response it relies on; the backend verifies it in its own CI. This catches "the backend changed a field the app depends on" — the more dangerous direction.

3. **Define the Pact for the critical flows only.** Contracts for every endpoint is unmaintainable. Prioritise: authentication, payments, transaction list, sync/delta, error shapes. The app writes a Pact like:
   ```kotlin
   @Rule
   val pact = PactConsumerRule(
       "transactions-consumer",
       "transactions-provider",
       PactDslWithProvider(
           given("there are transactions for the account",
               mapOf("accountId" to "acc-1"),
               TransactionPactDsl.responseForGet("/accounts/acc-1/transactions")),
       ),
   )
   ```
   The app's test asserts the client behaves as expected against the mock provider; the Pact JSON is published to a broker (Pactflow, or a shared S3 bucket in a shared repo).

4. **Make the provider verify.** The backend's CI runs a **pact verification** job: it replays the Pact files against the provider's real implementation and fails if any expectation is broken. This is the key move — the contract is enforced on the *provider's* pipeline, which is where the breaking change is introduced.
   ```bash
   ./gradlew pactVerify --pactBrokerUrl=$PACT_BROKER_URL
   ```
   The provider also sets **pending** status for new pacts so a consumer can publish a pact before the provider implements it, with a grace period before it breaks the build.

5. **Handle versioning explicitly.** Adopt **backward compatibility** as the rule: the provider must not break existing consumers. Practically:
   - **Additive changes only** — add optional fields; never remove or rename; never change a field's type.
   - **Ignore unknown fields** on the client (so the server can add fields freely) — configure the JSON deserialiser to ignore unknown keys, and never use strict-mode parsing that throws.
   - **Deprecation with a window** — announce, monitor usage by consumer version, then remove.
   - For **Protobuf**, never reuse a field number and never change a field's wire type; use `reserved`.

6. **Version the contract and communicate.** A shared CHANGELOG in the contract repo, published to Slack, plus a deprecation calendar. The staff-level move is to make the contract a **first-class artefact with an owner and a review path**, not a wiki page.

7. **Make the app resilient to contract drift anyway.** Contracts catch changes you test for; they do not catch changes you do not know about. So:
   - Parse defensively: unknown fields ignored, missing optional fields defaulted, **enums handled with an `UNKNOWN` fallback** so a new server enum value does not crash the app.
   - Include the **API version in the request headers** so the server can serve both versions during a migration.

8. **Run the contracts against a real-ish environment too.** Contract tests are not enough on their own: add a small suite of **integration tests against a staging environment** (using the same specs) to catch environment-specific issues (a misconfigured gateway, a proxy that strips a header).

9. **Measure and report.** Track the number of contracts, which flows are covered, and — importantly — **the number of production incidents caused by contract violations**. That number is the business case for the whole programme.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Pact (consumer-driven) | Catches the dangerous direction: provider breaking a consumer; the consumer defines the truth | Requires both teams to run Pact; broker infrastructure; pact files to maintain |
| OpenAPI schema testing (provider-driven) | One spec, many generated cases; easy for the backend team | Catches schema violations, not client expectations; misses semantic changes |
| OpenAPI spec only | Simple; tooling everywhere | Weak enforcement; a spec can drift from reality without failing anything |
| Protobuf + schema registry | Strong typing; explicit compatibility rules | Requires codegen on both sides; less readable for humans |
| Generated client from the spec | Eliminates DTO drift entirely | The generator is rigid; some hand-written customisation needed |
| Strict parsing (fail on unknown fields) | Catches server drift early | Breaks the app the moment the server adds a field — brittle |
| Lenient parsing (ignore unknowns) | Resilient to additive server changes | Silently accepts a broken contract; requires defensive code |
| Shared contract repo | Versioned, reviewable, auditable | Cross-repo PR coordination overhead |

**Key detail:** The contract must be **enforced on the provider's CI**. A Pact that only the consumer runs proves the consumer still works against the *old* provider — it does not stop the backend from breaking the app. The pact verification job on the backend's pipeline is what makes this a cross-team mechanism rather than a documentation exercise.

**Code example**

```kotlin
// Consumer side: the app declares what it relies on.
@Rule
val pact = PactConsumerRule(
    "transactions-consumer",
    "transactions-provider",
    PactDslWithProvider(
        given("there are transactions for account acc-1",
            mapOf("accountId" to "acc-1"),
            TransactionPactDsl.responseForGet("/v1/accounts/acc-1/transactions")),
    ),
)

@Test
fun `lists transactions`() {
    val result = TransactionApi(baseUrl = mockServer.url("/").toString())
        .transactions("acc-1", cursor = null)
    assertEquals(2, result.size)
    pact.verify { TransactionPactDsl.requestForGet("/v1/accounts/acc-1/transactions") }
}

// Provider side (backend CI): replay the pact against the real implementation.
// ./gradlew pactVerify --pactBrokerUrl=$PACT_BROKER_URL
// Fails the backend build if the implementation no longer satisfies what the app expects.
```

```kotlin
// Client resilience: tolerate additive server changes.
@Serializable
data class TransactionDto(
    val id: String,
    val amount: Long,
    val currency: String,
    val status: String? = null,               // optional, defaulted
    val category: Category = Category.UNKNOWN, // enum fallback for unknown server values
)

enum class Category { FOOD, TRANSPORT, UNKNOWN }
```

**Talking points / follow-ups**
- *"Consumer-driven or provider-driven?"* → Both, for different purposes: provider-driven (schema) catches "the response violates the spec"; consumer-driven (Pact) catches "the provider broke what the app depends on".
- *"How do you handle a breaking change the app has not released yet?"* → Use **pending pacts** with a grace period, and version the API in the path or header so both versions are served during the migration.
- *"What if the backend team will not adopt Pact?"* → Start with the shared OpenAPI spec + provider-side schema testing, generate the app's DTOs from it, and add strict response validation in the app's integration tests. Adopt Pact only for the two or three highest-risk flows.
- *"How do you prevent enum drift crashing the app?"* → An `UNKNOWN` fallback in the client and a contract test with an unknown value.
- *"How do you know the contracts are worth the effort?"* → Track contract-caught incidents and the reduction in "server broke the app" production issues.

---

### 8. A new release causes a significant increase in app startup time. How would you detect and prevent performance regressions?

**What the interviewer is really asking**
Can you build a **performance regression pipeline** — measure on real devices, gate the build, and attribute the regression to a specific change — rather than eyeballing it after release?

**Key concepts**
- **Cold start**, **warm start**, **hot start** — and what each measures
- **TTID** (time to initial draw) and **TTFD** (time to full draw) — the metric Android's **Startup Performance** tooling reports
- **`androidx.profileinstaller`** — the baseline profile and the ART `Baseline Profile`
- **`macrobenchmark` module** — `StartupTimingMetric`, `frameTimingMetric`, `TraceSectionMetric`; `JankStats`
- **`BaselineProfileRule`** — collects a baseline profile from a Macrobenchmark run
- **`Startup Profiler`** (Android Studio) and **Perfetto** traces
- **`adb shell am start -W`** — measures launch times from the shell
- **`ActivityManager` / `Process.getStartElapsedRealtime()`** and `Application.getStartUptimeMillis()`
- **Systrace / Perfetto** for the main-thread and startup traces
- **Class loading, DEX verification, ContentProvider initialisation** — the classic startup cost centres
- **`android:extractNativeLibs`, `useLegacyPackaging`, App Bundle delivery**
- **`Startup` timing instrumentation**: `Trace.beginSection` / `androidx.tracing`
- **`R8` shrinking** — a smaller APK means less to verify and load
- **Regression gates in CI** — a threshold that fails the build
- **Real devices in the lab** — emulators are useless for startup measurement

**Approach**

1. **Define the metric precisely before measuring.** Startup is ambiguous; pick:
   - **Cold start** — the process is created from scratch. This is the one users feel and the one to gate on.
   - **Warm start** — the process exists, the Activity is recreated.
   - **Hot start** — the Activity is brought back to the foreground.
   For Android, the most useful primary metric is **TTID** (time to the first frame the user can see) and secondarily **TTFD** (when the content is fully rendered). Gate on cold-start TTID p50/p90.

2. **Measure on real devices with Macrobenchmark.** This is the only repeatable, automatable measurement:
   ```kotlin
   @get:Rule
   val benchmarkRule = BaselineProfileRule()

   @Test
    fun startupCold() = benchmarkRule.measureRepeated(
        packageName = TARGET_PACKAGE,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }
   ```
   `Macrobenchmark` runs the app on a **physical device**, kills the process between iterations, and reports the timings. Emulator numbers are meaningless for startup.

3. **Establish a baseline and a regression gate in CI.**
   - Keep a **historical baseline** of the metric per device model.
   - Fail the build if the new build's p90 is **more than X% worse** than the baseline (e.g. 5%).
   - Store the results as a build artifact and comment them on the PR so the reviewer sees "TTID p50 780ms → 910ms (+17%)".
   - The threshold must be **sensitive enough to catch real regressions but loose enough not to be flaky** — a good practice is to measure on a dedicated, quiet device in a dedicated CI runner.

4. **Find the cause with a trace.** When a regression is detected, capture a **Perfetto trace** of the cold start and look at the main thread:
   ```bash
   adb shell am start -W -S -n com.example/.MainActivity   # -S forces a cold start
   ```
   - **`-W`** prints `TotalTime`, `WaitTime`, and `Complete`.
   - Then record a trace (`adb shell perfetto ...` or the Studio profiler) and inspect:
     - **Main-thread blocking** — is the main thread busy with I/O, disk reads, or heavy computation before the first frame?
     - **Class loading / DEX verification** — too many classes loaded before the first frame; this is fixed by a **baseline profile** and by **R8 shrinking**.
     - **`ContentProvider` initialisation** — a third-party SDK's `ContentProvider` runs before `Application.onCreate` and can add hundreds of milliseconds. This is a classic and is invisible in code review. `android:initOrder` no longer controls it on modern Android; the fix is to **remove the provider** with `tools:node="remove"` or make it lazy.
     - **Dex/verification and class count** — check the APK's method count.
     - **Layout inflation** — a huge or deeply nested XML layout inflated synchronously on the main thread; prefer Compose or a flatter hierarchy.
     - **`Application.onCreate` doing work** — initialising three SDKs there.

5. **Fix the common causes.**
   - **Move work off the critical path**: initialise non-critical SDKs lazily (on first use) or on a background thread *after* the first frame. Do not use `runBlocking` on the main thread.
   - **Baseline profile**: ship an AAB with a baseline profile (generated by `BaselineProfileRule`) so ART can AOT-compile the hot paths and skip verification. This typically shaves 20–40% off cold start and improves scroll smoothness.
   - **R8/ProGuard**: enable shrinking and resource shrinking for release. Every removed class is less to load.
   - **App Bundle + `android:extractNativeLibs="false"`** — reduces the install size and the amount of native extraction at install time (and improves install, not start).
   - **Defer third-party SDKs** behind an interface with lazy initialisation, and remove unnecessary `ContentProvider`s.
   - **Avoid blocking I/O on the main thread** — `StrictMode` with `detectDiskReads`/`detectDiskWrites`/`detectNetwork` will flag violations in debug builds.
   - **Reduce the first-frame work** — render a placeholder shell and hydrate the content (skeleton) after the first frame.

6. **Prevent regressions structurally.**
   - **CI gate** on the startup metric (above).
   - **`StrictMode` in the debug build** to catch main-thread disk/network.
   - **A "startup budget" documented in the architecture guide** and enforced by the metric.
   - **A per-dependency review** — every new SDK gets a startup cost assessment (does it have a `ContentProvider`? does it do work in `Application.onCreate`? does it register a broadcast receiver?).
   - **Track it in production too** — `Firebase Performance` (or your own) can report real-world cold-start timings per version, correlated with crashes and ANRs. Lab numbers and field numbers differ, and only the field number is what users feel.

7. **Compare against the previous release, not an absolute.** Absolute thresholds go stale as the device population changes. Always compare **the same metric, the same device, the same procedure, new build vs. baseline build**. That is why the benchmark runs in CI on a pinned device.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Macrobenchmark on a real device | Repeatable, automated, attributable | Needs physical devices in CI; slower; noisy if the runner is busy |
| `adb shell am start -W` | Quick, no setup | Less controlled; no p50/p90 statistics; no trace |
| Startup Profiler / Perfetto | Full attribution of the main-thread cost | Manual analysis; time-consuming |
| Firebase Performance in the field | Real-world numbers, per version, per device | Coarse; noisy; not usable as a build gate |
| Baseline profile | Big cold-start win; also helps scroll jank | Must be regenerated when the code changes; needs a device to collect |
| R8 shrinking | Less to load, smaller APK | Can strip reflection-based code (keep rules); long build times |
| Lazy SDK initialisation | Removes startup cost | Defers the first-use cost; may shift it to a visible moment |
| A strict CI threshold | Prevents regressions | Can be flaky; needs a quiet, dedicated runner |

**Key detail:** The single most-overlooked startup cost is **third-party `ContentProvider` initialisation**, which runs **before** `Application.onCreate()` and therefore before any of your code — so it cannot be deferred by your own code, only removed or avoided. The correct diagnostic is a Perfetto trace of a cold start, not a code review.

**Code example**

```kotlin
// build.gradle.kts (app) — enable shrinking for release.
android {
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

// The launcher Activity must opt in to the baseline profile.
dependencies {
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    baselineProfile(project(":benchmark"))
}

// benchmark/build.gradle.kts — a real CI gate.
android {
    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.3.3")
    implementation("androidx.test.ext:junit:1.2.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
```

```kotlin
// benchmark/src/main/java/.../StartupBenchmark.kt
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = BaselineProfileRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = "com.example.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        setupBlock = { pressHome() },
    ) {
        startActivityAndWait()
    }
}
```

```kotlin
// Defer non-critical SDK work until after the first frame.
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Only what the first frame needs.
        AppGraph.install(this)
        // Everything else, after the UI is up.
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.Default) {
            deferredInitializers.forEach { it.initialize() }
        }
    }
}
```

**Talking points / follow-ups**
- *"Which metric do you gate on?"* → Cold-start **TTID** p90 on a pinned physical device, compared to the previous baseline. TTFD is a useful secondary metric.
- *"How do you attribute a regression to a change?"* → Bisect with the benchmark: build several commits and measure each; the device and procedure must be identical.
- *"What is the biggest surprise in startup profiling?"* → Third-party SDK `ContentProvider`s and `Application.onCreate` work running before your first frame.
- *"Does a baseline profile help?"* → Yes, substantially — it lets ART pre-compile the app's hot paths, removing JIT and verification cost at startup (typically 20–40%).
- *"Should this be enforced in CI?"* → Yes, with a threshold, on a dedicated device, and with the historical baseline stored so the PR shows the delta.

---

### 9. How would you roll out a risky feature gradually and disable it remotely if problems occur?

**What the interviewer is really asking**
Can you design a **staged rollout with a remote kill switch** — the safety mechanism that makes it possible to ship risky changes to a small percentage of users first and pull back in seconds.

**Key concepts**
- **Feature flags**: build-time vs runtime/dynamic; per-user vs percentage; scoped to a cohort
- **LaunchDarkly / Firebase Remote Config / Unleash / Statsig** — flag management
- **Percentage rollout with deterministic bucketing** — a hash of `(userId, flagKey)` → bucket, so a user stays in the same bucket across sessions and devices
- **Staged rollout** in the Play Console: 1% → 5% → 20% → 50% → 100%, with a **halt rollout** button
- **Play "revert"** only affects the rollout, not installed apps (see Section 10, Q5)
- **Remote config fetch cadence** — `min_fetch_interval`; fetch on foreground to reduce the kill-switch latency
- **Flag evaluation caching** and **local defaults** — the app must behave sanely when the config is unavailable
- **Analytics**: exposure events (`flag_exposure`) so you can compare metrics between the treatment and control cohorts
- **Guardrails**: crash-free rate, ANR rate, conversion, latency — with **automated rollback**
- **Server-side flags** — the server decides, which is the fastest kill switch
- **Flag debt** — flags accumulate; they need an owner and a removal date

**Approach**

1. **Classify the risk and choose the mechanism.**
   - **Schema/data-shape risk** (a new backend response shape) → an **API version** or a server-side response flag, so the server can serve the old shape to most clients.
   - **Behaviour risk** (a new checkout flow) → a **client-side flag** read at runtime.
   - **Infrastructure risk** (a new backend service) → a **server-side flag** with a load-balancer/routing decision — the safest because nothing ships to the client.
   - Prefer the **server-side** flag where possible: the client does not need a new release, and the kill switch takes effect on the next request.

2. **Bucket users deterministically.** Percentage rollout must be **stable per user**:
   ```kotlin
   object Bucketing {
       fun bucket(userId: String, salt: String, percentage: Int): Int {
           val hash = MurmurHash3.hash32("$salt:$userId".toByteArray())
           return (hash.toLong() and 0xFFFFFFFFL) % 100
       }
   }
   ```
   Using a hash of `(userId, flagKey)` means: the same user is always in the same bucket for a given flag; different flags have independent distributions (so a user unlucky in one experiment is not unlucky in all); and the assignment survives app reinstalls and device changes. **Never use `Random`** — a user flipping between variants produces inconsistent experiences.

3. **Design the flag contract.** For each flag, define in advance:
   - The **key**, the owner, and the **default value** (what happens if the config never arrives).
   - The **kill switch semantics** — a flag whose *off* state is the safe, shipped behaviour.
   - The **expiry date** — flags are technical debt; a flag that has been at 100% for a month should be deleted.
   - **The metrics** that will be watched and the **threshold** that triggers the rollback.

4. **Make the default safe.** If the remote config cannot be fetched (offline, first launch, the config service is down), the app must use the **safe default** — usually "feature off". And the client must be able to work offline with the cached value. Do not make the default "on" for a risky feature.

5. **Ship in stages with automated gates.**
   - Play Console staged rollout: **1% → 5% → 20% → 50% → 100%**, typically with a minimum soak time (24h) at each stage.
   - At each stage, compare **treatment vs. control** on the guardrail metrics: crash-free users, ANR rate, key funnel conversion, API latency, and support tickets.
   - **Automate the gate**: a job that reads the crash-free rate for the new version/cohort and **automatically halts the rollout** when it drops below the threshold. Automatic rollback is what makes a rollout safe at scale — humans are slow and asleep.

6. **Make the kill switch fast and reachable.**
   - The flag must be readable **without a client release**.
   - **Fetch remote config on every foreground transition** to cut the latency (the default periodic fetch is up to 12 hours). Note the trade-off: a fetch on every foreground costs bandwidth and battery, and the fetch itself is asynchronous, so the kill switch still has a latency of seconds-to-minutes for the *already running* app — the running app will pick it up on its next foreground, and a **server-side flag** takes effect on the very next request.
   - For a client-side-only feature, add a **periodic in-app poll** (e.g. every 60s while the feature is active) if the risk warrants it.

7. **Instrument the flag.** Emit an **exposure event** when the user actually sees the treatment (`flag_exposure` with the flag key and variant). Without exposure events you cannot compute the metric for the treatment cohort correctly — a common and subtle analysis bug. Compare like-for-like cohorts.

8. **Define the rollback runbook before launch.** The runbook must state: who declares a rollback, how the flag is flipped (which console, which key), what the expected effect is, how it is verified (metrics recover within N minutes), and what the customer-facing communication is. Practise it — a kill switch nobody has ever used is not a kill switch.

9. **Handle the "flag flip does not reach everyone" reality.** Users with the app backgrounded, offline, or on an old version keep the old behaviour. State this honestly in the product plan.

10. **Clean up.** After the feature reaches 100% and has soaked, **delete the flag and the dead code** (the R8-kept branches). Track "flag debt" as a metric — the number of live flags and their age. A flag that lives for a year is a permanent branch in your codebase.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Server-side flag | Fastest kill switch; no client release; per-request | Requires server work; only helps server-driven behaviour |
| Client-side remote config flag | Works for pure client behaviour | Kill-switch latency (fetch cadence); needs a client release to add a new flag |
| Play staged rollout | Built-in; familiar; 1% steps | Coarse; no per-user bucketing; does not cover the behaviour itself |
| Deterministic hash bucketing | Stable per user; independent per flag | Needs care: salt and hash choice; users in the same household share a device but not an id |
| Automatic rollback on a metric threshold | Safe at scale; no human latency | False positives can stop a good rollout; needs tuning and a metric pipeline |
| Manual rollback | Simple | Slow; human error; 3am latency |
| Fetch config on every foreground | Lower kill-switch latency | Extra network/battery; still not instant for a running app |
| Safe default = off | Fails safe | The feature is unavailable if the config service is down |

**Key detail:** **A kill switch that requires a client release is not a kill switch.** The flag must be evaluable by code that is *already installed*. This is why server-side flags are the preferred mechanism for risky backend behaviour, and why remote config must be fetched on foreground to be useful as an emergency lever.

**Code example**

```kotlin
// Deterministic, stable bucketing — the same user always gets the same variant.
class FlagEvaluator(private val config: RemoteConfig) {

    fun isEnabled(key: String, userId: String, percentage: Int = 100): Boolean {
        // A hard kill switch always wins over the percentage rollout.
        if (config.getBoolean("${key}_kill_switch")) return false

        val cached = config.getBoolean(key)
        if (!cached) return false            // safe default: OFF

        if (percentage >= 100) return true
        val bucket = MurmurHash3.hash32("$key:$userId".toByteArray()).toLong() and 0xFFFFFFFFL
        return (bucket % 100) < percentage
    }
}

// Fetch on foreground so the kill switch takes effect within seconds, not hours.
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycleScope.launch {
            ProcessLifecycleOwner.get().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    runCatching { config.fetchAndActivate() }   // failure is safe: cached value
                    delay(60_000)                              // poll while in the foreground
                }
            }
        }
    }
}

// Exposure telemetry — without this, cohort analysis is wrong.
class CheckoutViewModel(private val flags: FlagEvaluator, private val analytics: Analytics) {
    fun onCheckoutShown(userId: String) {
        val enabled = flags.isEnabled("new_checkout", userId, percentage = 5)
        analytics.log("flag_exposure", mapOf("flag" to "new_checkout", "variant" to enabled))
    }
}
```

**Talking points / follow-ups**
- *"How fast can you turn the feature off?"* → Seconds for a server-side flag (next request); seconds-to-minutes for a client-side flag with a foreground fetch; days for anything that needs a release.
- *"How do you know the rollout is safe at 20%?"* → Compare the treatment and control cohorts on crash-free rate, ANRs, and the funnel metric — using **exposure** events, not just assignment.
- *"What if the config service is down?"* → The app uses the cached value with the safe default (off). The feature is unavailable, but nothing breaks.
- *"How do you avoid the flag becoming permanent?"* → Every flag has an owner and an expiry; the flag count and age are tracked; a flag at 100% for a month gets deleted along with the dead code.
- *"Why deterministic hashing rather than `Random`?"* → A user must always get the same variant, or they see inconsistent behaviour and report it as a bug.

---

### 10. How would you test database migrations, backward compatibility, and offline synchronization across app upgrades?

**What the interviewer is really asking**
Can you design a **migration test harness** that proves every shipped version can upgrade to the current one, and an **upgrade-safety** strategy that never loses user data?

**Key concepts**
- Room `Migration` objects, `AutoMigration` (Room 2.6+), `schemaLocation` / `room.schemaLocation` for exporting schemas
- **`MigrationTestHelper`** (Room) — creates the DB at a given version with a schema JSON, runs the migration, and validates the result against the expected schema
- Schema export to `schemas/` in version control — **the schema JSON is the contract**
- `fallbackToDestructiveMigration` — **destroys user data**; a last resort that must never ship for a user-facing app
- **Destructive migration risk**: silently wiping a user's local data is a data-loss incident
- Backward compatibility: an old app version must be able to read a DB written by a new version (forward compatibility of the schema) — this is why you **add columns, never drop or rename** in the same release that ships
- "Expand and contract" — a multi-release schema migration strategy
- Instrumentation tests on a device (Room's `MigrationTestHelper` needs instrumentation or Robolectric)
- `AutoMigration` with `from`/`to` and `spec` for column changes
- `RoomDatabase.Callback` / `onOpen` for data migrations
- Sync across upgrades: the outbox/sync-cursor state must survive the upgrade; the **server schema version** must be compatible with the client's local version
- **Backup/restore**: `android:allowBackup`, `Auto Backup` and the fact that a restored DB may be an **older version** than the code expects — the migration must handle it
- Test data helpers: `Room.inMemoryDatabaseBuilder`

**Approach**

1. **Export and version the schemas.** Configure Room to export the schema so it is reviewed in every PR:
   ```kotlin
   // app/build.gradle.kts
   android {
       defaultConfig {
           javaCompileOptions {
               annotationProcessorOptions {
                   argument("room.schemaLocation", "$projectDir/schemas")
                   argument("room.incremental", "true")
               }
           }
       }
   }
   ```
   The exported JSON in `app/schemas/<version>.json` is a **committed artefact** — a schema change shows up in code review, and it is the input to the migration tests. **Never delete the old schema files** — they are what the tests migrate *from*.

2. **Write an explicit `Migration` for every version bump** (or use `AutoMigration` where it is safe). Prefer explicit migrations: an auto-migration cannot express data transformation, and you often need one (renaming a value, backfilling a column, merging rows).
   ```kotlin
   val MIGRATION_3_4 = object : Migration(3, 4) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'UNKNOWN'")
           db.execSQL("UPDATE transactions SET category = 'LEGACY' WHERE category IS NULL")
           db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_accountId_date ON transactions(accountId, date DESC)")
       }
   }
   ```
   Note: `ALTER TABLE ... ADD COLUMN` with a `NOT NULL` requires a `DEFAULT`, otherwise SQLite rejects it.

3. **Test every migration path with `MigrationTestHelper`.** The critical test is not just "the current version migrates" but "**every shipped version migrates to the current one**".
   ```kotlin
   @RunWith(AndroidJUnit4::class)
   class MigrationTest {
       @get:Rule val helper = MigrationTestHelper(
           InstrumentationRegistry.getInstrumentation(),
           MyDb::class.java,
           emptyList(),            // the auto-migrations list
           FrameworkSQLiteOpenHelperFactory(),
       )

       @Test
       fun migratesFrom3To4() {
           helper.createDatabase(DB_NAME, 3).apply {
               execSQL("INSERT INTO transactions (id, amount) VALUES ('t1', 100)")
               close()
           }
           val db = helper.runMigrationsAndValidate(DB_NAME, 4, true, MIGRATION_3_4)
           // validateDroppedTables = true → also checks that dropped tables are gone
           db.query("SELECT category FROM transactions WHERE id = 't1'").use { c ->
               assertTrue(c.moveToFirst())
               assertEquals("LEGACY", c.getString(0))   // the data transformation ran
           }
       }
   }
   ```
   `runMigrationsAndValidate` compares the resulting schema against the **exported schema JSON** for the target version — so it catches a migration that forgets to create an index or renames a column wrongly. This is the test that makes the exported schema meaningful.

4. **Test the full chain.** For each historical version `v` in the shipped set, add a test that migrates `v → current`. To avoid `N` tests, use a helper that applies the migrations in sequence:
   ```kotlin
   @Test
   fun migratesFromEveryShippedVersion() {
       listOf(1, 2, 3, 4, 5).forEach { from ->
           val db = helper.createDatabase(DB_NAME, from)
           db.close()
           val migrated = helper.runMigrationsAndValidate(
               DB_NAME, CURRENT_VERSION, true, *migrationsFor(from),
           )
           migrated.close()
       }
   }
   ```
   Also test **migrating from a version with data** (not an empty DB) — an empty-DB migration can hide a bug where the `UPDATE` statement touches no rows.

5. **Never use `fallbackToDestructiveMigration` for a user-facing app.** If a device has a version of the DB for which no migration path exists (e.g. the app was downgraded, or a migration was missed), Room would either throw or wipe the data. Instead:
   - Ensure every version has a migration path, enforced by the test above.
   - If you must have a fallback, catch it gracefully and **re-sync from the server** rather than wiping — the data is recoverable from the backend; the local cache is not authoritative. Use `fallbackToDestructiveMigrationOnDowngrade` only with a plan to re-fetch.
   - Alternatively, use a **"migration + rebuild"**: rename the old table, create the new one, copy the data across, drop the old — which is exactly what Room's `AutoMigration` does for table changes.

6. **Design for backward compatibility across releases.** The rule: **an old app version must be able to read a DB written by a newer version.** Because a user can always be running the old version while the new one has written data, and Android auto-installs updates — but a user can also *skip* an update and go from v1 to v5.
   - **Add columns, never drop or rename**, in the release where the feature ships. Do the drop/rename in the **next** release, after the old version is no longer in the wild (use a "minimum supported version" server check to force users forward).
   - If you must rename, use the **expand-and-contract** pattern: release N adds `new_name` and copies the data; release N+1 removes `old_name`.
   - Never change a column's **type** in place — SQLite's type affinity makes it work, but the semantics may not. Add a new column and migrate the data.
   - Keep the **sync/outbox state** compatible: the outbox rows and the sync cursor must survive the upgrade, or pending user actions are lost. Test that explicitly.

7. **Test the sync behaviour across an upgrade.** This is the scenario most teams miss:
   - Create a DB at version N with **pending outbox entries** and a **sync cursor**.
   - Run the migration.
   - Assert the outbox entries and the cursor are **still present and valid** — the user's unsent changes must not be lost.
   - Assert that the app then syncs correctly to the server.
   - Also test the **server-side compatibility**: the client sends a `schemaVersion`/`apiVersion` and the server adapts. A client on an old schema talking to a new server must not be sent a payload it cannot parse (see Section 10, Q7).

8. **Handle Auto Backup and restore.** Android's auto-backup can restore a DB that is an **older version** than the running code (the user restored a backup onto a newer app). The migration path must handle that — it does, if the migration chain is complete. But be careful with `allowBackup=false` for databases containing sensitive data (Section 8, Q2), and test the restore path.

9. **Test with production-like data volumes.** A migration that is instant on an empty DB can take seconds on a 500k-row DB (a `CREATE INDEX` over a large table, or an `UPDATE` without an index). Test the migration on a realistic dataset and measure the time — a 10-second migration on launch is an ANR risk (see Section 7, Q9). If it is too slow, use `AutoMigration`'s deferred `CREATE INDEX`, or migrate in the background after the first frame.

10. **Automate it in CI.** Run the migration tests on every PR. Add a **matrix job** that tests every supported-from version. Because the schema JSON is committed, a PR that changes the schema without adding a migration **fails the build** — this is the highest-value automated guard in the whole area.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Explicit `Migration` objects | Full control over data transformation; reviewable | More code; manual |
| `AutoMigration` | Less code; generated and validated by Room | Cannot transform data; limited to supported schema changes |
| `fallbackToDestructiveMigration` | Simple; never fails to open | **Destroys user data** — unacceptable for a user-facing app |
| Re-sync from the server on failure | Data is recoverable from the backend | Requires a network round trip; the offline experience is degraded |
| Expand and contract (multi-release) | Old and new versions coexist safely | Slower rollout; temporary schema bloat; two releases of work |
| Test every shipped version | Catches the "skip an update" path | N tests; slow if done naively — batch them |
| Migrate in the background after the first frame | No launch delay on large DBs | The app runs against the old schema briefly; more complexity |
| Commit the schema JSON | Schema changes are reviewable; enables migration tests | Requires the Gradle config; the files accumulate |

**Key detail:** The single most valuable automated guard here is that **the exported schema JSON is committed**. Because `runMigrationsAndValidate` compares against it, a PR that changes an entity without adding a matching migration fails the test — the migration and the schema can never drift apart.

**Code example**

```kotlin
// app/schemas/<db>/4.json is committed → the schema is a reviewed artefact.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE transactions ADD COLUMN category TEXT NOT NULL DEFAULT 'UNKNOWN'"
        )
        db.execSQL("UPDATE transactions SET category = 'LEGACY' WHERE category = 'UNKNOWN'")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_transactions_accountId_date " +
                "ON transactions(accountId, date DESC)"
        )
    }
}

@Database(entities = [TransactionEntity::class], version = 4,
          exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDb : RoomDatabase() {
    companion object {
        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
    }
}
```

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDb::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratesFrom3To4_preservingPendingSyncState() {
        // 1. A v3 DB with real data AND pending unsynced changes.
        helper.createDatabase("app.db", 3).apply {
            execSQL("INSERT INTO transactions (id, amount, syncState) VALUES ('t1', 100, 'PENDING')")
            execSQL("INSERT INTO sync_cursor (id, cursor) VALUES ('account', 'c-42')")
            close()
        }

        // 2. Migrate and validate against the exported schema for v4.
        val db = helper.runMigrationsAndValidate("app.db", 4, true, MIGRATION_3_4)

        // 3. The user's unsynced changes and the cursor survived the upgrade.
        db.query("SELECT syncState FROM transactions WHERE id = 't1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("PENDING", c.getString(0))
        }
        db.query("SELECT cursor FROM sync_cursor WHERE id = 'account'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("c-42", c.getString(0))
        }
        db.close()
    }
}
```

**Talking points / follow-ups**
- *"Why commit the schema JSON?"* → It makes the schema a reviewable artefact and enables `runMigrationsAndValidate` to fail the build when a migration and the schema drift apart.
- *"What if a user skips an update?"* → Every shipped version must have a migration path to the current version; test that path explicitly.
- *"Can you drop a column?"* → Not in the same release that changes the behaviour — an old app version may still be running. Use expand-and-contract across two releases.
- *"What happens if the migration is slow on a large DB?"* → A 500k-row `CREATE INDEX` can take seconds. Move it off the launch path and measure it with production-like data.
- *"Is `fallbackToDestructiveMigration` ever acceptable?"* → Only if the data is a pure cache that can be re-fetched. Never for data that exists only on the device (drafts, offline transactions).

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Determinism as the foundation** | 1, 2, 3, 4 — injected time/ids/randomness, virtual time, `backgroundScope`, no global state |
| **Testing the semantics, not the implementation** | 1, 2, 3 — unknown ≠ failed; assert on state transitions and invariants, not on call ordering |
| **Making failures visible before release** | 5, 8, 9 — kill switches, staged rollout with automated gates, startup regression budgets |
| **Cross-team contracts and reproducibility** | 6, 7, 10 — pinned dependencies and toolchains, Pact/OpenAPI, exported Room schemas |
| **Isolation and hygiene** | 1, 4 — fresh DI graph and database per test, rules that always restore global state |

**The one sentence to memorise for this section:**
> *"Flaky tests are a property of the harness, not of the code — make time, randomness and I/O injectable, isolate every test's state, and assert on the semantics that matter; and on the release side, ship behind a server-driven kill switch with automated crash gates, because the rollback you wish you had is the one you never needed."*
