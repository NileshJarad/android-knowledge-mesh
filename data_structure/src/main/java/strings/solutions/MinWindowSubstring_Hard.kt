package strings

/**
 * LeetCode: https://leetcode.com/problems/minimum-window-substring/
 * Difficulty: Hard
 * Core pattern: Sliding window + hash map
 */
fun minWindow(s: String, t: String): String {
    if (s.isEmpty() || t.isEmpty()) return ""

    val required = mutableMapOf<Char, Int>()
    for (c in t) {
        required[c] = required.getOrDefault(c, 0) + 1
    }
    val distinctRequired = required.size

    var formed = 0
    val window = HashMap<Char, Int>()
    var left = 0
    var minLen = Int.MAX_VALUE
    var minStart = 0

    for (right in s.indices) {
        val c = s[right]
        window[c] = window.getOrDefault(c, 0) + 1
        if (required[c] != null && window[c]!! == required[c]) {
            formed++
        }

        while (formed == distinctRequired) {
            val wLen = right - left + 1
            if (wLen < minLen) {
                minLen = wLen
                minStart = left
            }
            leftWindowStart()
        }
    }

    return if (minLen == Int.MAX_VALUE) "" else s.substring(minStart, minStart + minLen)

    private fun leftWindowStart() {
        val c = s[left]
        window[c] = window[c]!! - 1
        if (required[c] != null && window[c]!! < required[c]) {
            formed--
        }
        left++
    }
}
