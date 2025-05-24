


## Stack

| Stack Operation | Kotlin Code using `ArrayDeque`     |
|-----------------|------------------------------------|
| Push            | `addLast(element)`  `add(element)` |
| Pop             | `removeLast()`                     |
| Peek            | `lastOrNull()`                     |
| Is Empty        | `isEmpty()`                        |
| Size            | `size`                             |
| Contains        | `contains(element)`                |
| Clear           | `clear()`                          |


## Queue

| Operation | Purpose                          | Method Used on `ArrayDeque`       |
|-----------|----------------------------------|-----------------------------------|
| Enqueue   | Add element to the end (tail)    | `addLast(element)` `add(element)` |
| Dequeue   | Remove element from front (head) | `removeFirst()`                   |
| Peek      | View front element               | `firstOrNull()`                   |
| Is Empty  | Check if queue is empty          | `isEmpty()`                       |
| Size      | Number of items in queue         | `size`                            |
| Contains  | Check if an item exists          | `contains(element)`               |
| Clear     | Remove all items                 | `clear()`                         |


## Char array

1-D

```kotlin
val size = 5
val arr = CharArray(size)  // Default values are '\u0000' (null char)
arr[0] = 'X'

```

2-D
```kotlin
val rows = 3
val cols = 4

val char2D = Array(rows) { CharArray(cols) }

```