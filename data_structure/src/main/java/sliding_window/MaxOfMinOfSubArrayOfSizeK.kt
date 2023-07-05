package sliding_window

import kotlin.math.max

fun main() {
    val intArray = intArrayOf(2, 4, 3, 6, 7, 8, 0, 1, 3)
    println("Max sum is ${MaxSumSlidingWindow().maxSumOfArrayOfSize(intArray, 3)}")
}

class MaxSumSlidingWindow {
    fun maxSumOfArrayOfSize(array: IntArray, size: Int): Int {
        var maxSum = 0
        var currentSum = 0
        var start = 0
        var end = 0
        while (end < array.size) {
            currentSum += array[end]

            val windowSize = end - start + 1
            if (windowSize < size) {
                end++
            }
            if (windowSize == size) {
                maxSum = max(maxSum, currentSum)
                currentSum -= array[start]
                start++
                end++
            }
        }
        return maxSum
    }

}