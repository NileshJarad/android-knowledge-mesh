package sliding_window

import java.util.LinkedList

fun main() {
    val nums = intArrayOf(12, -1, -7, 8, -15, 30, 16, 28)
    val k = 3
    println(firstNegativeInSubArray(nums, k)) // Output: [-1, -1, -7, -15, -15, 0]

}


fun firstNegativeInSubArray(nums: IntArray, k: Int): List<Int> {

    val result = mutableListOf<Int>()
    val queue = LinkedList<Int>()

    var start = 0
    var end = 0

    while (end < nums.size) {
        if (nums[end] < 0) queue.add(nums[end])
        if (end < k - 1) {
            end++
            continue
        }
        result.add(if (queue.isNotEmpty()) queue.first else 0)
        end++
        if (queue.isNotEmpty() && nums[start] < 0) queue.pollFirst()
        start++
    }
    return result
}