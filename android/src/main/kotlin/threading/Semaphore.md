# Semaphore

## What is a semaphore?

A semaphore is a counting gate. It has `N` permits; each permit lets one thread enter the protected region. Unlike a plain mutex (`N=1`), a semaphore protects a *region* that admits many concurrent callers up to capacity.

### The semaphore invariant

```
1. Semaphore initialized with N permits
2. Thread A calls acquire() → decrements permit count to N-1
3. Thread B calls acquire() → decrements to N-2
4. Thread A calls release() → increments back to N-1
5. When permits reach 0, acquire() blocks until someone releases
```

## Why it matters

A semaphore is the correct abstraction when you want *capacity control* rather than *exclusive access*. It backs queue sizing in connection pools, pool eviction in thread pools, and bursty-load throttling behind a rate limiter.

## The Kotlin semaphore

```kotlin
import java.util.concurrent.Semaphore
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class RateLimitedPool(private val permits: Int = 4) {
    private val count = Semaphore(permits)

    suspend fun acquireSlot(): Unit = count.withPermit {}

    fun releaseSlot(): Unit = count.release()
}
```

`Semaphore.withPermit` is a suspending function — the coroutine that acquires a permit suspends, but the thread is released so other coroutines can run.

## When you'll see it

- A `ConnectionPool` borrowing from a DB connection pool with a fixed reserve of free connections.
- Deciding how many concurrent HTTP requests can be in flight before going back to the client.
- Limiting how many in-flight writes hit a single slow index shard.

## Classic use cases

| Concern                | Semaphore strategy                         | How many permits |
|------------------------|--------------------------------------------|------------------|
| Connection pool        | Reserve non-free connections               | `total - reserved` |
| Bursty I/O             | Limit outstanding operations to a window    | e.g. 100         |
| Cache admission        | Cap writes, allow reads                    | varies            |
| Throttle               | Starve excess producers                     | 1 or few         |

## Connection pool example

```kotlin
class ConnectionPool(private val maxConnections: Int) {
    private val permits = Semaphore(maxConnections)

    fun borrowConnection(): Connection {
        permits.acquire()  // blocks until a slot is free
        return Connection()
    }

    fun returnConnection(conn: Connection) {
        permits.release()  // frees the slot
    }
}
```

## System Design summary

Semaphores are used as backpressure signals in data pipelines: the producer commits work only when permits are available, so the pipeline self-throttles against slow downstream consumers. The choice of `permits` is itself a tuning decision — too small and you can't keep pipeline pressure; too large and customers stall.

## Key takeaway

A semaphore is a counting gate that limits the number of concurrent accesses to a resource. It's the right tool when you need capacity control, not exclusive access.