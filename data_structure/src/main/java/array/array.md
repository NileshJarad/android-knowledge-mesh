# Array

- Array is data structure which allows to store the data of same type.
- Arrays store the data in continuous memory location
- It allows index base value access
- Arrays can be of different types
    - One-Dimensional - knows as Vector
    - Two-Dimensional - knows as Matrix
    - Multi Dimensional
- Array can have maximum approx  **2^31 – 1**. Based on this approximation, we can say that the array can theoretically hold 2,147,483,647 elements.

```kotlin
for (i in 2 downTo 0) {
    try {
        val arr = IntArray(Int.MAX_VALUE - i)
        println("Max-Size : ${arr.size}")
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

```


| Operation | Non-Sorted | Sorted   |
|-----------|------------|----------|
| Insert    | O(N)       | O(N)     |
| Delete    | O(N)       | O(N)     |
| Search    | O(N)       | O(log N) |
| Access    | O(1)       | O(1)     |



----

## Tricks 
- Can be solved easily by sorting ?
- Can be solved if we think from reverse order?
- Can we find the index relation ?
- Can it be solved by index as hash?
- Can it be solved using sliding window ?
- Can it be solved by two pointer 
