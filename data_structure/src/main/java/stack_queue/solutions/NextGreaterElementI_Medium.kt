package stack_queue

/**
 * LeetCode: https://leetcode.com/problems/next-greater-element-i/
 * Difficulty: Medium
 * Core pattern: Decreasing monotonic stack
 */
fun nextGreaterElement(nums: IntArray): IntArray {
    val result = IntArray(nums.size) { -1 }
    val decreasingStack = mutableListOf<Int>()  // stores indices with decreasing heights

    for (i in nums.indices) {
        while (decreasingStack.isNotEmpty() && nums[decreasingStack.last()] < nums[i]) {
            val idx = decreasingStack.pop()
            result[idx] = nums[i]  // nums[i] is next greater for idx
        }
        decreasingStack.add(i)
    }

    return result
}
