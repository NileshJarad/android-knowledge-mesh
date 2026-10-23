# Android Lifecycle and Process Death

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. An Activity is recreated during screen rotation while an API request is running. How would you prevent unnecessary duplicate requests?

**What the interviewer is really asking**
Do you understand the *exact* recreation contract — that a configuration change destroys the Activity/Fragment but **not** the ViewModelStore, so the ViewModel and its in-flight coroutine survive — and can you place the request in the right scope? A junior answer is "cancel the request in `onDestroy`" (wrong: it duplicates work and loses the result). A staff answer is "the request belongs to a scope whose lifetime is decoupled from the View hierarchy, and the result is delivered to whichever UI instance is currently subscribed."

**Key concepts**
- Configuration change ≠ process death: `Activity`/`Fragment` recreated, `ViewModelStore` retained
- `viewModelScope` is cancelled only in `ViewModel.onCleared()`, not on rotation
- `viewModelScope` uses `Dispatchers.Main.immediate`; cancellation propagates to Retrofit's `Call.cancel()`
- Repository-level caching / single-flight so *even a ViewModel scope loss* does not refetch
- `stateIn(viewModelScope, SharingStarted.WhileSubscribed(...))` vs `Eagerly`
- `android:configChanges` as an escape hatch, and why it is usually the wrong answer

**Approach**

1. **Get the contract right.** On rotation the platform calls `onDestroy()` on the old Activity and `onCreate()` on a new one, but the **same `ViewModel` instance** is retained by the `ViewModelStore` (retained via `NonConfigurationInstances` — since AndroidX it is a `ViewModelStore` held by `ComponentActivity`). Therefore a coroutine launched in `viewModelScope` is **not** cancelled by rotation. It completes, and the new Activity observes the same `StateFlow`.

2. **Put the request in the ViewModel, not in `onCreate()`/`LaunchedEffect` of the Activity.**
   ```kotlin
   // Wrong: request is re-issued on every Activity instance.
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       lifecycleScope.launch { viewModel.load() }   // fires again after rotation
   }
   ```
   ```kotlin
   // Right: the ViewModel is retained across the configuration change.
   class ProfileViewModel(private val repo: ProfileRepository) : ViewModel() {
       private val _state = MutableStateFlow<UiState>(UiState.Loading)
       val state: StateFlow<UiState> = _state.asStateFlow()

       init { load() }   // runs exactly once per ViewModel instance
       fun load() { viewModelScope.launch { /* ... */ } }
   }
   ```

3. **Make the state a `StateFlow` in the ViewModel, collected by the UI with `collectAsStateWithLifecycle()`.** The new Activity instance collects the *current* value immediately — there is no "loading flicker", because the state already lives outside the view.

4. **Guard against the case the ViewModel does *not* survive: a fresh ViewModel.** `ViewModel.onCleared()` is called when the user finishes the Activity (not on rotation). If the request must survive *that*, the request belongs in a **repository with its own application-scoped job**, not in `viewModelScope`. The ViewModel then just *observes* the repository's `StateFlow`:
   ```kotlin
   class ProfileRepository(private val scope: CoroutineScope) {
       private val _profile = MutableStateFlow<ProfileState>(ProfileState.Loading)
       val profile: StateFlow<ProfileState> = _profile.asStateFlow()
       fun refresh() { scope.launch { /* ... */ } }   // survives the ViewModel
   }
   ```
   This is the "repository is the source of truth, ViewModel is a thin observer" pattern.

5. **Add single-flight / de-duplication in the repository** so two concurrent callers (e.g. two screens, or a retry after `onCleared`) share one in-flight request:
   ```kotlin
   private var inFlight: Deferred<Profile>? = null
   suspend fun profile(): Profile {
       inFlight?.let { return it.await() }
       val d = scope.async { api.profile() }
       inFlight = d
       try { return d.await() } finally { if (inFlight === d) inFlight = null }
   }
   ```
   Also cache the last successful result with a TTL so a re-created ViewModel shows cached data instantly instead of a spinner.

6. **Do not reach for `android:configChanges`.** Handling `orientation|screenSize|keyboardHidden` yourself keeps the Activity alive and dodges the problem, but it leaks resources (the Activity holds the old `Context`, `Resources`, and view tree), breaks multi-window/foldable handling, and the platform still recreates for unlisted changes (locale, night mode, font size, density). Fix the *state* instead.

7. **Verify with a test.** `ActivityScenario.recreate()` recreates the Activity in-process; assert the fake API was called **exactly once**.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Request in `viewModelScope` | Survives rotation; result delivered to the new UI instance; zero extra plumbing | Lost on `onCleared()` (user navigated away for good) — acceptable for most screens |
| Request in a repository with an app-level scope | Survives rotation *and* ViewModel destruction; result re-observable | Must design lifetime deliberately; a leak if the scope is the Application's forever; stale data risk |
| `android:configChanges="orientation\|screenSize"` | Activity is not recreated; simplest "fix" | Leaks the old Activity; breaks `onConfigurationChanged` correctness; still recreated for locale/density; hides the real problem |
| Single-flight in the repository | No duplicate network calls even under concurrent access | Needs careful identity-checked cleanup of the in-flight `Deferred` |
| Cache + TTL in the repository | Instant content on re-entry, good UX | Stale data; needs an explicit refresh path and a staleness indicator |
| `SharingStarted.WhileSubscribed(5_000)` | Keeps the upstream alive briefly across config changes so no reload happens | Slightly surprising for a `viewModelScope` (which already survives); `Eagerly` is usually simpler inside a ViewModel |

**Key detail to call out:** `viewModelScope` is built on `Dispatchers.Main.immediate` and cancelled in `onCleared()`. The coroutine therefore survives rotation *by construction*, and a cancellation triggered by `onCleared()` propagates into Retrofit's `Call.cancel()` — which closes the socket but does **not** undo a request the server already processed. Combine with the idempotency discussion in Section 1, Q1.

**Code example**

```kotlin
class ProfileViewModel(private val repo: ProfileRepository) : ViewModel() {

    // Survives rotation because the ViewModelStore is retained.
    // stateIn(..., WhileSubscribed(5_000)) keeps the upstream warm across recreation.
    val state: StateFlow<UiState> = repo.profile
        .map { UiState.Success(it) }
        .catch { emit(UiState.Error(it.toUiText())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun refresh() { repo.refresh() }   // idempotent: single-flight inside the repo
}

// The Activity only observes; it never fires the request itself.
class ProfileActivity : AppCompatActivity() {
    private val vm: ProfileViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { render(it) }
            }
        }
    }
}
```

**Talking points / follow-ups**
- *"How do you prove the request isn't duplicated?"* → `ActivityScenario.recreate()` + a fake API counting calls; assert count == 1.
- *"What if the ViewModel is cleared because the user finished the Activity?"* → The request is cancelled and the socket closed; the server may still have processed it. If the call has side effects, it needs an idempotency key (Section 1, Q1) and a reconciliation path.
- *"What about `onSaveInstanceState` — could you cache the response there?"* → Technically possible (if the response is `Parcelable`), but it bloats the saved-state `Bundle` and risks `TransactionTooLargeException`. Caching in the repository is better.
- *"How does `LaunchedEffect` in Compose fit in?"* → `LaunchedEffect` is keyed to the *composition*, and a configuration change recreates the composition, so it re-runs. Hoist the request into the ViewModel or a `remember`ed state holder with `rememberSaveable` only if you want it tied to the composition. `produceState`/`collectAsStateWithLifecycle` on a ViewModel-backed `StateFlow` is the standard fix.
- *"What about `onConfigurationChanged` not being called if you declare `configChanges`?"* → The system calls it only for the specific configurations you list; `onCreate` is not re-run, so you must manually re-inflate/re-apply resources. This is why it is a last resort.

---

### 2. The Android process is killed while the app is in the background. How would you restore the user's screen state?

**What the interviewer is really asking**
Can you distinguish the three "kinds of death" — **configuration change** (ViewModel survives), **process death** (everything in memory is gone; only the saved-instance-state `Bundle` survives), and **task removal / swipe-away** (Activity destroyed, process may live) — and do you know which storage mechanism belongs to which tier? The staff-level answer is a *tiered state model*, not a single mechanism.

**Key concepts**
- Process death: the system reclaims the process to reclaim memory; the task is retained with its saved instance state
- `onSaveInstanceState(Bundle)` → the system persists it to disk and restores it on recreation
- `SavedStateHandle` is backed by the same `Bundle` mechanism (`SavedStateRegistry` + `SavedStateRegistryController`)
- `SavedStateHandle` survives process death **only if** `onSaveInstanceState` ran before the kill
- `Bundle` transaction limit → `TransactionTooLargeException` (~500 KB–1 MB Binder limit)
- Room / DataStore / files = durable truth that survives uninstall-free process death and task removal
- `android:alwaysRetainTaskState`, `noHistory`, `finishAndRemoveTask`

**Approach**

1. **Classify the state before choosing a mechanism.** Ask: *how much data*, *how long must it live*, *must it survive process death?*

2. **Tier 1 — transient view state (`remember`).** Scroll offset not worth restoring, an animation flag, a text-field draft that the user has not committed. Dies with the composition. Cost: zero. If it must survive rotation but is small, use `rememberSaveable` (which writes to the `Bundle` via `Parcelable`/`Bundle`-compatible savers).

3. **Tier 2 — screen/business state (`ViewModel`).** Survives configuration change, dies on `onCleared()`. This is where in-flight request state, filtered lists, and form logic live.

4. **Tier 3 — restoreable state across process death (`SavedStateHandle`).** `SavedStateHandle` in a ViewModel is wired to the Activity/Fragment's `SavedStateRegistry`. When the process is killed, the `Bundle` the system wrote in `onSaveInstanceState` is restored, and `SavedStateHandle` is rehydrated with it. Key caveats to state out loud:
   - It only holds `Bundle`-able types: primitives, `String`, `Parcelable`, `Serializable`, `ArrayList` of those. A `List<MyModel>` needs a `Parcelable`/`Serializable` model or a custom `Saver`.
   - It survives process death **only if** the system actually got to `onSaveInstanceState` before killing the process. If the process is killed while the app is in the *foreground*, or killed via `adb shell am kill` without a saved state, the handle is empty. This is why "restore gracefully" must handle the empty case.
   - It is **not durable**: the `Bundle` is dropped when the task is removed, and it is not a database.

5. **Tier 4 — durable truth (`Room`, `DataStore`, files).** Anything the user would be *angry* to lose: a draft message, a shopping cart, a completed multi-step form, a pending transaction. Write it to disk at the moment the user commits it, not at `onSaveInstanceState`.

6. **The restore pipeline.** On recreation after process death:
   ```
   savedInstanceState Bundle
        └─> SavedStateRegistry restores SavedStateHandle values
        └─> ViewModel created with the rehydrated SavedStateHandle
              └─> VM reads handle for "resume where I was"
                    └─> if the handle is empty (kill before save), fall back to the DB
   ```
   The fallback is the crucial part: **never assume the handle has data.** Code the "cold start with no state" path as the primary path and treat the restored handle as an optimisation.

7. **Restore the *destination*, not just the data.** Persist a route/destination id (`"checkout/step2"`) alongside the data so that after process death the app navigates to the right screen. With `androidx.navigation`, save the `NavController` back stack using `navController.saveState()` / the `NavHostFragment`'s own `SavedStateHandle` support (Navigation 2.4+ saves the back stack automatically), plus your own destination argument.

8. **Handle the "no saved state" case gracefully.** On a cold launch there is no `savedInstanceState` and the ViewModel is new. The screen must render a sensible default (e.g. "resume your draft?" if a draft exists in the DB) rather than an empty broken form.

9. **Do not stuff the `Bundle`.** The `Bundle` is marshalled through Binder during `onSaveInstanceState` and the system holds it in memory; exceeding the transaction limit throws `TransactionTooLargeException` and the app crashes on the *next* start. Keep the saved state to identifiers and small primitives — put the bulk in Room.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `remember` | Zero cost, zero ceremony | Lost on rotation; worthless for restore |
| `rememberSaveable` | Survives rotation and process death | `Bundle`-able types only; ~500 KB limit → `TransactionTooLargeException`; lost when the task is removed |
| `ViewModel` | Survives rotation; rich typed state; coroutine scope | Dies on `onCleared()`; **does not** survive process death |
| `SavedStateHandle` | Survives process death; type-safe-ish; scoped to the screen | Only `Bundle`-able values; empty if the process died before `onSaveInstanceState`; not durable across task removal |
| `DataStore` | Durable, transactional, `Flow`-able | Not appropriate for large structured data; async API |
| `Room` | Durable, queryable, relational, survives everything | Heavier; needs a schema and migrations; a write on every keystroke is wasteful — write on commit, not on every change |
| Writing state to disk eagerly | Always recoverable | I/O cost on the main thread risk; wasteful for ephemeral state |
| `android:alwaysRetainTaskState="true"` | Keeps the task's `Bundle` alive | Only affects task removal, not process death; does not save in-memory state |

**Key detail:** `SavedStateHandle`'s survival across process death is *entirely* dependent on the system having written `onSaveInstanceState` before reclaiming the process. The system guarantees this for backgrounded apps it kills, but not for every kill. Treat the handle as a *fast path*, and Room/DataStore as the *truth*.

**Code example**

```kotlin
class CheckoutViewModel(
    private val savedStateHandle: SavedStateHandle,   // survives process death (Bundle)
    private val draftDao: DraftDao,                    // Room: durable truth
) : ViewModel() {

    private val _state = MutableStateFlow<CheckoutState>(CheckoutState.Empty)

    init {
        // Fast path: restored from the saved-instance-state Bundle.
        val restoredStep = savedStateHandle.get<Int>(KEY_STEP)
        val restoredId = savedStateHandle.get<String>(KEY_DRAFT_ID)
        // Fallback: the durable source of truth, always available.
        val draft = draftDao.findDraft(restoredId ?: draftDao.latestDraftId())
        _state.value = if (draft != null) CheckoutState.Resumed(draft)
                       else CheckoutState.Empty
    }

    fun onStepChanged(step: Int) {
        savedStateHandle[KEY_STEP] = step              // Bundle-able primitive
        draftDao.updateStep(step)                     // durable
    }

    private companion object { const val KEY_STEP = "step"; const val KEY_DRAFT_ID = "draftId" }
}
```

**Talking points / follow-ups**
- *"What is the difference between process death and task removal?"* → Process death = the process was killed; memory is gone, the task and its saved `Bundle` remain. Task removal (swipe away from Recents) = the task is gone; the saved `Bundle` is discarded too.
- *"Does `SavedStateHandle` survive process death?"* → Yes, via the saved-instance-state `Bundle` — **provided** the system saved the state before killing the process. It does not survive task removal or app data being cleared.
- *"How much can I put in the `Bundle`?"* → The system marshals it over Binder; the practical ceiling is a few hundred KB and overshooting throws `TransactionTooLargeException`. Keep to ids and small primitives.
- *"How do you test process death?"* → Developer Options → "Don't keep activities" for rotation-level tests; `adb shell am kill <package>` or `adb shell am force-stop` for hard kills; and `adb shell am start` to observe restore. Also `ActivityScenario` + `recreate()` for the config-change tier.
- *"What is `onRestoreInstanceState` vs `onSaveInstanceState`?"* → The former is called only when the system *is* restoring a previously saved `Bundle`; the latter is called on every stop (and on every config change) to write it.

---

### 3. A user navigates away from a screen while a network request is in progress. Should the request continue or be cancelled?

**What the interviewer is really asking**
Do you distinguish **"the user stopped looking"** from **"the work should stop"**? The canonical staff answer is to **separate observation from execution**: cancel the *collection* of the result (the UI no longer cares), but decide deliberately — per operation — whether the *work* continues. And can you explain what cancelling actually does to the HTTP request and to the server?

**Key concepts**
- Structured concurrency: cancelling the collector does not cancel a `viewModelScope` job unless the job is a child of it
- `repeatOnLifecycle(STARTED)` stops collection when the screen goes to `STOPPED` — but the upstream flow keeps running unless it is tied to the scope
- `viewModelScope` is cancelled only at `onCleared()` — i.e. when the Activity is *finished*, not merely backgrounded
- `CancellationException` semantics; Retrofit/OkHttp `Call.cancel()` closes the socket
- Server-side effect is unaffected by client cancellation (Section 1, Q12)
- Scope separation: app scope for must-survive work; ViewModel scope for screen-bound work

**Approach**

1. **Ask what the operation *is* before deciding.** A read (feed refresh, profile fetch) is safe to cancel. A write (payment, upload, "submit application") is usually **not** safe to abandon blindly — cancel the UI wait, but keep the operation running and reconcile later.

2. **Understand the platform's default behaviour — it may already be what you want.**
   - Backgrounding the screen (Home button): the Activity goes to `STOPPED`, but the Activity is **not** destroyed, so `viewModelScope` keeps running and the request continues. With `repeatOnLifecycle(STARTED)` the *collection* stops, so the UI stops updating — but the work continues and the result is stored in the `StateFlow` for when the user returns.
   - Pressing Back (finishing the Activity): the Activity is destroyed, `onCleared()` runs, `viewModelScope` is cancelled → the Retrofit call is cancelled → the socket closes → the pending read fails with an `IOException`. The **server may still have processed the request.**

3. **Make the decision explicit in code, not by accident.** Two shapes:
   - *Cancel-on-leave* (reads): launch in `viewModelScope`; it dies with the screen.
   - *Continue-in-background* (writes, uploads): launch in a scope owned by the repository/application, and have the ViewModel *observe* the result. Cancelling the collector leaves the work running.

4. **Stop the collection with lifecycle-aware APIs.** `repeatOnLifecycle(Lifecycle.State.STARTED)` cancels the collector when the screen goes below `STARTED` and restarts it on return — and because the source is a `StateFlow`, the UI immediately receives the latest value with no lost intermediate states. `flowWithLifecycle` / `collectAsStateWithLifecycle` do the same for the modern Compose path.

5. **Never let a stale result mutate a dead screen.** In a ViewModel this is automatic (the VM is gone). In a shared singleton or a repository-scoped `StateFlow`, guard against "result arrives after the user left" by keying results to a request id / user id and ignoring mismatched ones.

6. **If the work must survive even the Activity being finished, use a real background mechanism**: `WorkManager` (deferrable, constraint-aware, survives process death) or a foreground service (`androidx.work` `setForeground` for long uploads). A plain `viewModelScope` coroutine cannot survive the process being killed.

7. **Model the outcome as tri-state when the request has side effects.** `Success` / `Failure` / `Cancelled-with-unknown-outcome`. If the operation was cancelled after being sent, the client cannot know the server's state → reconciliation via an idempotency key (Section 1, Q1/Q2).

8. **Do the cleanup in `finally`, and make it cancellation-safe.** Use `withContext(NonCancellable)` for the persistence step so the record of "this operation was in flight" is written even though the scope is cancelled.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Cancel everything when the screen is left | Saves battery and bandwidth; simplest mental model | For a write, the outcome becomes unknown and the user may think it failed when it succeeded — the classic "payment said failed but money moved" bug |
| Let `viewModelScope` continue until `onCleared()` | Default behaviour; results are available when the user returns to a still-alive screen | Keeps the radio on in the background; if the Activity was *finished*, the work dies anyway |
| Move the work to an application/repository scope | Work completes; the truth is known; result re-observable | Needs explicit lifetime design; a scope that outlives the app is a leak; risk of updating a UI the user has left |
| `WorkManager` for deferrable work | Survives process death; constraint-aware; retry/backoff built in | Not suitable for a user-facing request the user is currently waiting on (latency) |
| Foreground service for long transfers | Continues under background restrictions; user-visible notification | Notification + permission (Android 13+ `POST_NOTIFICATIONS`) + `FOREGROUND_SERVICE_DATA_SYNC` type complexity |
| Ignoring the result after navigate-away | Simplest | Silently loses the answer; the user must reload manually |

**Key detail:** the most common misconception is that "backgrounding the app cancels the request". It does **not** — backgrounding only stops the *collection* under `repeatOnLifecycle`. The request keeps running in `viewModelScope` until the Activity is finished or the ViewModel is otherwise cleared.

**Code example**

```kotlin
class FeedViewModel(private val repo: FeedRepository) : ViewModel() {

    val state: StateFlow<UiState> = repo.feed
        .map { UiState.Success(it) }
        .catch { emit(UiState.Error(it.toUiText())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun retry() = viewModelScope.launch { repo.refresh() }   // cancel-on-leave read
}

// The repository owns the long-running, side-effecting work.
class FeedRepository(private val appScope: CoroutineScope, private val api: FeedApi) {

    private val _feed = MutableStateFlow<Feed>(Feed.Empty)
    val feed: StateFlow<Feed> = _feed.asStateFlow()

    fun refresh() {
        appScope.launch {                       // survives the ViewModel being cleared
            try {
                _feed.value = api.fetch()
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _feed.value = Feed.Error(t)
            }
        }
    }
}

// The UI stops *collecting* when it is not visible; the work is unaffected.
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        vm.state.collect { render(it) }
    }
}
```

**Talking points / follow-ups**
- *"What does cancelling a Retrofit call actually do?"* → It calls `Call.cancel()`, closing the socket; the pending read fails with an `IOException`/"Canceled". The server is unaffected and may still commit the request.
- *"How do I know whether the screen was merely backgrounded or actually finished?"* → `isFinishing` / `isDestroyed` in the Activity, or observe the ViewModel's `onCleared()`.
- *"Should I cancel in `onStop()`?"* → Almost never for reads; the platform already gives you `viewModelScope` lifetime. Only cancel when the request is expensive *and* the data is definitely not wanted (e.g. a large download the user navigated away from).
- *"How do you avoid a result arriving after the user left, updating a stale screen?"* → Scope the state to the ViewModel (per-screen), or key results with a request id and drop mismatches in a shared repository.
- *"What about a screen that is still alive but backgrounded — should it keep updating the UI?"* → No: `repeatOnLifecycle(STARTED)` stops the UI updates and resumes them on return, delivering the latest value.

---

### 4. A configuration change causes duplicate collectors for a StateFlow. How would you prevent them?

**What the interviewer is really asking**
This is really about **where you subscribe** and **how the subscription is scoped**. The interviewer wants to hear that a `StateFlow` is *hot* and multi-subscriber (so "duplicate collectors" is not a data-corruption problem), but that duplicate *collection side effects* — duplicate network calls, duplicate DB queries, duplicate analytics — are a real bug, and that the fix is to move the subscription out of the recreated scope into a retained one, or to make the flow cold and owned by the ViewModel.

**Key concepts**
- `StateFlow` is hot and conflated: multiple collectors each get the same values; no duplicate emission to a single collector
- `SharedFlow` (replay, extraBufferCapacity) vs `stateIn`/`shareIn`
- `SharingStarted.Eagerly` / `WhileSubscribed(stopTimeoutMillis)` / `Lazily`
- `collectAsStateWithLifecycle()` (Compose) — lifecycle-aware, stops in the background
- `repeatOnLifecycle(Lifecycle.State.STARTED)` — the XML/Fragment equivalent
- `LaunchedEffect(key)` keys — an unstable key re-runs the effect; a key derived from a changing value causes re-subscription
- `flowOn` vs `collect` context; `stateIn` upstream scope

**Approach**

1. **First, correct the framing.** A `StateFlow` collected twice does **not** duplicate the data — it is a hot, replaying, conflated flow. Every active collector receives the same current value. The real problem is that *each collector runs the same upstream side effects* if the upstream is cold and shared incorrectly:
   ```kotlin
   // Cold flow: every collector runs its own query.
   val items = flow { dao.queryAll() }        // ← N collectors ⇒ N queries
   ```
   ```kotlin
   // Hot flow, shared upstream: one query, many collectors.
   val items = flow { dao.queryAll() }
       .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
   ```
   Room's `Flow` queries are cold, so **sharing them is mandatory** the moment two subscribers exist.

2. **Put the subscription in a scope that survives the configuration change.** The `ViewModel` is retained; the Activity is not. So:
   ```kotlin
   class ListViewModel(private val dao: ItemDao) : ViewModel() {
       val items: StateFlow<List<Item>> = dao.observeItems()
           .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
   }
   ```
   Now there is exactly **one** upstream subscription for the lifetime of the ViewModel, no matter how many times the Activity is recreated.

3. **In Compose, use `collectAsStateWithLifecycle()`, not `collectAsState()`.** The former subscribes only between `STARTED` and `STOPPED` and cancels on stop; the latter keeps collecting in the background. Combine with `LaunchedEffect(Unit)` (a **stable** key) for side effects:
   ```kotlin
   val items by vm.items.collectAsStateWithLifecycle()
   LaunchedEffect(Unit) { vm.loadIfNeeded() }   // stable key → runs once per composition
   ```

4. **Watch out for unstable `LaunchedEffect` keys.** `LaunchedEffect(vm.filter)` re-runs whenever `filter` changes — that is *intended* for filter-driven work, but `LaunchedEffect(listOf(x))` allocates a new list each recomposition and re-runs the effect every time. Use `remember { derivedStateOf { ... } }` or a stable key.

5. **Do not "fix" it by cancelling aggressively.** Cancelling the collection in `onStop()` and re-subscribing in `onStart()` is fine, but cancelling in `onDestroy()` and re-subscribing in the new Activity's `onCreate()` is what causes the duplicate *upstream* work — unless the upstream is shared in the ViewModel.

6. **`WhileSubscribed` timeout is the subtle knob.** `SharingStarted.WhileSubscribed(5_000)` keeps the upstream alive for 5 seconds after the last subscriber goes away, which smooths rotation (the new Activity subscribes within milliseconds) and avoids tearing down a database cursor. `WhileSubscribed(0)` restarts the upstream on every rotation — a flicker and a redundant query. `Eagerly` starts the work even with no subscriber, which is right for "keep the cache warm" but wrong for expensive work.

7. **For `SharedFlow` events**, use a `Channel`-backed pattern or `MutableSharedFlow` with `extraBufferCapacity` and explicit replay semantics; a `SharedFlow` with `replay = 0` will drop events if there is no subscriber — a common source of "the event was emitted but nobody received it" bugs.

8. **Verify with a counting fake.** Use a Room DAO fake that counts `observeItems()` invocations, rotate the Activity, and assert the count is 1.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `stateIn` in the ViewModel | One upstream subscription; survives rotation; canonical | Requires a ViewModel; the scope's lifetime must be understood |
| `collectAsStateWithLifecycle()` | Lifecycle-aware; no background work | Compose-only; slightly more verbose than `collectAsState` |
| `collectAsState()` | Simplest | Keeps collecting in the background; wastes resources; no lifecycle awareness |
| `repeatOnLifecycle(STARTED)` | Fragment/XML equivalent; explicit | Boilerplate; must remember to cancel the inner collection |
| `SharingStarted.Eagerly` | Cache stays warm; no restart flicker | Work runs even when nobody is watching — battery cost; can start a fetch before the user opens the screen |
| `SharingStarted.WhileSubscribed(0)` | Releases resources immediately | Restarts upstream on every rotation → duplicate work and UI flicker |
| `SharingStarted.WhileSubscribed(5_000)` | Smooths rotation; standard default | Holds resources for 5 s after the screen closes |
| `subscribeIn(scope)` on a cold flow | Explicit about the owning scope | Easy to leak if the scope is the Activity's; must `stateIn`/`shareIn` to dedupe |

**Key detail:** `stateIn`/`shareIn` create the shared upstream **once**; the `scope` you pass is where the upstream runs, not where the collectors live. Passing `viewModelScope` means the upstream is cancelled at `onCleared()` — correct for screen-scoped data, wrong for a process-wide cache.

**Code example**

```kotlin
class ListViewModel(private val dao: ItemDao) : ViewModel() {

    // One upstream subscription for the whole ViewModel lifetime.
    val items: StateFlow<List<Item>> = dao.observeItems()      // cold Room query
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),    // survives rotation
            initialValue = emptyList(),
        )

    fun setFilter(f: Filter) { filter.value = f }                // stable key source
}

// Compose: lifecycle-aware collection; the effect key is stable.
@Composable
fun ListScreen(vm: ListViewModel) {
    val items by vm.items.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.refreshIfStale() }   // stable key → runs once

    LazyColumn {
        items(items, key = { it.id }) { ItemRow(it) }
    }
}
```

**Talking points / follow-ups**
- *"Is it actually a bug to have two collectors of a StateFlow?"* → No, not for the data. It is a bug if the *upstream* is cold and therefore duplicated. Say this explicitly — it shows you understand hot vs cold.
- *"What is `WhileSubscribed`'s timeout for?"* → Bridging the gap between the old Activity's collector going away and the new one arriving during a configuration change, and between navigation steps.
- *"Why did `collectAsState` change to `collectAsStateWithLifecycle`?"* → The latter is lifecycle-aware and cancels in the background, fixing both wasted work and the "state updates arrive when the screen is not visible" class of bugs.
- *"How do you prove there is no duplicate work?"* → Count invocations of the fake DAO with `ActivityScenario.recreate()`; assert 1.
- *"What about `LaunchedEffect` re-running because its key changed?"* → That is the intended mechanism for reacting to input changes; the bug is using a non-stable key (a freshly allocated list/object) for something that should run once.

---

### 5. A user presses Back during a multi-step form. How would you preserve entered data without keeping the Activity alive?

**What the interviewer is really asking**
Can you distinguish *"the user left"* from *"the user abandoned the work"*? And do you know the correct scope for each piece of a multi-step form's state — draft data (durable), step index (restoreable), in-flight validation (ephemeral)? The staff answer: **durable draft + `SavedStateHandle` for the step + `ViewModel` for the logic**, and an explicit *resume or discard* decision on return.

**Key concepts**
- `OnBackPressedDispatcher` / `OnBackPressedCallback` for intercepting Back
- `NavController` `popBackStack()` and `popUpTo` for multi-step navigation
- `SavedStateHandle` in a shared/activity-scoped ViewModel for the step index
- `SavedStateHandle` for form fields (Bundle-able primitives) vs Room/DataStore for rich drafts
- `ViewModel` scoped to the Activity (`activityViewModels()`) to share one draft across steps
- Back-stack restoration after process death; `NavController` `saveState`/`restoreState`
- `isChangingConfigurations` — distinguishing "Back due to rotation" from "user left"

**Approach**

1. **Model the form as one state object, not as per-screen state.** A multi-step form is one logical entity ("checkout"). It should have one owner — typically a ViewModel scoped to the `NavHostFragment`'s Activity — so that step transitions do not lose data. Steps are just *views* over that shared state.

2. **Persist the draft durably as the user progresses.** Write to Room/DataStore when the user commits a step or pauses (debounced), not on every keystroke. That is what survives Back, process death, and app kill. `SavedStateHandle` alone is *not* enough: it is dropped when the task is removed and it is capped at the `Bundle` size.

3. **Use `SavedStateHandle` for the small, restoreable bits:** the current step index, the draft id, the selected option ids. These are primitives/strings — perfect for the `Bundle`.

4. **Handle Back with `OnBackPressedDispatcher`, not by overriding `onBackPressed()`.** The dispatcher is the modern, composable API and plays correctly with predictive back on Android 13+/14:
   ```kotlin
   onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
       override fun handleOnBackPressed() {
           // Persist, then decide: go back a step, or leave the flow entirely.
       }
   })
   ```
   Decide the semantics explicitly:
   - *Non-destructive Back* → persist the draft, `popBackStack()` to the previous step, and let the user resume later.
   - *Destructive Back* → if the form is untouched, just pop; if dirty, show a confirm dialog ("Save draft and exit?" / "Discard changes?").

5. **Distinguish "Back" from "rotation".** `isChangingConfigurations` is true during a configuration change; do not treat it as abandonment. Only persist-and-exit when the Activity is actually finishing.

6. **Use `NavController` for the steps**, with the shared ViewModel owning the data:
   ```kotlin
   // In each step Fragment/Composable, scope the ViewModel to the Activity so all steps share it.
   private val vm: CheckoutViewModel by activityViewModels()
   ```
   Or, in Compose, hoist the ViewModel to the `NavHost` level and pass it down.

7. **Restore the whole flow after process death.** Because the draft lives in Room, the restore path is: on app start, read the draft → if it exists and is not expired, offer "Resume your order" → navigate to the saved step. The `NavController` back stack itself is saved by Navigation 2.4+ (`NavHostFragment` saves the back stack into the saved-state `Bundle`), but the *business* state is your responsibility.

8. **Do not keep the Activity alive to "preserve" the form.** `android:noHistory` and `finishAndRemoveTask()` make things worse. The Activity should be allowed to die; the *data* is what persists.

9. **Handle multi-process / multi-window edge cases.** If the app can have two instances of the Activity alive (multi-window, or the user starting the flow twice), the draft must be keyed by a draft id so the two do not clobber each other.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Persist draft to Room/DataStore | Survives Back, process death, task removal; enables "resume later" | Extra write path; must manage draft expiry/cleanup; a stale draft can confuse the user |
| `SavedStateHandle` only | Zero extra infra; automatic | Lost when the task is removed; `Bundle` size limit; empty if the process died before `onSaveInstanceState` |
| Activity-scoped ViewModel (`activityViewModels()`) | One owner for all steps; survives rotation | Tightly couples steps together; a very long-lived ViewModel can hold memory; multiple independent flows in one Activity collide |
| Destination-scoped ViewModel (`navGraphViewModels` / per-destination) | Steps are independent; natural back-stack scoping | Data must be passed explicitly between steps — more plumbing; risk of losing state on pop |
| Override `onBackPressed()` | "Works" | Deprecated; breaks predictive back and the dispatcher; not composable with other consumers |
| `OnBackPressedDispatcher` + `OnBackPressedCallback` | Correct, composable, predictive-back-ready | Must enable/disable callbacks as state changes (`isEnabled`) |
| Intercept Back and always save+exit | Predictable | Too aggressive for a multi-step flow — the user may just want to go back one step |
| Keep the Activity alive (`noHistory=false`, hold in memory) | Instant resume | Leaks memory; never survives process death anyway; an anti-pattern |

**Key detail:** the distinction that earns staff credit is **"the flow" vs "the step"**. A multi-step form is one business object with one draft id; each step is a rendering of it. Anything that assumes "the Activity holds the form" breaks as soon as the process is killed.

**Code example**

```kotlin
class CheckoutViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val draftDao: DraftDao,
) : ViewModel() {

    val draft: StateFlow<Draft> = savedStateHandle.getStateFlow(KEY_DRAFT, Draft.Empty)

    fun update(block: (Draft) -> Draft) {
        val next = block(draft.value)
        savedStateHandle[KEY_DRAFT] = next          // survives process death (Bundle-able)
        draftDao.upsert(next.toEntity())          // survives task removal (durable)
    }

    // Non-destructive Back: persist and step back.
    fun onBackPressed(): Boolean {
        if (savedStateHandle.get<Int>(KEY_STEP) ?: 0 > 0) {
            savedStateHandle[KEY_STEP] = savedStateHandle.get<Int>(KEY_STEP)!! - 1
            draftDao.upsert(draft.value.toEntity())
            return true                                  // handled → don't pop the Activity
        }
        return false                                     // let the Activity finish
    }

    private companion object { const val KEY_DRAFT = "draft"; const val KEY_STEP = "step" }
}

// In the Activity/Fragment:
onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
        if (!vm.onBackPressed()) {
            isEnabled = false
            onBackPressedDispatcher.onBackPressed()        // let the system pop
        }
    }
})
```

**Talking points / follow-ups**
- *"Why not just keep the form in the Activity's fields?"* → The Activity dies on process death; those fields are gone. Data outlives the view.
- *"How do you know if the user wants to discard?"* → Compare against the last persisted snapshot; if dirty, show a confirm. `isChangingConfigurations` tells you it is not an abandonment.
- *"How do you handle Back on the last step?"* → It should leave the flow (pop to the previous screen) — the draft is already persisted, so the user can resume.
- *"What about predictive back?"* → Use `OnBackPressedDispatcher`; a plain `onBackPressed()` override or `onKeyUp(KEYCODE_BACK)` will misbehave on Android 13/14.
- *"How would you test this?"* → Fill step 1, press Back, assert the draft row exists in Room and that re-entering restores step 1's values; then kill the process and assert the draft still restores.

---

### 6. A deep link opens a screen while the app is already running. How would you handle navigation and existing back-stack entries?

**What the interviewer is really asking**
Do you understand the **four** ways a deep link reaches your app (cold start, warm start with a live Activity, warm start with a stopped Activity, and a link handled by another app)? Can you make navigation **idempotent** so the same link never stacks two copies of the same screen, and do you know the difference between `handleDeepLink`, `launchSingleTop`, `popUpTo`, and `saveState`/`restoreState`?

**Key concepts**
- `androidx.navigation` `NavDeepLink`, `navDeepLink` DSL, `<nav-graph android:autoVerify="true">` + `intent-filter` with `BROWSABLE`
- `NavController.handleDeepLink(Intent)` vs `NavController.navigate(deepLink)`
- `launchSingleTop` — do not create a second instance of the destination if it is already on top
- `popUpTo` / `popUpToInclusive` — how much of the stack to clear
- `saveState` / `restoreState` — preserve/restore the back stack of a tab-like graph
- `TaskStackBuilder` + `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`
- `PendingIntent` with `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`
- `NavBackStackEntry` / `savedStateHandle` for arguments
- `android:launchMode="singleTask"` / `singleTop` and their effects on `onNewIntent`

**Approach**

1. **Separate link *routing* from link *delivery*.** The manifest declares the intent filter; the app declares which destination the link maps to. With `androidx.navigation`, declare the mapping on the destination:
   ```xml
   <navigation ...>
       <fragment android:id="@+id/orderDetail"
           android:name="com.app.OrderDetailFragment">
           <deepLink app:uri="https://shop.example.com/orders/{orderId}" />
       </fragment>
   </navigation>
   ```
   The `NavController` then parses the URI, matches the pattern, and produces a `NavDeepLink` with typed arguments.

2. **Handle all entry points in one place.** The `Activity` should funnel every incoming `Intent` through the same code path:
   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContentView { AppNavHost(intent = intent) }
   }
   override fun onNewIntent(intent: Intent) {
       super.onNewIntent(intent)
       setIntent(intent)                 // keep getIntent() current
       navController.handleDeepLink(intent)
   }
   ```
   The `ActivityResultRegistry`/Compose variant: `val navController = rememberNavController()` then `navController.handleDeepLink(intent)` in a `LaunchedEffect(intent)`.

3. **Make it idempotent — the key discipline.** A link tapped twice, or a link that arrives while the same destination is already displayed, must not push a duplicate. Options:
   - `navigate(deepLink) { launchSingleTop = true; popUpTo(graph.findStartDestination().id) { inclusive = false }; saveState = true; restoreState = true }`
   - `launchSingleTop` prevents a second instance when the destination is already on top of the stack.
   - `popUpTo(startDestination) { saveState = true }` + `restoreState = true` clears the intermediate stack while remembering each tab's state — the right pattern for "link opens a detail screen, Back should return to the home tab, not to a stale detail page".
   - `launchSingleTask` in the manifest means the system delivers the link to the existing Activity via `onNewIntent` instead of creating a second one. It also clears activities above the one with the same affinity — which can surprise users if it is used carelessly.

4. **Handle cold start properly.** On a cold start the `Intent` is in `onCreate()`'s intent; the `NavHost` must apply the deep link *after* the graph is set, otherwise `handleDeepLink` runs against an empty back stack and the start destination is not yet known. With Compose, `NavHost(navController, startDestination = ...)` then a `LaunchedEffect` that calls `handleDeepLink`.

5. **Distinguish "the app was already on that screen".** If the user is already viewing order 42 and taps a link to order 42, the desired behaviour is usually to *not* push a new screen — use `launchSingleTop` plus a check on the current destination. If the link points to a *different* order, push it.

6. **Handle links that arrive while the app is in the background.** The Activity may be recreated rather than receive `onNewIntent` (if the process was killed). Handle both: `onCreate` (fresh instance, `intent` carries the link) and `onNewIntent` (existing instance).

7. **Use `TaskStackBuilder` for links that should feel like they came from outside** (e.g. a notification that should open the app's task with a specific screen): build the parent chain so that Back from the deep-linked screen returns into the app's normal hierarchy rather than exiting the app.

8. **Validate the link.** Untrusted URIs can carry hostile arguments. Parse into typed arguments (`NavType.StringType` etc.), validate ids server-side, and never pass raw URIs to a WebView. `NavDeepLink` matching is already restrictive — it only navigates when the pattern matches, which is a safety property worth mentioning.

9. **Combine with `PendingIntent` flags** for notification-driven links (see Q7): `FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE`.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `navController.handleDeepLink(intent)` | One canonical path; typed args; validates the pattern | Must be called after the graph is set; a no-op if the pattern does not match (handle that) |
| `navigate(deepLink)` | Explicit | Can double-push if not guarded |
| `launchSingleTop = true` | Prevents duplicate destinations on top | Only guards the *top* destination; a link to a mid-stack destination still duplicates |
| `popUpTo(start) { saveState = true }` + `restoreState = true` | Clean, predictable stack; tab state preserved | Requires `saveState`/`restoreState` on every pop/push; subtle to reason about |
| `popUpTo(..., inclusive = true)` | Fully resets to a known stack | Destroys the user's back history — surprising UX for a notification tap |
| `android:launchMode="singleTask"` | Single Activity instance; `onNewIntent` delivery | Clears activities above the target; complicates multi-window; changes task semantics globally |
| `FLAG_ACTIVITY_CLEAR_TOP` | Reuses the existing Activity | Legacy API; with `singleTask` it is redundant |
| Manual `Intent` parsing + `startActivity` | Full control | Loses `NavDeepLink`'s typed argument parsing and pattern safety; more code |
| `TaskStackBuilder` | Sensible parent chain for external entry | More ceremony; Nav integrates its own way |

**Key detail:** `handleDeepLink` **does not** clear the existing back stack by default — it navigates. If the design requires a predictable stack, pair it with `popUpTo`/`saveState`/`restoreState` explicitly. This is the difference between "the link opened a screen" and "the link opened a screen and Back now does something sensible".

**Code example**

```kotlin
// Single entry point for every inbound Intent.
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        navController = (supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment).navController
        setContentView { AppNavHost(navController) }
        if (savedInstanceState == null) {
            navController.handleDeepLink(intent)     // cold start
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController.handleDeepLink(intent)         // warm start
    }
}

// Idempotent navigation options used for every external entry.
navController.navigate(deepLink) {
    launchSingleTop = true                                        // no duplicate on top
    popUpTo(navController.graph.findStartDestination().id) {
        saveState = true                                          // remember each tab's stack
    }
    restoreState = true                                          // restore it on the way back
}
```

**Talking points / follow-ups**
- *"What is the difference between `launchSingleTop` and `popUpTo`?"* → `launchSingleTop` prevents re-creating the destination if it is already on top; `popUpTo` removes entries. They solve different halves of "don't stack duplicates".
- *"What does `saveState`/`restoreState` do?"* → They let you clear a stack and later restore it when the user returns to that graph — used for bottom-nav tabs.
- *"How do you test deep links?"* → `adb shell am start -W -a android.intent.action.VIEW -d "https://shop.example.com/orders/42" com.example.shop`, plus `adb shell am start` while the app is in the foreground to exercise `onNewIntent`.
- *"Do I need `autoVerify`?"* → For App Links (verified https domains) yes — it enables seamless handling without the disambiguation dialog. Without it, the user may have to pick the app.
- *"How do you secure deep-link arguments?"* → Treat every argument as untrusted input; validate ids and never render raw HTML/JS from the URI.

---

### 7. An app receives a push notification while it is terminated. How would you navigate to the correct screen when the user taps it?

**What the interviewer is really asking**
The full **cold-start-from-notification** path: `PendingIntent` construction and mutability flags, the `BroadcastReceiver` vs `Activity` entry point, the notification channel/permission requirements, and — the part interviewers actually probe — **whether the navigation survives the process having been killed** (i.e. does the payload survive in the `PendingIntent`, and do you re-deliver it after the Activity is created?).

**Key concepts**
- `PendingIntent` with `FLAG_IMMUTABLE` (Android 12+ / API 31+ **required**) and `FLAG_UPDATE_CURRENT`
- `BroadcastReceiver` (`androidx.core:core` `NotificationCompat`, or Firebase `onMessageReceived` / `onNewToken`)
- Tapping the notification → `PendingIntent.getActivity` launches the Activity; `getBroadcast` goes through a receiver first
- `NotificationChannel` (API 26+) and `POST_NOTIFICATIONS` runtime permission (API 33+)
- Cold start: the Activity is created with the `Intent`; the `NavController` graph may not exist yet
- `intent.extras` must be `Bundle`-able and small
- `TaskStackBuilder` for a proper parent stack; `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`
- Firebase `onMessageReceived` runs in a background service (not a `BroadcastReceiver`) when in the foreground

**Approach**

1. **Push arrives while the app is terminated.** The payload is stored by the system (FCM persists it). When the user taps, the notification's `PendingIntent` fires and the system launches (or reuses) your Activity with the payload in the `Intent` extras.

2. **Build a correct `PendingIntent`.**
   ```kotlin
   val intent = Intent(this, DetailActivity::class.java).apply {
       putExtra(EXTRA_ORDER_ID, orderId)
       flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
   }
   val pendingIntent = PendingIntent.getActivity(
       this,
       requestCode,                     // distinct per destination so extras don't collide
       intent,
       PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
   )
   ```
   - `FLAG_IMMUTABLE` is **mandatory from API 31** — the system throws `IllegalArgumentException` without it on Android 12+.
   - `FLAG_UPDATE_CURRENT` replaces the extras of an existing `PendingIntent` with the same identity (the identity is (requestCode, intent filter equality, flags)). Use a stable request code per notification *type* and put the payload in the extras, so the extras are refreshed each time.
   - Use `FLAG_MUTABLE` only if a *system* component (e.g. an inline reply / `RemoteInput` notification) must fill in the reply — and then only for that specific `PendingIntent`.

3. **Route to the right destination on cold start.** The Activity is created with the `Intent`; you must navigate *after* the graph is set up:
   ```kotlin
   override fun onCreate(savedInstanceState: Bundle?) {
       super.onCreate(savedInstanceState)
       setContentView {
           val nav = rememberNavController()
           NavHost(nav, startDestination = Routes.HOME) {
               composable(Routes.ORDER) { OrderScreen(...) }
           }
           LaunchedEffect(Unit) { nav.handleDeepLink(intent) }   // cold-start routing
       }
   }
   ```
   With `NavDeepLink` URIs in the graph you can reuse the same machinery as Q6, which is the cleanest unification: convert the notification payload to a deep-link URI and let `handleDeepLink` route it.

4. **Handle the warm case too.** If the app is already running, the notification tap delivers `onNewIntent` (with `FLAG_ACTIVITY_SINGLE_TOP`/`singleTask`) — handle it in `onNewIntent` and call `setIntent(intent)`.

5. **Use a `BroadcastReceiver` only when you need a side effect before the UI.** For "the user tapped → mark the notification as read", a `BroadcastReceiver` with `PendingIntent.getBroadcast` can post a result or update a `ContentProvider`. But for *navigation*, `PendingIntent.getActivity` is simpler and more reliable. If you use a receiver, remember it must finish fast (`goAsync()` for a `PendingResult` with a 10-second budget) or you will get an ANR.

6. **Handle the notification permission and channel properly.** Create a `NotificationChannel` on app start (API 26+), request `POST_NOTIFICATIONS` on API 33+, and degrade gracefully when the user denies it — the app still works, but notifications are not delivered.

7. **Never trust the payload.** Validate the ids, check that the resource still exists and the user still has access, and handle a "not found" deep link gracefully (route to home with a message) rather than crashing.

8. **Handle the "notification arrives while the app is in the foreground" case** — FCM delivers it to `onMessageReceived` in a background service; you typically show a local notification or update the UI in place, and must avoid double-navigation when the user then taps it (see Section 9, Q4 in the notification section of this repo).

9. **Consider `setOnlyAlertOnce`, the notification id, and grouping.** A repeated notification for the same order should reuse a stable id so it replaces rather than stacks.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `PendingIntent.getActivity` | Direct; no receiver; survives process death; simplest | Navigation happens only when the Activity is created — no pre-UI side effects |
| `PendingIntent.getBroadcast` + receiver | Can do work before showing UI (mark read, sync) | Receiver has a ~10 s budget; `goAsync()` needed; more moving parts; a receiver bug can crash the app in the background |
| `FLAG_IMMUTABLE` | Required on API 31+; prevents a third party rewriting the extras | Cannot be used with inline reply, which needs `FLAG_MUTABLE` |
| `FLAG_UPDATE_CURRENT` | Extras always reflect the newest payload | The `PendingIntent` identity must be stable per destination, or you leak `PendingIntent`s |
| Deep-link URI in the notification | Reuses the navigation machinery; uniform with Q6 | Requires a `NavDeepLink` for every notification type; URI parsing vs typed extras |
| Typed extras in the `Intent` | Type-safe; no URI encoding issues | Every consumer must know the key names; no reuse of the deep-link graph |
| `TaskStackBuilder` parent chain | Back from the notification screen returns into the app | More ceremony; if the app was never opened, the parent is created empty |
| Handling it only in `onCreate` | Simple | Misses the warm case — the user taps and nothing happens |

**Key detail:** the payload survives because it is embedded in the `PendingIntent`'s `Intent` extras, which the **system** holds — not in your process. So a cold start from a notification works even though your process was killed. What does not survive is anything you only kept in memory.

**Code example**

```kotlin
object Notifier {

    private const val REQUEST_ORDER = 1001

    fun showOrderNotification(context: Context, orderId: String, title: String) {
        val channelId = "orders"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(channelId, "Orders", NotificationManager.IMPORTANCE_DEFAULT))
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            action = "android.intent.action.VIEW"
            data = Uri.parse("https://shop.example.com/orders/$orderId")   // reusable NavDeepLink
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_ORDER,                 // stable per destination
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,  // IMMUTABLE required on API 31+
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_order)
            .setContentTitle(title)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(orderId.hashCode(), notification)
    }
}
```

**Talking points / follow-ups**
- *"What happens if I omit `FLAG_IMMUTABLE`?"* → On API 31+ (Android 12) `PendingIntent.getActivity` throws `IllegalArgumentException` unless the Intent is explicitly immutable or mutable.
- *"How do you handle the app being killed between the tap and the Activity start?"* → Nothing special is needed — the payload is in the system-held `PendingIntent`. But anything else you were carrying in memory (e.g. a pending navigation stack) is gone; reconstruct from the payload.
- *"Do you need a `BroadcastReceiver` for Firebase?"* → Only if you need to handle the message when the app is in the background and the system shows the notification for you. For custom notifications, `onMessageReceived` handles the foreground case.
- *"How do you test a cold-start notification tap?"* → `adb shell am start -n com.example/.MainActivity` with the extras, or send an FCM message with the app force-stopped and tap it.
- *"What about the notification permission on API 33+?"* → Request `POST_NOTIFICATIONS` at a sensible point in the onboarding flow; a denial means no notifications — design an in-app alternative (inbox screen).

---

### 8. An Activity launches an external payment application and later receives a result. How would you handle process death during that flow?

**What the interviewer is really asking**
The **Activity Result API** was created precisely to make this flow safe. The interviewer wants to know that you use `ActivityResultLauncher`/`ActivityResultContracts` instead of `startActivityForResult`, that the result is delivered through the `ActivityResultRegistry` which is **parcelled and saved with the Activity's saved instance state** (so it survives process death), and that you understand the difference between "the Activity was recreated" and "the process was killed and relaunched from the launcher intent".

**Key concepts**
- `ActivityResultContracts.StartActivityForResult` / `StartActivityForResult` / `PickContact`
- `ActivityResultLauncher`, `registerForActivityResult` — must be called **before** the Activity is `STARTED`
- `ActivityResultRegistry` — the registry is saved/restored as part of the saved instance state
- `registerForActivityResult` in a Fragment vs an Activity — the Fragment's result is delivered to the Fragment's registry
- Process death while the external app is in the foreground → the system recreates the Activity with the result
- `onSaveInstanceState` + the launcher's `key` → the pending result is restored
- `setResult`/`RESULT_OK` + `Intent` extras from the external app
- `PendingIntent` for a callback-style payment (external app returns via a `PendingIntent` instead of `setResult`)
- `TaskStackBuilder` / `FLAG_ACTIVITY_CLEAR_TOP` for the return path

**Approach**

1. **Use the Activity Result API, never `startActivityForResult`.** `registerForActivityResult` must be invoked during initialization (before `onStart`), because the registry needs to be able to re-deliver a result that arrived while the process was dead. Registering in `onCreate` (property initialisers or `onCreate` before `super`/before `STARTED`) is the rule.

2. **Understand what actually happens on process death.**
   - You call `launcher.launch(intent)`; the system records the request with a unique request code and remembers the `Intent` that was sent.
   - The external app runs; the system kills your process (memory pressure) or the Activity is destroyed.
   - When the external app finishes with a result, the system recreates your Activity **with the result**, and `ActivityResultRegistry` delivers it to your callback — *including* the case where the process was killed and the Activity is recreated from scratch with the original `Intent` that started it.

3. **The launcher's callback must be idempotent and re-entrant.** It may fire on a *brand-new* Activity instance whose only prior state is the saved instance state. So:
   - Do not assume the callback is paired with a UI that is already on screen — you may need to navigate first, then apply the result.
   - Do not assume "the user is waiting for this" — the user may have abandoned the flow; check the current destination / business state before acting.

4. **Handle the "external app never returns" case.** The user may kill the external app, or the payment app may crash. Result: your Activity is recreated with **no result**. Detect "I launched a payment and came back with `RESULT_CANCELED` or no result" and treat it as *unknown*, not *failed* — reconcile with the payment provider using the idempotency key (Section 1, Q1/Q2).

5. **Persist the intent of the transaction before launching.** Write the pending transaction (with its idempotency key) to Room *before* `launcher.launch(...)`. This is what lets you recover after process death: on restart you see a `PENDING` row, query the provider, and resolve.

6. **Verify the result — do not trust it.** The `Intent` extras come from another app. Validate the status, the amount, and the transaction id against your own record; never treat an arbitrary extra as "payment succeeded". Some providers return a `PendingIntent` that is delivered to your app directly (Firebase-style callback), in which case the flow is: external app → your `PendingIntent` → your `BroadcastReceiver`/Activity — and you need the same idempotency machinery.

7. **Use `setResult` semantics correctly on your side** when *you* are the external app's callee: `setResult(RESULT_OK, Intent().putExtra(...))` and `finish()`.

8. **Use `TaskStackBuilder`/`FLAG_ACTIVITY_CLEAR_TOP`** so the return path is a single Activity with the right back stack — the user should not have to press Back three times through your splash/launch screens.

9. **Fragment-scoped results** are delivered to the Fragment that registered the launcher. Use `parentFragmentManager.setFragmentResult` / `setFragmentResultListener` to pass results between fragments in a well-defined way (with a `requestKey`), rather than holding a reference to another fragment.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `ActivityResultLauncher` | Survives process death; lifecycle-correct; the modern API | Must be registered before `STARTED`; the callback may fire on a fresh Activity instance |
| `startActivityForResult` / `onActivityResult` | Legacy; familiar | Deprecated since API 30; weaker process-death handling story; manual request-code bookkeeping |
| `PendingIntent` callback from the provider | Works even if your Activity is not alive; the provider calls back into your app | Needs a receiver or an Activity; `PendingIntent` flags and immutability rules apply; duplicate delivery must be handled |
| Trusting `RESULT_OK` | Simple | **Wrong** — the external app is untrusted; validate the payload and reconcile with your own backend |
| Reconciling with the provider after an unknown outcome | Correct | Requires an API on the provider's side and a durable pending record |
| Polling instead of waiting for a result | Works even if the external app never returns | Battery/latency cost; needs a background job |

**Key detail:** `ActivityResultRegistry` is part of the `ComponentActivity`'s saved state — the pending result (the request code and the result `Intent`) is saved in the `Bundle` and restored when the Activity is recreated, which is exactly why the modern API is process-death-safe in a way that ad-hoc `startActivityForResult` code often is not.

**Code example**

```kotlin
class PaymentActivity : AppCompatActivity() {

    private val payLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // May run on a BRAND-NEW Activity instance after process death.
        handlePaymentResult(result.resultCode, result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If we were recreated after process death while the external app was running,
        // the pending transaction is already in Room — reconcile instead of re-launching.
        lifecycleScope.launch {
            pendingTxDao.findPending()?.let { reconcile(it) }
        }
    }

    fun startPayment(tx: PendingTx) {
        lifecycleScope.launch {
            pendingTxDao.insert(tx)                       // durable BEFORE launching
            payLauncher.launch(paymentAppIntent(tx.idempotencyKey))
        }
    }

    private fun handlePaymentResult(code: Int, data: Intent?) {
        lifecycleScope.launch {
            val tx = pendingTxDao.findPending() ?: return@launch
            when (code) {
                Activity.RESULT_OK -> {
                    val providerId = data?.getStringExtra("transaction_id")
                    // Trust but verify: never trust an external app's "success".
                    val verified = paymentApi.verify(tx.idempotencyKey, providerId)
                    pendingTxDao.mark(tx.id, verified.status)
                }
                else -> {
                    // Cancelled or no result ⇒ UNKNOWN, not failed. Reconcile.
                    pendingTxDao.markUnknown(tx.id)
                    reconcile(tx)
                }
            }
        }
    }
}
```

**Talking points / follow-ups**
- *"What is the difference between the Activity being recreated and the process being killed?"* → Recreated: the `Bundle` and the `ActivityResultRegistry` state are restored. Killed: same mechanism, but *all* in-memory state is gone, so only the saved state and durable storage help.
- *"Can the result be delivered twice?"* → The registry delivers once per request code, but your handler must still be idempotent because a `reconcile()` can be triggered from more than one path (the callback *and* the on-create recovery sweep).
- *"What if the external app returns a `PendingIntent` instead of `setResult`?"* → Then you implement a receiver for that `PendingIntent`; treat the delivery as potentially duplicated and make the handler idempotent on the transaction id.
- *"How do you test process death during this flow?"* → Launch the external intent, then `adb shell am kill <package>` (or enable "Don't keep activities"), then trigger the result and assert the payment is resolved correctly.
- *"Why not just use `onActivityResult`?"* → Deprecated API; the modern API encapsulates the request-code bookkeeping and the saved-state plumbing.

---

### 9. A dialog or bottom sheet reappears unexpectedly after configuration changes. How would you manage its state?

**What the interviewer is really asking**
Do you understand the difference between **the system restoring your dialog** (because it is in the saved instance state) and **your code re-showing it** (which is the actual bug — the classic "dialog shown twice" / "bottom sheet reappears on every rotation" problem)? The staff answer is: make showing a dialog a **one-shot event driven by state**, and let the system restore the dialog's *contents* rather than re-triggering the *action*.

**Key concepts**
- `DialogFragment` / `BottomSheetDialogFragment` are restored by the `FragmentManager` after process death — that is correct behaviour
- The bug: calling `show()` from a `LaunchedEffect`/`lifecycleScope` that re-runs, or from a `LaunchedEffect(state)` keyed on a state that survives rotation
- `FragmentManager.isStateSaved` → calling `show()` after state is saved throws `IllegalStateException: Can not perform this action after onSaveInstanceState`
- `show()` vs `showNow()` — `show()` enqueues the transaction, `showNow()` commits immediately (only legal before state is saved)
- `DialogFragment.onSaveInstanceState` / `onDismiss` / `onCancel`
- `AlertDialog` (platform) vs `AlertDialog.Builder` — the platform dialog is *not* state-restorable; it is dismissed and must be re-created
- `setFragmentResult` / `setFragmentResultListener` for one-shot results
- `Lifecycle.State.CREATED` vs `STARTED` for deciding when to show
- `MaterialAlertDialogBuilder` / `BottomSheetDialogFragment` `onDismiss` semantics; `dismissAllowingStateLoss`

**Approach**

1. **Diagnose which of the two problems you have.**
   - *System restore* (expected): the dialog is in the `FragmentManager`'s saved state and the platform re-creates it. Its *contents* come back. This is what you want — do not fight it.
   - *Re-trigger* (the bug): your code calls `show()` again on every recreation, so the user sees the dialog again after rotating, or (worse) two copies stack.

2. **Make "show the dialog" a one-shot event, not a function of persistent state.** The classic fix: keep a *consumed* flag in the ViewModel/`SavedStateHandle`, so the action fires once:
   ```kotlin
   class MyViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
       fun shouldShowDialog(): Boolean = savedStateHandle.get<Boolean>(KEY_SHOWN) != true
       fun markShown() { savedStateHandle[KEY_SHOWN] = true }
   }
   ```
   With `rememberSaveable`, use `var shown by rememberSaveable { mutableStateOf(false) }` — it survives rotation, so it does not re-trigger, but it *does* reset if the process dies (which is the right semantic: a fresh launch may legitimately show it again).

3. **For a `DialogFragment`, use `show()` (not `showNow()`) and check `isStateSaved`.** A `DialogFragment` that is re-shown after the Activity has saved its state throws `IllegalStateException`. Use `childFragmentManager.isStateSaved` as the guard and defer with `FragmentLifecycleCallbacks` or `commitAllowingStateLoss` if you must show it late.

4. **For a Compose `AlertDialog`, use a state-driven dialog with a "consumed" pattern.** The idiomatic Compose solution is a one-shot event channel rather than a boolean state:
   ```kotlin
   // The screen renders a dialog only while the event is pending.
   LaunchedEffect(Unit) {
       vm.events.collect { event ->
           when (event) {
               is UiEvent.ShowConfirm -> { showConfirm = true }
               is UiEvent.Dismiss -> { showConfirm = false }
           }
       }
   }
   ```
   The key point: the *event* is emitted once (from a `Channel`/`MutableSharedFlow` with `replay = 0`), while the *dialog's open state* is a `rememberSaveable` boolean so rotation does not dismiss and re-open it.

5. **For a `BottomSheetDialogFragment`, the same rules apply, plus the sheet's own state.** Material's `BottomSheetDialogFragment` restores itself; the `BottomSheetBehavior` state (expanded/collapsed/hidden) is saved by the `BottomSheetDialog` when the `Dialog` has an `id` set on its `Window` — `BottomSheetDialogFragment` handles this, but a *custom* `Dialog` with a `BottomSheetBehavior` needs `behavior.state = ...` restored in `onStart`/`onDialogStarted`, otherwise the sheet jumps to the default state on rotation.

6. **For a plain platform `AlertDialog`, there is no automatic restore.** The system dismisses it when the Activity is destroyed and recreates nothing. You must re-create it yourself — and therefore you must guard against re-showing it twice. This is the most common source of "the dialog pops up again on rotation" bugs.

7. **Use `onDismiss`/`onCancel` for the result, not for the trigger.** `onDismiss` fires for every dismissal path (cancel, button, programmatic `dismiss()`, and system dismissal during a config change) — so do not treat it as "the user confirmed". Distinguish `onCancel` (user dismissed) from `onDismiss` (any dismissal), and deliver the *decision* through `setFragmentResult` so the result is delivered exactly once, even after process death.

8. **Deliver results via the `FragmentResult` API**, which is saved in the `FragmentManager` state and delivered when the listener's fragment is at least `STARTED`:
   ```kotlin
   // Producer
   parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf("confirmed" to true))
   // Consumer — delivered once, survives process death
   parentFragmentManager.setFragmentResultListener(REQUEST_KEY) { _, bundle -> ... }
   ```
   This is much more robust than a callback on a fragment reference.

9. **Dismiss in `onDestroyView` for an `AlertDialog` you own.** A `Dialog` holds a reference to the Activity's window; if you leak it, you leak the Activity. `onDestroyView { dialog?.dismiss() }` is the safe pattern.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Let the system restore the `DialogFragment` | Free; contents survive process death | You must not also call `show()` on recreation |
| One-shot event (`Channel`/`SharedFlow`) | Fires exactly once; the natural Compose pattern | Requires discipline: state events (e.g. "screen loaded") do not belong in a channel |
| Boolean state + `rememberSaveable` | Simple; the dialog stays open across rotation | Must be paired with a "consumed" flag to avoid re-triggering the *action* |
| Re-create a platform `AlertDialog` yourself | Full control | No automatic restore; easy to double-show; must dismiss in `onDestroyView` |
| `dismissAllowingStateLoss` | Avoids the "action after onSaveInstanceState" crash | Silently loses the dialog's state; the user may never see the dialog |
| Deliver the result via `FragmentResult` | Survives process death; delivered exactly once | Must be wired with matching request keys; the listener needs a `STARTED` fragment |
| `onDismiss` as the result channel | Simple | `onDismiss` also fires on config-change dismissal — it is not a user decision |

**Key detail:** the *state* of a dialog and the *trigger* to show it are different concerns. Persist the state (so it survives rotation), but make the trigger consumable (so it does not re-fire). That single distinction resolves most "dialog reappears" bugs.

**Code example**

```kotlin
// Compose: state survives rotation, the event fires exactly once.
class ScreenViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    val isDialogVisible = savedStateHandle.getStateFlow(KEY_DIALOG, false)

    fun onDeleteClicked() {
        if (savedStateHandle.get<Boolean>(KEY_TRIGGERED) == true) return   // already fired
        savedStateHandle[KEY_TRIGGERED] = true
        viewModelScope.launch { _events.send(UiEvent.ConfirmDelete) }
    }

    fun onDialogResult(confirmed: Boolean) {
        savedStateHandle[KEY_DIALOG] = false
        if (confirmed) viewModelScope.launch { repo.delete() }
    }

    private companion object { const val KEY_DIALOG = "dialogVisible"; const val KEY_TRIGGERED = "triggered" }
}

@Composable
fun Screen(vm: ScreenViewModel) {
    val visible by vm.isDialogVisible.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                is UiEvent.ConfirmDelete -> vm.showDialog()      // one-shot
                else -> Unit
            }
        }
    }

    if (visible) {
        AlertDialog(
            onDismissRequest = { vm.onDialogResult(false) },
            confirmButton = { TextButton(onClick = { vm.onDialogResult(true) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { vm.onDialogResult(false) }) { Text("Cancel") } },
            title = { Text("Delete this item?") },
        )
    }
}
```

**Talking points / follow-ups**
- *"Why did my bottom sheet reappear on rotation?"* → Because the trigger was keyed on state that survives rotation (`rememberSaveable`, or a ViewModel flag), so it re-fired; or because a `show()` call was re-executed on recreation. Fix the trigger, not the sheet.
- *"What is `isStateSaved` and why does it matter?"* → After `onSaveInstanceState`, the `FragmentManager` refuses transactions; `show()` then throws `IllegalStateException`. Guard with `if (!childFragmentManager.isStateSaved)`.
- *"Difference between `show()` and `showNow()`?"* → `show()` enqueues the transaction for the next execution; `showNow()` commits it immediately and can only be used before the state is saved.
- *"How do I deliver the result if the Fragment is not STARTED yet?"* → Use `setFragmentResult`/`setFragmentResultListener`; the result is stored and delivered when the listener's lifecycle reaches `STARTED`.
- *"Bottom sheet expanded vs collapsed after rotation?"* → `BottomSheetDialogFragment` saves the `BottomSheetBehavior` state; a custom `BottomSheet` must restore `behavior.state` in `onStart` (the `BottomSheetDialog` requires the `Window` to have an `id` set for the state to be saved).

---

### 10. How would you distinguish state that belongs in remember, rememberSaveable, ViewModel, SavedStateHandle, and persistent storage?

**What the interviewer is really asking**
This is the **taxonomy** question — the thing that determines whether an engineer's app survives real-world lifecycle events. The interviewer wants a *decision procedure*, not a list. A staff answer is a small decision tree driven by two questions: *how long must this live?* and *does it need to survive process death?*

**Key concepts**
- `remember` — composition lifetime; lost on any recomposition that drops the call, on rotation, on navigation
- `rememberSaveable` — survives configuration change and process death via the `Bundle`; `Bundle`-able types only; ~500 KB limit
- `ViewModel` — survives configuration change; scoped to a `ViewModelStore`; cleared on `onCleared()`; `viewModelScope`
- `SavedStateHandle` — ViewModel-owned; survives configuration change **and** process death; restored from the `Bundle`
- `SavedStateRegistry` / `SavedStateRegistryOwner` — the mechanism behind both `rememberSaveable` and `SavedStateHandle`
- Room / DataStore / files — durable; survives process death, task removal, and reboot
- `Parcelable` vs `Serializable` vs custom `Saver`
- `SavedStateRegistry` `consumeRestoredStateForKey` — consume-once semantics
- The `Bundle` limit → `TransactionTooLargeException`

**Approach**

1. **The decision procedure.** Ask three questions in order:
   - **Q1: Does it need to survive process death?** If yes → it must live in the `Bundle` (saved state) or in durable storage. If no → continue.
   - **Q2: Does it need to survive a configuration change?** If yes → `ViewModel` or saved state. If no → `remember`.
   - **Q3: How big is it, and does it need to be queryable / transactional?** If it is large, relational, or queried → durable storage (Room/DataStore).

2. **The canonical ladder (shortest lifetime → longest):**
   | State | Survives rotation | Survives process death | Survives task removal | Size limit | Use for |
   |---|---|---|---|---|---|
   | `remember` | No | No | No | none | Pure derived/transient UI values |
   | `rememberSaveable` | Yes | Yes (if state was saved) | No | Bundle (~500 KB) | Scroll offsets, selected tab, text drafts, small filters |
   | `ViewModel` | Yes | No | No | memory | Screen logic, in-flight work, expensive computation, `StateFlow` |
   | `SavedStateHandle` | Yes | Yes (if state was saved) | No | Bundle (~500 KB) | Ids and small state that must survive process death |
   | Room / DataStore / files | Yes | Yes | Yes | disk | Anything the user would be angry to lose |

3. **`remember` — the default for anything derivable.** If a value can be computed from other state, do not store it; derive it. `remember` is right for: a `remember { mutableStateOf(...) }` local toggle, an `Animatable`, a `derivedStateOf`, a one-time `LaunchedEffect` key object. **If you use `remember` for something that must survive rotation, it is a bug** — `rememberSaveable` is the one-word fix.

4. **`rememberSaveable` — small, primitive-ish, user-visible UI state.** The Compose implementation uses the `SavedStateRegistry` to write the value into the Activity's/Fragment's saved `Bundle` via a `Saver`. It accepts `Bundle`-able types or a custom `Saver`:
   ```kotlin
   val listState = rememberSaveable(saver = listSaver(
       save = { it.toList() },
       restore = { it.toMutableStateList() },
   )) { rememberLazyListState() }
   ```
   Restriction: the value must be `Bundle`-able (`Parcelable`, `Serializable`, primitives, `String`, `ArrayList` of those). A `List<MyModel>` where `MyModel` is not `Parcelable` will throw at save time.

5. **`ViewModel` — the screen's logic and its long-lived, non-persistent state.** The right home for: `StateFlow`s of UI state, in-flight request state, retry logic, debounce jobs, `viewModelScope` coroutines, and the *orchestration* of a screen. It survives rotation because the `ViewModelStore` is retained across configuration changes, and it is cleared when the Activity finishes (or the Fragment's `ViewModelStore` is cleared). Crucially: **`viewModelScope` is cancelled in `onCleared()`, so a `ViewModel` is *not* a place for work that must survive process death.**

6. **`SavedStateHandle` — the "survive process death" channel for small state.** It is the ViewModel-scoped equivalent of `rememberSaveable`: the ViewModel exposes it, and it is rehydrated from the `Bundle` when the process is restored. Use it for: the id of the entity being edited, the current step of a wizard, the scroll index, a pending deep-link target, a search query. **Constraint:** only `Bundle`-able values; and it survives process death **only if** the system saved the instance state before killing the process — always code the empty-handle fallback.

7. **Persistent storage — the only true source of truth.** Room for relational/structured data, DataStore for preferences/key-value, files for blobs. Anything the user would be upset to lose goes here: a draft, a cart, a completed form, a downloaded file, a pending transaction. This tier is the one that survives task removal, app restart, and (for files/Room) a reboot.

8. **The anti-pattern to call out: "state in the Activity/Fragment fields".** Activity fields die on rotation and on process death. If a value must outlive the view, it belongs in one of the tiers above. Similarly, `lateinit var` in the Activity that is filled in `onCreate` and read in `onStart` is a classic bug.

9. **A practical rule of thumb staff use:** *"If you had to explain to a colleague where a value lives, in one sentence, without saying 'the Activity' — good. If the sentence is 'it's a field in the Activity', you have a problem."* Another: *"ViewModel = what the screen is doing; SavedStateHandle = what the screen was doing when the phone ran out of memory; Room = what the user did."*

10. **Anti-pattern: putting everything in the ViewModel.** A `ViewModel` holding a large list of domain objects is memory pressure and is lost on process death anyway. Keep the ViewModel as a thin coordinator over a repository, with the repository owning the durable state.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `remember` | Simplest; zero ceremony | Lost on rotation; a common source of "my state resets" bugs |
| `rememberSaveable` | Survives rotation and process death; idiomatic Compose | `Bundle`-able types only; Bundle size limit; lost on task removal; does not survive an *unsaved* kill |
| `ViewModel` | Survives rotation; rich typed state; coroutine scope; survives navigation within a graph | **Does not survive process death**; holds memory; risk of holding an Activity/Context reference |
| `SavedStateHandle` | Survives process death; scoped to the screen; typed getters | `Bundle`-able values only; size limit; empty if the state was never saved; not queryable |
| Room / DataStore | Durable; queryable; survives everything | Heavier; needs migrations; I/O must be off the main thread; over-use for trivial values adds latency |
| `SharedPreferences` (legacy) | Trivial for tiny values | Synchronous `apply`/`commit` on the main thread; no atomicity or queries; `DataStore` is the modern replacement |
| Duplicating state in both ViewModel and Room | Fast reads | Two sources of truth → divergence bugs; pick one owner per fact |
| Saving everything to `onSaveInstanceState` | One mechanism | Bundle bloat → `TransactionTooLargeException`; slow `onSaveInstanceState` → jank/ANR |

**Key detail:** the `Bundle` is the *only* channel that survives process death without durable storage, and it is **size-capped** (the Binder transaction limit, roughly 500 KB–1 MB). The moment you start stuffing images, lists of entities, or blobs into it, you get `TransactionTooLargeException` on the *next* launch — a bug that is notoriously hard to reproduce locally.

**Code example**

```kotlin
// remember        → transient, derived UI value
val isExpanded by remember { mutableStateOf(false) }

// rememberSaveable → small user-visible state that survives rotation + process death
val selectedTab by rememberSaveable { mutableStateOf(0) }
val scrollIndex by rememberSaveable { mutableIntStateOf(0) }

// ViewModel       → screen logic, StateFlow, in-flight work (rotation-safe, not process-safe)
class ProfileViewModel(private val repo: ProfileRepository) : ViewModel() {
    val state: StateFlow<ProfileState> = repo.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileState.Loading)
}

// SavedStateHandle → small state that must survive PROCESS DEATH
class EditViewModel(private val savedStateHandle: SavedStateHandle, private val dao: DraftDao) : ViewModel() {
    val itemId: String? = savedStateHandle.get<String>("itemId")   // restored from the Bundle
    fun onNameChanged(name: String) {
        savedStateHandle["name"] = name          // Bundle-able primitive
        dao.saveDraft(name)                      // durable, survives task removal
    }
}
```

**Talking points / follow-ups**
- *"Does `SavedStateHandle` survive process death?"* → Yes, via the saved-instance-state `Bundle` — but only if the system saved the state before killing the process, and only for `Bundle`-able values.
- *"What is the practical Bundle limit?"* → The Binder transaction limit, on the order of 500 KB–1 MB across the whole transaction; exceeding it throws `TransactionTooLargeException`. Keep saved state to ids and small primitives.
- *"Where should the source of truth for a list live?"* → Room (or the server), with the ViewModel exposing a `StateFlow` view of it. Never in `rememberSaveable` — a list of entities will blow the Bundle limit.
- *"What about `android:configChanges` as a way to avoid all this?"* → It sidesteps the problem by leaking and by failing to cover all configuration changes. It is a tool for specific, well-understood cases (e.g. a camera preview), not a state-management strategy.
- *"How do I prove my state survives?"* → Test matrix: rotate (`ActivityScenario.recreate`), kill the process (`adb shell am kill`), remove the task from Recents, and reboot. Each tier has a different expectation.
- *"What is `SavedStateRegistry.consumeRestoredStateForKey` for?"* → Consume-once semantics — the value is returned once and then removed, which is how "show the dialog once after a restore" is implemented robustly.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **The lifetime ladder** | 1, 2, 3, 10 — `remember` → `rememberSaveable` → `ViewModel` → `SavedStateHandle` → Room/DataStore |
| **Process death vs configuration change** | 1, 2, 4, 5, 8, 10 — the distinction that separates a working app from a broken one |
| **Scope separation (observation vs execution)** | 3, 5, 7, 8 — cancel the collector, decide deliberately whether the work continues |
| **Idempotent, one-shot behaviour** | 4, 5, 6, 7, 9 — no duplicate requests, no duplicate screens, no duplicate dialogs |
| **Navigation as a state machine** | 6, 7, 8 — deep links, notification taps, external results all funnel through one path |
| **Durability & the unknown outcome** | 2, 5, 8 — persist intent before acting; reconcile "unknown", never "failed" |
| **Avoiding the anti-patterns** | 1, 4, 9, 10 — `configChanges`, Activity fields, `startActivityForResult`, boolean flags for events |

**The one sentence to memorise for this section:**
> *"Every piece of state has a lifetime, and the interviewer's job is to check whether you know which one. Configuration change keeps the ViewModel but not the view; process death keeps only the `Bundle` and your disk; and anything the user would be angry to lose belongs in Room, not in a field, a `remember`, or a `Bundle`."*
