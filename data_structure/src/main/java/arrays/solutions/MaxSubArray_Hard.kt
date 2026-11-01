package arrays

/**
 * LeetCode: https://leetcode.com/problems/maximum-subarray/
 * Difficulty: Medium (often grouped with Hard)
 * Core pattern: Kadane's algorithm / Dynamic Programming
 */
fun maxSubArray(nums: IntArray): Int {
    var maxSoFar = nums[0]
    var currentMax = nums[0]

    for (i in 1 until nums.size) {
        currentMax = maxOf(nums[i], currentMax + nums[i])
        maxSoFar = maxOf(maxSoFar, currentMax)
    }

    return maxSoFar
}