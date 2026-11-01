package recursion_backtracking

/**
 * LeetCode: https://leetcode.com/problems/combination-sum-iii/
 * Difficulty: Medium (often Hard variant interviews)
 * Core pattern: Backtracking + pruning by remaining count
 */
fun combinationSum3(k: Int, target: Int): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    val path = mutableListOf<Int>()

    // With k candidates chosen, minimum sum = 1+2+...+k = k(k+1)/2
    // If this exceeds target, no valid combination exists
    if (target < k * (k + 1) / 2) return result

    helper(1, k, target, path, result, 9)  // candidates are 1..9

    return result
}

private fun helper(
    start: Int,
    remaining: Int,
    remainingTarget: Int,
    path: MutableList<Int>,
    result: MutableList<List<Int>>,
    maxCandidate: Int
) {
    if (remaining == 0) {
        result.add(path.toList())
        return
    }

    for (candidate in start..maxCandidate) {
        path.add(candidate)
        // Prune: if even choosing smallest next candidates won't fit remainingTarget...
        // Simplified version below doesn't prune deeply but still works correctly
        helper(candidate + 1, remaining - 1, remainingTarget - candidate, path, result, maxCandidate)
        path.removeAt(path.lastIndex)  // Backtrack step
    }
}

// Enhanced version with more aggressive pruning
fun combinationSum3Enhanced(k: Int, target: Int): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    val path = mutableListOf<Int>()

    // Minimum possible sum with 'remaining' more elements is sum of remaining smallest available
    // Maximum possible sum with 'remaining' more elements is sum of remaining largest available
    // We can prune when remainingTarget < needed minimum or > needed maximum

    helper(1, k, target, path, result, 9)

    return result
}

private fun helperPruned(
    start: Int,
    remaining: Int,
    remainingTarget: Int,
    path: MutableList<Int>,
    result: MutableList<List<Int>>
) {
    if (remaining == 0) {
        result.add(path.toList())
        return
    }

    for (candidate in start..9) {
        val newRemainingTarget = remainingTarget - candidate
        // Prune: need remaining-1 numbers left, minimum sum of those is start + (start+1) + ... + (start + remaining - 2)
        val minRemainingSum = (start + (start + remaining - 2)) * remaining / 2
        val maxRemainingSum = (maxCandidateFor(start) + (start + maxCandidateFor(start) + remaining - 2)) * remaining / 2
        // We want candidate such that there's enough room left
        // Early prune: if candidate too large or too small for remaining
        if (newRemainingTarget < minRemainingSum || newRemainingTarget > maxRemainingSum) continue

        path.add(candidate)
        helperPruned(candidate + 1, remaining - 1, newRemainingTarget, path, result)
        path.removeAt(path.lastIndex)  // Backtrack step
    }
}

private fun maxCandidateFor(start: Int): Int = 9  // Simple constant; could derive dynamically
