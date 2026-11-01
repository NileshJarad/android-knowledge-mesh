package arrays

/**
 * LeetCode: https://leetcode.com/problems/container-with-most-water/
 * Difficulty: Medium
 * Core pattern: Two pointers
 */
fun maxArea(height: IntArray): Int {
    var left = 0
    var right = height.size - 1
    var maxArea = 0

    while (left < right) {
        val width = right - left
        val currentHeight = minOf(height[left], height[right])
        val area = width * currentHeight
        maxArea = maxOf(maxArea, area)

        if (height[left] < height[right]) {
            left++
        } else {
            right--
        }
    }

    return maxArea
}

import kotlin.math.min
import kotlin.math.max