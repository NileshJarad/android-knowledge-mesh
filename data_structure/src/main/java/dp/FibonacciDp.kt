package dp


fun main() {
    val num = 6
    println("N the fibonacci number using Memoization = ${fibMemoization(num)}")

    println("N the fibonacci number using Tabulation = ${fibTabulation(num)}")
    println("N the fibonacci number using Tabulation fibTabulationOptimal = ${fibTabulationOptimal(num)}")

}

val memoization = mutableMapOf<Int, Long>()


/**
 * Time complexity -  O(N)
 * Space complexity = O(N) [memo map] + O(N) [call stack] = O(N)
 */
fun fibMemoization(n: Int): Long {
    if (n <= 1) return n.toLong()
    if (memoization[n] != null) return memoization[n]!!
    val value = fibMemoization(n - 1) + fibMemoization(n - 2)
    memoization[n] = value
    return memoization[n]!!
}


/**
 * Time complexity -  O(N)
 * Space complexity = O(N)  --> No call stack space
 */
fun fibTabulation(n: Int): Long {
    if (n <= 1) return n.toLong()
    val dp = LongArray(n + 1)
    dp[0] = 0
    dp[1] = 1

    for (i in 2..n) {
        dp[i] = dp[i - 1] + dp[i - 2]
    }
    return dp[n]
}

/**
 * Time complexity -  O(N)
 * Space complexity = O(1)
 */
fun fibTabulationOptimal(n: Int): Long {
    if (n <= 1) return n.toLong()
    var prev2 = 0L
    var prev = 1L

    for (i in 2..n) {
        val cur = prev + prev2
        prev2 = prev
        prev = cur
    }
    return prev
}


