package hashing

/**
 * LeetCode: https://leetcode.com/problems/longest-consecutive-sequence/
 * Difficulty: Medium (interview difficulty varies)
 * Core pattern: HashSet for O(1) lookup of consecutive numbers
 */
fun longestConsecutive(nums: IntArray): Int {
    if (nums.isEmpty()) return 0

    val numSet = mutableSetOf<Int>()
    for (num in nums) {
        numSet.add(num)
    }

    var longest = 0

    for (num in numSet) {
        // Only start a sequence if num-1 is not in set (avoids re-checking shorter substrings)
        if (num - 1 !in numSet) {
            var currentNum = num
            var currentLen = 1

            while (currentNum + 1 in numSet) {
                currentNum++
                currentLen++
            }

            longest = maxOf(longest, currentLen)
        }
    }

    return longest
}
