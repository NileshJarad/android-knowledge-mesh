package dp

import kotlin.math.max

/***
 * https://takeuforward.org/data-structure/maximum-sum-of-non-adjacent-elements-dp-5/
 */
fun main() {
//    val array = intArrayOf(2, 1, 4, 9)
//    val array = intArrayOf(2, 7, 9, 3,1)
    val array = intArrayOf(2, 3, 2)
    val maxSum = maxSum(array.size - 1, array, IntArray(array.size))


    val maxSumTabulation = maxSumTabulation(array)
    println("Max Non adjacent sum $maxSum")
    println("MaxSumTabulation Non adjacent sum $maxSumTabulation")

    val spaceOptimisedMaxSumTabulation = spaceOptimisedMaxSumTabulation(array)
    println("spaceOptimisedMaxSumTabulation Non adjacent sum $spaceOptimisedMaxSumTabulation")
}

fun maxSum(pos: Int, array: IntArray, dp: IntArray): Int {

    if (pos == 0) return array[pos]
    if (pos < 0) return 0

    if (dp[pos] != 0) return dp[pos]

    val picked = array[pos] + maxSum(pos - 2, array, dp)
    val notPicked = maxSum(pos - 1, array, dp)
    dp[pos] = max(picked, notPicked)
    return dp[pos]
}

fun maxSumTabulation(array: IntArray): Int {
    val dp = IntArray(array.size)
    dp[0] = array[0]
    for (i in 1 until array.size) {
        var take = array[i]
        if (i > 1) take += dp[i - 2]
        val nonTake = dp[i - 1]
        dp[i] = max(take, nonTake)
    }
    return dp[array.size - 1]
}

fun spaceOptimisedMaxSumTabulation(array: IntArray): Int {
    var prev = array[0]
    var prev2 = 0
    for (i in 1 until array.size) {
        val curr = array[i] + prev2
        prev2 = prev
        prev = max(curr, prev)
    }
    return prev
}


