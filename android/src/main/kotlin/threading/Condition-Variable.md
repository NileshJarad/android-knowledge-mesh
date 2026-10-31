# Condition Variable

## What is a condition variable?

A condition variable allows a waiting thread to block until some predicate becomes true, instead of polling in a tight loop. A thread "signals" that a condition changed and wakes at most one waiter.

### The condition variable pattern

```
1. Thread A acquires the monitor (Mutex)
2. Thread A checks the predicate → false
3. Thread A calls await() → releases monitor, blocks
4. Thread B acquires the monitor
5. Thread B sets the predicate → true
6. Thread B calls signal() → wakes Thread A
7. Thread B releases the monitor
8. Thread A re-acquires the monitor, checks predicate → true, proceeds
```

## Why it matters

Polling is CPU-wasteful; busy-wait loses energy. Condition variables convert *"I don't know when the other side is ready"* into *"block until someone tells me when,"* which is the right contract for coordination. In system design, this pattern often underpins producer→consumer handshake queues, retry triggers, and lifecycle coordination.

## The Kotlin condition variable

```kotlin
import kotlinx.coroutines.sync.*

class EventBus {
    private val lock = Mutex()
    private val ready = Condition(lock) // condition variable bound to the mutex

    suspend fun publish(payload: String): Boolean {
        lock.withLock {
            ready.awaitAll { true }   // wait until someone publishes
            // ... receive event ...
            return true
        }
    }

    suspend fun notify(message: String) {
        lock.withLock {
            ready.signalAll()          // wake waiters
        }
    }
}
```

## When you'll see it

- Producer-consumer handshakes inside a bounded queue (`ArrayBlockingQueue.take()`).
- Onboarding events where a subscriber registers only after a readiness signal propagates.
- Cancellation flow where a coroutine suspends until another coroutine releases a resource.

## Common gotchas

- **Always hold the underlying mutex** when calling `await` / `signal`; otherwise the monitor is abandoned and a live waiter starves.
- **Always pair a waiter with a predicate**: `while (!predicate) wait()` — the classic "spurious wakeup" invariant.
- **There is a fairness-optional mode** (`fair = true`); default unfairness is usually OK because fairness costs throughput.

## Producer-consumer example

```kotlin
class MessageQueue {
    private val lock = Mutex()
    private val notEmpty = Condition(lock)
    private val notFull = Condition(lock)
    private val items = ArrayBlockingQueue<String>(100)

    suspend fun put(item: String) {
        lock.withLock {
            while (items.size >= 100) notFull.await()  // wait until space is available
            items.put(item)
            notEmpty.signal()  // wake a consumer
        }
    }

    suspend fun take(): String {
        lock.withLock {
            while (items.isEmpty()) notEmpty.await()  // wait until an item is available
            val item = items.take()
            notFull.signal()  // wake a producer
            return item
        }
    }
}
```

## Summary

Condition variables trade directness for efficiency: you keep your API expressive (yes/no gate) instead of polling, at the cost of carefully holding the monitor and checking the predicate. They deserve attention once you master Mutex; beyond that they belong to mature concurrency designs only.

## Key takeaway

A condition variable is a signaling mechanism that lets threads wait for a predicate to become true. It's the foundation of producer-consumer coordination.