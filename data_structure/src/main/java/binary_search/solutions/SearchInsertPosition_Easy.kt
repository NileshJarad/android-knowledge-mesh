package binary_search

/**
 * LeetCode: https://leetcode.com/problems/search-insert-position/
 * Difficulty: Easy
 * Core pattern: Standard binary search returning insertion position
 */
fun searchInsert(nums: IntArray, target: Int): Int {
    var left = 0
    var right = nums.size

    while (left < right) {
        val mid = left + (right - left) / 2
        when {
            nums[mid] < target -> left = mid + 1
            else -> right = mid  // nums[mid] >= target, could be insertion point or exact match
        }
    }

    return left
}
