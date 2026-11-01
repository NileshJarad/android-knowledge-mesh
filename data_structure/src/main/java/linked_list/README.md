# Linked List

## Concept Explanation

### What is a Linked List?
A linked list is a linear data structure where elements (called nodes) are connected via pointers/references rather than contiguous memory locations. Each node contains:
- **Data**: The value stored
- **Next**: Reference to the next node in the sequence (or null for tail)

**Types:**
1. **Singly Linked List**: Each node points only forward (one-directional traversal)
2. **Doubly Linked List**: Each node has both `next` and `prev` pointers (bidirectional)
3. **Circular Linked List**: Last node points back to first (circular traversal)
4. **Array-based variants**: Not typically called "linked lists" but similar concepts

### How It Works Internally
- Nodes allocated separately in heap memory (non-contiguous)
- Head pointer is the entry point; traverse via following `next` pointers
- No random access — must traverse from head each time (O(N) per access)
- Memory overhead: pointer + data per node (typically 2x memory vs array)

### Real-Life Analogy
Think of linked list as **organizing items by zip codes**:
- Each item has its own folder with the item's zip code and address of next folder
- Items are scattered across different postal boxes — not in any order
- You read one folder, then look at its address to find next item
- Adding new folder: attach after existing last one
- Removing: modify references so removed folder stops leading somewhere
- No fixed size, you can have as many folders as needed

### Time and Space Complexity

| Operation | Singly | Doubly | Notes |
|-----------|-------|--------|-------|
| Insert at head | O(1) | O(1) | Update head pointer |
| Insert at tail | O(N) | O(1) | Need last reference or traverse |
| Delete at head | O(1) | O(1) | Update head pointer |
| Delete at position | O(N) | O(N) | Need prev node; singly needs traverse |
| Find middle | O(N/2) = O(N) | O(N) | Fast+slow pointers |
| Detect cycle | O(N) | O(N) | Floyd's tortoise & hare |
| Merge two sorted | O(N+M) | O(N+M) | Classic merge |
| Reverse | O(N) | O(N) | Iterative or recursive |
| Count nodes | O(N) | O(N) | Simple traversal |

### Common Mistakes and Edge Cases

1. **Null checks**: Always handle empty list (`head == null`), single node, etc.
2. **Dangling pointers**: After deletion, set deleted node's next to null for safety
3. **Memory leaks (Java)**: In languages without GC, manual deletion needed. Kotlin/Java GC handles this automatically.
4. **Reference cycles**: Circular linked list creates infinite loops; detect using Floyd's algorithm
5. **Off-by-one errors**: When deleting at position k, remember we delete node at index k, not 0-indexed from end
6. **Modifying while iterating**: Use explicit iteration with Iterator pattern
7. **NULL pointer dereference**: Java throws NullPointerException, Kotlin lets it propagate
8. **Integer overflow in indices**: Not applicable here but watch out for large sizes
9. **Head changes in deletion**: Must update `head = head.next` when deleting first node
10. **Forgetting reverse `prev` in doubly linked list**: Update both `prev` and `next` on operations

### When to Use Linked List in Interviews

| Scenario | Use Approach | Why |
|----------|-------------|-----|
| Building from scratch without library | Implement your own list | Teach concepts |
| Stack implementation | Arrays or linked list | LIFO operations are natural |
| Queue implementation | Linked list or array | FIFO efficiently |
| Dynamic memory allocation problems | Linked list over array | Size changes freely |
| Merging sorted lists | Sorted merge | Efficient linear merge |
| Removing duplicates | HashSet + traverse | Or sort then scan |
| Cycle detection | Floyd's algorithm | O(N) elegant solution |

## Important Patterns

### 1. Fast & Slow Pointer (Find Middle)
- Fast pointer moves 2 steps, slow pointer moves 1 step
- When fast reaches end, slow is at middle
- Edge case: odd vs even number of nodes (fast ends exactly at last vs lands before)
- Also used in cycle detection and merging sorted lists

### 2. Floyd's Tortoise & Hare (Cycle Detection)
- Slow: moves 1 step, Fast: moves 2 steps
- If they meet → cycle exists
- Why works: relative speed of fast over slow is 1, so they must meet inside cycle
- Also find cycle start node: reset one pointer to head, move both at same speed

### 3. Reverse Linked List
- Iterative: three pointers `prev`, `curr`, `next`
  - `next = curr.next`, `curr.next = prev`, `prev = curr`, `curr = next`
- Recursive: recursively reverse rest, then swap first and last connections
- In-place: no extra space for arrays

### 4. Palindrome Check
- Two pointers from both ends toward center
- For doubly linked list, skip original pointers or use set
- For singly: traverse forward collecting values into array, then check palindrome on array
- Optimal: reverse half then compare without extra storage

### 5. Remove Nth Node From End
- Two pointers: `fast` starts at nth node ahead of `slow`
  Wait — actually: `fast` starts ahead of `slow` by exactly n positions
- Then traverse together until `fast.next == null`; `slow` is at (n+1)th from end
  OR simpler: use fast+slow starting both at head with slow offset by n+1 nodes
- Standard approach: advance fast pointer n+1 times first, then advance both until fast reaches end

### 6. Merge Two Sorted Lists
- Classic merge like merge-sort combine step
- Compare heads of both lists, take smaller, advance that list's pointer
- Link remaining sublists when one is exhausted
- O(N+M) time, O(1) extra if reusing existing nodes

### 7. Circular Linked List Rotation / Circle Dance
- Similar to circular buffer concept
- Detect rotation point, then operate within circular segment

### 8. Given Position, Remove Element
- Same as remove nth from end but generalize
- Handle boundary cases: position 0 (head), position > length (invalid)
- Adjust index if head changes

## Dry Run

### Example: Remove Nth Node From End

**Input**: `list = [1,2,3,4,5], n = 2`
Goal: Remove node at index 3 (value 4), result = [1,2,3,5]

Using two pointers:
```
Fast starts at node 3 (skip first 2: index 1 and 2... wait)
Actually: advance fast by n=2 positions first:
  Start: fast=1, slow=1 (both at head index 0 = node 1)
  Advance fast once: fast=2, slow=1
  Advance fast again: fast=3, slow=1
Now traverse both: fast advances, slow follows
  fast=3, slow=1 → advance both
  fast=4, slow=2
  fast=5, slow=3 (now slow at target position!)
  advance one more since fast.next is null
  fast=null stop
Result: slow is node with value 4 → remove it
```

Result list: [1,2,3,5]

### Example: Detect Cycle

**Input**: list = [3→2→0→-4], next[2] = 4, next[4] = 2 (cycle back to node 2)

**Tortoise & hare dry run:**
```
Step 0: slow=3, fast=3
Step 1: slow=2, fast=0
Step 2: slow=0, fast=2 (wait fast did 2 steps: 3→0→2)
Step 3: slow=-4, fast=2 (fast: 2→0→-4→2)
Step 4: slow=2, fast=2 → SAME NODE! Cycle detected
```

## Code Template

```kotlin
package linked_list

/**
 * Template for linked list problems in Kotlin
 */
// Node definition
class Node<T>(val data: T, var next: Node<T>? = null) {
    companion object {
        fun <T> create(data: T, next: Node<T>? = null): Node<T> {
            return Node(data, next)
        }
    }
}

// Utility functions template
fun reverseList(head: Node<Int>?): Node<Int>? {
    var prev: Node<Int>? = null
    var curr = head
    while (curr != null) {
        val nextTemp = curr.next
        curr.next = prev
        prev = curr
        curr = nextTemp
    }
    return prev
}

// Expected complexity: O(N) time, O(1) space
```

### Complexity Analysis (Template)
- **Time**: O(N) for most list traversals, O(1) for head operations
- **Space**: O(1) auxiliary for pointer manipulation, O(N) for storing references to nodes being created

## Practice Problems

### Easy Problems

#### 1. **Reverse Linked List**
- **LeetCode**: https://leetcode.com/problems/reverse-linked-list/
- **Difficulty**: Easy
- **Core pattern**: Iterative reversal with three pointers
- **Approach hint**: Maintain `prev`, `curr`, `next`. On each iteration, save `curr.next` temporarily, redirect `curr.next = prev`, advance both pointers. Result head is final `prev`.
- **Expected time**: O(N), **space**: O(1)

#### 2. **Remove Nth Node From End of List**
- **LeetCode**: https://leetcode.com/problems/remove-nth-node-from-end-of-list/
- **Difficulty**: Easy
- **Core pattern**: Two pointers with fast pre-advancement
- **Approach hint**: Advance fast pointer n+1 times first, then walk both pointers simultaneously until fast reaches null. Slow pointer is now at (n+1)th from end — node to delete.
- **Expected time**: O(N), **space**: O(1)

### Medium Problems

#### 3. **Linked List Cycle**
- **LeetCode**: https://leetcode.com/problems/linked-list-cycle/
- **Difficulty**: Medium
- **Core pattern**: Floyd's tortoise & hare cycle detection
- **Approach hint**: Slow moves 1 step, fast moves 2. If they ever meet, cycle exists. If fast reaches null, no cycle. To find cycle start: keep one pointer at meeting point, second at head, move both 1 step until they meet.
- **Expected time**: O(N), **space**: O(1)

#### 4. **Merge Two Sorted Lists**
- **LeetCode**: https://leetcode.com/problems/merge-two-sorted-lists/
- **Difficulty**: Easy
- **Core pattern**: Classic merge algorithm
- **Approach hint**: Create dummy head, compare heads of both lists, append smaller to result, advance that list's pointer. When one list exhausted, append remaining of other. Reuse nodes (no new allocations).
- **Expected time**: O(N+M), **space**: O(1)

### Hard Problems

#### 5. **Linked List Random Node**
- **LeetCode**: https://leetcode.com/problems/linked-list-random-node/
- **Difficulty**: Medium (often Hard variant interviews)
- **Core pattern**: Reservoir sampling / two passes
- **Approach hint**: Pass 1: count total length N using traversal. Pass 2: generate random index r uniformly in [0, N), traverse to r-th node, return its value. O(N) time O(1) space.
- **Expected time**: O(N), **space**: O(1)

#### 6. **Kth Largest Value in Binary Tree Linked List**
- **LeetCode**: https://leetcode.com/problems/kth-largest-element-in-a-binary-search-tree/
- **Difficulty**: Medium (may be easier variant)
- **Core pattern**: In-order traversal + quickselect / heap
- **Approach hint**: Do in-order traversal and store values in array, then pick kth largest via QuickSelect (O(N) average) or just sort O(N log N). Simpler: directly build min-heap of size k during traversal.
- **Expected time**: O(N log K) heap / O(N) average QuickSelect, **space**: O(N) or O(K)

---
*End of Linked List topic*