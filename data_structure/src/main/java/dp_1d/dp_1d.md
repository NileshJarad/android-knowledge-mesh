# Topic 15: 1-D Dynamic Programming

## Concept Explanation

### What is 1-D Dynamic Programming?
Dynamic Programming (DP) is a method for solving complex problems by breaking them down into simpler subproblems. **1-D DP** refers to problems where the state can be represented using a single dimension (one index variable). The DP table is a 1-D array where each element represents the optimal solution to a subproblem.

### How it works internally
1. **Identify the state**: Define what `dp[i]` represents (e.g., "max profit up to day i", "min cost to reach step i")
2. **Find the recurrence relation**: How to compute `dp[i]` from previous states (`dp[i-1]`, `dp[i-2]`, etc.)
3. **Define base cases**: Initial values for `dp[0]`, `dp[1]`, etc.
4. **Determine iteration order**: Bottom-up (tabulation) or top-down with memoization
5. **Extract final answer**: Usually `dp[n-1]` or max/min over the entire array

**Key insight**: We solve each subproblem once and store the result to avoid recomputation.

### Real-life analogy
**Staircase climbing**: You want to reach step N. You can take 1 or 2 steps at a time. The number of ways to reach step N = ways to reach step N-1 + ways to reach step N-2. You don't recount from scratch for each step — you build on previous counts.

### Time and space complexity for every major operation
- **Time**: O(n) to O(n²) depending on transitions per state
- **Space**: O(n) for DP array; can optimize to O(1) if only need previous 1-2 states

### Common mistakes and edge cases
- **Off-by-one errors**: Wrong base cases or array bounds
- **Missing state**: Not capturing all necessary information in DP state
- **Wrong transition**: Incorrect recurrence relation
- **Overlapping subproblems not recognized**: Missing the memoization opportunity
- **Space optimization too early**: Getting wrong results when reducing DP array to variables

### When to use it in interviews
- Optimization problems with overlapping subproblems (min/max cost, ways, length)
- When greedy doesn't work (e.g., coin change, knapsack variants)
- String problems: longest palindromic subsequence, edit distance (1D version)
- Array problems: house robber, max subarray, jump game

## 2. Important Patterns

### Pattern 1: Linear Recurrence (Fibonacci-style)
**Example**: Climbing Stairs / House Robber
```kotlin
dp[i] = dp[i-1] + dp[i-2]  // or max(dp[i-1], dp[i-2] + nums[i])
```
**Brute force**: Recursion with exponential time (2^n)
**Better**: Memoization (top-down) - O(n) time, O(n) space
**Optimal**: Bottom-up with O(1) space - O(n) time, O(1) space

### Pattern 2: Max/Min Subarray (Kadane's Algorithm)
**Example**: Maximum Subarray Sum
```kotlin
dp[i] = max(nums[i], dp[i-1] + nums[i])
```
**Brute force**: Check all subarrays O(n²)
**Better**: DP array O(n) time, O(n) space
**Optimal**: Kadane's O(n) time, O(1) space

### Pattern 3: Decision at Each Step (Include/Exclude)
**Example**: House Robber, Max Sum Non-Adjacent
```kotlin
dp[i] = max(dp[i-1], dp[i-2] + value[i])
```
**Brute force**: Try all subsets 2^n
**Better**: DP with O(n) time, O(n) space
**Optimal**: O(n) time, O(1) space (just two variables)

### Pattern 4: State Machine / Multiple States
**Example**: Best Time to Buy/Sell Stock with Cooldown
```kotlin
hold[i] = max(hold[i-1], sold[i-2] - price[i])
sold[i] = max(sold[i-1], hold[i-1] + price[i])
```
**Brute force**: All possible buy/sell sequences
**Better**: DP with multiple state arrays
**Optimal**: O(n) time, O(1) space per state

### Pattern 5: Prefix/Suffix DP
**Example**: Trapping Rain Water (1D prefix max + suffix max)
```kotlin
leftMax[i] = max(leftMax[i-1], height[i])
rightMax[i] = max(rightMax[i+1], height[i])
```
**Brute force**: For each bar, scan left and right O(n²)
**Better**: Two arrays O(n) time, O(n) space
**Optimal**: Two pointers O(n) time, O(1) space

## 3. Dry Run

**Problem**: House Robber (LeetCode 198)
**Input**: [2, 7, 9, 3, 1]
**Expected Output**: 12 (rob houses 0, 2, 4 = 2 + 9 + 1)

**Algorithm**: dp[i] = max(dp[i-1], dp[i-2] + nums[i])

**Step-by-Step**:
```
Initial: nums = [2, 7, 9, 3, 1]
dp array initialized with base cases

i = 0: dp[0] = 2  (only one house)
i = 1: dp[1] = max(dp[0], nums[1]) = max(2, 7) = 7
i = 2: dp[2] = max(dp[1], dp[0] + nums[2]) = max(7, 2+9) = 11
i = 3: dp[3] = max(dp[2], dp[1] + nums[3]) = max(11, 7+3) = 11
i = 4: dp[4] = max(dp[3], dp[2] + nums[4]) = max(11, 11+1) = 12

Result: dp[4] = 12
```

**Variable Tracking**:
- `prev2` (dp[i-2]): 2 → 7 → 11 → 11
- `prev1` (dp[i-1]): 7 → 11 → 11 → 12
- `current` (dp[i]): 7 → 11 → 11 → 12
- Final answer in `prev1`

**Space Optimized**: Only track last 2 values, not full array

## 4. Code Template

**Time Complexity**: O(n) where n is input size
**Space Complexity**: O(1) — only two variables needed

```kotlin
package dp_1d

/**
 * LeetCode: https://leetcode.com/problems/house-robber/
 * Difficulty: Medium
 * Core pattern: Linear DP with include/exclude decision
 */
fun rob(nums: IntArray): Int {
    if (nums.isEmpty()) return 0
    if (nums.size == 1) return nums[0]
    
    // prev2 = dp[i-2], prev1 = dp[i-1]
    var prev2 = nums[0]
    var prev1 = maxOf(nums[0], nums[1])
    
    for (i in 2 until nums.size) {
        // Decision: skip current (prev1) or take current (prev2 + nums[i])
        val current = maxOf(prev1, prev2 + nums[i])
        
        // Slide window: shift states forward
        prev2 = prev1
        prev1 = current
    }
    
    return prev1
}

/**
 * Time Complexity: O(n) - single pass through array
 * Space Complexity: O(1) - only two variables
 * 
 * Key Insight: At each house, we either:
 * - Skip it: profit = prev1 (best up to previous house)
 * - Rob it: profit = prev2 + nums[i] (best up to i-2 + current)
 * Take the maximum of both choices
 */
```

**Line-by-line explanation**:
- Lines 7-9: Handle edge cases (empty array, single element)
- Lines 12-13: Initialize base cases — first two houses
- Line 15: Iterate from third house to end
- Line 18: Core recurrence — max of (skip current, rob current)
- Lines 21-22: Update sliding window variables
- Line 24: Return final maximum

**Why this works**:
- Each house decision only depends on previous two optimal solutions
- No need to store entire DP array — just last two values
- Greedy choice at each step leads to global optimum

## 5. Practice Problems

### Easy Problem 1: Climbing Stairs
- **LeetCode**: https://leetcode.com/problems/climbing-stairs/
- **Difficulty**: Easy
- **Core pattern**: Linear recurrence (Fibonacci)
- **Approach hint**: Ways to reach step i = ways(i-1) + ways(i-2)
- **Expected time and space complexity**: O(n) time, O(1) space

### Easy Problem 2: Maximum Subarray (Kadane's)
- **LeetCode**: https://leetcode.com/problems/maximum-subarray/
- **Difficulty**: Easy
- **Core pattern**: Max subarray ending at each position
- **Approach hint**: Track max sum ending at current index, update global max
- **Expected time and space complexity**: O(n) time, O(1) space

### Medium Problem 1: House Robber
- **LeetCode**: https://leetcode.com/problems/house-robber/
- **Difficulty**: Medium
- **Core pattern**: Include/Exclude decision at each step
- **Approach hint**: dp[i] = max(dp[i-1], dp[i-2] + nums[i])
- **Expected time and space complexity**: O(n) time, O(1) space

### Medium Problem 2: Best Time to Buy and Sell Stock
- **LeetCode**: https://leetcode.com/problems/best-time-to-buy-and-sell-stock/
- **Difficulty**: Medium
- **Core pattern**: Track minimum price and maximum profit
- **Approach hint**: For each day, profit = price - minPriceSoFar
- **Expected time and space complexity**: O(n) time, O(1) space

### Hard Problem 1: Best Time to Buy and Sell Stock with Cooldown
- **LeetCode**: https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-cooldown/
- **Difficulty**: Hard
- **Core pattern**: State machine DP (hold, sold, rest)
- **Approach hint**: Three states with transitions between them
- **Expected time and space complexity**: O(n) time, O(1) space

### Hard Problem 2: Word Break
- **LeetCode**: https://leetcode.com/problems/word-break/
- **Difficulty**: Medium-Hard
- **Core pattern**: DP on string prefixes
- **Approach hint**: dp[i] = true if substring(0,i) can be segmented
- **Expected time and space complexity**: O(n²) time, O(n) space