package dp_1d

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/best-time-to-buy-and-sell-stock/
 * Difficulty: Medium
 * Core pattern: Track minimum price seen, compute max profit at each step
 *
 * Time Complexity: O(n)
 * Space Complexity: O(1)
 */
fun maxProfit(prices: IntArray): Int {
    if (prices.isEmpty()) return 0

    var minPrice = prices[0]
    var maxProfit = 0

    for (i in 1 until prices.size) {
        maxProfit = maxOf(maxProfit, prices[i] - minPrice)
        minPrice = minOf(minPrice, prices[i])
    }

    return maxProfit
}

/**
 * Explanation:
 * - Track lowest price seen so far (minPrice)
 * - At each day, profit = current price - minPrice
 * - Update maxProfit if current profit is better
 * - Update minPrice if current price is lower
 */