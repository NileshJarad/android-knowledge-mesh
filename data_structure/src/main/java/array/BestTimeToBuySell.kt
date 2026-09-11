package array

import kotlin.math.min

class BestTimeToBuySell {

    // https://leetcode.com/problems/best-time-to-buy-and-sell-stock/description/
    fun calculate() {
        val stockPrices = intArrayOf(7,1,5,3,6,4)
//        val stockPrices = intArrayOf(7,6,4,3,1)
        var minPrice = Int.MAX_VALUE
        var maxProfit = Int.MIN_VALUE

        for (price in stockPrices) {
            if(minPrice > price) {
                minPrice = price
            }

            if(maxProfit < price - minPrice) {
                maxProfit = price - minPrice
            }
        }
        println("Max Profit will be = $maxProfit")
    }
}


fun main() {
    BestTimeToBuySell().calculate()
}