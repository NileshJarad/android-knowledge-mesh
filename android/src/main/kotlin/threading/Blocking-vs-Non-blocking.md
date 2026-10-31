# Blocking vs Non-blocking

Blocking and non-blocking are two approaches to concurrency in programming.

## Blocking concurrency

Blocking means the current thread pauses execution until the operation completes. The thread remains blocked, another thread can run, but the blocked thread cannot proceed until the I/O or lock is released.

### Blocking example (Thread.sleep)

```kotlin
fun fetchDataBlocking(): String {
    Thread.sleep(1000) // Thread blocked here for up to 1 second
    return "data"
}
```

### Blocking with locks

```kotlin
val lock = Mutex()

suspend fun criticalOperation() {
    lock.withLock {
        // This thread is now blocked for others
        Thread.sleep(100)  // work under lock
    }
}
```

## Non-blocking concurrency

Non-blocking means the operation never stops the thread. Instead, it either returns immediately with a result, or the completion is indicated by a callback / future / async continuation.

### Non-blocking example (Future)

```kotlin
import java.util.concurrent.CompletableFuture

fun fetchDataNonBlocking(): CompletableFuture<String> {
    return CompletableFuture.supplyAsync {
        // Simulate network call without blocking
        Thread.sleep(1000)
        "data"
    }
}
```

### Non-blocking with coroutines

```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun fetchDataAsync(): String = withContext(Dispatchers.IO) {
    // Coroutine suspends here, but other coroutines run in this thread
    delay(1000)  // yields to other coroutines
    "data"
}
```

## Trade-offs

| Aspect                | Blocking                        | Non-blocking                      |
|-----------------------|---------------------------------|------------------------------------|
| Simplicity            | ✅ Easier to reason             | ✅ Requires callbacks/futures      |
| Resource usage        | ❌ Thread idle, wasted          | ✅ Thread stays usable              |
| Debugging             | ✅ Predictable flow             | ❌ Flow splits across threads       |
| Error handling        | ✅ Straightforward              | ❌ Multiple callbacks               |

## When to use each

### Choose blocking when
- Working with legacy APIs that expect threads
- Small number of threads (e.g., thread pool with CPU work)
- You control the async environment

### Choose non-blocking when
- High-concurrency (thousands of concurrent ops)
- Building a reactive API
- You need to maximize resource utilization

## Key takeaway

Blocking pauses the thread; non-blocking keeps the thread busy with callbacks. The choice depends on your workload and the APIs you are using.