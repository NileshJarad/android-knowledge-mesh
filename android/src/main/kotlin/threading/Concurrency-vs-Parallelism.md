# Concurrency vs Parallelism

This is the most-asked conceptual trap in concurrency interviews. The two terms look interchangeable but are not interchangeable at all.

## What is concurrency?

**Concurrency** = interleaving. Multiple tasks *cooperate* by taking turns on a single core. They overlap in logical time without overlapping in wall-clock time.

### The single-core experiment

Put two CPU-bound tasks on a **single-core** machine and watch them both make progress within one wall-clock tick. No task runs at the same instant as the other — yet the program is concurrent.

```kotlin
// Example: two threads sharing one core
val t1 = Thread {
    for (i in 1..5) {
        println("Task A step $i")
        Thread.sleep(100) // yields CPU
    }
}
val t2 = Thread {
    for (i in 1..5) {
        println("Task B step $i")
        Thread.sleep(100)
    }
}
t1.start()
t2.start()
Thread.sleep(1200)
```

Typical output (order interleaved, not simultaneous):

```
Task A step 1
Task B step 1
Task A step 2
Task B step 2
...
```

Only one task executes at any instant, but both complete. That is concurrency.

## What is parallelism?

**Parallelism** = true simultaneous execution on multiple cores. Two tasks occupy two CPUs at the same instant.

### The multi-core experiment

On a machine with at least two CPU cores:

```kotlin
val t1 = Thread {
    for (i in 1..5) {
        println("Task A step $i")
        Thread.sleep(100)
    }
}
val t2 = Thread {
    for (i in 1..5) {
        println("Task B step $i")
        Thread.sleep(100)
    }
}
t1.start()
t2.start()
Thread.sleep(1200)
```

Typical output (both may print at roughly the same moment):

```
Task A step 1   Task B step 1
Task A step 2   Task B step 2
```

Both cores are busy simultaneously. That is parallelism — and it is impossible on a single core.

## Side-by-side comparison

| Aspect           | Concurrency                                         | Parallelism                                      |
|------------------|-----------------------------------------------------|--------------------------------------------------|
| Definition       | Tasks interleave and take turns                     | Tasks run simultaneously on separate cores       |
| CPU requirement  | Works on one core                                   | Needs two or more cores                          |
| Real-time        | Not simultaneous                                    | Simultaneous                                     |
| Primary goal     | Structure, responsiveness, throughput under load    | Raw speed, reduced wall-clock time               |
| Example in Kotlin| `CoroutineScope(Dispatchers.Default).launch{}`      | Thread pool across multiple cores                |

## Visual diagram

```mermaid
graph TB
    subgraph Concurrency ["Concurrency — one core, interleaved"]
        A1["Task A starts"] -->|"pause| A2["Task A waits"]
        A2 -->|"resume"| A3["Task A runs"]
        B1["Task B starts"] -->|"pause| B2["Task B waits"]
        B2 -->|"resume"| B3["Task B runs"]
    end
    subgraph Parallelism ["Parallelism — two cores, simultaneous"]
        P1["Task A on Core 1"] -.->|"same instant"| P2["Task B on Core 2"]
    end
```

## Why the distinction matters

A Staff Engineer answer is expected to deliver this distinction, not just rephrase the definition. Worse, picking the wrong term immediately makes the next half-hour of your answer wrong.

| Scenario                          | Correct term | Reason                                              |
|-----------------------------------|--------------|-----------------------------------------------------|
| Two coroutines on `Dispatchers.IO`| Concurrency  | They interleave waiting on network I/O              |
| Two computations on different cores| Parallelism | They truly execute at the same instant              |
| UI thread + background worker     | Concurrency  | Alternate between UI updates and background work    |

## When you will see it

- **"Are these two coroutines running concurrently?"** → look for `delay`, suspension points, or dispatcher contention, not wall-clock overlap.
- **"Is this system parallel?"** → ask how many cores / hardware threads participate in the workload.
- **"Can we speed this up by adding threads?"** → concurrency structure first; parallelism only if you actually have spare cores and CPU-bound work.

## Key takeaway

- **Concurrent** = interleaving, possible on one core.
- **Parallel** = simultaneous multi-core execution.
- In system design both usually appear together; naming both correctly is what separates good answers from bad ones.