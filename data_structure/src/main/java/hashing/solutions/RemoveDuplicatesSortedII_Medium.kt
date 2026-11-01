package hashing

/**
 * LeetCode: https://leetcode.com/problems/remove-duplicates-from-sorted-array-ii/
 * Difficulty: Medium
 * Core pattern: Sliding window with distinct tracking
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

    // slow is the count of unique elements from start to slow
    return slow + 1
}
