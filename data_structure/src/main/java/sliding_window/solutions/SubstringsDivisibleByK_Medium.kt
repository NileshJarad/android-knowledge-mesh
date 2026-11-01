package sliding_window

/**
 * LeetCode: https://leetcode.com/problems/number-of-substrings-with-length-divisible-by-k/
 * Difficulty: Medium
 * Core pattern: Sliding window with modulo counting
 */
fun countSubstringsDivisibleByK(s: String, k: Int): Int {
    // Two pointers with prefix count map
    val counts = mutableMapOf<Int, Int>()  // remainder → count of valid subarrays ending at right
    var left = 0
    var answer = 0

    for (right in s.indices) {
        val currentRemainder = (right - left + 1) % k
        if (currentRemainder == 0) {
            answer += counts[0]?.let { it } ?: 0
        } else if (counts.containsKey(currentRemainder)) {
            answer += counts[currentRemainder]!!
        }
        counts.putIfAbsent(currentRemainder, 0)
        counts[currentRemainder] = counts[currentRemainder]!! + 1
    }

    return answer
}
