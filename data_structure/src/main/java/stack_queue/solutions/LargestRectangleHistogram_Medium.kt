package stack_queue

/**
 * LeetCode: https://leetcode.com/problems/largest-rectangle-in-histogram/
 * Difficulty: Medium
 * Core pattern: Increasing monotonic stack
 */
fun largestRectangleArea(heights: IntArray): Int {
    val stack = mutableListOf<Int>()  // stores indices in increasing order of heights
    var maxArea = 0

    for (i in heights.indices) {
        while (stack.isNotEmpty() && heights[stack.last()] > heights[i]) {
            val h = heights[stack.pop()]
            val w = if (stack.isEmpty()) i else i - stack.last() - 1
            maxArea = maxOf(maxArea, h * w)
        }
        stack.add(i)
    }

    // Process remaining elements in stack — extend to end of array
    while (stack.isNotEmpty()) {
        val h = heights[stack.pop()]
        val w = if (stack.isEmpty()) heights.size else heights.size - stack.last() - 1
        maxArea = maxOf(maxArea, h * w)
    }

    return maxArea
}
