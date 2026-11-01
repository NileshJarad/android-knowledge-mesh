package greedy_intervals

/**
 * LeetCode: https://leetcode.com/problems/maximum-profit-in-job-scheduling/
 * Difficulty: Hard
 * Core pattern: DP + Binary Search (weighted interval scheduling, NOT pure greedy)
 *
 * Time Complexity: O(n log n)
 * Space Complexity: O(n)
 */
fun jobScheduling(startTime: IntArray, endTime: IntArray, profit: IntArray): Int {
    val n = startTime.size

    // Combine into jobs and sort by end time
    val jobs = Array(n) { Triple(startTime[i], endTime[i], profit[i]) }
    jobs.sortBy { it.second }  // Sort by end time

    // dp[i] = max profit considering jobs[0..i]
    val dp = IntArray(n)
    dp[0] = jobs[0].third

    for (i in 1 until n) {
        val currentStart = jobs[i].first
        val currentProfit = jobs[i].third

        // Option 1: Skip current job
        var takeCurrent = currentProfit

        // Option 2: Take current + best non-overlapping before it
        // Binary search for last job ending <= currentStart
        var lo = 0
        var hi = i - 1
        var prevNonOverlap = -1

        while (lo <= hi) {
            val mid = (lo + hi) / 2
            if (jobs[mid].second <= currentStart) {
                prevNonOverlap = mid
                lo = mid + 1  // Try to find later non-overlapping job
            } else {
                hi = mid - 1
            }
        }

        if (prevNonOverlap != -1) {
            takeCurrent += dp[prevNonOverlap]
        }

        // dp[i] = max(skip current, take current)
        dp[i] = maxOf(dp[i - 1], takeCurrent)
    }

    return dp[n - 1]
}

/**
 * Explanation:
 * - Weighted interval scheduling: each job has profit, can't overlap
 * - Sort by end time so we can binary search for previous compatible job
 * - dp[i] = max(dp[i-1] [skip], profit[i] + dp[j] [take, where j is last compatible])
 * - Binary search finds j = last job ending <= current job's start
 * - NOT pure greedy — weighted version requires DP because local choice isn't always optimal
 *
 * Key difference from non-overlapping intervals:
 * - Here, intervals have weights (profits), so earliest-end greedy doesn't work
 * - Example: short low-profit job vs long high-profit job overlapping
 */