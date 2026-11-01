package hashing

/**
 * LeetCode: https://leetcode.com/problems/contains-duplicate/
 * Difficulty: Easy
 * Core pattern: HashSet membership check
 */
fun containsDuplicate(nums: IntArray): Boolean {
    val seen = mutableSetOf<Int>()
    for (num in nums) {
        if (num in seen) return true
        seen.add(num)
    }
    return false
}
