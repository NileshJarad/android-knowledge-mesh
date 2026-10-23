# Performance, Large Datasets and UI Rendering

> Answers written from the perspective of a **Staff Android Engineer**. Each answer is structured as:
> *What the interviewer is really asking* → *Key concepts* → *Approach* → *Trade-offs* → *Code* → *Talking points*.

---

### 1. An API returns 1,000 transactions. How would you display them without making the UI lag?

**What the interviewer is really asking**
Can you distinguish between *"the data is large"* and *"the rendering is naive"*? A 1,000-item list is not a large-data problem — it is a **virtualisation** problem. The interviewer wants to see that you know the difference between *paging*, *lazy loading*, and *diffing*, and that you never map 1,000 rows into a single non-lazy container.

**Key concepts**
- `LazyColumn` (Compose) / `RecyclerView` (View system) — only compose/bind visible items
- `items(key = { it.id })` — stable identity for item reuse and animation
- `rememberLazyListState()` — scroll state survives recomposition, not just configuration change
- Off-main-thread parsing + a background dispatcher for the diff
- `ListAdapter` + `DiffUtil` / `AsyncListDiffer` for incremental updates
- Payload-based partial rebinds (`notifyItemChanged(pos, payload)`) instead of full rebinds

**Approach**

1. **Never render 1,000 items eagerly.** The single most important decision. `LazyColumn` composes only what is on screen plus a small prefetch buffer:
   ```kotlin
   LazyColumn {
       items(
           items = transactions,
           key = { it.id },                 // identity, not position
           contentType = { it.type },       // lets Lazy reuse slots across heterogeneous rows
       ) { tx -> TransactionRow(tx) }
   }
   ```
   Without a `key`, Compose/LazyList uses **position** as identity, so inserting one item at the top re-runs every row's content lambda. With `key`, only the shifted rows recompose.

2. **Keep the list scrollable state out of composition.** `rememberLazyListState()` survives recomposition; `rememberSaveable` survives process death so the user returns to the same position:
   ```kotlin
   val listState = rememberLazyListState()
   LazyColumn(state = listState) { ... }
   ```

3. **Do the parsing and the list building off the main thread.** JSON → list of domain objects is CPU work; on 1,000 items with a heavy model it can be tens of milliseconds. Use `Dispatchers.Default` for the transform and `Dispatchers.IO` for I/O:
   ```kotlin
   viewModelScope.launch {
       val models = withContext(Dispatchers.Default) { parse(rawJson) }
       _uiState.value = UiState.Success(models)
   }
   ```

4. **Use `contentType`** to let the lazy list recycle/compose identical row types. In a heterogeneous feed (header, transaction, promo) `contentType` prevents a header slot from being reused for a transaction row.

5. **Diff instead of replacing.** If the list is updated (polling, live feed), replace-all triggers a full rebind of every row. Use `ListAdapter`/`AsyncListDiffer` so only changed rows rebind:
   ```kotlin
   class TransactionAdapter : ListAdapter<Tx, VH>(DIFF) { ... }
   ```
   In Compose, `LazyColumn` already diffs the *item list* by key, but the *contents* of each row still recompose — so keep the row's inputs stable (`@Immutable` data class) so Compose can skip it.

6. **Use payloads for partial rebinds** in the View system. `notifyItemChanged(position, payload)` rebinds only the changed text field instead of the entire row layout.

7. **Measure it.** `adb shell dumpsys gfxinfo <package> framestats` gives per-frame durations; the Android Studio **System Trace** (`Profiler > System Trace` / Perfetto) shows `Choreographer` frame callbacks, and **JankStats** in debug builds gives a live jank-rate readout in-app.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `LazyColumn` / `RecyclerView` | Only visible items are composed; constant memory regardless of list size | Requires stable keys and consistent item heights to be effective; nested lazy lists in scrollable columns throw |
| Rendering all 1,000 items eagerly | Simple code, instant scroll position | Full layout + compose cost on first frame; obvious jank on low-end devices. Wrong for 1,000; catastrophic for 100,000 |
| `items(key = { it.id })` | Correct identity; enables item animations and minimal recomposition | Keys must be unique and stable — using an index as key silently defeats it |
| `contentType` | Slot reuse across row types | Only a hint; the real win comes from stable `key`s |
| `ListAdapter` / `AsyncListDiffer` | Incremental, off-thread diffing | Diff is O(N) in list size; for very frequent updates it can still be a bottleneck (see Q11) |
| Off-main-thread parsing | Keeps the frame budget intact | Adds coroutine plumbing; ordering must be preserved explicitly |

**Key detail:** `LazyColumn` measures lazily, but `items { ... }` content lambdas are still recomposed whenever *any* observed state used inside them changes. Keep per-item state read inside the item, not in the parent scope, or the whole list recomposes.

**Code example**

```kotlin
@Composable
fun TransactionsList(transactions: List<Transaction>) {
    // rememberLazyListState survives recomposition; rememberSaveable survives process death.
    val listState = rememberLazyListState()

    LazyColumn(state = listState) {
        items(
            items = transactions,
            key = { it.id },
            contentType = { it.kind },
        ) { tx ->
            TransactionRow(
                tx = tx,
                // Modifier.animateItem() animates insert/remove/reorder (Compose 1.7+)
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Immutable
data class Transaction(
    val id: String,
    val kind: String,
    val amountCents: Long,
    val merchant: String,
)
```

**Talking points / follow-ups**
- *"Is `LazyColumn` the same as `RecyclerView`?"* → Conceptually yes (lazy composition of visible items), but Compose's implementation is not RecyclerView under the hood — it reuses slot state via `key` and the composition, and it has no adapter/ViewHolder indirection.
- *"What if items have wildly different heights?"* → Fine for `LazyColumn`; it measures each item. But avoid nesting a `LazyColumn` inside a vertically scrollable parent — that throws.
- *"How do you know the list is actually smooth?"* → JankStats (in debug) and `dumpsys gfxinfo` for jank stats; Macrobenchmark for regression gates in CI.
- *"What if you need to scroll to an item?"* → `listState.animateScrollToItem(index)` — but you must first fetch that page (see Q2).
- *"Does `DiffUtil` block the main thread?"* → `DiffUtil.calculateDiff` is O(N) and must run on a background thread; `AsyncListDiffer` does that for you and `ListAdapter` wires it to `RecyclerView`.

---

### 2. An API returns 100,000 records. How would you design the data-loading and rendering strategy?

**What the interviewer is really asking**
This is a *systems* question disguised as a UI question. The interviewer wants to see that you refuse to download 100,000 records into memory, and that you know the whole stack: **Paging 3** (`PagingSource` + `RemoteMediator`), a **Room** cache as the single source of truth, keyset/cursor pagination (not `OFFSET`), and a UI that renders only a window.

**Key concepts**
- Paging 3: `Pager`, `PagingSource`, `PagingData`, `RemoteMediator`
- Keyset (cursor) pagination vs `LIMIT/OFFSET` — offsets skip/duplicate rows when the data mutates
- Room as the cache / single source of truth; `PagingSource` from Room (`RoomPagingSource`)
- `LazyPagingItems` in Compose, `PagingDataAdapter` in the View system
- `initialLoadSize` / `pageSize` / `prefetchDistance` / `maxSize`
- `placeholdersEnabled` — avoid enabling when items are heterogeneous (causes layout jumps)

**Approach**

1. **Never fetch 100,000 rows.** The server must paginate. Ask for a keyset/cursor API:
   ```
   GET /transactions?limit=50&cursor=<opaque>
   → { items: [...], nextCursor: "eyJpZCI6..." }
   ```
   `OFFSET` pagination is O(offset) on the server and is *wrong* under concurrent inserts — rows shift and you get duplicates/skips. Insist on a cursor derived from a stable, unique, monotonically-sortable key (`(createdAt, id)`).

2. **Use Paging 3.** It gives you: placeholders, prefetching, retry, `RemoteMediator` for network+cache, and an infinite-scroll-ready API that integrates with `LazyColumn` via `collectAsLazyPagingItems()`.

3. **Use Room as the cache so the UI reads from the DB, not the network.** `RemoteMediator` splits the work: `PagingSource` (DB) supplies the UI; `RemoteMediator.load()` fetches from the network and writes into Room, which invalidates the DB `PagingSource` and pushes new items down automatically. The UI then works offline and instantly.

4. **Tune the paging parameters.** `pageSize` ~50–100 items; `prefetchDistance` ~10–20 pages worth of items *before* the user reaches the end (it is measured in items, not pages); `initialLoadSize` defaults to `pageSize * 3` — keep it modest on slow networks; `maxSize` bounds the in-memory page cache (a few hundred items).

5. **Render with `LazyPagingItems`** — it is lazy, gives you `itemKey` for identity, and supports `itemContentType`:
   ```kotlin
   LazyColumn {
       items(
           count = pagingItems.itemCount,
           key = pagingItems.itemKey { it.id },
           contentType = pagingItems.itemContentType { it.kind },
       ) { index -> pagingItems[index]?.let { TransactionRow(it) } }
   }
   ```

6. **Choose the data shape for the list.** A 100,000-row transaction list should almost never render 100,000 rows — it should be *filtered, grouped, and summarised* (per-day sections, per-merchant totals). Part of the staff answer is pushing back on the requirement: *"Do we need 100,000 rows of detail, or a queryable, aggregatable view of them?"*

7. **For aggregation-heavy needs, push it to SQL.** A `GROUP BY` in SQLite over an indexed column is dramatically faster than aggregating in Kotlin over 100,000 deserialised objects. Room supports `@RawQuery` for dynamic queries and `EXPLAIN QUERY PLAN` for verification.

8. **Virtualise everything downstream.** If the 100,000 rows feed a chart or a calendar, do not materialise the list at all — query only the visible window (see Q14).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Paging 3 with `RemoteMediator` | Cached, offline-capable, incremental, standard | `RemoteMediator` invalidation logic is subtle; a bad `getRefreshKey` causes infinite loops |
| `LIMIT/OFFSET` paging | Trivial server-side, allows jumping to page N | Degrades linearly with offset; duplicates/skips under concurrent writes |
| Keyset / cursor paging | Constant cost, stable under mutation, no duplicates | No random page access; cursor must be opaque and server-authoritative |
| Room as the source of truth | Offline-first, one code path, queryable | Write amplification; DB size grows (needs pruning/archival) |
| Loading everything and filtering in memory | Simplest UI code | 100,000 objects → memory pressure, GC churn, seconds of parse time. Not acceptable |
| Pushing aggregation to SQL | Orders of magnitude faster for counts/totals | SQL complexity; query plan must be verified with `EXPLAIN QUERY PLAN` |
| `placeholdersEnabled = true` | No blank cells while loading | Causes layout jumps with variable-height rows; often better left off for heterogeneous lists |

**Key detail:** `RemoteMediator` must implement `getRefreshKey()` correctly (usually `state.anchorPosition?.let { pagingState.closestPageToPosition(it) }`) — a wrong implementation causes an infinite refresh loop, which is the single most common Paging 3 bug.

**Code example**

```kotlin
class TransactionRemoteMediator(
    private val api: TransactionApi,
    private val dao: TransactionDao,
) : RemoteMediator<Int, Transaction>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Transaction>,
    ): MediatorResult = try {
        val page = when (loadType) {
            LoadType.REFRESH -> api.page(cursor = null, limit = state.config.pageSize)
            LoadType.PREPEND -> return MediatorResult.Success(endOfPagination = true)
            LoadType.APPEND -> {
                val last = state.endOfPaginationReached.not()
                    .let { state.getRefreshKey(state) } ?: return MediatorResult.Success(true)
                val cursor = dao.lastCursor() ?: return MediatorResult.Success(true)
                api.page(cursor = cursor, limit = state.config.pageSize)
            }
        }
        // Single writer: Room invalidates the DB PagingSource, which pushes to the UI.
        dao.transaction {
            dao.insertAll(page.items)
            dao.setCursor(page.nextCursor)
        }
        MediatorResult.Success(endOfPagination = page.nextCursor == null)
    } catch (e: IOException) {
        MediatorResult.Error(e)
    }
}

@Composable
fun PagedTransactions(pagingItems: LazyPagingItems<Transaction>) {
    LazyColumn {
        items(
            count = pagingItems.itemCount,
            key = pagingItems.itemKey { it.id },
            contentType = pagingItems.itemContentType { it.kind },
        ) { index ->
            pagingItems[index]?.let { TransactionRow(it) }
        }
    }
}
```

**Talking points / follow-ups**
- *"Why not just use `LazyColumn` with 100,000 items fetched once?"* → Memory (~100–200 bytes/row minimum, more with strings) plus parse time plus a JSON payload of tens of MB. On a low-end device that is an OOM, not a slowdown.
- *"What is `PagingData`'s `withLoadStateHeader`/`withLoadStateFooter` for?"* → Spinner / retry UI at the ends of the list, driven by `LoadState` — including `LoadState.Error` with a `retry()` action.
- *"How do you support 'jump to date' in a cursor-paged list?"* → A separate query parameter (`?at=<timestamp>`) that starts a new cursor stream from that point — the server computes the anchor.
- *"Do you need `RemoteMediator` at all?"* → Only if you want a DB cache. `PagingSource` alone (network-only) is simpler but loses offline support and repeats requests on config change.
- *"How do you test paging?"* → `PagingSource` is easy to unit-test with a fake; use `androidx.paging.testing` (`asSnapshot()`) to assert the sequence of loaded pages.

---

### 3. A RecyclerView scrolls smoothly initially but starts lagging after several minutes. How would you investigate the issue?

**What the interviewer is really asking**
This is a **profiling** question disguised as a UI question. The interviewer wants a *hypothesis-driven investigation order*: reproduce → measure → attribute → fix → verify. The "smooth at first, laggy later" signature has a small set of classic causes (unbounded caches, leaks, GC pressure, growing diffs, image cache growth), and the staff answer is *"measure before you guess"*.

**Key concepts**
- `adb shell dumpsys gfxinfo <pkg>` — frame stats, jank percentage, `Profile data in ms`
- Perfetto / Android Studio System Trace — main-thread, render thread, GC pauses
- `Choreographer` frame callbacks; 16.67 ms budget at 60 Hz, 8.33 ms at 120 Hz
- `FrameMetrics` API — `TOTAL_DURATION`, `INPUT_DURATION`, `DRAW` timings per frame
- JankStats (Compose + View) — live jank rate in debug builds
- LeakCanary — retained objects, `MemoryProfiler` heap dump
- `RecyclerView.RecycledViewPool`, `setHasFixedSize`, `setItemViewCacheSize`
- GC pressure and allocation in `onBindViewHolder`

**Approach**

1. **Reproduce with the same duration.** Scroll for the same 5–10 minutes on a device you suspect. "After several minutes" is a strong hint: it is *not* a per-frame cost issue, it is an *accumulation* issue.

2. **Measure frame timing.** Turn on `Profile GPU rendering` in Developer Options (or run `adb shell dumpsys gfxinfo <pkg> reset`, scroll, then `adb shell dumpsys gfxinfo <pkg>`) and look at:
   - **Janky frames %** and the 95th/99th percentile frame time
   - **`Draw`/`Prepare`/`Process`/`Execute`** columns: if `Process` (UI thread) is high, the problem is in your code; if `Draw` is high, it is overdraw or too many GPU ops.
   - `adb shell dumpsys gfxinfo <pkg> framestats` gives a per-frame trace you can load in Perfetto.

3. **Get a system trace.** Perfetto with `android.surfaceflinger`, `android.gpu.memory`, `com.android.systemui` and the app's own processes. Look for:
   - Long main-thread slices in `onBindViewHolder`
   - **GC pauses** (the classic "degrades over time" signature)
   - **Bitmap allocations** in the bind path (allocation churn → GC → jank)

4. **Check for leaks with LeakCanary.** Install it in the debug build; if a retained `Activity`/`View` accumulates per scroll, you leak. Also check `adb shell dumpsys meminfo <pkg>` for heap growth over the scroll.

5. **Check for unbounded caches.** The most common accumulation bugs:
   - A `HashMap`/`LruCache` in the image loader with no bound → use `LruCache` sized as a fraction of `maxMemory`.
   - A `List` of loaded items that keeps growing because you `add` instead of `set`.
   - `RecyclerView` adapter holding the full data set plus a second copy in a diff list (`AsyncListDiffer` keeps `oldList` and `newList` — 2x memory during a diff).
   - `DiffUtil` on a 100,000-item list — the diff itself is O(N) and runs on every poll.

6. **Check `setHasFixedSize(true)`** and item heights. If the row layout height varies, RecyclerView must re-measure on every bind. If the size of the RecyclerView itself never changes, `setHasFixedSize(true)` removes an entire measure/layout pass.

7. **Check `onBindViewHolder` for per-bind work.** Anything expensive there (string formatting, `DateFormat` creation, `Spannable` building, bitmap decode, `notifyDataSetChanged`) is called on every bind — and binds happen continuously while scrolling. Hoist expensive objects (formatters, painters) to fields.

8. **Check `notifyDataSetChanged()`** being called periodically (polling). It invalidates everything, forces a full rebind, and triggers a diff. Replace with `DiffUtil`/`ListAdapter`.

9. **Check `RecycledViewPool` sharing** if you have multiple RecyclerViews (nested lists, grids inside lists) — by default each RecyclerView has its own pool, so a grid inside a list re-creates views instead of reusing them.

10. **Verify the fix with the same measurement.** Re-measure the 95th-percentile frame time over the same 10-minute scroll. If jank went from 8% to <1% and heap is flat, you are done. Add a **Macrobenchmark** so this never regresses.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `ListAdapter` + `DiffUtil` instead of `notifyDataSetChanged` | Incremental rebinds, no full invalidation | Diff cost is O(N); needs stable IDs and a real `equals`/`areItemsTheSame` |
| `setHasFixedSize(true)` | Removes a measure/layout pass | Wrong if the RecyclerView size genuinely changes; no effect if it already was fixed |
| `RecycledViewPool` shared across lists | Fewer view inflations | Views must be layout-compatible; subtle bugs when not |
| `LruCache` sized to `maxMemory` | Bounded memory | Eviction causes re-decode; size must be tuned per screen |
| Manual `onViewRecycled` cleanup | Releases listeners, cancels requests | Must be implemented per-holder; forgetting it leaks |
| System trace over heap dump | Shows *where* time goes, incl. GC and RenderThread | Requires a repro and a trace-reading skill |

**Key detail:** "Smooth at first, then slow" almost always means **accumulation** — a growing collection, a growing cache, a leak, or GC pressure — not a constant per-frame cost. That single inference narrows the search space enormously and is the thing to say out loud in the interview.

**Code example**

```kotlin
class TxAdapter : ListAdapter<Tx, TxAdapter.VH>(DIFF) {

    // Expensive, reusable objects hoisted OUT of onBindViewHolder.
    private val formatter = DateTimeFormatter.ofPattern("dd MMM")
    private val locale = Locale.getDefault()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTxBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position), formatter, locale)   // no allocation of formatters
    }

    // Release per-holder resources (image requests, listeners) to avoid leaks.
    override fun onViewRecycled(holder: VH) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Tx>() {
            override fun areItemsTheSame(a: Tx, b: Tx) = a.id == b.id
            override fun areContentsTheSame(a: Tx, b: Tx) = a == b
        }
    }
}
```

**Talking points / follow-ups**
- *"What jank percentage is acceptable?"* → Google Play's vitals threshold is not "no jank" but bad-frame rate; industry practice is <1% janky frames on the median user's device, and definitely <5% at p95.
- *"What is the frame budget on a 120 Hz device?"* → 8.33 ms, not 16.67. Low-end devices often have 60 Hz panels but weak SoCs, so the budget is 16.67 ms with much less headroom.
- *"How do you capture a trace?"* → `adb shell perfetto -o trace -t 30s` with a config, or the Android Studio System Trace panel; `dumpsys gfxinfo framestats` for a quick-and-dirty CSV.
- *"Is `notifyDataSetChanged` really that bad?"* → It invalidates every holder and forces a full rebind + a layout pass. At 10,000 items it is visibly janky.
- *"What is `FrameMetrics`?"* → The framework API behind `dumpsys gfxinfo` — you can register a `Window.OnFrameMetricsAvailableListener` and get `TOTAL_DURATION`, `INPUT_DURATION`, and (API 29+) `DEADLINE`/`MISSED_VSYNC` to attribute jank precisely.

---

### 4. A Jetpack Compose screen recomposes too frequently. How would you identify and reduce unnecessary recompositions?

**What the interviewer is really asking**
Do you understand the Compose **recomposition model** — scope-based, skippable, restartable — and can you use the *tools* (compiler metrics, `trace` markers, `Compose Compiler Report`) rather than guess? A junior answer is "use `remember`". A staff answer distinguishes **recomposition** (cheap, scope-local) from **recomposition of the wrong scope** (expensive) and from **restart** (side effects re-run).

**Key concepts**
- Recomposition is scope-based: only the smallest `@Composable` scope that reads changed state recomposes
- Skippability: a composable is skipped when its parameters are unchanged and it is not reading other state
- Stability: `@Stable` / `@Immutable` contracts tell the compiler it can skip
- `remember` vs `rememberUpdatedState` vs `derivedStateOf`
- `LaunchedEffect` — keyed side effects, re-launched when keys change
- `snapshotFlow` — observe snapshot state in a Flow
- `key()` — identity within a `forEach`/composition
- Compose Compiler metrics (`-P plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination`) — unstable/skip counts

**Approach**

1. **Measure before changing anything.** Turn on Compose compiler metrics to see which composables are unstable:
   ```
   kotlinCompilerPluginArgs {
       freeCompilerArgs += "-P",
           "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=build/compose-metrics.json"
   }
   ```
   This reports `unstable classes` and `composables that were restarted but not skippable`. A high count of "restarted" composables is your list of suspects.

2. **Use the Layout Inspector / Recomposition counts.** In Android Studio's Layout Inspector you can enable **"Recomposition counts"** in the Compose section to see how many times each composable has recomposed. A composable recomposing on every frame is the smoking gun.

3. **Find the state read too high in the tree.** The classic bug: a `Text()` reads a `State` that changes every second (a clock), forcing the whole subtree to recompose. Move the read down to the smallest scope that needs it:
   ```kotlin
   // BAD: the entire list recomposes because the parent reads the ticking state.
   @Composable fun Screen() {
       val seconds by remember { mutableStateOf(0) }  // updates every second
       Header(seconds)          // reads it
       HugeList(items)          // recomposed too, because it is in the same scope
   }
   ```
   The fix is to **push the read down**: pass the `State` (not its value) down, or wrap the ticking part in its own composable.

4. **Pass `State<T>` instead of `T` when the value changes often.** A composable that receives a `State<T>` reads it at the point of use and skips recomposition of its siblings:
   ```kotlin
   @Composable fun Counter(state: State<Int>) {
       Text("${state.value}")   // only the Text recomposes when state changes
   }
   ```

5. **Stabilise your types.** If a model class is not `@Immutable`/`@Stable`, the compiler treats its properties as unstable and cannot skip the composable. Annotate immutable data classes:
   ```kotlin
   @Immutable
   data class Transaction(val id: String, val amountCents: Long, val merchant: String)
   ```
   Beware: `@Immutable` is a *promise*. If you mutate a field, you break the compiler's assumptions and get skipped frames (a hard bug).

6. **Use `derivedStateOf` for expensive derived values** so the recomposition is triggered by the *result*, not the inputs:
   ```kotlin
   val filtered by remember { derivedStateOf { items.filter { it.matches(query) } } }
   ```
   Note: `derivedStateOf` does not help if the derived value is computed *during* composition of a large subtree — it only helps when the dependency changes rarely relative to the input.

7. **`remember` expensive objects, but key it correctly.** `remember { DateFormat.getInstance() }` avoids re-creating a formatter on every recomposition. `remember(key)` recomputes when the key changes. Use `rememberUpdatedState` for a callback that must always be current but must not restart the effect:
   ```kotlin
   val currentOnClick by rememberUpdatedState(onClick)
   LaunchedEffect(Unit) { buttonClicks.collect { currentOnClick() } }
   ```

8. **Use `LaunchedEffect` with correct keys.** An effect with no key runs once per composition lifetime; with a key it is cancelled and relaunched when the key changes. Wrong keys cause effect churn (e.g. restarting a network call on every recomposition):
   ```kotlin
   LaunchedEffect(userId) { viewModel.load(userId) }   // key = the real dependency
   ```

9. **Stabilise lambdas passed as parameters.** A lambda that is re-created on every recomposition breaks skippability of the callee. Either hoist it (`val onClick = { ... }` inside a `remember`), or mark the parameter with `@Composable` lambda types / use `remember` for the callback reference.

10. **Avoid reading `State` in the middle of a `LazyColumn` item list.** Read it inside the item.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Pushing state reads down to the smallest scope | Precise, idiomatic, minimal recomposition | Requires understanding of scope boundaries; can fragment the composable tree |
| Passing `State<T>` down instead of `T` | Sibling composables skip cleanly | Less testable/readable; consumers must remember to read `.value` |
| `@Immutable` / `@Stable` annotations | Lets the compiler skip aggressively | A lie about mutability causes stale UI — a real source of production bugs |
| `derivedStateOf` | Recompute only when the result changes | No benefit if the derived value is read high in the tree; small overhead per read |
| `remember` for expensive objects | Avoids re-allocation | Stale state if the captured value should change — use `remember(key)` |
| `derivedStateOf` / `remember` inside a `LazyColumn` item | Item-scoped state, no list-wide churn | Requires per-item keys to be correct |

**Key detail:** Recomposition is not the same as **recomposition of the parent**. Compose skips a composable only if (a) it is skippable, (b) all its parameters are unchanged, and (c) it does not read any state that changed. Any one of those failing recomposes it — and `remember` alone does not make it skippable.

**Code example**

```kotlin
@Immutable
data class Transaction(val id: String, val amountCents: Long, val merchant: String)

@Composable
fun TransactionList(
    transactions: List<Transaction>,      // @Immutable elements → list is stable
    query: State<String>,                 // read down here, not up in the parent
    onClick: (String) -> Unit,
) {
    // Hoisted, stable callback → the callee can be skipped.
    val stableOnClick = remember(onClick) { onClick }

    LazyColumn {
        items(items = transactions, key = { it.id }) { tx ->
            TransactionRow(tx = tx, onClick = stableOnClick)
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction, onClick: (String) -> Unit) {
    var expanded by remember(tx.id) { mutableStateOf(false) }   // keyed per item
    Column(Modifier.clickable { onClick(tx.id) }) {
        Text(tx.merchant)
        if (expanded) {
            Text("${tx.amountCents}")   // only this subtree recomposes on expand
        }
    }
}
```

**Talking points / follow-ups**
- *"Is recomposition expensive?"* → Recomposition itself is cheap-ish; the cost is in what the recomposed composable does (allocation, layout, image decode, list diffing). So "reduce recomposition" is really "reduce recomposition of expensive scopes".
- *"What is a restart vs a recomposition?"* → A *restart* re-runs the `remember`ed values and `LaunchedEffect`s of a skipped-invalid composable. Compose tries to skip; when it cannot, it recomposes and, for effectful composables, restarts effects.
- *"Does `Modifier` ordering matter?"* → It matters for the *modifier chain* cost, not recomposition, but a `Modifier.clickable` after a `padding` vs before changes hit-testing behaviour.
- *"How do you prove a fix?"* → Compose compiler metrics + Layout Inspector recomposition counts, plus a Macrobenchmark that scrolls the screen and asserts the frame-time metric. Recomposition count alone is not a perf metric — it is a hint.
- *"What about `ComposeView` interop with a View hierarchy?"* → View interop adds a `AndroidView`/`AndroidViewBinding` composition boundary; state reads across it recompose the interop node. Keep interop leaves small.

---

### 5. A screen displays images in a scrolling list, and scrolling becomes slow. How would you optimize image loading and caching?

**What the interviewer is really asking**
Image loading is the classic source of scroll jank: **main-thread decode**, **oversized bitmaps**, **cache misses during fast fling**, and **GC churn**. The interviewer wants to see that you know the modern answer — a dedicated image library with an **in-memory LruCache sized from `maxMemory`**, a **disk cache**, **downsampling at decode time**, and **`AsyncImage`/prefetching** — and that you can compute the correct target bitmap size rather than shipping a full-resolution bitmap into a 200 dp box.

**Key concepts**
- Coil 3 / Glide — decode off the main thread, memory + disk cache, request coalescing
- `BitmapFactory.Options` — `inSampleSize`, `inPreferredConfig` (`RGB_565` for opaque images), `inMutable`
- Bitmap memory: `width * height * 4 bytes` (ARGB_8888); a 4000×3000 photo = ~48 MB
- `ImageRequest` sizing (`ContentScale`, target size), `sizeResolver`
- `LruCache` sized from `ActivityManager.memoryClass` / `Runtime.maxMemory()`
- Disk cache, `CacheControl`, `DiskCacheStrategy`
- Prefetching during fling; `LazyListState.layoutInfo` for prefetch hints
- `androidx.compose.ui.graphics.ImageBitmap` / `painterResource` vs network images

**Approach**

1. **Measure first.** Profile the scroll with Perfetto/System Trace and look for: main-thread slices in `BitmapFactory.decode*` (the #1 cause), bitmap allocation in the profiler, and GC pauses. Also check `dumpsys meminfo` for the `Graphics`/`Asset`/heap growth as you scroll.

2. **Never decode on the main thread.** `BitmapFactory.decodeStream` on a 5 MB JPEG takes tens to hundreds of ms. Use Coil (or Glide). Coil 3 does its work on `Dispatchers.IO` and returns a `Painter`.

3. **Downsample to the display size.** This is the biggest single win. Compute the decode target from the view size and the screen density, then use `inSampleSize` to decode ~1/4 the pixels:
   ```kotlin
   val reqW = imageView.width                     // target px
   val reqH = imageView.height
   val opts = BitmapFactory.Options().apply {
       inJustDecodeBounds = true
   }
   BitmapFactory.decodeStream(url.openStream(), null, opts)
   opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, reqW, reqH)
   opts.inJustDecodeBounds = false
   ```
   `calculateInSampleSize` rounds the power of two down so the decoded bitmap is still >= the requested size (so it can be scaled up slightly).

4. **Size the memory cache from the device, not from a constant.** A hardcoded `LruCache(10 * 1024)` (count-based, kilobytes) is wrong for modern devices. Size it as a fraction of the app's heap:
   ```kotlin
   val maxMem = (Runtime.getRuntime().maxMemory() / 1024).toInt()
   val cacheSize = (maxMem * 0.20).toInt()   // ~20% of heap
   ```
   Coil 3 does this for you with `MemoryCache.Builder(context).maxSizePercent(0.25)`.

5. **Two-level caching.** Memory cache (LRU, in-process) → disk cache (encrypted if sensitive) → network. The disk cache is what makes a re-open instant and what saves data. Set `CacheControl`/`maxAge` and `DiskCacheStrategy.AUTOMATIC`.

6. **Coalesce duplicate requests.** Coil dedupes identical requests for the same target automatically; if you build your own layer, key requests by `(url, targetSize, transformations)` so a 40-item list with the same avatar does not decode 40 times.

7. **Prefetch during fast scroll.** When the user flings, the list needs images *before* the item is composed. Options:
   - Coil's `AsyncImage` with a `sizeResolver` that requests a slightly larger bitmap than the slot (avoids a re-decode on rotation).
   - Prefetch the next screenful using `LazyListState.layoutInfo.visibleItemsInfo` and issue `ImageRequest`s ahead of the viewport.
   - Set `LazyColumn`'s `beyondViewport` count if supported by your version.

8. **Do not put the image in a `remember` that survives recomposition of the wrong scope** — use the library's `AsyncImage` with a stable `model` key.

9. **Consider `RGB_565`** for opaque photos with no alpha — it halves memory (2 bytes/px instead of 4). Do not use it for images needing alpha or gradients.

10. **Measure the fix** with a Macrobenchmark that scrolls a list of images and asserts `frameTimeCpuMs` and `jank`.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Coil / Glide instead of manual decoding | Off-main-thread decode, caching, request coalescing, lifecycle-aware | Adds a dependency; less control over exotic pipelines (custom transforms, video frames) |
| Decode at display size (`inSampleSize`) | Massive memory + CPU saving | Slightly blurry if you over-downsample; compute the size correctly |
| 20% of heap for the memory cache | Adapts to device memory class | Still needs invalidation on trim; `onTrimMemory` matters for backgrounded apps |
| Disk cache | Instant re-open, works offline, saves data | Needs a max size and an eviction policy; stale images are a product issue |
| Prefetching | Removes blank/flash frames during flings | Wastes bandwidth and CPU on items the user may never reach |
| `RGB_565` | Halves bitmap memory | No alpha, visible banding on gradients |
| `androidx.compose.ui.res.painterResource` for local drawables | Zero-cost, no decode at runtime | Drawables are decoded at full size into the resource cache — be careful with large PNGs |

**Key detail:** Bitmap memory is `w * h * 4` bytes, independent of file size. A 3 MB JPEG can decode to a 48 MB bitmap. Any image pipeline that decodes to full resolution is a memory bug waiting to happen.

**Code example**

```kotlin
@Composable
fun TransactionImage(url: String, contentScale: ContentScale = ContentScale.Crop) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .size(Size.ORIGINAL)            // let the loader downsample to the slot
            .build(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f),
    )
}

// Manual downsample when you need to own the pipeline.
fun decodeSampled(resolver: ContentResolver, uri: Uri, reqW: Int, reqH: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= reqW && bounds.outHeight / (sample * 2) >= reqH) {
        sample *= 2
    }
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = if (hasAlpha) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
    }
    return resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts)!! }
}
```

**Talking points / follow-ups**
- *"Coil 2 vs Coil 3?"* → Coil 3 is multiplatform and has a cleaner `ImageLoader`/singleton API; Coil 2 is the widely deployed 2.x line. Both are safe; do not have both.
- *"How big should the memory cache be?"* → A percentage of `Runtime.maxMemory()`; on a modern device 128–256 MB is fine. Combine with `onTrimMemory(TRIM_MEMORY_RUNNING_LOW)` to evict.
- *"What about `ContentScale.Crop` vs `Fit`?"* → `Crop` fills the slot (may crop); `Fit` fits inside (letterboxes). `Crop` means the request size is exactly the slot size.
- *"Do placeholder animations cost?"* → Crossfades are GPU work; they are fine, but hundreds of simultaneous crossfades can hurt. `rememberAsyncImagePainter` state can be used to skip the fade for cached results.
- *"How do you test image performance?"* → A Macrobenchmark scrolling an image-heavy list, plus a memory assertion (`Runtime.totalMemory - freeMemory` delta) — `Macrobenchmark` has a `MemoryUsageMetric`.

---

### 6. The app freezes while parsing a large JSON response. How would you identify and fix the problem?

**What the interviewer is really asking**
Classic **main-thread I/O + CPU** problem. The interviewer wants to see that you (a) locate the freeze precisely (main thread blocked in `read`/`decode`), (b) move *both* the I/O and the parse off the main thread, and (c) consider streaming/alternative parsers for large payloads — plus the anti-pattern of `JSONObject`/`JSONArray` on large arrays.

**Key concepts**
- Main-thread network I/O → `NetworkOnMainThreadException`
- `Dispatchers.IO` for blocking reads, `Dispatchers.Default` for CPU-bound parsing
- `org.json` vs Moshi vs kotlinx.serialization vs Gson — reflection vs code-generated
- Streaming parsers: `JsonReader` (Okio), `JsonReader`/`decodeFromStream` (kotlinx.serialization), Retrofit's `StreamingResponseBody`
- `InstantTaskExecutorRule` / `runTest` with `StandardTestDispatcher` for tests
- `StrictMode` — `detectDiskReads`, `detectDiskWrites`, `detectNetwork`, penaltyDeath for debug
- `trace` / `androidx.tracing` for measuring the parse
- Payload size limits, gzip, field selection (server returns only what the screen needs)

**Approach**

1. **Confirm it is the main thread.** Enable **StrictMode** in the debug build:
   ```kotlin
   StrictMode.setThreadPolicy(
       StrictMode.ThreadPolicy.Builder()
           .detectAll().detectNetwork().penaltyLog().build()
   )
   ```
   A `NetworkOnMainThreadException` or a StrictMode disk-read violation on the main thread is the smoking gun. Use `adb logcat` to see it.

2. **Get the exact hot method.** Android Studio **Profiler → CPU / Live Edit**, or a **method-trace** (`Debug.startMethodTracingSampling`) on the release-like build. You will almost always find `java.io.InputStream.read` or `org.json.JSONTokener.nextValue` inside `String`/`JSONObject` construction.

3. **Move the whole pipeline off the main thread.** The key mistake people make is moving only the network call but parsing on the main thread:
   ```kotlin
   viewModelScope.launch {
       val raw = withContext(Dispatchers.IO) { api.get().body()?.string() }  // IO
       val model = withContext(Dispatchers.Default) { parse(raw) }          // CPU
       _state.value = UiState.Success(model)
   }
   ```
   `Dispatchers.IO` for the read (elastic, blocking), `Dispatchers.Default` for the parse (bounded by CPU cores).

4. **Use a code-generated parser, not reflection.** `org.json` and `Gson` use reflection and build intermediate trees; Moshi and kotlinx.serialization generate adapters at compile time and skip the intermediate tree entirely. For a large array, this is often a 3–10x difference.

5. **Stream instead of buffering.** For a genuinely large payload, avoid `Response.body().string()` (which materialises the entire body as one `String` — a second copy) and parse incrementally:
   ```kotlin
   @Streaming
   @GET("events")
   suspend fun events(): Response<ResponseBody>
   ```
   then wrap the body in a `JsonReader` and decode one element at a time, emitting into a `Flow` or a Room insert. This keeps peak memory to one item rather than the whole document.

6. **Stop at the API boundary.** The strongest fix: the server should not return 100,000 records when the screen shows 50. Add pagination/filtering/field-selection to the API contract (see Q2). A 20 MB JSON response is a server bug as much as a client bug.

7. **Reuse the buffer and avoid copies.** `String` → `ByteArray` → `String` is 3 copies of your payload. If you must parse a `String`, use a `JsonReader` over `StringReader` and avoid `JSONObject` (which re-scans).

8. **Consider the wire format.** CBOR/protobuf/MessagePack parse faster and smaller than JSON for structured data. If you control both ends, that is a legitimate platform-level answer.

9. **Bound the work.** Cap the payload size (reject >N MB), and consider `withTimeout` so a pathological response cannot freeze the UI indefinitely.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Parse on `Dispatchers.Default` | Simple, fixes the freeze | Still holds the whole payload in memory; does not reduce peak memory |
| Streaming parse | Constant-ish memory, can emit results incrementally | More complex code; partial-failure handling; must cancel properly |
| Moshi / kotlinx.serialization | 3–10x faster than reflection-based parsers | Requires codegen (kapt/KSP) or `@Serializable` annotations; Moshi reflection mode is not the fast path |
| `org.json` | Zero dependencies, built in | Reflection, intermediate trees, `JSONObject` re-scans the string — slow on large arrays |
| Change the API to paginate | Best fix: less data on the wire | Requires backend work; needs a migration plan for old clients |
| gzip response | Smaller payload, less parse work | CPU cost for decompression; automatic in OkHttp |

**Key detail:** `Response.body().string()` reads the **entire** body into a `String` and closes the stream. If you then parse it, you have the payload in memory at least twice, plus the parser's intermediate structures. Streaming is the only way to make peak memory independent of payload size.

**Code example**

```kotlin
// Retrofit: stream the body instead of buffering it.
interface EventApi {
    @Streaming
    @GET("events")
    suspend fun events(): Response<ResponseBody>
}

class EventRepository(private val api: EventApi, private val dao: EventDao) {
    suspend fun load(): Int = withContext(Dispatchers.IO) {
        val response = api.events()
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        val body = response.body() ?: error("empty body")

        // Streaming parse: one element in memory at a time, no giant String.
        val reader = body.charStream().buffered().let { JsonReader(it) }
        var count = 0
        reader.beginArray()
        while (reader.hasNext()) {
            val event = moshi.adapter(Event::class.java).fromJson(reader)!!  // one item
            dao.insert(event)      // write through to Room as we go
            count++
        }
        reader.close()
        count
    }
}

// Simple (non-streaming) version for medium payloads — still off the main thread.
suspend fun loadAll(): List<Event> = withContext(Dispatchers.Default) {
    val raw = withContext(Dispatchers.IO) { api.events().body()?.string() }
    moshi.adapter(Types.newParameterizedType(List::class.java, Event::class.java))
        .fromJson(raw)!!
}
```

**Talking points / follow-ups**
- *"How do you know the freeze is the parse and not the network?"* → StrictMode / `NetworkOnMainThreadException`; if the network is off-main-thread but the freeze persists, it is the parse. `trace` sections around each stage tell you which one.
- *"What about `Dispatchers.IO` for parsing?"* → It works but is wrong: `IO` has a large limit (64+ threads) meant for blocking calls; CPU-bound work there can starve the IO pool. Use `Default` for CPU.
- *"How do you unit-test the parser?"* → Feed it a fixture file; assert the parsed model. Keep the parser a pure function of `(String) -> T` so it is testable without the network.
- *"Should I parse on a background thread and then publish to the UI?"* → Yes — and publish atomically (one state emission) to avoid partial rendering.
- *"What if the payload is genuinely huge and the user wants all of it?"* → Stream to a local file/DB, then let the UI page over the local store (Paging 3 over Room). The network payload never needs to be in memory as a whole.

---

### 7. A large Room database query blocks the UI. How would you optimize database access?

**What the interviewer is really asking**
Room is already off-main-thread if you use `suspend`/`Flow` DAOs — so "blocks the UI" usually means something else: a query that returns tens of thousands of rows, a query without an index, or a **blocking** DAO call (`@Query` non-suspend, `allowMainThreadQueries()`). The interviewer wants to see you go to `EXPLAIN QUERY PLAN`, add indexes, project only the columns you need, and paginate.

**Key concepts**
- `suspend` DAO functions run on Room's own executor — never `allowMainThreadQueries()`
- `Flow<List<T>>` emits off the main thread but **materialises the whole list**
- Indexes: `@Entity(indices = [...])`, composite index column order, covering indexes
- `EXPLAIN QUERY PLAN` — `SCAN table` vs `SEARCH table USING INDEX`
- Projection: `@Query("SELECT id, amount FROM ...")` returns a lightweight POJO, not the whole entity
- `@Transaction` — atomicity, and it keeps the read on one connection
- `setJournalMode(WRITE_AHEAD_LOGGING)`, `setQueryCallback` for slow-query logging
- `RoomDatabase.Builder.setQueryCallback { sql, bindArgs -> ... }` — the official way to find slow queries
- `invalidate()` / `Flow` invalidation overhead on large tables
- Paging over Room: `RoomPagingSource` with a keyset query

**Approach**

1. **Establish where the block is.** Room's `QueryCallback` (or a SQL trace) tells you exactly which statement is slow:
   ```kotlin
   Room.databaseBuilder(context, AppDb::class.java, "app.db")
       .setQueryCallback { sql, bindArgs ->
           if (sql.contains("transactions")) Timber.d("SQL: $sql args=$bindArgs")
       }
       .build()
   ```
   Enable a `StrictMode` disk-read policy and check for `allowMainThreadQueries()` anywhere in the codebase — that is an instant main-thread hit.

2. **Check the query plan.** In `adb shell sqlite3` against a pulled DB, or via Room's generated code, run:
   ```sql
   EXPLAIN QUERY PLAN SELECT * FROM transactions WHERE account_id = ? ORDER BY created_at DESC;
   ```
   `SCAN transactions` = full table scan = missing index. `SEARCH transactions USING INDEX idx_...` = good.

3. **Add the right index.** Composite index column order must match the query's `WHERE` equality columns first, then the `ORDER BY` columns:
   ```kotlin
   @Entity(
       indices = [
           Index(value = ["account_id", "created_at"]),
           Index(value = ["account_id", "category", "created_at"]),
       ]
   )
   data class Transaction(...)
   ```
   Note the rule of thumb: equality predicates first, then the sort column, then the range column.

4. **Project only what you need.** `SELECT *` on a wide table (with JSON blobs, base64 blobs) is the hidden cost. Query a DTO:
   ```kotlin
   @Query("SELECT id, amount_cents, merchant FROM transactions WHERE account_id = :id")
   fun observeSummary(id: String): Flow<List<TransactionSummary>>
   ```

5. **Never materialise a huge result.** `Flow<List<T>>` on 100,000 rows loads all of them into memory on every emission and re-runs the query on every invalidation. Use **Paging 3 with a keyset query**:
   ```kotlin
   @Query("""
       SELECT * FROM transactions
       WHERE account_id = :accountId AND (created_at, id) < (:cursorCreatedAt, :cursorId)
       ORDER BY created_at DESC, id DESC
       LIMIT :limit
   """)
   fun pagingSource(accountId: String, cursorCreatedAt: Long, cursorId: String, limit: Int): PagingSource<Int, Transaction>
   ```
   A row-value comparison `(created_at, id) < (...)` is the standard keyset predicate; it uses the composite index and avoids `OFFSET`'s O(n) cost.

6. **Avoid `suspend` queries returning giant lists in the ViewModel.** Use `Flow` + `map` + `distinctUntilChanged` so the UI only recomposes on real changes, and let the Room flow do the heavy lifting on Room's executor.

7. **Use `@Transaction` when you need consistency across multiple queries** — and remember it pins the transaction to a single connection, which is why it should be short.

8. **Keep writes batched.** A thousand single-row inserts each trigger their own transaction and each invalidate the Flow. Use `RoomDatabase.withTransaction { }` to batch, and consider `@Insert(onConflict = REPLACE)` with a list.

9. **Enable WAL** (on by default in Room 2.x) so readers do not block writers.

10. **Watch out for Flow invalidation storms.** A `Flow` on a table that is written to frequently re-runs the query for every subscriber. Debounce or batch the writes (a `WorkManager` sync writing every 30s, not every event).

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Indexes | Turns a full scan into a seek; orders results for free | Slower writes, more disk, more memory; each index is a maintenance cost. Over-indexing is a real mistake |
| Projection DTOs | Much less data materialised and copied | Extra mapping layer; a new class per query shape |
| Paging over Room | Bounded memory and query cost | No random access to arbitrary offsets; needs a cursor |
| `suspend` DAO | Room runs it on its executor | Returns the whole list at once — fine for thousands, not for 100k |
| `Flow<List<T>>` | Reactive | **Materialises everything and re-runs on every invalidation** — a common source of "the UI freezes" reports |
| `allowMainThreadQueries()` | Convenient in tests | **Never in production** — it is literally a main-thread DB call |
| Batching writes in one transaction | One fsync, one invalidation | Long transactions block other writers |

**Key detail:** Room's `suspend` functions and `Flow` already run off the main thread. If the UI blocks, the cause is almost always (a) a query plan without an index, (b) a result set too large to materialise, or (c) a `Flow` invalidation storm — not the thread the query runs on.

**Code example**

```kotlin
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["account_id", "created_at"]),   // equality then sort
        Index(value = ["account_id", "category", "created_at"]),
    ],
)
data class Transaction(
    @PrimaryKey val id: String,
    val accountId: String,
    val category: String,
    val createdAt: Long,
    val amountCents: Long,
    val rawJson: String,      // wide payload — do NOT select this for list rows
)

data class TransactionSummary(val id: String, val amountCents: Long, val merchant: String)

@Dao
interface TransactionDao {
    // Projection: only the columns the list needs.
    @Query("SELECT id, amount_cents AS amountCents, merchant FROM transactions WHERE account_id = :id")
    fun observeSummaries(id: String): Flow<List<TransactionSummary>>

    // Keyset pagination: uses the composite index, no OFFSET cost.
    @Query("""
        SELECT * FROM transactions
        WHERE account_id = :accountId
          AND (created_at, id) < (:cursorCreatedAt, :cursorId)
        ORDER BY created_at DESC, id DESC
        LIMIT :limit
    """)
    fun pagingSource(
        accountId: String,
        cursorCreatedAt: Long,
        cursorId: String,
        limit: Int,
    ): PagingSource<Int, Transaction>

    @Transaction
    suspend fun insertAllAndBump(rows: List<Transaction>) {   // one transaction, one invalidation
        insertAll(rows)
        bumpSyncVersion(rows.first().accountId)
    }
}
```

**Talking points / follow-ups**
- *"How do you verify an index is used?"* → `EXPLAIN QUERY PLAN` in `adb shell sqlite3`; look for `USING INDEX` vs `SCAN`. Also compare `PRAGMA index_list` / `ANALYZE`.
- *"Is Room's `Flow` a live query?"* → Yes — it re-runs when the observed tables are invalidated. On a large table with frequent writes, that is expensive; consider a periodic refresh instead of a reactive query.
- *"Should I use `@RawQuery` with `SupportSQLiteQuery`?"* → Yes for dynamic filters; but wrap it in `Flow` carefully and always with the same SQL shape so Room can reuse the prepared statement.
- *"What does `@Transaction` on a DAO method do?"* → Runs the queries in a transaction on a single connection — required for consistency across multiple statements, and it makes the whole method a single invalidation point.
- *"How do you benchmark DB changes?"* → Use the `sqlite3` shell on a device-pull of the production-sized DB, or `room-testing` with a seeded DB; assert query time in a Macrobenchmark.

---

### 8. The application consumes excessive memory when displaying thousands of items. How would you investigate and reduce memory usage?

**What the interviewer is really asking**
Memory questions are **measurement** questions. The interviewer wants to see a hierarchy of evidence: **heap dump analysis** (Memory Profiler) → **allocation profiling** → **retained-size analysis** → **leak detection** → and then the three big memory categories in Android: **Java heap**, **graphics (bitmaps)**, and **native (mmapped, `Bitmap` pixel data since API 26)**.

**Key concepts**
- Android memory buckets: Dalvik/ART heap, graphics buffers, native heap, code, stack
- `adb shell dumpsys meminfo <pkg>` — the ground truth per bucket, with `TOTAL PSS`
- Android Studio Memory Profiler — heap dump, allocation tracking, retained size
- LeakCanary — retained objects and their leak traces
- `Bitmap` memory is **native** since API 26 (`Bitmap.getAllocationByteCount()`); `recycle()` is deprecated and unnecessary
- `onTrimMemory` / `ComponentCallbacks2` — release caches when backgrounded
- `Process.setTrimMemoryLevels`, `ActivityManager.getMemoryClass()`
- `ImageDecoder` vs `BitmapFactory`; `inMutable`/`inPremultiplied`
- `LargeHeap` — a last resort, not a solution

**Approach**

1. **Get the numbers first.** `adb shell dumpsys meminfo <pkg>` after reproducing the issue. Read `TOTAL PSS`, and the breakdown: `Java Heap`, `Native Heap`, `Graphics`, `Code`, `Stack`. If **Graphics** dominates, it is bitmaps. If **Java Heap** dominates, it is your objects. If **Native Heap** dominates, suspect bitmaps' pixel data, `mmap`, or a third-party SDK.

2. **Take a heap dump at the peak** (Android Studio Memory Profiler → `Dump the heap`, or `adb shell am dumpheap <pkg> /sdcard/heap.hprof`). Compare the dump before and after reproducing. Look for:
   - **Thousands of live objects of one type** — the smoking gun (e.g. 100,000 `Transaction` objects because you materialised the full list).
   - **LeakCanary** reports for retained `Activity`/`View`/`Context`.

3. **Check the obvious large-holders first.**
   - Are you holding the full data set in memory *and* in the DB *and* in a diff list? `AsyncListDiffer` holds both old and new lists during a diff — 2x.
   - Are you keeping an unbounded `HashMap` cache of bitmaps/objects?
   - Is a `ListAdapter`'s `currentList` backed by an immutable copy that also holds the original list?

4. **Bitmap strategy.**
   - Decode at the display size, not at source resolution (see Q5, Q13).
   - Size the memory cache from `Runtime.maxMemory()`; evict on `onTrimMemory(TRIM_MEMORY_UI_HIDDEN)`.
   - Use `Bitmap.recycle()` only for pre-API-26 behaviour or for bitmaps you own outside the image library — on modern Android, `recycle()` is deprecated because the bitmap's pixels are freed by GC and manual recycling causes use-after-free crashes.
   - Use `inPreferredConfig = RGB_565` where alpha is not needed; use `inMutable = true` only if you will draw into the bitmap (otherwise you pay for a second buffer).

5. **Reduce object count and size.**
   - Use `Int`/primitive arrays (`IntArray`, `LongSparseArray`) instead of boxed `Int`/`Integer` in hot collections.
   - Avoid `Pair`/`Triple`/`data class` wrappers in per-item paths.
   - `String` interning / `char[]` for large text — rarely worth it; prefer not loading giant strings at all.
   - Consider `@Parcelize` with lazy fields vs eager deserialization of nested JSON blobs.

6. **Release caches when backgrounded.**
   ```kotlin
   override fun onTrimMemory(level: Int) {
       super.onTrimMemory(level)
       if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
           imageLoader.memoryCache?.clear()
           // or evict a portion
       }
   }
   ```
   `onTrimMemory(TRIM_MEMORY_COMPLETE)` means the app is likely to be killed — release aggressively.

7. **Reduce the native side.** If native heap dominates, look for: `mmap`ed files, `DirectByteBuffer`, the ART "large object space", and third-party SDKs (analytics, ads, maps) — those frequently leak native memory and are invisible to the Java heap profiler. Use `libheapsnapshot` for native analysis if needed.

8. **Do not reach for `largeHeap=true`.** It raises the OOM threshold but increases the likelihood of the OS killing the app (low-memory killer), and it does nothing about genuine leaks. It is a diagnostic aid, not a fix.

9. **Set a memory budget per screen and assert it in CI.** A Macrobenchmark with `MemoryUsageMetric` catches regressions; an `adb shell dumpsys meminfo` baseline in CI is a cheap gate.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Store data in Room, load a window | Memory flat regardless of data size | More queries; needs a Paging layer |
| Keep everything in memory | Fast, simple | OOM risk; GC pressure; the classic "thousands of items" bug |
| Large in-memory image cache | Instant scrolling, no re-decode | Counts against the heap; must be evicted on trim |
| Smaller cache + disk cache | Lower footprint | Re-decode cost on cache miss (CPU, not memory) |
| `Bitmap.recycle()` | Deterministic early free on old APIs | **Deprecated and dangerous on API 26+** — can crash the app with a recycled bitmap |
| `largeHeap=true` | Raises the ceiling | Masks the problem; increases low-memory kill risk |
| Primitive arrays instead of boxed collections | Much lower footprint | Less readable; manual index math |

**Key detail:** `Bitmap` pixels live in the **native** heap on API 26+, so the Java heap dump can look perfectly healthy while the app is OOMing on graphics. Always cross-check `dumpsys meminfo`'s Graphics bucket.

**Code example**

```kotlin
class ImageCache(context: Context) {
    private val maxMemKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cache = object : LruCache<String, Bitmap>(maxMemKb / 5) {   // 20% of heap
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount / 1024
    }

    fun get(key: String): Bitmap? = cache.get(key)

    fun put(key: String, bmp: Bitmap) {
        // allocationByteCount accounts for hardware bitmaps correctly.
        cache.put(key, bmp)
    }

    fun trim(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> cache.evictAll()
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> cache.trimToSize(maxMemKb / 20)
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> cache.trimToSize(maxMemKb / 40)
        }
    }
}

// Application-level hook
class App : Application() {
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        imageCache.trim(level)
    }
}
```

**Talking points / follow-ups**
- *"What is PSS vs RSS?"* → PSS (proportional set size) accounts for shared pages fairly; RSS double-counts shared pages. Use PSS for comparisons.
- *"How do you find a memory leak?"* → LeakCanary after the screen is destroyed; look at the retained count per type and the leak trace's GC roots.
- *"Does `AsyncListDiffer` double memory?"* → During a diff it holds the old and new lists. With a 100k list this is a real spike — another reason to page.
- *"What is `getAllocationByteCount` vs `getByteCount`?"* → `allocationByteCount` includes the actual allocated size (hardware bitmaps, row bytes); `byteCount` is the logical size. Use `allocationByteCount` for cache sizing.
- *"How do you know the app will not OOM?"* → Test on a low-RAM device/emulator (`adb emu -memory 512`) and set a memory budget with an assertion in tests.

---

### 9. A screen contains deeply nested layouts and takes too long to render. How would you optimize its view hierarchy?

**What the interviewer is really asking**
Classic View-system question: **measurement and layout are recursive, and `LinearLayout` with `layout_weight` causes a second measure pass**. The interviewer wants to hear that you flatten the hierarchy, replace `LinearLayout` weights with `ConstraintLayout` constraints, remove unnecessary nesting, and understand the difference between *measure/layout* cost and *draw* cost. And that you know the View-system answer (flatten) even if you would prefer Compose.

**Key concepts**
- `measure()` → `layout()` → `draw()`; measure/layout are `O(tree depth)`
- `LinearLayout` with `layout_weight` → **two measure passes** (the second with `EXACTLY` specs)
- `RelativeLayout` → double traversal; `FrameLayout` overlays children (measure is cheap, draw is not)
- `ConstraintLayout` — flat hierarchy, single measure pass, `layout_constraint*` + chains + `Barrier`/`Guideline`
- `merge` + `<include>` — reduces hierarchy depth
- `ViewStub` — inflate lazily, only when needed
- `setHasFixedSize` (RecyclerView), `android:background` overdraw, `clipToPadding`
- `ConstraintLayout` performance pitfalls: too many constraints, `0dp` chains
- Compose alternative: `Column`/`Row` are not layout-algorithm-expensive like `LinearLayout` weights, but deep Compose trees still cost composition time

**Approach**

1. **Measure the layout pass, not just the frame.** Use Perfetto/System Trace and look at the `Choreographer#doFrame` slice broken into `Measure`, `Layout`, `Draw`. `Measure`+`Layout` dominating = hierarchy problem; `Draw` dominating = overdraw or a custom `View.onDraw`.

2. **Count the depth.** `adb shell dumpsys activity top` does not give it, but Layout Inspector shows the hierarchy and depth, and the `RecyclerView`/`ViewStub` counts are visible in the merged layout. A depth of 20+ is a smell.

3. **Flatten the tree.** Replace nested `LinearLayout`s with a single `ConstraintLayout` and constrain children to each other directly. A `ConstraintLayout` with 20 direct children is one measure pass over a flat list — much cheaper than 20 levels of nested `LinearLayout`.

4. **Eliminate `layout_weight`.** A weighted `LinearLayout` performs a **second measure pass** over all children, so a weighted parent costs 2x. Replace with `ConstraintLayout` chains (`layout_constraintHorizontal_weight`) or `ConstraintLayout` `0dp` dimensions with constraints — single pass.

5. **Collapse `RelativeLayout`.** `RelativeLayout` traverses the tree twice to resolve dependencies. Convert to `ConstraintLayout`.

6. **Use `merge` and `<include>`** to avoid an extra wrapper level when inflating a reusable header/footer, and `ViewStub` for content that is not always visible:
   ```xml
   <ViewStub
       android:id="@+id/promo_stub"
       android:layout="@layout/view_promo"
       android:layout_width="match_parent"
       android:layout_height="wrap_content" />
   ```
   ```kotlin
   binding.promoStub.inflate()   // only now is the layout inflated
   ```

7. **Remove overdraw.** Transparent backgrounds, elevation shadows, and overlapping full-screen draws cost GPU time. Use `tools:background` in previews but remove the real background; avoid unnecessary `foreground` layers.

8. **For lists, prefer `RecyclerView` with `setHasFixedSize(true)`** and a `ConstraintLayout` root per item (or a plain `FrameLayout`/`ConstraintLayout` with fixed-height rows) — item layout inflation is the dominant cost, so keep item XML shallow.

9. **If you are choosing today: Compose.** Compose removes the two-pass measure/layout of nested `LinearLayout`s and computes layout in one pass. But Compose has its own cost model — deep nesting means deep composition and recomposition scope issues; prefer flat structures with `Column`/`Row` and `Box` only when needed.

10. **Verify with a Macrobenchmark** measuring `frameTimeCpuMs` for inflation of the screen and for scroll.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Flatten to `ConstraintLayout` | Single measure pass; 30–50% layout-time reduction is common | Verbose XML; constraint mistakes cause subtle bugs; `ConstraintLayout` adds a dependency (bundled in AndroidX) |
| Keep nested `LinearLayout`s | Readable, familiar | `layout_weight` doubles the measure cost; depth scales layout time |
| `RelativeLayout` | Simple relative positioning | Two tree traversals; slower than `ConstraintLayout` |
| `merge` + `<include>` | Removes a level, reuses markup | Only works with `<merge>` roots; `findViewById` semantics change |
| `ViewStub` | Zero cost until needed | Must be inflated explicitly; state is lost if re-inflated; awkward in RecyclerView items |
| Migrate to Compose | Modern, single-pass layout, easier to make responsive | Migration cost; Compose's own composition cost; needs a new perf baseline |
| Reduce overdraw / transparency | Cheaper GPU | Visual changes; some designs genuinely need layers |

**Key detail:** `LinearLayout` with `layout_weight` forces a **second measure pass**, and `RelativeLayout` traverses twice — these two are the highest-leverage, lowest-effort fixes in the View system. Most "deeply nested layout" performance tickets are solved by replacing weighted `LinearLayout`s with `ConstraintLayout`.

**Code example**

```kotlin
// BEFORE: nested LinearLayouts with weights → two measure passes per level.
setContentView(R.layout.activity_profile_old)

// AFTER: one flat ConstraintLayout; no weights → one measure pass.
class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)   // single ConstraintLayout root

        // Lazy inflation for the rarely-visible promo block.
        binding.promoStub.setOnInflateListener { stub, _ ->
            stub.findViewById<View>(R.id.promo_close).setOnClickListener {
                binding.promoStub.visibility = View.GONE
            }
        }
    }
}
```

```xml
<!-- activity_profile.xml: flat, no nesting, no weights -->
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <ImageView android:id="@+id/avatar"
        android:layout_width="48dp" android:layout_height="48dp"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"/>

    <TextView android:id="@+id/name"
        android:layout_width="0dp" android:layout_height="wrap_content"
        app:layout_constraintStart_toEndOf="@id/avatar"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="@id/avatar"/>

    <TextView android:id="@+id/balance"
        android:layout_width="0dp" android:layout_height="wrap_content"
        app:layout_constraintStart_toEndOf="@id/avatar"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toBottomOf="@id/name"/>

    <ViewStub android:id="@+id/promo_stub"
        android:layout_width="0dp" android:layout_height="wrap_content"
        android:layout="@layout/view_promo"
        app:layout_constraintTop_toBottomOf="@id/balance"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```

**Talking points / follow-ups**
- *"Does `ConstraintLayout` have its own overhead?"* → Yes, it is more expensive per-view than a simple `LinearLayout`, but a single pass over a flat tree usually beats multiple passes over a deep tree. Measure, do not assume.
- *"What about `FrameLayout` overlays?"* → `FrameLayout` measures children cheaply but **draws all of them**, so full-screen overlapping children cause massive overdraw. Watch `FrameLayout` + `foreground` + elevation.
- *"How do you find overdraw?"* → Developer Options → "Debug GPU overdraw" (shows red/green/pink bands), or the GPU counter in the GPU profiler.
- *"Does `RecyclerView` care about item hierarchy depth?"* → Yes — item inflation and measure happen per bind; shallow item layouts directly improve scroll performance.
- *"When would you still use nested layouts?"* → When the nesting is genuinely shallow (2–3 levels) and the layout is static; the cost is proportional to depth, so keep it small.

---

### 10. A user reports dropped frames on low-end Android devices, but the issue cannot be reproduced on your development device. How would you investigate it?

**What the interviewer is really asking**
This is the **production debugging / fleet observability** question. The interviewer wants to see that you do not stop at "cannot reproduce" — you build a **remote diagnostic pipeline**: field metrics, dynamic sampling, crash/ANR telemetry, and a device-fidelity strategy (emulators, low-end test hardware, throttled CPUs). And that you know the hardware strata of the Android ecosystem (low-end devices are often 4-core ARM with 1–2 GB RAM, and 120 Hz panels on mid-range devices change the frame budget).

**Key concepts**
- Field data: `JankStats` in debug builds, `FrameMetrics` + `androidx.tracing` in release
- `Macrobenchmark` / `MacrobenchmarkRule` for reproducible CI measurement
- Perfetto system traces; `atrace`/`am profile`
- Low-end device characteristics: 64-bit but weak cores, 2 GB RAM, older GPUs, driver differences
- 120 Hz vs 60 Hz frame budget; `Choreographer` vsync alignment
- Emulator as a low-end proxy (but its GPU/renderer is not representative)
- Emulation: `adb shell setprop debug.hwui.profile`, CPU throttling via `adb emu` / Perfetto stress
- `dumpsys gfxinfo` per-app histograms; "percentile" analysis (p95, p99) over the fleet
- ANR and crash telemetry as a proxy for main-thread stalls

**Approach**

1. **Get a device histogram, not a hunch.** Pull the device model / SoC distribution of your active users from analytics. "Low-end" usually means: 4-core Cortex-A53-class, 2 GB RAM, Adreno 5xx/Mali-T720-class GPU, Android 9–11. Identify the exact models that dominate complaints.

2. **Ship field metrics.** You cannot profile a user's device remotely, so you must collect data on it:
   - **JankStats** (AndroidX) in a debug/beta build gives a live jank rate; in production, wrap it with a custom `FrameMetrics` collector and report percentiles (p50/p95/p99) of `TOTAL_DURATION`, plus the device model and API level.
   - `androidx.tracing` (`trace { }` sections) around the critical paths, with `Trace.beginAsyncSection`/`endAsyncSection` for async work, so a slow upload or a slow parse is visible in a trace.
   - Be privacy-conscious: aggregate, sample (e.g. 1% of sessions), and never capture user content.

3. **Reproduce on representative hardware.** Get real low-end devices — an emulator is not a substitute (its GPU and memory behaviour differ, and it may hide GPU-bound issues while exaggerating CPU ones). If hardware is unavailable, use the emulator with **CPU throttling** and a **low-RAM configuration**, and be explicit that it approximates only the CPU part.

4. **Reproduce the *user's* conditions, not just your actions.** Low-end devices struggle with:
   - **Thermal throttling** — a device that has been running for 10 minutes is slower. Reproduce with a long session.
   - **Memory pressure** — other apps competing; low-RAM devices get killed and their caches trimmed.
   - **Background work** — a sync running while the user scrolls.
   - **Slow storage / low I/O** — cold Room reads, cache misses.
   - **60 vs 120 Hz** — a 120 Hz panel halves the frame budget.

5. **Check for hardware-specific rendering differences.** GPU driver bugs, vendor-specific `Bitmap` decode issues, and 4-byte-per-pixel vs 2-byte-per-pixel differences can matter. Check `adb shell dumpsys SurfaceFlinger` for the refresh rate and the GLES renderer.

6. **Look at what the field data says.** Percentile frame-time distributions per device model. If p99 is 200 ms only on a specific SoC, that is a driver/hardware issue; if it correlates with a specific screen or action, it is a code issue.

7. **Use CI to prevent regressions.** Add a **Macrobenchmark** that runs on a low-emulated device (`MacrobenchmarkRule` with `CompilationMode.None` to simulate cold code paths) measuring `frameTimeCpuMs` and `jank`. Gate CI on it. `CompilationMode.Partial` (Baseline Profile) vs `None` gives you the A/B of JIT vs AOT.

8. **Ship a Baseline Profile** so the low-end device is not interpreting bytecode at startup — this disproportionately benefits low-end devices, where JIT compilation competes with the UI thread.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Field `FrameMetrics` telemetry | Sees the real distribution on real devices | Privacy/sampling complexity; needs aggregation infrastructure; volume cost |
| JankStats in production | Zero extra code | JankStats is designed for debug; in release it still costs a little and gives only a rate |
| Real low-end test hardware | Highest fidelity | Expensive, hard to source, slow to iterate |
| Emulator + CPU throttling | Cheap, CI-friendly | **Not representative for GPU-bound or memory-bound issues**; must not be trusted alone |
| Macrobenchmark in CI | Regression gate, repeatable | Measures one flow on one device class; can be flaky under shared runners |
| Baseline Profiles | Big cold-start and first-frame win on low-end | Needs a release build and a `profileinstaller` dependency; maintenance overhead |
| Thermal/cold-cache simulation | Reproduces the "after several minutes" case | Slow, manual, hard to automate |

**Key detail:** "Cannot reproduce on my device" is a *symptom of a device-stratification problem*, not a dead end. The professional response is to instrument the fleet so that the reproduction is no longer needed on your desk — and to add CI coverage so the class of bug is caught before release.

**Code example**

```kotlin
// Release-build frame telemetry: aggregate percentiles per device model.
class FrameMetricsCollector(private val onSample: (FrameSample) -> Unit) {
    fun attach(activity: Activity) {
        val handler = Handler(Looper.getMainLooper())
        activity.window.addOnFrameMetricsAvailableListener(
            { _, metrics, _ ->
                val frame = FrameMetricsCalculator(metrics)
                onSample(
                    FrameSample(
                        totalMs = frame.totalDurationMs,
                        drawMs = frame.drawDurationMs,
                        model = Build.MODEL,
                        api = Build.VERSION.SDK_INT,
                    )
                )
            },
            handler,
        )
    }
}

// Macrobenchmark: gate CI on frame time for a low-end class device.
@RunWith(AndroidJUnit4::class)
class ScrollPerfTest {
    @get:Rule val perfRule = MacrobenchmarkRule()

    @Test fun scrollTransactionList_isSmooth() = perfRule.measureRepeated(
        packageName = "com.example.app",
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        setupBlock = { pressHome(); startActivityAndWait() },
        measureBlock = { scrollToEnd(); scrollToStart() },
    )
}
```

**Talking points / follow-ups**
- *"Why not just buy a Pixel?"* → The Pixel (even the cheap one) is not representative of the low-end fleet. You need a device from the offending model family, ideally with a real user build.
- *"What is `CompilationMode` for?"* → `None` measures cold/worst-case (no AOT), `BaselineProfile` measures with AOT, `Full` is fully optimised. Comparing `None` vs `BaselineProfile` quantifies the value of your profile.
- *"What does `FrameTimingMetric` report?"* → `frameTimeCpuMs` (CPU time in the frame) — the metric you can actually influence in code, as opposed to total frame time which is largely display-bound.
- *"How do you handle flaky CI benchmarks?"* → Use `CompilationMode.None` for stability, take the median of iterations, and set a threshold with headroom (e.g. fail at >16 ms p95) rather than chasing exact numbers.
- *"What about ANRs?"* → ANRs are the extreme of main-thread stalls; correlate ANR traces with the same device models to find the worst offenders.

---

### 11. A list receives frequent real-time updates while the user scrolls. How would you prevent unnecessary UI updates?

**What the interviewer is really asking**
Real-time feeds + scrolling is a classic **feedback loop**: every socket message triggers a list update → diff → rebind → jank, which also makes the feed arrive slower, which makes it worse. The staff answer is about **coalescing, throttling, and not updating what the user is looking at**, plus knowing exactly which APIs do what (`AsyncListDiffer`, `notifyItemRangeInserted` vs full diff, Compose `key`s and `LazyColumn`'s item snapshot).

**Key concepts**
- `AsyncListDiffer` — background diff, batched main-thread dispatch
- `DiffUtil` cost is O(N + D²) where D = number of changes — frequent small changes on a 100k list is expensive
- Coalescing: `MutableSharedFlow` + `debounce`/`sample`/`conflate`
- `snapshotFlow`, `derivedStateOf`
- Compose `LazyColumn`'s item snapshot is a *list snapshot* — recomposing the list container is cheap, but rebinding items is not
- `notifyItemRangeInserted/Removed/Changed` vs `notifyDataSetChanged`
- `RecyclerView.Adapter` `getItemId` + `setHasStableIds(true)`
- Backpressure: `Channel.CONFLATED` vs `BufferOverflow.DROP_OLDEST`
- Recycler view prefetch / `setItemViewCacheSize`

**Approach**

1. **Throttle the source, not the UI.** The first and biggest win is to not deliver every tick to the UI. Coalesce at the flow level:
   ```kotlin
   val uiUpdates: Flow<List<Event>> = repository.events
       .scan(emptyList()) { acc, e -> (acc + e).takeLast(MAX) }
       .sample(500.milliseconds)                 // or .debounce(300)
       .conflate()                               // keep only the latest if the UI is busy
   ```
   - `conflate()` is the key operator: if the UI cannot keep up, intermediate values are dropped rather than queued. This is *exactly* the semantics you want for a live feed.
   - `sample(500)` bounds the update rate to 2 fps — imperceptible to the user, hugely cheaper.

2. **Batch inserts into one range update.** If 30 events arrive in a burst, do one `notifyItemRangeInserted(0, 30)` — not 30 individual notifications. In Compose, mutate the underlying list once per batch so the snapshot changes once.

3. **Do not re-diff the whole list on every event.** `AsyncListDiffer` diffs the *entire* old vs new list. If the list is 50,000 items and 5 events arrive per second, that is 50,000-element diffs 5x a second. Instead:
   - Keep the list append-only and mutate it directly, notifying only the changed range.
   - Or use `Paging 3` with a `RemoteMediator` — appends go through the DB and Paging handles the window, so the diff surface is bounded.

4. **Use stable IDs and partial payloads.**
   ```kotlin
   init { setHasStableIds(true) }
   override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()
   override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
       if (payloads.isEmpty()) holder.bind(getItem(position)) else holder.bindPayload(payloads)
   }
   ```
   With stable IDs, RecyclerView can animate and reuse views without a full rebind.

5. **Skip updates for rows the user is actively interacting with.** If the user is scrolling or has a row expanded, defer the update for that row: keep the "incoming" value in a side map and apply it when the interaction ends. This is a real pattern in trading/chat apps.

6. **Pause updates during active scrolling.** `RecyclerView.OnScrollListener` (or `LazyListState.isScrollInProgress`) can gate the collector:
   ```kotlin
   uiUpdates.collect { list ->
       if (!listState.isScrollInProgress) adapter.submitList(list) else pendingUpdate = list
   }
   ```
   Apply `pendingUpdate` when scrolling stops. Combined with `conflate`, the feed stays live and the list stays smooth.

7. **In Compose, use `key` and `contentType`** so unchanged items are skipped. Also pass only the primitives the row needs — a row that reads a large object recomposes when any of its fields change.

8. **Bound the retained list.** Keep only the last N (e.g. 200) events in memory and page older history on demand. An unbounded growing list is both a memory and a diff problem.

9. **Watch `derivedStateOf` misuse.** `derivedStateOf` helps only if the derived value is read in a scope that is *not* recomposed on every input change; if the expensive computation is inside the item lambda, it runs per item.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Throttle/`sample` the update stream | Massive CPU reduction; user-visible behaviour unchanged | Updates are not instant — bad for price ticks or chat delivery guarantees |
| `conflate()` | Backpressure-safe; never queues stale updates | Intermediate events are dropped — correct for a feed, wrong for a ledger |
| `AsyncListDiffer` / full diff | Correct, animated inserts | O(N) per update — too expensive for a 50k list with frequent updates |
| Direct range notifications | O(changed) cost | Bypasses DiffUtil's correctness guarantees; must manage the list manually |
| Paging 3 + Room | Bounded work; appends handled by the DB | More machinery; the feed is paged rather than a simple list |
| Pause updates while scrolling | Smoothest scroll | Items can be momentarily stale; must apply the pending update on scroll end |
| Keep only the last N items | Bounded memory | History needs a separate fetch path |

**Key detail:** `DiffUtil` is not O(N) in the naive sense — it is O(N + D²) where D is the number of differences. A *small* number of changes is cheap; but if the whole list is replaced (e.g. a new server response that reorders everything) D = N and it becomes O(N²)-ish. Frequent full replacements on a 50k list is the killer.

**Code example**

```kotlin
class FeedViewModel(private val repo: FeedRepository) : ViewModel() {

    private val pending = MutableStateFlow<List<Event>>(emptyList())

    // Conflate + throttle: the UI never queues more than one pending update.
    val feed: StateFlow<List<Event>> = pending
        .sample(500.milliseconds)          // bound the update rate
        .conflate()                        // drop intermediate values under pressure
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            repo.events.collect { e ->
                // One snapshot mutation per burst → one diff, one notify range.
                pending.value = (pending.value + e).takeLast(200)
            }
        }
    }
}

class FeedAdapter : ListAdapter<Event, FeedAdapter.VH>(DIFF) {
    init { setHasStableIds(true) }
    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) holder.bindFull(getItem(position))
        else holder.bindPartial(getItem(position), payloads)   // only the changed field
    }
}
```

**Talking points / follow-ups**
- *"Is `conflate()` safe for a chat app?"* → It can drop messages if the collector is slow. Use a conflated channel only for *UI state*; keep the authoritative message log in the DB and re-read it on scroll-to-bottom.
- *"How do you handle a burst of 100 updates?"* → Coalesce into one list mutation, then one `submitList` → one diff → one `notifyItemRangeInserted`. Batching at the source beats batching at the UI.
- *"What about `DiffUtil` on the main thread?"* → It must never run on the main thread; `AsyncListDiffer` handles it, `ListAdapter` too.
- *"Does `sample` hurt chat UX?"* → 500 ms is imperceptible for a feed; for a live scoreboard use `sample(100)` or none at all.
- *"How do you test this?"* → `Turbine` for flow emissions; a fake source that emits 100 events rapidly and assert the number of UI updates (should be ~1–2).

---

### 12. The app's startup time increases significantly after adding several SDKs. How would you identify the bottleneck?

**What the interviewer is really asking**
**Startup performance is a measured discipline.** The interviewer wants to hear: define the metric (TTID, TTFD, cold start), measure with the right tools (`Macrobenchmark` StartupTimingMetric, `am start -W`, `atrace`/`Perfetto` with `am` slices, `StartupMetric`s in Play Console), understand the anatomy of startup (`Application.onCreate` → ContentProvider init → Activity onCreate → first frame), and know the standard remedies — **App Startup library** for lazy initialisation, **R8** for shrinking, **Baseline Profiles**, avoiding `ContentProvider` initialisation, deferring non-critical work.

**Key concepts**
- Cold start, warm start, hot start; TTID (time to initial draw), TTFD (time to first draw of content)
- `Application.onCreate()` is on the critical path — everything in it is
- **`ContentProvider.onCreate()` runs BEFORE `Application.onCreate()`** — a classic SDK trap (Firebase, WorkManager, LeakCanary, EMM SDKs register providers)
- `androidx.startup:startup-runtime` — the App Startup library for lazy, ordered component initialisation
- `BaselineProfile` / `androidx.profileinstaller` — AOT for critical paths
- R8/ProGuard — shrinking, `keep` rules; `-dontobfuscate` in debug
- `Lazy<T>` / `by lazy` / `dagger.hilt` `Lazy` injection for components not needed at startup
- `Macrobenchmark` `StartupTimingMetric`
- Play Console **Startup Performance** report (real-user TTID)

**Approach**

1. **Define the metric before optimising.** Cold-start TTID = time from process fork to the first frame the user sees. Measure it on a low-end device with the app **not** in the recents list and after `adb shell am force-stop`.

2. **Get a per-phase breakdown.**
   - `adb shell am start -W -n com.example/.MainActivity` prints `TotalTime`, `WaitTime`, `Complete`.
   - `adb shell am start -S -W ...` for a cold start (kills first).
   - `adb shell am start -n com.example/.MainActivity --ez "profile" true` + `atrace`/`Perfetto` with the `am` slices category gives you `am_proc_start`, `bindApplication`, `activityThreadMain`, `activityStart`, `activityResume`, `Choreographer#doFrame`.
   - `Macrobenchmark` `StartupTimingMetric` in CI.

3. **Find the SDK's contribution.** Each SDK adds cost in one of a few places:
   - **ContentProvider init** — runs before `Application.onCreate`; you see it in the trace as `ContentProvider` / `<SDK> init` between `bindApplication` and `Application.<init>`. This is invisible in `onCreate` and is the #1 surprise.
   - **Static initialisers** — `<clinit>` of a utility class triggered by a static field access in `onCreate`.
   - **`Application.onCreate` work** — SDK `init()` calls (analytics, crash reporting, ads, push).
   - **DEX loading / class verification** — the first time a class is touched, ART verifies it; thousands of classes → seconds. This is what Baseline Profiles fix.
   - **Disk I/O** — SDKs reading/writing files or SharedPreferences on the main thread (`StrictMode` disk-read policy will catch it).

4. **Use the App Startup library to make SDK init lazy.** Declare an `Initializer` per component and initialise only what the first screen needs:
   ```kotlin
   class App : Application(), Initializer<Analytics> {
       override fun onCreate() {
           AppInitializer.getInstance(this).initialize(this)   // only declared initializers run
       }
       override fun create(context: Context): Analytics = AnalyticsSdk.init(context)
       override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
   }
   ```
   Then trigger deferred components explicitly when the user reaches the relevant feature (e.g. `AppInitializer.getInstance(ctx).initializeComponent(PushInitializer::class.java)`), which is much cheaper than at startup.

5. **Use lazy initialisation inside `Application.onCreate`.** Do not eagerly create SDKs you do not need on the first screen. With Hilt, inject `Lazy<Analytics>` so the SDK is constructed on first use.

6. **Watch for `ContentProvider` auto-init.** For SDKs you cannot change, disable their provider in the manifest:
   ```xml
   <provider
       android:name="com.sdk.SyncProvider"
       android:authorities="${applicationId}.sdk"
       android:enabled="false"
       tools:replace="android:enabled" />
   ```
   Only do this if the provider is genuinely optional; otherwise you break the SDK.

7. **Enable R8 for release.** Debug builds are 2–10x slower to start (no shrinking, no optimisation, no `dex` layout optimisation). Always compare release builds. Configure `android.enableR8.fullMode=true` and check the mapping file for over-kept classes.

8. **Ship a Baseline Profile.** `androidx.profileinstaller` + a `baseline-prof.txt` generated by Macrobenchmark (`--profile` output) removes the ART interpretation/JIT cost on cold start. This is the single highest-leverage fix on low-end devices.

9. **Move work off the startup path.** Defer non-essential initialisation to after the first frame:
   ```kotlin
   override fun onCreate() {
       super.onCreate()
       // critical path only
       val deferred = WorkManager... // no; instead:
       lifecycleScope.launch { yield(); initSecondarySdk() }   // after first frame
   }
   ```
   Better: use `Choreographer.postFrameCallback` or `Activity`'s `onPostResume`, or `ProcessLifecycleOwner` + `repeatOnLifecycle(STARTED)`.

10. **Verify with Play Console's Startup Performance report** — real-user data across the device fleet, with the cold-start TTID distribution. Local numbers on a flagship tell you nothing about the fleet.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| App Startup library with lazy initialisers | Defers SDK cost to first use; explicit dependency graph | SDKs not designed for it may still self-initialise via their own providers |
| Disabling a provider via the manifest | Removes pre-`onCreate` init entirely | Can break the SDK's background behaviour; only for optional components |
| `Lazy<T>` / Hilt `Lazy` injection | Pay only when used | Slightly more complex wiring; the cost lands on the first feature instead |
| R8 / full mode | Smaller dex, faster class loading, obfuscation | Longer builds; can break reflection-based SDKs without `keep` rules |
| Baseline Profile | Large cold-start win, especially on low-end | Needs a release build and a maintenance loop to keep the profile fresh |
| Deferring work to after the first frame | Perceived TTID improvement | Work still happens; if it is heavy it can jank the first interaction |
| Reducing `ContentProvider` count | Removes pre-`Application` serial init | Constraint on which SDKs you can use |

**Key detail:** `ContentProvider.onCreate()` runs **before** `Application.onCreate()`. If an SDK's init shows up nowhere in your `onCreate`, this is why — and the fix is the manifest `android:enabled="false"` trick or the App Startup library, not editing your `onCreate`.

**Code example**

```kotlin
class App : Application(), Initializer<CrashReporter> {

    override fun onCreate() {
        super.onCreate()
        // Only initializers that are actually required to draw the first screen run here.
        AppInitializer.getInstance(this).initialize(this)
        // Everything else is initialised lazily, at the point of use:
        //   AppInitializer.getInstance(ctx).initializeComponent(AnalyticsInitializer::class.java)
    }

    override fun create(context: Context): CrashReporter =
        CrashReporterSdk.install(context)      // crash reporting IS needed at startup

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

// Lazy SDK access for anything not needed on the first screen.
class HomeViewModel @Inject constructor(
    private val analytics: Lazy<Analytics>,           // Hilt Lazy: constructed on first use
    private val recommendations: Lazy<RecommendationClient>,
) : ViewModel() {
    fun onScreenShown() { analytics.get().track("home_shown") }
}
```

**Talking points / follow-ups**
- *"What is the difference between TTID and TTFD?"* → TTID is the first frame the system draws (often the splash); TTFD is the first frame with real content. Optimising only TTID can make the app feel *slower* if the splash is instant but the content takes 2 s.
- *"How do you test startup in CI?"* → Macrobenchmark `StartupTimingMetric` with `CompilationMode.Partial` (profile) and `None`; run on an emulated low-end device in CI.
- *"Does `androidx.startup` replace all SDK init?"* → No — only for SDKs that expose an `Initializer` or that you can gate behind one; others still self-initialise.
- *"What is the biggest win on low-end devices?"* → Baseline Profiles (removes JIT/interpretation on the cold path) and removing main-thread disk I/O from `Application.onCreate`.
- *"Why did startup get worse after adding SDKs — is it the size of the APK?"* → Dex size matters (more classes to verify), but usually the regression is one SDK doing I/O or network work on the main thread during init. The trace tells you which.

---

### 13. A large image causes an OutOfMemoryError. How would you handle image decoding and memory management?

**What the interviewer is really asking**
The interviewer wants precision about **where bitmaps live** (native since API 26), the difference between an `OutOfMemoryError` from the Java heap and one from the graphics allocator, and the specific remedies: **downsample at decode time**, `inSampleSize` maths, `inPreferredConfig`, `ImageDecoder` with `setTargetSampleSize`, cache sizing, and eviction. They will also probe whether you know that `Bitmap.recycle()` is deprecated.

**Key concepts**
- `OutOfMemoryError: Failed to allocate a ... byte allocation` vs "Canvas: trying to use a recycled bitmap"
- `Bitmap` pixel data is **native** memory since API 26; `getAllocationByteCount()` vs `getByteCount()`
- `BitmapFactory.Options.inSampleSize` — power-of-two subsampling
- `BitmapFactory.Options.inPreferredConfig` — `RGB_565` (2 bytes/px), `ARGB_4444` (deprecated), `ARGB_8888` (4 bytes/px), `RGBA_F16` (8 bytes/px, HDR)
- `ImageDecoder` (API 28+) — `setTargetSampleSize`, `setTargetSize`, `setAllocator(ALLOCATOR_SOFTWARE)` (avoids the hardware bitmap limit)
- Hardware bitmaps: occupy graphics memory; `Bitmap.recycle()` is deprecated
- `inMutable` — doubles memory when you then draw into it
- Coil/Glide handle sampling automatically from the target size
- Large-image limits: some devices cap hardware bitmap dimensions

**Approach**

1. **Identify the OOM type and the allocation site.** Look at the stack trace: `java.lang.OutOfMemoryError: Failed to allocate a 41943052 byte allocation with a 12582912 free bytes...` usually points at `BitmapFactory.nativeDecodeStream` or `Bitmap.createBitmap`. The "free bytes" figure tells you whether it was the Java heap (small number) or a large contiguous allocation.

2. **Compute the required size.** Bitmap memory = `width × height × bytesPerPixel`. A 4000×3000 ARGB_8888 image = 48 MB. If your image view is 200 dp × 200 dp on a xxhdpi device, that is 600×600 px = 1.44 MB. You are decoding **33x more pixels than you need.**

3. **Downsample during decode, not after.** Decoding full-size and then calling `Bitmap.createScaledBitmap` does not help — the full-size bitmap already exists and is already in the OOM. Use `inJustDecodeBounds` to read the dimensions first, then `inSampleSize` to decode directly at roughly the target size:
   ```kotlin
   fun decodeSampled(resolver: ContentResolver, uri: Uri, reqW: Int, reqH: Int): Bitmap {
       val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
       resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
       val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqW, reqH)
       val opts = BitmapFactory.Options().apply {
           inSampleSize = sample
           inPreferredConfig = Bitmap.Config.RGB_565
       }
       return resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts)!! }
   }

   private fun calculateInSampleSize(w: Int, h: Int, reqW: Int, reqH: Int): Int {
       var sample = 1
       if (reqW <= 0 || reqH <= 0) return sample
       val (halfW, halfH) = w / 2 to h / 2
       while (halfW / sample >= reqW && halfH / sample >= reqH) sample *= 2
       return sample
   }
   ```
   The `while` uses `halfW/sample` so the result is the largest power of two that still covers the requested size (so it can be scaled down slightly, never up).

4. **Prefer `ImageDecoder` on API 28+** — it does the sampling for you and handles EXIF orientation:
   ```kotlin
   val source = ImageDecoder.createSource(resolver, uri)
   val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
       val size = info.size
       decoder.setTargetSampleSize(calculateSample(size.width, size.height, reqW, reqH))
       decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE  // optional: avoids hw bitmap limits
   }
   ```
   `setTargetSize` (exact) is even better when the image will be displayed at a known size.

5. **Prefer a library for anything user-facing.** Coil/Glide take the target size and configure the decoder for you, handle hardware/software bitmaps, cache, and lifecycle. Hand-rolled decoders are a source of production OOMs.

6. **Choose the pixel format deliberately.** `RGB_565` halves memory (2 bytes/px) and is fine for opaque photos with no gradients. `ARGB_8888` is required for alpha. `RGBA_F16` is 8 bytes/px — HDR content; do not enable it "just in case".

7. **Cap the memory cache.** `LruCache` sized as a fraction of `Runtime.maxMemory()` and evicted on `onTrimMemory`. A cache that grows without bound is the second OOM after the initial decode.

8. **Do not call `recycle()` on API 26+.** It is deprecated; the GC frees the bitmap, and manual recycling causes `Canvas: trying to use a recycled bitmap` crashes if a draw is still pending. Use it only for bitmaps you own and know are not referenced (rare).

9. **Handle hardware-bitmap limits.** Hardware bitmaps are limited in count/dimension by the graphics driver; use `ALLOCATOR_SOFTWARE` for very large images that must be manipulated (crop, rotate, filter).

10. **Guard the whole thing.** Wrap image work in `runCatching` on `OutOfMemoryError` and fall back to a placeholder, so one bad image cannot kill the app. Also check `ActivityManager.isLowRamDevice()` to reduce cache sizes on low-RAM devices.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Decode at target size (`inSampleSize`) | 10–100x less memory; faster decode | Slight quality loss if you over-sample; needs the target size up front |
| `RGB_565` | Half the memory | No alpha; banding on gradients; not for UI icons or text |
| Image library (Coil/Glide) | Handles sampling, caching, lifecycle, orientation | Dependency; less control over exotic pipelines |
| `recycle()` | Deterministic early free (pre-API-26) | **Deprecated on API 26+**; crash risk if the bitmap is still in use |
| Large memory cache | No re-decode on scroll | Counts against the heap; must be evicted |
| `ALLOCATOR_SOFTWARE` | No hardware bitmap limits; mutable | Uses Java heap instead of graphics memory; slower to draw |
| Catching `OutOfMemoryError` | App survives a single bad image | The heap may be in a bad state; it is a mitigation, not a fix |

**Key detail:** `Bitmap.createScaledBitmap` on a full-size bitmap does not reduce peak memory — the full-size bitmap is already allocated. **Downsampling must happen during decode** (`inSampleSize` / `ImageDecoder.setTargetSampleSize`).

**Code example**

```kotlin
object SafeDecoder {

    fun decodeForDisplay(
        resolver: ContentResolver,
        uri: Uri,
        reqWidthPx: Int,
        reqHeightPx: Int,
    ): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(resolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val size = info.size
                decoder.setTargetSampleSize(sampleFactor(size.width, size.height, reqWidthPx, reqHeightPx))
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeSampledLegacy(resolver, uri, reqWidthPx, reqHeightPx)
        }
    } catch (e: OutOfMemoryError) {
        // Last-resort guard: one oversized image must not crash the app.
        null
    }

    private fun sampleFactor(w: Int, h: Int, reqW: Int, reqH: Int): Int {
        if (reqW <= 0 || reqH <= 0) return 1
        var sample = 1
        while (w / (sample * 2) >= reqW && h / (sample * 2) >= reqH) sample *= 2
        return sample
    }
}
```

**Talking points / follow-ups**
- *"Why is `Bitmap` memory not in the Java heap?"* → Since API 26 the pixel data is allocated in native/graphics memory; `dumpsys meminfo` shows it under "Graphics". This is why the Memory Profiler's Java heap dump can look clean during an image OOM.
- *"What is `getAllocationByteCount` vs `getByteCount`?"* → `allocationByteCount` is the real allocated size (including row padding and hardware bitmap accounting); use it for cache sizing.
- *"How many bitmaps can I hold?"* → Depends on the heap. With `inSampleSize` you never need a full-resolution bitmap; the rule is *never decode more pixels than you will draw*.
- *"What about `ExifInterface` rotation?"* → Rotation metadata is not applied by `BitmapFactory`; the image appears sideways. Use `ImageDecoder` (applies EXIF) or read the EXIF and rotate — but rotating means allocating a second bitmap, so rotate *after* downsampling.
- *"How do you test OOM handling?"* → Feed a huge image (e.g. 8000×8000) in a test and assert the app survives and renders a placeholder.

---

### 14. A dashboard performs multiple expensive calculations whenever its state changes. How would you optimize it?

**What the interviewer is really asking**
This is the **derived state / recomposition-scope / computation-placement** question. The interviewer wants to see that you can (a) measure which calculation is expensive, (b) push computation out of composition, (c) memoise and invalidate correctly, (d) move heavy work off the main thread, and (e) know when the answer is "do it in SQL, not in Kotlin" — and the classic staff detail: `derivedStateOf` helps only when the derived value is read *below* the changing state, not above it.

**Key concepts**
- Compose: `derivedStateOf`, `remember`, `remember(key)`, computation in `ViewModel` vs in the composable
- "Push reads down" — the derived value should be read in the smallest scope
- Calculation off the main thread: `Dispatchers.Default`, `withContext`
- Memoisation with a correct invalidation key (`remember(input)`)
- Room/SQL aggregation vs in-memory aggregation
- `Flow` operators: `map`, `combine`, `runningFold`, `scan`
- `SnapshotStateList` / immutable data snapshots
- `kotlinx.collections.immutable` (persistent lists) for cheap copies

**Approach**

1. **Find which calculation is expensive.** Use the Compose compiler metrics (`metricsDestination`) plus a `trace` section around the computation, or simply time it in a unit test. If the dashboard recomputes on every frame, the number-one cause is that it reads a rapidly-changing state (a clock, a scrolling offset, an animation) high in the tree.

2. **Push the state read down.** This is the highest-value, lowest-cost fix:
   ```kotlin
   // BAD: a ticking state read at the top forces the whole dashboard to recompute.
   @Composable fun Dashboard() {
       val now by remember { mutableStateOf(System.currentTimeMillis()) }
       LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }
       Header(now)                 // reads `now`
       StatsPanel(items)           // recomposes every second for no reason
   }
   ```
   Fix: read `now` only inside `Header`. Then the stats panel is skipped.

3. **Use `derivedStateOf` for genuinely derived values.** It caches the computation and only re-runs it when its *dependencies* change — and, critically, the consumer only recomposes when the *result* changes:
   ```kotlin
   val total by remember(items) {
       derivedStateOf { items.sumOf { it.amountCents } }
   }
   ```
   Important nuance: `derivedStateOf` must be read *below* the changing state. If you read it in the same scope that reads the raw input, you gain nothing.

4. **Memoise with the right key.** `remember { compute() }` computes once per composition; `remember(input) { compute(input) }` recomputes when `input` changes. If the input is a `List`, `remember(items)` compares by `equals` — for 100,000 items that comparison is itself expensive, so key on a cheaper identity (e.g. `items.size` plus a version counter) or use a stable key type.

5. **Move the computation off the main thread and cache the result.** For genuinely heavy work (aggregating 100,000 rows, running a forecast), compute in the ViewModel on `Dispatchers.Default` and expose the *result* as UI state:
   ```kotlin
   val stats: StateFlow<DashboardStats> = combine(itemsFlow, periodFlow) { items, p ->
       withContext(Dispatchers.Default) { computeStats(items, p) }
   }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardStats.EMPTY)
   ```
   Now the UI just reads a value; it never computes.

6. **Do the aggregation in SQL.** If the data is in Room, `SELECT SUM(amount), COUNT(*) FROM transactions WHERE ... GROUP BY category` with a proper index is orders of magnitude faster than pulling rows into Kotlin and summing. This is the strongest staff answer for "expensive calculations over large data". Verify with `EXPLAIN QUERY PLAN`.

7. **Incremental computation.** If the data arrives in a stream, use `scan`/`runningFold` to maintain a running aggregate instead of recomputing over the whole set on every emission:
   ```kotlin
   val runningTotal: Flow<Long> = events.scan(0L) { acc, e -> acc + e.amountCents }
   ```

8. **Avoid unnecessary state churn.** If the dashboard's data is refreshed by polling, do not emit a new list object every poll if nothing changed — compare a version/hash and skip the emission, so downstream `derivedStateOf` and `DiffUtil` do not run.

9. **Beware the "state read in a lambda" trap.** A composable that reads a `State` inside a *non-composable* callback (e.g. a `onValueChange` lambda) does not subscribe to it — you get stale values instead of a recomposition. Read state in the composable body, pass values down.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| `derivedStateOf` | Recompute only when the result changes; minimal recomposition | No benefit if read at the same level as the input; `by` delegate only in composables |
| Compute in the ViewModel off the main thread | UI thread free; result cached in state | Adds a layer; the state must be pushed to the UI (one recomposition of the consumer) |
| Aggregation in SQL | Orders of magnitude faster for large data | SQL complexity; query plan must be verified; schema constraints |
| `remember(input)` memoisation | Avoids recomputation for the same input | Key comparison cost; `equals` on big lists is not free |
| Incremental `scan`/`runningFold` | O(1) per event instead of O(n) per emission | Only works for associative/commutative folds; more complex state |
| Reordering calculations | Cheap win, no new machinery | Can only help if a cheaper formulation exists; does not fix O(n) |

**Key detail:** `derivedStateOf` reduces **recomposition**, not **computation**. It re-runs the block whenever its dependencies change, and only notifies consumers when the *result* differs. If the block is expensive and the dependency changes often, `derivedStateOf` does not save you — you need to move the work off the main thread or into SQL.

**Code example**

```kotlin
@Immutable
data class DashboardStats(
    val totalCents: Long,
    val byCategory: Map<String, Long>,
    val transactionCount: Int,
)

class DashboardViewModel(private val dao: TransactionDao) : ViewModel() {

    // Heavy aggregation happens in SQL, off the main thread, as a Flow.
    val stats: StateFlow<DashboardStats> = dao.observeCategoryTotals()
        .map { rows ->
            DashboardStats(
                totalCents = rows.sumOf { it.totalCents },
                byCategory = rows.associate { it.category to it.totalCents },
                transactionCount = rows.sumOf { it.count },
            )
        }
        .flowOn(Dispatchers.Default)        // mapping off the main thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardStats.EMPTY)
}

@Composable
fun Dashboard(stats: State<DashboardStats>) {
    // Read the state DOWN HERE — only the consumers of `stats` recompose.
    val value by stats
    Column {
        Header()
        StatsPanel(total = value.totalCents)      // skippable if value is unchanged
        CategoryChart(value.byCategory)
    }
}

@Composable
fun StatsPanel(total: Long) {
    // Expensive formatting memoised on its input.
    val formatted by remember(total) { derivedStateOf { formatCurrency(total) } }
    Text(formatted)
}
```

**Talking points / follow-ups**
- *"What is the difference between `remember` and `derivedStateOf`?"* → `remember` caches a value for the composition lifetime (or until its key changes); `derivedStateOf` caches a *computation* whose dependencies may change independently of the composition, notifying only when the result changes.
- *"Does `derivedStateOf` run on a background thread?"* → No — the block runs where it is read, on the main thread. For heavy work use `withContext(Dispatchers.Default)` in the ViewModel.
- *"How do you decide SQL vs Kotlin?"* → If the data is already in Room and the operation is an aggregate/filter/sort, do it in SQL. If it needs domain logic that SQL cannot express, compute in Kotlin off the main thread.
- *"What about a chart with 10,000 points?"* → Do not map 10,000 points into Compose on every recomposition. Pre-aggregate to the pixel bucket size (e.g. 200 buckets) and keep the chart input stable, ideally an `@Immutable` holder.
- *"How do you avoid recomputing when polling returns identical data?"* → Emit only on change (compare a content hash / version) so `StateFlow`'s `distinctUntilChanged` semantics suppress downstream work.

---

### 15. How would you measure and improve cold-start time, frame rendering, and memory usage in a production Android app?

**What the interviewer is really asking**
This is the **staff-level capstone**. The interviewer wants the *system*: a measurement infrastructure (local profiling + field telemetry + CI regression gates), a prioritisation method (which metric matters most for your app), and a delivery mechanism (Baseline Profiles, R8, startup restructuring, list/Compose optimisation, memory budgeting). They want to hear that you know the tools by name and that you treat performance as a **process with owners and budgets**, not a one-off optimisation sprint.

**Key concepts**
- `Macrobenchmark` + `MacrobenchmarkRule` — CI-gated metrics: `StartupTimingMetric`, `FrameTimingMetric`, `MemoryUsageMetric`
- `androidx.tracing` + `Trace.beginSection` / `beginAsyncSection` — instrumented spans
- `FrameMetrics` / `JankStats` — runtime frame timing, in-app and in the field
- `Baseline Profiles` (`androidx.profileinstaller`) — AOT for cold start and first frames
- R8 / `android.enableR8.fullMode` — dex shrinking, less class loading
- `androidx.startup` — lazy initialisation
- `adb shell dumpsys gfxinfo <pkg>` and `dumpsys meminfo <pkg>` — the ground truth
- Android Vitals (Play Console): startup time, ANR, jank
- `StartupTimingMetric` gives `TTID` (time to initial draw) and `TTFD` (time to first draw)
- Memory budgets per screen; `MemoryUsageMetric`

**Approach**

1. **Define budgets, not just measurements.** A performance programme without targets is a wish. Set concrete goals: *"cold start p75 TTID < 1.2 s on low-end devices"*, *"< 1% janky frames on the top 5 screens"*, *"peak PSS < 180 MB"*. Then measure against them.

2. **Establish the local measurement harness.**
   - **Cold start:** `adb shell am start -S -W -n <pkg>/<activity>` (the `-S` force-stops first, making it cold), and Macrobenchmark `StartupTimingMetric` for a repeatable number.
   - **Frame rendering:** `adb shell dumpsys gfxinfo <pkg> reset` → interact → `dumpsys gfxinfo <pkg>` (jank %, `Draw`/`Process` breakdown, `framestats` for a per-frame CSV). Perfetto for deep dives.
   - **Memory:** `adb shell dumpsys meminfo <pkg>` (Java heap, Graphics, native, TOTAL PSS), the Memory Profiler for heap dumps and allocation tracking, LeakCanary for retention.
   - Always compare **release** builds — debug builds are unrepresentative (no R8, no optimisation, no ART AOT).

3. **Get real-user data.** Ship field telemetry:
   - `FrameMetrics` percentiles per device model and screen.
   - `androidx.tracing` spans around critical sections (startup, parse, image decode, DB query) — collect a sampled trace from opt-in users for slow cases.
   - Play Console **Android Vitals** for startup, ANR rate, and jank across the device fleet.

4. **Prioritise by user impact, not by elegance.** Rank by (frequency × severity): cold start affects every launch; a slow list affects one screen. Fix the top contributors first.

5. **Cold start work.**
   - Move everything non-essential out of `Application.onCreate`; use `androidx.startup` and lazy initialisers.
   - Disable unnecessary `ContentProvider`s via the manifest.
   - Enforce R8 in release; keep an eye on the `keep` rules.
   - **Ship a Baseline Profile** generated by Macrobenchmark (`--profile`), installed via `androidx.profileinstaller`. This is typically the single biggest cold-start win on low-end devices — it removes JIT/interpretation from the startup path.
   - Target the **first frame of real content**, not just the splash — measure TTFD.
   - Avoid blocking disk I/O on the main thread (`StrictMode` in debug).

6. **Frame rendering work.**
   - Fix layout cost first: flatten hierarchies, drop `layout_weight`, use `ConstraintLayout`; prefer Compose for new screens.
   - Reduce main-thread work per frame: no allocations in `onBindViewHolder`/`onDraw`, no main-thread I/O, no expensive formatting in composition.
   - Virtualise lists (`LazyColumn`), use `key`, batch updates, throttle live feeds.
   - Move image decoding off the main thread and downsample (see Q5, Q13).
   - Gate on `FrameTimingMetric` in Macrobenchmark with a threshold.

7. **Memory work.**
   - Set a per-screen memory budget; measure with `MemoryUsageMetric`.
   - Page large datasets instead of materialising them; cap caches; evict on `onTrimMemory`.
   - Fix leaks with LeakCanary in CI-friendly mode (a leak test per screen).
   - Reduce graphics memory: downsample images, `RGB_565` where appropriate.
   - Avoid `largeHeap`; treat it as a symptom.

8. **Make it a pipeline, not a project.**
   - CI runs Macrobenchmark on an emulated low-end device and fails the build on regressions.
   - Baseline Profile is regenerated and committed when the startup path changes.
   - A weekly dashboard of the three metrics per device stratum.
   - Performance tests live in the same repo as the code and are owned by the team, not by a "performance task force".

9. **Report honestly.** Publish the numbers and the trend. A performance programme that cannot show a chart over time is not a programme.

**Trade-offs**

| Decision | Pros | Cons |
|---|---|---|
| Macrobenchmark in CI | Repeatable, catches regressions automatically | Requires dedicated/emulated devices; can be flaky under load; measures a narrow flow |
| Field `FrameMetrics` telemetry | Sees the real fleet, real devices | Volume, privacy, sampling, and aggregation cost; harder to act on |
| Baseline Profiles | Big cold-start and first-frame win on low-end | Must be regenerated as code changes; release-only |
| R8 full mode | Smaller dex, faster startup, obfuscation | Build time; reflection-based SDKs break without `keep` rules |
| Lazy SDK init (App Startup) | Removes SDK cost from the critical path | Requires SDK cooperation or manifest provider disabling |
| Strict budgets + gates | Prevents regression | Requires the team to accept the constraint; some legacy screens will need work |
| Optimising for the low-end fleet | Reaches the most users | Costs engineering time on devices most users do not own |

**Key detail:** Local numbers on a flagship device are close to meaningless for the field. A credible performance programme has **three legs**: repeatable local measurement (Macrobenchmark), real-user telemetry (FrameMetrics/Vitals), and a CI gate that blocks regressions — plus a Baseline Profile to convert the wins into shipped code.

**Code example**

```kotlin
// CI gate: startup + scrolling + memory, all in one Macrobenchmark module.
@RunWith(AndroidJUnit4::class)
class PerfGateTest {

    @get:Rule val perfRule = MacrobenchmarkRule()

    @Test fun coldStart_isUnderBudget() = perfRule.measureRepeated(
        packageName = TARGET,
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        setupBlock = { pressHome() },
        measureBlock = { startActivityAndWait() },
    )

    @Test fun scrollTransactionList_isSmooth() = perfRule.measureRepeated(
        packageName = TARGET,
        metrics = listOf(FrameTimingMetric()),
        iterations = 5,
        setupBlock = { pressHome(); startActivityAndWait() },
        measureBlock = { repeat(3) { scrollToEnd(); scrollToStart() } },
    )

    @Test fun memory_staysUnderBudget() = perfRule.measureRepeated(
        packageName = TARGET,
        metrics = listOf(MemoryUsageMetric(MemoryUsageMetric.MemoryMode.Total)),
        iterations = 3,
        setupBlock = { pressHome(); startActivityAndWait() },
        measureBlock = { scrollToEnd() },
    )
}

// Startup instrumentation in the app itself (sampled, privacy-safe).
class StartupTracer : Initializer<Unit> {
    override fun create(context: Context) {
        Trace.beginSection("AppInit")
        AppInitializer.getInstance(context).initialize(context)
        Trace.endSection()
    }
    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
```

**Talking points / follow-ups**
- *"What are the three metrics you would report to leadership?"* → Cold-start TTID p75 (real-user, from Vitals), janky-frame rate on the top screens, and peak memory/ANR rate — each with a target and a trend.
- *"How do you profile on a user's device?"* → You do not; you ship telemetry and sampled traces, and you reproduce on representative hardware in the lab.
- *"When is a Baseline Profile worth the maintenance?"* → Any app with a meaningful cold-start path (almost all of them). The cost is regenerating it when the startup path changes — automate it in CI.
- *"How do you prevent perf regressions from code review?"* → Macrobenchmark gates in CI, plus lint rules for known anti-patterns (main-thread I/O, `notifyDataSetChanged`, `BitmapFactory` without `inSampleSize`).
- *"How do you prioritise when everything is slow?"* → Measure, rank by user impact, fix the top item, re-measure. Never optimise without a baseline — and never claim a win without a before/after number.

---

## Section Summary — Staff-Level Themes

| Theme | Questions |
|---|---|
| **Measure before you fix** | 3, 6, 8, 10, 15 — profilers, `dumpsys gfxinfo`/`meminfo`, Perfetto, Macrobenchmark, field telemetry |
| **Virtualisation and windowing** | 1, 2, 11 — `LazyColumn` with `key`, Paging 3, Room as the window, never materialise 100k rows |
| **Identity and incremental updates** | 1, 3, 11 — `DiffUtil`/`AsyncListDiffer`, stable IDs, range notifications, batching, coalescing |
| **Main-thread discipline** | 4, 6, 9, 14 — off-main parsing, no allocations in bind, flattening hierarchies, derived state |
| **Compose recomposition model** | 4, 14 — scopes, skippability, `@Immutable`/`@Stable`, `derivedStateOf`, `remember`, state pushed down |
| **Memory as a budget** | 5, 8, 13, 15 — downsample at decode, LRU sized from `maxMemory`, native/graphics memory, `onTrimMemory` |
| **Startup as a discipline** | 12, 15 — `Application.onCreate` cost, `ContentProvider` init, App Startup, R8, Baseline Profiles |
| **Field reality** | 10, 15 — low-end device strata, thermal throttling, 120 Hz budget, real-user telemetry, CI gates |

**The one sentence to memorise for this section:**
> *"Performance is not an optimisation you do to the app — it is a budget you enforce. Name the metric (TTID, frame time, PSS), measure it on the hardware your users actually own, hold the architecture accountable (virtualise, downsample, page, defer, AOT), and put a CI gate on it so the win cannot quietly disappear."*
