package recursion

/**
 * https://leetcode.com/problems/combination-sum/description/
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
