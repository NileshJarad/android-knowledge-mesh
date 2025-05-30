package search.binary


fun main() {
    val intArray = intArrayOf(1, 2, 4, 6, 7, 8, 9)

    println("Found 2 at ${findUsingBinarySearch(leftPar = 0, rightPar = intArray.size - 1, 2, arr = intArray)}")
    println("Found 5 at ${findUsingBinarySearch(leftPar = 0, rightPar = intArray.size - 1, 5, arr = intArray)}")
    println("Found 10 at ${findUsingBinarySearch(leftPar = 0, rightPar = intArray.size - 1, 10, arr = intArray)}")
    println("Found 9 at ${findUsingBinarySearch(leftPar = 0, rightPar = intArray.size - 1, 9, arr = intArray)}")
    println()
    println("Found 2 at ${binarySearchRec(left = 0, right = intArray.size - 1, arr = intArray, 2)}")
    println("Found 5 at ${binarySearchRec(left = 0, right = intArray.size - 1, arr = intArray, 5)}")
    println("Found 10 at ${binarySearchRec(left = 0, right = intArray.size - 1, arr = intArray, 10)}")
    println("Found 9 at ${binarySearchRec(left = 0, right = intArray.size - 1, arr = intArray, 9)}")
}


/***
 *  - Also think of applying binary search on Reverse sorted array
 *  - Also think of give array is sorted but don't know in which order
 */
fun findUsingBinarySearch(leftPar: Int, rightPar: Int, target: Int, arr: IntArray): Int {
    var left = leftPar
    var right = rightPar

    while (left <= right) {
        val mid = (left + right).ushr(1) //left + (right - left) / 2
        when {
            target == arr[mid] -> return mid
            target < arr[mid] -> right = mid - 1
            else -> left = mid + 1
        }
    }

    return -1
}

fun binarySearchRec(left: Int, right: Int, arr: IntArray, target: Int): Int {
    if (right >= arr.size || left < 0 || left > right) return -1
    val mid = left + (right - left) / 2
    if (arr[mid] == target) return mid

    return if (arr[mid] > target) {
        binarySearchRec(
            left, mid - 1, arr, target
        )
    } else {
        binarySearchRec(
            mid + 1,
            right,
            arr,
            target
        )
    }
}