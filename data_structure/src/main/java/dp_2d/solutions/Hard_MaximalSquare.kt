package dp_2d

import kotlin.math.minOf

/**
 * LeetCode: https://leetcode.com/problems/maximal-square/
 * Difficulty: Hard
 * Core pattern: dp[i][j] = min(dp[i-1][j], dp[i][j-1], dp[i-1][j-1]) + 1 for '1' cells
 *
 * Time Complexity: O(m × n)
 * Space Complexity: O(n) — 1D array optimization
 */
fun maximalSquare(matrix: Array<CharArray>): Int {
    if (matrix.isEmpty() || matrix[0].isEmpty()) return 0

    val m = matrix.size
    val n = matrix[0].size

    // dp[j] = side length of largest square ending at (current row, column j)
    val dp = IntArray(n + 1)  // Extra column for easier boundary handling
    var maxSide = 0

    var prev = 0  // Stores dp[j-1] from previous row (top-left diagonal)

    for (i in 0 until m) {
        for (j in 0 until n) {
            val temp = dp[j + 1]  // Save current value (will be top for next iteration)

            if (matrix[i][j] == '1') {
                // dp[j+1] = min(top, left, top-left) + 1
                dp[j + 1] = minOf(dp[j + 1], dp[j], prev) + 1
                maxSide = maxOf(maxSide, dp[j + 1])
            } else {
                dp[j + 1] = 0  // '0' cell — no square can end here
            }

            prev = temp  // For next iteration, this becomes top-left
        }
    }

    return maxSide * maxSide  // Area = side²
}

/**
 * Explanation:
 * - dp[j+1] represents side length of largest square ending at current cell
 * - For '1' cells: min(top, left, top-left) + 1
 * - For '0' cells: dp = 0 (no square can end here)
 * - prev tracks top-left diagonal value (dp[i-1][j-1])
 * - maxSide tracks the largest square found
 * - Return area = maxSide²
 */