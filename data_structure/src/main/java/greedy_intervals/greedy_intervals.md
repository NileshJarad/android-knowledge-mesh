# Topic 17: Greedy Algorithms and Intervals

## Concept Explanation

### What are Greedy Algorithms?
A **Greedy Algorithm** makes the locally optimal choice at each step with the hope of finding a global optimum. It never revisits previous decisions. The key is proving that local optimal choices lead to global optimal solution.

### What are Interval Problems?
**Interval problems** deal with ranges `[start, end]`. Common operations:
- **Merging**: Combine overlapping intervals
- **Scheduling**: Select maximum non-overlapping intervals
- **Partitioning**: Divide intervals into groups
- **Covering**: Find minimum points to cover all intervals

### How Greedy works internally
1. **Sort** the input based on a greedy criterion (end time, start time, value/weight ratio)
2. **Iterate** through sorted input making greedy choices
3. **Prove** that local choice doesn't prevent global optimum (exchange argument)

### Real-life analogy
**Choosing meetings**: You have a calendar with many meeting requests. You want to attend the maximum number of meetings. The greedy strategy: **always pick the meeting that ends earliest**. This leaves maximum time for remaining meetings.

### Time and space complexity for every major operation
- **Sorting**: O(n log n)
- **Single pass**: O(n)
- **Total time**: O(n log n) dominated by sorting
- **Space**: O(1) extra (excluding sort), or O(n) if storing result

### Common mistakes and edge cases
- **Wrong sorting key**: Sorting by start time instead of end time for scheduling
- **Empty/single interval**: Handle edge cases
- **Strict vs non-strict overlap**: `[1,2]` and `[2,3]` — do they overlap?
- **Multiple intervals with same end time**: Order doesn't matter for end-time sort
- **Integer overflow**: When computing durations or differences
- **Assuming greedy works without proof**: Not all problems are greedy-solvable

### When to use it in interviews
- **Interval scheduling**: Maximum non-overlapping intervals, meeting rooms
- **Coin change (canonical systems)**: US coins
- **Fractional knapsack**: Value/weight ratio
- **Huffman coding**: Build optimal prefix codes
- **Minimum spanning tree**: Kruskal's, Prim's
- **Any problem with "earliest end time" or "highest value/weight" pattern**

## 2. Important Patterns

### Pattern 1: Interval Scheduling (Maximum Non-Overlapping)
**Example**: Non-overlapping Intervals / Meeting Rooms
```kotlin
// Sort by end time
intervals.sortBy { it[1] }
var end = -infinity
var count = 0
for (interval in intervals) {
    if (interval[0] >= end) {
        count++
        end = interval[1]
    }
}
```
**Brute force**: Try all subsets O(2^n)
**Better**: DP with sorting O(n²)
**Optimal**: Greedy with end-time sort O(n log n)

### Pattern 2: Merge Intervals
**Example**: Merge Overlapping Intervals (LeetCode 56)
```kotlin
// Sort by start time
intervals.sortBy { it[0] }
val merged = mutableListOf<IntArray>()
for (interval in intervals) {
    if (merged.isEmpty() || merged.last()[1] < interval[0]) {
        merged.add(interval)  // No overlap
    } else {
        merged.last()[1] = maxOf(merged.last()[1], interval[1])  // Merge
    }
}
```
**Brute force**: Compare all pairs O(n²)
**Better**: Sort + single pass O(n log n)
**Optimal**: Same as better

### Pattern 3: Minimum Points to Cover Intervals
**Example**: Minimum Number of Arrows to Burst Balloons (LeetCode 452)
```kotlin
// Sort by end coordinate
points.sortBy { it[1] }
var arrow = -infinity
var count = 0
for (point in points) {
    if (point[0] > arrow) {
        count++
        arrow = point[1]
    }
}
```
**Brute force**: Try all point combinations
**Better**: Greedy with end-time sort O(n log n)
**Optimal**: Same as better

### Pattern 4: Meeting Rooms II (Minimum Resources)
**Example**: Minimum Meeting Rooms (LeetCode 253)
```kotlin
// Two arrays: start times and end times, both sorted
starts.sort()
ends.sort()
var rooms = 0, endPtr = 0
for (start in starts) {
    if (start >= ends[endPtr]) {
        endPtr++  // Free a room
    } else {
        rooms++   // Need new room
    }
}
```
**Brute force**: Check all time points
**Better**: Sort + two pointers O(n log n)
**Optimal**: Same as better — also heap-based O(n log n)

### Pattern 5: Partition Labels / Jump Game Style
**Example**: Partition Labels (LeetCode 763)
```kotlin
// Track last occurrence of each character
val lastIndex = IntArray(26) { -1 }
for (i in s.indices) lastIndex[s[i]-'a'] = i

// Greedy expansion
var start = 0, end = 0
for (i in s.indices) {
    end = maxOf(end, lastIndex[s[i]-'a'])
    if (i == end) {
        partitions.add(end - start + 1)
        start = end + 1
    }
}
```
**Brute force**: Try all partitions
**Better**: Two passes O(n) time, O(1) space
**Optimal**: Same as better

## 3. Dry Run

**Problem**: Minimum Number of Arrows to Burst Balloons (LeetCode 452)
**Input**: `points = [[10,16],[2,8],[1,6],[7,12]]`
**Expected Output**: 2 (arrows at x=6 and x=11)

**Algorithm**: Sort by end coordinate, shoot arrow at end of first balloon, skip all balloons it bursts

**Step-by-Step**:
```
Initial points: [[10,16], [2,8], [1,6], [7,12]]

Step 1: Sort by end coordinate
Sorted: [[1,6], [2,8], [7,12], [10,16]]
         ↑ end=6  ↑ end=8  ↑ end=12  ↑ end=16

Step 2: Initialize
arrow = -∞, count = 0

Step 3: Process [1,6]
  start=1 > arrow(-∞) → YES, shoot new arrow
  count = 1, arrow = 6

Step 4: Process [2,8]
  start=2 > arrow(6) → NO (2 ≤ 6), skip — arrow at 6 bursts this too
  count = 1, arrow = 6

Step 5: Process [7,12]
  start=7 > arrow(6) → YES, shoot new arrow
  count = 2, arrow = 12

Step 6: Process [10,16]
  start=10 > arrow(12) → NO (10 ≤ 12), skip — arrow at 12 bursts this too
  count = 2, arrow = 12

Result: 2 arrows needed ✓
```

**Variable Tracking**:
- `arrow`: Position of last shot arrow (initialized to -∞)
- `count`: Number of arrows shot
- For each balloon `[start, end]`:
  - If `start > arrow`: current arrow can't reach → shoot new arrow at `end`
  - Else: current arrow at `arrow` can burst this balloon → skip

**Why this works**: 
- Shooting at the **earliest end** maximizes the chance to burst subsequent balloons
- Any balloon starting before `arrow` is already burst
- This is the classic interval scheduling proof by exchange argument

## 4. Code Template

**Time Complexity**: O(n log n) — dominated by sorting
**Space Complexity**: O(1) extra (or O(n) for sort)

```kotlin
package greedy_intervals

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/non-overlapping-intervals/
 * Difficulty: Medium
 * Core pattern: Greedy interval scheduling — sort by end time
 * 
 * Time Complexity: O(n log n)
 * Space Complexity: O(1) extra
 */
fun eraseOverlapIntervals(intervals: Array<IntArray>): Int {
    if (intervals.size <= 1) return 0
    
    // KEY: Sort by END time (not start time!)
    intervals.sortBy { it[1] }
    
    var end = intervals[0][1]
    var nonOverlapping = 1  // First interval always selected
    
    for (i in 1 until intervals.size) {
        val current = intervals[i]
        // If current interval starts after or at end of last selected
        if (current[0] >= end) {
            nonOverlapping++
            end = current[1]  // Update end to current interval's end
        }
        // Else: overlap detected, skip current interval (implicit removal)
    }
    
    // Minimum removals = total - maximum non-overlapping
    return intervals.size - nonOverlapping
}

/**
 * Explanation:
 * - Sort by end time to maximize room for remaining intervals
 * - Always keep the interval that ends earliest among overlapping ones
 * - Count maximum non-overlapping intervals, subtract from total
 * - "Remove minimum" = "Keep maximum non-overlapping"
 * 
 * Why end time sort works:
 * - Interval ending earlier leaves more room for future intervals
 * - Exchange argument: any optimal solution can be transformed to use earliest-ending
 *   without reducing the count
 */
```

**Line-by-line explanation**:
- Lines 8-9: Handle edge cases (0 or 1 interval)
- Line 12: **Critical** — sort by end time (`it[1]`), not start time
- Line 14: Initialize with first interval's end time
- Line 15: First interval is always part of optimal solution
- Lines 17-23: Iterate through remaining intervals
- Line 20: Check if current interval starts after (or at) last selected end
- Lines 21-22: If no overlap, include it and update end
- Line 26: Answer = total intervals - maximum we can keep

**Why this works**:
- Greedy choice: pick interval with earliest end time
- Proof by exchange: if optimal solution doesn't pick earliest-ending, we can swap
- Equivalent to: keep maximum non-overlapping, remove the rest

## 5. Practice Problems

### Easy Problem 1: Non-overlapping Intervals
- **LeetCode**: https://leetcode.com/problems/non-overlapping-intervals/
- **Difficulty**: Medium
- **Core pattern**: Greedy interval scheduling — sort by end time
- **Approach hint**: Keep max non-overlapping, answer = total - kept
- **Expected time and space complexity**: O(n log n) time, O(1) space

### Easy Problem 2: Merge Intervals
- **LeetCode**: https://leetcode.com/problems/merge-intervals/
- **Difficulty**: Medium
- **Core pattern**: Sort by start, merge overlapping
- **Approach hint**: If current.start <= last.end, merge; else add new
- **Expected time and space complexity**: O(n log n) time, O(n) space for result

### Medium Problem 1: Minimum Number of Arrows to Burst Balloons
- **LeetCode**: https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/
- **Difficulty**: Medium
- **Core pattern**: Sort by end, shoot at end of first, skip bursted
- **Approach hint**: Same as interval scheduling but with [start, end] as balloon diameter
- **Expected time and space complexity**: O(n log n) time, O(1) space

### Medium Problem 2: Meeting Rooms II
- **LeetCode**: https://leetcode.com/problems/meeting-rooms-ii/
- **Difficulty**: Medium
- **Core pattern**: Two pointers on sorted start/end arrays (or min-heap)
- **Approach hint**: Track concurrent meetings with sorted start times and end times
- **Expected time and space complexity**: O(n log n) time, O(n) space

### Hard Problem 1: Interval Scheduling with Weights (Weighted Interval Scheduling)
- **LeetCode**: https://leetcode.com/problems/maximum-profit-in-job-scheduling/
- **Difficulty**: Hard
- **Core pattern**: DP with binary search (not pure greedy!)
- **Approach hint**: Sort by end time, DP with binary search for previous non-overlapping
- **Expected time and space complexity**: O(n log n) time, O(n) space

### Hard Problem 2: Partition Labels
- **LeetCode**: https://leetcode.com/problems/partition-labels/
- **Difficulty**: Medium-Hard
- **Core pattern**: Greedy expansion with last occurrence tracking
- **Approach hint**: Track last index of each char, expand partition until all chars in it end
- **Expected time and space complexity**: O(n) time, O(1) space