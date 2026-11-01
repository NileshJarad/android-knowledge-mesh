package mixed_revision

import kotlin.math.maxOf
import kotlin.math.minOf

/**
 * LeetCode: https://leetcode.com/problems/trapping-rain-water/
 * Difficulty: Hard
 * Core pattern: Two pointers with height comparison
 *
 * Time Complexity: O(n)
 * Space Complexity: O(1)
 */
fun trap(height: IntArray): Int {
    if (height.isEmpty()) return 0

    var left = 0
    var right = height.size - 1
    var leftMax = 0
    var rightMax = 0
    var water = 0

    while (left < right) {
        if (height[left] < height[right]) {
            if (height[left] >= leftMax) {
                leftMax = height[left]
            } else {
                water += leftMax - height[left]
            }
            left++
        } else {
            if (height[right] >= rightMax) {
                rightMax = height[right]
            } else {
                water += rightMax - height[right]
            }
            right--
        }
    }

    return water
}

/**
 * Explanation:
 * - Water trapped at position i = min(maxLeft, maxRight) - height[i]
 * - Two pointers: move from both ends toward center
 * - At each step, process the shorter side (guarantees correctness)
 * - leftMax tracks max height seen from left, rightMax from right
 * - If height[left] < height[right], left side determines water level
 * - O(n) time, O(1) space — no extra arrays needed
 *
 * Why process shorter side:
 * - Water level at left = min(leftMax, rightMax)
 * - If height[left] < height[right], right side is guaranteed >= height[right] > height[left]
 * - So water at left = leftMax - height[left] (if leftMax > height[left])
 * - We don't need to know exact rightMax, just that it's >= height[left]
 */