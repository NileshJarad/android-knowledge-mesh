package dp

import java.lang.Integer.min
import kotlin.math.abs


fun main() {
    val height = intArrayOf(30, 10, 60, 10, 60, 50)
    println("Min const for Jump = ${minCost(height)}")
}

fun minCost(height: IntArray): Int {
    return helperMinCost(height, (height.size - 1), mutableMapOf())
}

fun helperMinCost(height: IntArray?, pos: Int, mutableMapOf: MutableMap<Int, Int>): Int {
    if (pos == 0) return 0

        if (mutableMapOf[pos] != null) return mutableMapOf[pos]!!

    var right = Int.MAX_VALUE
    val left = helperMinCost(height, pos - 1, mutableMapOf) + abs(height?.get(pos)!! - height[pos - 1])
    if (pos > 1) {
        right = helperMinCost(height, pos - 2, mutableMapOf) + abs(height[pos] - height[pos - 2])
    }
    return min(left, right)
}

