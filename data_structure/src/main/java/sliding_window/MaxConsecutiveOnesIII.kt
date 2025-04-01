package sliding_window

fun main() {

    /**
     * https://leetcode.com/problems/max-consecutive-ones-iii/description/
     */
    println("Max Ones ${longestOnes(intArrayOf(1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0), 2)}")
}



fun longestOnes(nums: IntArray, k: Int): Int {
    var zeroCount = 0
    var maxLen = 0
    var left = 0
    var right = 0
    while (right < nums.size) {
        if (nums[right] == 0) {
            zeroCount++
        }

        if (zeroCount <= k) {
            maxLen = maxOf(maxLen, right - left + 1)
        } else {
            if(nums[left] == 0){
                zeroCount--
            }
            left++
        }
        right++
    }
    return maxLen
}

fun longestOnesWithLoop(nums: IntArray, k: Int): Int {
    var zeroCount = 0
    var maxLen = 0
    var left = 0
    var right = 0
    while (right < nums.size) {
        if (nums[right] == 0) {
            zeroCount++
        }

        while (zeroCount > k) {
            if (nums[left] == 0) zeroCount--
            left++
        }

        maxLen = maxOf(maxLen, right - left + 1)
        right++
    }
    return maxLen
}