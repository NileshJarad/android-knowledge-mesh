# Backpressure

Backpressure is the mechanism that lets a fast producer slow down when a slow consumer cannot keep up.

## The backpressure problem

```
Producer ──► Buffer ──► Consumer
     heavy load                light load
```

If producer pushes faster than consumer can process, the buffer grows. If unbounded, it can OOM (OutOfMemory). Backpressure controls this by limiting how much work the producer can do.

## Types of backpressure

| Type          | How it works                                   | Example                                      |
|---------------|-----------------------------------------------|-----------------------------------------------|
| **Drop**      | Discard excess items (oldest, newest, random)  | `LinkedBlockingQueue` of bounded size         |
| **Delay**     | Sleep/pause producer until buffer free         | Producer thread yielding between puts         |
| **Signal**    | Explicitly notify producer to stop emitting     | `Channel` full condition                       |
| **Buffer**    | Hold limited items in memory (bounded queue)   | `ArrayBlockingQueue(100)`                    |

## Java/BlockingQueue backpressure

```kotlin
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class RateLimiter {
    private val queue: BlockingQueue<Int> = ArrayBlockingQueue(100)

    fun submit(item: Int): Boolean {
        return queue.offer(item) // returns false if queue full
    }

    fun process() {
        while (true) {
            val item = queue.take() // blocks until item available
            process(item)
        }
    }
}
```

## Kotlin Channel backpressure

```kotlin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

val channel = Channel<Int>(100) // Bounded channel

// Producer: suspends if channel full
suspend fun produce() {
    for (i in 1..1000) {
        channel.send(i) // suspends if full
    }
}

// Consumer: suspends if channel empty
suspend fun consume() {
    for (item in channel) {
        process(item)
    }
}
```

## Reactive streams backpressure (Project Reactor, RxJava)

```kotlin
Flux.range(1, 1000)
    .limitRate(100)   // emit at most 100 items per request
    .subscribe { println(it) }
```

## When you'll see it

- Event loops (database triggers -> listeners)
- Load balancers (servers -> queue -> workers)
- Workflows (stream processing pipelines)

## Key takeaway

Backpressure is about controlling the flow of data from fast producers to slow consumers. Use bounded queues, channel semantics, and explicit signaling to prevent OOM and resource exhaustion.

---
