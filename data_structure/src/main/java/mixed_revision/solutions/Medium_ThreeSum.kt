package mixed_revision

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/3sum/
 * Difficulty: Medium
 * Core pattern: Sorting + Two pointers
 *
 * Time Complexity: O(n²)
 * Space Complexity: O(1) excluding output
 */
fun threeSum(nums: IntArray): List<List<Int>> {
    nums.sort()
    val result = mutableListOf<List<Int>>()
    val n = nums.size

    for (i in 0 until n - 2) {
        // Skip duplicate first elements
        if (i > 0 && nums[i] == nums[i - 1]) continue

        var left = i + 1
        var right = n - 1
        val target = -nums[i]

        while (left < right) {
            val sum = nums[left] + nums[right]

            when {
                sum == target -> {
                    result.add(listOf(nums[i], nums[left], nums[right]))
                    // Skip duplicates
                    while (left < right && nums[left] == nums[left + 1]) left++
                    while (left < right && nums[right] == nums[right - 1]) right--
                    left++
                    right--
                }
                sum < target -> left++
                else -> right--
            }
        }
    }

    return result
}

/**
 * Explanation:
 * - Sort array first (O(n log n))
 * - Fix first element, use two pointers for remaining
 * - For each i, find pairs (left, right) where nums[left] + nums[right] = -nums[i]
 * - Skip duplicates to avoid duplicate triplets
 * - O(n²) time: outer loop O(n), inner two-pointer O(n)
 * - O(1) space excluding output (sort in-place)
 */