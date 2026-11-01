package sliding_window

/**
 * LeetCode: https://leetcode.com/problems/longest-substring-without-repeating-characters/
 * Difficulty: Easy
 * Core pattern: Variable-size sliding window with HashSet
 */
fun lengthOfLongestSubstring(s: String): Int {
    val seen = mutableSetOf<Char>()
    var maxLen = 0
    var left = 0

    for (right in s.indices) {
        while (s[right] in seen) {
            seen.remove(s[left])
            left++
        }
        seen.add(s[right])
        maxLen = maxOf(maxLen, right - left + 1)
    }

    return maxLen
}
