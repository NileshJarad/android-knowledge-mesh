package binary_search

/**
 * LeetCode: https://leetcode.com/problems/search-in-rotated-sorted-array/
 * Difficulty: Medium
 * Core pattern: Binary search with half-array selection based on which half is sorted
 */
fun search(nums: IntArray, target: Int): Int {
    var left = 0
    var right = nums.size - 1

    while (left <= right) {
        val mid = left + (right - left) / 2
        when {
            nums[mid] == target -> return mid
            nums[left] <= nums[mid] -> {  // left half sorted
                if (target in nums[left]..nums[mid]) {
                    right = mid - 1
                } else {
                    left = mid + 1
                }
            }
            else -> {  // right half sorted
                if (target in nums[mid]..nums[right]) {
                    left = mid + 1
                } else {
                    right = mid - 1
                }
            }
        }
    }

    return -1
}
