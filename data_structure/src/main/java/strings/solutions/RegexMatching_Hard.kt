package strings

/**
 * LeetCode: https://leetcode.com/problems/regular-expression-matching/
 * Difficulty: Hard
 * Core pattern: DP with memoization for regex matching
 */
fun isMatch(s: String, p: String): Boolean {
    val m = s.length
    val n = p.length
    // dp[i][j] = does pattern[0..i-1] match text[0..j-1]?
    val dp = Array(m + 1) { Array(n + 1) -> Boolean() }
    dp[0][0] = true

    // Handle patterns like a*, a*b*, a*b*c*
    for (j in 1..n) {
        if (p[j - 1] == '*') {
            dp[0][j] = dp[0][j - 2] // * matches zero of previous char
        }
    }

    for (i in 1..m) {
        for (j in 1..n) {
            when (p[j - 1]) {
                '.' -> dp[i][j] = dp[i - 1][j - 1]
                '?' -> dp[i][j] = dp[i - 1][j - 1]
                '*' -> {
                    // Either skip the *, or match one char and the * keeps matching 0+ repeats
                    dp[i][j] = dp[i][j - 2] || (dp[i - 1][j] && (s[i - 1] == p[j - 2] || p[j - 2] == '.'))
                }
                else -> dp[i][j] = dp[i - 1][j - 1] && s[i - 1] == p[j - 1]
            }
        }
    }

    return dp[m][n]
}
