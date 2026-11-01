package binary_search

/**
 * LeetCode: https://leetcode.com/problems/find-peak-element/
 * Difficulty: Medium (often appeared in Hard variant)
 * Core pattern: Binary search comparing mid with right neighbor
 */
fun findPeakElement(nums: IntArray): Int {
    var left = 0
    var right = nums.size - 1

    while (left < right) {
        val mid = left + (right - left) / 2
        when {
            nums[mid] > nums[mid + 1] -> right = mid  // peak is at mid or to the left
            else -> left = mid + 1                     // peak is to the right of mid
        }
    }

    return left  // left == right, point is a peak
}

import kotlin.math.abs
