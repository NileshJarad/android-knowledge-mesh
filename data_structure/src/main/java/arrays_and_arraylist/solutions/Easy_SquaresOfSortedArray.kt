package arrays_and_arraylist

/**
 * LeetCode: https://leetcode.com/problems/square-of-sorted-array/
 * Difficulty: Easy
 * Core pattern: Two pointers (both ends)
 */
fun sortedSquares(nums: IntArray): IntArray {
    // Initialize pointers at both ends
    var left = 0
    var right = nums.size - 1
    var writePos = right  // Position to write result

    // Create result array
    val result = IntArray(nums.size)

    // Process from largest to smallest
    while (left <= right) {
        // Get squares of both ends
        val leftSquare = nums[left] * nums[left]
        val rightSquare = nums[right] * nums[right]

        // Place larger square at write position
        if (leftSquare > rightSquare) {
            result[writePos] = leftSquare
            left++
        } else {
            result[writePos] = rightSquare
            right--
        }

        writePos--
    }

    return result
}

// Helper functions (if needed)
private fun square(x: Int): Int = x * x

/**
 * Time Complexity: O(n) - each element processed exactly once
 * Space Complexity: O(n) - result array of size n
 */