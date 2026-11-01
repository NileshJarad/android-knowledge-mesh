package binary_search

/**
 * LeetCode: https://leetcode.com/problems/sqrtx/description/
 * Difficulty: Easy
 * Core pattern: Binary search on answer value (not array!)
 */
fun mySqrt(x: Int): Int {
    if (x < 2) return x

    var lo = 1
    var hi = x / 2

    while (lo <= hi) {
        val mid = lo + (hi - lo) / 2
        when {
            mid * mid <= x -> lo = mid + 1
            else -> hi = mid - 1
        }
    }

    return hi  // hi is largest value where hi² ≤ x
}
