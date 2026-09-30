# Android Interview Flashcards

> Topic-wise Q&A flashcards for Android interview preparation. Each card is self-contained for RAG retrieval.

---

# 1. Kotlin Fundamentals

---

### Q: What is the difference between `val`, `var`, and `const val` in Kotlin?

**A:**
- **`val`** declares a **read-only (immutable) reference**. Once assigned, it cannot be reassigned. However, the object it points to can still be mutable (e.g., a `val list = mutableListOf()`).
- **`var`** declares a **mutable variable**. Its value can be reassigned after initialization.
- **`const val`** declares a **compile-time constant**. It must be a top-level declaration or inside an `object`/`companion object`. It can only hold primitive types or `String`. The value is inlined at compile time wherever it's referenced.

```kotlin
val x: Int = 5      // immutable reference
var y: Int = 5       // mutable, can reassign: y = 10
const val PI = 3.14  // compile-time constant, must be top-level or in object
```

---

### Q: What is the difference between `lateinit` and `lazy` in Kotlin?

**A:**
- **`lateinit`** is used for **`var` properties** that will be initialized later (after object creation). It works only with **non-primitive, non-null types**. Accessing before initialization throws `UninitializedPropertyAccessException`. Commonly used for dependency injection or views.
- **`lazy`** is used for **`val` properties** that are computed on **first access**. It takes a lambda and returns a `Lazy<T>`. It is **thread-safe by default** and computes the value only once.

```kotlin
lateinit var name: String        // must be var, non-null, non-primitive
val config: String by lazy {     // must be val, computed once on first access
    "Computed value"
}
```

---

### Q: What is Null Safety in Kotlin? How does it work?

**A:** Kotlin's type system distinguishes between **nullable** (`String?`) and **non-nullable** (`String`) types at compile time, eliminating most `NullPointerException`s.

Key operators:
- **Safe call `?.`** - calls method only if not null, returns null otherwise: `name?.length`
- **Elvis operator `?:`** - provides a default when left side is null: `name?.length ?: 0`
- **Not-null assertion `!!`** - throws NPE if null: `name!!.length` (avoid unless certain)
- **Safe cast `as?`** - returns null instead of throwing ClassCastException: `obj as? String`

---

### Q: What is a Data Class in Kotlin?

**A:** A **data class** is a class primarily used to hold data. The compiler automatically generates `equals()`, `hashCode()`, `toString()`, `copy()`, and `componentN()` functions from the properties declared in the primary constructor.

Rules:
- Must have at least one parameter in the primary constructor
- All primary constructor parameters must be `val` or `var`
- Cannot be `abstract`, `open`, `sealed`, or `inner`

```kotlin
data class User(val name: String, val age: Int)
val user1 = User("Alice", 25)
val user2 = user1.copy(name = "Bob")  // copy with modification
```

---

### Q: What is a Sealed Class in Kotlin? How is it different from an Enum?

**A:** A **sealed class** restricts class hierarchy -- all direct subclasses must be defined in the same file (same package in Kotlin 1.5+). This allows the compiler to know all possible types, enabling **exhaustive `when` expressions** without an `else` branch.

**Difference from Enum:**
- **Enum**: Each value is a **single instance** (singleton). Cannot hold different state per instance.
- **Sealed class**: Each subclass can be a **class with its own state**, including data classes with different properties.

```kotlin
sealed class Result {
    data class Success(val data: String) : Result()
    data class Error(val exception: Exception) : Result()
    object Loading : Result()
}
```

---

### Q: What is the difference between `==` and `===` in Kotlin?

**A:**
- **`==`** is **structural equality** -- calls `equals()` under the hood. Compares values.
- **`===`** is **referential equality** -- checks if two references point to the **same object in memory**.

```kotlin
val a = "hello"
val b = "hello"
a == b   // true (same value)
a === b  // true (string pool optimization, same reference)

data class User(val name: String)
val u1 = User("A")
val u2 = User("A")
u1 == u2   // true (data class equals)
u1 === u2  // false (different objects)
```

---

### Q: What are Extension Functions in Kotlin?

**A:** Extension functions allow you to **add new functions to existing classes without modifying their source code** or using inheritance. They are resolved **statically** at compile time (not dynamically dispatched).

```kotlin
fun String.addExclamation(): String = "$this!"
"Hello".addExclamation()  // "Hello!"
```

Key points:
- They don't actually modify the class
- They are resolved statically (based on declared type, not runtime type)
- They cannot access private members of the class

---

### Q: What are Higher-Order Functions in Kotlin?

**A:** A **higher-order function** is a function that takes another function as a parameter or returns a function. This is fundamental to Kotlin's functional programming support.

```kotlin
fun operate(a: Int, b: Int, operation: (Int, Int) -> Int): Int {
    return operation(a, b)
}

val sum = operate(3, 4) { x, y -> x + y }  // 7
```

Common stdlib higher-order functions: `map`, `filter`, `reduce`, `fold`, `forEach`, `let`, `run`, `apply`, `also`.

---

### Q: What is the `inline` keyword in Kotlin? Why use it?

**A:** The `inline` keyword tells the compiler to **copy the function body and lambda body directly at the call site** instead of creating a function object for the lambda.

**Benefits:**
- Eliminates lambda object allocation overhead
- Allows `return` from enclosing function inside the lambda (non-local return)

**When to use:** When the function takes lambda parameters, especially for small utility functions.

```kotlin
inline fun measureTime(block: () -> Unit) {
    val start = System.currentTimeMillis()
    block()
    println("Time: ${System.currentTimeMillis() - start}ms")
}
```

---

### Q: What are the five Scope Functions in Kotlin? How do they differ?

**A:** Scope functions execute a block of code in the context of an object. They differ in how they reference the object and what they return.

| Function | Object Reference | Return Value | Use Case |
|----------|-----------------|-------------|----------|
| `let` | `it` | Lambda result | Null checks, transformations |
| `run` | `this` | Lambda result | Object config + compute result |
| `with` | `this` | Lambda result | Calling multiple methods on object |
| `apply` | `this` | Context object | Object configuration (builder-style) |
| `also` | `it` | Context object | Side effects (logging, validation) |

```kotlin
// let - null safety
name?.let { println(it) }

// apply - configure object
val paint = Paint().apply { color = Color.RED; style = Style.FILL }

// also - side effects
list.also { Log.d("TAG", "list size: ${it.size}") }
```

---

### Q: What is the difference between `object` and `companion object` in Kotlin?

**A:**
- **`object`** declares a **singleton** -- a class with exactly one instance, created lazily on first access. Used for utility classes, state managers, etc.
- **`companion object`** is an object tied to a **class** (not an instance). It provides static-like members accessible via the class name. Each class can have only one companion object.

```kotlin
object DatabaseManager {              // Singleton
    fun connect() { }
}

class MyClass {
    companion object {                // Static-like members
        const val TAG = "MyClass"
        fun create(): MyClass = MyClass()
    }
}
MyClass.TAG          // access like static
MyClass.create()
```

---

### Q: What is the difference between `List` and `MutableList` in Kotlin?

**A:**
- **`List`** is a **read-only** interface. You cannot add, remove, or modify elements.
- **`MutableList`** extends `List` and adds **mutation operations** like `add()`, `remove()`, `set()`.

```kotlin
val readOnly: List<Int> = listOf(1, 2, 3)         // immutable reference, read-only
val mutable: MutableList<Int> = mutableListOf(1, 2, 3)  // can add/remove
mutable.add(4)
```

The same pattern applies to `Set`/`MutableSet` and `Map`/`MutableMap`.

---

### Q: What are Generics in Kotlin? What is `in` and `out`?

**A:** Generics allow writing type-safe classes and functions that work with any type.

- **`out` (covariance)** - The type parameter is only **produced** (returned), never consumed. `Producer<out T>` means `Producer<Dog>` is a subtype of `Producer<Animal>`.
- **`in` (contravariance)** - The type parameter is only **consumed** (passed in), never produced. `Consumer<in T>` means `Consumer<Animal>` is a subtype of `Consumer<Dog>`.

```kotlin
interface Producer<out T> { fun produce(): T }       // covariant
interface Consumer<in T> { fun consume(item: T) }    // contravariant
```

Mnemonic: **`out` = output/produce, `in` = input/consume**.

---

### Q: What is a Coroutine Scope function `coroutineScope` vs `supervisorScope`?

**A:**
- **`coroutineScope`**: If any child coroutine fails, it **cancels all other children** and the scope itself fails.
- **`supervisorScope`**: If a child fails, **other children continue running**. The failure doesn't propagate to siblings.

```kotlin
coroutineScope {
    launch { throw Exception() }   // ALL children cancelled
    launch { /* also cancelled */ }
}

supervisorScope {
    launch { throw Exception() }   // only this child fails
    launch { /* still runs */ }
}
```

Use `supervisorScope` when child tasks are independent (e.g., loading multiple UI sections).

---

# 2. Android Components

---

### Q: What is an Activity in Android?

**A:** An **Activity** is an Android component that provides a **window for the app to draw its UI**. It represents a single screen with a user interface. Each Activity goes through a defined **lifecycle** managed by the system.

Key points:
- Generally, one Activity = one screen
- The window typically fills the screen but can float on top of other windows
- Activities are managed in a **back stack** (LIFO)

---

### Q: What are the Activity Lifecycle methods? Explain each.

**A:**

1. **`onCreate()`** - Called when Activity is **first created** (once per instance). Initialize views, restore state.
2. **`onStart()`** - Activity becomes **visible** but not yet interactive.
3. **`onResume()`** - Activity is in **foreground**, user can interact.
4. **`onPause()`** - Activity is **partially obscured** (dialog, another activity). Pause animations, save lightweight data.
5. **`onStop()`** - Activity is **completely hidden**. Release resources not needed off-screen.
6. **`onRestart()`** - Called when returning from stopped state (user navigates back).
7. **`onDestroy()`** - Activity is being **destroyed** (finish() called or configuration change). Cleanup to avoid memory leaks.

**Flow:** `onCreate` -> `onStart` -> `onResume` -> [Running] -> `onPause` -> `onStop` -> `onDestroy`

---

### Q: What happens to the Activity lifecycle when navigating from Activity A to Activity B and pressing back?

**A:**

**Launch A -> B:**
- A: `onPause()`
- B: `onCreate()` -> `onStart()` -> `onResume()`
- A: `onStop()`

**Press Back (B -> A):**
- B: `onPause()`
- A: `onRestart()` -> `onStart()` -> `onResume()`
- B: `onStop()` -> `onDestroy()`

Key insight: The **new activity is fully created before the old one stops**.

---

### Q: What happens to an Activity when the device is rotated?

**A:** The Activity is **destroyed and recreated** by default. Rotation triggers a **configuration change**.

Lifecycle: `onPause()` -> `onStop()` -> `onDestroy()` -> `onCreate()` -> `onStart()` -> `onResume()`

To retain data across rotation:
- **ViewModel** (recommended) - survives configuration changes
- **`onSaveInstanceState()`** - save small data in Bundle, restore in `onCreate(savedInstanceState)`

---

### Q: What is the Android Activity priority system? Which activities get killed first?

**A:** When system is low on memory, it kills processes from **lowest to highest priority**:

1. **Foreground Activity** (highest) - visible and interacting with user
2. **Visible Activity** - visible but not in foreground (dialog on top)
3. **Service Process** - running foreground/background service
4. **Background Activity** - not visible, in back stack
5. **Empty Process** (lowest) - no active components, cached for faster reload

When multiple background activities compete, the system considers **memory usage**, **process age**, and **LRU order** (least recently used killed first).

---

### Q: What is a Fragment in Android? How does its lifecycle differ from Activity?

**A:** A **Fragment** represents a reusable portion of an app's UI. It has its own lifecycle but is always hosted within an Activity and its lifecycle is directly affected by the host Activity's lifecycle.

**Additional lifecycle methods compared to Activity:**
- `onAttach()` - Fragment attached to Activity (get context)
- `onCreateView()` - Inflate the layout
- `onViewCreated()` - View fully created, safe to setup UI
- `onDestroyView()` - View being destroyed (nullify bindings to prevent leaks)
- `onDetach()` - Fragment disassociated from Activity

**Order:** `onAttach` -> `onCreate` -> `onCreateView` -> `onViewCreated` -> `onStart` -> `onResume` -> `onPause` -> `onStop` -> `onDestroyView` -> `onDestroy` -> `onDetach`

---

### Q: What is a Service in Android? What are the types?

**A:** A **Service** is an Android component that performs **long-running operations in the background** without a UI. By default, it runs on the **Main Thread**.

**Types:**

1. **Started Service** - Started with `startService()`. Runs indefinitely until stopped. Return flags:
   - `START_STICKY` - System restarts service if killed
   - `START_NOT_STICKY` - System does NOT restart service

2. **Bound Service** - Bound with `bindService()`. Lives as long as a component is bound to it. Good for client-server communication.

3. **Foreground Service** - Displays a persistent notification. Higher priority, less likely to be killed. Required for long tasks on Android 8.0+.

---

### Q: What is WorkManager? When should you use it?

**A:** **WorkManager** is a Jetpack library for **deferrable, guaranteed background work** that executes even if the app exits or device restarts.

**Use when tasks:**
- Should run even if the app process dies
- Are guaranteed to execute
- Can be delayed or scheduled with constraints (network, charging, etc.)

**Types of work:**
- `OneTimeWorkRequest` - runs once
- `PeriodicWorkRequest` - repeats at intervals (minimum 15 minutes)
- `UniqueWorkRequest` - avoids duplication with policies (KEEP, REPLACE, APPEND)

**Features:** Chaining with `.then()`, constraints (network, charging, battery), observing status via LiveData.

---

### Q: What is a BroadcastReceiver in Android?

**A:** A **BroadcastReceiver** is a component that lets your app **listen to and respond to system-wide or app-specific broadcast events** (like a radio receiver tuned to specific channels).

**Types of Broadcasts:**
- **System broadcasts** - Sent by Android system (`BOOT_COMPLETED`, `BATTERY_LOW`, `CONNECTIVITY_ACTION`)
- **Custom broadcasts** - Sent by your app for internal communication

**Types of Receivers:**
- **Static (Manifest-declared)** - Works even when app is not running. Limited on Android 8.0+ for most implicit broadcasts.
- **Dynamic (Context-registered)** - Registered in code with `registerReceiver()`. Active only while app/component is running. Must unregister to prevent memory leaks.

---

### Q: What is a ContentProvider in Android?

**A:** A **ContentProvider** manages access to **structured data** and acts as a bridge between apps for **secure data sharing**. It uses a **URI-based interface** and supports CRUD operations.

**Types:**
- **Standard (System-defined)** - `ContactsContract`, `MediaStore`, `CalendarContract`
- **Custom (User-defined)** - Extend `ContentProvider` class

**When to use:**
- Share data between apps (contacts, media, files)
- Need data abstraction with permission control
- Access structured data consistently via URIs

**When NOT to use:** If data is purely local, use Room/SQLite directly.

---

### Q: What is an Intent in Android? What are the types?

**A:** An **Intent** is a messaging object used to request an action from another app component. It carries information about **what action to perform** and **data to act upon**.

**Types:**

1. **Explicit Intent** - Specifies the **exact component** (class name) to start. Used for internal app navigation.
   ```kotlin
   val intent = Intent(this, SecondActivity::class.java)
   startActivity(intent)
   ```

2. **Implicit Intent** - Declares a **general action** to perform. The system finds the appropriate component (may show chooser).
   ```kotlin
   val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://google.com"))
   startActivity(intent)
   ```

---

### Q: What are the four Launch Modes in Android? Explain each.

**A:** Launch modes define how a new instance of an Activity is associated with the current task.

1. **`standard`** (default) - Creates a **new instance every time**, even if one exists. Multiple instances can exist in the same task.

2. **`singleTop`** - If the Activity is already at the **top of the stack**, it reuses it (calls `onNewIntent()`). Otherwise creates new instance. Prevents duplicate on top.

3. **`singleTask`** - The Activity can only have **one instance in the entire system**. If it exists in any task, that task is brought to foreground and `onNewIntent()` is called. All activities above it in that task are destroyed.

4. **`singleInstance`** - Like `singleTask` but the Activity is the **only member of its task**. Any new activity launched from it goes into a different task.

---

### Q: What is a ViewModel in Android? Why does it survive configuration changes?

**A:** A **ViewModel** is an architecture component designed to **store and manage UI-related data** in a lifecycle-conscious way. It survives **configuration changes** (like screen rotation).

**Why it survives rotation:**
- ViewModel is stored in a `ViewModelStore` owned by the `ViewModelStoreOwner` (Activity/Fragment)
- During configuration change, the Activity is destroyed and recreated, but the `ViewModelStore` is **retained by the framework** and re-attached to the new Activity instance
- It is only cleared when the Activity is **finished** (not recreated)

**Key rules:**
- Never hold references to Activity, Fragment, or Views (causes memory leaks)
- Use `SavedStateHandle` if you need to survive process death

---

### Q: What is the difference between `onSaveInstanceState()` and ViewModel for retaining data?

**A:**

| Feature | `onSaveInstanceState()` | ViewModel |
|---------|------------------------|-----------|
| Survives config change | Yes | Yes |
| Survives process death | Yes | **No** (use `SavedStateHandle`) |
| Data type | Small, serializable (Bundle) | Any object |
| Storage limit | ~1MB (Bundle limit) | Limited by heap memory |
| Use case | Small UI state (scroll position, text input) | Large data (lists, API responses) |

**Best practice:** Use ViewModel for large/complex data + `SavedStateHandle` or `onSaveInstanceState()` for critical small state that must survive process death.

---

### Q: What is the difference between LiveData, StateFlow, and SharedFlow?

**A:**

| Feature | LiveData | StateFlow | SharedFlow |
|---------|----------|-----------|------------|
| Platform | Android-only | Kotlin (multiplatform) | Kotlin (multiplatform) |
| Lifecycle-aware | Yes (auto stops in background) | No (need `repeatOnLifecycle`) | No (need `repeatOnLifecycle`) |
| Initial value | Not required | **Required** | Not required |
| Replays last value | Yes (to new observers) | Yes (always has current value) | Configurable (`replay`) |
| Multiple collectors | N/A | Yes | Yes |
| Hot/Cold | Hot | **Hot** | **Hot** |
| Best for | Simple UI state | UI state in ViewModel | Events (one-shot, like navigation, snackbar) |

**Key insight:** `StateFlow` is like `LiveData` but Kotlin-native. `SharedFlow` is for **events** (no initial value, configurable replay).

---

### Q: What is the difference between Serializable and Parcelable in Android?

**A:**

| Feature | Serializable | Parcelable |
|---------|-------------|-----------|
| Origin | Java interface | Android-specific interface |
| Performance | **Slower** (uses reflection) | **Faster** (manual serialization) |
| Implementation | Just implement interface | Override `writeToParcel()`, `createFromParcel()` |
| Boilerplate | Zero | More (but `@Parcelize` in Kotlin eliminates it) |
| Use case | Simple cases, non-Android | Passing data between Android components (Intent, Bundle) |

**Kotlin shortcut:** Use `@Parcelize` annotation with `Parcelable` for zero boilerplate:
```kotlin
@Parcelize
data class User(val name: String, val age: Int) : Parcelable
```

---

# 3. Jetpack Compose

---

### Q: What is Jetpack Compose?

**A:** Jetpack Compose is Android's **modern declarative UI toolkit**. Instead of XML layouts and imperative view manipulation, you describe your UI as **composable functions** that transform state into UI. When state changes, Compose **automatically re-renders** only the affected parts.

Key concepts:
- **Declarative** - You describe *what* the UI should look like, not *how* to update it
- **Composable functions** - Functions annotated with `@Composable` that emit UI
- **State-driven** - UI is a function of state: `UI = f(state)`

---

### Q: What is Recomposition in Jetpack Compose?

**A:** **Recomposition** is the process where Compose **re-executes composable functions** when their input state changes, updating only the parts of the UI tree that have changed.

Key points:
- Compose tracks which composables read which state
- Only composables that read changed state are recomposed (smart recomposition)
- Composable functions can run in **any order**, be **skipped**, or run **in parallel**
- Recomposition is **optimistic** -- Compose expects it to finish; if state changes during recomposition, it may restart

**Stability matters:** Compose skips recomposition for composables whose parameters haven't changed (if parameters are **stable types** like primitives, strings, or `@Stable`/`@Immutable` classes).

---

### Q: What is `remember` in Jetpack Compose? What about `rememberSaveable`?

**A:**
- **`remember`** stores a value in the Composition. It **survives recomposition** but is **lost on configuration change** (rotation) or process death.
- **`rememberSaveable`** stores a value that **survives both recomposition AND configuration changes**. It uses the `savedInstanceState` mechanism internally.

```kotlin
var count by remember { mutableStateOf(0) }                // lost on rotation
var count by rememberSaveable { mutableStateOf(0) }        // survives rotation
```

Use `remember` for transient UI state. Use `rememberSaveable` for state that must survive rotation.

---

### Q: What is `State` and `MutableState` in Compose?

**A:** `State<T>` is a **value holder** that Compose can observe. When the value changes, any composable reading it is automatically **scheduled for recomposition**.

- `mutableStateOf(value)` creates a `MutableState<T>` -- readable and writable
- Use `by` delegation for convenient property syntax

```kotlin
var name by remember { mutableStateOf("") }  // delegate syntax
name = "Alice"  // triggers recomposition of readers
```

Other state types: `mutableStateListOf()`, `mutableStateMapOf()`, `derivedStateOf { }` (computed state).

---

### Q: What are Side Effects in Jetpack Compose? Name the key ones.

**A:** Side effects are operations that **escape the scope of a composable function** (like launching coroutines, registering callbacks, writing to external state). Compose provides controlled APIs for them:

1. **`LaunchedEffect(key)`** - Launches a coroutine scoped to the Composition. Cancels and relaunches when key changes. Use for one-time operations or key-dependent async work.

2. **`DisposableEffect(key)`** - For effects that need **cleanup** (like registering/unregistering listeners). Provides `onDispose` block.

3. **`SideEffect`** - Runs after **every successful recomposition**. Use to sync Compose state with non-Compose code.

4. **`rememberCoroutineScope()`** - Returns a coroutine scope tied to the Composition. Use to launch coroutines from event handlers (like button clicks).

5. **`derivedStateOf`** - Creates derived state that only recomputes when its dependencies change. Reduces unnecessary recompositions.

6. **`rememberUpdatedState(value)`** - Captures the latest value in a long-lived lambda (e.g., inside `LaunchedEffect`) without restarting the effect.

7. **`produceState`** - Converts non-Compose state (like Flow) into Compose State.

---

### Q: What is the difference between `LaunchedEffect` and `rememberCoroutineScope`?

**A:**
- **`LaunchedEffect(key)`** - Tied to the **Composition lifecycle**. Auto-launches and auto-cancels. Relaunches when key changes. Use for effects that should run when entering composition or when a key changes.
- **`rememberCoroutineScope()`** - Returns a scope you call **manually** from event handlers. Use when you need to launch coroutines in response to **user actions** (button clicks, gestures).

```kotlin
// LaunchedEffect - runs automatically when userId changes
LaunchedEffect(userId) { viewModel.loadUser(userId) }

// rememberCoroutineScope - runs on user action
val scope = rememberCoroutineScope()
Button(onClick = { scope.launch { viewModel.save() } })
```

---

# 4. Kotlin Coroutines & Flow

---

### Q: What is a Coroutine in Kotlin?

**A:** A **coroutine** is a lightweight, suspendable computation. Unlike threads, coroutines are **not bound to a particular thread** -- they can suspend execution in one thread and resume in another. They are managed by the Kotlin runtime, not the OS.

Key properties:
- **Lightweight** -- thousands of coroutines can run concurrently (unlike threads)
- **Suspendable** -- can pause without blocking the thread
- **Structured concurrency** -- coroutines are organized in a parent-child hierarchy; parent waits for all children to complete

---

### Q: What are Coroutine Dispatchers? Name the main ones.

**A:** A **Dispatcher** determines which **thread or thread pool** a coroutine runs on.

| Dispatcher | Thread | Use Case |
|-----------|--------|----------|
| `Dispatchers.Main` | Main/UI thread | UI updates, lightweight work |
| `Dispatchers.IO` | Shared pool (64+ threads) | Network calls, file I/O, database |
| `Dispatchers.Default` | Shared pool (CPU cores) | CPU-intensive work (sorting, parsing, computation) |
| `Dispatchers.Unconfined` | Caller thread initially, then any | Testing, special cases (avoid in production) |

```kotlin
withContext(Dispatchers.IO) { /* network call */ }
withContext(Dispatchers.Default) { /* heavy computation */ }
```

---

### Q: What is the difference between `launch` and `async` in coroutines?

**A:**
- **`launch`** starts a coroutine and returns a **`Job`**. It is "fire and forget" -- does not return a result. Use for coroutines where you don't need a return value.
- **`async`** starts a coroutine and returns a **`Deferred<T>`** (a future with a result). Call `.await()` to get the result. Use when you need a computed value.

```kotlin
// launch - fire and forget
val job = launch { doSomething() }

// async - returns result
val deferred = async { computeValue() }
val result = deferred.await()
```

**Key:** `async` without `await` is effectively the same as `launch`. Always `await` your `async` results.

---

### Q: What is Structured Concurrency?

**A:** **Structured concurrency** means coroutines are organized in a **parent-child hierarchy** through `CoroutineScope`. This guarantees:

1. **A parent waits for all children** to complete before completing itself
2. **Cancelling a parent cancels all children** (and their children)
3. **A child's failure propagates to the parent** (and cancels siblings, unless using `supervisorScope`)
4. **No coroutine leaks** -- every coroutine has a well-defined lifetime

This is enforced through scopes: `viewModelScope`, `lifecycleScope`, `coroutineScope { }`.

---

### Q: What is a Flow in Kotlin? How is it different from a suspend function?

**A:** A **Flow** is a **cold asynchronous stream** that emits multiple values sequentially. A suspend function returns a **single value**; a Flow emits **multiple values over time**.

- **Cold** -- code inside `flow { }` only executes when collected
- **Sequential** -- values are emitted one at a time
- **Cancellable** -- respects coroutine cancellation

```kotlin
fun numbersFlow(): Flow<Int> = flow {
    emit(1)
    delay(100)
    emit(2)
    emit(3)
}

// Collecting
numbersFlow().collect { value -> println(value) }
```

Key operators: `map`, `filter`, `flatMapConcat`, `zip`, `combine`, `catch`, `onEach`, `stateIn`, `shareIn`.

---

### Q: What is the difference between StateFlow and SharedFlow?

**A:**

| Feature | StateFlow | SharedFlow |
|---------|-----------|------------|
| Initial value | **Required** | Not required |
| Current value | Always has `.value` | No `.value` property |
| Replay | Always replays last value (replay=1) | Configurable (default replay=0) |
| Conflation | **Conflated** (skips intermediate values) | Not conflated by default |
| Best for | **UI state** (like LiveData replacement) | **Events** (navigation, snackbar, one-shot) |

```kotlin
val _state = MutableStateFlow(UiState())        // always has current value
val _events = MutableSharedFlow<Event>()         // events, no initial value
```

---

### Q: What is a Channel in Kotlin Coroutines?

**A:** A **Channel** is a **hot, concurrent communication primitive** for transferring values between coroutines. It's like a blocking queue but with suspend functions instead of blocking.

- **Hot** -- exists regardless of receivers
- **One-to-one** -- each value is received by exactly one receiver (unlike Flow which can have multiple collectors)
- Types: `RENDEZVOUS` (default, no buffer), `BUFFERED`, `CONFLATED`, `UNLIMITED`

```kotlin
val channel = Channel<Int>()
launch { channel.send(1) }
launch { val value = channel.receive() }  // gets 1
```

Use `Flow` for reactive streams. Use `Channel` for point-to-point communication between coroutines.

---

# 5. Threading

---

### Q: What is the relationship between Thread, Looper, Handler, and MessageQueue in Android?

**A:**
- **Thread** - A unit of execution. Android's main thread (UI thread) is a special thread with a Looper.
- **Looper** - An infinite loop that processes messages from a **MessageQueue**. The main thread has a Looper by default; worker threads don't (unless you call `Looper.prepare()`).
- **MessageQueue** - A queue of `Message` and `Runnable` objects waiting to be processed.
- **Handler** - Allows you to **send and process Messages/Runnables** on a specific thread's MessageQueue. You create a Handler with a Looper to communicate with that thread.

**Flow:** Handler posts Message -> MessageQueue -> Looper picks it up -> Handler processes it.

```kotlin
val handler = Handler(Looper.getMainLooper())
handler.post { /* runs on main thread */ }
```

---

### Q: Why can't you update the UI from a background thread in Android?

**A:** Android's UI toolkit is **not thread-safe**. The View system expects all modifications from a single thread (the main/UI thread). Accessing Views from a background thread causes `CalledFromWrongThreadException`.

**Solutions to update UI from background:**
- `Handler(Looper.getMainLooper()).post { }`
- `runOnUiThread { }` (from Activity)
- `withContext(Dispatchers.Main) { }` (coroutines)
- `LiveData.postValue()` / `StateFlow` (reactive)

---

# 6. Dependency Injection

---

### Q: What is Dependency Injection? How is it different from Service Locator?

**A:** **Dependency Injection (DI)** is a design pattern where an object **receives its dependencies from outside** rather than creating them itself. This improves testability, modularity, and loose coupling.

**Service Locator** is a pattern where a central **registry** provides dependencies on request. The object **asks** for its dependencies.

| Feature | Dependency Injection | Service Locator |
|---------|---------------------|----------------|
| Who provides dependencies? | External (injector/framework) | Object requests from registry |
| Coupling | Low (doesn't know about container) | Medium (knows about locator) |
| Testability | Easy (inject mocks) | Harder (must setup locator) |
| Compile-time safety | Yes (with Dagger/Hilt) | No (runtime errors) |
| Android framework | Dagger, Hilt, Koin | Koin (also acts as SL) |

**Hilt** (built on Dagger) is the recommended DI framework for Android.

---

# 7. Networking

---

### Q: What is REST? What are the main HTTP methods?

**A:** **REST (Representational State Transfer)** is an architectural style for designing networked applications using HTTP. Resources are identified by URIs and manipulated using standard HTTP methods.

| Method | Purpose | Idempotent | Safe |
|--------|---------|-----------|------|
| `GET` | Retrieve data | Yes | Yes |
| `POST` | Create new resource | No | No |
| `PUT` | Update/replace entire resource | Yes | No |
| `PATCH` | Partial update | No | No |
| `DELETE` | Remove resource | Yes | No |

**Idempotent** = same request produces same result regardless of how many times called.

---

### Q: What is an OkHttp Interceptor? What are the types?

**A:** An **Interceptor** in OkHttp is a mechanism to **observe, modify, and potentially short-circuit requests and responses**. They form a chain that processes each network call.

**Types:**
- **Application Interceptor** (`.addInterceptor()`) - Called once per request. Sees the original request. Can short-circuit without calling network. Used for: logging, adding headers, authentication tokens.
- **Network Interceptor** (`.addNetworkInterceptor()`) - Called for each network call (including redirects). Sees the actual network request/response. Used for: caching headers, network-level logging.

```kotlin
val client = OkHttpClient.Builder()
    .addInterceptor(AuthInterceptor())           // application
    .addNetworkInterceptor(LoggingInterceptor()) // network
    .build()
```

---

# 8. Android Security

---

### Q: What is the Android Keystore System?

**A:** The **Android Keystore System** provides a **secure container** to store cryptographic keys. Keys stored in the Keystore are **protected by hardware-backed security** (TEE/StrongBox on supported devices) and cannot be extracted from the device.

Key features:
- Keys **never leave the Keystore** -- all crypto operations happen inside it
- Supports **key use authorization** (require user authentication, biometrics)
- Hardware-backed on most modern devices (Trusted Execution Environment)
- Used for: encryption, signing, secure authentication

---

### Q: What is EncryptedSharedPreferences?

**A:** **EncryptedSharedPreferences** is a Jetpack Security library wrapper around SharedPreferences that **encrypts both keys and values** using AES-256.

- Uses **Android Keystore** to securely store the encryption key
- Provides the same `SharedPreferences` API -- drop-in replacement
- Keys are encrypted with **deterministic AES-SIV** (allows lookup)
- Values are encrypted with **AES-GCM** (authenticated encryption)

```kotlin
val sharedPrefs = EncryptedSharedPreferences.create(
    "secret_prefs",
    masterKeyAlias,
    context,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

---

# 9. OOP Concepts

---

### Q: What are the four pillars of Object-Oriented Programming?

**A:**

1. **Encapsulation** - Bundling data (properties) and methods that operate on that data into a single unit (class), and **restricting direct access** to internal state using access modifiers. The user interacts through a public interface.

2. **Abstraction** - **Hiding complex implementation details** and exposing only the essential features. Focuses on *what* an object does, not *how*. Achieved through abstract classes and interfaces.

3. **Inheritance** - Mechanism that allows a class to **inherit properties and methods** from another class. Promotes code reuse. The child class can override parent behavior.

4. **Polymorphism** - The ability to take **multiple forms**. Two types:
   - **Compile-time (static)** - Method overloading (same name, different parameters)
   - **Runtime (dynamic)** - Method overriding (subclass provides specific implementation)

---

### Q: What is the difference between Abstraction and Encapsulation?

**A:**
- **Abstraction** is about **hiding complexity** -- showing only relevant features to the user. It answers "*what* does this do?" Example: abstract class `Shape` with abstract `area()`.
- **Encapsulation** is about **hiding data** -- restricting access to internal state. It answers "*how* is this protected?" Example: `private var speed` with public `getSpeed()`.

**Analogy:** A car's steering wheel is **abstraction** (you don't know the internal mechanism). The engine compartment being locked is **encapsulation** (you can't directly access internals).

---

### Q: What is the difference between Method Overloading and Method Overriding?

**A:**

| Feature | Overloading | Overriding |
|---------|------------|-----------|
| Type | Compile-time polymorphism | Runtime polymorphism |
| Where | Same class | Parent-child classes |
| Method name | Same | Same |
| Parameters | **Different** (type/count) | **Same** |
| Return type | Can be different | Must be same or covariant |
| Keyword | None | `override` in Kotlin |

```kotlin
// Overloading
fun draw() { }
fun draw(radius: Double) { }

// Overriding
open class Shape { open fun draw() { } }
class Circle : Shape() { override fun draw() { } }
```

---

### Q: What is the difference between a Function and a Method?

**A:**
- **Function** - A set of instructions to perform a task. It is **independent** and can work with provided data.
- **Method** - A function that is **associated with an object/class**. It can access all data within the class.

In Kotlin, top-level functions exist outside classes (true functions), while member functions are methods.

---

# 10. SOLID Principles

---

### Q: What is the Single Responsibility Principle (SRP)?

**A:** **A class should have only one reason to change.** Each class should do one thing and do it well.

**Example violation:** A `UserManager` that creates users, updates users, AND sends emails. It has multiple reasons to change (database logic, email logic).

**Fix:** Separate into `UserDatabaseService` (create/update users) and `EmailService` (send emails). `UserManager` delegates to both.

> SRP does NOT mean a class should have only one method. It can have multiple methods as long as they serve a single responsibility.

---

### Q: What is the Open/Closed Principle (OCP)?

**A:** Software entities should be **open for extension but closed for modification**. You should be able to add new functionality **without changing existing code**.

**Example violation:** A `Shape` class with `if/else` in `area()` for each shape type. Adding a new shape requires modifying the method.

**Fix:** Make `Shape` abstract with abstract `area()`. Each shape (Rectangle, Circle) extends it. New shapes are added by creating new classes, not modifying existing code.

---

### Q: What is the Liskov Substitution Principle (LSP)?

**A:** **Subtypes must be substitutable for their base types** without altering program correctness. Any instance of a parent class should be replaceable by an instance of a child class.

**Example violation:** `Ostrich extends Bird` where `Bird.fly()` exists. Calling `fly()` on Ostrich throws an exception -- breaks the contract.

**Fix:** Separate `Bird` (with `eat()`) from `FlyingBird` (adds `fly()`). Sparrow implements `FlyingBird`, Ostrich implements only `Bird`. Functions expecting `FlyingBird` never receive Ostrich.

---

### Q: What is the Interface Segregation Principle (ISP)?

**A:** **Clients should not be forced to depend on interfaces they do not use.** Break up large interfaces into smaller, focused ones.

**Example violation:** `Worker` interface with `work()` and `eat()`. `RobotWorker` is forced to implement `eat()` which it doesn't need.

**Fix:** Split into `Workable` (with `work()`) and `Eatable` (with `eat()`). `HumanWorker` implements both. `RobotWorker` implements only `Workable`.

---

### Q: What is the Dependency Inversion Principle (DIP)?

**A:** **High-level modules should not depend on low-level modules. Both should depend on abstractions.** Depend on interfaces/abstract classes rather than concrete implementations.

**Example violation:** `UserService` directly creates `DatabaseService` instance. Switching databases requires modifying `UserService`.

**Fix:** Create `Database` interface. `DatabaseService` implements it. `UserService` depends on `Database` interface (injected via constructor). Easy to swap implementations and test with mocks.

---

# 11. Design Patterns

---

### Q: What is the Singleton Pattern?

**A:** **Singleton** is a creational design pattern that ensures a class has **only one instance** and provides a **global access point** to it.

**Steps:**
1. Make the constructor **private**
2. Create a **static method** that returns the single instance (creates it if it doesn't exist)

**In Kotlin:** Simply use `object` declaration -- it's a built-in thread-safe singleton.

```kotlin
object DatabaseManager {
    fun connect() { }
}
```

**Cons:** Breaks modularity and SRP (acts as global state). Makes unit testing harder.

---

### Q: What is the Factory Method Pattern?

**A:** **Factory Method** is a creational pattern that defines an **interface for creating objects** but lets **subclasses decide** which class to instantiate. Also known as the **Virtual Constructor**.

**Key idea:** Instead of calling `new ConcreteClass()` directly, you call a factory method that returns the appropriate subclass.

**Pros:** Adheres to SRP and OCP -- adding new product types doesn't modify existing code.
**Cons:** Can lead to many subclasses.

It heavily relies on **inheritance** -- each concrete creator overrides the factory method.

---

### Q: What is the Abstract Factory Pattern?

**A:** **Abstract Factory** is a creational pattern that produces **families of related objects** without specifying concrete classes. It's a factory of factories.

**Difference from Factory Method:**
- **Factory Method** creates **one product** via inheritance
- **Abstract Factory** creates **families of related products** via composition

**Example:** A `PenFactory` interface with `getBody()` and `getRefill()`. `BlueGelPenFactory` returns gel body + blue refill. `BlackBallPenFactory` returns plastic body + black refill. Products from the same factory are guaranteed compatible.

---

### Q: What is the Builder Pattern?

**A:** **Builder** is a creational pattern that allows constructing **complex objects step by step**. The same construction process can create different representations.

**When to use:** When an object has many optional parameters and constructors become unwieldy (telescoping constructor problem).

```kotlin
val house = MyHouseBuilder().apply {
    buildWalls(4)
    buildDoor(2)
    buildWindows(4)
    buildGarden()
}.getResult()
```

**Pros:** Step-by-step construction, reusable construction code.
**Note:** In Kotlin, named arguments and default parameters often replace Builder pattern.

---

### Q: What is the Prototype Pattern?

**A:** **Prototype** is a creational pattern that lets you **copy existing objects** without depending on their specific classes. The object provides a `clone()` method.

**When to use:** When creating an object is expensive (complex initialization) and you need copies with slight variations.

```kotlin
interface Prototype { fun clone(): Prototype }

class ToyDuck(var color: String, var date: String) : Prototype {
    override fun clone() = ToyDuck(color, date)
}
```

**Note:** In Kotlin, `data class` provides `copy()` which serves the same purpose.

---

### Q: What is the Adapter Pattern?

**A:** **Adapter** is a structural pattern that acts as a **bridge between two incompatible interfaces**, allowing them to work together without modifying either.

**Analogy:** A US-to-UK power adapter lets your US plug work with a UK socket -- neither plug nor socket changes.

**Two types:**
- **Object Adapter** -- Uses **composition** (has-a). Wraps the adaptee.
- **Class Adapter** -- Uses **multiple inheritance** (is-a). Not applicable in Java/Kotlin (single inheritance).

```
Existing System (no changes) --> Adapter (bridge) --> Old Code (no changes)
```

---

### Q: What is the Observer Pattern?

**A:** **Observer** is a behavioral pattern that allows objects (observers) to be **notified of changes** in another object (subject). It establishes a one-to-many dependency.

**Two models:**
- **Push model** -- Subject pushes update data directly to observers
- **Pull model** -- Observers request/pull data from subject when notified

**In Android:** LiveData, Flow, RxJava all implement the Observer pattern. `LiveData.observe()` is a classic example.

---

### Q: What is the Strategy Pattern?

**A:** **Strategy** is a behavioral pattern that defines a **family of interchangeable algorithms**, encapsulates each one, and makes them **swappable at runtime**.

**When to use:**
- Need to switch behavior dynamically at runtime
- Multiple variations of an algorithm exist
- Want to avoid large `if/else` or `when` blocks
- Want to decouple algorithm from the class using it

**Example:** Payment processing with `CreditCardStrategy`, `PayPalStrategy`, `ApplePayStrategy` -- all implement `PaymentStrategy`. The context can switch strategy at runtime.

**Difference from inheritance:** Strategy uses **composition** (has-a strategy), allowing runtime swapping. Inheritance is static.

---

### Q: What is the Iterator Pattern?

**A:** **Iterator** is a behavioral pattern that lets you **traverse elements of a collection** without exposing its underlying representation (array, linked list, tree, etc.).

**Components:**
- **Iterator interface** -- declares `hasNext()`, `next()`
- **Concrete Iterator** -- implements traversal algorithm
- **Iterable interface** -- declares `iterator()` method
- **Concrete Collection** -- returns iterator instance

Kotlin collections already implement this pattern via `Iterable<T>` and `Iterator<T>`.

---

# 12. Object-Oriented Design & UML

---

### Q: What are the UML class relationships? Explain Association, Aggregation, Composition, Inheritance.

**A:**

1. **Association (has-a)** `─────>` -- Two classes are related through their objects. Can be one-to-one, one-to-many, many-to-many. Both can exist independently.

2. **Aggregation (has-a, weak)** `◇─────>` -- A special form of association. The **whole** contains **parts**, but parts can exist independently. (e.g., Department has Teachers -- teachers exist without department)

3. **Composition (has-a, part-of, strong)** `◆─────>` -- A stronger form of aggregation. Parts **cannot exist** without the whole. If the whole is destroyed, parts are destroyed. (e.g., House has Rooms -- rooms don't exist without house)

4. **Inheritance (is-a)** `──────▷` -- Child class extends parent class. Solid line with hollow arrow toward parent.

5. **Implementation (is-a)** `- - - -▷` -- Class implements interface. Dashed line with hollow arrow toward interface.

6. **Dependency** `- - - ->` -- One class uses another temporarily (e.g., as a method parameter).

---

### Q: What is a Use Case Diagram? What are its components?

**A:** A **Use Case Diagram** shows the **interactions between actors and the system** at a high level.

**Components:**
- **Actor** -- User or external system that interacts with the system (Primary = initiates, Secondary = assists)
- **Use Case** -- Actions performed on the system by actors (shown as ovals)

**Relationships:**
- **Association** (solid line) -- Actor connects to use case
- **Include** `<<include>>` (dashed arrow toward included) -- Base use case **always** requires included use case
- **Extend** `<<extend>>` (dashed arrow toward base) -- Extended use case **optionally** adds behavior based on conditions
- **Generalization** (solid line with hollow arrow) -- Parent-child use case inheritance

---

# 13. Data Structures & Algorithms

---

### Q: What is an Array? What are its time complexities?

**A:** An **Array** is a contiguous block of memory that stores elements of the same type, accessible by index.

| Operation | Time Complexity |
|-----------|----------------|
| Access by index | O(1) |
| Search (unsorted) | O(n) |
| Search (sorted, binary) | O(log n) |
| Insert at end | O(1) amortized |
| Insert at position | O(n) |
| Delete at position | O(n) |

**Key algorithms:** Kadane's Algorithm (max subarray sum), Two Pointers, Prefix Sum, Dutch National Flag.

---

### Q: What is a Linked List? What are the types?

**A:** A **Linked List** is a linear data structure where elements (nodes) are stored in **non-contiguous memory**, connected via pointers.

**Types:**
- **Singly Linked List** -- Each node has data + pointer to next node
- **Doubly Linked List** -- Each node has data + pointers to next AND previous
- **Circular Linked List** -- Last node points back to the first

| Operation | Time Complexity |
|-----------|----------------|
| Access by index | O(n) |
| Insert at head | O(1) |
| Insert at tail | O(n) or O(1) with tail pointer |
| Delete head | O(1) |
| Search | O(n) |

**Common problems:** Reverse, detect cycle (Floyd's), find middle (fast-slow pointers), merge sorted lists.

---

### Q: What is a Stack? What are its operations and time complexity?

**A:** A **Stack** is a linear data structure that follows **LIFO (Last In, First Out)** -- the last element added is the first to be removed.

**Operations:**
| Operation | Time |
|-----------|------|
| `push(element)` | O(1) |
| `pop()` | O(1) |
| `peek()/top()` | O(1) |
| `isEmpty()` | O(1) |

**Use cases:** Function call stack, undo operations, expression evaluation, balanced parentheses check, DFS.

---

### Q: What is a Queue? What are the types?

**A:** A **Queue** is a linear data structure that follows **FIFO (First In, First Out)** -- the first element added is the first removed.

**Types:**
- **Simple Queue** -- Insert at rear, remove from front
- **Circular Queue** -- Last position connects back to first (efficient array usage)
- **Priority Queue** -- Elements dequeued by priority, not insertion order
- **Deque (Double-ended Queue)** -- Insert/remove from both ends

| Operation | Time |
|-----------|------|
| `enqueue` | O(1) |
| `dequeue` | O(1) |
| `peek` | O(1) |

**Use cases:** BFS, scheduling, buffering.

---

### Q: What is a HashMap? How does it work internally?

**A:** A **HashMap** stores **key-value pairs** with O(1) average time for get/put operations.

**Internal working:**
1. **Hashing** -- Key's `hashCode()` is computed and mapped to a bucket index
2. **Buckets** -- Array of buckets (linked lists or trees at each index)
3. **Collision handling** -- When two keys hash to the same index:
   - **Chaining** -- Store in linked list (Java/Kotlin default). Converts to balanced tree when chain length > 8.
   - **Open Addressing** -- Find next empty slot

| Operation | Average | Worst |
|-----------|---------|-------|
| `get/put` | O(1) | O(n) |
| `containsKey` | O(1) | O(n) |

**Load factor** (default 0.75) triggers **rehashing** (doubling capacity) when exceeded.

---

### Q: What is a Binary Tree? What is a Binary Search Tree (BST)?

**A:**
- **Binary Tree** -- A tree where each node has **at most 2 children** (left and right).
- **Binary Search Tree (BST)** -- A binary tree with the property: **left child < parent < right child** for all nodes.

**BST Time Complexities:**
| Operation | Average | Worst (skewed) |
|-----------|---------|----------------|
| Search | O(log n) | O(n) |
| Insert | O(log n) | O(n) |
| Delete | O(log n) | O(n) |

**Traversals:**
- **Inorder** (LNR) -- Gives sorted order for BST
- **Preorder** (NLR) -- Used for serialization/copying
- **Postorder** (LRN) -- Used for deletion/evaluation
- **Level Order** (BFS) -- Level by level using queue

---

### Q: What is an AVL Tree?

**A:** An **AVL Tree** is a **self-balancing Binary Search Tree** where the height difference (balance factor) between left and right subtrees of any node is **at most 1**.

**Balance Factor** = height(left subtree) - height(right subtree). Must be -1, 0, or 1.

**Rotations to rebalance:**
- **Left Rotation** -- When right-heavy (balance factor < -1)
- **Right Rotation** -- When left-heavy (balance factor > 1)
- **Left-Right Rotation** -- Left child is right-heavy
- **Right-Left Rotation** -- Right child is left-heavy

All operations (search, insert, delete) are guaranteed **O(log n)**.

---

### Q: What is a Graph? What are the types and representations?

**A:** A **Graph** is a non-linear data structure consisting of **vertices (nodes)** connected by **edges**.

**Types:**
- **Directed** vs **Undirected** -- edges have direction or not
- **Weighted** vs **Unweighted** -- edges have weights or not
- **Cyclic** vs **Acyclic** -- contains cycles or not (DAG = Directed Acyclic Graph)

**Representations:**
- **Adjacency Matrix** -- 2D array, O(1) edge lookup, O(V^2) space
- **Adjacency List** -- Array of lists, O(V+E) space (preferred for sparse graphs)

**Traversals:**
- **BFS** -- Uses queue, explores level by level. O(V+E).
- **DFS** -- Uses stack/recursion, explores depth first. O(V+E).

---

### Q: What is a Heap? What is a Priority Queue?

**A:** A **Heap** is a complete binary tree that satisfies the **heap property**:
- **Max-Heap** -- Parent >= children (root is maximum)
- **Min-Heap** -- Parent <= children (root is minimum)

A **Priority Queue** is an ADT where elements are dequeued by priority. It is typically implemented using a heap.

| Operation | Time |
|-----------|------|
| Insert (add) | O(log n) |
| Extract min/max | O(log n) |
| Peek min/max | O(1) |
| Build heap | O(n) |

**Use cases:** Dijkstra's algorithm, median finding (two heaps), task scheduling, top-K elements.

---

### Q: What is Recursion? What is Backtracking?

**A:** **Recursion** is when a function **calls itself** to solve a smaller instance of the same problem. Every recursive function needs:
1. **Base case** -- condition to stop recursion
2. **Recursive case** -- function calls itself with a smaller input

**Backtracking** is a technique built on recursion where you **explore all possibilities** and **undo (backtrack)** when a path doesn't lead to a solution. Used for constraint satisfaction problems.

**Examples:** N-Queens, Sudoku solver, Permutations, Combination Sum, Subset generation.

---

### Q: What is Dynamic Programming (DP)?

**A:** **Dynamic Programming** is an optimization technique for problems with:
1. **Overlapping subproblems** -- Same subproblems are solved multiple times
2. **Optimal substructure** -- Optimal solution can be built from optimal sub-solutions

**Approaches:**
- **Top-down (Memoization)** -- Recursion + caching results in a map/array
- **Bottom-up (Tabulation)** -- Iteratively fill a table from smallest subproblem to largest

**Classic problems:** Fibonacci, Climbing Stairs, Frog Jump, Knapsack, Longest Common Subsequence, Maximum Sum Non-Adjacent.

**Time improvement:** Typically reduces exponential O(2^n) to polynomial O(n) or O(n*m).

---

### Q: What is Binary Search? When can it be applied?

**A:** **Binary Search** is a search algorithm that finds the target in a **sorted array** by repeatedly dividing the search interval in half.

**Prerequisite:** Array must be **sorted** (or have a monotonic property).

**Algorithm:**
1. Compare target with middle element
2. If equal, found
3. If target < middle, search left half
4. If target > middle, search right half

**Time:** O(log n) | **Space:** O(1) iterative, O(log n) recursive

**Variations:** First/last occurrence, floor/ceiling, search in rotated array, peak element in bitonic array.

---

### Q: What is the Sliding Window technique?

**A:** **Sliding Window** is a technique for problems involving **contiguous subarrays/substrings**. Instead of recalculating from scratch, you maintain a "window" and **slide** it across the array, adding/removing one element at a time.

**Types:**
- **Fixed window** -- Window size is fixed (e.g., max sum of subarray of size k)
- **Variable window** -- Window size expands/shrinks based on a condition (e.g., smallest subarray with sum >= target)

**Pattern:**
1. Expand window (move right pointer)
2. Shrink window when condition is violated (move left pointer)
3. Track the answer

**Time:** O(n) -- each element is processed at most twice.

---

### Q: What are the basic sorting algorithms and their complexities?

**A:**

| Algorithm | Best | Average | Worst | Space | Stable |
|-----------|------|---------|-------|-------|--------|
| **Bubble Sort** | O(n) | O(n^2) | O(n^2) | O(1) | Yes |
| **Selection Sort** | O(n^2) | O(n^2) | O(n^2) | O(1) | No |
| **Insertion Sort** | O(n) | O(n^2) | O(n^2) | O(1) | Yes |
| **Merge Sort** | O(n log n) | O(n log n) | O(n log n) | O(n) | Yes |
| **Quick Sort** | O(n log n) | O(n log n) | O(n^2) | O(log n) | No |
| **Heap Sort** | O(n log n) | O(n log n) | O(n log n) | O(1) | No |

**Bubble Sort** -- Repeatedly swap adjacent elements if in wrong order.
**Selection Sort** -- Find minimum in unsorted part, swap with first unsorted position.
**Insertion Sort** -- Insert each element into its correct position in the sorted part.

---

### Q: What are Bitwise Operators? List the common ones.

**A:** Bitwise operators work on individual **bits** of integers.

| Operator | Symbol | Description |
|----------|--------|-------------|
| AND | `&` | 1 only if both bits are 1 |
| OR | `\|` | 1 if at least one bit is 1 |
| XOR | `^` | 1 if bits are different |
| NOT | `~` | Flips all bits |
| Left Shift | `<<` | Shifts bits left (multiply by 2) |
| Right Shift | `>>` | Shifts bits right (divide by 2) |

**Useful tricks:**
- `n & 1` -- Check if odd
- `n & (n-1)` -- Turn off rightmost set bit
- `n ^ n = 0` -- XOR with itself is 0
- `n ^ 0 = n` -- XOR with 0 is itself
- `1 << k` -- Get 2^k

---

# 14. System Design (LLD)

---

### Q: How would you design a Parking Lot System? (Key entities and relationships)

**A:** **Key entities:**
- `ParkingLot` -- has multiple floors/levels
- `ParkingFloor` -- has multiple spots
- `ParkingSpot` -- has type (Compact, Large, Handicapped, Motorcycle), status (Free/Occupied)
- `Vehicle` -- base class with subtypes (Car, Truck, Motorcycle)
- `Ticket` -- issued on entry (vehicle, spot, entry time)
- `Payment` -- calculated on exit (based on duration)

**Key design decisions:**
- Strategy pattern for parking spot assignment
- Observer pattern for spot availability updates
- Factory pattern for vehicle/ticket creation
- Enum for spot types and vehicle types

---

### Q: How would you design a Library Management System? (Key entities)

**A:** **Key entities:**
- `Library` -- has books, members, librarians
- `Book` -- title, author, ISBN, copies
- `BookItem` -- physical copy of a book (barcode, status)
- `Member` -- can search, borrow, return books (limit on borrowing)
- `Librarian` -- can add/remove books, manage members
- `BookReservation` -- reserve a book that's currently borrowed
- `Fine` -- calculated for late returns

**Key operations:** Search (by title, author, ISBN), Borrow, Return, Reserve, Renew.

---

### Q: How would you design a Car Rental System? (Key entities)

**A:** **Key entities:**
- `CarRentalSystem` -- manages vehicles, locations, reservations
- `Vehicle` -- base class with types (Car, SUV, Van, Truck)
- `VehicleInventory` -- tracks available vehicles per location
- `RentalLocation` -- pickup/drop-off locations
- `Reservation` -- customer, vehicle, dates, pickup/drop-off location
- `Bill` -- calculated based on duration, vehicle type, insurance, extras
- `Customer` -- account, driving license, payment methods

---

# 15. Git

---

### Q: What is the difference between `git merge` and `git rebase`?

**A:**
- **`git merge`** -- Creates a **merge commit** that combines two branches. Preserves complete history. Non-destructive.
- **`git rebase`** -- **Replays** your commits on top of the target branch, creating a linear history. Rewrites commit history.

**When to use:**
- `merge` -- For shared/public branches (preserves history, safe)
- `rebase` -- For local/feature branches before merging (clean linear history)

**Golden rule:** Never rebase commits that have been pushed to a shared/public branch.

---

### Q: What is `git stash`?

**A:** `git stash` **temporarily saves uncommitted changes** (both staged and unstaged) so you can switch branches or pull changes without committing unfinished work.

```bash
git stash           # save changes
git stash pop       # restore and remove from stash
git stash apply     # restore but keep in stash
git stash list      # list all stashes
git stash drop      # delete a stash
```

---

### Q: What is the difference between `git reset`, `git revert`, and `git checkout`?

**A:**
- **`git reset`** -- Moves the branch pointer backward. Can unstage changes (`--mixed`), discard changes (`--hard`), or keep changes staged (`--soft`). **Rewrites history** -- dangerous for shared branches.
- **`git revert`** -- Creates a **new commit** that undoes a previous commit. **Safe for shared branches** -- doesn't rewrite history.
- **`git checkout`** -- Switches branches or restores files. Doesn't modify history.

---

# 16. Behavioral Interview

---

### Q: What is the STAR method for behavioral interviews?

**A:** **STAR** is a structured framework for answering behavioral questions:

- **S - Situation**: Set the context. Where were you? What was the project?
- **T - Task**: What was your responsibility or the challenge you faced?
- **A - Action**: What specific steps did YOU take? (Focus on your individual contribution)
- **R - Result**: What was the outcome? Quantify if possible (reduced bugs by 30%, shipped on time).

**Example prompt:** "Tell me about a time you handled a conflict."

**Tips:**
- Keep answers 1-2 minutes
- Focus on YOUR actions, not the team's
- Always end with a positive result or learning
- Prepare 5-6 stories that cover: conflict, failure, leadership, tight deadline, disagreement, going above and beyond

---

### Q: How do you handle disagreements with a team member about a technical approach?

**A:** Use the STAR framework:
- **Listen first** without judgment to understand their perspective fully
- **Evaluate objectively** -- compare approaches on metrics (performance, maintainability, timeline)
- **Propose a compromise** or suggest prototyping both approaches if feasible
- **Escalate constructively** to a tech lead if you can't reach consensus
- **Accept the decision** once made, even if it's not your preferred approach
- Focus on **what's best for the project**, not on being right

---

> This document was auto-generated from the android-knowledge-mesh repository for interview preparation and RAG training.
