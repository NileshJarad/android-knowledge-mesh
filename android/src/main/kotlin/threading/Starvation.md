# Starvation

## Definition

Starvation occurs when a thread is perpetually denied access to resources it needs because other threads monopolize them. Unlike deadlock or livelock, the starving thread *is scheduled*, but it never makes progress because it can never acquire the lock/resource it needs.

## Why it matters

Starvation is insidious: the system looks alive (threads are running, CPU is busy) but one or more threads make zero progress. It often surfaces as "this background job never completes" or "this request times out intermittently."

## Code example: starvation with an unfair lock

```kotlin
class UnfairResource {
    private val lock = java.util.concurrent.locks.ReentrantLock(false) // unfair
    private var count = 0

    fun useResource(name: String) {
        lock.lock()
        try {
            count++
            println("$name used resource ($count)")
            Thread.sleep(100)
        } finally {
            lock.unlock()
        }
    }
}

val resource = UnfairResource()

// Worker A hogs the resource constantly
val workerA = Thread {
    for (i in 1..1000) {
        resource.useResource("Worker A")
    }
}

// Worker B rarely gets a turn
val workerB = Thread {
    for (i in 1..1000) {
        resource.useResource("Worker B")
    }
}

workerA.start()
workerB.start()
workerA.join()
workerB.join()
```

With an unfair lock, Worker B may be starved because Worker A re-acquires the lock immediately after releasing it.

## Remedies

| Strategy                  | Description                                                        |
|---------------------------|--------------------------------------------------------------------|
| **Fair locks**            | `ReentrantLock(true)` grants locks in FIFO order                   |
| **Lock striping**         | Split hot locks so not all threads contend on one lock             |
| **Thread priority tuning**| Raise priority of starving threads (Android caution: don't starve UI thread) |
| **Semaphore-based limits**| Guarantee minimum permits per class of worker                      |

## Key takeaway

Starvation is a fairness problem. Use fair locks and lock striping to ensure every thread eventually makes progress.