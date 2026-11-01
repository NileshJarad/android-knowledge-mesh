package greedy_intervals

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/merge-intervals/
 * Difficulty: Medium
 * Core pattern: Sort by start time, merge overlapping intervals
 *
 * Time Complexity: O(n log n) — dominated by sorting
 * Space Complexity: O(n) — for result list
 */
fun merge(intervals: Array<IntArray>): Array<IntArray> {
    if (intervals.size <= 1) return intervals

    // Sort by START time
    intervals.sortBy { it[0] }

    val result = mutableListOf<IntArray>()

    for (interval in intervals) {
        val start = interval[0]
        val end = interval[1]

        // If result is empty OR no overlap with last merged interval
        if (result.isEmpty() || result.last()[1] < start) {
            result.add(interval)
        } else {
            // Overlap — merge by extending end of last interval
            result.last()[1] = maxOf(result.last()[1], end)
        }
    }

    return result.toTypedArray()
}

/**
 * Explanation:
 * - Sort intervals by start time so overlapping intervals are adjacent
 * - Maintain merged list; each new interval either extends last or starts new
 * - Overlap condition: current.start <= last.end → merge
 * - Non-overlap condition: current.start > last.end → add as new interval
 * - Merging: last.end = max(last.end, current.end)
 *
 * Edge case: [1,2] and [2,3] — 2 == 2, NOT overlapping (non-strict)
 * If you want to merge touching intervals, change < to <=
 */