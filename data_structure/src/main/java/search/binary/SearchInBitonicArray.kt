package search.binary

fun main() {
    val intArray = intArrayOf(1, 2, 4, 6, 7, 8, 9, 5, 3)

    println("Search in Bitonic array ${searchBitonicArray(6, arr = intArray)}")
}

fun searchBitonicArray(target: Int, arr: IntArray): Int {

    val peakElement = peakInBitonicArray(arr)

    val left = findUsingBinarySearch(0, peakElement - 1, target, arr)
    val right = findUsingBinarySearch(peakElement, arr.size - 1, target, arr)

    return  if(left != -1) left else right
}