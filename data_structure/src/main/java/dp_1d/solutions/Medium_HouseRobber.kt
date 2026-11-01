package dp_1d

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/house-robber/
 * Difficulty: Medium
 * Core pattern: Include/Exclude decision at each step
 *
 * Time Complexity: O(n)
 * Space Complexity: O(1)
 */
fun rob(nums: IntArray): Int {
    if (nums.isEmpty()) return 0
    if (nums.size == 1) return nums[0]

    var prev2 = nums[0]                    // dp[i-2]
    var prev1 = maxOf(nums[0], nums[1])    // dp[i-1]

    for (i in 2 until nums.size) {
        val current = maxOf(prev1, prev2 + nums[i])
        prev2 = prev1
        prev1 = current
    }

    return prev1
}

/**
 * Explanation:
 * - At each house: skip (prev1) or rob (prev2 + nums[i])
 * - dp[i] = max(dp[i-1], dp[i-2] + nums[i])
 * - Space optimized to O(1) using two variables
 */