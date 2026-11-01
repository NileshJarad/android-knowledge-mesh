package recursion_backtracking

/**
 * LeetCode: https://leetcode.com/problems/combination-sum/
 * Difficulty: Medium
 * Core pattern: Backtracking with reuse allowed, no reorder across candidates
 */
fun combinationSum(candidates: IntArray, target: Int): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    val path = mutableListOf<Int>()
    candidates.sorted()  // Sort for potential pruning (though not strictly needed)
    helper(0, candidates, target, path, result)
    return result
}

private fun helper(
    startIdx: Int,
    candidates: IntArray,
    remaining: Int,
    path: MutableList<Int>,
    result: MutableList<List<Int>>
) {
    if (remaining <= 0) {
        result.add(path.toList())
        return
    }

    for (i in startIdx until candidates.size) {
        if (candidates[i] <= remaining) {
            path.add(candidates[i])
            helper(i, candidates, remaining - candidates[i], path, result)  // i allows reuse
            path.removeAt(path.lastIndex)  // Backtrack step
        }
        // Early pruning: since candidates are sorted and positive, if even smallest exceeds remaining, break
        else if (candidates[i] > remaining) break
    }
}
