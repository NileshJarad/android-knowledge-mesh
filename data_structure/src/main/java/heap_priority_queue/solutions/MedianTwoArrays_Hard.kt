package heap_priority_queue

/**
 * LeetCode: https://leetcode.com/problems/median-of-two-sorted-arrays/
 * Difficulty: Hard
 * Core pattern: Binary search on partition + careful comparison logic
 */
fun findMedianSortedArrays(nums1: IntArray, nums2: IntArray): Double {
    val total = nums1.size + nums2.size
    if (total % 2 == 0) {
        // Even total: average of two middle values
        val leftMax = findPartitionValue(nums1, nums2, total / 2)
        val rightMin = findPartitionValue(nums1, nums2, total / 2 + 1)
        return ((leftMax + rightMin).toDouble()) / 2.0
    } else {
        // Odd total: single middle value
        return findPartitionValue(nums1, nums2, (total + 1) / 2).toDouble()
    }
}

// Find the value at partition position k (0-indexed) such that elements at left and right are balanced
private fun findPartitionValue(nums1: IntArray, nums2: IntArray, k: Int): Int {
    // Binary search on the smaller array to avoid O(N log N) worst case
    var lo = 0
    var hi = Math.min(nums1.size, nums2.size)

    while (lo <= hi) {
        val i = lo + (hi - lo) / 2  // partition index in nums1
        val j = k - i               // partition index in nums2

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
                // i is too large, shrink
                hi = i - 1
            }
            else -> {
                // j is too small, grow
                lo = i + 1
            }
        }
    }

    throw IllegalArgumentException("Input arrays not sorted")
}

import kotlin.math.min
import kotlin.math.max