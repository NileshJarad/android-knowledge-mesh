# Jetpack Compose and UI Architecture

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. A Compose screen recomposes whenever an unrelated state variable changes. How would you identify the cause?

**What the interviewer is really asking**
Do you actually understand the **recomposition model** — restartable/skippable groups, read tracking, and *why* a state change anywhere up the call tree can invalidate a whole subtree — or do you reach for `derivedStateOf` and `@Immutable` as incantations? The staff answer is a **diagnosis procedure**: measure first (Compose compiler metrics, Layout Inspector recomposition counts), then reason about *read boundaries*, not about "the whole screen recomposes".

**Key concepts**
- `@Composable` functions are **restartable** and **skippable** — the compiler inserts groups and remembers the parameters of each composable
- A composable is invalidated only when a **state it actually read** changes, *and* one of its parameters changed (unstable params make it non-skippable)
- **Read tracking**: `State<T>.value` reads are recorded against the enclosing restart scope; the read is *not* the whole function, it is the *group*
- `mutableStateOf` default policy is `structuralEqualityPolicy()` — a new-but-equal instance does **not** invalidate
- Unstable types (e.g. a `MutableList`, a plain `List` field not typed as immutable) make a composable **non-skippable** — it re-runs whenever its parent re-runs
- `derivedStateOf` — caches a computed value and only invalidates the read when the *derived* value actually changes
- `kotlinx.collections.immutable` (`ImmutableList`, `persistentListOf`) — makes collections stable for the compiler
- `@Stable` / `@Immutable` — promises to the compiler about a class's mutability
- Diagnostics: Compose compiler `CompilerMetrics` (via the `metrics` Gradle task / `composeCompiler { reportsDestination }`), Android Studio Compose **Performance** panel, Layout Inspector **Recomposition** counts, `trace {}` from `androidx.compose.runtime.trace`, `debugCompositionLocal`

**Approach**

1. **Measure before you theorise.** Turn on compiler reports for the module:
   ```kotlin
   composeCompiler {
       reportsDestination = layoutBuildDirectory("compose_compiler")
       metricsDestination = layoutBuildDirectory("compose_metrics")
   }
   ```
   The `metrics` destination gives you, per class, the counts of composable calls, `remember` calls, restartable groups and **skippable groups**. A class with `restartableGroups = 12, skippableGroups = 0` is your smoking gun — nothing in it can be skipped, so it re-executes wholesale whenever its parent does.

2. **Understand *why* an "unrelated" change matters.** Three causes account for nearly all cases:
   - **Read tracking**: the composable *did* read the state, just indirectly — e.g. it reads `state` and then passes `state.value` down, or it reads a `derivedStateOf` whose dependencies changed.
   - **Unstable parameters**: the parent passes a `List<Item>` (the `kotlin.collections.List` interface is *unstable* to the compiler unless it is `ImmutableList`). The child then cannot be skipped, so it re-executes.
   - **Scope read hoisting**: a `State` read inside a **lambda** passed to a child is attributed to the child's scope, not the parent's — but a read inside a lambda that is *created* in the parent and *invoked* later (e.g. in a `LaunchedEffect`, a `Modifier.clickable` handler, or a `derivedStateOf`) is still tracked to whichever scope invokes it.

3. **Use Layout Inspector to localise it.** Layout Inspector's **Recomposition** and **Composition** tabs show, per node, how many times it recomposed and how long it took. Find the *highest* node in the tree with an inflated count — that is where the invalidation starts. If the root recomposes but a leaf does not, the leaf is being skipped correctly and the work is upstream.

4. **Read the compiler's "unstable" report.** The Compose compiler emits a stability report (`stabilityConfigurationPath` / `StrongSkipping` reports). It lists every class inferred as unstable. Typical culprits: a domain model with a `MutableList` property, a `List` returned from a `StateFlow`, a lambda capturing a non-stable object, or a class you hand-rolled that has a `var`.

5. **Fix at the right layer.**
   - If the recomposition is genuinely needed but too coarse → split the composable so the changing part is in its own restartable group, and pass **only the changed values** down.
   - If the recomposition is *not* needed → make the read cheaper: `derivedStateOf` for a filtered/sorted list, `ImmutableList` for collections, `@Immutable`/`@Stable` for your model classes, or `remember` for expensive derived objects.
   - If the recomposition is caused by an unstable parameter → type the parameter as `ImmutableList<Item>` or `ImmutableSet`, or make the model `@Immutable`.

6. **Verify.** Re-run the metrics task and compare skippable-group counts before/after. A staff answer ends with a *measurement delta*, not a guess.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `derivedStateOf` for filtered/sorted data | Downstream recomposes only when the *result* changes | The dependency still invalidates the derived scope; over-using it adds an extra snapshot layer and hides real dependencies |
| `ImmutableList` / `persistentListOf` | Compiler sees a stable type → child becomes skippable | Extra allocation on every write; `kotlinx-collections-immutable` adds a dependency and a small runtime cost |
| `@Immutable` on a model | Cheapest fix, one annotation | A **lie** to the compiler if the class actually mutates → silent stale-UI bugs. Use only when genuinely immutable |
| Splitting into smaller composables | Fine-grained invalidation | More functions, more parameter plumbing; over-splitting hurts readability |
| `remember` for a derived object | Zero recomposition cost | Bites back: a `remember` with a stale key holds stale data; must key it on every input that matters |
| `referentialEqualityPolicy` | Avoids deep `equals()` on large objects | Invalidates on *any* new instance → **more** recompositions, not fewer. Use only for objects that are expensive to compare but mutated in place |
| Compose compiler metrics in CI | Makes regressions visible, reviewable | Report format churns between compiler versions; needs a baseline strategy |
| Debug-only `trace {}` markers | Precise attribution of expensive work | Must be removed or gated before release; adds noise |

**Key detail to mention:** *`remember` does not protect against recomposition of its containing scope's siblings* — it only preserves the value across recompositions of that scope. And *`remember` does not survive configuration change*; that needs `rememberSaveable`. Separately, the single most common cause of "unrelated state causes recomposition" is not a Compose bug at all — it is **a `State` read at a high scope** that the UI did not realise was a dependency.

**Code example**

```kotlin
// The list itself is stable AND the derived read only invalidates when the
// filtered result actually changes.
@Immutable
data class Row(val id: Long, val title: String, val unread: Boolean)

@Composable
fun Inbox(rows: ImmutableList<Row>, query: String) {
    // Derived once per query change, not per row mutation.
    val visible by remember(rows, query) {
        derivedStateOf {
            if (query.isBlank()) rows
            else rows.filter { it.title.contains(query, ignoreCase = true) }
        }
    }
    LazyColumn {
        items(visible, key = { it.id }) { RowItem(it) }
    }
}

@Composable
fun RowItem(row: Row) {                       // skips when `row` is unchanged
    // state read here is scoped to RowItem's group
    Text(row.title, modifier = Modifier.semantics { contentDescription = row.title })
}
```

**Talking points / follow-ups**
- *"Does the whole screen recompose when one `Text` changes?"* → No — Compose is scoped. The `RestartScope` for that `Text` invalidates; sibling groups with unchanged, stable parameters are *skipped*. But a parent that reads the state will re-run, and its children are only skipped if their parameters are unchanged and stable.
- *"What is the difference between `structuralEqualityPolicy` and `referentialEqualityPolicy`?"* → Structural compares with `equals()` (default, correct for value types); referential compares identity. For a `MutableList` field, `equals` is content-based and expensive, but `mutableStateOf` on a list *itself* still invalidates on identity.
- *"How do you debug this in a real app?"* → Layout Inspector → Recomposition counts; `trace("SectionName") { ... }` in `androidx.compose.runtime` to see timings; `CompilerMetrics` in CI for regressions.
- *"Is `derivedStateOf` a silver bullet?"* → No. It is only correct when the derivation is a *pure function of the dependencies* and you want to gate invalidation on the *output*. If the derivation is expensive, `remember(rows, query) { rows.filter {...} }` (with correct keys) is better because it does not re-run on every upstream recomposition.
- *"What about `Strong Skipping`?"* → The Compose compiler's strong skipping mode (enabled by default in recent compiler versions) allows skipping even when a parameter is unstable, by comparing instance identity of the *unstable* params instead of deep equality. It reduces recompositions but can mask genuinely mutated objects — mention it as a known change in behaviour.

---

### 2. A LaunchedEffect unexpectedly launches an API request multiple times. How would you fix it?

**What the interviewer is really asking**
Do you understand the exact **restart semantics of `LaunchedEffect`** — it restarts whenever *any* key changes, and the previous coroutine is cancelled and a new one launched — and can you reason about **which key is unstable**? The deeper skill is knowing that `LaunchedEffect(Unit)` is *not* "run once per screen lifetime"; it is "run once per **key set** per composition".

**Key concepts**
- `LaunchedEffect(key1, key2) { }` — the coroutine is launched when the composable enters composition and **restarted when any key changes**; the previous block is cancelled first
- Keys are compared with `equals` — a key that is a new-but-equal object is fine; a key that is a **new instance every recomposition** with reference semantics (e.g. a `List` interface, an unstable object) restarts the effect every time
- `LaunchedEffect(Unit)` is "once per composition instance", **not** once per ViewModel or once per app
- `remember { }` vs `remember(key) { }` — `remember` also restarts when the key changes, but does *not* cancel anything; use `remember` for values, `LaunchedEffect` for side effects
- `DisposableEffect(key) { onDispose { } }` — cleanup tied to the composition
- `SideEffect { }` — runs after every successful (re)composition; for publishing non-state side effects
- `rememberCoroutineScope()` — a scope cancelled when the composable leaves the composition
- `produceState(initialValue, key) { }` — a `LaunchedEffect` that writes into a `MutableState`
- `snapshotFlow { }` — emits when the observed snapshot state changes
- Restart causes: unstable key types, keys derived from `remember { }`-less computations, keys that are collections (`List` interface is unstable → but `equals` still used for key comparison; instability affects *skipping*, not key comparison — however a key that is a lambda or object with identity `equals` restarts constantly)
- `viewModelScope` vs composition scope: the effect dies with the composition, not the ViewModel

**Approach**

1. **Confirm it is really a restart, not a re-entry.** If the composable leaves and re-enters the composition (navigation, a conditional `if` flipping, an `AnimatedVisibility` hiding content), `LaunchedEffect` runs again *from scratch*. This is not a bug — it is the contract. Fix the *composition structure* if a re-entry is unintended.

2. **Find the unstable key.** The usual culprit is a key that is a freshly-allocated object on every recomposition:
   ```kotlin
   LaunchedEffect(filterState) { load() }   // filterState rebuilt each recomposition
   ```
   where `filterState` is a `data class` containing an unstable field, or a `List` of filters typed as `List<Filter>` (structural `equals` on the interface is fine, but if the field is a `MutableList` or a lambda, `equals` differs each time).

3. **Make the key minimal and stable.** Key on the **smallest primitive that identifies the work**: an id, a query string, a paging key. Do not key on the whole request object, and do not key on a lambda.

4. **Remember the derived value if it is genuinely needed.** Use `remember(key) { derived }` to compute a stable key from a changing input, and only depend on that.

5. **Guard against duplicate in-flight work** at the source of truth. Even with a correct key, the effect can restart (e.g. the key legitimately changes) while the previous request is still running. In the ViewModel, use `flatMapLatest` so the old request is cancelled, or use a `Mutex`/in-flight guard. Compose-level effects should trigger *intent*, not own the network call:
   ```kotlin
   LaunchedEffect(id) { viewModel.load(id) }   // ViewModel owns cancellation + dedupe
   ```

6. **Use `repeatOnLifecycle`/`collectAsStateWithLifecycle` for collection, not `LaunchedEffect`.** If the "API request" is actually a Flow collection inside `LaunchedEffect`, replacing it with `collectAsStateWithLifecycle` removes an entire class of restart bugs and gives correct backgrounding for free.

7. **Prefer `produceState` for "state that comes from an async source with a key".** It is a `LaunchedEffect` that writes into state and avoids the extra `mutableStateOf` + `LaunchedEffect` pairing.

8. **Verify.** Add a `Log.d` or a `trace` marker in the effect body and count invocations in Layout Inspector / logcat. Also check `LaunchedEffect`'s behaviour with `key` being a `SnapshotStateList` — reading a list as a key is a smell.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `LaunchedEffect(Unit)` | Simplest | Restarts on every re-entry into composition; **not** a "once per app" guarantee — a common misconception |
| `LaunchedEffect(stablePrimitiveKey)` | Restarts exactly when the work should change | Requires discipline to pick the right primitive; the key can become a hidden dependency |
| `LaunchedEffect` with an unstable object key | — | **The bug**: new instance per recomposition → effect restarts → repeated network calls |
| `LaunchedEffect` + ViewModel-owned request | Dedupe and cancellation live in the ViewModel | Indirection; the effect is a thin trigger |
| `collectAsStateWithLifecycle` instead of `LaunchedEffect` for flows | Correct lifecycle handling for free; no manual key | Only works for flows you can expose as state; cannot express imperative side effects |
| `produceState(initial, key)` | Concise "async → state with a key" | The value is read inside the effect; error handling is manual |
| `DisposableEffect` for cleanup | Deterministic teardown | Must pair with `LaunchedEffect`; easy to leak if misused |
| `rememberCoroutineScope` for one-shot work | Cancelled with the composition | Not tied to a key — use for user-triggered actions, not data loading |
| `viewModelScope` + `flatMapLatest` | Latest-wins, cancels superseded work | Does not survive process death; needs `repeatOnLifecycle` for UI observation |

**Key detail:** `LaunchedEffect` **cancels the previous coroutine and starts a new one** when a key changes — it does not "run again" on top of the old one. So a repeated API call is either (a) the previous call was cancelled *before* it reached the network, so you only ever *see* a fresh call each time, or (b) the cancellation happened after the request was sent and you are seeing duplicates. This distinction matters for idempotency (see Section 1).

**Code example**

```kotlin
// BEFORE — the key is a fresh instance every recomposition → effect restarts → repeat calls.
LaunchedEffect(filter.toString()) { viewModel.search(filter) }

// AFTER — key on a stable primitive; the ViewModel owns the request + latest-wins.
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val query by viewModel.query.collectAsStateWithLifecycle()

    // key is a String → equals-stable → restarts only when the query really changes.
    LaunchedEffect(query) {
        viewModel.search(query)
    }
    // ...
}

class SearchViewModel : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val results: StateFlow<SearchUiState> = _results

    fun search(q: String) {
        _query.value = q
        // flatMapLatest cancels the previous in-flight search → no duplicates.
        viewModelScope.launch {
            repository.search(q)
                .collect { _results.value = SearchUiState.Content(it) }
        }
    }
}
```

**Talking points / follow-ups**
- *"Is `LaunchedEffect(Unit)` guaranteed to run once?"* → Once **per composition instance**. Navigate away and back, and it runs again. If you want once per ViewModel, launch in the ViewModel's `init` instead.
- *"Why doesn't `LaunchedEffect` run again on every recomposition?"* → Because the compiler makes it restartable/skippable and the `Unit` key is unchanged; the effect's *group* is skipped when the composable is skipped.
- *"What's the difference between `LaunchedEffect` and `DisposableEffect`?"* → `LaunchedEffect` runs a suspending block keyed to the composition and is cancelled on key change/exit; `DisposableEffect` runs a non-suspending setup block and gives you `onDispose` for teardown. `LaunchedEffect` already provides cancellation, so `DisposableEffect` is for resources.
- *"What about `remember` vs `LaunchedEffect` for a one-time setup?"* → `remember` for pure value creation (no cancellation semantics, no key-restart-with-cancel); `LaunchedEffect` for suspending work. `remember { }` also runs again on re-entry into the composition, just like `LaunchedEffect`.
- *"How would you detect this in production?"* → Log a counter in the effect body and alert on unexpected call volume; the same signal as a duplicated-network-call metric.

---

### 3. A LazyColumn loses the user's scroll position after a data refresh. How would you preserve it?

**What the interviewer is really asking**
Do you understand *why* the position is lost (item **identity** changed, the list was **re-keyed**, the item count shrank, or the state itself was recreated) and do you know the difference between a `remember`ed `LazyListState` and a **`rememberSaveable`**-backed one? The staff answer also distinguishes *preserving the anchor item* from *preserving the raw pixel offset*.

**Key concepts**
- `rememberLazyListState()` — `LazyListState` remembered in the composition; **lost on configuration change**
- `rememberSaveable(saver = LazyListState.Saver) { LazyListState() }` — survives configuration change **and** process death (saved instance state)
- `key = { it.id }` in `LazyColumn` items — **identity** for the lazy layout; without stable keys, Compose matches items by index and reuses slots incorrectly
- `LazyListState.firstVisibleItemIndex` / `firstVisibleItemScrollOffset` / `layoutInfo` — the anchor
- Re-keying items (e.g. keying by `index`, or changing the key function) forces the lazy layout to rebuild → position lost
- `animateScrollToItem(index)` / `scrollToItem(index)` — programmatic scroll, requires a running `LazyListState` and a laid-out list
- `rememberCoroutineScope()` — scope for programmatic scrolls
- `LaunchedEffect(listState)` + `snapshotFlow` — react to the first visible item to persist it
- `SaveableStateHolder` — saves state for items in a lazy list so off-screen item state survives
- `Paging Compose` — `LazyPagingItems` maintains its own scroll anchoring across appends

**Approach**

1. **Diagnose the cause before adding a fix.** In order of likelihood:
   - **Item count shrank** below the current `firstVisibleItemIndex` → the list clamps the scroll to the new end. Fix with a different anchor strategy, not by restoring the index.
   - **Keys changed or are index-based.** `key = { it.id }` is mandatory. If you key by a value that changes on refresh (e.g. `updatedAt`), Compose throws away and rebuilds the item → scroll resets.
   - **The `LazyListState` itself was recreated.** If the list lives inside a `key(...)` block, a conditional, or a `NavHost` destination that is recreated, `rememberLazyListState()` gives a brand-new state.
   - **The screen was recreated** (config change / process death) — `remember`ed state is lost; only `rememberSaveable` survives.

2. **Make the state saveable.** This is the single highest-value change:
   ```kotlin
   val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
   ```
   `LazyListState.Saver` persists `firstVisibleItemIndex` and `firstVisibleItemScrollOffset`. Note: `rememberSaveable` survives configuration change and process death, but **not** a fresh launch.

3. **Give every item a stable key.** `key = { it.id }`. If your domain has no id, add one on the server or derive a stable composite key. Never key by index.

4. **Handle the "list shrank" case by anchoring on an item, not an index.** Capture the anchor *before* the refresh and scroll back to it after the new data lands:
   ```kotlin
   val anchor = remember { mutableStateOf<Pair<Int, Int>?>(null) }
   ```
   Better: keep the top visible item's **id** in state, and after the refresh, find its new index and scroll to it with `animateScrollToItem(newIndex, offset)`. This survives insertions above the anchor — a strictly better behaviour than restoring a raw index.

5. **Do not fight the refresh with a `LaunchedEffect` that scrolls on every data change.** That causes the "list jumps to top" bug. Only scroll when the anchor is missing or its index changed meaningfully.

6. **For paging, let Paging do it.** `LazyPagingItems` keeps the anchor across appends and refreshes by design; you generally should not manage the scroll position yourself there.

7. **Consider `SaveableStateHolder` for per-item state** (text fields inside list items) — otherwise a recycled item loses its internal `remember` state when scrolled off-screen and back.

8. **Verify with a scenario test**: scroll to item 40, trigger a refresh that inserts 5 items at the top, assert the same item is still at the top. That is the exact regression that bites users.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `rememberLazyListState()` | Simple, one line | Lost on configuration change and process death — the classic "scroll resets on rotation" bug |
| `rememberSaveable(saver = LazyListState.Saver)` | Survives rotation and process death | Must be applied consistently; saved-state bundle size limits (transaction too large) if abused |
| Restore by raw index | Easy | Wrong when items are inserted/removed above the anchor; index shifts silently |
| Restore by anchor item **id** | Correct under insertions/deletions above the anchor | Needs a stable id and an index lookup after refresh; fails if the anchor item was deleted |
| Stable `key = { it.id }` in `items` | Correct item identity; enables slot reuse and per-item state | Requires unique, stable ids; changing the key function resets the list |
| `SaveableStateHolder` | Preserves per-item state in lazy lists | Extra API surface; `key` must match the item key |
| `animateScrollToItem` after refresh | Smooth, visible correction | Can look janky if fired on every data change; must be guarded |
| Let Paging manage anchoring | Handles append/prepend/refresh correctly | Only applies to paged lists; less control |
| Disable item reordering animation | Avoids visual jumps | Hides real data changes |

**Key detail:** The distinction that impresses interviewers: **`remember` survives recomposition but not configuration change; `rememberSaveable` survives configuration change and process death but not a cold start.** And the *more correct* restoration is **anchor-by-id**, not index — restoring an index after an insert above the anchor silently shows the user the wrong content.

**Code example**

```kotlin
@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val items by viewModel.items.collectAsStateWithLifecycle()

    // Survives configuration change and process death.
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val scope = rememberCoroutineScope()

    // Anchor by stable id, not by index.
    var anchorId by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(state = listState) {
        items(items, key = { it.id }) { item -> FeedRow(item) }
    }

    // Persist the anchor whenever the visible item changes.
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                anchorId = items.getOrNull(index)?.id
            }
    }

    // After a refresh that changed the item set, restore by id (only if needed).
    LaunchedEffect(items.size) {
        val anchor = anchorId ?: return@LaunchedEffect
        val newIndex = items.indexOfFirst { it.id == anchor }
        if (newIndex >= 0 && newIndex != listState.firstVisibleItemIndex) {
            scope.launch { listState.scrollToItem(newIndex) }
        }
    }
}
```

**Talking points / follow-ups**
- *"Why does the position jump even with `rememberLazyListState`?"* → Either the item count dropped (clamping), the keys changed (slot rebuild), or the composable was removed from and re-added to the composition (new state object).
- *"What happens if the item the user was looking at is deleted?"* → Fall back to the nearest surviving index (e.g. `min(oldIndex, lastIndex)`) and keep the offset — do not jump to the top.
- *"Does `LazyColumn` reuse item composition?"* → Yes; without stable keys, Compose reuses the slot for a *different* item, which produces the classic "content flickers/wrong row shows" bug.
- *"How do you handle this with `LazyPagingItems`?"* → Paging maintains its own anchoring on append/refresh; you should not call `scrollToItem` on refresh, only when the user explicitly requests it (e.g. "scroll to top" after a new post).
- *"What is `SaveableStateHolder` for?"* → Preserving `remember` state (e.g. an expanded card, a focused text field) for items scrolled out of a lazy list.
- *"Any pitfall with `rememberSaveable`?"* → Saved state is written to a `Bundle`; large lists of state will trigger `TransactionTooLargeException`. Save only the scroll anchor, not the data.

---

### 4. A composable collects a Flow, but UI updates stop when the app enters the background. How would you investigate it?

**What the interviewer is really asking**
This is a **lifecycle-scoping** question with a classic correct answer (`collectAsState` vs `collectAsStateWithLifecycle`) plus the deeper question: *is stopping the updates correct behaviour, and did the collection actually stop, or did the upstream flow stop emitting?* The staff answer separates **collection** from **production**.

**Key concepts**
- `collectAsState()` — collects for the whole lifetime of the composition; keeps collecting (and doing work) while backgrounded
- `collectAsStateWithLifecycle()` — `androidx.lifecycle-runtime-compose`; wraps the flow in `repeatOnLifecycle(Lifecycle.State.STARTED)`; **cancels collection when the lifecycle drops below STARTED** and restarts it when it returns to STARTED
- `repeatOnLifecycle` — suspends until the lifecycle reaches the state, runs the block, and **cancels the block** when the lifecycle falls below it
- `Lifecycle.State`: `CREATED → STARTED → RESUMED`; backgrounded app is `STARTED` at most (not `RESUMED`)
- `lifecycleScope` — cancelled at `DESTROY`; `viewModelScope` — cancelled when the ViewModel is cleared
- `repeatOnLifecycle` vs `Lifecycle.repeatOnLifecycle` (Kotlin Flow's `flowWithLifecycle`)
- `SharingStarted.WhileSubscribed(stopTimeoutMillis)` — the upstream stops when there are no subscribers, after a timeout
- `stateIn` / `shareIn` — `Eagerly`, `Lazily`, `WhileSubscribed`
- `Dispatchers.Main.immediate` — `collectAsState` collects on the main dispatcher
- Upstream producers: `flow { }` in a repository, `callbackFlow`, a `Flow` that only emits on a live subscription (e.g. a WebSocket or a `LiveData` observer)

**Approach**

1. **State the expected behaviour first.** If the app is backgrounded, the lifecycle falls to `STARTED` (or `CREATED`), and `collectAsStateWithLifecycle` **cancels the collection**. The UI freezing is *by design*, not a bug — but it becomes a bug if the collection **never resumes**, or if the upstream flow is *killed* rather than paused.

2. **Identify which collector is in use.**
   - `collectAsState()` → collection continues in the background (wasteful, but updates would still arrive when you return).
   - `collectAsStateWithLifecycle()` → collection stops below `STARTED` and restarts at `STARTED`.
   - `LaunchedEffect { flow.collect { ... } }` → **keeps collecting** while backgrounded (until the composable leaves composition). This is the most common cause of "it *should* have stopped but it didn't" — and, conversely, of "updates stopped" if the effect was cancelled by something else.

3. **Check the upstream.** If the flow is produced with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`, then when the collector goes away the upstream stops after the timeout. On resume, the flow **re-subscribes** and re-emits its current value — so the UI should catch up. If it does not:
   - The `StateFlow`'s value was **overwritten** by a stale value while unsubscribed (a race in the producer).
   - The producer is **one-shot** (a `channelFlow`/`callbackFlow` that only emits on the *first* subscription) — re-subscribing yields nothing.
   - The upstream is a `LiveData`-style **observer-based** source that lost its observer while backgrounded.

4. **Check for a lifecycle-scoping mistake in the producer.** If the repository collects a flow inside `lifecycleScope` (the *Activity's* scope) rather than `viewModelScope` or `repeatOnLifecycle`, the collection is tied to the wrong owner. If a `Flow` is collected in a `LaunchedEffect` inside the Activity's `setContent`, it survives rotation incorrectly (re-collected) or dies with the Activity.

5. **Check `SharingStarted`.** `WhileSubscribed(5_000)` with a *hot* upstream (a `MutableStateFlow` in the repository) is fine. But `WhileSubscribed(0)` with an upstream that does expensive work (network) means every background/foreground flip re-triggers the work — a battery problem. And `Eagerly` means the work continues forever.

6. **Instrument.** Log with `repeatOnLifecycle` callbacks: log on `STARTED` (subscribed) and on `STOP` (cancelled). If you see "subscribed" without a matching "cancelled", the block is not being cancelled properly (e.g. it is inside a `NonCancellable` block or a `GlobalScope`).

7. **Check for a leak of the other kind**: if the collection continues while backgrounded, you are burning CPU/battery and possibly keeping a socket open. The fix is `collectAsStateWithLifecycle` — and then ensure the upstream supports re-subscription.

8. **Reproduce with `adb`:** `adb shell input keyevent KEYCODE_HOME`, then `adb shell am start -n <pkg>/<activity>` to resume. Watch logcat for the subscribe/cancel markers.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `collectAsState()` | Simplest; keeps receiving data in background | Wastes CPU/battery; keeps sockets alive; can deliver updates to a screen nobody sees; Android may kill the process |
| `collectAsStateWithLifecycle()` | Stops work below STARTED; resumes automatically; the default recommendation | The upstream must support re-subscription; state can be momentarily stale on resume; a `WhileSubscribed(0)` upstream restarts work on every resume |
| `LaunchedEffect { flow.collect() }` | Full control; can use `repeatOnLifecycle` inside | Easy to get wrong; not lifecycle-aware by default; duplicates `collectAsStateWithLifecycle` |
| `repeatOnLifecycle(STARTED)` inside `LaunchedEffect` | Explicit; works for non-state side effects (e.g. submitting, animations) | Same re-subscription caveats |
| `SharingStarted.WhileSubscribed(5_000)` | Upstream survives brief subscriber gaps (e.g. rotation) without restarting work | Holds resources for the timeout; upstream work continues briefly after the last subscriber leaves |
| `SharingStarted.Eagerly` | Value always available; instant UI on resume | Never stops; wasteful for network-backed flows |
| `viewModelScope` for the producer | Survives rotation; the ViewModel owns the lifetime | Cleared only on `onCleared`; does not stop when backgrounded — pair with `WhileSubscribed` |
| `Dispatchers.IO` for the producer | Off-main-thread work | Not a lifecycle fix; work still runs in the background unless the scope is tied to the lifecycle |

**Key detail:** `collectAsStateWithLifecycle` uses `repeatOnLifecycle(Lifecycle.State.STARTED)`, **not** `RESUMED`. That means the collection continues while the app is visible but another activity is on top (e.g. a transparent dialog activity, or the app is partially visible in multi-window), and stops only when the app is fully backgrounded. Choosing `RESUMED` instead is a legitimate choice when the UI should not update behind a system dialog, but it causes more restarts.

**Code example**

```kotlin
// ✅ Lifecycle-aware: stops collecting below STARTED, restarts on resume.
@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // ...
}

// ViewModel: upstream work is tied to subscriber presence, not to the Activity.
class FeedViewModel(repo: FeedRepository) : ViewModel() {
    val state: StateFlow<FeedUiState> = repo.feed()
        .map { FeedUiState.Content(it) as FeedUiState }
        .catch { emit(FeedUiState.Error(it)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = FeedUiState.Loading,
        )
}

// ❌ Common bug: collection continues while backgrounded.
@Composable
fun Bad() {
    LaunchedEffect(Unit) {
        viewModel.flow.collect { /* ... */ }   // never cancelled on STOP
    }
}

// ✅ Explicit lifecycle control when you need a side effect, not state.
@Composable
fun SyncingEffect(viewModel: FeedViewModel) {
    LaunchedEffect(Unit) {
        viewModel.lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sync()      // runs only while visible; cancelled on STOP
        }
    }
}
```

**Talking points / follow-ups**
- *"What is the difference between `collectAsState` and `collectAsStateWithLifecycle`?"* → The latter suspends collection below `STARTED` and restarts it on resume; the former collects for the entire composition lifetime, including in the background.
- *"Does `collectAsStateWithLifecycle` miss emissions while stopped?"* → For a `StateFlow`, no — it replays the latest value on re-subscription. For a *cold* or *one-shot* flow, the emissions during the gap are genuinely lost, which is why one-shot effects should not be modelled as a collected state flow.
- *"What does `WhileSubscribed(5_000)` buy us?"* → It keeps the upstream alive across a rotation (the collector detaches and re-attaches within milliseconds), avoiding a redundant network fetch.
- *"Why did my UI show stale data on resume?"* → Because the producer kept running and overwrote the state while the UI was not observing, or because a cold flow re-ran and produced a *newer* value that was then lost. Log the producer's emissions with timestamps.
- *"How do you test this?"* → Use `TestLifecycleOwner` / `LifecycleRegistry` in a Compose test, move the lifecycle to `CREATED`, assert the fake upstream's subscription count dropped to zero, then move back to `RESUMED` and assert it re-subscribed.

---

### 5. A one-time navigation event is emitted but never received by the UI. How would you design event handling?

**What the interviewer is really asking**
This is the classic **"event can be missed"** problem in an event-driven UI. The interviewer wants to know whether you understand why a plain `StateFlow<UiEvent>` is a *state* (replayed to every new collector, replayed on rotation) and not an *event*, and whether you can design a **one-shot, exactly-once-ish, lifecycle-safe** channel — plus acknowledge the hard parts (rotation, process death, multi-consumer).

**Key concepts**
- **State vs event**: state is *current truth* (replayable, idempotent); an event is a *past occurrence* (must be consumed once)
- `SharedFlow(replay = 0)` — **drops** events emitted while there are no subscribers; with `extraBufferCapacity = 0` and `BufferOverflow.SUSPEND`, the *emitter suspends* until a collector arrives
- `Channel(Channel.BUFFERED)` + `receiveAsFlow()` — buffers events; **exactly-once delivery to one collector**; `receiveAsFlow()` is a cold flow that competes for elements (fan-out semantics are per-collector: each element goes to exactly one collector)
- `Channel.receiveAsFlow()` vs `consumeAsFlow()` — the former allows multiple collectors but each element goes to one of them; the latter is for a single consumer
- `Channel.CONFLATED` — keeps only the latest event (good for "scroll to top", bad for "show toast")
- `Channel.BUFFERED` (default capacity 64) — `send` suspends when full
- `trySend` — fails if the buffer is full / the channel is closed
- `Channel.isClosedForSend` / `close()`
- The problem with `SharedFlow(replay = 0)` and **no subscriber at emission time**: the event is lost. This is exactly the reported bug.
- `LaunchedEffect(Unit) { viewModel.events.collect { ... } }` — the collector must be **subscribed before** the event is emitted; a race exists if the event is emitted before composition.
- `repeatOnLifecycle` inside the collector to avoid handling events while backgrounded
- `savedStateHandle` / `SavedStateHandle.getStateFlow` for events that must survive process death (rare, and only for *state*, not events)
- Android's official guidance (2024): **prefer modelling everything as state**; for genuine one-shot effects use a `Channel`/`SharedFlow`, and never rely on a `StateFlow` of events

**Approach**

1. **Classify the event.** Most "events" are actually **state** and should be modelled as such:
   - "Show snackbar" → could be a `message: String?` in the UI state; the UI clears it after showing. Replayable, survives rotation, no loss.
   - "Navigate to detail" → could be a **navigation state** (`navTarget: NavTarget?`); the UI navigates and clears it. Rotation-safe.
   - "Open the camera" → a genuine one-shot **effect**; a side effect that must happen exactly once.

2. **For state-like events, use state, not events.** This is the modern recommendation and eliminates the whole class of lost-event bugs:
   ```kotlin
   data class FeedUiState(
       val items: List<Item> = emptyList(),
       val snackbar: UiText? = null,
       val navigateTo: NavTarget? = null,
   )
   ```
   The UI consumes and *clears* the value (`onSnackbarShown()`), so the "already handled" problem disappears and rotation is handled by the ViewModel surviving it.

3. **For genuine one-shot effects, use a `Channel` and consume it in the composition.** `Channel(Channel.BUFFERED)` + `receiveAsFlow()` gives at-most-once semantics to a single collector and buffers events emitted before the collector arrives:
   ```kotlin
   private val _effects = Channel<UiEffect>(Channel.BUFFERED)
   val effects = _effects.receiveAsFlow()
   ```
   `send` suspends if the buffer is full (backpressure) and throws if the channel is closed. Because the buffer holds events emitted before the collector subscribes, **the "emitted before the UI was ready" case is fixed** — this is the key difference from `SharedFlow(replay = 0)`.

4. **Collect with `repeatOnLifecycle(STARTED)`** so events are not handled while the screen is backgrounded — but note the channel *buffers* them, so nothing is lost, they are just delivered later. If the event should be dropped when backgrounded (e.g. an animation), use a `SharedFlow(replay = 0, extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST)` and accept the loss.

5. **Know the failure modes of each primitive and pick explicitly:**
   - `SharedFlow(replay = 0)` — **loses** events emitted with no subscriber. Use it for *broadcast* events (e.g. "network lost" banner) where every active collector should see them and missing one is acceptable.
   - `SharedFlow(replay = 1)` — the last event is re-delivered to every new collector → a snackbar reappears on rotation. Wrong for one-shot events, right for "latest state".
   - `Channel(BUFFERED).receiveAsFlow()` — buffered, delivered once to one collector. The default choice for one-shot UI effects.
   - `Channel.CONFLATED` — only the latest survives. Right for "scroll to top" / "refresh"; wrong for a queue of toasts.

6. **Close the channel in `onCleared()`** so late `send`s fail loudly instead of leaking:
   ```kotlin
   override fun onCleared() { _effects.close() }
   ```
   And use `trySend` from `viewModelScope` (non-suspending) or `send` from a coroutine.

7. **Handle navigation events specially.** Navigation must be idempotent: if the event is redelivered after process death, you navigate twice. Options: model navigation as **state** (`navTarget`), or make the navigation effect **idempotent** by checking `navController.currentDestination` before navigating, or use the newer `NavHost` with a `savedStateHandle`-based approach.

8. **Test it.** Compose test: emit an effect *before* the screen composes → assert it is handled. Rotate → assert it is not handled twice. This is where most "one-time event" implementations break.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `StateFlow<UiEvent>` | Trivial to write; replayable | **Wrong semantics**: the event re-fires on every new collector (rotation, re-entry) — "navigate twice", "toast twice" |
| `MutableStateFlow<T?>` for "pending event" | Simple; survives rotation; cleared explicitly | Requires the UI to clear it; a forgotten `clear()` causes an infinite loop of effects |
| `SharedFlow(replay = 0)` | Broadcast to all collectors | **Loses events** when there is no subscriber — the reported bug; drops on buffer overflow unless configured |
| `SharedFlow(replay = 1)` | New collectors get the latest | Re-delivers → duplicate side effects after rotation |
| `Channel(BUFFERED).receiveAsFlow()` | Buffered → survives "emitted before the collector was ready"; exactly-once to one collector | Single-consumer semantics; `send` suspends when full; must be closed |
| `Channel.CONFLATED` | Never blocks the emitter; latest-wins | Older events are dropped silently |
| Model as **state** instead | Best: no loss, rotation-safe, testable | Requires discipline; some genuine side effects (opening a camera) do not fit |
| `repeatOnLifecycle(STARTED)` around the collector | No effects handled in the background | Effects are buffered and delivered late, which may be wrong for animations |
| Store the pending event in `SavedStateHandle` | Survives process death | Only one event; ordering is not preserved; not really "one-shot" semantics |

**Key detail:** `Channel(BUFFERED).receiveAsFlow()` fixes the "emitted before the UI subscribed" case **because the channel buffers**, while `SharedFlow(replay = 0)` loses it **because there is no buffer and no replay**. This single distinction explains the bug in the question. And the honest caveat: *exactly-once delivery across process death is impossible* — if the process dies after the side effect but before the state is persisted, the effect will re-run. Design effects to be idempotent, or accept at-most-once.

**Code example**

```kotlin
// ViewModel: one-shot effects through a Channel; state stays in the StateFlow.
class FeedViewModel(repo: FeedRepository) : ViewModel() {

    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    // Buffered → events emitted before the UI subscribes are NOT lost.
    private val _effects = Channel<FeedEffect>(Channel.BUFFERED)
    val effects: Flow<FeedEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            repo.feed().catch { _effects.send(FeedEffect.ShowError) }.collect { items ->
                _state.update { it.copy(items = items) }
            }
        }
    }

    fun onItemClicked(id: Long) {
        viewModelScope.launch { _effects.send(FeedEffect.NavigateToDetail(id)) }
    }

    override fun onCleared() { _effects.close() }
}

// UI: consume effects exactly once while STARTED.
@Composable
fun FeedScreen(viewModel: FeedViewModel) {
    val navController = rememberNavController()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is FeedEffect.NavigateToDetail ->
                    navController.navigate("detail/${effect.id}")   // idempotent-guard below
                is FeedEffect.ShowError -> { /* snackbar */ }
            }
        }
    }
    // ...
}

// Idempotency guard so a redelivered event cannot navigate twice.
private fun NavHostController.navigateOnce(route: String) {
    if (currentDestination?.route == route) return
    navigate(route)
}
```

**Talking points / follow-ups**
- *"Why is a `StateFlow` of events wrong?"* → Because a `StateFlow` always has a current value; a new collector receives it immediately, so the effect re-runs on rotation. That is state semantics, not event semantics.
- *"What if two collectors subscribe to the same `receiveAsFlow()`?"* → The elements are distributed — each event goes to exactly one collector. For a genuinely single-consumer effect, use `consumeAsFlow()` so the exclusivity is explicit.
- *"How do you guarantee exactly-once?"* → You cannot, across process death. Guarantee **at-most-once per process** with a Channel and make the side effect idempotent (navigate guard, unique snackbar id).
- *"How do you handle an event emitted while the app is backgrounded?"* → With a Channel it is buffered and delivered on resume (often wrong for a "navigate" event — it may surprise the user); with `SharedFlow(replay=0)` it is dropped. Choose per event type.
- *"What does Google's 2024 guidance say?"* → Model the UI as a stream of state; use `Channel` for genuinely one-off effects; avoid "event" as a `StateFlow` field. Mention this to show you follow current guidance.

---

### 6. A screen has complex UI state involving loading, success, empty, error, and pagination. How would you model it?

**What the interviewer is really asking**
Can you design a **state machine** that is exhaustive and mutually exclusive, so the UI has no impossible states? And do you know how that interacts with **paging** (which has its *own* load states) and with **pull-to-refresh**? The staff answer is a sealed interface with explicit states, plus the discipline to model *content* and *append state* separately.

**Key concepts**
- Sealed interface / `sealed class` hierarchies for exhaustive `when`
- `Loading` / `Content` / `Empty` / `Error` — but **loading and error are orthogonal to content**: you can have content *and* a refresh spinner
- **Sealed `UiState` per screen** vs **per-section state** (Section 1, Q5)
- Unidirectional data flow: state in, events out; the Composable renders, the ViewModel decides
- `StateFlow` with `update { }` for atomic state transitions
- Paging 3: `PagingData`, `LazyPagingItems`, `CombinedLoadStates` (`refresh`, `append`, `prepend`), `LoadState.Loading/NotLoading/Error`, `LoadType`
- `AsyncPagingDataDiffer` vs `collectAsLazyPagingItems()`
- `PullToRefreshBox` (Material3) / `PullRefreshContainer` ( Accompanist) for pull-to-refresh
- `rememberLazyListState` + `LoadState` for the "load more when near the end" trigger
- `derivedStateOf` to compute "should we load more" from the scroll position
- `SaveableStateHandle` for the paging key / filter state

**Approach**

1. **Start by listing the states the screen can actually be in, and make them mutually exclusive.** A flat sealed interface is right for a screen whose *entire* content is one thing:
   ```kotlin
   sealed interface FeedUiState {
       data object Loading : FeedUiState
       data object Empty : FeedUiState
       data class Content(val items: List<FeedItem>) : FeedUiState
       data class Error(val error: UiText, val cause: Throwable) : FeedUiState
   }
   ```
   `when (state)` is then exhaustive — the compiler enforces it.

2. **Split "content" from "transient activity"** as soon as the screen can show content *and* be loading. A flat model cannot represent "showing 20 old items while refreshing", and it forces a choice between a spinner and stale content. Model it as two orthogonal fields:
   ```kotlin
   data class FeedUiState(
       val content: ContentState,          // Loading | Empty | Content | Error
       val isRefreshing: Boolean = false,  // pull-to-refresh overlay
       val isAppending: Boolean = false,   // paging footer spinner
       val appendError: AppendError? = null,
   )
   ```
   Content state and paging state are genuinely different concerns, and separating them removes a whole class of impossible states.

3. **Let the ViewModel be the single owner of the transition logic.** The Composable never sets `isLoading = true` itself; it calls `viewModel.refresh()`. All state transitions are `update { }` calls inside the ViewModel — testable without Compose.

4. **Use a single `StateFlow<FeedUiState>`**, not several independent flows, unless they are genuinely independent. Multiple flows cause *torn state* (e.g. `content = Loading` and `items = old` observed at different times). One atomic state object is the staff answer.

5. **For Paging, do not duplicate what Paging already knows.** `LazyPagingItems` exposes `loadState` (the `CombinedLoadStates`), `itemCount`, `refresh()`, and `append()`. Derive the UI from it rather than mirroring it into your own state:
   ```kotlin
   val lazyItems = flow.collectAsLazyPagingItems()
   when (val refresh = lazyItems.loadState.refresh) {
       is LoadState.Loading -> LoadingState()
       is LoadState.Error -> ErrorState(refresh.error) { lazyItems.retry() }
       else -> { /* content */ }
   }
   ```
   The empty case is `loadState.refresh is LoadState.NotLoading && lazyItems.itemCount == 0` — a detail people frequently miss.

6. **Model the *append* error separately** — the list is still usable; show an inline footer with "Retry", not a full-screen error.

7. **Trigger append from the scroll position**, not from a scroll listener in the UI. `derivedStateOf` on the list state gives a cheap "near the end" signal:
   ```kotlin
   val shouldLoadMore by remember {
       derivedStateOf {
           val last = lazyItems.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
           last >= lazyItems.itemCount - 3
       }
   }
   ```

8. **Test the state machine, not the pixels.** Unit-test the ViewModel: assert the exact sequence of `FeedUiState` for (initial load, success, empty, error, refresh-fail-while-content-present). That is the highest-value test in the file.

9. **Make the `when` exhaustive and derive the screen from it.** Add `@Immutable` to the state class so the compiler can skip it, and keep `UiText` (a `StringRes` wrapper) rather than raw `String` so the ViewModel stays Android-agnostic.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Flat sealed state (`Loading/Content/Empty/Error`) | Exhaustive, easy `when`, hard to have impossible states | Cannot express "content + refreshing" or "content + append error" — you end up with a `Loading` that blanks the list |
| Content state + orthogonal flags | Expresses every real situation; pagination fits naturally | More fields; the UI must combine them; more test cases |
| Single `StateFlow<UiState>` | Atomic, no torn state | One large class; fine-grained updates recompose more of the screen |
| Multiple independent `StateFlow`s | Fine-grained recomposition | **Torn state** and ordering bugs; rarely worth it |
| Paging's own `CombinedLoadStates` | No duplicate state; handles prepend/refresh/append correctly | Different mental model; Paging's `LoadState` is not a sealed UI state — you must map it |
| Hand-rolled pagination with your own `LoadState` | Full control; simpler mental model | You reimplement prefetch, placeholders, and error recovery — usually a mistake |
| `derivedStateOf` for "load more" | Cheap; only invalidates when the boolean flips | Must read the right state; can be subtly wrong if the index source changes |
| `update { }` on a `MutableStateFlow` | Atomic, thread-safe | Slightly more verbose than direct assignment |
| `data class` + `@Immutable` state | Compiler can skip the consuming composable | A lie if the class actually holds a mutable list |

**Key detail:** "Loading" is **not** a peer of "Content" — they are two axes of the same axis, but *refreshing* and *appending* are **orthogonal** to content. The moment a screen can show stale content while a spinner runs, a flat four-state model becomes wrong. Say this explicitly; it is the crux of the question.

**Code example**

```kotlin
@Immutable
sealed interface ContentState<out T> {
    data object Loading : ContentState<Nothing>
    data object Empty : ContentState<Nothing>
    data class Data<T>(val items: List<T>) : ContentState<T>
    data class Error(val error: UiText) : ContentState<Nothing>
}

@Immutable
data class FeedUiState(
    val content: ContentState<FeedItem> = ContentState.Loading,
    val isRefreshing: Boolean = false,
    val isAppending: Boolean = false,
    val appendError: UiText? = null,
)

class FeedViewModel(repo: FeedRepository) : ViewModel() {
    private val _state = MutableStateFlow(FeedUiState())
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    fun refresh() {
        _state.update { it.copy(isRefreshing = true, appendError = null) }
        viewModelScope.launch {
            repo.refresh()
                .onSuccess { items ->
                    _state.update {
                        it.copy(
                            content = if (items.isEmpty()) ContentState.Empty
                                      else ContentState.Data(items),
                            isRefreshing = false,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { s ->
                        // Keep existing content if we have any; only blank on first load.
                        val content = if (s.content is ContentState.Data) s.content
                                      else ContentState.Error(e.toUiText())
                        s.copy(content = content, isRefreshing = false)
                    }
                }
        }
    }
}

@Composable
fun FeedScreen(state: FeedUiState, onRefresh: () -> Unit) {
    when (val c = state.content) {
        ContentState.Loading -> LoadingState()
        ContentState.Empty -> EmptyState()
        is ContentState.Error -> ErrorState(c.error, onRefresh)
        is ContentState.Data -> ListContent(c.items, state.isRefreshing, onRefresh)
    }
}
```

**Talking points / follow-ups**
- *"How do you avoid the spinner flashing on every small update?"* → Separate the initial `Loading` from `isRefreshing`, and only show the full-screen spinner when there is no content. This is the "loading vs refreshing" distinction that most implementations miss.
- *"Should empty be a subtype of content or of success?"* → Model it as a distinct state so the UI is forced to handle it; a "successful empty list" and "an error" are very different to the user.
- *"How do you model pagination error?"* → As an **inline footer**, not a screen-level error: the already-loaded content stays usable.
- *"Where should the state live?"* → ViewModel, exposed as a `StateFlow`, collected with `collectAsStateWithLifecycle`. The Composable is a pure function of state.
- *"How do you test it?"* → `runTest` with a fake repository; assert the exact sequence of emitted states, including the "error while content present" case.
- *"How does this connect to Section 1, Q5 (partial API failures)?"* → That is a *per-section* content state inside a single screen state; the same `ContentState` type can be reused per section, and the screen-level state becomes a wrapper of section states.

---

### 7. How would you prevent state updates from reaching a screen after the user has navigated away?

**What the interviewer is really asking**
Do you understand the difference between **cancelling the observation** and **cancelling the work**, and do you know that Compose already protects the UI (a disposed composition ignores updates) while the *expensive work* keeps running unless you tie it to the right scope? The staff answer: navigation-scoped ViewModels, `viewModelScope`, lifecycle-aware collection, and idempotent state writes.

**Key concepts**
- A **disposed composition** ignores recompositions; writing to a `StateFlow` is harmless but wasteful — the state is simply not observed
- `viewModelScope` — cancelled in `ViewModel.onCleared()`, which happens when the `NavBackStackEntry` is popped (not on rotation)
- `viewModel()` in Compose — scoped to the **current `NavBackStackEntry`**; a new instance per destination instance
- `hiltViewModel()` — same scoping, Hilt-provided
- `navGraphViewModel()` / a scoped `ViewModelStore` on the parent graph — a **shared** ViewModel across child destinations
- `rememberCoroutineScope()` — cancelled when the composable leaves the composition
- `LaunchedEffect` — cancelled when the composable leaves the composition; this is the *safe* place for UI-owned async work
- `collectAsStateWithLifecycle` — stops collecting below STARTED
- `DisposableEffect { onDispose { } }` — explicit teardown
- `DisposableEffect`/`onDispose` for **non-coroutine** resources (listeners, sensors, `LifecycleObserver`s)
- **Race**: the work completes *after* the screen is gone but *before* the coroutine is cancelled → a late state write
- `Lifecycle.State.DESTROYED` guard, and `savedStateHandle` for state that must survive

**Approach**

1. **Be precise about what "reaching the screen" means.** Once the composition is disposed, Compose will not render the update — the update is *not* applied, but the **work is not automatically stopped**. The real problems are (a) wasted work, (b) a stale write that corrupts state a *new* screen instance will read, and (c) crashes from touching a destroyed context.

2. **Scope the work to the screen's lifetime.** Use `LaunchedEffect` for UI-owned async work:
   ```kotlin
   LaunchedEffect(id) { viewModel.load(id) }
   ```
   The coroutine is cancelled automatically when the composable leaves the composition or when `id` changes. Use `rememberCoroutineScope()` for user-triggered work (button clicks) and cancel it on exit.

3. **Scope the ViewModel to the navigation entry.** `viewModel()` inside a `NavHost` destination is bound to that destination's `NavBackStackEntry`; it is cleared when the entry is popped, cancelling `viewModelScope`. This is the primary mechanism — *not* extra guards in the UI.

4. **Handle the race explicitly.** Even with a cancelled scope, a coroutine may already be past its last suspension point when cancellation happens, so it can write state once more. Defend with:
   - An **id/epoch check** before publishing: only write if the request id matches the current one.
   - A **lifecycle check** before writing: `if (!lifecycle.currentState.isAtLeast(STARTED)) return`.
   - Better: make the write **idempotent and scoped** — write into a state object owned by the ViewModel for *that* screen instance, so a late write cannot affect a new screen.

5. **Never let a stale screen update a shared ViewModel.** If the ViewModel is shared across destinations (see Q11), a late write from the popped destination *does* affect the surviving one. Guard with a per-request id:
   ```kotlin
   private var currentRequestId = 0L
   fun load() {
       val id = ++currentRequestId
       viewModelScope.launch {
           val result = repo.load()
           if (id == currentRequestId) _state.value = result     // stale results dropped
       }
   }
   ```

6. **Release non-coroutine resources deterministically.** `DisposableEffect` with `onDispose` for listeners, `LifecycleObserver`s, sensor registrations, and `AndroidView` factories. Do not use `LaunchedEffect` for teardown — use `DisposableEffect`.

7. **Cancel on explicit user intent too.** If the user hits "cancel" on an upload, cancel the `Job` explicitly (`job.cancel()`), not just hide the UI.

8. **Prefer `collectAsStateWithLifecycle` over `LaunchedEffect` collection.** It handles the "stop when backgrounded" case for free, and the underlying scope handles "stop when navigated away".

9. **Do not use `GlobalScope`.** It is the root cause of "updates from a screen that no longer exists" in many codebases: the work outlives everything and writes to a dead ViewModel. Use an application-scoped `CoroutineScope` only for work that genuinely should outlive the UI (e.g. an upload), and route the result back through a `Channel`/`SharedFlow` on the app level.

10. **Add a crash guard for `Context`/View access after destroy** — never capture an `Activity` in a long-lived callback; use `applicationContext` or the `LocalContext`'s `applicationContext`.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `LaunchedEffect` for UI-owned work | Cancelled automatically with the composition; idiomatic | Must be re-launched on key change; not available for work triggered outside composition |
| `viewModelScope` | Survives rotation; cancelled when the nav entry is popped | Survives *backgrounding*; work continues while the app is not visible |
| `rememberCoroutineScope()` | Tied to the composition | Cancelled when the composable leaves composition — correct for one-shot UI work, wrong for work that must outlive the screen |
| Application-level scope + `Channel` back to the UI | Work completes even if the user leaves (e.g. a payment) | More plumbing; the result must be delivered when the user returns — needs explicit "pending" state |
| `DisposableEffect` + `onDispose` | Deterministic teardown for non-coroutine resources | Easy to forget; misuse (e.g. disposing a `LaunchedEffect` job) is confusing |
| Epoch / request-id guard | Prevents stale writes into a shared ViewModel | Extra bookkeeping; must be applied consistently |
| Lifecycle `isAtLeast(STARTED)` guard before writing | Simple, semantically meaningful | Prevents *background* writes, not *post-navigation* writes; the nav case needs the scope |
| `GlobalScope` | "Always runs" | **Never** — leaks, writes to dead state, survives everything, untestable |
| `collectAsStateWithLifecycle` | Lifecycle-correct observation | Stops below STARTED; the upstream must support re-subscription |

**Key detail:** The idiomatic answer is **"you do not need to guard against it — scope the work correctly"**. `viewModelScope` (or `LaunchedEffect`) already cancels on navigation. Explicit guards are a **second line of defence** needed only when the state is *shared* across destinations, because then a stale write is genuinely harmful. Making that distinction — "the scope handles the common case; the epoch guard handles the shared-ViewModel case" — is what separates a mid-level from a staff answer.

**Code example**

```kotlin
@Composable
fun DetailScreen(viewModel: DetailViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // UI-owned async work: cancelled automatically when we navigate away.
    LaunchedEffect(viewModel.itemId) {
        viewModel.load(viewModel.itemId)
    }

    // Non-coroutine resources: explicit teardown.
    DisposableEffect(Unit) {
        val listener = viewModel.registerAnalyticsListener()
        onDispose { listener.remove() }
    }

    DetailContent(state)
}

class DetailViewModel(private val repo: DetailRepository) : ViewModel() {

    // Guards against a stale response overwriting a newer one.
    private var requestId = 0L

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun load(id: String) {
        val myId = ++requestId
        viewModelScope.launch {                       // cancelled on nav-away
            val result = repo.fetch(id)
            if (myId == requestId) {                 // drop stale results
                _state.value = result
            }
        }
    }

    fun onUserCancelled() {
        requestId++                                  // invalidate any in-flight result
    }
}
```

**Talking points / follow-ups**
- *"Can a state update actually crash a navigated-away screen?"* → Not via Compose (the composition is disposed and ignores updates). It crashes if the update touches an `Activity`/View reference, or corrupts state a *shared* ViewModel exposes.
- *"What is the difference between `viewModelScope` and `rememberCoroutineScope`?"* → `viewModelScope` outlives the composition — it survives rotation (the ViewModel survives) and is cancelled only when the ViewModel is cleared (nav entry popped). `rememberCoroutineScope` dies the moment the composable leaves the composition, **including on rotation**.
- *"How do you test that nothing arrives after navigation?"* → Compose test: navigate away while a fake repository is suspended; complete the fake; assert the (shared) ViewModel state was not overwritten.
- *"What about work that *must* survive navigation (a payment, an upload)?"* → Move it to an application-scoped `CoroutineScope` / `WorkManager`, model the in-progress operation as **state** with a `PENDING` status, and reconcile on return. See Section 1, Q2.
- *"Is `LaunchedEffect` cancelled when the app goes to the background?"* → No — only when the composable leaves the composition or a key changes. Backgrounding does **not** cancel it; use `repeatOnLifecycle` for that.

---

### 8. A composable performs expensive sorting and filtering on every recomposition. How would you optimize it?

**What the interviewer is really asking**
Do you know the difference between **`remember` (cache across recompositions)** and **`derivedStateOf` (cache an invalidation)** and **`remember` with keys (cache until the key changes)** — and do you reach for the right one? The staff answer also covers moving work **out of composition** entirely (into the ViewModel) and profiling with the Compose compiler metrics rather than guessing.

**Key concepts**
- `remember { }` — recomputes only when the composable is **re-entered into the composition** (not on every recomposition of the same scope)
- `remember(key1, key2) { }` — recomputes when a key changes; keys compared with `equals`
- `derivedStateOf { }` — a *state* whose value is computed lazily and only invalidates its readers when the **output** changes, not when the inputs change
- `remember` inside a `LazyColumn` item — the cache is per item slot; a `remember` inside `items { }` is re-created when the item is scrolled out and back
- `mutableStateListOf` / `SnapshotStateList` — reads are tracked per element
- The recomposition *scope* — the cache lives in the composable's `remember` group
- Compose compiler metrics — `remember` counts reveal a `remember` inside a hot path
- `key` on `items {}` — stable identity also stabilises the `remember` cache of each item
- Kotlin `sortedWith`/`filter` are eager; `Sequence` is lazy (not necessarily faster for small lists)
- Moving the work to the **ViewModel** — the computation happens once per data change, not per composition; also makes it unit-testable
- `java.util.Arrays.sort` vs `sortedBy` — `sortedBy` allocates; use `sortWith` on a mutable list when the result is not needed

**Approach**

1. **First, find out *where* the work actually happens.** "On every recomposition" has two very different causes:
   - The **filter/sort runs in the composable body** — i.e. it is inside the `remember` scope of the parent composable, so it re-runs whenever that scope recomposes (which is whenever any read state changes).
   - The **item composables recompose** and each one re-sorts its own sub-list — this is O(n) sorts.
   Use the Compose compiler metrics report (count of `remember` calls vs composable calls) and Layout Inspector's recomposition counts to confirm.

2. **Move the derivation out of the composable into the ViewModel.** This is the strongest fix: the ViewModel computes `visibleItems` once per *data change* and exposes it in the UI state. The composable then only renders. Bonus: it is unit-testable without Compose.

3. **If it must stay in the composable, cache it with `remember` and correct keys:**
   ```kotlin
   val visible = remember(items, query, sortOrder) {
       items.filter { it.matches(query) }.sortedWith(comparator)
   }
   ```
   The keys are the *complete* set of inputs. A missing key is the classic bug: `remember(items)` when the sort order can change → stale order. A `remember` with no keys at all re-runs only on re-entry into the composition, so it will go stale when the inputs change — that is a correctness bug, not just a performance one.

4. **Use `derivedStateOf` when the expensive work must be re-evaluated on every change but should only *notify* when the result changes:**
   ```kotlin
   val visible by remember {
       derivedStateOf {
           if (query.isEmpty()) items else items.filter { it.matches(query) }
       }
   }
   ```
   `derivedStateOf` re-runs the block when its dependencies change, but only invalidates the reader if the *returned value* is different (under the equality policy). This is the right tool when the filter is cheap but the *notification* is expensive (i.e. it feeds a large lazy list).

5. **Prefer the cheapest correct tool:**
   - Cheap filter + expensive downstream → `derivedStateOf` (gates the invalidation).
   - Expensive filter + rare input changes → `remember(items, query) { ... }` (skip the work entirely).
   - Both expensive and frequent → precompute in the ViewModel, or pre-index the data (e.g. a `Map<Char, List<Item>>` prefix index for search).

6. **Never sort inside `items { }`.** Sorting per item is O(n²) and a genuine bug. If each item needs a sorted sub-list, derive it once above the `LazyColumn`.

7. **Use a `LazyColumn` with stable keys** so items are not re-sorted/re-created on scroll; and remember per-item derived state with `remember(item.id) { ... }` inside the item.

8. **Profile before optimising further.** The Compose compiler metrics show `remember` counts; Android Studio's **Compose Performance** panel shows recomposition durations. `trace("Screen") { ... }` in `androidx.compose.runtime` gives you a named span in the trace viewer.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Compute in the ViewModel | Runs once per data change; unit-testable; no recomposition cost | ViewModel holds more logic; the UI state can get large; requires the data to be observable |
| `remember(inputs) { compute() }` | Skips the work entirely until inputs change | **Must enumerate every input as a key** — a missing key yields stale results, a redundant key yields wasted work |
| `remember { compute() }` (no keys) | Cheapest syntax | Recomputes only on re-entry; **goes stale** when inputs change — a correctness bug |
| `derivedStateOf { compute() }` | Re-evaluates on dependency change but notifies only on output change | The block re-runs on every dependency change — **not** a general performance win for expensive work; adds a snapshot layer |
| `derivedStateOf` with `structuralEqualityPolicy` | Standard, value-based | Deep `equals` on large lists is itself expensive — measure it |
| `referentialEqualityPolicy` on the derived state | Cheap comparison | Invalidates on every new list instance → more recompositions |
| Pre-indexing (e.g. search prefix map) | O(1) lookups | Build cost; invalidation complexity; overkill below a few thousand items |
| `java.util.Collections.sort` in place | No extra allocation | Mutates shared state — dangerous with Compose snapshots |
| `sortedWith` on a copy | Safe, idiomatic | Allocates a new list each call — fine when cached with `remember` |

**Key detail:** `remember` and `derivedStateOf` solve **different** problems and are frequently confused. `remember` = *"do not recompute this unless the composition is re-entered or a key changes"*. `derivedStateOf` = *"recompute when dependencies change, but only invalidate my readers when the result actually changes"*. Using `derivedStateOf` to avoid an expensive computation is a common misconception — the computation still happens; only the *notification* is filtered.

**Code example**

```kotlin
// ✅ Derivation in the ViewModel: computed once per data change, unit-testable.
class FeedViewModel(repo: FeedRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SortOrder.NEWEST)

    val state: StateFlow<FeedUiState> =
        combine(repo.items, query, sort) { items, q, s ->
            FeedUiState.Content(items.filter { it.matches(q) }.sortedWith(s.comparator))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState.Loading)
}

// ✅ In the composable: remember with COMPLETE keys.
@Composable
fun FeedList(items: List<Item>, query: String, sortOrder: SortOrder) {
    // Keys = every input. `remember(items)` alone would ignore query/sort changes.
    val visible = remember(items, query, sortOrder) {
        items.asSequence()
            .filter { it.matches(query) }
            .sortedWith(sortOrder.comparator)
            .toList()
    }

    // ✅ derivedStateOf when the filter is cheap but invalidation is expensive.
    val isEmpty by remember(items, query) {
        derivedStateOf { items.none { it.matches(query) } }
    }

    LazyColumn { items(visible, key = { it.id }) { ItemRow(it) } }
    if (isEmpty) EmptyState()
}
```

**Talking points / follow-ups**
- *"Does `remember` protect against recomposition?"* → Yes, against *re-execution of that scope*; the cached value is reused. But if the surrounding scope is invalidated and the `remember` group is re-entered, the block re-runs.
- *"Where do I put the expensive work so it does not block the UI thread?"* → In the ViewModel, in a `Dispatchers.Default` context via `flowOn`; the composition should only map state to UI.
- *"How do you know if the recomposition is actually expensive?"* → Compose compiler metrics for counts; Android Studio Compose Performance panel; `trace` spans. Do not optimise from intuition.
- *"What about `derivedStateOf` with a `List` result?"* → The default equality policy does a content comparison of the whole list on every notification — for large lists that can be as expensive as the filter. Prefer `remember` with keys when the computation itself is expensive.
- *"Does the `remember` cache survive scrolling in a `LazyColumn`?"* → A `remember` inside an item composable belongs to that item's slot; when the item is scrolled out it may be disposed and its cache lost. Put list-level derivations **above** the `LazyColumn`.

---

### 9. How would you implement an accessible, testable Compose screen with reusable components and unidirectional data flow?

**What the interviewer is really asking**
Can you design a **screen** rather than a pile of composables? The three pillars: (a) **unidirectional data flow** — state in, events out, no logic in the composable; (b) **accessibility** — semantics, roles, touch targets, and testing that a11y is not an afterthought; (c) **testability** — the screen is a pure function of state, so a Compose test can drive it without a ViewModel.

**Key concepts**
- **UDF**: `State → Composable → Callback → ViewModel → State`
- Stateless screen + stateful wrapper: `FeedScreen(state, onEvent)` (previewable, testable) and `FeedRoute(viewModel)` (binds the ViewModel)
- `Modifier.semantics { }` — `contentDescription`, `stateDescription`, `role = Role.Button`, `heading()`, `liveRegion`, `mergeDescendants`, `clearAndSetSemantics`, `testTag`, `onClick(label = ...) { true }`
- `Modifier.clickable(onClickLabel = ...)` — announces the action
- Minimum touch target: **48dp** (Material/Android accessibility guideline); `Modifier.minimumInteractiveComponentSize()`
- `Modifier.semantics { heading() }` for section headers
- `LocalAccessibilityManager` — `isEnabled`, `getTextToSpeechService`
- `testTag` — the stable handle for Compose UI tests
- `createComposeRule()` / `createAndroidComposeRule<ComponentActivity>()`; `onNodeWithText`, `onNodeWithTag`, `performClick`, `assertIsDisplayed`, `assertHasClickAction`
- `mainClock.autoAdvance = false` + `mainClock.advanceTimeBy()` for animation tests
- `ui-test-junit4` + `ui-test-manifest` (needed for `createAndroidComposeRule` with an empty activity)
- `composeTestRule.onRoot().printToLog()` / `printToString()` — the semantics tree dump, essential when a node is not found
- `Roborazzi` / screenshot testing for visual regression
- `@Preview` + `@PreviewParameter` for component galleries
- `ImageVector`/icons with `contentDescription = null` for decorative icons
- `LocalInspectionMode` — preview-only content

**Approach**

1. **Split the screen into a stateless core and a stateful route.** The stateless part is what you test and preview:
   ```kotlin
   @Composable
   fun FeedRoute(viewModel: FeedViewModel, modifier: Modifier = Modifier) {
       val state by viewModel.state.collectAsStateWithLifecycle()
       FeedScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
   }

   @Composable
   fun FeedScreen(state: FeedUiState, onEvent: (FeedEvent) -> Unit, modifier: Modifier = Modifier) {
       // pure rendering of state
   }
   ```
   Now the Compose test constructs the state directly and asserts rendering — no ViewModel, no coroutines, no mocking.

2. **Model interactions as a single `onEvent` channel.** One event type (`sealed interface FeedEvent`) keeps the contract explicit and testable:
   ```kotlin
   sealed interface FeedEvent {
       data object Retry : FeedEvent
       data class ItemClicked(val id: Long) : FeedEvent
       data class QueryChanged(val query: String) : FeedEvent
   }
   ```
   The composable never calls the repository; it emits an event.

3. **Centralise reusable components.** Extract `FeedCard`, `LoadingState`, `ErrorState`, `EmptyState` into a design-system module with:
   - A consistent API surface (`Modifier` first, then required params, then optional).
   - Built-in semantics and touch targets so they are accessible *by construction*.
   - `@Preview` for each state variant.

4. **Make accessibility a default, not an audit.**
   - Every clickable has a `contentDescription` (or `Modifier.semantics { }` with a role).
   - Use `Modifier.clickable(onClickLabel = "Open article")` so TalkBack announces the action.
   - Decorative icons get `contentDescription = null`; meaningful icons get a description.
   - Images that convey content get a description; images that are purely decorative do not.
   - Headers use `Modifier.semantics { heading() }`.
   - Live regions for changing content: `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`.
   - Group a card into a single node with `Modifier.semantics(mergeDescendants = true) { }` so TalkBack reads one coherent item instead of five fragments.
   - Respect **48dp** minimum touch targets (`Modifier.minimumInteractiveComponentSize()`); ensure icon buttons are padded.
   - Support font scaling: avoid fixed heights; use `Text` with `maxLines` + `overflow`; test at `fontScale = 2.0`.
   - Do not rely on colour alone to convey state (WCAG) — pair with an icon or text.

5. **Make the screen testable.** Use `testTag` for handles that have no visible text, and prefer `onNodeWithText` for anything user-visible:
   ```kotlin
   @Test
   fun errorState_showsRetryAnd_emitsEvent() {
       val events = mutableListOf<FeedEvent>()
       composeTestRule.setContent {
           FeedScreen(state = FeedUiState.Error(UiText("Boom")), onEvent = { events += it })
       }
       composeTestRule.onNodeWithText("Retry").performClick()
       assertEquals(FeedEvent.Retry, events.single())
   }
   ```
   - Use `createAndroidComposeRule<ComponentActivity>()` when you need a real Activity (themes, `LocalContext`, Dialogs); add `androidx.compose.ui:ui-test-manifest` in debug.
   - For animations, set `mainClock.autoAdvance = false` and `advanceTimeBy(...)` so the test is deterministic.
   - When a node is not found, dump the tree: `composeTestRule.onRoot().printToLog("FEED")`.

6. **Cover the state matrix, not just the happy path.** One test per `FeedUiState` variant plus one interaction test per event. That is a small test suite with high signal.

7. **Add screenshot tests for visual regressions** (Roborazzi) — especially for a11y-sensitive layouts (large font scale, RTL).

8. **Run a11y checks in CI.** Lint rules for `ContentDescription`, and a manual pass with TalkBack on a real device. Automate what you can (semantics assertions in tests), schedule what you cannot.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Stateless screen + stateful route | Testable without a ViewModel; previewable; reusable | One extra layer of plumbing; the state type must be complete |
| Direct `viewModel()` calls inside composables | Less code | Couples the screen to the ViewModel → cannot preview/test in isolation; harder to reuse |
| Single `onEvent: (FeedEvent) -> Unit` | One explicit contract; easy to fake in tests | Large screens need many event variants; a sealed interface with many cases |
| Per-callback lambdas (`onRetry`, `onClick`) | Simple, obvious, type-safe per callback | Event count grows; the contract is less discoverable as a whole |
| `testTag` everywhere | Deterministic test handles | Test tags leak into production semantics; prefer visible-text queries where possible; tags become a de-facto API |
| `contentDescription` on every icon | Safe by default | Noisy for decorative icons — TalkBack reads meaningless labels; use `null` deliberately |
| `mergeDescendants = true` on cards | One coherent announcement per item | Can hide meaningful sub-actions; must not swallow child click actions |
| `LocalAccessibilityManager` checks | Graceful degradation for TTS-absent devices | Requires manual handling in several places |
| Screenshot testing (Roborazzi) | Catches visual regressions | Flaky on different GPUs/fonts; needs a controlled CI environment |

**Key detail:** `testTag` is part of the **semantics tree**, so it is present in production too. Prefer querying by visible text or by `contentDescription` (which is what a user actually perceives) and use `testTag` only where there is no text. Also: a Compose screen is *accessible by construction* only if the reusable components own the semantics — pushing `contentDescription` to call sites is how a11y gets missed.

**Code example**

```kotlin
// Stateless, reusable, accessible, testable.
@Composable
fun FeedScreen(
    state: FeedUiState,
    onEvent: (FeedEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Text(
            text = "Feed",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() },      // announce as a header
        )
        when (val c = state.content) {
            ContentState.Loading -> LoadingState()
            ContentState.Empty -> EmptyState(onRetry = { onEvent(FeedEvent.Retry) })
            is ContentState.Error -> ErrorState(
                message = c.error,
                onRetry = { onEvent(FeedEvent.Retry) },        // 48dp target inside
            )
            is ContentState.Data -> LazyColumn {
                items(c.items, key = { it.id }) { item ->
                    FeedCard(
                        item = item,
                        onClick = { onEvent(FeedEvent.ItemClicked(item.id)) },
                        modifier = Modifier
                            .semantics(mergeDescendants = true) { }   // one TalkBack node
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

// Test: no ViewModel, no coroutines, deterministic.
@Test
fun errorState_clickingRetry_emitsRetryEvent() {
    val events = mutableListOf<FeedEvent>()
    composeTestRule.setContent {
        FeedScreen(state = FeedUiState.Error(UiText("Boom")), onEvent = { events += it })
    }
    composeTestRule.onNodeWithText("Retry").performClick()
    assertEquals(listOf(FeedEvent.Retry), events)
}
```

**Talking points / follow-ups**
- *"How do you know the screen is accessible?"* → Semantics assertions in tests (`assertHasClickAction`, `assertContentDescriptionEquals`), a manual TalkBack pass, font-scale and RTL previews, plus lint.
- *"What is `mergeDescendants` for?"* → Collapsing a card into one semantics node so TalkBack reads "Title, 3 minutes ago, Open article" instead of five separate stops.
- *"How do you make a screen reusable?"* → Stateless content composable with a `Modifier` parameter, a small explicit API, and a separate stateful `Route` that binds the ViewModel. Reuse the *content*, not the route.
- *"How do you test a screen that collects a flow?"* → Don't: test the stateless screen with a given state, and unit-test the ViewModel separately with `runTest` + a fake repository.
- *"What about `@Preview` for a component gallery?"* → Yes, with `@PreviewParameter` to iterate over states — but previews do not replace tests.
- *"How do you handle large font scales?"* → Avoid fixed heights; use `IntrinsicSize` sparingly; test at `fontScale = 2f`; make touch targets grow with content.

---

### 10. When would you choose XML with RecyclerView over Jetpack Compose, and what trade-offs would you consider?

**What the interviewer is really asking**
This is a **judgment** question, and the trap is answering "Compose is better, always". The staff answer is **incremental migration**: a hybrid codebase is a legitimate, often optimal, engineering state — and the interviewer wants the trade-off matrix plus an **interop strategy** and a clear decision rule.

**Key concepts**
- Incremental migration: `ComposeView` inside an XML layout, or `AndroidView` inside Compose
- `ComposeView` in XML — set `composeView.setContent { }`; needs `androidx.compose.ui.platform.ComposeView` and the correct `ViewCompositionStrategy`
- `AndroidView` / `AndroidViewBinding` — embedding a classic View in Compose; `factory`/`update` lambdas, `remember` the interop view, release it in `DisposableEffect`
- `ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed` vs `DisposeOnDetachedFromWindow` — the default detaches on removal from the window, which can destroy composition state too early
- RecyclerView: `ListAdapter` + `DiffUtil`, `AsyncListDiffer`, `ViewHolder` recycling, `setHasStableIds`, `ConcatAdapter`
- Compose: `LazyColumn`, `key = {}`, recomposition, `remember`
- `ViewBinding` / `DataBinding` vs `Compose`
- Interop cost: an `AndroidView` inside a `LazyColumn` breaks the lazy recycling model; each item hosts a real View → more memory, slower
- `AndroidComposeView` vs `ComposeView` in a `Fragment`
- Tooling: Compose previews vs Layout Inspector for XML, `adb shell uiautomator`
- Team skill, time-to-market, feature churn, and the "rewrite vs migrate" trap
- `MotionLayout`, `RecyclerView` view types, `SurfaceView`, `TextureView`, video players, maps (`com.google.maps.android.compose` vs `MapView` in XML) — some things are still simpler in the View world

**Approach**

1. **State the decision rule up front.** Compose wins for **new, greenfield, high-churn UI** — it removes the XML/View inflation and findViewById tax, enables fast iteration, and gives a single mental model. XML/RecyclerView wins for **large, stable, legacy surfaces** where a rewrite costs more than it returns and the team is already productive there.

2. **Do not rewrite.** The honest recommendation for a production codebase is **incremental migration, screen by screen**, in the order of *highest churn × lowest risk*:
   - Start with a **new feature** (no back-compat burden) — this builds team familiarity with zero regression risk.
   - Then migrate **leaf screens** (settings, profile, onboarding) before complex ones.
   - Migrate the **shell** (bottom nav, top bar) last, so navigation is stable while screens churn.

3. **Use `ComposeView` inside XML for the migration path.** This is the lowest-risk interop because the Compose side owns its own state:
   ```kotlin
   // res/layout/fragment_feed.xml
   <androidx.compose.ui.platform.ComposeView
       android:id="@+id/compose_view"
       android:layout_width="match_parent"
       android:layout_height="match_parent" />
   ```
   ```kotlin
   class FeedFragment : Fragment(R.layout.fragment_feed) {
       override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
           view.findViewById<ComposeView>(R.id.compose_view).setContent {
               FeedRoute(viewModel = feedViewModel)
           }
       }
   }
   ```
   Set `composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)` so the composition is not destroyed when the view is detached from the window (e.g. during a fragment transition) — otherwise state is lost on every navigation.

4. **Use `AndroidView` for the reverse direction — only for genuinely View-bound components:** a third-party `MapView`/`PlayerView`, a `SurfaceView`-based video surface, `TextureView`, a legacy `WebView` with a complex controller, or a widget you cannot rewrite. Keep the interop boundary **thin and at the leaf level**; never put an `AndroidView` inside a `LazyColumn` item, because each item then hosts a full View hierarchy → no recycling, memory blow-up, jank.

5. **Cost of interop is not symmetric.** `ComposeView` in XML is cheap and idiomatic. `AndroidView` in Compose is expensive: it defeats Compose's layout and drawing model, breaks `LazyColumn` recycling, and adds a "second UI system" to reason about. Budget for it.

6. **Consider the ecosystem reality.** Some libraries are only stable as Views (`AndroidViewBinding`-based SDKs, some ad SDKs, `ExoPlayer`'s `PlayerView`, certain map views). Compose alternatives exist for most (`androidx.media3.ui` still uses Views for the player; `maps-compose` exists). Audit before committing.

7. **Consider the team's operating model.** Compose has a higher ramp-up cost: the recomposition model, the new mental model, the tooling, and the review skills needed. If the team is strong on Compose, the migration cost collapses. If not, the migration may stall halfway and leave a hybrid codebase that is *worse* than either extreme — the real risk to name.

8. **Measure the migration.** Track: screen count migrated, crash-free users, ANR rate, frame metrics (JankStats / Macrobenchmark), and time-to-feature. The claim "Compose is faster to build" must be validated for *your* team, not assumed.

9. **Plan the endgame.** Hybrid is acceptable as a steady state for many apps, but write down the criteria for when a screen *must* be XML (legacy SDK, `SurfaceView` requirement, WebView-heavy, a vendor SDK with no Compose support) and when it *should* be Compose (any new UI).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Full Compose rewrite | One UI system; best DX; fastest iteration afterwards | Huge cost; big-bang risk; historical bugs re-introduced; delivery risk of a multi-quarter rewrite |
| Incremental migration (`ComposeView` in XML) | Low risk; ships value continuously; reversible per screen | Hybrid codebase; two UI stacks to maintain; mixed conventions; shared design tokens need work |
| Stay on XML/RecyclerView | No migration cost; the team is productive; mature tooling | Slower iteration; more boilerplate; no `State`/recomposition model; harder to do complex dynamic UI |
| `AndroidView` (XML inside Compose) | Reuse legacy components | Expensive; breaks lazy recycling; two UI paradigms; memory and jank cost |
| `ComposeView` (Compose inside XML) | Cheap; Compose keeps its own state | Requires `DisposeOnViewTreeLifecycleDestroyed`; layout params interop; `findViewById` for the bridge |
| RecyclerView + `ListAdapter`/`DiffUtil` | Mature; strong recycling; `setHasStableIds` | Verbose; manual diffing; ViewHolder boilerplate; no automatic state invalidation |
| `LazyColumn` + `key` | Automatic diffing, less code | RecyclerView can be faster for extreme item counts (1000+ homogeneous simple rows) — Compose wins on complexity, not always on raw throughput |
| Hybrid steady state | Pragmatic; fits real constraints | Long-term maintenance of two systems; skills dilution |

**Key detail:** The `ViewCompositionStrategy` default is `DisposeOnDetachedFromWindow`, which destroys the composition when the `ComposeView` is detached — in a fragment/nav transition this can wipe state and cause flicker. `DisposeOnViewTreeLifecycleDestroyed` ties disposal to the lifecycle instead. This single line is the most common source of "Compose state resets when I navigate" in mixed codebases.

**Code example**

```kotlin
// XML → Compose: the migration path.
class FeedFragment : Fragment(R.layout.fragment_feed) {
    private val viewModel: FeedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<ComposeView>(R.id.compose_view).apply {
            // Do NOT dispose on detach — state would be lost on every nav transition.
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                FeedRoute(viewModel = viewModel)
            }
        }
    }
}

// Compose → XML: only for View-bound leaves (maps, video, legacy SDKs).
@Composable
fun LegacyPlayer(url: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } },
        modifier = modifier,
        update = { view -> view.player = exoPlayer },
        onRelease = { view -> view.player = null },   // release the player
    )
}
```

**Talking points / follow-ups**
- *"How long does a Compose rewrite take?"* → Never answer with a number; answer with the *per-screen* cost and the sequencing, and point out that the value arrives only at the end — which is why incremental wins.
- *"Does Compose replace RecyclerView's recycling?"* → Yes for most lists: `LazyColumn` composes only the visible items plus a small prefetch buffer. But it does not recycle *View instances*; it re-composes. For very large, very simple lists, RecyclerView can still measure faster.
- *"What breaks when you mix them?"* → Touch handling and gesture arbitration, `View` focus/IME behaviour, `SurfaceView`/`TextureView` layering, and the `ViewCompositionStrategy` disposal bug.
- *"How do you share design tokens across both worlds?"* → One source of truth for colour/spacing/type in a shared module, mapped to both XML themes (`values/themes.xml`) and Compose (`MaterialTheme`), with a screenshot test to detect drift.
- *"What would make you go XML-only?"* → A team that cannot support Compose, a codebase with heavy vendor SDK dependencies, or a hard constraint like `SurfaceView`-based DRM video.
- *"How do you convince stakeholders?"* → With the per-screen migration playbook and measured outcomes (feature lead time, crash rate, jank), not with "Compose is modern".

---

### 11. A shared ViewModel is used across several navigation destinations. How would you determine its appropriate lifecycle and scope?

**What the interviewer is really asking**
Do you understand the **three distinct ViewModel scopes** in a Navigation-Compose app — per-`NavBackStackEntry` (destination), per-parent-graph (shared), and activity-scoped — and can you reason about **who clears it and when**? The staff answer pairs the *scope* with the *lifecycle of the data*, not with the *screen*.

**Key concepts**
- `viewModel()` in a `NavHost` destination → scoped to that destination's `NavBackStackEntry`; cleared when the entry is popped
- Navigation Compose creates a **nested** `NavBackStackEntry` for each graph; a child destination's `NavBackStackEntry` has the parent graph entry as its parent
- `navGraphViewModel()` / `hiltViewModel(viewModelStoreOwner = ...)` — a ViewModel scoped to the **parent graph's** `ViewModelStore`, shared by all children of that graph; cleared when the graph entry is popped
- `hiltViewModel()` — same scoping semantics, Hilt-provided
- Activity scope: `by activityViewModels()` / `ViewModelStoreOwner = activity` — survives everything until the Activity finishes
- `ViewModelStoreOwner` — the interface Navigation uses to hold ViewModels
- `SavedStateHandle` — the `NavBackStackEntry` provides one; `navArgument` values arrive in it; it also defines the **saved-state** scope
- `rememberNavController()` — the NavController for the whole graph
- `NavHost(navController, startDestination)` and `composable(route) { entry -> ... }` — the `entry` is the `NavBackStackEntry`
- `NavBackStackEntry` lifecycle vs the Activity lifecycle — the entry's lifecycle is driven by the NavController, and can be `RESUMED` while the Activity is `STARTED`
- The ViewModel's scope vs the *effect* scope: `viewModelScope` runs while the ViewModel exists, regardless of whether the screen is visible
- `collectAsStateWithLifecycle` uses the **local** lifecycle owner (the NavBackStackEntry's), so a shared ViewModel's flow is only collected while the *current* destination is STARTED

**Approach**

1. **Start from the lifetime of the *data*, not the screen.** Ask: "how long must this state outlive any single screen?"
   - Form input for one screen → **destination-scoped** ViewModel.
   - Data for a multi-step flow (checkout: address → payment → review) → **parent-graph-scoped** ViewModel (the flow is a graph), so the state survives across its steps.
   - Session-wide state (auth token, feature flags, user profile) → **activity-scoped** ViewModel (or a repository singleton).

2. **Use destination scope by default.** `viewModel()` inside a `composable(route)` is bound to that route's `NavBackStackEntry`:
   ```kotlin
   composable("feed") { entry ->
       val vm: FeedViewModel = viewModel()          // scoped to this entry
       FeedRoute(vm)
   }
   ```
   It survives configuration changes and is cleared when the destination is popped. Note that with the standard `NavHost`, popping the back stack destroys the entry, and therefore the ViewModel.

3. **Use a parent graph for shared state.** Wrap the shared destinations in a nested graph and scope the ViewModel to the **parent** entry:
   ```kotlin
   NavHost(navController, startDestination = "checkout") {
       navigation(
           route = "checkout",
           startDestination = "checkout/address",
       ) {
           composable("checkout/address") {
               val vm: CheckoutViewModel = hiltViewModel(
                   viewModelStoreOwner = it.parent!!,   // the graph's entry
               )
               AddressStep(vm)
           }
           composable("checkout/payment") {
               val vm: CheckoutViewModel = hiltViewModel(
                   viewModelStoreOwner = it.parent!!,
               )
               PaymentStep(vm)
           }
       }
   }
   ```
   All children of the graph share **one** `CheckoutViewModel` instance; it is cleared when the **graph** entry is popped, not when an individual step is popped. This is the canonical answer for "shared across several navigation destinations".

4. **Remember the `NavBackStackEntry` lifecycle nuance.** Each destination's entry has its own `Lifecycle`, driven by the NavController. `collectAsStateWithLifecycle()` inside a destination uses that entry's lifecycle, so a shared ViewModel's flow is collected only while the *current* destination is at least `STARTED` — the shared state itself is untouched, only the observation stops.

5. **Use `SavedStateHandle` for state that must survive process death**, and get navigation arguments from it:
   ```kotlin
   class DetailViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
       val itemId: String = savedStateHandle.get<String>("itemId")!!
   }
   ```
   `navArgument("itemId") { type = NavType.StringType }` puts it in the entry's `SavedStateHandle`. The `SavedStateHandle`'s scope matches the entry's lifetime — so the *state* scope and the *ViewModel* scope can differ if you use `rememberSaveable` for some values and a ViewModel for others.

6. **Be deliberate about the activity scope.** `activityViewModels()` / `hiltViewModel(viewModelStoreOwner = activity)` survives every destination, including the whole nav graph. Use it for genuinely app-wide state (auth session, theme). The cost: it survives until the Activity is destroyed — it does **not** survive process death unless its state is in `SavedStateHandle`.

7. **Do not put shared state in a `remember` at the NavHost level.** `remember { }` at the `NavHost` level is scoped to the *composition*, not to the navigation graph — it survives recomposition but is recreated when the Activity is recreated (rotation). The ViewModel is the right owner.

8. **Decide the *ownership* of the shared state explicitly.** For a checkout flow, the shared ViewModel holds the draft order and the *step*; it is cleared when the graph is popped (i.e. the flow is abandoned or completed). If the flow can be resumed later, persist the draft (Room/DataStore) and rehydrate it.

9. **Test the scoping.** A Compose navigation test: navigate to step A, set state, navigate to step B, assert the state is still there; pop the whole graph, assert a fresh instance is created.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Destination-scoped `viewModel()` | Default; state dies with the screen; no leakage between screens | State is lost when navigating to a sibling screen — forces re-fetch or awkward param passing |
| Parent-graph-scoped ViewModel | One instance shared across the flow's steps; cleared when the flow ends | The graph structure becomes load-bearing; a deeply nested graph means the state lives longer than expected |
| Activity-scoped ViewModel | Available everywhere; simple | Lives too long — leaks state across flows, holds references, is not cleared when a flow is abandoned |
| Manual singleton / repository | Trivially shared | Not lifecycle-aware; survives process death incorrectly; global mutable state — a classic source of bugs |
| `remember { }` in the `NavHost` scope | No ViewModel boilerplate | Recreated on rotation; not cleared on navigation; no `viewModelScope` |
| `SharedFlow` in a repository for cross-screen events | Decoupled | Global mutable event bus; events can be missed; hard to reason about ordering |
| Passing state via navigation arguments | Explicit, serialisable | Limited to primitives; large payloads break; a workaround, not a design |
| `SavedStateHandle` for arguments and small state | Survives process death | Bundle size limits; not for large graphs |

**Key detail:** The **`NavBackStackEntry`** is the unit of ViewModel ownership in Navigation Compose. A destination's entry is popped → its ViewModel is cleared. A **parent graph's** entry is popped (when the whole nested graph is popped) → the shared ViewModel is cleared. Choosing the scope means choosing *when the state is thrown away*, which should follow the lifetime of the **concept** (a checkout draft lives as long as the checkout flow), not the lifetime of a screen.

**Code example**

```kotlin
// Shared ViewModel scoped to the parent graph — one instance for all steps.
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "checkout") {
        navigation(route = "checkout", startDestination = "checkout/address") {

            composable("checkout/address") { entry ->
                // entry.parent is the graph's NavBackStackEntry → its ViewModelStore
                val vm: CheckoutViewModel = hiltViewModel(viewModelStoreOwner = entry.parent!!)
                AddressStep(vm)
            }

            composable("checkout/payment") { entry ->
                val vm: CheckoutViewModel = hiltViewModel(viewModelStoreOwner = entry.parent!!)
                PaymentStep(vm)
            }
        }
    }
}

@HiltViewModel
class CheckoutViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    // Shared across steps; cleared when the "checkout" graph is popped.
    private val _draft = MutableStateFlow(Draft())
    val draft: StateFlow<Draft> = _draft.asStateFlow()

    fun onEvent(event: CheckoutEvent) { /* ... */ }
}

// Per-destination ViewModel + navigation argument from the entry's SavedStateHandle.
@Composable
fun DetailRoute(navController: NavHostController) {
    composable(
        route = "detail/{itemId}",
        arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
    ) { entry ->
        val vm: DetailViewModel = hiltViewModel()             // destination-scoped
        val itemId = entry.arguments?.getString("itemId")     // or vm.itemId from SavedStateHandle
        DetailScreen(vm, itemId)
    }
}
```

**Talking points / follow-ups**
- *"When is a destination-scoped ViewModel actually cleared?"* → When its `NavBackStackEntry` is popped (and the transition completes), not on rotation, and not when the app is backgrounded.
- *"What is `entry.parent`?"* → The parent `NavBackStackEntry` — for a destination inside a nested graph, that is the graph's entry, whose `ViewModelStore` is the right owner for shared state.
- *"How do you share state between two unrelated screens?"* → Do **not** use a shared ViewModel; use a repository or a scoped `SavedStateHandle`/database. A shared ViewModel implies a shared lifecycle, and unrelated screens have none.
- *"How do you handle a flow the user can resume later?"* → Persist the draft; the graph-scoped ViewModel becomes a cache over the durable state.
- *"What if two screens need different slices of the same state?"* → Split into two ViewModels with the shared data in a repository — not one fat shared ViewModel with every field.
- *"How do you verify the scope in a test?"* → Compose navigation test: assert the same instance across steps (e.g. compare a `remember`ed instance id or a state value set in step A and read in step B), then pop the graph and assert it is recreated.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Recomposition & invalidation** | 1, 8 — read tracking, stability, `remember` vs `derivedStateOf`, compiler metrics as the diagnostic tool |
| **Effect lifecycle & correctness** | 2, 4, 7 — `LaunchedEffect` restart semantics, lifecycle-aware collection, scoping work to the right owner |
| **Identity, continuity and state** | 3, 6, 11 — stable keys, anchor-by-id, exhaustive state machines, ViewModel scoping |
| **State vs event semantics** | 5 — a `StateFlow` is state; one-shot effects need a `Channel`; exactly-once is impossible across process death |
| **Testability & accessibility** | 9 — stateless screen + stateful route, semantics by construction, test the state matrix |
| **Migration judgment & interop** | 10 — incremental, not rewrite; `ComposeView` is cheap, `AndroidView` is expensive |
| **Staff framing** | 1, 5, 9, 11 — measure first, model state explicitly, decide scope from the data's lifetime, prefer the boring correct tool |

**The one sentence to memorise for this section:**
> *"Compose is not a widget library — it is a recomposition system: the state you read defines what recomposes, the scope you own defines what survives, and the state machine you model defines what the user can possibly see. Every Compose bug is one of those three questions asked incorrectly."*
