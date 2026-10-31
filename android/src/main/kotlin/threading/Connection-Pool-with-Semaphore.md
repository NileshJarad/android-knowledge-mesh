# Connection Pool with Semaphore — Build Exercise

This build exercise constructs a database connection pool backed by a semaphore. The pool limits how many connections can be borrowed simultaneously.

## Step 1: Define the pool

```kotlin
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ConnectionPool(private val maxConnections: Int) {
    private val semaphore = Semaphore(maxConnections, true) // fair
    private val connections = Array<Any>(maxConnections) { Any() }

    fun acquire(timeoutMillis: Long = 5000): Any {
        try {
            semaphore.acquire(timeoutMillis, TimeUnit.MILLISECONDS)
            return Any() // acquire connection
        } catch (e: TimeoutException) {
            throw IllegalStateException("No connection available")
        }
    }

    fun release() {
        semaphore.release()
    }
}
```

## Step 2: Use the pool

```kotlin
fun main() {
    val pool = ConnectionPool(5)
    val threads = (1..10).map { t ->
        Thread {
            val conn = pool.acquire()
            try {
                // use connection
                Thread.sleep(100)
                println("Thread $t used connection")
            } finally {
                pool.release() // always release back
            }
        }
    }
    threads.forEach { it.start() }
    threads.forEach { it.join() }
}
```

## Step 3: Observe fairness

`Semaphore(maxConnections, true)` gives FIFO fairness. This means threads waiting for connections are granted in order, reducing starvation.

## Step 4: Test timeout behavior

```kotlin
val pool = ConnectionPool(5)

// Try to acquire 10 connections with a short timeout
val threads = (1..10).map { t ->
    Thread {
        try {
            val conn = pool.acquire(100) // short timeout
            println("Thread $t got connection")
        } catch (e: TimeoutException) {
            println("Thread $t timed out")
        }
    }
}
```

Some threads will time out because only 5 connections are available.

## Step 5: Verify

Run the pool with 5 max connections and check that at most 5 connections are borrowed simultaneously.

## Key takeaway

Semaphores back connection pools by limiting concurrent acquisitions. Fair acquisition prevents starvation.

## Trade-offs

| Approach              | Pros                          | Cons                              |
|-----------------------|-------------------------------|-----------------------------------|
| Semaphore             | Simple, fair                  | No connection lifecycle management|
| Manual lock           | More control                  | More code, error-prone            |
| Semaphore + lock      | Fine-grained control          | Complexity                        |

---