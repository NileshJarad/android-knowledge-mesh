# Thread Pool

A thread pool is a reusable collection of worker threads that process tasks from a queue. Instead of creating a new thread per task (expensive) or spawning unbounded threads, you pre-allocate a fixed pool.

## Why use a thread pool

- **Avoids thread creation overhead** — starting a thread is expensive (~100µs)
- **Bounds resource usage** — you know exactly how many threads exist
- **Improves latency** — no creation delay per task
- **Provides backpressure** — queue fills up, tasks queue (if bounded)

```kotlin
import java.util.concurrent.Executors

val threadPool = Executors.newFixedThreadPool(4) // ✅ Create pool of 4 threads

for (task in tasks) {
    threadPool.execute { task.run() }
}

threadPool.shutdown() // ✅ Stop accepting new tasks
threadPool.awaitTermination(5, TimeUnit.SECONDS) // ✅ Wait for completion
```

## Executor types

| Executor type                     | Behavior                                        | Use case                        |
|-----------------------------------|-------------------------------------------------|---------------------------------|
| `newSingleThreadExecutor()`       | 1 thread, executes tasks sequentially          | Simple sequential work         |
| `newFixedThreadPool(n)`           | N threads, tasks queue up                       | CPU-bound workloads             |
| `newCachedThreadPool()`          | Creates new threads per task if none idle      | Short-lived tasks              |
| `newScheduledThreadPool(n)`     | N threads, tasks scheduled on a timer          | Periodic tasks                 |

## When you'll see it

- Background task processing where you have many tasks to run.
- Parsing large documents offline.
- Image loading pipelines.

## Thread pool with bounded queue

```kotlin
val corePoolSize = 2
val maxPoolSize = 5
val queueCapacity = 25
val threadPool = ThreadPoolExecutor(
    corePoolSize, maxPoolSize,
    keepAliveMillis, TimeUnit.SECONDS,
    ArrayBlockingQueue(25)
)

// Threads grow to 5 before new tasks are rejected
// when queue (25) is full and core threads busy → tasks rejected
```

## Trade-offs

| Trade-off                 | Coarse (newFixedThreadPool) | Fine (ThreadPoolExecutor) |
|---------------------------|-----------------------------|----------------------------|
| Simplicity                | ✅ Easy                     | ✅ Slightly more config    |
| Control                  | Low                         | High                       |
| Overhead                  | Low                         | Low                        |
| Fairness                | Unfair by default           | Fair option available      |

## Key takeaway

Thread pools avoid creating threads per task and keep resource usage bounded. `newFixedThreadPool(n)` is the default choice; use `ThreadPoolExecutor` when you need bounded queues and rejection policies.

---
