package hashing

/**
 * LeetCode: https://leetcode.com/problems/majority-element-ii/
 * Difficulty: Medium
 * Core pattern: Three candidate tracking with HashMap capping at threshold
 */
fun majorityElement(nums: IntArray): Int {
    val candidates = mutableListOf<Int>()  // up to 3 candidates
    val counts = mutableMapOf<Int, Int>()  // candidate → count, capped at threshold
    val threshold = nums.size / 3

    for (num in nums) {
        if (candidates.any { it == num }) {
            counts[num] = counts[num]!! + 1
        } else {
            if (candidates.size < 3) {
                candidates.add(num)
            }
            counts[num] = if (candidates.size == 3) 0 else 1
        }
        // Trim excess candidates when count exceeds threshold
        if ((counts[num]!!) > threshold && candidates.contains(num)) {
            counts.remove(num)
            val idx = candidates.indexOf(num)
            candidates.removeAt(idx)
        }
    }

    return candidates.firstOrNull() ?: -1
}
