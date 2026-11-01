package recursion_backtracking

/**
 * LeetCode: https://leetcode.com/problems/climbing-stairs/
 * Difficulty: Easy
 * Core pattern: Classic recursion with memoization (or iterative)
 */
class StairsClimb {
    private val memo = mutableMapOf<Int, Int>()

    fun climb(n: Int): Int {
        if (n <= 1) return n
        return memo.getOrPut(n) { climb(n - 1) + climb(n - 2) }
    }
}
