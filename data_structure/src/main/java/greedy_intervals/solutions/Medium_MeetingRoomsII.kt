package greedy_intervals

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/meeting-rooms-ii/
 * Difficulty: Medium
 * Core pattern: Two pointers on sorted start and end arrays
 *
 * Time Complexity: O(n log n) — dominated by sorting
 * Space Complexity: O(n) — for start and end arrays
 */
fun minMeetingRooms(intervals: Array<IntArray>): Int {
    if (intervals.isEmpty()) return 0

    val n = intervals.size

    // Extract and sort start times and end times separately
    val starts = IntArray(n)
    val ends = IntArray(n)

    for (i in intervals.indices) {
        starts[i] = intervals[i][0]
        ends[i] = intervals[i][1]
    }

    starts.sort()
    ends.sort()

    var rooms = 0
    var endPtr = 0  // Pointer into ends array

    for (i in 0 until n) {
        val start = starts[i]

        if (start >= ends[endPtr]) {
            // A meeting ended — reuse its room
            endPtr++
        } else {
            // No meeting ended yet — need new room
            rooms++
        }
    }

    return rooms
}

/**
 * Explanation:
 * - Sort all start times and all end times independently
 * - Walk through start times chronologically
 * - If a meeting starts after the earliest ending meeting → reuse that room
 * - Otherwise → need a new room
 * - rooms tracks maximum concurrent meetings at any point
 *
 * Alternative: Min-heap of end times — same O(n log n) but more code
 *
 * Why start < end means overlap:
 * - Meeting A ends at time T, meeting B starts at time T → no overlap, room free
 * - Meeting A ends at time T, meeting B starts at T-1 → overlap, need new room
 */