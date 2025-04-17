package recursion

/**
 * https://leetcode.com/problems/combination-sum/description/
 *
 * Given an array of distinct integers candidates and a target integer target, return a list of all unique combinations of candidates where the chosen numbers sum to target. You may return the combinations in any order.
 *
 * The same number may be chosen from candidates an unlimited number of times. Two combinations are unique if the frequency of at least one of the chosen numbers is different.
 *
 * The test cases are generated such that the number of unique combinations that sum up to target is less than 150 combinations for the given input.
 *
 *
 *
 * Example 1:
 *
 * Input: candidates = [2,3,6,7], target = 7
 * Output: [[2,2,3],[7]]
 * Explanation:
 * 2 and 3 are candidates, and 2 + 2 + 3 = 7. Note that 2 can be used multiple times.
 * 7 is a candidate, and 7 = 7.
 * These are the only two combinations.
 * Example 2:
 *
 * Input: candidates = [2,3,5], target = 8
 * Output: [[2,2,2,2],[2,3,3],[3,5]]
 * Example 3:
 *
 * Input: candidates = [2], target = 1
 * Output: []
 *
 */
fun main() {
    val candidates = intArrayOf(2, 3, 6, 7)
    println("Combination ${combinationSum(candidates, 7)}")
}

fun combinationSum(candidates: IntArray, target: Int): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    helper(0, candidates, target, result, mutableListOf(), 0)
    return result
}


fun helper(
    index: Int,
    array: IntArray,
    target: Int,
    result: MutableList<List<Int>>,
    currentStack: MutableList<Int>,
    sum: Int
) {
    if (sum == target) {
        result.add(ArrayList(currentStack)) // Make a copy before adding
        return
    }
    if (index == array.size || sum > target) return
    currentStack.add(array[index])
    helper(index , array, target, result, currentStack, sum + array[index]) // reuse allowed
    currentStack.removeLast()

    // Exclude current element and move to next
    helper(index + 1, array, target, result, currentStack, sum )

}
