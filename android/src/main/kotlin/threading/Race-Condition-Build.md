# Race Condition — Build Exercise

This build exercise walks you through constructing a race condition, reproducing it, diagnosing it, and fixing it. Each step is a runnable Kotlin program.

## Step 1: Write the racing code

The bug is a **read-modify-write** on a shared mutable field. `count += 1` is not atomic — it reads the current value, adds one, and writes back. Two threads can interleave between read and write.

```kotlin
class Counter {
    private var count = 0  // ❌ Mutable, shared, non-atomic

    fun increment(): Int {
        count += 1
        return count
    }

    fun get(): Int = count
}

fun main() {
    val counter = Counter()

    val threads = (1..8).map { t ->
        Thread {
            repeat(1000) {
                counter.increment()
            }
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }

    val expected = 8 * 1000
    println("Expected: $expected, Got: ${counter.get()}")
}
```

## Step 2: Reproduce

Run the program. Run it multiple times. You will likely see the count is less than 8000 because increments were **lost** — one thread's write overwrote another's.

Typical output (varies by machine):

```
Expected: 8000, Got: 7984
Expected: 8000, Got: 7991
Expected: 8000, Got: 7977
```

The exact number is non-deterministic. That is the defining characteristic of a race condition.

## Step 3: Diagnose

The failure is a **lost update**. The sequence is:

1. Thread A reads `count = 42`
2. Thread B reads `count = 42`
3. Thread A writes `count = 43`
4. Thread B writes `count = 43`

Result: `count` is 43, not 44. One increment was silently dropped.

## Step 4: Fix it with atomics

```kotlin
import java.util.concurrent.atomic.AtomicInteger

class Counter {
    private val count = AtomicInteger(0) // ✅ Atomic

    fun increment(): Int = count.getAndIncrement()

    fun get(): Int = count.get()
}
```

`AtomicInteger.getAndIncrement()` is a single atomic instruction. No lost updates.

## Step 5: Fix it with a lock

```kotlin
import java.util.concurrent.locks.ReentrantLock

class CounterWithLock {
    private var count = 0
    private val lock = ReentrantLock()

    fun increment(): Int = lock.withLock {
        count += 1
        count
    }

    fun get(): Int = lock.withLock { count }
}
```

## Step 6: Verify

With the fix, run the program:

```
Expected: 8000, Got: 8000
```

Every time.

## Trade-offs

| Approach     | Pros                                  | Cons                                  |
|--------------|---------------------------------------|---------------------------------------|
| Atomic      | Simple, lock-free                     | Limited to simple operations         |
| Lock         | Flexible, general                     | More overhead, can deadlock          |
| Mutex        | Coroutine-friendly                    | Same properties as lock              |

## Key takeaway

Race conditions arise from non-atomic read-modify-write sequences. Fix with atomics or locks, and remember atomicity alone isn't enough — you also need visibility. Use `volatile` or `Atomic*` for cross-thread visibility.

## Verification

Run the fixed program 10 times. If any run shows `Got: 8000`, the fix is correct.