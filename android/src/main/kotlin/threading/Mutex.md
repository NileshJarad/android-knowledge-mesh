# Mutex

## What is a mutex?

A **mutex** is a mutual exclusion lock. At most **one** thread may hold it at a time; every other thread must wait. The guarantee is: while any thread holds this lock, no other thread touches the critical section.

### The mutex invariant

```
1. Lock is FREE
2. Thread A calls lock() → acquires, lock is HELD
3. Thread B calls lock() → blocks until Thread A releases
4. Thread A calls unlock() → lock is FREE
5. Thread B (or C, or D) acquires
```

## Why it matters

A mutex is the first and cheapest primitive taught to all engineers. Knowing exactly when and why to apply it is the single biggest predictor of clean code review. But mutex misuse — overuse, deadlock-prone ordering, blocking the wrong thread — is the most common review finding in Android code.

## The Kotlin mutex

```kotlin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedCounter {
    private val lock = Mutex()
    private var value = AtomicInteger()

    suspend fun increment(): Int {
        lock.withLock {
            // Critical section: exactly one coroutine at a time
            return value.getAndAdd(1).also { return it }
        }
    }
}
```

`Mutex.withLock` is a suspending function — the coroutine that acquires the lock suspends, but the thread is released so other coroutines can run. This is the key difference from `synchronized` (which blocks the thread).

## When you'll see it

- Protecting a shared mutable cache before producing new values.
- Coordinating a producer/consumer handshake where the queue must hold a guaranteed-free slot.
- Sliding-window aggregation where dozens of parallel workers join batches.

## Comparison with alternatives

| Primitive             | Reentrant | Blocking | Fairness      | Typical use                          |
|-----------------------|-----------|----------|----------------|--------------------------------------|
| `synchronized`       | No        | Yes      | Unfair by default | Kotlin stdlib inner locks          |
| `ReentrantLock`      | Yes       | Yes      | Fair option     | Reentrant recursive sections         |
| `Mutex` (kotlinx)    | Yes*      | suspend  | Fair           | Coroutine sections                   |
| `Semaphore(permits=1)`| No        | Yes      | Fair            | Permit signalling, queue sizing     |
| `volatile` / `@Volatile` | N/A   | Reads are visible, writes not atomic | N/A | Visibility only — never enough alone |

*`kotlinx.coroutines.sync.Mutex` is reentrant in scope because the owning coroutine already holds its Job; the *library* `java.util.concurrent.locks.ReentrantLock` has the same semantic.

## Common mistakes

- **Using `Mutex` from another thread** — the coroutine that owns the lock must be the one that releases it. If the owner coroutine is cancelled, the lock is automatically released.
- **Holding the lock for too long** — `withLock` is for short critical sections. For long operations, prefer a semaphore or a different design.
- **Forgetting the `withLock` pattern** — always use `withLock { ... }` rather than manual `lock()` / `unlock()`. The `withLock` pattern guarantees the lock is released on cancellation.

## System Design guidance

Prefer a lock only when the critical section is tiny, fast, and the waiting cost is predictable. For long sections prefer *lock striping* or split at multiple points; for sections shared across many independent consumers prefer `Semaphore`. If the owner can hold the lock for long, the waits become fairness-policy problems worth measuring before defaulting to a `Mutex`.

## Key takeaway

A mutex is the simplest, most fundamental synchronization primitive. It guarantees exclusive access to a critical section. Use it when you need to protect shared mutable state, but always pair it with `withLock` and keep the critical section short.