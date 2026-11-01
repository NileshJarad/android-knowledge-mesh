package dp_2d

import kotlin.math.minOf

/**
 * LeetCode: https://leetcode.com/problems/triangle/
 * Difficulty: Medium
 * Core pattern: Bottom-up DP on triangular grid
 *
 * Time Complexity: O(n²)
 * Space Complexity: O(n) — 1D array optimization
 */
fun minimumTotal(triangle: List<List<Int>>): Int {
    val n = triangle.size

    // dp[j] = minimum path sum to reach position j in current row
    val dp = IntArray(n)
    dp[0] = triangle[0][0]

    // Process each row
    for (i in 1 until n) {
        // Process right-to-left to avoid overwriting needed values
        // Last element in row i can only come from dp[i-1] (top-right)
        dp[i] = dp[i - 1] + triangle[i][i]

        // Middle elements: can come from top-left (dp[j-1]) or top-right (dp[j])
        for (j in i - 1 downTo 1) {
            dp[j] = minOf(dp[j], dp[j - 1]) + triangle[i][j]
        }

        // First element can only come from top-left (dp[0])
        dp[0] += triangle[i][0]
    }

    // Find minimum in last row
    var min = dp[0]
    for (j in 1 until n) {
        if (dp[j] < min) min = dp[j]
    }

    return min
}

/**
 * Explanation:
 * - Triangle structure means row i has i+1 elements
 * - Each cell can be reached from top-left (dp[j-1]) or top-right (dp[j])
 * - Process right-to-left to preserve dp[j] values for next iteration
 * - dp[j] represents minimum sum to reach position j in current row
 * - Final answer is minimum value in dp array after processing all rows
 */