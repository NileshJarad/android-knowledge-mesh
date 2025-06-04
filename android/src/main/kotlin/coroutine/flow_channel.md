# Flow and Channel 

# Flow
In coroutines, a flow is a type that can emit multiple values sequentially, as opposed to suspend functions that return
only a single value. For example, you can use a flow to receive live updates from a database

Flows are built on top of coroutines and can provide multiple values. A flow is conceptually a stream of data that can
be computed asynchronously. The emitted values must be of the same type. For example, a Flow<Int> is a flow that emits
integer values.

## What are Terminal Operators in Kotlin Flow

* Terminal operators start the flow collection.
* Without a terminal operator, the flow will not emit anything.
* Most use cases call collect as the primary terminal operator.

| Operator            | Description                                                                                                                                |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| **`collect`**       | Collects all emitted values and processes them                                                                                             |
| **`collectLatest`** | Collects only the latest emission, cancels previous collector if new value comes                                                           |
| **`toList`**        | Collects all emitted values into a `List`                                                                                                  |
| **`toSet`**         | Collects all emitted values into a `Set`                                                                                                   |
| **`single`**        | Expects exactly one value and returns it. If more than one value throws java.lang.IllegalArgumentException: Flow has more than one element |
| **`singleOrNull`**  | Returns the single value or null if none emitted or more than one value                                                                    |
| **`first`**         | Returns the first emitted value                                                                                                            |
| **`firstOrNull`**   | Returns the first emitted value or null if none emitted                                                                                    |
| **`last`**          | Returns the last emitted value                                                                                                             |
| **`lastOrNull`**    | Returns the last emitted value or null if none emitted                                                                                     |
| **`reduce`**        | Accumulates values into a single value (terminal)                                                                                          |
| **`fold`**          | Like reduce, but with an initial value                                                                                                     |

## Intermediate Operators (or Transforming Operators)

Operators that are not terminal are called Intermediate Operators (or Transforming Operators).

* They transform, filter, or modify the emitted values from a flow.
* They return a new flow and do not trigger execution on their own.
* You can chain multiple intermediate operators before collecting.

| Operator                   | Description                                                                                                                               |
|----------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| **`map`**                  | Transforms each emitted value                                                                                                             |
| **`filter`**               | Emits only values that match a condition                                                                                                  |
| **`take`**                 | Takes only the first N values                                                                                                             |
| **`drop`**                 | Skips the first N values                                                                                                                  |
| **`distinctUntilChanged`** | Emits only when value changes                                                                                                             |
| **`onEach`**               | Performs an action on each emitted value (side effect)                                                                                    |
| **`buffer`**               | Buffers emissions to allow concurrency                                                                                                    |
| **`flatMapConcat`**        | Maps values to flows and concatenates emissions. It waits for the previous inner flow to complete before starting the next one.           |
| **`flatMapMerge`**         | Maps values to flows and merges emissions concurrently. It starts all inner flows immediately and emits values as they arrive, unordered. |
| **`flatMapLatest`**        | Maps values to flows and switches to latest only.This is great for UI or search — only the latest result matters                          |
| **`debounce`**             | Emits value only if a specified time passed without new emission                                                                          |
| **`sample`**               | Emits the latest value periodically                                                                                                       |
| **`retry`**                | Retries flow collection on error                                                                                                          |
| **`flowOn`**               | Changes the coroutine dispatcher the flow runs on                                                                                         |
| **`zip`**                  | Combines emissions of two flows pairwise if extra value in one of flow it will be ignored                                                 |
| **`combine`**              | Combines latest emissions of multiple flows if extra value in one of flow always matched with last value of other flow                    |

### Creating  and collecting Flow

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flowOf(1, 2, 3, 3, 6, 7, 8)
        .flowOn(Dispatchers.IO) // // Change the upstream context to IO
        .distinctUntilChanged() // takes only distinct value (1, 2, 3, 6, 7, 8)
        .drop(2)// drops first two element (3, 6, 7, 8)
        .take(10) // takes only 10 element (3, 6, 7, 8) as size if less than 10
        .filter { it % 2 == 0 }  // Only even numbers (6,8)
        .map { it * 2 }  // Multiply each number by 2 (12, 16)
        .onEach { it / 2 } // nothing happens on actual value (12,16)
        .collect { println(it) }  // 12 16
}
```

### Reduce or fold

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    val result = flowOf(1, 2, 3, 3, 6, 7, 8)
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged() // 1, 2, 3, 6, 7, 8
        .drop(2)                // 3, 6, 7, 8
        .take(10)               // 3, 6, 7, 8
        .filter { it % 2 == 0 } // 6, 8
        .map { it * 2 }         // 12, 16
        .onEach { it / 2 }      // has no effect, consider logging here
        //.reduce { acc, value -> acc + value } // 12 + 16 = 28
        .fold(30) { acc, value -> acc + value } // 30 + 12 + 16 = 58

    println(result) // Output: 28
}

```

### Combine, Zip Flow

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    val flow1 = flowOf(1, 2, 3, 4)
    val flow2 = flowOf("A", "B")


    /***
     * 1 -> A
     * 2 -> B
     * 3 -> B
     * 4 -> B
     ***/
    flow1
        .combine(flow2) { num, letter ->
            "$num -> $letter"
        }
        .collect { println(it) }


    /***
     * 1 -> A
     * 2 -> B
     ***/
    flow1
        .zip(flow2) { number, letter ->
            "$number -> $letter"
        }
        .collect { println(it) }
}

```

### flatMapConcat, flatMapMerge and flatMapLatest

**flatMapConcat**

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flowOf(1, 2)
        .flatMapConcat { value ->
            flow {
                emit("Start $value")
                delay(100)
                emit("End $value")
            }
        }
        .collect { println(it) }
}
/***
 * Start 1
 * End 1
 * Start 2
 * End 2
 */
```

**flatMapMerge**

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flowOf(1, 2)
        .flatMapMerge { value ->
            flow {
                emit("Start $value")
                delay(100)
                emit("End $value")
            }
        }
        .collect { println(it) }
}

/***
 * Start 1
 * Start 2
 * End 1
 * End 2
 */
```

**flatMapLatest**

```kotlin
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

fun main() = runBlocking {
    flow {
        emit(1)
        delay(50)
        emit(2)
        delay(50)
        emit(3)
    }
        .flatMapLatest { value ->
            flow {
                emit("Start $value")
                delay(100)
                emit("End $value")
            }
        }
        .collect { println(it) }
}

/****
 * Start 1
 * Start 2
 * Start 3
 * End 3
 */
```

---

## Difference ways to create flow in Kotlin

### Using flowOf
 - Creates a flow that emits given values sequentially.

```kotlin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    flowOf(1, 2, 3).collect { println(it) }
}

```

### Using flow {} builder
- Custom flow that can emit values manually and support suspend functions.
```kotlin
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    flow {
        emit(1)
        delay(100)
        emit(2)
    }.collect { println(it) }
}

```

### Using asFlow()
 - Convert collections or sequences into a flow.
```kotlin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    listOf(10, 20, 30).asFlow().collect { println(it) }
}

```

### Using channelFlow
 - For flows where you want to emit values from multiple coroutines or channels.

```kotlin
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    channelFlow {
        launch { send(1) }
        launch { send(2) }
    }.collect { println(it) }
}

```

### Using callbackFlow
- To convert callback-based APIs into flows.

```kotlin
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect

fun main() = runBlocking {
    callbackFlow<Int> {
        val listener = object {
            fun onEvent(value: Int) {
                trySend(value)
            }
        }
        // Simulate event emission
        listener.onEvent(100)
        awaitClose { /* cleanup listener here */ }
    }.collect { println(it) }
}

```


| Method         | Use Case                                  |
|----------------|-------------------------------------------|
| `flowOf`       | Simple fixed set of values                |
| `flow {}`      | Manual emission, suspendable code         |
| `asFlow()`     | Convert collections/sequences             |
| `channelFlow`  | Multi-coroutine or channel-based emission |
| `callbackFlow` | Convert callback APIs to Flow             |


----


# Channel

| Aspect             | Channels                                                                            | Flows                                                                                              |
|--------------------|-------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| **Concept**        | A **hot** stream, like a queue for sending and receiving values between coroutines. | A **cold** asynchronous stream of data that emits values when collected.                           |
| **Hot vs Cold**    | **Hot:** Active even if no one is receiving. Sends values to whoever is listening.  | **Cold:** Starts producing values only when collected (like a sequence).                           |
| **Backpressure**   | Supports backpressure explicitly via suspending send/receive.                       | Handles backpressure implicitly by suspending upstream emissions until downstream is ready.        |
| **Use case**       | Useful for communication between coroutines, event bus, or pipelines.               | Used for representing asynchronous streams of data (e.g., UI updates, network responses).          |
| **API Style**      | Uses **send** and **receive** or **offer** and **poll**.                            | Uses declarative operators like `map`, `filter`, `combine`, and terminal operators like `collect`. |
| **Lifecycle**      | Has to be explicitly closed to avoid leaks.                                         | Lifecycle managed by collection; no explicit closing needed.                                       |
| **Error Handling** | Requires manual closing and exception handling on both ends.                        | Flow operators provide structured error handling with `catch`.                                     |
