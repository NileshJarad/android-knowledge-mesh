package heap_priority_queue

/**
 * LeetCode: https://leetcode.com/problems/find-median-from-stream-of-integers/
 * Difficulty: Easy
 * Core pattern: Two heaps (min for lower half, max for upper half)
 */
import kotlin.math.max
import kotlin.math.min

class MedianFromStream {
    private val minHeap = MinHeapImpl<Int>()  // lower half
    private val maxHeap = MaxHeapImpl<Int>()  // upper half (reversed min heap)

    fun addNumber(num: Int) {
        // Insert into appropriate heap based on current median
        if (minHeap.isEmpty() || num <= minHeap.peek()) {
            minHeap.insert(num)
        } else {
            maxHeap.insert(num)
        }

        // Balance: keep sizes such that lower has one more or equal
        when {
            minHeap.size > maxHeap.size + 1 -> {
                val fromMin = minHeap.extractMin()!!
                maxHeap.insert(fromMin)
            }
            maxHeap.size > minHeap.size -> {
                val fromMax = maxHeap.extractMax()!!
                minHeap.insert(fromMax)
            }
        }
    }

    fun findMedian(): Double {
        return when {
            minHeap.size > maxHeap.size -> minHeap.peek()!!.toDouble()
            else -> (minHeap.peek()!! + maxHeap.peek()!!).toDouble() / 2.0
        }
    }
}

// MinHeap implementation using ArrayDeque
class MinHeapImpl<T>(private val compare: (T, T) -> Boolean = { a, b -> a < b }) {
    private val data = ArrayDeque<T>()

    fun isEmpty(): Boolean = data.isEmpty()
    fun size(): Int = data.size

    fun peek(): T? = if (data.isNotEmpty()) data.first() else null

    fun insert(item: T) {
        data.addLast(item)
        siftUp(data.size - 1)
    }

    fun extractMin(): T? {
        if (data.isEmpty()) return null
        val item = data.removeFirst()
        siftDown(0)
        return item
    }

    private fun siftUp(idx: Int) {
        var i = idx
        while (i > 0) {
            val parentIdx = (i - 1) / 2
            if (!compare(data[i], data[parentIdx])) break
            swap(i, parentIdx)
            i = parentIdx
        }
    }

    private fun siftDown(idx: Int) {
        var i = idx
        while (true) {
            val leftIdx = 2 * i + 1
            val rightIdx = 2 * i + 2
            var smallest = i
            if (leftIdx < data.size && compare(data[leftIdx], data[smallest])) smallest = leftIdx
            if (rightIdx < data.size && compare(data[rightIdx], data[smallest])) smallest = rightIdx
            if (smallest == i) break
            swap(i, smallest)
            i = smallest
        }
    }

    private fun swap(i: Int, j: Int) {
        val temp = data[i]
        data[i] = data[j]
        data[j] = temp
    }
}

// MaxHeap using inverted comparator
class MaxHeapImpl<T>(compare: (T, T) -> Boolean = { a, b -> a > b }) : MinHeapImpl(compare) {
    // Override insert to use reversed logic? No — just reuse with comparator.
    // Actually MinHeap with `{ a, b -> a > b }` gives us a max heap!
}
