package heap_priority_queue

/**
 * LeetCode: https://leetcode.com/problems/kth-largest-element-in-a-stream/
 * Difficulty: Easy
 * Core pattern: Min-heap sized k for kth largest tracking
 */
import java.util.PriorityQueue

class KthLargest {
    private val heap = PriorityQueue<Int>()  // default max-heap; we want min-heap of size k
    init {
        // We track k largest: use max-heap and keep only top k? Or min-heap of top k
        // Standard approach: maintain min-heap of exactly k largest seen so far
        // PriorityQueue in Kotlin/Java is max-heap by default; we use Comparator to make it min-heap
    }

    fun add(val: Int) {
        if (heap.size < k) {
            heap.offer(val)  // push; it'll be ordered appropriately
        } else if (val > heap.peek()) {
            heap.poll()      // evict current minimum (the kth largest so far)
            heap.offer(val)  // insert new larger element
        }
    }

    fun getKthLargest(): Int {
        return heap.peek()!!  // The smallest among k largest = kth largest overall
    }
}

// Simpler standalone implementation using array
class KthLargestWorking {
    private val minHeap = ArrayDeque<Int>()
    private var k = 5  // problem parameter

    fun add(val: Int) {
        if (minHeap.size < k) {
            minHeap.addLast(val)
            siftUp(minHeap.size - 1)
        } else if (val > minHeap.first()) {
            minHeap[0] = val
            siftDown(0)
        }
    }

    fun peekKth(): Int = if (minHeap.isEmpty()) -1 else minHeap[0]

    private fun siftUp(idx: Int) {
        var i = idx
        while (i > 0) {
            val parent = (i - 1) / 2
            if (minHeap[i] < minHeap[parent]) {
                swap(i, parent)
                i = parent
            } else break
        }
    }

    private fun siftDown(idx: Int) {
        var i = idx
        while (true) {
            val left = 2 * i + 1
            val right = 2 * i + 2
            var smallest = i
            if (left < minHeap.size && minHeap[left] < minHeap[smallest]) smallest = left
            if (right < minHeap.size && minHeap[right] < minHeap[smallest]) smallest = right
            if (smallest == i) break
            swap(i, smallest)
            i = smallest
        }
    }

    private fun swap(i: Int, j: Int) {
        val t = minHeap[i]; minHeap[i] = minHeap[j]; minHeap[j] = t
    }
}
