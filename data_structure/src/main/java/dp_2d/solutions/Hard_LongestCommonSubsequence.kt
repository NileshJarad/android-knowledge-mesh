package dp_2d

/**
 * LeetCode: https://leetcode.com/problems/longest-common-subsequence/
 * Difficulty: Hard
 * Core pattern: 2D DP for string alignment — dp[i][j] = LCS of text1[0..i) and text2[0..j)
 *
 * Time Complexity: O(m × n)
 * Space Complexity: O(n) — 1D array optimization
 */
fun longestCommonSubsequence(text1: String, text2: String): Int {
    val m = text1.length
    val n = text2.length

    // dp[j] = LCS length of text1[0..i) and text2[0..j) for current row i
    val dp = IntArray(n + 1)

    for (i in 1..m) {
        var prev = 0  // dp[i-1][j-1] from previous iteration

        for (j in 1..n) {
            val temp = dp[j]  // Save current value (will be dp[i-1][j] for next row)

            if (text1[i - 1] == text2[j - 1]) {
                // Characters match — extend LCS from diagonal
                dp[j] = prev + 1
            } else {
                // Characters don't match — take max of left or top
                dp[j] = maxOf(dp[j], dp[j - 1])
            }

            prev = temp  // For next iteration, this becomes dp[i-1][j-1]
        }
    }

    return dp[n]
}

/**
 * Explanation:
 * - dp[j] represents LCS length for current row i and column j
 * - If characters match: dp[j] = dp[i-1][j-1] + 1 (extend LCS)
 * - If characters don't match: dp[j] = max(dp[i-1][j], dp[i][j-1])
 * - prev tracks diagonal value (dp[i-1][j-1]) from previous row
 * - Space optimized to O(n) using single array
 *
 * Why prev is needed:
 * - dp[j] before update = dp[i-1][j] (from above)
 * - dp[j-1] after update = dp[i][j-1] (from left)
 * - prev = dp[i-1][j-1] (from diagonal) — needs to be saved before overwriting
 */