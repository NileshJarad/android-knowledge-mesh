# Race Condition

## Definition

A race condition occurs when two or more threads access shared state concurrently and at least one of them writes. Without synchronization, the outcome is non-deterministic — it depends on the whim of the scheduler.

### The canonical example

```kotlin
var counter = 0

// Background task 1: increments counter by 1..1000
val t1 = Thread {
    for (i in 1..1000) Thread.sleep(2)
    counter += i
}

// Background task 2: increments counter by 1000..1
val t2 = Thread {
    for (i in 1000 downTo 1) Thread.sleep(7)
    counter += i
}

t1.start()
t2.start()
Thread.sleep(3000)
println("Final counter = $counter") // NOT necessarily 1000!
```

The bug: `counter += i` is a **read-modify-write** of a single non-atomic field. Between reading and writing, another thread can read the same value and write its own — the update is **lost**. The final value will be less than expected, or in weird interleavings, torn.

### Three failure modes

| Failure mode          | Description                                              | Example                                            |
|-----------------------|----------------------------------------------------------|----------------------------------------------------|
| **Lost update**       | Two writers read value `5`, both write `7`; final = 7   | Increment from two threads on shared counter      |
| **Torn write**        | Multi-field update: read A/B, write both separately    | Two-field config update without a compound op      |
| **Stale read**        | Reader reads old value after writer wrote newer value | UI showing stale cache value                       |

### Why it matters

If you cannot name the three failure modes, you cannot explain how you will fix a race condition once detected.

## Example that shows lost update clearly

```kotlin
class BankAccount {
    private var balance: Int = 1000

    fun deposit(amount: Int) { balance += amount }        // ❌ Not atomic across threads
}

val t1 = Thread { for (i in 1..100) acc.deposit(10) }  // deposits total 1000
val t2 = Thread { for (i in 1..100) acc.deposit(10) }  // deposits total 1000
t1.start(); t2.start()
Thread.sleep(2000)
println(acc.balance) // Might be < 2000
```

### When you'll see it

- Local DB caching where two workers issue `UPDATE users SET cache = ...` simultaneously.
- Request deduplication logic where two coroutines pass an identity check before writing.
- Cross-process IPC where two services read the same shared memory page.

## Summary

- Read-modify-write races are the most common.
- Lost updates arise when multiple writers merge into one final field without merging their intermediate values; use atomic CAS or optimistic locking.
- Torn writes matter for large buffers and multi-field updates; split updates across finer granularity or use compound operations.

Key takeaway: **A race condition is a synchronization bug.** If you don't see synchronization, you likely have one. If you do, ask whether the critical section is small enough for the chosen primitive, and whether every shared writer is covered.