package dp_1d

/**
 * LeetCode: https://leetcode.com/problems/maximum-subarray/
 * Difficulty: Easy
 * Core pattern: Kadane's Algorithm — max subarray ending at each position
 *
 * Time Complexity: O(n)
 * Space Complexity: O(1)
 */
fun maxSubArray(nums: IntArray): Int {
    var maxEndingHere = nums[0]
    var maxSoFar = nums[0]

    for (i in 1 until nums.size) {
        // Either extend previous subarray or start new at current element
        maxEndingHere = maxOf(nums[i], maxEndingHere + nums[i])
        maxSoFar = maxOf(maxSoFar, maxEndingHere)
    }

    return maxSoFar
}

/**
 * Explanation:
 * - At each element, decide: extend previous subarray or start fresh
 * - maxEndingHere = max(nums[i], maxEndingHere + nums[i])
 * - Track global maximum in maxSoFar
 * - Works for all-negative arrays (returns least negative / max single element)
 */