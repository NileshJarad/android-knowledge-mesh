package recursion_backtracking

/**
 * LeetCode: https://leetcode.com/problems/permutations/
 * Difficulty: Medium
 * Core pattern: Backtracking with used tracking
 */
fun permutations(nums: IntArray): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    val path = mutableListOf<Int>()
    val used = BooleanArray(nums.size)

    helper(0, nums.size, path, used, result)

    return result
}

private fun helper(
    position: Int,
    n: Int,
    path: MutableList<Int>,
    used: BooleanArray,
    result: MutableList<List<Int>>
) {
    if (position == n) {
        result.add(path.toList())
        return
    }

    for (i in 0 until n) {
        if (!used[i]) {
            used[i] = true
            path.add(i)
            helper(position + 1, n, path, used, result)
            path.removeAt(path.lastIndex)  // Undo choice (backtrack step)
            used[i] = false                 // Reset tracking
        }
    }
}
