package mixed_revision

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/two-sum/
 * Difficulty: Easy
 * Core pattern: Hash Map (one pass)
 *
 * Time Complexity: O(n)
 * Space Complexity: O(n)
 */
fun twoSum(nums: IntArray, target: Int): IntArray {
    val map = HashMap<Int, Int>()  // value → index

    for (i in nums.indices) {
        val complement = target - nums[i]

        // Check if complement already seen
        if (map.containsKey(complement)) {
            return intArrayOf(map[complement]!!, i)
        }

        // Store current value with its index
        map[nums[i]] = i
    }

    // No solution found (shouldn't happen per problem constraints)
    return intArrayOf()
}

/**
 * Explanation:
 * - For each number, calculate what number is needed (complement)
 * - Check if complement was already seen in hash map
 * - If yes, return both indices
 * - If no, store current number with its index for future lookups
 * - Single pass O(n), hash map O(n) space
 */