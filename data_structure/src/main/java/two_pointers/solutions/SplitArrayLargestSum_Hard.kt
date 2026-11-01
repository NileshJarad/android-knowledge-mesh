package two_pointers

/**
 * LeetCode: https://leetcode.com/problems/split-array-largest-sum/
 * Difficulty: Hard
 * Core pattern: Binary search + greedy assignment to two subarrays
 */
fun splitArray(nums: IntArray, k: Int): Int {
    var lo = nums.sumOf { it.toInt() } / k  // Each subarray should fit at least this much
    var hi = nums.sumOf { it.toInt() }       // Max possible per subarray is total sum

    while (lo < hi) {
        val mid = lo + (hi - lo) / 2
        if (canSplit(nums, k, mid)) {
            hi = mid  // Try smaller max sum
        } else {
            lo = mid + 1  // Need larger max sum
        }
    }

    return lo
}

private fun canSplit(nums: IntArray, k: Int, maxSum: Int): Boolean {
    var subarrays = 1
    var currentSum = 0

    for (num in nums) {
        if (currentSum + num > maxSum) {
            subarrays++
            currentSum = num
        } else {
            currentSum += num
        }
    }

    return subarrays <= k
}
