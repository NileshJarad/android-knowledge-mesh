package sliding_window

/**
 * LeetCode: https://leetcode.com/problems/maximum-length-of-pair-chain/
 * Difficulty: Medium (often Hard variant)
 * Core pattern: Sort pairs by end + greedy interval scheduling
 */
fun maxLengthOfPairChain(pairs: Array<List<Int>>): Int {
    if (pairs.isEmpty()) return 0

    // Sort pairs by end time (second element)
    val sorted = pairs.sortedByDescending { it[1] }  // Sort ascending by end is fine too

    var maxLength = 1
    var lastEnd = sorted[0][1]

    for (i in 1 until sorted.size) {
        if (sorted[i][0] > lastEnd) {
            maxLength++
            lastEnd = sorted[i][1]
        }
    }

    return maxLength
}
