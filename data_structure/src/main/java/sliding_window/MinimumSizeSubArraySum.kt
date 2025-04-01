package sliding_window

fun main() {
    /**
     * https://leetcode.com/problems/minimum-size-subarray-sum/description/?envType=problem-list-v2&envId=sliding-window
     */
    val num1 = intArrayOf(2, 3, 1, 2, 4, 3)
    val target1 = 7
    println("containsNearbyDuplicate target  $num1  $target1 o/p = 2 =  ${minSubArrayLen(7, num1)}")
}

fun minSubArrayLen(target: Int, nums: IntArray): Int {
    var left = 0
    var right = 0
    var sum = 0
    var minLength = Int.MAX_VALUE

    while (right < nums.size) {
        sum += nums[right] // Expand window

        // Shrink window when sum ≥ target
        while (sum >= target) {
            minLength = minOf(minLength, right - left + 1)
            sum -= nums[left] // Shrink from left
            left++
        }
        right++
    }

    return minLength
}