# Processes vs Threads

## Definitions

- **Thread**: A unit of execution *inside* one process. Threads share the process's address space and therefore its memory, but each owns its own stack and register set.
- **Process**: An isolated execution environment. Each process gets its own address space, virtual memory, OS scheduler slot, and often its own kernel resource set (file descriptors, sockets, DB connections).

## Why threads share memory is the risk

Because two threads can read and write the same field at the same moment, mutable shared state between threads is the source of every classic concurrency hazard (race conditions, stale reads, torn writes). With processes, two processes cannot read each other's fields through normal memory access — they communicate only through explicit channels (IPC, pipes, sockets). The isolation makes processes safer to reason about, at the cost of heavier bookkeeping.

```kotlin
// Two threads sharing a counter — classic race condition
var counter = 0
val t1 = Thread { for (i in 1..500) counter += i }
val t2 = Thread { for (i in 1..500) counter += 100 - i }
t1.start(); t2.start()
Thread.sleep(300)
println("Final counter = $counter") // Not necessarily 1000!
```

Typical output: `counter` may be less than 1000 because the increments interleave non-atomically.

```kotlin
// Same idea with processes — threads no longer touch shared state directly
// Two processes would communicate via pipes or files. IPC overhead is higher,
// but the invariant "two processes can never corrupt each other's field" holds.
```

## Comparison table

| Aspect           | Thread                                | Process                               |
|------------------|---------------------------------------|---------------------------------------|
| Address space    | Shared                                | Isolated                              |
| Memory sharing   | Direct (risky)                         | Indirect (IPC)                        |
| Creation cost    | Low                                    | High                                  |
| Number limit     | Hundreds of thousands                  | Tens of thousands                      |
| Crash isolation  | One crashes whole process             | One crashes that process only          |
| Cross-process safety | "safe by default"                 | Requires explicit synchronization       |

## When you choose which

### Choose threads when
- You need tight coupling and fast memory access (e.g., one producer writing into a shared buffer consumed by one reader).
- Performance profile is critical and you control all callers (daemon threads are fine if the host isn't going to be killed).

### Choose processes when
- You want crash isolation: a buggy rendering service crashes only that service, not the host app.
- Components must own their own resources (DB connections, file handles, sockets).
- You need stable scheduling semantics across component boundaries (OS schedulers hand out process slots independently).

## Android-specific note

Android uses a single UI thread as the main process thread; long tasks on the main thread cause an ANR (Application Not Responding). Background work typically lives in one of these spots:

- `Dispatchers.IO` coroutine scope (coroutine structured concurrency — strongly preferred over raw threads)
- WorkManager for deferrable, guaranteed background work
- Separate processes for services and content providers where isolation matters

## Summary

Same memory = faster sharing but more dangerous. Isolated = safer but more expensive to manage. The default in Android UI thread work is: one main thread, a handful of background workers on `Dispatchers.IO`, plus separate concerns for things that should not crash the host (WorkManager, separate processes).