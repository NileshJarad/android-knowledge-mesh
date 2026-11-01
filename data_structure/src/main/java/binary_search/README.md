# Binary Search

## Concept Explanation

### What is Binary Search?
Binary search is an efficient O(log N) algorithm for finding a target value within a sorted array. It works by repeatedly dividing the search space in half, eliminating half of remaining elements each iteration.

**Core principle**: If array is sorted, and we compare target to middle element:
- If `target == middle`: found!
- If `target < middle`: search left half only (target must be in left half since sorted ascending)
- If `target > middle`: search right half only

### Key Variations in Interviews
1. **Standard binary search**: Find target position in sorted array
2. **Find first / last occurrence**: Use `left < right` with extra comparison
3. **Find floor / ceiling**: Find largest value ≤ target (or smallest ≥ target)
4. **Find rotation point**: Find pivot in rotated sorted array
5. **Search in bitonic array**: Find peak, then search both halves
6. **Search in bitonic / valley**: Similar to above
7. **First and last position**: Modified binary search
8. **Minimize maximum**: For "minimize max sum" problems
9. **Custom comparator search**: Searching with custom ordering (e.g., digit manipulation)

### Real-Life Analogy
Imagine you're looking for a specific **book on a library shelf** where books are sorted by height:
- You split the shelf in half mentally
- Compare your desired book's title/height with the middle book
- Too tall → look at top half; too short → look at bottom half
- Repeat until found or shelves exhausted
- Each comparison eliminates ~half the shelf → O(log N) comparisons vs O(N) linear scan

### Time and Space Complexity

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| Single search | O(log N) | O(1) | Iterative or recursive |
| Find first / last occurrence | O(log N) | O(1) or O(log N) recursion | Invariant maintained differently |
| Rotated array search | O(log N) | O(1) | Even-odd indexing trick |
| Floor / ceiling | O(log N) | O(1) | Binary search on answer value |

### Common Mistakes and Edge Cases

1. **Off-by-one errors**: Using `<=` vs `<` in loop condition leads to infinite loops or missing elements
2. **Wrong search direction**: When `target < arr[mid]`, should move `right--` not `left++`
3. **Integer division truncation**: Midpoint calculation `(left + right) / 2` can overflow; use `left + (right - left) / 2`
4. **Off-by-one with duplicates**: Finding first occurrence requires careful handling
5. **Modifying array during search**: Array must be sorted before binary search (unless problem states otherwise)
6. **Zero-length arrays**: Check `arr.isEmpty()` before binary search
7. **Target outside range**: Return -1/-2 if target less than min or greater than max
8. **Sorted descending**: Need reverse comparator or sort first
9. **Floating point precision**: Be careful with floating point comparisons
10. **Infinite loop potential**: Ensure `left` and `right` always converge: after comparison, move one pointer only

### When to Use Binary Search in Interviews

| Scenario | Approach | Why |
|----------|----------|-----|
| Sorted array, find exact match | Standard BS | O(log N) guaranteed |
| Sorted array, find first/last occurrence | Modified BS | Same complexity with extra comparisons |
| Sorted array, find floor value | BS on values | Binary search over range [0..n-1] |
| Rotated sorted array | Modified BS | Use even/odd index comparison to find pivot |
| "Make array good" / monotonic condition | BS on answer | Search optimal value of n |
| Find peak in bitonic array | Two modified BSs | Find peak then search both sides |
| "Minimum window" type problems | BS on max allowed sum/value | Greedy feasibility check at each candidate |

## Important Patterns

### 1. Basic Binary Search
```
left = 0, right = n-1
while left <= right:
    mid = (left + right) / 2
    compare target with arr[mid]
    move pointers accordingly
```
Standard pattern, but watch out for integer overflow and equal elements.

### 2. Find First Occurrence (Leftmost)
- Maintain `left < right` (not `<=`)
- After finding match, keep searching left side with `right = mid` instead of `right--`
- Loop exits when `left > right` → return `left - 1` or use while with explicit tracking

### 3. Find Last Occurrence (Rightmost)
- Maintain `left < right`
- After finding match, keep searching right side with `left = mid + 1`
- Return `right` when loop ends

### 4. Rotation Detection (Rotated Sorted Array)
- Use even/odd index comparison:
  - If `arr[mid] > arr[right]`: left half is unsorted, search right
  - If `arr[mid] < arr[right]`: right half is unsorted, search left
- Found rotation point when `arr[mid] > arr[mid+1]`

### 5. Bitonic / Valley Search
- Find peak using binary search (compare mid to neighbors, one side must decrease)
- Then search left and right halves independently
- Each half is sorted → standard binary search on either side

### 6. Binary Search on Answer (Value-based)
- Determine search space: `lo = 0`, `hi = someMax`
- At each `mid`, check if `f(mid)` satisfies constraint
  - `f(mid)` → feasible if true, infeasible if false
- Binary search on value space, not array space
- This gives O(log(maxValue)) even when no sorted data structure exists

### 7. Fenwick Tree / BIT Optimization
- Not pure binary search but similar divide-and-conquer concept
- Often used as building block in more complex problems

## Dry Run

### Example: First Position of Target in Sorted Array

**Input**: `nums = [1, 2, 2, 3, 4, 4, 5], target = 3`

**Dry run:**
```
Step 1: left=0, right=6, mid=3 → arr[3]=4 > 3 → right=2
Step 2: left=0, right=2, mid=1 → arr[1]=2 < 3 → left=2
Step 3: left=2, right=2, mid=2 → arr[2]=2 < 3 → left=3
Step 4: left=3, right=2 → left > right, exit
Return left=3 → correct! arr[3]=3
```

Note: with `left <= right` loop, mid would still find position but might stop too early. Using `left < right` with post-loop check is cleaner.

For first occurrence specifically, continue searching left after hit:
```
...same but after mid match: right = mid, repeat
```

## Code Template

```kotlin
package binary_search

/**
 * Template for binary search problems in Kotlin
 */
fun solution(arr: IntArray, target: Int): Int {
    // TODO: Implement binary search
}

// Standard first occurrence template
fun firstOccurrence(arr: IntArray, target: Int): Int {
    var left = 0
    var right = arr.size - 1
    while (left < right) {
        val mid = left + (right - left) / 2
        when {
            arr[mid] < target -> left = mid + 1
            else -> right = mid  // found possible position, keep searching left
        }
    }
    return if (left < arr.size && arr[left] == target) left else -1
}

// Standard last occurrence template
fun lastOccurrence(arr: IntArray, target: Int): Int {
    var left = 0
    var right = arr.size - 1
    while (left < right) {
        val mid = left + (right - left) / 2
        when {
            arr[mid] <= target -> left = mid + 1
            else -> right = mid
        }
    }
    return if (right >= 0 && arr[right] == target) right else -1
}
```

### Complexity Analysis (Template)
- **Time**: O(log N) per search
- **Space**: O(1) iterative, O(log N) recursive stack

## Practice Problems

### Easy Problems

#### 1. **Search Insert Position**
- **LeetCode**: https://leetcode.com/problems/search-insert-position/
- **Difficulty**: Easy
- **Core pattern**: Standard binary search returning position or insertion point
- **Approach hint**: If found return its index, otherwise return position where it would be inserted to maintain order. Think about edge cases: insert at beginning, end, middle.
- **Expected time**: O(log N), **space**: O(1)

#### 2. **Sqrt(x)**
- **LeetCode**: https://leetcode.com/problems/sqrtx/description/
- **Difficulty**: Easy
- **Core pattern**: Binary search on answer value (not array!)
- **Approach hint**: Search space is [0, x]. At mid, compute mid² and check against x. If mid² ≤ x, search higher (left=mid+1, right=mid); else search lower. Stop when left > right.
- **Expected time**: O(log x), **space**: O(1)

### Medium Problems

#### 3. **Find First and Last Position of Element in Sorted Array**
- **LeetCode**: https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/
- **Difficulty**: Medium
- **Core pattern**: Two modified binary searches (first occurrence + last occurrence)
- **Approach hint**: Run two separate searches: one returns leftmost, one returns rightmost. Both modified binary search with different pointer movements.
- **Expected time**: O(log N), **space**: O(1)

#### 4. **Search in Rotated Sorted Array**
- **LeetCode**: https://leetcode.com/problems/search-in-rotated-sorted-array/
- **Difficulty**: Medium
- **Core pattern**: Binary search with half-array selection based on which half is sorted
- **Approach hint**: At each step, determine if left half or right half is sorted (using `arr[mid]` vs `arr[left]` comparison). Then check if target lies within the sorted half. Adjust pointers accordingly. Handle edge cases: mid == left, duplicate values.
- **Expected time**: O(log N), **space**: O(1)

### Hard Problems

#### 5. **Find Peak Element**
- **LeetCode**: https://leetcode.com/problems/find-peak-element/
- **Difficulty**: Medium (often appeared in Hard variant interviews)
- **Core pattern**: Binary search comparing mid with right neighbor
- **Approach hint**: Compare mid with mid+1. If mid < mid+1, peak must be to the right; else peak could be at mid itself or left. Continue binary search. Edge cases: array size 1, all equal elements, single changing element.
- **Expected time**: O(log N), **space**: O(1)

#### 6. **Find Minimum in Rotated Sorted Array II**
- **LeetCode**: https://leetcode.com/problems/find-minimum-in-rotated-sorted-array-ii/
- **Difficulty**: Medium (harder than non-duplicate version)
- **Core pattern**: Modified binary search accounting for duplicates
- **Approach hint**: When `arr[mid] == arr[right]`, cannot determine direction; decrement right (safe because mid != right after proper computation). All equal values → return middle directly.
- **Expected time**: O(N) worst case / O(log N) average, **space**: O(1)

---
*End of Binary Search topic*