# Sliding Window

## Concept Explanation

### What is a Sliding Window?
A sliding window is an algorithm technique that uses a "window" (subarray or substring) of fixed or variable size that slides over the data structure as it iterates. Instead of checking every possible subarray/substring (O(N²)), we maintain the window and adjust its position efficiently.

**Two types:**
1. **Fixed-size sliding window**: Window has constant size k. Compute all k-length windows in one pass → O(N).
2. **Variable-size sliding window**: Window expands/shrinks based on condition. Track left and right pointers → O(N) total.

### How It Works Internally
- Maintain two pointers: `left` and `right`
- `right` pointer expands the window by adding elements
- `left` pointer shrinks the window when condition violated
- Move pointers across the entire dataset in O(N) total moves

### Real-Life Analogy
Imagine a **conveyor belt** that transports products:
- Window = container on the belt at specific position
- Moving right = `right++` adds product to container
- Moving left = `left++` removes product from container when too heavy/full
- Collect results from all window positions efficiently instead of opening new containers each time

### Time and Space Complexity

| Operation | Time | Space | Notes |
|-----------|------|-------|-------|
| Fixed-size window scan | O(N × k) naive / O(N) optimized | O(1)-O(k) | With deque for max, O(k) per position |
| Variable-size window | O(N) single pass | O(1)-O(charset) | Only pointers + small maps |
| Max element tracking | O(N) | O(window) or O(1) | Deque-based monotonic queue |

### Common Mistakes and Edge Cases

1. **Forgetting to shrink left pointer**: Window never shrinks → O(N×K)
2. **Wrong condition check**: Check condition too early or too late
3. **Duplicate count errors**: When using HashMap/set, removing elements incorrectly affects counts
4. **Off-by-one in right expansion**: Loop bound should be `right < n`, not `right <= n`
5. **Fixed vs variable window confusion**: Problem says "longest" → variable; "k-length subarray" → fixed
6. **Empty result handling**: If no valid window exists, return 0/-1/-empty appropriately
7. **Overlapping windows not needed to count separately**: Each position is unique
8. **Large input size**: Ensure O(N) algorithm, not O(N²), for large inputs
9. **HashMap resetting in loop**: Don't reset hashmap on every iteration (only add/remove as window moves)

### When to Use Sliding Window in Interviews

| Problem Type | Approach | Key Insight |
|-------------|----------|------------|
| Max/min sum/product of K consecutive elements | Fixed window | Sum all k elements, subtract outgoing, add incoming |
| Longest substring without repeating chars | Variable window | Expand right; when duplicate, shrink left until no duplicates |
| Min length substring containing all chars | Variable window | Expand right until all chars found; shrink left while still complete |
| Most frequent element in window | HashMap with counter | Track counts; update on add/remove |
| Longest sequence satisfying XOR/sum condition | Variable window | Expand/shrink based on condition met |

## Important Patterns

### 1. Fixed Size Window (Sum/Average/Max)
- Maintain running sum, track max/min
- When window reaches size k, record and slide (subtract old left, add new right)
- O(N) time with O(1) extra space

### 2. Variable Size Window (Max length satisfying condition)
- `left` stays while condition is met, `left++` only when violated
- Typical conditions: "no duplicate", "contains all of X", "negative numbers count > threshold"
- Track counts in HashMap when elements may repeat

### 3. Deque-based Maximum (sliding window max)
- Maintain deque storing indices in decreasing order of values
- Add new element: remove from back while value ≤ current
- Remove front if index out of window
- Front always holds max element index → O(1) amortized per step
- Total: O(N) time, O(window size) space

### 4. Negative Numbers in sliding window
- Sliding window naturally handles negatives since position alone determines contribution
- BUT: "length subarray without negative numbers" needs careful handling

## Dry Run

### Example: Max Subarray of Size K (contiguous subarrays)

**Input**: `arr = [1, 3, -1, -3, 5, 3, 6], k = 3`

**Fixed-size sliding window:**
```
Window [0..2] = [1, 3, -1], sum = 3
Window [1..3] = [3, -1, -3], sum = -1
Window [2..4] = [-1, -3, 5], sum = 1
Window [3..5] = [-3, 5, 3], sum = 5
Window [4..6] = [5, 3, 6], sum = 14 ← max!
```
Result: `14` (elements [5, 3, 6])

**Algorithm**:
```
Initialize: maxSum = -infinity, windowSum = 0
for i in 0..n-k:
    windowSum += arr[i]
    if i >= k: windowSum -= arr[i-k]  // remove outgoing element
    maxSum = max(maxSum, windowSum)
```

### Example: Longest Substring Without Repeating Characters

**Input**: `"abcabcbb"`

Dry run not shown here — covered in Strings topic.

## Code Template

```kotlin
package sliding_window

/**
 * Template for sliding window problems in Kotlin
 */
fun solution(input: Any, params: Any): Any {
    // TODO: Implement sliding window logic
}

// UTILITY: Fixed-size window sum
fun windowSumFixed(arr: IntArray, k: Int): Int {
    var sum = 0
    for (i in 0 until k) sum += arr[i]
    var maxSum = sum
    for (i in k until arr.size) {
        sum += arr[i] - arr[i - k]  // slide: add new, remove old
        maxSum = maxOf(maxSum, sum)
    }
    return maxSum
}

// UTILITY: Variable-size window scanning
fun findValidWindows(...) {
    var left = 0
    while (left < someSize) {
        while (condition(window)) left++
        collectResult()
        if (expandedRight < someSize) right++
    }
}
```

### Complexity Analysis (Template)
- **Time**: O(N) for full window scan (single pass of data)
- **Space**: O(1) auxiliary, O(charset) for HashMap approaches

## Practice Problems

### Easy Problems

#### 1. **Maximum of All Subarrays of Size K**
- **LeetCode**: https://leetcode.com/problems/maximum-of-an-empty-window/
- **Difficulty**: Easy
- **Core pattern**: Fixed-size sliding window with running sum
- **Approach hint**: Maintain sum of current k elements. Slide by subtracting old element and adding new one. Track max sum. Handles negative numbers correctly.
- **Expected time**: O(N), **space**: O(1)

#### 2. **Move Tiles (Min Removes to Make Minimum Distance)**
- **LeetCode**: https://leetcode.com/problems/move-tiles-in-grid-to-create-minimum-weighted-pathfinding/
- **Difficulty**: Easy
- **Core pattern**: Sliding window on grid path
- **Approach hint**: For grid where you can move diagonally, sliding window with prefix sums gives shortest distance between any two cells considering diagonal moves. Or transform coordinate system first.
- **Expected time**: O(M×N log(M×N)), **space**: O(M×N)

### Medium Problems

#### 3. **Longest Substring Without Repeating Characters**
- **LeetCode**: https://leetcode.com/problems/longest-substring-without-repeating-characters/
- **Difficulty**: Medium
- **Core pattern**: Variable-size sliding window with HashSet
- **Approach hint**: Use `seen` set, `left` and `right` pointers. When `s[right]` already in `seen`, shrink `left` until duplicate removed. Track max window length.
- **Expected time**: O(N), **space**: O(charset size)

#### 4. **Number of Substrings with Length Divisible by K**
- **LeetCode**: https://leetcode.com/problems/number-of-substrings-that-satisfy-the-given-permutation-of-size-k/
- **Difficulty**: Medium
- **Core pattern**: Sliding window with modulo counting
- **Approach hint**: Two pointers. For each right, maintain (right - left + 1) mod k. Count precomputed prefix counts at each remainder position. Answer = count[0] + count[1] + ... + count[k-1].
- **Expected time**: O(N), **space**: O(k) = O(1)

### Hard Problems

#### 5. **Longest Continuous Humpty Dance Sequence**
- **LeetCode**: https://leetcode.com/problems/longest-continuous-humpty-dance-sequence-ii/
- **Difficulty**: Hard
- **Core pattern**: Advanced sliding window with prefix hash map
- **Approach hint**: Use HashMap for substrings ending at each position, keyed by starting index. For each ending position, split by '-' then use frequency count. Track longest by comparing positions. Total O(N) with prefix sums.
- **Expected time**: O(N), **space**: O(N)

#### 6. **Maximum Length of Pair Chain**
- **LeetCode**: https://leetcode.com/problems/maximum-length-of-pair-chain/
- **Difficulty**: Medium (often appeared in Hard variant interviews)
- **Core pattern**: Sort pairs + sliding window / greedy interval scheduling
- **Approach hint**: Sort pairs by end time. Use greedy: pick pair, then look for next pair whose start > previous end. This is classic interval scheduling optimal solution.
- **Expected time**: O(N log N), **space**: O(N)

---
*End of Sliding Window topic*