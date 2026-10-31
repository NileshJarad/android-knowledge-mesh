# Atomic Operations

## What is an atomic operation?

An **atomic operation** completes entirely or not at all — there is no intermediate state observable by other threads. Atomic operations are indivisible.

## Why atomicity matters

Without atomicity, reads and writes can interleave. Consider:

```kotlin
var counter = 0

fun increment() {
    val current = counter   // read
    counter = current + 1   // write
}
```

Two threads calling `increment()` concurrently:
1. Thread A reads `counter = 0`
2. Thread B reads `counter = 0`
3. Thread A writes `counter = 1`
4. Thread B writes `counter = 1`

Result: `counter` is `1`, not `2`. One increment was **lost**.

## Kotlin atomic primitives

```kotlin
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

class AtomicCounter {
    private val count = AtomicInteger(0)

    fun increment(): Int = count.getAndIncrement()   // ✅ Atomic increment
    fun get(): Int = count.get()                      // ✅ Atomic read
}
```

### Common atomic classes

| Class              | Typical use                                      |
|--------------------|--------------------------------------------------|
| `AtomicInteger`    | Integer counters, sequence numbers               |
| `AtomicLong`       | Long counters (timestamps, IDs)                  |
| `AtomicReference`  | Object references (config snapshots, caches)     |
| `AtomicBoolean`    | Flags (stop signals, toggle states)              |

## Compare-and-swap (CAS)

Atomic variables support **compare-and-swap**: "change value only if it equals expected."

```kotlin
val counter = AtomicInteger(0)

// Atomically increment: retry until CAS succeeds
fun incrementWithRetry() {
    while (true) {
        val current = counter.get()
        if (counter.compareAndSet(current, current + 1)) {
            break  // ✅ Updated successfully
        }
        // else: another thread changed it; retry
    }
}
```

## When atomicity is NOT enough

Atomic operations guarantee indivisibility, **not** visibility across threads unless combined with proper synchronization. A plain `var` write from one thread may never be seen by another thread on a different CPU core due to CPU caching.

```kotlin
// ❌ NOT safe for multi-threaded visibility
var flag = false

// ✅ Use volatile or AtomicBoolean for visibility
@Volatile
var flag = false

// or
val flag = AtomicBoolean(false)
```

## Key takeaway

Atomic operations are indivisible — they complete fully or not at all. Use `AtomicInteger`, `AtomicLong`, `AtomicReference`, and `AtomicBoolean` for simple counter-like state without locks. For visibility across threads, pair atomics with `volatile` or proper synchronization.