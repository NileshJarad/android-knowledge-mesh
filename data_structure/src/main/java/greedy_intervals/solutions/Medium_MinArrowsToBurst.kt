package greedy_intervals

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/minimum-number-of-arrows-to-burst-balloons/
 * Difficulty: Medium
 * Core pattern: Sort by end, shoot arrow at end of first balloon
 *
 * Time Complexity: O(n log n)
 * Space Complexity: O(1) extra
 */
fun findMinArrowShots(points: Array<IntArray>): Int {
    if (points.isEmpty()) return 0

    // Sort by END coordinate (right edge of balloon)
    points.sortBy { it[1] }

    var arrow = Long.MIN_VALUE  // Position of last shot arrow
    var arrows = 0

    for (point in points) {
        val start = point[0].toLong()
        val end = point[1].toLong()

        // If balloon starts after arrow position → need new arrow
        if (start > arrow) {
            arrows++
            arrow = end  // Shoot at this balloon's end
        }
        // Else: current arrow at 'arrow' can burst this balloon too
    }

    return arrows
}

/**
 * Explanation:
 * - Each balloon is [x_start, x_end] — an arrow at x bursts all balloons where x_start <= x <= x_end
 * - Greedy: shoot at earliest end coordinate → maximizes coverage of subsequent balloons
 * - If balloon's start > arrow position → previous arrow can't reach → shoot new arrow
 * - Use Long to avoid overflow with extreme coordinate values
 *
 * Proof: shooting at earliest end dominates shooting later — any solution shooting later
 * can be moved earlier without missing any balloons
 */