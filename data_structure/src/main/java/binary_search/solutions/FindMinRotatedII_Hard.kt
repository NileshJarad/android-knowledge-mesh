package binary_search

/**
 * LeetCode: https://leetcode.com/problems/find-minimum-in-rotated-sorted-array-ii/
 * Difficulty: Medium (harder than non-duplicate version)
 * Core pattern: Modified binary search accounting for duplicates
 */
fun findMin(nums: IntArray): Int {
    var left = 0
    var right = nums.size - 1

    while (left < right) {
        val mid = left + (right - left) / 2
        when {
            nums[mid] < nums[right] -> {  // normal case, search left half
                if (nums[mid] < nums[left]) {
                    right = mid
                } else {
                    left = mid + 1
                }
            }
            nums[mid] == nums[right] -> {  // tricky case with duplicates, safe to decrement right
                right--
            }
            else -> {  // can't determine which side is rotated due to duplicates
                // Fall through to standard handling below
            }
        }
    }

    return nums[left]
}

// Cleaner standard version of the above logic
fun findMinClean(nums: IntArray): Int {
    var left = 0
    var right = nums.size - 1

    while (left < right) {
        val mid = left + (right - left) / 2
        when {
            nums[mid] < nums[right] -> right = mid
            nums[mid] == nums[right] -> right--  // remove duplicate from search space
            else -> left = mid + 1
        }
    }

    return nums[left]
}
