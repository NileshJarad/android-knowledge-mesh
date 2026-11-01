package two_pointers

/**
 * LeetCode: https://leetcode.com/problems/median-of-two-sorted-arrays/
 * Difficulty: Hard
 * Core pattern: Binary search on partition points (not two pointers!)
 */
fun findMedianSortedArrays(nums1: IntArray, nums2: IntArray): Double {
    val total = nums1.size + nums2.size
    if (total % 2 == 0) {
        // Even total - average of two middle values
        val leftMax = findMedianPartition(nums1, nums2, total / 2)
        val rightMin = findMedianPartition(nums1, nums2, total / 2 + 1)
        return (leftMax + rightMin).toDouble() / 2.0
    } else {
        // Odd total - single middle value
        return findMedianPartition(nums1, nums2, (total + 1) / 2).toDouble()
    }
}

private fun findMedianPartition(nums1: IntArray, nums2: IntArray, k: Int): Int {
    // Binary search on the smaller array to find correct partition
    var lo = 0
    var hi = Math.min(nums1.size, nums2.size)

    while (lo <= hi) {
        val i = lo + (hi - lo) / 2  // partition index in nums1
        val j = k - i                 // partition index in nums2

        val maxLeft1 = if (i == 0) Int.MIN_VALUE else nums1[i - 1]
        val minRight1 = if (i == nums1.size) Int.MAX_VALUE else nums1[i]
        val maxLeft2 = if (j == 0) Int.MIN_VALUE else nums2[j - 1]
        val minRight2 = if (j == nums2.size) Int.MAX_VALUE else nums2[j]

        when {
            maxLeft1 <= minRight2 && maxLeft2 <= minRight1 -> {
                // Correct partition found
                return maxOf(maxLeft1, maxLeft2)
            }
            maxLeft1 > minRight2 -> {
                // Partition is too far right in nums1, move left
                hi = i - 1
            }
            else -> {
                // Partition is too far left in nums1, move right
                lo = i + 1
            }
        }
    }

    throw IllegalArgumentException("Input arrays not sorted")
}
