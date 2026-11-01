package arrays

/**
 * LeetCode: https://leetcode.com/problems/squares-of-a-sorted-array/
 * Difficulty: Easy
 * Core pattern: Two pointers
 */
fun squaresSortedArray(nums: IntArray): IntArray {
    val n = nums.size
    val result = IntArray(n)
    var left = 0
    var right = n - 1
    var position = n - 1

    while (left <= right) {
        val leftSquare = nums[left].abs() * nums[left].abs()
        val rightSquare = nums[right].abs() * nums[right].abs()

        if (leftSquare > rightSquare) {
            result[position] = leftSquare
            left++
        } else {
            result[position] = rightSquare
            right--
        }
        position--
    }

    return result
}