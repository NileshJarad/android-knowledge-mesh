package greedy_intervals

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/partition-labels/
 * Difficulty: Medium-Hard
 * Core pattern: Greedy expansion with last occurrence tracking
 *
 * Time Complexity: O(n)
 * Space Complexity: O(1) — 26-element array for lowercase letters
 */
fun partitionLabels(s: String): List<Int> {
    val n = s.length

    // Track last occurrence index of each character
    val lastIndex = IntArray(26) { -1 }
    for (i in s.indices) {
        lastIndex[s[i] - 'a'] = i
    }

    val result = mutableListOf<Int>()

    var start = 0   // Start of current partition
    var end = 0     // End of current partition (furthest last occurrence)

    for (i in s.indices) {
        // Expand partition end if current char appears later
        end = maxOf(end, lastIndex[s[i] - 'a'])

        // If we've reached the end of current partition
        if (i == end) {
            result.add(end - start + 1)
            start = end + 1
        }
    }

    return result
}

/**
 * Explanation:
 * - Each character must appear in exactly one partition
 * - Track last occurrence of each character (first pass)
 * - Greedy: expand partition end to include all chars seen so far
 * - When i reaches end, all characters in partition have their last occurrence inside → close partition
 * - Time: O(n) — single pass after building lastIndex
 * - Space: O(1) — fixed 26-element array (or O(Σ) for general alphabet)
 *
 * Example: s = "ababcbacadefegdehijhklij"
 * - Partition 1: "ababcbaca" → indices 0-8 (chars a,b,c all end within)
 * - Partition 2: "defegde" → indices 9-14 (chars d,e,f,g all end within)
 * - Partition 3: "hijhklij" → indices 15-22 (chars h,i,j,k,l all end within)
 * Result: [9, 7, 8]
 */