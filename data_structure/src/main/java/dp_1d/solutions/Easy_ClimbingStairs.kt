package dp_1d

/**
 * LeetCode: https://leetcode.com/problems/climbing-stairs/
 * Difficulty: Easy
 * Core pattern: Linear recurrence (Fibonacci)
 *
 * Time Complexity: O(n)
 * Space Complexity: O(1)
 */
fun climbStairs(n: Int): Int {
    if (n <= 2) return n

    var first = 1   // ways to reach step 1
    var second = 2  // ways to reach step 2

    for (i in 3..n) {
        val current = first + second
        first = second
        second = current
    }

    return second
}

/**
 * Explanation:
 * - To reach step n, you can come from step n-1 (1 step) or step n-2 (2 steps)
 * - ways[n] = ways[n-1] + ways[n-2] — this is Fibonacci
 * - Base cases: ways[1] = 1, ways[2] = 2
 * - Space optimized: only track last two values
 */