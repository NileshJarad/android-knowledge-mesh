package dp_2d

import kotlin.math.minOf

/**
 * LeetCode: https://leetcode.com/problems/minimum-path-sum/
 * Difficulty: Medium
 * Core pattern: dp[i][j] = min(dp[i-1][j], dp[i][j-1]) + grid[i][j]
 *
 * Time Complexity: O(m × n)
 * Space Complexity: O(n) — 1D array optimization
 */
fun minPathSum(grid: Array<IntArray>): Int {
    val m = grid.size
    val n = grid[0].size

    val dp = IntArray(n)
    dp[0] = grid[0][0]

    // Initialize first row — can only come from left
    for (j in 1 until n) {
        dp[j] = dp[j - 1] + grid[0][j]
    }

    // Process remaining rows
    for (i in 1 until m) {
        // First column — can only come from above
        dp[0] += grid[i][0]

        // Remaining columns
        for (j in 1 until n) {
            dp[j] = minOf(dp[j], dp[j - 1]) + grid[i][j]
        }
    }

    return dp[n - 1]
}

/**
 * Explanation:
 * - dp[j] = minimum path sum to reach current cell at column j
 * - First row initialized with cumulative sum from left
 * - First column handled by dp[0] += grid[i][0] each row
 * - Core transition: min(come from above, come from left) + current cell value
 */