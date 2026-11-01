package dp_1d

/**
 * LeetCode: https://leetcode.com/problems/word-break/
 * Difficulty: Medium-Hard
 * Core pattern: DP on string prefixes
 *
 * Time Complexity: O(n²) where n is string length
 * Space Complexity: O(n) for DP array
 */
fun wordBreak(s: String, wordDict: List<String>): Boolean {
    val wordSet = wordDict.toSet()
    val n = s.length

    // dp[i] = true if s.substring(0, i) can be segmented into words
    val dp = BooleanArray(n + 1)
    dp[0] = true  // Empty string is always breakable

    for (i in 1..n) {
        for (j in 0 until i) {
            // If prefix [0..j) is breakable and substring [j..i) is in dictionary
            if (dp[j] && wordSet.contains(s.substring(j, i))) {
                dp[i] = true
                break
            }
        }
    }

    return dp[n]
}

/**
 * Explanation:
 * - dp[i] represents whether prefix s[0..i) can be segmented
 * - Base case: dp[0] = true (empty string)
 * - For each position i, check all possible split points j
 * - If dp[j] is true and s[j..i) is in dictionary, then dp[i] = true
 * - Answer is dp[n] — can the full string be segmented?
 */