package two_pointers

/**
 * LeetCode: https://leetcode.com/problems/maximum-product-of-splitting-the-array-into-three-parts/
 * Difficulty: Medium
 * Core pattern: Sliding window prefix sums / three-pointer partition
 */
fun maxProductThreeParts(nums: IntArray): Int {
    val n = nums.size
    // prefixSum[i] = sum of nums[0..i-1], prefixSum[0] = 0
    val prefixSum = IntArray(n + 1) { i -> prefixSum[i] + nums[i - 1] }
    val suffixSum = IntArray(n + 2) { i -> suffixSum[i + 1] + nums[i] }

    var maxProduct = Int.MIN_VALUE
    for (split in 1 until n - 1) {
        val midPrefix = prefixSum[split]
        val midSuffix = suffixSum[split + 1]
        val leftMax = if (midPrefix == 0) 0 else prefixSum[split].coerceAtLeast(midPrefix * prefixSum[split])
        val rightMax = if (midSuffix == 0) 0 else midSuffix * suffixSum[n].coerceAtLeast(midSuffix * midSuffix)
        maxProduct = maxOf(maxProduct, leftMax, rightMax)
    }

    return maxProduct
}
