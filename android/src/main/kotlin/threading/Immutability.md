# Immutability

An immutable object is one that cannot be modified after creation. Once created, its state remains constant.

## Why immutability solves concurrency

Because immutable objects have no internal mutable fields, they are naturally thread-safe. No synchronization is required to read them from any thread at any time.

```kotlin
data class Point(val x: Int, val y: Int) // data class is immutable by default

fun moveRight(p: Point): Point {
    // Returns a new immutable object; original unchanged
    return Point(p.x + 1, p.y)
}
```

## Benefits

| Benefit              | Explanation                               |
|----------------------|-------------------------------------------|
| Thread safety        | No synchronization needed                |
| Predictable behavior | Cannot change unexpectedly                |
| Easier reasoning     | Can't be in inconsistent state           |
| Caching efficiency   | Can share widely without locks           |

## When you'll see it

- Configuration objects
- Value types (Int, String, enums)
- Kotlin data classes
- Functional libraries

## Systems design impact

Immutability changes the entire coordination model:

- **Producers** create immutable values.
- **Consumers** only read them, never modify.
- **Shared caches** are safe to read concurrently.

## Key takeaway

Immutable objects are thread-safe by nature. Use them wherever possible to eliminate complex synchronization issues.

---
