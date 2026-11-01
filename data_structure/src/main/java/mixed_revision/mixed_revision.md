# Topic 18: Mixed Revision and Mock Interview Problems

## Concept Explanation

### What is Mixed Revision?
Mixed revision is the final stage of DSA preparation where you solve problems that **combine multiple concepts** from previous topics. Unlike topic-focused practice where you know which technique to apply, mixed problems require you to:

1. **Identify the pattern** — Is this two pointers? DP? Greedy? Sliding window?
2. **Choose the right data structure** — Array, hash map, heap, tree, graph?
3. **Combine techniques** — E.g., DP + binary search, greedy + sorting, two pointers + hash map

### Why it matters for FAANG interviews
FAANG interviewers rarely ask "implement Kadane's algorithm" directly. Instead, they ask problems where:
- You need to recognize which technique applies
- Multiple approaches work, but one is optimal
- Edge cases and constraints matter
- You need to explain your reasoning clearly

### Real-life analogy
**Cooking a complex meal**: You know how to chop, sauté, boil, and bake individually. Mixed revision is like being given ingredients and asked to cook a dish — you decide which techniques to use, in what order, and adapt based on what you have.

### Time and space complexity expectations
- Easy: O(n) or O(n log n) time, O(1) or O(n) space
- Medium: O(n log n) or O(n²) time, O(n) or O(n²) space
- Hard: O(n²) or O(n³) time, O(n) to O(n²) space, with optimizations

### Common mistakes and edge cases
- **Recognizing wrong pattern**: Using DP when greedy works, or vice versa
- **Over-optimizing prematurely**: Getting stuck on O(1) space when O(n) is fine
- **Missing edge cases**: Empty input, single element, all same values, negative numbers
- **Not validating constraints**: Input size hints at expected complexity
- **Code bugs under pressure**: Off-by-one, null handling, integer overflow

### When to use it in interviews
- **Final rounds / onsite**: Mixed problems test breadth and adaptability
- **System design + coding**: Problems that span multiple domains
- **Time-constrained**: Need to quickly identify pattern and implement

## 2. Important Patterns

### Pattern 1: Two Pointers + Hash Map
**Example**: Longest Substring Without Repeating Characters
```kotlin
// Sliding window with hash set for character tracking
var left = 0
val charSet = HashSet<Char>()
for (right in s.indices) {
    while (charSet.contains(s[right])) {
        charSet.remove(s[left])
        left++
    }
    charSet.add(s[right])
    maxLen = maxOf(maxLen, right - left + 1)
}
```

### Pattern 2: DP + Binary Search
**Example**: Russian Doll Envelopes / Job Scheduling
```kotlin
// Sort by one dimension, DP with binary search on other
envelopes.sortBy { it.first }.sortByDescending { it.second }
// LIS on second dimension with binary search
```

### Pattern 3: Greedy + Sorting + Heap
**Example**: Maximum Performance of a Team / Task Scheduler
```kotlin
// Sort by one criteria, use heap for maintaining best candidates
```

### Pattern 4: Sliding Window + Two Pointers
**Example**: Minimum Window Substring / Longest Repeating Character Replacement
```kotlin
// Expand window, contract when condition met, track optimal
```

### Pattern 5: Tree + DP / Graph + DP
**Example**: House Robber III (Tree DP), Network Delay Time (Dijkstra + DP)
```kotlin
// Tree DP: return multiple values from each subtree
// Graph DP: shortest path with state
```

## 3. Dry Run

**Problem**: Longest Substring Without Repeating Characters
**Input**: `s = "abcabcbb"`
**Expected Output**: 3 ("abc")

**Algorithm**: Sliding window with hash set

**Step-by-Step**:
```
s = "abcabcbb"
left = 0, maxLen = 0, charSet = {}

right = 0: 'a' not in set → add 'a', set={a}, maxLen = 1
right = 1: 'b' not in set → add 'b', set={a,b}, maxLen = 2
right = 2: 'c' not in set → add 'c', set={a,b,c}, maxLen = 3
right = 3: 'a' IN set → remove s[left]='a', left=1, set={b,c}
           'a' not in set → add 'a', set={b,c,a}, maxLen = 3
right = 4: 'b' IN set → remove s[left]='b', left=2, set={c,a}
           'b' not in set → add 'b', set={c,a,b}, maxLen = 3
right = 5: 'c' IN set → remove s[left]='c', left=3, set={a,b}
           'c' not in set → add 'c', set={a,b,c}, maxLen = 3
right = 6: 'b' IN set → remove s[left]='a', left=4, set={b,c}
           'b' IN set → remove s[left]='b', left=5, set={c}
           'b' not in set → add 'b', set={c,b}, maxLen = 3
right = 7: 'b' IN set → remove s[left]='c', left=6, set={b}
           'b' IN set → remove s[left]='b', left=7, set={}
           'b' not in set → add 'b', set={b}, maxLen = 3

Result: 3 ✓
```

**Variable Tracking**:
- `left`: Start of current window
- `right`: End of current window (iterating through string)
- `charSet`: Characters currently in window
- `maxLen`: Best window size found

**Why this works**:
- Each character added once, removed at most once → O(n) total
- Window always contains unique characters
- Max window size tracked throughout

## 4. Code Template

**Time Complexity**: O(n) — each character processed at most twice
**Space Complexity**: O(min(m, n)) where m = character set size

```kotlin
package mixed_revision

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/longest-substring-without-repeating-characters/
 * Difficulty: Medium
 * Core pattern: Sliding window + Hash Set (Two pointers)
 * 
 * Time Complexity: O(n)
 * Space Complexity: O(min(m, n)) where m = charset size
 */
fun lengthOfLongestSubstring(s: String): Int {
    val n = s.length
    val charSet = HashSet<Char>()
    var left = 0
    var maxLen = 0
    
    for (right in 0 until n) {
        // Shrink window until s[right] is not in set
        while (charSet.contains(s[right])) {
            charSet.remove(s[left])
            left++
        }
        // Expand window
        charSet.add(s[right])
        maxLen = maxOf(maxLen, right - left + 1)
    }
    
    return maxLen
}

/**
 * Explanation:
 * - charSet tracks characters in current window [left, right]
 * - When duplicate found, shrink from left until duplicate removed
 * - Each character added once, removed at most once → O(n)
 * - maxLen tracks largest valid window seen
 * 
 * Alternative optimization: Use HashMap<Char, Int> to store last index,
 * jump left directly to lastIndex[s[right]] + 1 instead of while loop
 */
```

**Line-by-line explanation**:
- Line 8: Initialize hash set for tracking characters in window
- Line 9: Left pointer marks start of window
- Line 10: Track maximum valid window length
- Lines 12-15: Shrink window from left when duplicate found
- Line 17: Add current character to window
- Line 18: Update maximum length

**Why this works for mixed revision**:
- Combines: Two pointers + Hash Set + Sliding window
- Requires pattern recognition (not obvious from problem statement)
- Multiple valid approaches (hash map optimization)
- Edge cases: empty string, all same chars, all unique chars

## 5. Practice Problems

### Easy Problem 1: Two Sum
- **LeetCode**: https://leetcode.com/problems/two-sum/
- **Difficulty**: Easy
- **Core pattern**: Hash Map (one pass)
- **Approach hint**: Store complement in map as you iterate
- **Expected time and space complexity**: O(n) time, O(n) space

### Easy Problem 2: Valid Palindrome
- **LeetCode**: https://leetcode.com/problems/valid-palindrome/
- **Difficulty**: Easy
- **Core pattern**: Two pointers from both ends
- **Approach hint**: Skip non-alphanumeric, compare case-insensitive
- **Expected time and space complexity**: O(n) time, O(1) space

### Medium Problem 1: Longest Substring Without Repeating Characters
- **LeetCode**: https://leetcode.com/problems/longest-substring-without-repeating-characters/
- **Difficulty**: Medium
- **Core pattern**: Sliding window + Hash Set (Two pointers)
- **Approach hint**: Expand right, shrink left on duplicate, track max
- **Expected time and space complexity**: O(n) time, O(min(m,n)) space

### Medium Problem 2: 3Sum
- **LeetCode**: https://leetcode.com/problems/3sum/
- **Difficulty**: Medium
- **Core pattern**: Sorting + Two pointers
- **Approach hint**: Fix one element, two-sum on remainder with two pointers
- **Expected time and space complexity**: O(n²) time, O(1) space (excluding output)

### Hard Problem 1: Trapping Rain Water
- **LeetCode**: https://leetcode.com/problems/trapping-rain-water/
- **Difficulty**: Hard
- **Core pattern**: Two pointers with height comparison
- **Approach hint**: Water at position = min(maxLeft, maxRight) - height[i]
- **Expected time and space complexity**: O(n) time, O(1) space

### Hard Problem 2: Merge k Sorted Lists
- **LeetCode**: https://leetcode.com/problems/merge-k-sorted-lists/
- **Difficulty**: Hard
- **Core pattern**: Min-heap + Linked list
- **Approach hint**: Maintain heap of k list heads, repeatedly extract min
- **Expected time and space complexity**: O(n log k) time, O(k) space