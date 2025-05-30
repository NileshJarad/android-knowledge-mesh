package search.binary

fun main() {
    val intArray = intArrayOf(1, 2, 4, 6, 7, 8, 9, 5, 3)

    println("Search in Bitonic array ${intArray[peakInBitonicArray(arr = intArray)]}")
}

fun peakInBitonicArray(arr: IntArray): Int {
    var left = 0
    var right = arr.size - 1

    while (left < right) {
        val mid = (left + right).ushr(1)
        if (arr[mid] < arr[mid + 1]) {
            // Peak is in the right half
            left = mid + 1
        } else {
            // Peak is in the left half (including mid)
            right = mid
        }
    }

    return left
}