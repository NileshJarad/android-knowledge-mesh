package practice

class MaxSubarraySum {

    fun maxSubarraySum(array: IntArray, k: Int): Int {
        var maxSum = Int.MIN_VALUE
        val len = array.size
        var start = 0
        var end = 1
        var sum = array[0]
        while (end < len) {
            // as we need to fix the window size
            while (end - start  != k) {
                sum += array[end]
                end++
            }

            // now we got the fix window size we can add next element and remove first element
            sum = sum - array[start] + array[end]
            maxSum = maxOf(maxSum, sum)
            start++
            end++
        }
        return maxSum
    }
}

fun main() {
    val nums = intArrayOf(2, 1, 5, 1, 3, 2)
    val k = 3
    println("Max sum is ${MaxSubarraySum().maxSubarraySum(nums, k)}")
    println("Max sum kotlin  ${maxSubarraySum(nums, k)}")
}

fun maxSubarraySum(array: IntArray, k: Int): Int {
    if (array.size < k) return 0 // Edge case: array smaller than window size

    var windowSum = 0

    // 1. Compute the sum of the first window of size k
    for (i in 0 until k) {
        windowSum += array[i]
    }

    var maxSum = windowSum

    // 2. Slide the window from index k to the end of the array
    for (i in k until array.size) {
        // Add the new element entering the window on the right,
        // and subtract the element leaving the window on the left
        windowSum = windowSum + array[i] - array[i - k]
        maxSum = maxOf(maxSum, windowSum)
    }

    return maxSum
}