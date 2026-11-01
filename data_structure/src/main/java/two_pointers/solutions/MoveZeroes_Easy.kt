package two_pointers

/**
 * LeetCode: https://leetcode.com/problems/move-zeroes/
 * Difficulty: Easy
 * Core pattern: Two pointers (slow and fast)
 */
fun moveZeroes(nums: IntArray): IntArray {
    var slow = 0

    for (fast in nums.indices) {
        if (nums[fast] != 0) {
            val temp = nums[slow]
            nums[slow] = nums[fast]
            nums[fast] = temp
            slow++
        }
    }

    return nums
}
