# Idempotency under Concurrency

An operation is **idempotent** if running it multiple times with the same input always produces the same result as running it once. Idempotency ensures safety under retries and concurrent execution.

## Why idempotency matters

When you cannot guarantee exactly-once execution (retries, race conditions), idempotent operations are the only safe way to build distributed systems.

## Idempotent vs. Non-idempotent

```kotlin
// Idempotent: result same each time
fun deleteUser(id: String): Unit = 
    db.execute("DELETE FROM users WHERE id = ?", id) // ❌ Not idempotent

fun deleteUser(id: String): Unit = 
    db.execute("UPDATE users SET deleted_at = NOW WHERE id = ? AND deleted_at IS NULL", id) // ✅ Idempotent
```

## Idempotent patterns

| Pattern            | How it works                               |
|--------------------|---------------------------------------------|
| Check-and-Act      | Read state, act only if condition matches   |
| Tombstone          | Mark resource as done; later requests no-op |
| Versioned updates  | Conditional writes using version numbers   |
| No-op on duplicate | Ignore subsequent duplicate requests         |

## Single-flight deduplication

```kotlin
class SingleFlight {
    private val lock = Mutex()
    private val ongoingRequests = mutableMapOf<String, CompletableDeferred<Result>>()

    suspend fun <T> execute(key: String, block: suspend () -> T): T {
        lock.withLock {
            ongoingRequests[key]?.let { return it.await() }
        }

        val deferred = CompletableDeferred<T>()
        lock.withLock {
            ongoingRequests[key] = deferred as CompletableDeferred<Result>
        }

        return try {
            val result = block()
            lock.withLock {
                ongoingRequests.remove(key)
            }
            deferred.complete(result)
            result
        } catch (e: Exception) {
            lock.withLock {
                ongoingRequests.remove(key)
            }
            deferred.completeExceptionally(e)
            throw e
        }
    }
}
```

## Summary

Idempotency allows safe retry and concurrency. Use check-and-act, tombstones, versioned updates, or deduplication to guarantee idempotent behavior.

---
