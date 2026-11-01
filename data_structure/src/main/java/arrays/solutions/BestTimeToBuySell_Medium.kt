package arrays

/**
 * LeetCode: https://leetcode.com/problems/best-time-to-buy-and-sell-stock/
 * Difficulty: Medium
 * Core pattern: One pass / tracking minimum
 */
fun maxProfit(prices: IntArray): Int {
    var minPrice = Int.MAX_VALUE
    var maxProfit = 0

    for (price in prices) {
        if (price < minPrice) {
            minPrice = price
        } else if (price - minPrice > maxProfit) {
            maxProfit = price - minPrice
        }
    }

    return maxProfit
}