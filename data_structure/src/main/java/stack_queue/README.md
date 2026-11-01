# Stack and Queue

## Concept Explanation

### What is a Stack?
A stack is an LIFO (Last In, First Out) data structure. Elements are added and removed from the same end called the "top". Think of a stack of plates — you can only add/remove from the top.

**Operations:**
- **Push**: Add element to top — O(1)
- **Pop**: Remove top element — O(1)
- **Peek/Top**: View top without removing — O(1)
- **isEmpty**: Check if empty — O(1)

### What is a Queue?
A queue is FIFO (First In, First Out) data structure. Elements are added at one end ("rear"/"tail") and removed from the other end ("front"). Think of waiting in line at a ticket counter.

**Operations:**
- **Enqueue** (push back): Add to rear — O(1) with circular buffer
- **Dequeue** (pop front): Remove from front — O(1) with circular buffer
- **Peek/Front**: View front without removing — O(1)
- **isEmpty**: Check if empty — O(1)

### Implementation Details

**Stack implementations:**
- **Array-based**: Fixed array with `top` pointer, resizable when needed
- **Linked list based**: Nodes linked together, head = top of stack
- **Kotlin approach**: Use `ArrayDeque`, `Stack`, or implement your own

**Queue implementations:**
- **Array-based (circular)**: Circular array with front and rear pointers — no shifting needed
- **Linked list based**: Front = dequeue target, rear = enqueue target
- **Deque-based**: Use `ArrayDeque` which supports both push/pop from both ends efficiently

### Real-Life Analogy
- **Stack**: Pile of plates at restaurant table — you stack them on top; take from top only
- **Queue**: Conveyor belt at checkout — items stay in order; first item served first
- **Deques**: Shopping cart — items added at cart edge, taken from any edge

### Time and Space Complexity

| Operation | Stack | Queue (circular) | Notes |
|-----------|-------|-------------------|-------|
| Push | O(1) | O(1) | Both efficient |
| Pop | O(1) | N/A (peek/dequeue only) | |
| Peek | O(1) | O(1) | Both efficient |
| Enqueue | N/A | O(1) | Circular avoids shift |
| Dequeue | N/A | O(1) | Circular avoids shift |
| Size | O(1) | O(1) | Stored separately |

### Common Mistakes and Edge Cases

1. **Underflow errors**: Pop/peek on empty stack throws exception; empty queue throws exception
2. **Null elements**: Stack allows null (if designed that way); some problems forbid nulls
3. **Forgetting size tracking**: Need to track top index / front+re rear index for O(1) size check
4. **Off-by-one in circular buffer**: When front == rear means empty, not full — careful boundary handling
5. **Concurrent modification while iterating**: Iterate with explicit iterator() rather than simple for loop over entire collection
6. **Array overflow**: Array-based structures need resizing logic when capacity exceeded
7. **Head changes in queue deletion**: Always update head pointer in dequeue operations
8. **Null pointer dereferences**: In Java/Kotlin, accessing null node's next gives NullPointerException
9. **Pop from non-empty assumption**: Verify stack/queue is non-empty before pop/dequeue
10. **Not handling resize in array-based structures**: Resize when load factor exceeds threshold

### When to Use Stack vs Queue in Interviews

| Scenario | Data Structure | Why |
|----------|---------------|-----|
| Syntax parsing, bracket matching | Stack | LIFO matches nesting order |
| Undo/redo operations | Stack (two stacks!) | Each undo pops last action |
| Function call stack | Stack | Recursion uses call stack |
| Scheduling tasks | Queue | FIFO task execution |
| Print jobs / messages | Queue | Order preservation |
| Deduplication | Stack + Set | Push seen values, detect duplicates |
| Monotonic stacks | Stack | Track minimum/maximum ending values |
| BFS level processing | Queue | Level by level expansion |
| Top K frequent items | Heap + counter | Efficient max extraction |
| Palindrome checking | Stack | Build, compare, check |

## Important Patterns

### 1. Monotonic Stack (increasing)
- Maintain increasing stack; when new element smaller than top, pop until condition met
- Used for: largest rectangle in histogram, maximum area problems
- Time: O(N), Space: O(N)

### 2. Monotonic Stack (decreasing)
- Maintain decreasing stack
- Used for: next greater element, previous smaller element
- Time: O(N), Space: O(N)

### 3. Two Stacks for Queue
- Use two stacks to simulate queue: enqueue pushes to stackA, dequeue pops from stackA then pushes to stackB
- Amortized O(N) per operation across N operations
- Classic problem for teaching amortized analysis

### 4. Queue Using Stack (naive)
- Enqueue → push to stackB, Dequeue → pop from stackA then push stackA to stackB
- Naive implementation; more complex than proper implementation

### 5. String Reversal with Stack
- Push characters onto stack, pop to reverse order
- Simple but less efficient than direct pointer manipulation

## Dry Run

### Example: Balanced Parentheses
Using stack:
```
Push '(', push '('
Check peek: '(', next char is ')', pop
Check peek: '(', next char is ')', pop
Check peek: '(', next char is ')', pop
Stack empty → balanced!
```
If any mismatch at peek: unbalanced. If leftover elements: unbalanced.

### Example: Next Greater Element (decreasing monotonic stack)
Input: `[7, 1, 5, 3, 6, 4]`
Output: `[None, None, 5, 6, None, None]`

Processing with decreasing stack:
- 7: stack empty, push 7. Result[7]=None
- 1: 1 < 7, push 1
- 5: 5 > 1 (top), 5 > 7? No. Push 5
- 3: 3 < 5, push 3
- 6: pop 3, 6 > 5? yes, result[6]=5. pop 5, 6 > 7? No. Push 6
- 4: pop 6, 4 < 6, push 4. Stack = [7, 6, 4]
Result: [None, None, 5, 6, None, None] ✓

## Code Template

```kotlin
package stack_queue

/**
 * Template for stack/queue problems in Kotlin
 */
// Node definition for custom implementation
class Node<T>(val data: T, var next: Node<T>? = null) {
    companion object {
        fun create(data: T, next: Node<T>? = null): Node<T> {
            return Node(data, next)
        }
    }
}

// Monotonic increasing stack template
fun largestRectangleArea(heights: IntArray): Int {
    val stack = mutableListOf<Int>()
    var maxArea = 0
    for (i in heights.indices) {
        while (stack.isNotEmpty() && heights[stack.last()] > heights[i]) {
            val h = heights[stack.pop()]
            val w = if (stack.isEmpty()) i else i - stack.last() - 1
            maxArea = maxOf(maxArea, h * w)
        }
        stack.add(i)
    }
    // Remaining elements in stack extend to end of array
    while (stack.isNotEmpty()) {
        val h = heights[stack.pop()]
        val w = if (stack.isEmpty()) heights.size else heights.size - stack.last() - 1
        maxArea = maxOf(maxArea, h * w)
    }
    return maxArea
}
```

### Complexity Analysis (Template)
- **Time**: O(N) for monotonic stack algorithms, O(1) amortized for two-stack queue simulation
- **Space**: O(N) for stack storage, O(1) auxiliary beyond storage

## Practice Problems

### Easy Problems

#### 1. **Valid Parentheses**
- **LeetCode**: https://leetcode.com/problems/valid-parentheses/
- **Difficulty**: Easy
- **Core pattern**: Stack checking nesting
- **Approach hint**: Push opening brackets. On closing bracket, verify matching open was pushed (peek check). Pop when matched. Final check: stack should be empty.
- **Expected time**: O(N), **space**: O(N)

#### 2. **Implement Queue Using Two Stacks**
- **LeetCode**: https://leetcode.com/problems/implement-queue-using-stacks/
- **Difficulty**: Easy
- **Core pattern**: Two stacks simulate queue — enqueue to stackA, dequeue from stackA→stackB reverse
- **Approach hint**: When dequeue needs work and stackA is empty, pop all of stackA into stackB then pop from stackB. Amortized O(1) per operation across N calls.
- **Expected time**: O(N) amortized per operation, **space**: O(N)

### Medium Problems

#### 3. **Maximum Area of Rectangle in Histogram**
- **LeetCode**: https://leetcode.com/problems/largest-rectangle-in-histogram/
- **Difficulty**: Medium
- **Core pattern**: Increasing monotonic stack
- **Approach hint**: For each bar, find how far left and right it can extend before shorter bar blocks. Pop shorter bars and compute area using remaining height. Total O(N).
- **Expected time**: O(N), **space**: O(N)

#### 4. **Next Greater Element I**
- **LeetCode**: https://leetcode.com/problems/next-greater-element-i/
- **Difficulty**: Medium
- **Core pattern**: Decreasing monotonic stack
- **Approach hint**: Traverse left to right. New element smaller than top → pop and record result for popped value. Push current onto stack. Remaining have no next greater → None.
- **Expected time**: O(N), **space**: O(N)

### Hard Problems

#### 5. **3Sum Smaller (find triples sum < k)**
- **LeetCode**: https://leetcode.com/problems/3sum-smaller/
- **Difficulty**: Hard
- **Core pattern**: Sort + two pointers + prefix count modification
- **Approach hint**: Sort array. For each i from 0 to n-3, use two pointers on i+1..n-1 with left+right varying. Count pairs where sum < target - arr[i]. Early termination when pair count exceeds remaining space.
- **Expected time**: O(N²), **space**: O(N) sorted or O(1) if sorting in place

#### 6. **Product of Array Except Self**
- **LeetCode**: https://leetcode.com/problems/product-of-array-except-self/
- **Difficulty**: Medium
- **Core pattern**: Prefix × Suffix arrays + forward and backward passes
- **Approach hint**: Build prefixProducts[i] = product of [0..i], suffixProducts[i] = product of [i..n-1]. Answer[i] = prefixProducts[i-1] * suffixProducts[i+1]. O(N) without division, O(N) space (can optimize to O(1) extra).
- **Expected time**: O(N), **space**: O(N) (or O(1) extra excluding output)

---
*End of Stack and Queue topic*