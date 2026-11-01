package dp_1d

import kotlin.math.maxOf

/**
 * LeetCode: https://leetcode.com/problems/best-time-to-buy-and-sell-stock-with-cooldown/
 * Difficulty: Hard
 * Core pattern: State machine DP — hold, sold, rest states
 *
 * Time Complexity: O(n)
 * Space Complexity: O(1)
 */
fun maxProfit(prices: IntArray): Int {
    if (prices.isEmpty()) return 0

    // State variables
    var hold = -prices[0]   // Max profit holding a stock
    var sold = 0            // Max profit just sold (in cooldown)
    var rest = 0            // Max profit resting (can buy)

    for (i in 1 until prices.size) {
        val prevHold = hold
        val prevSold = sold

        // Can hold from previous, or buy today from rest state
        hold = maxOf(prevHold, rest - prices[i])

        // Can only sell if we were holding
        sold = prevHold + prices[i]

        // Rest from previous rest, or come out of cooldown
        rest = maxOf(prevRest, prevSold)
    }

    return maxOf(sold, rest)
}

// Note: prevRest needs to be tracked. Cleaner version below:

fun maxProfitClean(prices: IntArray): Int {
    if (prices.isEmpty()) return 0

    var hold = -prices[0]
    var sold = 0
    var rest = 0

    for (i in 1 until prices.size) {
        val prevHold = hold
        val prevSold = sold

        hold = maxOf(prevHold, rest - prices[i])
        sold = prevHold + prices[i]
        rest = maxOf(rest, prevSold)
    }

    return maxOf(sold, rest)
}

/**
 * Explanation:
 * - hold: max profit ending day i holding a stock
 * - sold: max profit ending day i just sold a stock (cooldown next day)
 * - rest: max profit ending day i not holding, not in cooldown
 *
 * Transitions:
 * - hold[i] = max(hold[i-1], rest[i-1] - prices[i])  // keep holding or buy today
 * - sold[i] = hold[i-1] + prices[i]                   // sell today
 * - rest[i] = max(rest[i-1], sold[i-1])               // stay resting or cooldown ends
 *
 * Final answer: max(sold, rest) — we shouldn't end holding a stock
 */