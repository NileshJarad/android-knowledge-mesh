package greedy_intervals

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/non-overlapping-intervals/
 * Difficulty: Medium
 * Core pattern: Greedy interval scheduling — sort by end time
 *
 * Time Complexity: O(n log n)
 * Space Complexity: O(1) extra
 */
fun eraseOverlapIntervals(intervals: Array<IntArray>): Int {
    if (intervals.size <= 1) return 0

    // KEY: Sort by END time — earliest end leaves most room
    intervals.sortBy { it[1] }

    var end = intervals[0][1]
    var nonOverlapping = 1  // First interval always kept

    for (i in 1 until intervals.size) {
        val currentStart = intervals[i][0]
        val currentEnd = intervals[i][1]

        // If current starts at or after end of last kept → no overlap
        if (currentStart >= end) {
            nonOverlapping++
            end = currentEnd  // Keep this interval
        }
        // Else: overlap — skip this interval (implicitly remove)
    }

    return intervals.size - nonOverlapping
}

/**
 * Explanation:
 * - "Remove minimum" = "Keep maximum non-overlapping"
 * - Greedy proof: interval ending earliest leaves most room for others
 * - Exchange argument: any optimal solution can be transformed to pick earliest-ending
 * - Sort by end time ensures we always consider the best candidate next
 *
 * Overlap definition: [1,2] and [2,3] do NOT overlap (current.start >= end)
 * If touching counts as overlap, use > instead of >=
 */