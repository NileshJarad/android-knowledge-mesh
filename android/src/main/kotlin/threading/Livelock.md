# Livelock

## Definition

Livelock occurs when threads execute **active but pointless work** — they keep reacting to each other and making decisions based on what the peer did, but no real progress happens. Semantically similar to deadlock; the *underlying* distinction is that livelock keeps threads running rather than parking them.

### The visual difference

```
Deadlock:  Thread A ▯  Thread B ▯  (both parked, CPU idle)
Livelock:  Thread A ↻  Thread B ↻  (both spinning, CPU burning)
```

## Why it matters

Livelock smells like a busy-wait loop that reviewers love to poke. Distinguishing deadlock (can't make any move) from livelock (can make moves but they cancel each other) is essential: only deadlock truly stalls; livelock hides behind high CPU usage.

## Code example: two workers mirroring each other

```kotlin
var turn = 0
val lock = java.util.concurrent.locks.ReentrantLock()

val workerA = Thread {
    for (i in 1..1000000) {
        lock.lock()
        turn = 0  // Worker A claims turn
        lock.unlock()
        Thread.sleep(0)  // yield
    }
    println("Worker A finished")
}

val workerB = Thread {
    for (i in 1..1000000) {
        lock.lock()
        turn = 1  // Worker B claims turn
        lock.unlock()
        Thread.sleep(0)  // yield
    }
    println("Worker B finished")
}

workerA.start()
workerB.start()
workerA.join()
workerB.join()
```

### What happens

Both workers run millions of iterations, but `turn` never settles to a stable value because each worker sees the peer just reacted, sets its *counter*, and loops again. No state transitions; CPU burns forever with no real work accomplished.

## System Design summary

Borrow the rule: if two counterparties can always reverse the same decision you made about them, the protocol is livelocky and needs a *deterministic* tie-break (e.g., oldest timestamp wins, random seed win, leader election) to guarantee monotonic progress.

## Key takeaway

Livelock is active but goes nowhere. Prevent it with deterministic tie-breaking rules so that conflicts resolve to a single winner rather than endless back-and-forth.