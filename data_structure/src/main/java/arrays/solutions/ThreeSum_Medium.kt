package arrays

/**
 * LeetCode: https://leetcode.com/problems/three-sum/
 * Difficulty: Medium
 * Core pattern: Two pointers on sorted array + fixed element
 */
fun threeSum(nums: IntArray): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    Arrays.sort(nums)
    val n = nums.size

    for (i in 0 until n - 2) {
        // Skip duplicate first element
        if (i > 0 && nums[i] == nums[i - 1]) continue

        var left = i + 1
        var right = n - 1

        while (left < right) {
            val sum = nums[i] + nums[left] + nums[right]
            when {
                sum == 0 -> {
                    result.add(listOf(nums[i], nums[left], nums[right]))
                    left++
                    right--
                    // Skip duplicates
                    while (left < right && nums[left] == nums[left - 1]) left++
                    while (left < right && nums[right] == nums[right + 1]) right--
                }
                sum < 0 -> left++
                sum > 0 -> right--
            }
        }
    }

    return result
}