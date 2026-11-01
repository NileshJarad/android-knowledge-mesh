package arrays

/**
 * LeetCode: https://leetcode.com/problems/remove-duplicates-from-sorted-array/
 * Difficulty: Easy
 * Core pattern: Two pointers (slow and fast)
 */
fun removeDuplicates(nums: IntArray): Int {
    if (nums.isEmpty()) return 0

    var slow = 0

    for (fast in 1 until nums.size) {
        if (nums[fast] != nums[slow]) {
            slow++
            nums[slow] = nums[fast]
        }
    }

    // slow is the index of last unique element, so length is slow + 1
    return slow + 1
}