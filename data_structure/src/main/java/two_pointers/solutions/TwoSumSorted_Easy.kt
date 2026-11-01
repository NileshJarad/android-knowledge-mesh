package two_pointers

/**
 * LeetCode: https://leetcode.com/problems/two-sum-ii-input-array-is-sorted/
 * Difficulty: Easy
 * Core pattern: Two pointers (left at start, right at end, converge)
 */
fun twoSum(nums: IntArray, target: Int): List<Int> {
    var left = 0
    var right = nums.size - 1

    while (left < right) {
        val sum = nums[left] + nums[right]
        when {
            sum == target -> return listOf(left, right)
            sum < target -> left++
            else -> right--
        }
    }

    return emptyList() // No solution found (shouldn't happen per problem constraints)
}
