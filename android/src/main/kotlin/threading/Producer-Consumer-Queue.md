# Producer-Consumer Queue — Build Exercise

This build exercise constructs a complete producer-consumer system with a bounded blocking queue, demonstrating backpressure, shutdown, and graceful termination.

## Step 1: Create a bounded blocking queue

```kotlin
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

val queue: BlockingQueue<String> = ArrayBlockingQueue(100) // bounded
```

The queue has capacity 100. When full, producers block. When empty, consumers block. This is **backpressure**.

## Step 2: Implement the producer

```kotlin
class Producer(
    private val queue: BlockingQueue<String>,
    private val producerId: Int
) : Runnable {
    override fun run() {
        try {
            for (i in 1..1000) {
                val item = "Producer-$producerId-item-$i"
                queue.put(item) // blocks if queue is full
                println("Produced: $item")
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
```

## Step 3: Implement the consumer

```kotlin
class Consumer(
    private val queue: BlockingQueue<String>,
    private val consumerId: Int,
    private val stopFlag: AtomicBoolean
) : Runnable {
    override fun run() {
        while (!stopFlag.get() || queue.isNotEmpty()) {
            try {
                val item = queue.poll(1, TimeUnit.SECONDS)
                if (item != null) {
                    processItem(item)
                    println("Consumed by $consumerId: $item")
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun processItem(item: String) {
        // simulate work
        Thread.sleep(10)
    }
}
```

## Step 4: Wire it all together

```kotlin
import java.util.concurrent.atomic.AtomicBoolean

fun main() {
    val stopFlag = AtomicBoolean(false)
    val queue = ArrayBlockingQueue<String>(100)

    val producers = (1..3).map { Producer(queue, it) }
    val consumers = (1..5).map { Consumer(queue, it, stopFlag) }

    val producerThreads = producers.map { Thread(it) }
    val consumerThreads = consumers.map { Thread(it) }

    // Start consumers
    consumerThreads.forEach { it.start() }

    // Start producers
    producerThreads.forEach { it.start() }

    // Wait for producers to finish
    producerThreads.forEach { it.join() }

    // Signal consumers to stop after queue is drained
    stopFlag.set(true)
    consumerThreads.forEach { it.join() }

    println("All done!")
}
```

## Step 5: Observe backpressure

Reduce the queue capacity to `10`. Notice how:

- Producers block more frequently when the queue fills.
- Consumer throughput stays steady because consumers are ready.
- Producers throttle themselves automatically.

## Step 6: Deadlock risk

If you have **only consumers** (no producers) and the queue starts empty, all consumers will block forever waiting for items. Always ensure shutdown logic handles this.

## Key insight

**Backpressure** appears when producers hit the queue capacity and block. This is the system's way of saying "you're making work faster than consumers can process it; slow down."

## Trade-offs

| Scenario           | Result                                          |
|--------------------|-------------------------------------------------|
| Bounded queue      | Memory constrained; backpressure works          |
| Unbounded queue    | Possible OOM if consumers fall behind           |
| Multiple consumers | Throughput scales; contention increases         |
| Single consumer    | Simpler; bottleneck if consumer too slow        |

---
