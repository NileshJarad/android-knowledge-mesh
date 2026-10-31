# Producer-Consumer

The producer-consumer pattern is a concurrency pattern where one or more producer threads add items to a shared buffer, and one or more consumer threads remove items from it.

## Why it matters

It decouples production from consumption. Producers don't wait for consumers; consumers don't wait for producers (except when the buffer is full/empty). This is the backbone of any pipeline, queue, or stream processing system.

## Classic implementation with blocking queue

```kotlin
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue

class MessageQueue {
    private val queue: BlockingQueue<String> = ArrayBlockingQueue(100)

    fun produce(item: String) = queue.put(item)      // blocks if full
    fun consume(): String = queue.take()              // blocks if empty
}
```

## Kotlin coroutine channels

```kotlin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.send
import kotlinx.coroutines.channels.receive

class CoroutineQueue {
    private val channel = Channel<String>(100)

    suspend fun produce(item: String) = channel.send(item)
    suspend fun consume(): String = channel.receive()
}
```

## Comparison

| Primitive            | Blocks thread? | Backpressure | Cancellation |
|----------------------|----------------|--------------|--------------|
| `ArrayBlockingQueue` | Yes            | Yes (put blocks) | InterruptedException |
| `Channel`            | Suspends       | Yes (send suspends) | Job cancellation |

## When you'll see it

- Data pipelines (file reader → parser → writer)
- Logging systems (producers log, consumer writes to disk)
- Task queues (web requests enqueue jobs, workers process)

## Key takeaway

Producer-consumer decouples throughput and latency. Use `BlockingQueue` for thread-blocking code, `Channel` for coroutines. Both provide backpressure automatically.