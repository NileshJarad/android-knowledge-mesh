package recursion


fun subsets(nums: IntArray): List<List<Int>> {
    val result = mutableListOf<List<Int>>()
    helper(nums, 0, mutableListOf<Int>(), result)
    return result
}


fun helper(
    nums: IntArray,
    index: Int,
    current: MutableList<Int>,
    result: MutableList<List<Int>>
){
//    println("Index $index")
    if(index == nums.size) {
        result.add(current.toList())
//        println("Adding current to result ${current.joinToString()}")
        return
    }

    // exclude the current element
    helper(nums, index + 1, current, result)
//    println("Current ${current.joinToString()} result = ${result.joinToString()}")
//
    current.add(nums[index])
//    println("Adding ${nums[index]} Index = $index  Current ${current.joinToString()}")

    helper(nums, index + 1, current, result)
//    println("Removing last from current ${nums[index]} result = ${result.joinToString()}\"")
    current.removeLast()

}

fun main() {
    val inputSet = intArrayOf(1, 2, 3)
    val powerSet = subsets(inputSet)

    println("Power set:")
    for (subset in powerSet) {
        println(subset)
    }
}
