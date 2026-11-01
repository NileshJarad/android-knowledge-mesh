package dp_2d

/**
 * LeetCode: https://leetcode.com/problems/unique-paths/
 * Difficulty: Medium
 * Core pattern: Path counting — dp[i][j] = dp[i-1][j] + dp[i][j-1]
 *
 * Time Complexity: O(m × n)
 * Space Complexity: O(n) — 1D array optimization
 */
fun uniquePaths(m: Int, n: Int): Int {
    // dp[j] = number of unique paths to reach (current row, column j)
    val dp = IntArray(n) { 1 }  // All cells in first row have 1 path

    for (i in 1 until m) {
        // First column always has exactly 1 path (only down moves)
        // dp[0] stays at 1, no need to update

        for (j in 1 until n) {
            // dp[j] = paths from above (previous row) + paths from left (current row)
            dp[j] = dp[j] + dp[j - 1]
        }
    }

    return dp[n - 1]
}

/**
 * Explanation:
 * - dp[j] represents number of unique paths to reach current cell at column j
 * - Initialize all to 1 (first row has exactly 1 path for each cell)
 * - For each subsequent row, dp[j] = dp[j] (from above) + dp[j-1] (from left)
 * - Space optimized to O(n) using single array
 */