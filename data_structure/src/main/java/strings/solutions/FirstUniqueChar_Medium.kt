package strings

/**
 * LeetCode: https://leetcode.com/problems/first-unique-character-in-a-string/
 * Difficulty: Medium
 * Core pattern: Hash map with frequency count
 */
fun firstUniqChar(s: String): Int {
    val freq = mutableMapOf<Char, Int>()
    for (c in s) {
        freq[c] = freq.getOrDefault(c, 0) + 1
    }
    for (i in s.indices) {
        if (freq[s[i]] == 1) return i
    }
    return -1
}
