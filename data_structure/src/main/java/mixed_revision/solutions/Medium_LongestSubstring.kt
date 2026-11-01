package mixed_revision

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/longest-substring-without-repeating-characters/
 * Difficulty: Medium
 * Core pattern: Sliding window + Hash Set (Two pointers)
 *
 * Time Complexity: O(n)
 * Space Complexity: O(min(m, n)) where m = charset size
 */
fun lengthOfLongestSubstring(s: String): Int {
    val n = s.length
    val charSet = HashSet<Char>()
    var left = 0
    var maxLen = 0

    for (right in 0 until n) {
        // Shrink window from left until duplicate removed
        while (charSet.contains(s[right])) {
            charSet.remove(s[left])
            left++
        }
        // Add current character to window
        charSet.add(s[right])
        maxLen = maxOf(maxLen, right - left + 1)
    }

    return maxLen
}

/**
 * Explanation:
 * - charSet tracks characters in current window [left, right]
 * - When duplicate found, shrink from left until duplicate removed
 * - Each character added once, removed at most once → O(n)
 * - maxLen tracks largest valid window seen
 *
 * Optimization: Use HashMap<Char, Int> to store last index,
 * jump left directly to lastIndex[s[right]] + 1 instead of while loop
 */