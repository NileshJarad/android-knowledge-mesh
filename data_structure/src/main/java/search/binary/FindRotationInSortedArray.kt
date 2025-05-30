package search.binary

fun main() {
//    val intArray = intArrayOf(12, 15, 18, 2, 5, 6, 8, 11)
    val intArray = intArrayOf(4, 5, 6, 7, 0, 1, 2)

    println("Find rotation in sorted array ${findRotationInSortedArray(intArray)}")
}

/**
 * One property for sorted array is that first element is always smaller
 * than last element in case onf ascending sorted element
 *
 *  -- how to find the element in rotated sorted array
 */
fun findRotationInSortedArray(arr: IntArray): Int {

    var left = 0
    var right = arr.size - 1

    while (left <= right) {
        if (arr[left] <= arr[right]) return left
        val mid = left + (right - left) / 2

        if (arr[mid] <= arr[mid - 1] && arr[mid] <= arr[mid + 1]) {
            return mid
        }
        if (arr[mid] < arr[left]) { // if left is sorted then first element should be small than mid
            right = mid - 1
        } else {
            left = mid + 1
        }
    }
    return 0
}