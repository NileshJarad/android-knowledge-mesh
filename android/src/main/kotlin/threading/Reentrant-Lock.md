# Reentrant Lock

## What is a reentrant lock?

A reentrant lock can be acquired **multiple times by the same thread** without deadlocking. Each subsequent acquisition returns immediately, and a single `unlock()` removes exactly one acquired level. This mirrors Java's intrinsic behavior for `synchronized` on the JVM, but Kotlin's explicit `ReentrantLock` is opt-in and also gives you non-intrusive options such as try-lock + backoff strategies.

### The reentrant invariant

```
1. Thread A calls lock() → acquires (level 1)
2. Thread A calls lock() again → acquires (level 2)
3. Thread A calls unlock() → drops to level 1
4. Thread A calls unlock() → drops to level 0 → lock is FREE
5. Thread B can now acquire
```

## Why it matters

A reentrant lock lets a single routine (often inside a recursive call path or nested suspend block) safely re-enter the same critical section from within its own body. Without it you have to use different primitive lock types per nesting level — an anti-pattern flagged in code reviews because it complicates every lock ordering decision.

## Kotlin example

```kotlin
import java.util.concurrent.locks.ReentrantLock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ReentrantSection {
    private val lock = ReentrantLock()
    private var depth = 0

    fun enter(): Unit {
        lock.lock()
        depth++
    }

    fun exit(): Unit {
        depth--
        if (depth <= 0) lock.unlock()
    }
}
```

### Coroutine-based reentrant pattern

```kotlin
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecursiveCounter {
    private val lock = Mutex()
    private var count = 0

    // This method can be called recursively within the same coroutine
    suspend fun add(value: Int): Int = lock.withLock {
        count += value
        if (value > 0) {
            add(value - 1)  // ← same coroutine re-acquires the same lock
        } else count
    }
}
```

## When you'll see it

- A `mutex` that `withLock` guards a coroutine which itself does not re-lock the same mutex; instead choose reentrant to allow a helper routine nested deep inside the critical section to call back into the same lock semantics.
- Recursive tree traversals where one node inspects and recurses into its children, all while holding the same parent lock.

## Common misuses flagged by reviewers

| Misuse                        | Why it's wrong                                            |
|-------------------------------|-----------------------------------------------------------|
| Using `Mutex` from another thread that then calls `withLock` inside the critical section | No recursion benefit because the owning coroutine is suspended elsewhere |
| Calling a blocking operation from within the section while holding the lock | Long hold time defeats the cheap reentry promise |
| Forgetting to pair every `lock()` with an `unlock()` | Becomes a longer-lived reentrant hole, still racy on next external caller |

## Summary

Reentrant locks exist so the same logical nesting path can run fully under protection without fighting lock ordering. They should be chosen when your section naturally nests, never as a workaround for a genuine concurrency design gap.

## Key takeaway

A reentrant lock allows the same thread to acquire it multiple times safely. It's essential for recursive methods and nested critical sections.