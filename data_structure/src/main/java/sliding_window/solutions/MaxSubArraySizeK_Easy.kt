package sliding_window

/**
 * LeetCode: https://leetcode.com/problems/maximum-of-an-empty-window/ (check exact match)
 * Difficulty: Easy
 * Core pattern: Fixed-size sliding window with running sum
 */
fun maxSubArraySum(arr: IntArray, k: Int): Int {
    if (k <= 0 || arr.size < k) return 0

    var currentSum = 0
    for (i in 0 until k) {
        currentSum += arr[i]
    }

    var maxSum = currentSum
    for (i in k until arr.size) {
        currentSum += arr[i] - arr[i - k]  // slide: add new, remove old
        maxSum = maxOf(maxSum, currentSum)
    }

    return maxSum
}
