# Thread Pool with Bounded Queue — Build Exercise

This build exercise constructs a thread pool with a bounded queue, demonstrating resource limits and backpressure.

## Step 1: Create the thread pool

```kotlin
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.ThreadFactory
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.AbortPolicy

class TaskPool {
    private val threadPool: ThreadPoolExecutor = ThreadPoolExecutor(
        2, 5,          // core 2, max 5
        30, TimeUnit.SECONDS,
        ArrayBlockingQueue(25), // bounded queue
        ThreadFactory { r -> Thread(r, "task-pool") },
        AbortPolicy()   // reject if queue full
    )

    fun submit(task: () -> Unit) {
        threadPool.execute(task)
    }

    fun shutdown() {
        threadPool.shutdown()
        threadPool.awaitTermination(30, TimeUnit.SECONDS)
    }
}
```

## Step 2: Use it

```kotlin
fun main() {
    val pool = TaskPool()
    for (i in 1..20) {
        pool.submit { heavyTask(i) }
    }
    pool.shutdown()
}

fun heavyTask(id: Int) {
    println("Task $id started on ${Thread.currentThread().name}")
    Thread.sleep(1000)
    println("Task $id completed")
}
```

## Step 3: Observe

With core=2, max=5, queue=25, 20 tasks:
- First 2 tasks run immediately on core threads.
- Tasks 3–25 queue up.
- Tasks 26+ are rejected (with AbortPolicy).

## Step 4: Test rejection

```kotlin
val pool = TaskPool()

// Submit more tasks than capacity
for (i in 1..100) {
    try {
        pool.submit { heavyTask(i) }
    } catch (e: RejectedExecutionException) {
        println("Task $i rejected")
    }
}
```

## Step 5: Verify

Run the pool and observe that at most 5 threads are active simultaneously.

## Key takeaway

Bounded queue provides backpressure; thread pool limits resources.

## Trade-offs

| Approach              | Pros                          | Cons                              |
|-----------------------|-------------------------------|-----------------------------------|
| Bounded queue         | Memory constrained            | May reject tasks                  |
| Unbounded queue       | Never rejects                 | Possible OOM                      |
| Rejection policy      | Predictable                   | Loses tasks                       |

---