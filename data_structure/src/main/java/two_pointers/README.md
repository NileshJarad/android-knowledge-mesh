# Two Pointers

## Concept Explanation

### What are Two Pointers?
Two pointers is an algorithm technique using two indices/variables that move toward each other (or in the same direction). It's one of the most common interview patterns for array problems, especially when working with sorted arrays.

**Common variations:**
1. **Two pointers moving from both ends inward** — left and right pointers converge
2. **Two pointers moving in same direction** — can be inside one array or two arrays
3. **Fast and slow pointer** — one moves faster (like Floyd's cycle detection)

### How It Works
- Start pointers at opposite ends of a data structure (sorted array, linked list, etc.)
- Compare elements at both pointers
- Move the pointer pointing to the "wrong" side inward
- Continue until pointers meet or satisfy condition

**Key insight**: Moving pointers inward skips O(N²) nested loops. Sort first if needed, then two pointers give O(N) or O(N log N).

### Real-Life Analogy
Imagine you're choosing **a pizza** by picking crust pieces from both ends of the box:
- Crust piece at far left → label as option A
- Crust piece at far right → label as option B  
- There's only **one correct combination**
- We move the pointers toward each other to find it efficiently instead of checking all combinations

### Time and Space Complexity

| Operation | Time | Space | Notes |
|-----------|-----|-------|-------|
| Basic two pointer scan | O(N) | O(1) | Single pass with two indices |
| Sorted array + two pointers | O(N) | O(1) | Sort takes O(N log N), scan is O(N) |
| Find pair with sum X (sorted) | O(N) | O(1) | Nested inner loop but overall O(N) |
| Find median of two sorted arrays | O(log(min(M,N))) | O(1) | Binary search approach |

### Common Mistakes and Edge Cases

1. **Forgetting to sort**: Don't apply two pointers on unsorted arrays without sorting first
2. **Off-by-one errors**: `left <= right` vs `left < right` loop condition (off by one!)
3. **Wrong movement direction**: Pointer should move toward where condition is satisfied, not always inward
4. **Dealing with duplicates**: When target has multiple valid answers, use `<=` vs `<` carefully
5. **Integer overflow**: `arr[i] + arr[j]` can overflow; use `Long` or compute incrementally
6. **Fixed two arrays**: Pointer 1 starts at arr1[0], pointer 2 starts at arr2[last] since larger index means larger value in sorted arrays
7. **Infinite loops**: Ensure pointers eventually converge (e.g., while `left < right`)
8. **Returning indices vs values**: Understand whether problem wants indices or values returned
9. **Empty or single element edge cases**: Always handle these explicitly

### When to Use Two Pointers in Interviews

| Scenario | Approach | Why |
|----------|----------|-----|
| Sorted array, find pair with sum | Two pointers | O(N) after sort |
| Subarray / substring problems | Sliding window variant | Single pass optimization |
| Merge two sorted lists | Two pointers | Linear merge like merge-sort |
| Reverse linked list | Two pointers (fast+slow or slow+tail) | In-place O(N) reversal |
| Container most water | Two pointers (ends inward) | Minimize distance × min height |
| Three sum (sorted) | Two pointers with fixed element | O(N²) total after sort |

## Important Patterns

### 1. Pair Finding in Sorted Array
- **Brute**: Check all pairs → O(N²)
- **Better**: Sort then two pointers → O(N log N)
- **Optimal**: Already sorted, two pointers → O(N)
  - Left pointer starts at begin, right at end
  - If sum < target: advance left
  - If sum > target: advance right
  - If sum == target: record answer, advance both

### 2. Container With Most Water
- **Brute**: For each pair compute area → O(N²)
- **Better**: Sort two pointers, same as above → O(N log N)
- **Optimal**: Sort then two pointers → O(N log N)
  - Start both at ends, swap toward shorter height

### 3. Median of Two Sorted Arrays
- **Brute**: Concatenate and sort → O((N+M) log(N+M))
- **Better**: Sort each separately then merge → O(N log N + M log M)
- **Optimal**: Binary search on partition points → O(log(min(N,M)))
  - Partition both arrays so left half has exactly half elements
  - Use partition index i for arr1 and j = (N+M)/2 - i for arr2

### 4. Merge Two Sorted Lists
- Directly reuse merge step from merge sort → O(N) time, O(1) space (if reusing nodes)
- Or create new list → O(N) time, O(N) space

### 5. Reverse Linked List
- Use slow+fast pointers or tail+slow, swap values, terminate at middle
- Or iterative: iterate forward keeping track of previous node
- Recursive: reverse tail backward

## Dry Run

### Example Problem: Two Sum (sorted arrays version)

**Input**: `nums = [1, 2, 3, 4], target = 6` (already sorted)

**Two pointer dry run:**
```
left=0 (val=1), right=3 (val=4): sum=5 < 6 → left++
left=1 (val=2), right=3 (val=4): sum=6 == 6 → found! return [1,3]
```

Wait — original Two Sum needs unsorted input too. Let me also show non-sorted case:

**Input**: `nums = [2, 7, 11, 15], target = 9` (unsorted)

```
Sort: [2, 7, 11, 15]
left=0 (val=2), right=3 (val=15): sum=17 > 9 → right--
right=2 (val=11): sum=13 > 9 → right--
right=1 (val=7): sum=9 == 9 → found! (but original index lost)
```
Note: losing original index is the reason HashMap approach is often preferred for Two Sum.

## Code Template

```kotlin
package two_pointers

/**
 * Template for two pointer problems in Kotlin
 */
fun solution(arr: IntArray, n: Int, target: Int): Any {
    var left = 0
    var right = n - 1
    // TODO: Implement algorithm
}
```

### Complexity Analysis (Template)
- **Time**: O(N) for single pass after sorting
- **Space**: O(1) auxiliary space (in-place comparison)

## Practice Problems

### Easy Problems

#### 1. **Two Sum II - Input Array Is Sorted**
- **LeetCode**: https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/
- **Difficulty**: Easy
- **Core pattern**: Two pointers (left at start, right at end, converge)
- **Approach hint**: Single pass with left/right pointers. If sum < target, move left rightward. If sum > target, move right leftward. Stop when found or converged. Handle duplicates carefully.
- **Expected time**: O(N), **space**: O(1)

#### 2. **Move Zeroes**
- **LeetCode**: https://leetcode.com/problems/move-zeroes/
- **Difficulty**: Easy
- **Core pattern**: Two pointers (slow and fast)
- **Approach hint**: Slow tracks position for non-zero elements. Fast scans, swaps non-zeros to slow position when encountered. Result length is slow count.
- **Expected time**: O(N), **space**: O(1)

### Medium Problems

#### 3. **Container With Most Water**
- **LeetCode**: https://leetcode.com/problems/container-with-most-water/
- **Difficulty**: Medium
- **Core pattern**: Two pointers (start at ends, swap shorter line inward)
- **Approach hint**: Place pointers at both ends of heights array. Area = min(height[left], height[right]) × (right - left). Move the pointer pointing to shorter line inward since this might increase area.
- **Expected time**: O(N), **space**: O(1)

#### 4. **Maximum Product of Splitting the Array into Three Parts**
- **LeetCode**: https://leetcode.com/problems/maximum-product-of-splitting-the-array-into-three-parts/ (check exact match)
- **Difficulty**: Medium
- **Core pattern**: Sliding window prefix sums / three-pointer partition
- **Approach hint**: For any position i, max product = (maxPrefix[i]) × (maxSuffix[i+1]) × (suffixSum - prefixSum). Compute prefix and suffix arrays in O(N), then find max over all positions.
- **Expected time**: O(N), **space**: O(N)

### Hard Problems

#### 5. **Median of Two Sorted Arrays**
- **LeetCode**: https://leetcode.com/problems/median-of-two-sorted-arrays/
- **Difficulty**: Hard
- **Core pattern**: Binary search on partition points (not two pointers!)
- **Approach hint**: Binary search for partition point i in smaller array such that elements on both sides fall in left/right halves. Adjust based on comparison of maxLeft vs minRight elements across both arrays. Result is avg of two middle values or single middle value depending on even/odd total count.
- **Expected time**: O(log(min(N,M))), **space**: O(1)

#### 6. **Split Array Largest Sum**
- **LeetCode**: https://leetcode.com/problems/split-array-largest-sum/
- **Difficulty**: Hard
- **Core pattern**: Binary search + greedy assignment to two subarrays
- **Approach hint**: Binary search on the answer (max sum per subarray). For mid value, greedily assign each remaining element to current subarray if it fits (≤ mid); otherwise start new subarray. Count subarrays needed. If count ≤ k, try smaller max; else try larger.
- **Expected time**: O(N log(SUM)), **space**: O(1)

---
*End of Two Pointers topic*