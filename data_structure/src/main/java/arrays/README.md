# Arrays and ArrayList

## Concept Explanation

### What is an Array?
An array is a fundamental data structure that stores a fixed-size sequential collection of elements of the same type. Arrays store data in contiguous memory locations, which means each element is placed right next to each other in memory. This allows for index-based access where any element can be accessed directly in O(1) time using its index position.

Key characteristics:
- **Fixed size**: Traditional arrays have a fixed size determined at creation time (though ArrayList in Java/Kotlin provides dynamic resizing)
- **Homogeneous**: All elements must be of the same data type
- **Index-based access**: Elements accessed via integer index starting from 0
- **Contiguous memory**: Elements stored in sequential memory locations

### What is an ArrayList (or ArrayList)?
ArrayList is a resizable array implementation available in Java's `java.util.ArrayList` and Kotlin's `kotlin.ArrayList` (or `mutableListOf()`). Unlike traditional arrays:
- **Dynamic size**: Can grow or shrink as needed
- **Automatic resizing**: Internal array is resized when elements are added beyond current capacity
- **Type-safe**: Stores elements as objects (in Java) or specific type (in Kotlin)
- **Rich API**: Provides methods like `add()`, `remove()`, `set()`, `get()`, `size()`, etc.

### How Arrays Work Internally
- **Memory layout**: Base address + (index × element_size)
- **Address calculation**: `address = base_address + (index × stride)`
- **No bounds checking** (in C/C++): Accessing out-of-bounds can lead to undefined behavior
- **Bounds checking** (in Java/Kotlin): ArrayIndexOutOfBoundsException thrown for invalid indices

### Real-Life Analogy
Think of an array like a row of **mailboxes** on a street:
- Each mailbox has a unique number (index)
- Mailboxes are placed at equal distances (contiguous memory)
- To get the 3rd piece of mail, you go directly to mailbox #3 (O(1) access)
- Adding new mail when all mailboxes are full requires getting a whole new row of mailboxes (resizing)

### Time and Space Complexity

| Operation | Array | ArrayList |
|-----------|-------|-----------|
| **Access (get)** | O(1) | O(1) |
| **Search** | O(N) | O(N) |
| **Insert (at end)** | O(1) amortized | O(1) amortized |
| **Insert (at position)** | O(N) | O(N) |
| **Delete (at position)** | O(N) | O(N) |
| **Memory** | O(N) fixed | O(N) with capacity overhead |

### Common Mistakes and Edge Cases

1. **ArrayIndexOutOfBoundsException**: Always validate indices before access
   - Valid indices: 0 to length-1
   - Common: accessing index `arr.length` or negative indices

2. **Off-by-one errors**: Loop conditions like `i <= arr.length` vs `i < arr.length`
   - `arr.length` is a field (not a method in Java), `arr.size` in Kotlin

3. **Modifying array while iterating**: Can cause ConcurrentModificationException or skip elements
   - Solution: Use iterator's remove() or iterate over a copy

4. **Confusing length vs size**: 
   - Java array: `arr.length` 
   - Java ArrayList: `list.size()`
   - Kotlin array:  c`array.size` or `array.length`
   - Kotlin List: `list.size`

5. **Assuming sorted order**: Don't assume arrays are sorted unless documented
   - Always check if array is sorted before applying binary search

6. **Integer overflow in size calculations**: `i + j` can overflow for large arrays
   - Use `long` or check bounds before addition

### When to Use Arrays vs ArrayList in Interviews

| Scenario | Use Array | Use ArrayList |
|----------|-----------|---------------|
| **Fixed-size collection** | ✓ | ✗ |
| **Performance-critical, no resizing needed** | ✓ | ✗ |
| **Know size at compile time** | ✓ | ✗ |
| **Need dynamic resizing** | ✗ | ✓ |
| **Frequent add/remove operations** | ✗ | ✓ |
| **Memory constraints are strict** | ✓ | ✗ (overhead) |
| **Returning collections from methods** | ✗ | ✓ |

**Rule of thumb**: Use arrays when size is known and fixed; use ArrayList when you need flexibility and dynamic resizing.

## Important Patterns

### 1. Two Pointers Pattern
Used for problems involving sorted arrays or finding pairs/triplets.

- **Brute force**: Nested loops checking all pairs → O(N²)
- **Better approach**: Sort array first, then use two pointers → O(N log N)
- **Optimal approach**: Two pointers from start and end → O(N) after sorting

### 2. Sliding Window Pattern
Used for finding subarrays/substrings with certain conditions.

- **Fixed-size window**: Subarray of size k, compute sum/max/min
- **Variable-size window**: Expand/shrink based on condition, O(N) total

### 3. Prefix Sum Pattern
Precompute cumulative sums to answer range sum queries in O(1).

- **Brute force**: Recalculate sum for each query → O(1) per query after O(N) preprocessing
- **Better**: Prefix sum array → O(1) per range sum query

### 4. In-Place Reversal
Reverse array elements without using extra space.

- **Approach**: Swap elements from both ends moving toward center
- **Complexity**: O(N) time, O(1) space

### 5. Cyclic Sort
Sort array containing numbers 1..N by placing each number at its correct index.

- **Use case**: Finding missing numbers, duplicates in O(N) time, O(1) space

### 6. Sliding Window Maximum
Find maximum in all subarrays of size k.

- **Brute force**: O(N × k) using nested loops
- **Better**: O(N) using deque data structure

## Dry Run

### Example Problem: Maximum Subarray Sum (Kadane's Algorithm)

**Input**: `[-2, 1, -3, 4, -1, 2, 1, -5, 4]`

**Goal**: Find contiguous subarray with largest sum

**Step-by-step execution**:

| i | arr[i] | runningSum | maxSoFar | Action |
|---|--------|------------|----------|--------|
| - | - | - | - | Initialize |
| 0 | -2 | -2 | -2 | runningSum = arr[0] = -2 |
| 1 | 1 | max(1, -2+1=−1) = 1 | max(-2, 1) = 1 | runningSum becomes 1 (ignore negative prefix) |
| 2 | -3 | max(-3, 1+(-3)=−2) = -2 | max(1, -2) = 1 | runningSum = -2 |
| 3 | 4 | max(4, -2+4=2) = 4 | max(1, 4) = 4 | runningSum = 4 (start new subarray) |
| 4 | -1 | max(-1, 4+(-1)=3) = 3 | max(4, 3) = 4 | runningSum = 3 |
| 5 | 2 | max(2, 3+2=5) = 5 | max(4, 5) = 5 | runningSum = 5 |
| 6 | 1 | max(1, 5+1=6) = 6 | max(5, 6) = 6 | runningSum = 6 |
| 7 | -5 | max(-5, 6+(-5)=1) = 1 | max(6, 1) = 6 | runningSum = 1 |
| 8 | 4 | max(4, 1+4=5) = 5 | max(6, 5) = 6 | runningSum = 5 |

**Final result**: `maxSoFar = 6`

**Subarray**: `[4, -1, 2, 1]` with sum = 6

**Variables state change**:
- `runningSum`: Tracks maximum sum of subarray ending at current position
- `maxSoFar`: Tracks overall maximum seen so far
- Key insight: If `runningSum` becomes negative, start new subarray from current element

### Example Problem: Two Sum

**Input**: `nums = [2, 7, 11, 15]`, `target = 9`

**Goal**: Find indices of two numbers that add up to target

**Brute force approach dry run**:
```
i=0, j=1: 2+7=9 ✓ → return [0,1]
```
Time: O(N²), Space: O(1)

**Better approach (HashMap)**:
```
i=0, num=2, complement=9-2=7, map={2:0}
i=1, num=7, complement=9-7=2, found in map at index 0 → return [0,1]
```
Time: O(N), Space: O(N)

**Optimal approach**: Use HashMap to store (num → index) while iterating once

## Code Template

```kotlin
package arrays

import kotlin.math.max
import kotlin.math.min

/**
 * Template for array problems in Kotlin
 * Language: Kotlin 1.8+ / Java 8+
 * 
 * Common patterns embedded as comments
 */

fun solution(arr: IntArray, n: Int): Int {
    // n = size of array, arr = input array
    // Return type depends on problem
    var result = 0
    
    // TODO: Implement algorithm here
    // Common patterns:
    // - Two pointers: var left = 0; var right = arr.size - 1
    // - Sliding window: maintain window [left, right]
    // - Prefix sum: compute cumulative sums first
    // - Cyclic sort: place elements at correct indices
    // - Binary search on answer
    
    return result
}

// OVERLOADED: For 1D array problems
fun maxSubArraySum(arr: IntArray): Int {
    // Kadane's algorithm template
    var maxSoFar = arr[0]
    var runningSum = arr[0]
    
    for (i in 1 until arr.size) {
        runningSum = maxOf(arr[i], runningSum + arr[i])
        maxSoFar = maxOf(maxSoFar, runningSum)
    }
    return maxSoFar
}

// OVERLOADED: For Two Sum problem
fun twoSum(nums: IntArray, target: Int): Pair<Int, Int> {
    // HashMap approach template
    val numToIndex = mutableMapOf<Int, Int>()
    
    for (i in nums.indices) {
        val complement = target - nums[i]
        if (complement in numToIndex) {
            return Pair(numToIndex[complement]!!, i)
        }
        numToIndex[nums[i]] = i
    }
    throw IllegalArgumentException("No two sum solution")
}

// UTILITY: Print array
fun printArray(arr: IntArray) {
    for (i in arr.indices) {
        print("${arr[i]} ")
    }
    println()
}

// UTILITY: Reverse array in-place
fun reverseArray(arr: IntArray): IntArray {
    var left = 0
    var right = arr.size - 1
    while (left < right) {
        val temp = arr[left]
        arr[left] = arr[right]
        arr[right] = temp
        left++
        right--
    }
    return arr
}
```

### Line-by-Line Explanation

```kotlin
package arrays          // 1: Package declaration matches directory structure

import kotlin.math.max   // 2: Import max function for comparisons
import kotlin.math.min   // 3: Import min function for comparisons

/**
 * Template for array problems in Kotlin
 * Language: Kotlin 1.8+ / Java 8+
 * 
 * Common patterns embedded as comments
 * This serves as a skeleton that you fill in based on the specific problem
 */
fun solution(arr: IntArray, n: Int): Int {   // 4: Function signature - takes IntArray and size
    // n = size of array, arr = input array                          // 5: Parameter documentation
    //return type depends on problem
    var result = 0                                                    // 6: Initialize result variable
    
    // TODO: Implement algorithm here                                  // 7: Placeholder for actual implementation
    // Common patterns:                                              // 8: Comment guiding what patterns to use
    // - Two pointers: var left = 0; var right = arr.size - 1        // 9: Two pointer pattern setup
    // - Sliding window: maintain window [left, right]               // 10: Sliding window pattern
    // - Prefix sum: compute cumulative sums first                   // 11: Prefix sum pattern
    // - Cyclic sort: place elements at correct indices              // 12: Cyclic sort pattern
    // - Binary search on answer                                     // 13: Binary search pattern
    
    return result                                                     // 14: Return the computed result
}

// OVERLOADED: For 1D array problems                                  // 15: Function overload annotation
fun maxSubArraySum(arr: IntArray): Int {                             // 16: Function name and parameter
    // Kadane's algorithm template                                     // 17: Algorithm description comment
    var maxSoFar = arr[0]                                             // 18: Initialize with first element
    var runningSum = arr[0]                                           // 19: Initialize running sum
    
    for (i in 1 until arr.size) {                                    // 20: Loop from second element to end
        runningSum = maxOf(arr[i], runningSum + arr[i])              // 21: Key Kadane logic - either start new or extend
        maxSoFar = maxOf(maxSoFar, runningSum)                        // 22: Update global maximum if running sum is larger
    }
    return maxSoFar                                                    // 23: Return maximum subarray sum
}

// OVERLOADED: For Two Sum problem                                     // 24: Function overload annotation
fun twoSum(nums: IntArray, target: Int): Pair<Int, Int> {             // 25: Function with pair return type
    // HashMap approach template                                       // 26: Algorithm description comment
    val numToIndex = mutableMapOf<Int, Int>()                         // 27: Create mutable map for num→index
    
    for (i in nums.indices) {                                        // 28: Iterate through array indices
        val complement = target - nums[i]                             // 29: Calculate what we need to find
        if (complement in numToIndex) {                               // 30: Check if complement already seen
            return Pair(numToIndex[complement]!!, i)                  // 31: Found! Return indices
        }
        numToIndex[nums[i]] = i                                       // 32: Store current number and its index
    }
    throw IllegalArgumentException("No two sum solution")             // 33: Throw if no solution found
}

// UTILITY: Print array                                                 // 34: Utility function annotation
fun printArray(arr: IntArray) {                                      // 35: Function definition
    for (i in arr.indices) {                                         // 36: Loop through each element
        print("${arr[i]} ")                                          // 37: Print element with space
    }
    println()                                                        // 38: Newline after printing all elements
}

// UTILITY: Reverse array in-place                                       // 39: Utility function annotation
fun reverseArray(arr: IntArray): IntArray {                          // 40: Function with return type
    var left = 0                                                     // 41: Left pointer starts at beginning
    var right = arr.size - 1                                         // 42: Right pointer starts at end
    while (left < right) {                                           // 43: Continue while pointers haven't crossed
        val temp = arr[left]                                         // 44: Store left element temporarily
        arr[left] = arr[right]                                       // 45: Replace left with right element
        arr[right] = temp                                            // 46: Replace right with stored left element
        left++                                                       // 47: Move left pointer rightward
        right--                                                      // 48: Move right pointer leftward
    }
    return arr                                                       // 49: Return modified array
}
```

### Complexity Analysis (Template)
- **Time complexity**: Depends on algorithm chosen (O(N), O(N log N), O(N²), etc.)
- **Space complexity**: 
  - O(1) for in-place algorithms (two pointers, reversal, cyclic sort)
  - O(N) for hash map based approaches (Two Sum, subset problems)
  - O(N) for prefix sum arrays

### When to Use This Template

| Problem Type | Template Pattern | Key Insight |
|-------------|-----------------|-------------|
| Maximum subarray | Kadane's algorithm | Track running sum, reset when negative |
| Two sum | HashMap | Store num→index, check complement |
| Three sum | Fixed + Two pointers | Fix one, use two pointers for rest |
| Rotated array | Modified binary search | Find pivot, then search in half |
| Container with most water | Two pointers | Area = min(height[i],height[j]) × distance |
| Product subarray | Track max/min | Handle zeros and negatives carefully |
| Permutation/check anagram | Sort or count | Sorting O(N log N), counting O(N) |

## Practice Problems

### Easy Problems

#### 1. **Squares of a Sorted Array**
- **LeetCode**: https://leetcode.com/problems/squares-of-a-sorted-array/
- **Difficulty**: Easy
- **Core pattern**: Two pointers (compare from both ends, place larger square at end)
- **Approach hint**: Use two pointers starting at both ends of the sorted array. Compare absolute values, square the larger one, and place it at the end of the result array. Move the pointer inward. Continue until pointers meet.
- **Expected time**: O(N), **space**: O(N) for result array

#### 2. **Remove Duplicates from Sorted Array**
- **LeetCode**: https://leetcode.com/problems/remove-duplicates-from-sorted-array/
- **Difficulty**: Easy
- **Core pattern**: Two pointers (slow and fast)
- **Approach hint**: Use a slow pointer to track the position of unique elements and a fast pointer to scan through the array. When nums[fast] != nums[slow], increment slow and copy nums[fast] to nums[slow]. The new length is slow + 1.
- **Expected time**: O(N), **space**: O(1) (in-place)

### Medium Problems

#### 3. **Container With Most Water**
- **LeetCode**: https://leetcode.com/problems/container-with-most-water/
- **Difficulty**: Medium
- **Core pattern**: Two pointers (start at ends, move shorter line inward)
- **Approach hint**: Place pointers at both ends of the array. The area is determined by the shorter line and the distance between them. Move the pointer pointing to the shorter line inward, as this might lead to a larger area. Continue until pointers meet.
- **Expected time**: O(N), **space**: O(1)

#### 4. **Find Pairs with Given Sum**
- **LeetCode**: https://leetcode.com/problems/problems/ (check for exact match or similar)
- **Difficulty**: Medium
- **Core pattern**: HashMap (store complement) or Two pointers (on sorted array)
- **Approach hint**: For sorted array, use two pointers from start and end. For unsorted array, use HashMap to store visited numbers and check if target - current exists in map.
- **Expected time**: O(N) with hashmap, O(N log N) with sorting + two pointers; **space**: O(N) or O(1)

### Hard Problems

#### 5. **Maximum Subarray**
- **LeetCode**: https://leetcode.com/problems/maximum-subarray/
- **Difficulty**: Hard (categorized as Medium on LeetCode but concepts extend to Hard)
- **Core pattern**: Kadane's algorithm / Prefix sum
- **Approach hint**: Use Kadane's algorithm: maintain running sum and maximum sum seen so far. At each element, decide whether to start a new subarray or extend the existing one (runningSum = max(nums[i], runningSum + nums[i])).
- **Expected time**: O(N), **space**: O(1)

#### 6. **Maximum Product Subarray**
- **LeetCode**: https://leetcode.com/problems/maximum-product-subarray/
- **Difficulty**: Medium (often appeared in Hard variant interviews)
- **Core pattern**: Track both max and min products (handling negatives flips max↔min)
- **Approach hint**: At each position, compute three candidates: current element, current × max so far, current × min so far. The max and min at this position are the max/min of these three. Track overall maximum throughout.
- **Expected time**: O(N), **space**: O(1)

---
*End of Arrays and ArrayList topic*