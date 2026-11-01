package arrays

/**
 * LeetCode: https://leetcode.com/problems/maximum-product-subarray/
 * Difficulty: Medium
 * Core pattern: Track both max and min (handle negatives)
 */
fun maxProduct(nums: IntArray): Int {
    var maxProduct = nums[0]
    var minProduct = nums[0]
    var result = nums[0]

    for (i in 1 until nums.size) {
        // If current is negative, flipping max to min and vice versa
        val candidates = mutableListOf<Int>()
        candidates.add(nums[i])
        candidates.add(maxProduct * nums[i])
        candidates.add(minProduct * nums[i])

        val currentMax = maxOf(*candidates.toTypedArray())
        val currentMin = minOf(*candidates.toTypedArray())

        maxProduct = currentMax
        minProduct = currentMin
        result = maxOf(result, maxProduct)
    }

    return result
}