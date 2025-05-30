package search.binary


fun main() {
//    val intArray = intArrayOf(11, 12, 15, 18, 2, 5, 6, 8)
    val intArray = intArrayOf(4, 5, 6, 7, 0, 1, 2)
    val target = 1

    println("Find rotation in sorted array ${finElementInRotatedArray(intArray, 1)}")
}

fun finElementInRotatedArray(arr: IntArray, target: Int): Int {
    val index = findRotationInSortedArray(arr) // we got the min element where array is pivoted

    if (index == -1) return findUsingBinarySearch(0, arr.size - 1, target, arr)
    if (arr[index] == target) return index
    val indexLeft = findUsingBinarySearch(0, index - 1, target, arr)
    val indexRight = findUsingBinarySearch(index + 1, arr.size - 1, target, arr)
    return if (indexLeft != -1) indexLeft else indexRight
}
