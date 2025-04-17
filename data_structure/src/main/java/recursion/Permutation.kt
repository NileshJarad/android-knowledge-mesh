package recursion


fun main() {
    val nums = intArrayOf(1, 2, 3)
    println(permutation(nums))

}

fun permutation(nums: IntArray): MutableList<MutableList<Int>> {
    val ans = mutableListOf<MutableList<Int>>()
    val freq = BooleanArray(nums.size) { false }
    permHelper(nums, ans, mutableListOf(), freq)
    return ans
}

fun permHelper(nums: IntArray, ans: MutableList<MutableList<Int>>, currDs: MutableList<Int>, freq: BooleanArray) {
    if (currDs.size == nums.size) {
        ans.add(ArrayList(currDs))
        return
    }

    for (i in nums.indices) {
        if (!freq[i]) {
            freq[i] = true
            currDs.add(nums[i])
            permHelper(nums, ans, currDs, freq)
            currDs.removeLast()
            freq[i] = false
        }
    }

}