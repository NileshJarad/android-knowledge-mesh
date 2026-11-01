package binary_search

/**
 * LeetCode: https://leetcode.com/problems/find-first-and-last-position-of-element-in-sorted-array/
 * Difficulty: Medium
 * Core pattern: Two modified binary searches (first occurrence + last occurrence)
 */
fun searchRange(nums: IntArray, target: Int): IntPair {
    val first = firstOccurrence(nums, target)
    if (first == -1) return IntPair(-1, -1)
    val last = lastOccurrence(nums, target)
    return IntPair(first, last)
}

fun firstOccurrence(arr: IntArray, target: Int): Int {
    var left = 0
    var right = arr.size - 1
    while (left < right) {
        val mid = left + (right - left) / 2
        when {
            arr[mid] < target -> left = mid + 1
            else -> right = mid  // found possible position, keep searching left
        }
    }
    return if (left < arr.size && arr[left] == target) left else -1
}

fun lastOccurrence(arr: IntArray, target: Int): Int {
    var left = 0
    var right = arr.size - 1
    while (left < right) {
        val mid = left + (right - left) / 2
        when {
            arr[mid] <= target -> left = mid + 1
            else -> right = mid  // found possible position, keep searching right
        }
    }
    return if (right >= 0 && arr[right] == target) right else -1
}
