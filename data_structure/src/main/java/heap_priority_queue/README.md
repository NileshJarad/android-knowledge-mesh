# Heap and Priority Queue

## Concept Explanation

### What is a Heap?
A heap (specifically a binary heap) is a specialized tree-based data structure that satisfies the heap property:

**Max Heap**: Parent ≥ children at every node. Root holds maximum element.
**Min Heap**: Parent ≤ children at every node. Root holds minimum element.

**Implementation forms:**
1. **Array-based binary heap**: Implemented as an array where parent-child relationships follow index math:
   - Parent of index `i`: `(i - 1) / 2`
   - Left child of `i`: `2i + 1`
   - Right child of `i`: `2i + 2`
   This is efficient and cache-friendly.

2. **Tree-based representation**: Actual binary tree nodes with pointers (less common in interviews but useful for conceptual understanding)

**Operations and complexities:**
| Operation | Time | Notes |
|-----------|------|-------|
| Insert | O(log N) | Add to end, bubble up |
| Extract Max/Min | O(log N) | Remove root, replace with last, bubble down |
| Increase/Decrease Key | O(log N) | Bubble up |
| Peek | O(1) | Return root without removal |
| Heapify (build heap from sorted array) | O(N) | Bottom-up sift down |
| Find kth largest | O(k log N) or O(N) with two heaps | Various approaches |

### What is a Priority Queue?
A priority queue is an abstract data type where each element has a priority, and the element with highest priority is dequeued first. A heap is one way to implement it efficiently.

**Key characteristic**: Unlike regular queues, priorities determine order of access — no FIFO guarantee.

### Real-Life Analogy
Think of an **air traffic control system**:
- Each plane on screen has "priority" (based on altitude, urgency, etc.)
- Most urgent plane gets cleared first, even if scheduled earlier than others
- You constantly maintain priorities: planes don't just drop off in order; highest priority planes always move first
- When queue gets long, priority queue ensures critical flights handled first

### Time and Space Complexity

| Operation | Min Heap | Max Heap | Notes |
|-----------|----------|----------|-------|
| Insert | O(log N) | O(log N) | Both scale with heap height |
| Extract min/max | O(log N) | O(log N) | Remove root, sift down |
| Peek min/max | O(1) | O(1) | Root value |
| Top k frequent items | O(k log N) | O(k log N) | Use two heaps / size-bounded heap |
| Decrease key | O(log N) | O(log N) | Updated and re-heapify |

Space: O(N) for storage of elements.

### Common Mistakes and Edge Cases

1. **Off-by-one index math**: Index formulas `parent = (i-1)/2`, `left = 2i+1` are easy to misremember
2. **Incorrect bubbling direction**: Bubble up on insert (smallest upward); bubble down on extract (largest downward)
3. **Wrong comparison**: Compare children BEFORE parent to find violating sibling, then compare parent with violating child
4. **Not handling empty heap**: Always check before peek/extract operations
5. **Bubble up vs bubble down confusion**: Both involve comparing with parent OR children depending on operation
6. **Heapify order**: Building heap bottom-up requires sifting DOWN from last non-leaf to root — NOT top-down
7. **Integer overflow in array sizing**: For large heaps use `long` array indices or avoid direct array sizing
8. **Duplicate values**: Comparisons must handle ties correctly (`<=` vs `<`) based on problem requirements
9. **Extract all elements repeatedly**: O(N log N) total — efficient overall but watch for redundant passes
10. **Large N → array sizing**: Pre-allocate generously or grow dynamically

### When to Use Heap/Priority Queue in Interviews

| Scenario | Approach | Why |
|----------|----------|-----|
| Top K frequent elements | Two heaps (min + max) | O(K log N) extraction |
| Median from streaming data | Two heaps | O(log N) insert, O(1) peek |
| BFS level processing | Priority queue with BFS | Order by level naturally |
| Huffman coding / Huffman tree | Min heap build | Merge smallest frequencies first |
| Graph Dijkstra | Priority queue (min) | Process nodes by current shortest path |
| Job scheduling with deadlines | Min heap for earliest deadline | Schedule latest finish time first |
| Merge k sorted arrays | Min heap of k pointers | Extract min k times |

## Important Patterns

### 1. Single Heap for k-Largest
- Maintain min-heap of size k; when new element larger than min of heap, replace
- Extract max = peek of max heap; insert into min heap; evict smallest

### 2. Two Heaps for Median (Streaming)
- Min heap for lower half, max heap for upper half
- Balance sizes: keep min heap one element ahead or equal
- Median = midpoint of tops or average of both tops
- O(log N) per insertion, O(1) for median peek

### 3. Sliding Window Minimum (Monotonic Deque alternative)
- Alternatively track minimum via sliding window
- Heap doesn't directly support sliding window efficiently
- Better: monotonic deque approach

### 4. Building Heap from Array (Heapify)
- Bottom-up sift down starting from last non-leaf node
- Only need to re-heapify once for entire array initialization
- O(N) despite being tempting to think O(N log N) — analysis shows telescoping sum converges to O(N)

### 5. Pareto Optimal (Pareto Frontier)
- Consider events sorted by time, yield, deadline simultaneously
- Insert into heap sorted by "next event" value while maintaining Pareto optimality
- Key insight: only events that can't be improved are kept

### 6. Dijkstra's Algorithm Optimization
- Standard: O(E log V) with heap priority queue
- Optimized: decrease-key operation available in some implementations; otherwise simply update priority and reinsert

### 7. Huffman Coding Build
- Repeatedly merge two smallest frequencies, add cost of merge
- Track frequencies; outputed code lengths derived from tree shape

## Dry Run

### Example: Insert into Max Heap (array representation)
Array: [9, 7, 4]
Insert 10: add to end → [9, 7, 4, 10]
Bubble up: 10 > 9 → swap → [10, 7, 4, 9]
Done: 10 at root

Insert 2: add to end → [10, 7, 4, 2]
Bubble up: 2 < 10, stop
Result: [10, 7, 4, 2]

Extract Max: swap root (10) with last → [2, 7, 4, 10], remove last (marked done)
Sift down: 2 < left(7) → swap → [7, 2, 4, 10]
    Now check right child of new position 0... actually checking positions:
    root=0 (val 7), left=1 (val 2), right=2 (val 4)
    Since we compare parent with children, 7 > 4 so no further swap needed
    Done: max still 7
Final array: [7, 2, 4]

Wait — let me redo this. After removing max:
- Array was [10, 7, 4, 10] (last removed)
- Replace root with last element: [10, 7, 4, 10] → after removal [7, 2, 4]
Actually original heap [10, 7, 4, 2]:
- Swap root (10) with last (2): [2, 7, 4]
- Sift down: parent=2 (index 0), left=7 (index 1), right=4 (index 2)
- 2 < 7 → swap → [7, 2, 4]
- Now check if 2 needs further bubbling down: left child exists, 7 is max so 2 stays at index 1? Wait — recheck.
- Parent(0)=2, child(1)=7, child(2)=4. Max of children = 7. 2 < 7 → swap → [7, 2, 4]
- New position of 2 is index 1. Its children: none (both were 7, 4 now). So 2 is leaf now. Done.
Final array: [7, 2, 4]

## Code Template

```kotlin
package heap_priority_queue

/**
 * Template for heap problems in Kotlin
 */
// Min-heap template class (array-based)
class MinHeap<T>(compare: (T, T) -> Boolean) {
    private val data: ArrayDeque<T> = ArrayDeque()

    // Utility methods — simplified placeholder
    fun insert(item: T) { data.addLast(item); siftUp(data.size - 1) }
    fun peek(): T? { return if (data.isNotEmpty()) data.first() else null }
    fun extractMin(): T? { return if (data.isEmpty()) null else { val item = data.removeFirst(); siftDown(0); item }; }
    private fun siftUp(idx: Int) { ... }
    private fun siftDown(idx: Int) { ... }
}

// Utilities: MaxHeap variant using inverted comparator
fun maxHeapFromComparator(compare: (T, T) -> Boolean) {
    return MinHeap { a, b -> compare(b, a) }  // Invert for max heap
}
```

### Complexity Analysis (Template)
- **Time**: O(log N) per operation, O(N) for building from array
- **Space**: O(N) for heap storage

## Practice Problems

### Easy Problems

#### 1. **Find Median from Data Stream**
- **LeetCode**: https://leetcode.com/problems/find-median-from-stream-of-integers/
- **Difficulty**: Easy
- **Core pattern**: Two heaps (min heap for lower half, max heap for upper half)
- **Approach hint**: Maintain balance between heaps. When inserting new value: insert into appropriate heap (based on comparison with median so far), then balance by moving elements between heaps when sizes differ by more than 1. Median = either top of upper heap, or average of both tops.
- **Expected time**: O(log N) per insertion, **space**: O(N)

#### 2. **Kth Largest Element in Stream**
- **LeetCode**: https://leetcode.com/problems/kth-largest-element-in-a-stream/
- **Difficulty**: Easy
- **Core pattern**: Min-heap sized k, eviction of smallest elements
- **Approach hint**: Maintain min-heap of exactly k largest seen so far. For each new element: if heap size < k, add and sift up. If new element > heap min, replace min and sift up. Query returns heap min (the kth largest).
- **Expected time**: O(log N) per operation, **space**: O(k)

### Medium Problems

#### 3. **Find Median of Two Sorted Arrays**
- **LeetCode**: https://leetcode.com/problems/median-of-two-sorted-arrays/
- **Difficulty**: Hard
- **Core pattern**: Binary search on partition + careful comparison logic
- **Approach hint**: Binary search the smaller array on partition indices such that left halves combined have equal count of elements. Check if maxLeft[0] ≤ minRight[0]. Adjust partitions based on comparison. Median = avg of two middle elements or single middle when odd total count.
- **Expected time**: O(log(min(M,N))), **space**: O(1)

#### 4. **Huffman Coding**
- **LeetCode**: https://leetcode.com/problems/huffman-coding/ (or similar source)
- **Difficulty**: Medium
- **Core pattern**: Min heap build + frequency counting
- **Approach hint**: Count character frequencies. Build min-heap of (freq, char) pairs. Repeatedly pop two smallest, merge, push merged pair back. Total operations = N-1 merges where each merge cost O(log N). Total time O(N log N) where N = number of distinct chars.
- **Expected time**: O(N log N), **space**: O(N)

### Hard Problems

#### 5. **Super Ugly Number** (Kth Ugly Number Sequence)
- **LeetCode**: https://leetcode.com/problems/super-ugly-number-ii/
- **Difficulty**: Hard
- **Core pattern**: Multiple pointers with heap tracking candidates
- **Approach hint**: Maintain candidate heap. Each candidate represents (product of prime pointer × prime). Candidate comparison uses numeric product modulo number of primes (number of trailing zeros). Track product modulo. Newly generated candidate could be super ugly. Push to heap. Pop smallest at each step.
- **Expected time**: O(N·K) where N = sequence length, K = distinct primes; **space**: O(K)

#### 6. **Build All Possible Monotone Increasing Sequences**
- **LeetCode**: https://leetcode.com/problems/build-array-where-you-can-move-parent-to-child/
- **Difficulty**: Medium (often Hard variant interviews)
- **Core pattern**: Tree DP storing sequences from each subtree
- **Approach hint**: dp[node] = list of possible sequences ending at this node. Merge: combine all sequences from left subtree, append node's value, combine all sequences from right subtree. Filter duplicates. Concatenate results.
- **Expected time**: O(N × output_size), **space**: O(output_size²) per node

---
*End of Heap and Priority Queue topic*