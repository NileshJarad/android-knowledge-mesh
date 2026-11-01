package dp_2d

/**
 * LeetCode: https://leetcode.com/problems/unique-paths-ii/
 * Difficulty: Medium
 * Core pattern: Path counting with obstacles — dp[i][j] = 0 for obstacle cells
 *
 * Time Complexity: O(m × n)
 * Space Complexity: O(n) — 1D array optimization
 */
fun uniquePathsWithObstacles(obstacleGrid: Array<IntArray>): Int {
    val m = obstacleGrid.size
    val n = obstacleGrid[0].size

    // If start or end is blocked, no paths exist
    if (obstacleGrid[0][0] == 1 || obstacleGrid[m-1][n-1] == 1) {
        return 0
    }

    val dp = IntArray(n)
    dp[0] = 1  // Start position

    // Initialize first row
    for (j in 1 until n) {
        dp[j] = if (obstacleGrid[0][j] == 1) 0 else dp[j - 1]
    }

    // Process remaining rows
    for (i in 1 until m) {
        // First column
        if (obstacleGrid[i][0] == 1) {
            dp[0] = 0
        }

        // Remaining columns
        for (j in 1 until n) {
            if (obstacleGrid[i][j] == 1) {
                dp[j] = 0  // Obstacle — no paths through here
            } else {
                dp[j] = dp[j] + dp[j - 1]  // from above + from left
            }
        }
    }

    return dp[n - 1]
}

/**
 * Explanation:
 * - If start or end is blocked, return 0 immediately
 * - Obstacle cells always have dp = 0 (no paths through them)
 * - For non-obstacle cells: dp[j] = dp[j] (from above) + dp[j-1] (from left)
 * - First row/column handled specially since they have only one direction
 */