package stack_queue

/**
 * LeetCode: https://leetcode.com/problems/product-of-array-except-self/
 * Difficulty: Medium
 * Core pattern: Prefix × Suffix arrays + forward and backward passes
 */
fun productExceptSelf(nums: IntArray): IntArray {
    val n = nums.size
    val output = IntArray(n)

    // Forward pass: output[i] = product of all elements before i
    var runningProduct = 1L
    for (i in 0 until n) {
        output[i] = runningProduct.toInt()
        runningProduct *= nums[i]
    }

    // Backward pass: multiply by product of all elements after i
    runningProduct = 1L
    for (i in n - 1 downTo 0) {
        output[i] = (output[i].toLong() * runningProduct).toInt()
        runningProduct *= nums[i]
    }

    return output
}
