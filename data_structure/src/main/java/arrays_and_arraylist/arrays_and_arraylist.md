# Topic 1: Arrays and ArrayList

## Concept Explanation

### What arrays and ArrayList are
- **Arrays**: Fixed-size data structures that store elements of the same type in contiguous memory locations. Elements can be accessed in O(1) time using index.

- **ArrayList** (Java) / **ArrayDeque** (Kotlin): Dynamic arrays that can resize themselves. Underlying array structure but supports dynamic growth with amortized O(1) insertion at end.

### How it works internally
- **Arrays**: Stored in contiguous memory. Memory layout is sequential. Array object contains: (1) reference to memory block, (2) length, (3) component type.

- **ArrayList**: Uses a backing array. When capacity is exceeded, it creates a new larger array (typically 1.5x or 2x) and copies elements. In Kotlin's `kotlin.collections.ArrayDeque`, uses a circular buffer internally.

### Real-life analogy
- **Array**: Like a fixed-size parking lot with numbered spaces. Each car must park in its designated spot, no changes to total capacity.

- **ArrayList**: Like a growing bookshelf where you can add or remove books, and the bookshelf can expand or shrink as needed.

### Time and space complexity for every major operation

**Array (fixed-size):**
- Access: O(1) – direct memory addressing
- Insert at beginning: O(n) – shift all elements
- Insert at end: O(1) if not full, otherwise requires creating new array
- Insert at middle: O(n) – shift elements
- Delete: O(n) if deleting from middle or beginning
- Search (linear): O(n) – check each element
- Space: O(n) – stores n elements

**ArrayList:**
- Access: O(1) – direct index access
- Insert at end: O(1) amortized, O(n) worst case (array resize)
- Insert at beginning: O(n) – shift all elements
- Insert at middle: O(n) – shift elements
- Delete: O(n) – shift elements
- Search (linear): O(n) – check each element
- Space: O(n) – stores n elements with potential extra capacity

### Common mistakes and edge cases
- **ArrayIndexOutOfBoundsException**: Accessing index outside array bounds
- **Off-by-one errors**: Starting from 0 vs 1, ending conditions
- **Array resizing**: Thinking ArrayList is truly dynamic when it has amortized costs
- **Primitive vs object**: Arrays store primitives, ArrayList stores objects (boxing overhead)
- **Memory efficiency**: Using full arrays when exact size is known
- **Equality**: `Arrays.equals()` vs `Arrays.deepEquals()` for multi-dimensional

### When to use it in interviews
- Perfect for: frequency counting, sliding window, two pointers, prefix sums, DP initialization
- Good for: storing sequences where order matters
- Choose Array when: exact size is known, memory efficiency is critical, no resizing needed
- Choose ArrayList when: size is dynamic, need dynamic growth, ease of use important

## 2. Important Patterns

### Pattern 1: Two Pointers (Slow and Fast)
**Example**: Move all zeros to the end
```java
// Initialize slow pointer for next non-zero position
slow = 0
for (fast = 0; fast < n; fast++) {
    if (nums[fast] != 0) {
        swap nums[slow], nums[fast]
        slow++
    }
}
```
**Brute force**: Nested loops to find zeros and non-zeros, swap manually
**Better**: Two pointers approach above
**Optimal**: Same as better - O(n) time, O(1) space

### Pattern 2: Two Pointers (Left and Right)
**Example**: Container With Most Water
```java
left = 0, right = n-1
while (left < right) {
    area = width * min(height[left], height[right])
    if (height[left] < height[right]) left++
    else right--
}
```
**Brute force**: Check all pairs (O(n²))
**Better**: Two pointers approach (O(n) with pointer movement)
**Optimal**: Same as better - smart pointer movement guarantees finding max

### Pattern 3: Prefix Sum
**Example**: Subarray Sum Equals K
```java
// Build prefix sum array
calc prefix[i] = sum of elements from 0 to i-1
// Use hash map to find prefix[j] - prefix[i] = k
```
**Brute force**: Check all subarrays (O(n²))
**Better**: Prefix sum with hash map (O(n))
**Optimal**: Same as better - O(n) time, O(n) space

### Pattern 4: Binary Search on Sorted Arrays
**Example**: Search in Rotated Sorted Array
```java
find pivot where nums[mid] > nums[mid+1]  
// First binary search to find rotation point
// Second binary search in appropriate half
```
**Brute force**: Linear search O(n)
**Better**: Modified binary search O(log n)
**Optimal**: Same as better - two binary searches

### Pattern 5: Sliding Window
**Example**: Maximum Average Subarray
```java
// Expand window: add next element
// Contract window: remove element from beginning
// Track max while maintaining window size k
```
**Brute force**: Check all subarrays of size k (O(n*k))
**Better**: Sliding window with running sum (O(n))
**Optimal**: Same as better - maintain sum as window moves

## 3. Dry Run

**Problem**: Move Zeroes
**Input**: [0, 1, 0, 3, 12]
**Expected Output**: [1, 3, 12, 0, 0]

**Algorithm Step-by-Step**:
```
Initial state: [0, 1, 0, 3, 12], slow = 0, fast = 0

fast = 0: nums[0] == 0 → skip (slow stays at 0)
fast = 1: nums[1] == 1 → swap nums[0] and nums[1], slow = 1
Array: [1, 0, 0, 3, 12]

fast = 2: nums[2] == 0 → skip (slow stays at 1)
fast = 3: nums[3] == 3 → swap nums[1] and nums[3], slow = 2
Array: [1, 3, 0, 0, 12]

fast = 4: nums[4] == 12 − swap nums[2] and nums[4], slow = 3
Array: [1, 3, 12, 0, 0]

Done: slow reached end, non-zero elements moved to front
```

**Variable Tracking**:
- `slow`: Always points to next position to place non-zero element
- `fast`: Always scans ahead looking for non-zero elements
- Both pointers only move forward, never backward
- Array is modified in-place
- Each element is visited exactly once by `fast` pointer

## 4. Code Template

**Time Complexity**: O(n) where n is the number of elements
**Space Complexity**: O(1) – in-place modification

```kotlin
package arrays_and_arraylist

/**
 * LeetCode: https://leetcode.com/problems/square-of-sorted-array/
 * Difficulty: Easy
 * Core pattern: Two pointers (both ends)
 */
fun sortedSquares(nums: IntArray): IntArray {
    // Initialize pointers at both ends
    var left = 0
    var right = nums.size - 1
    var writePos = right  // Position to write result
    
    // Create result array
    val result = IntArray(nums.size)
    
    // Process from largest to smallest
    while (left <= right) {
        // Get squares of both ends
        val leftSquare = nums[left] * nums[left]
        val rightSquare = nums[right] * nums[right]
        
        // Place larger square at write position
        if (leftSquare > rightSquare) {
            result[writePos] = leftSquare
            left++
        } else {
            result[writePos] = rightSquare
            right--
        }
        
        writePos--
    }
    
    return result
}

// Helper functions (if needed)
private fun square(x: Int): Int = x * x
```

**Line-by-line explanation**:
- Lines 8-11: Initialize pointers for two-pointer approach
- Line 12: Create result array of same size as input
- Line 15: Main loop continues while left pointer <= right pointer
- Lines 18-19: Calculate squares of both ends
- Lines 22-25: Compare squares and place larger at write position
- Lines 28-29: Move appropriate pointer and decrement write position

**Why this works**:
- Input array is sorted (including negatives)
- Squares are sorted by absolute value
- We can fill result from end to beginning
- Two pointers compare which square is larger at each step

## 5. Practice Problems

### Easy Problem 1: Squares of Sorted Array
- **LeetCode**: https://leetcode.com/problems/square-of-sorted-array/
- **Difficulty**: Easy
- **Core pattern**: Two pointers (both ends)
- **Approach hint**: Use two pointers at both ends, compare absolute values, fill result from end
- **Expected time and space complexity**: O(n) time, O(n) space

### Easy Problem 2: Move Zeroes
- **LeetCode**: https://leetcode.com/problems/move-zeroes/
- **Difficulty**: Easy
- **Core pattern**: Two pointers (slow and fast)
- **Approach hint**: Use slow pointer for next position, fast pointer for scanning
- **Expected time and space complexity**: O(n) time, O(1) space

### Medium Problem 1: Container With Most Water
- **LeetCode**: https://leetcode.com/problems/container-with-most-water/
- **Difficulty**: Medium
- **Core pattern**: Two pointers (left and right)
- **Approach hint**: Start with widest container, try to find taller walls
- **Expected time and space complexity**: O(n) time, O(1) space

### Medium Problem 2: Best Time to Buy and Sell Stock
- **LeetCode**: https://leetcode.com/problems/best-time-to-buy-sell-stock/
- **Difficulty**: Medium
- **Core pattern**: Single pass tracking min price
- **Approach hint**: Track minimum seen so far, calculate profit at each step
- **Expected time and space complexity**: O(n) time, O(1) space

### Hard Problem 1: Trapping Rain Water
- **LeetCode**: https://leetcode.com/problems/trapping-rain-water/
- **Difficulty**: Hard
- **Core pattern**: Two pointers with height comparison
- **Approach hint**: Water trapped depends on min of max left and right heights
- **Expected time and space complexity**: O(n) time, O(1) space

### Hard Problem 2: Maximum Product Subarray
- **LeetCode**: https://leetcode.com/problems/maximum-product-subarray/
- **Difficulty**: Hard
- **Core pattern**: Dynamic programming with tracking both max and min
- **Approach hint**: Keep track of both maximum and minimum products (due to negatives)
- **Expected time and space complexity**: O(n) time, O(1) space