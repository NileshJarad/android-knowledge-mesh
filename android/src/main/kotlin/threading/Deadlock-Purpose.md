# Deadlock — Build Exercise

This build exercise deliberately creates a deadlock to understand the failure mode, then shows how to fix it.

## Step 1: Create the deadlock scenario

Two threads each hold one lock and wait for the other's lock. Neither can proceed.

```kotlin
class Account {
    var balance = 0
    val lock = java.util.concurrent.locks.ReentrantLock()
}

class PaymentService {
    fun transfer(from: Account, to: Account, amount: Int) {
        from.lock.lock()
        to.lock.lock()
        try {
            from.balance -= amount
            to.balance += amount
        } finally {
            to.lock.unlock()
            from.lock.unlock()
        }
    }
}

fun main() {
    val a = Account()
    val b = Account()

    val t1 = Thread {
        a.lock.lock()
        b.lock.lock()
        try {
            // do transfer
        } finally {
            b.lock.unlock()
            a.lock.unlock()
        }
    }

    val t2 = Thread {
        b.lock.lock()
        a.lock.lock()
        try {
            // do transfer
        } finally {
            a.lock.unlock()
            b.lock.unlock()
        }
    }

    t1.start()
    t2.start()
}
```

## Step 2: Observe

Both threads will deadlock waiting for each other's lock. The program hangs forever.

## Step 3: Diagnose

The deadlock is a **circular wait**:

```
Thread 1: holds A, waits for B
Thread 2: holds B, waits for A
```

## Step 4: Fix with consistent lock ordering

Always lock in the same order:

```kotlin
fun safeTransfer(from: Account, to: Account, amount: Int) {
    val first = if (from === a) to else from
    val second = if (first === a) a else b
    // Lock in ascending order
    first.lock.lock()
    second.lock.lock()
    try { ... } finally { second.lock.unlock(); first.lock.unlock() }
}
```

## Step 5: Verify

The fixed program completes without hanging.

## Key takeaway

Use consistent lock ordering to avoid deadlocks.

## Trade-offs

| Approach              | Pros                          | Cons                              |
|-----------------------|-------------------------------|-----------------------------------|
| Consistent ordering   | Prevents deadlock             | Requires discipline               |
| Timeout + retry       | Avoids permanent hang         | Leaves deadlock risk              |
| Deadlock detection    | Runtime safety                | Overhead                         |

---